/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.machine.derrick;

import java.util.List;

import javax.annotation.Nullable;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.multiblock.Multiblocks;
import io.github.soundgoodizerfan.feedback.core.multiblock.Part;
import io.github.soundgoodizerfan.feedback.core.multiblock.RoleRule;
import io.github.soundgoodizerfan.feedback.core.multiblock.Structure;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.geology.BasinState;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The bottom of the derrick stack: a rotation load that turns a bit down through the rock under it
 * (spec 3.1). Boring is the Removal comparison done a block at a time: the bit's material hardness
 * must exceed the rock's ({@code Removal#canBeCutBy}), and once it does, a turn delivers drive over
 * rock hardness in Fu ({@code Removal#cutFrom}). A bit too soft for the rock delivers nothing and
 * takes the force itself -- it wears, the machine does not, and nothing is ever refused. Turning
 * past the rated RPM wears the bit the same way.
 * <p>
 * Reaching the basin's depth swaps this block for a wellhead and hands the casing back.
 * shortcut: the hole is not carved out of the world; upgrade if a visible bore is wanted.
 * shortcut: the target depth is the basin's centre height ({@code BasinState.Entry} has no top yet).
 */
public class DerrickBaseBlockEntity extends RotationNode implements Part {

    /** What the stack is doing, for the status line. Not synced: read server side on use. */
    public enum Status {
        IDLE, NO_BIT, NO_CASING, TOO_SOFT, BEDROCK, BORING
    }

    @Nullable
    private Structure structure;

    private ItemStack bit = ItemStack.EMPTY;
    private int bored;
    private float fu;
    private Status status = Status.IDLE;

    @Nullable
    private BasinState.Entry basin;
    private boolean basinKnown;
    private int rockIndex = -1;
    private float rock;
    private BlockState rockState = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();

    public DerrickBaseBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.DERRICK_BASE.get(), pos, state);
    }

    // --- Part ------------------------------------------------------------------------------

    @Override
    public String role() {
        return DerrickStructure.BASE;
    }

    @Override
    public boolean connects(Direction face) {
        return face == Direction.UP;
    }

    @Override
    public RoleRule rule() {
        return DerrickStructure.RULE;
    }

    @Nullable
    @Override
    public Structure structure() {
        return structure;
    }

    @Override
    public void setStructure(@Nullable Structure structure) {
        this.structure = structure;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        Multiblocks.load(this);
    }

    @Override
    public void setRemoved() {
        Multiblocks.remove(this);
        super.setRemoved();
    }

    // --- the bit ---------------------------------------------------------------------------

    @Override
    public Su getLoadSu() {
        return bit.isEmpty() ? NO_SU : FTuning.DERRICK_LOAD_SU;
    }

    public boolean insertBit(ItemStack stack) {
        if (!bit.isEmpty() || stack.isEmpty())
            return false;
        bit = stack.split(1);
        loadChanged();
        return true;
    }

    public ItemStack takeBit() {
        ItemStack taken = bit;
        bit = ItemStack.EMPTY;
        loadChanged();
        return taken;
    }

    private void loadChanged() {
        setChanged();
        if (network != null)
            network.recalculate();
    }

    // --- boring ----------------------------------------------------------------------------

    /** Called once per level tick by the structure, server side. */
    public void bore(Level level, List<Part> casings) {
        if (!(level instanceof ServerLevel server))
            return;
        if (!basinKnown || server.getGameTime() % 200 == 0) {
            basin = BasinState.at(server, worldPosition);
            basinKnown = true;
        }
        int target = basin == null ? Integer.MAX_VALUE : Math.max(1, worldPosition.getY() - basin.centre().getY());
        if (bored >= target) {
            complete(server, casings);
            return;
        }
        if (bit.isEmpty()) {
            status = Status.NO_BIT;
            return;
        }
        float rpm = Math.abs(getRpm().value());
        if (rpm <= 0f) {
            status = Status.IDLE;
            return;
        }
        if (bored >= casings.size() * FTuning.DERRICK_CASING_REACH) {
            status = Status.NO_CASING;
            return;
        }
        BlockPos at = worldPosition.below(bored + 1);
        if (!server.isLoaded(at) || at.getY() < server.getMinBuildHeight()) {
            status = Status.BEDROCK;
            return;
        }
        if (rockIndex != bored) {
            rockState = server.getBlockState(at);
            if (rockState.getDestroySpeed(server, at) < 0f) {
                status = Status.BEDROCK;
                return;
            }
            rock = rockHardness(server, at, rockState);
            rockIndex = bored;
        }

        float passes = rpm * FTuning.DERRICK_PASSES_PER_RPM;
        float drive = FTuning.DERRICK_DRIVE_ST.value();
        float absorbed = rpm > FTuning.DERRICK_RATED_RPM
                ? (rpm - FTuning.DERRICK_RATED_RPM) * FTuning.DERRICK_OVERSPEED_ST_PER_RPM : 0f;
        if (DerrickBitItem.hardness(bit) > rock) {
            status = Status.BORING;
            fu += drive / rock * passes * DerrickBitItem.condition(bit);
        } else {
            status = Status.TOO_SOFT;
            absorbed += drive * passes;
        }
        if (absorbed > 0f)
            wearBit(absorbed);

        if (server.getGameTime() % 5 == 0)
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, rockState), worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, 3, 0.25, 0.1, 0.25, 0.05);
        if (fu >= FTuning.DERRICK_FU_PER_BLOCK) {
            fu -= FTuning.DERRICK_FU_PER_BLOCK;
            bored++;
            setChanged();
        }
    }

    private static float rockHardness(Level level, BlockPos at, BlockState state) {
        if (state.isAir() || state.canBeReplaced())
            return 1f;
        var found = Material.find(MaterialTable.get().materials(), state.getBlock().asItem());
        if (found.isPresent())
            return found.get().material().hardness().value();
        return Math.max(1f, state.getDestroySpeed(level, at) * FTuning.DERRICK_ROCK_HARDNESS_PER_DESTROY_TIME);
    }

    /** The bit takes the force that could not go into the rock (digest: wear only by misuse). */
    private void wearBit(float stAbsorbed) {
        float condition = Math.max(FTuning.DERRICK_BIT_CONDITION_FLOOR,
                DerrickBitItem.condition(bit) - stAbsorbed * FTuning.DERRICK_BIT_WEAR_PER_ST);
        bit.set(FDataComponents.TOOL_CONDITION.get(), condition);
        setChanged();
    }

    /** The hole has reached the basin: the casing goes down it, the base becomes the wellhead. */
    private void complete(ServerLevel level, List<Part> casings) {
        for (Part casing : casings) {
            Block.popResource(level, casing.pos(), new ItemStack(FItems.DERRICK_CASING.get()));
            level.removeBlock(casing.pos(), false);
        }
        ItemStack worn = takeBit();
        if (!worn.isEmpty())
            Block.popResource(level, worldPosition, worn);
        int depth = bored;
        BlockPos pos = worldPosition;
        level.setBlock(pos, FBlocks.WELLHEAD.get().defaultBlockState(), 3);
        if (level.getBlockEntity(pos) instanceof io.github.soundgoodizerfan.feedback.machine.derrick.WellheadBlockEntity well)
            well.setDepth(depth);
        level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 1f, 0.5f);
    }

    public Component describe() {
        return Component.translatable("message.feedback.derrick." + status.name().toLowerCase(), bored,
                bit.isEmpty() ? 0 : Math.round(DerrickBitItem.condition(bit) * 100f));
    }

    // --- persistence -----------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Bored", bored);
        tag.putFloat("Fu", fu);
        if (!bit.isEmpty())
            tag.put("Bit", bit.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        bored = tag.getInt("Bored");
        fu = tag.getFloat("Fu");
        bit = tag.contains("Bit") ? ItemStack.parseOptional(registries, tag.getCompound("Bit")) : ItemStack.EMPTY;
    }
}
