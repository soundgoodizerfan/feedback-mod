// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.induction;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricTuning;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A coil that heats one metal workpiece laid in it, quickly and cleanly, to a target the player sets on the block
 * ({@link ElectricMachineTuning#PREHEAT_TARGETS_TU}, a few steps up to {@link ElectricMachineTuning#INDUCTION_CEILING_TU}, the first of them off). It knows nothing of
 * the piece's forging window: matching the target to the work is the player's. It heats the held stack's {@link ItemHeat} stamp while
 * powered and does nothing else: unpowered, the piece cools in the slot at the item's own rate. Only a metal couples to the field.
 */
public class PreheatCoilBlockEntity extends ElectricNode {

    private ItemStack held = ItemStack.EMPTY;
    /** Whether the held piece is a metal, which is what a field couples to. Looked up when laid in. */
    private boolean coupled;
    private boolean coupledKnown;
    private int target = ElectricMachineTuning.PREHEAT_DEFAULT_TARGET;
    private float power;

    public PreheatCoilBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.PREHEAT_COIL.get(), pos, state);
    }

    public ItemStack held() {
        return held;
    }

    /** The temperature the coil heats a piece to, in Tu; 0 when it is switched off. */
    public float targetTu() {
        return ElectricMachineTuning.PREHEAT_TARGETS_TU[target];
    }

    /** The held piece's temperature now, or null with nothing laid in. */
    public Tu heldTemperature() {
        return level == null || held.isEmpty() ? null : ItemHeat.get(held, level);
    }

    /** Whether the coil is delivering power to the piece this tick, which a readout tells apart from a piece that is not a metal. */
    public boolean heating() {
        return power > 0f && wantsHeat();
    }

    /** Whether the held piece couples to the field (is a metal). */
    public boolean couples() {
        if (!coupledKnown)
            findCoupling();
        return coupled;
    }

    public void cycleTarget() {
        target = (target + 1) % ElectricMachineTuning.PREHEAT_TARGETS_TU.length;
        setChanged();
    }

    public void lay(ItemStack stack) {
        if (level != null)
            ItemHeat.settle(stack, level);
        held = stack;
        coupledKnown = false;
        setChanged();
    }

    public ItemStack take() {
        ItemStack out = held;
        held = ItemStack.EMPTY;
        coupledKnown = false;
        setChanged();
        return out;
    }

    private void findCoupling() {
        coupledKnown = true;
        coupled = !held.isEmpty() && Material.find(MaterialTable.get().materials(), held)
                .filter(found -> found.material().kind().filter(k -> k == Material.Kind.METAL).isPresent()).isPresent();
    }

    private boolean wantsHeat() {
        if (level == null || targetTu() <= 0f)
            return false;
        if (!coupledKnown)
            findCoupling();
        return coupled && ItemHeat.get(held, level).value() < targetTu();
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return wantsHeat() ? ElectricMachineTuning.PREHEAT_AMPS : 0f;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.MV;
    }

    @Override
    public void onResolved(VoltageBand band, float eu, float amps, float wanted, boolean blocked) {
        super.onResolved(band, eu, amps, wanted, blocked);
        power = eu > 0f && amps > 0f ? Math.min(1f, eu * amps / (ElectricTuning.PACKET_MV_EU * ElectricMachineTuning.PREHEAT_AMPS)) : 0f;
    }

    public void tickServer() {
        if (level == null || power <= 0f || !wantsHeat())
            return;
        float now = ItemHeat.get(held, level).value();
        ItemHeat.set(held, new Tu(Math.min(targetTu(), now + ElectricMachineTuning.PREHEAT_TU_PER_TICK.tuPerTick() * power)), level);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!held.isEmpty())
            tag.put("Held", held.save(registries));
        tag.putInt("Target", target);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        held = tag.contains("Held") ? ItemStack.parseOptional(registries, tag.getCompound("Held")) : ItemStack.EMPTY;
        target = tag.contains("Target") ? Math.floorMod(tag.getInt("Target"), ElectricMachineTuning.PREHEAT_TARGETS_TU.length) : ElectricMachineTuning.PREHEAT_DEFAULT_TARGET;
        coupledKnown = false;
    }
}
