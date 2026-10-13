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
package io.github.soundgoodizerfan.feedback.machine.valve;

import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import java.util.Set;
import java.util.Collections;
import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.core.unit.Mb;
import io.github.soundgoodizerfan.feedback.machine.bellows.Blown;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A gate on a {@link Blown} line. No new mechanic -- {@link Switchable} and {@code Blown} already
 * exist for the Bellows/Firebox/Pressure Vessel chain, and a Valve is simply a block that sits
 * between two of them and answers both. Engaged state lives on the blockstate, the same choice
 * {@code DamperBlockEntity} already makes -- nothing here needs to be synced by hand.
 *
 * <h2>Air, not the switch, is what it moves</h2>
 * Open, it forwards whatever is blown into it to whatever {@code Blown} sits on its output face.
 * Closed, the air is simply lost -- the same "anything over the buffer is lost to the room" rule
 * {@link Blown#addAir} already states, applied one block earlier. Nothing here decides what the
 * air is for; that is entirely the far end's business, the same separation the Bellows keeps.
 *
 * <h2>Steam is the same valve</h2>
 * The roadmap's "steam valve" is not a second block. A valve is a cutoff on a line, and a fluid
 * line is the same shape as an air line with a different handle: so this one block also gates a
 * {@link Capabilities.FluidHandler} line, with no identity check on what flows through it. It
 * holds nothing -- it is not a tank -- so a neighbour that pulls from its output face is handed
 * whatever the block behind its input face offers, while open, and nothing while closed. That is
 * exactly how a {@code SteamEngineBlockEntity} already reaches a {@code BoilerBlockEntity}, so
 * putting a valve between them needs nothing from either. Flow is one way, input to output, like
 * {@link Blown}; {@link #getFluidLine} says which face answers to what.
 */
public class ValveBlockEntity extends BlockEntity implements Blown, Switchable, DataNode {

    /** Set while forwarding, so valves facing round in a loop lose the air rather than recurse forever. */
    private boolean forwarding;

    public ValveBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.VALVE.get(), pos, state);
    }

    @Override
    public void addAir(Mb air) {
        if (level == null || forwarding || !isEngaged())
            return;
        if (level.getBlockEntity(worldPosition.relative(getBlockState().getValue(ValveBlock.FACING)))
                instanceof Blown blown) {
            forwarding = true;
            try {
                blown.addAir(air);
            } finally {
                forwarding = false;
            }
        }
    }

    // --- fluids -------------------------------------------------------------------------------

    /**
     * What the line exposes on {@code side}: the output face is the pull side (drain from whatever
     * is behind the input face), the input face is the push side (fill into whatever is in front
     * of the output face), and the four others are walls.
     */
    @Nullable
    public IFluidHandler getFluidLine(@Nullable Direction side) {
        Direction out = getBlockState().getValue(ValveBlock.FACING);
        if (side == out)
            return pull;
        return side == out.getOpposite() ? push : null;
    }

    /** The neighbour {@code face} of this valve, as seen from the valve; null if it has none. */
    @Nullable
    private IFluidHandler beyond(Direction face) {
        return level == null ? null
                : level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(face),
                        face.getOpposite());
    }

    private boolean flowing() {
        return level != null && !forwarding && isEngaged();
    }

    /** Output face. A pull is forwarded to the input side's handler and nothing is ever stored. */
    private final IFluidHandler pull = new Line() {
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            IFluidHandler upstream = flowing() ? beyond(getBlockState().getValue(ValveBlock.FACING).getOpposite()) : null;
            return upstream == null ? FluidStack.EMPTY : through(() -> upstream.drain(resource, action));
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            IFluidHandler upstream = flowing() ? beyond(getBlockState().getValue(ValveBlock.FACING).getOpposite()) : null;
            return upstream == null ? FluidStack.EMPTY : through(() -> upstream.drain(maxDrain, action));
        }
    };

    /** Input face. A fill is forwarded to the output side's handler and nothing is ever stored. */
    private final IFluidHandler push = new Line() {
        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!flowing())
                return 0;
            IFluidHandler downstream = beyond(getBlockState().getValue(ValveBlock.FACING));
            if (downstream == null)
                return 0;
            forwarding = true;
            try {
                return downstream.fill(resource, action);
            } finally {
                forwarding = false;
            }
        }
    };

    /** Runs a forwarded drain with {@link #forwarding} set, so valves round in a loop lose rather than recurse. */
    private FluidStack through(java.util.function.Supplier<FluidStack> drain) {
        forwarding = true;
        try {
            return drain.get();
        } finally {
            forwarding = false;
        }
    }

    /** A handler with no tank in it: every method refuses unless a subclass overrides it. */
    private abstract static class Line implements IFluidHandler {
        @Override
        public int getTanks() {
            return 0;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    // --- DataNode: so a Punch Card actuator can target it ------------------------------------

    @Override
    public BlockPos getNodePos() {
        return worldPosition;
    }

    @Override
    public Direction getNodeSide() {
        return null;
    }

    @Override
    public Level getNodeLevel() {
        return level;
    }

    @Override
    public BlockEntity getNodeOwner() {
        return this;
    }

    /** Targeted by a card, never linked -- see {@link DataNode#acceptsLinks}. */
    @Override
    public boolean acceptsLinks() {
        return false;
    }

    @Override
    public Set<DataNodeRef> getNodeLinks() {
        return Collections.emptySet();
    }

    // --- Switchable ---------------------------------------------------------------------------

    @Override
    public boolean isEngaged() {
        return getBlockState().getValue(ValveBlock.OPEN);
    }

    @Override
    public void setEngaged(boolean engaged) {
        if (level == null || level.isClientSide || isEngaged() == engaged)
            return;
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(ValveBlock.OPEN, engaged));
        level.playSound(null, worldPosition,
                engaged ? SoundEvents.WOODEN_TRAPDOOR_OPEN : SoundEvents.WOODEN_TRAPDOOR_CLOSE,
                SoundSource.BLOCKS, 0.4f, 1.0f);
    }
}
