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
package io.github.soundgoodizerfan.feedback.control.timer;

import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TimerBlockEntity extends BlockEntity {

    /**
     * Which wind is selected, as an index into {@link FTuning#TIMER_SETTINGS}.
     * <p>
     * A short list of positions rather than a number of ticks, per philosophy 3 — a dial with 300
     * positions would be a spreadsheet. It matters less than usual here, because the thing this
     * sets has no correct value to find: the run it is timing is not repeatable enough for one to
     * exist. But a device with a scalar on it teaches players to look for one, and that habit is
     * worth not teaching.
     */
    private int setting;

    private int ticksRemaining;

    public TimerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.TIMER.get(), pos, state);
    }

    public void toggle() {
        if (isRunning())
            stop();
        else
            start();
    }

    public void cycleSetting(Player player) {
        setting = (setting + 1) % FTuning.TIMER_SETTINGS.size();
        setChanged();
        int ticks = FTuning.TIMER_SETTINGS.get(setting);
        player.displayClientMessage(Component.translatable("feedback.timer.setting",
                String.format("%.0f", ticks / 20f)), true);
    }

    private void start() {
        ticksRemaining = FTuning.TIMER_SETTINGS.get(setting);
        setRunning(true);
        switchAdjacent(true);
    }

    private void stop() {
        ticksRemaining = 0;
        setRunning(false);
        switchAdjacent(false);
    }

    public void tickServer() {
        if (ticksRemaining <= 0)
            return;
        if (--ticksRemaining <= 0) {
            stop();
            if (level != null)
                level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_HAT.value(),
                        SoundSource.BLOCKS, 0.5f, 1.4f);
        }
    }

    public boolean isRunning() {
        return getBlockState().getValue(TimerBlock.RUNNING);
    }

    private void setRunning(boolean running) {
        if (level != null && isRunning() != running)
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(TimerBlock.RUNNING, running));
    }

    /**
     * Throw every switchable thing touching this block.
     * <p>
     * Direct contact rather than any kind of signal, because beat 1 has no data links and should
     * not pretend to. It also keeps the wiring visible: a player can see what a timer controls by
     * looking at what it is bolted to.
     */
    private void switchAdjacent(boolean engaged) {
        for (Switchable target : adjacentSwitchables())
            target.setEngaged(engaged);
    }

    private List<Switchable> adjacentSwitchables() {
        List<Switchable> found = new ArrayList<>(6);
        if (level == null)
            return found;
        for (Direction face : Direction.values()) {
            BlockEntity be = level.getBlockEntity(worldPosition.relative(face));
            if (be instanceof Switchable switchable)
                found.add(switchable);
        }
        return found;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Setting", setting);
        tag.putInt("TicksRemaining", ticksRemaining);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        setting = tag.getInt("Setting");
        ticksRemaining = tag.getInt("TicksRemaining");
    }
}
