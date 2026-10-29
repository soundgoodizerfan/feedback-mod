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
package io.github.soundgoodizerfan.feedback.vehicle.seat;

import io.github.soundgoodizerfan.feedback.control.ChannelSource;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

/**
 * The driver's side of a vehicle's controls ({@code docs/feedback_vehicles_spec.md} section 5,
 * <em>Controls</em>): the whole keyboard, as readings. While someone sits here every key they press
 * is sent here instead of to the game, and each key held is a reading that ramps from 0 to 1 over
 * {@link FTuning#SEAT_RAMP_TICKS} and falls back the same way once let go -- tap for a little,
 * hold for full. A Sensor card on a controller reads one key ({@link ChannelSource}, the key's GLFW
 * code as the channel). The seat links to nothing and drives nothing: seat, then controller, then
 * parts, always.
 * <p>
 * Taken from Create Aeronautics' Linked Typewriter (Simulated Project, MIT; design only, nothing
 * adapted): whole-keyboard capture while in use, press and release sent to the server, mapped keys
 * swallowed so they do not also play the game, Escape to let go. Not taken: its key-to-frequency
 * table and wireless redstone -- here a key is a reading and the controller is what binds it.
 */
public class DriverSeatBlockEntity extends BlockEntity implements DataNode, ChannelSource, io.github.soundgoodizerfan.feedback.vehicle.VehiclePart {

    private final IntOpenHashSet held = new IntOpenHashSet();
    private final Int2FloatOpenHashMap levels = new Int2FloatOpenHashMap();
    private final Set<DataNodeRef> links = new HashSet<>();

    public DriverSeatBlockEntity(BlockPos pos, BlockState state) {
        super(FVehicles.DRIVER_SEAT_ENTITY.get(), pos, state);
    }

    /** A key went down or came up under the player sitting here. */
    public void key(int key, boolean down) {
        if (down)
            held.add(key);
        else
            held.remove(key);
    }

    /** Server tick: every key ramps toward 1 while held and back to 0 after; nobody seated holds nothing. */
    static void serverTick(Level level, BlockPos pos, BlockState state, DriverSeatBlockEntity be) {
        if (be.driver() == null)
            be.held.clear();
        float step = 1f / FTuning.SEAT_RAMP_TICKS;
        for (int key : be.held)
            be.levels.put(key, Math.min(1, be.levels.get(key) + step));
        for (var it = be.levels.int2FloatEntrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            if (be.held.contains(e.getIntKey()))
                continue;
            float next = e.getFloatValue() - step;
            if (next <= 0)
                it.remove();
            else
                e.setValue(next);
        }
    }

    /** Whoever is sitting here, if anyone. */
    @Nullable
    public Player driver() {
        if (level == null)
            return null;
        for (DriverSeatEntity seat : level.getEntitiesOfClass(DriverSeatEntity.class, new net.minecraft.world.phys.AABB(worldPosition)))
            if (seat.getFirstPassenger() instanceof Player player)
                return player;
        return null;
    }

    @Override
    public float read(int channel) {
        return levels.get(channel);
    }

    /** W: the key a freshly linked card reads, since a throttle is what most cards want first. */
    @Override
    public int defaultChannel() {
        return GLFW.GLFW_KEY_W;
    }

    // --- DataNode -----------------------------------------------------------------------------

    @Override
    public boolean acceptsLinks() {
        return false;
    }

    @Override
    public BlockPos getNodePos() {
        return worldPosition;
    }

    @Nullable
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

    @Override
    public Set<DataNodeRef> getNodeLinks() {
        return links;
    }
}
