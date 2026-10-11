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
package io.github.soundgoodizerfan.feedback.machine.drillpress;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.process.Operation;
import io.github.soundgoodizerfan.feedback.process.Removing;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The removal primitive's cheapest, first real test. The bit rotates for as long as the network
 * drives it -- that alone does not cut anything, exactly as a real drill press's spindle can spin
 * all day against a workpiece nobody is feeding in. Cutting only happens on a {@link #push()},
 * fed by hand with a right click.
 * <p>
 * <b>An automatic-feed actuator was cut.</b> A first pass here invented a {@code Piston} block --
 * "the Bellows' own crank-linkage mechanism, generalised" -- meant to bolt onto this and feed it
 * under controller control. The premise was wrong: the Bellows has no crank-linkage of its own to
 * generalise. It consumes reciprocating motion from an ordinary {@code CrankLinkageBlockEntity}
 * placed next to it, the same way every other reciprocating machine does. A network-driven pusher
 * gated by {@code Switchable} is therefore just a Crank Linkage with a Clutch already sitting
 * upstream of it -- both of which already exist -- so the Piston bought no capability a player
 * could not already build, which fails §5's "two answers that cost different things" test on its
 * own premise. Hand feed is the only route until a real second one is designed.
 * <p>
 * Overrun here is deliberately mild: drilling through the far side of a workpiece just finishes
 * the hole (see {@code Removing}'s cascade). The real failure mode is a bound, fast-fed bit --
 * see {@link FTuning#BIT_SNAP_CHANCE} -- which costs the push, not the machine or the workpiece.
 */
public class DrillPressBlockEntity extends RotationNode {

    private ItemStack workpiece = ItemStack.EMPTY;

    public DrillPressBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.DRILL_PRESS.get(), pos, state);
    }

    public ItemStack getWorkpiece() {
        return workpiece;
    }

    public boolean insert(ItemStack stack) {
        if (!workpiece.isEmpty() || stack.isEmpty())
            return false;
        workpiece = stack.split(1);
        sync();
        return true;
    }

    public ItemStack removeWorkpiece() {
        ItemStack taken = workpiece;
        workpiece = ItemStack.EMPTY;
        if (level != null)
            ItemHeat.settle(taken, level);
        sync();
        return taken;
    }

    public boolean isFastFeed() {
        return getBlockState().getValue(DrillPressBlock.FAST);
    }

    @Override
    public Su getLoadSu() {
        return FTuning.DRILL_PRESS_LOAD_SU;
    }

    /**
     * The bit must actually be spinning -- a stalled or unpowered spindle biting into anything is
     * a hazard in reality and a no-op here, the same "isDriving" question {@code
     * CrankLinkageBlockEntity#isDriving} answers for a reciprocating machine, asked of a direct
     * network member instead.
     */
    private boolean isSpinning() {
        return Math.abs(getRpm().value()) > FTuning.STOPPED_RPM_THRESHOLD.value();
    }

    public void push() {
        if (level == null || level.isClientSide || workpiece.isEmpty() || !isSpinning())
            return;

        boolean fast = isFastFeed();
        if (fast && level.random.nextFloat() < FTuning.BIT_SNAP_CHANCE) {
            level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.2f, 0.5f);
            return;
        }

        St driveForce = fast ? FTuning.DRILL_PRESS_ST_FAST : FTuning.DRILL_PRESS_ST_SLOW;
        Removing.Cut cut = Removing.apply(workpiece, driveForce, FTuning.DRILL_BIT_HARDNESS, Operation.DRILL);
        if (!cut.landed())
            return;

        workpiece = cut.result();
        if (level.random.nextFloat() < FTuning.SWARF_DROP_CHANCE)
            dropSwarf();
        sync();
    }

    private void dropSwarf() {
        ItemEntity swarf = new ItemEntity(level,
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.6, worldPosition.getZ() + 0.5,
                new ItemStack(FItems.METAL_SWARF.get()));
        level.addFreshEntity(swarf);
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Workpiece", workpiece.saveOptional(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        workpiece = ItemStack.parseOptional(registries, tag.getCompound("Workpiece"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
