// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.derrick;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The top of a hole bored down to the Nether's magma. Heat decides whether magma melt comes up at all
 * (the fire reaching this block against {@code MAGMA_MIN_TU}); rotation decides how fast (mB per RPM, scaled by
 * how far the fire is past the minimum). Output is pushed into any adjacent fluid handler, normally a vessel
 * that settles it into metals and slag. The bit stays in the hole: fire hotter than its service
 * temperature wears it, nothing else does, so a good bit lasts and a poor one tells you why.
 * shortcut: the melt is one fixed composition at any depth; the settling vessel does the separating.
 */
public class MagmaHeadBlockEntity extends RotationNode {

    private ItemStack bit = ItemStack.EMPTY;
    private float owed;
    private int lastMoved;
    private float fireTu;

    public MagmaHeadBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.MAGMA_HEAD.get(), pos, state);
    }

    public void setBit(ItemStack bit) {
        this.bit = bit;
        setChanged();
    }

    public ItemStack takeBit() {
        ItemStack taken = bit;
        bit = ItemStack.EMPTY;
        setChanged();
        return taken;
    }

    public boolean hasBit() {
        return !bit.isEmpty();
    }

    @Override
    public Su getLoadSu() {
        return bit.isEmpty() ? new Su(0f) : FTuning.DERRICK_LOAD_SU;
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel server))
            return;
        lastMoved = 0;
        if (bit.isEmpty())
            return;
        fireTu = HeatSource.at(server, worldPosition).value();
        float rpm = Math.abs(getRpm().value());
        if (rpm <= 0f || fireTu < FTuning.MAGMA_MIN_TU)
            return;
        float heat = Math.min(1f, (fireTu - FTuning.MAGMA_MIN_TU) / (FTuning.MAGMA_FULL_TU - FTuning.MAGMA_MIN_TU)) * 0.9f + 0.1f;
        // Wear is for over-firing only: the heat the head needs to flow freely never wears a bit, a fire past it (an arc) wears one that cannot take it.
        float service = Math.max(DerrickBitItem.serviceTemperature(bit), FTuning.MAGMA_FULL_TU);
        if (fireTu > service) {
            float condition = Math.max(FTuning.DERRICK_BIT_CONDITION_FLOOR,
                    DerrickBitItem.condition(bit) - (fireTu - service) * FTuning.MAGMA_BIT_WEAR_PER_TU);
            bit.set(FDataComponents.TOOL_CONDITION.get(), condition);
            setChanged();
        }
        owed += rpm * FTuning.MAGMA_MB_PER_RPM * heat * DerrickBitItem.condition(bit);
        int amount = (int) owed;
        owed -= amount;
        if (amount > 0)
            lastMoved = push(server, amount);
        if (lastMoved > 0 && server.getGameTime() % 5 == 0)
            server.sendParticles(ParticleTypes.LAVA, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                    worldPosition.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0.02);
    }

    private int push(ServerLevel server, int amount) {
        // A bit that takes more heat reaches the richer melt below the lean first layer; a poorer one only brings up the lean.
        boolean rich = DerrickBitItem.serviceTemperature(bit) >= FTuning.MAGMA_RICH_BIT_SERVICE_TU;
        FluidStack offer = new FluidStack((rich ? FFluids.MAGMA_MELT_RICH : FFluids.MAGMA_MELT).get(), amount);
        int moved = 0;
        for (Direction d : Direction.values()) {
            if (moved >= amount)
                break;
            IFluidHandler target = server.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(d), d.getOpposite());
            if (target != null)
                moved += target.fill(offer.copyWithAmount(amount - moved), IFluidHandler.FluidAction.EXECUTE);
        }
        return moved;
    }

    /** What the player reads on use: fire, speed and the bit. */
    public Component describe() {
        String state = bit.isEmpty() ? "no_bit" : fireTu < FTuning.MAGMA_MIN_TU ? "cold" : Math.abs(getRpm().value()) <= 0f ? "idle"
                : lastMoved > 0 ? "flowing" : "blocked";
        return Component.translatable("message.feedback.magma_head." + state, Math.round(fireTu),
                bit.isEmpty() ? 0 : Math.round(DerrickBitItem.condition(bit) * 100f));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!bit.isEmpty())
            tag.put("Bit", bit.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        bit = tag.contains("Bit") ? ItemStack.parseOptional(registries, tag.getCompound("Bit")) : ItemStack.EMPTY;
    }
}
