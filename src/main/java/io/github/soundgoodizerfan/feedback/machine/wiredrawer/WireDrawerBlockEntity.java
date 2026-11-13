// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.wiredrawer;

import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.machine.ShopMachineBlockEntity;
import io.github.soundgoodizerfan.feedback.process.Deforming;
import io.github.soundgoodizerfan.feedback.process.Operation;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Wire Drawer: the hammer's own model, minus the hammer -- one draw per {@link #strokesDue},
 * through the exact same {@link Deforming#apply} every hand and machine tool in the mod already
 * shares. Pulling stock through a die is tension, not a blow, so it always applies
 * {@link Operation#DRAW} -- see that enum's own doc for why a hammer's {@code BLOW} would collide
 * with this on the same input item.
 *
 * <h2>Draw speed is the block's one setting</h2>
 * Fast clears a higher hardness -- {@link MachineTuning#WIRE_DRAWER_ST_FAST} reaches steel, {@link
 * MachineTuning#WIRE_DRAWER_ST_SLOW} does not, a genuine hard gate rather than patience -- but risks the
 * wire snapping on each draw that lands. A draw that cannot move the metal (too hard for the
 * setting, nothing left to draw) is absorbed by the die and wears the machine, the Hammer's rule.
 * Slow is always safe and is what a plain copper draw actually wants.
 */
public class WireDrawerBlockEntity extends ShopMachineBlockEntity {

    private static final Operation OPERATION = Operation.DRAW;

    public WireDrawerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.WIRE_DRAWER.get(), pos, state);
    }

    public boolean isFastDraw() {
        return getBlockState().getValue(WireDrawerBlock.FAST);
    }

    @Override
    public Su getLoadSu() {
        return MachineTuning.WIRE_DRAWER_LOAD_SU;
    }

    public void tickServer() {
        if (level == null || level.isClientSide)
            return;
        boolean fast = isFastDraw();
        boolean changed = false;
        for (int due = strokesDue(); due > 0 && !workpiece.isEmpty(); due--) {
            St strength = new St((fast ? MachineTuning.WIRE_DRAWER_ST_FAST : MachineTuning.WIRE_DRAWER_ST_SLOW).value() * condition);
            Deforming.Blow blow = Deforming.apply(workpiece, strength, level, OPERATION);
            if (!blow.landed()) {
                wear(strength.value());
                continue;
            }
            // Only a draw that actually pulls metal through the die can snap it.
            if (fast && level.random.nextFloat() < MachineTuning.WIRE_SNAP_CHANCE) {
                workpiece = ItemStack.EMPTY;
                level.playSound(null, worldPosition, SoundEvents.TRIPWIRE_DETACH, SoundSource.BLOCKS, 0.6f, 0.7f);
            } else {
                workpiece = blow.result();
            }
            changed = true;
        }
        if (changed)
            sync();
    }
}
