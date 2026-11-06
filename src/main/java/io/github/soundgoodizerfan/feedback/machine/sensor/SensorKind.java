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
package io.github.soundgoodizerfan.feedback.machine.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * What a sensor block looks at: the face it points at. Sensing only, nothing here controls anything; a controller card links the block
 * and reads {@link #read}. Every reading is a fraction from 0 to 1 and is quantised to a few bands, the cheap sensor's coarseness.
 */
public enum SensorKind {

    /** A crop's growth: the block's {@code age} state over its largest age. Reads by property, so any crop with an age works. */
    RIPENESS("ripeness_probe") {
        @Override
        public float read(Level level, BlockPos pos, Direction face, SensorBlockEntity self) {
            BlockState state = level.getBlockState(pos.relative(face));
            for (var property : state.getProperties())
                if (property instanceof IntegerProperty age && property.getName().equals("age")) {
                    int max = age.getPossibleValues().stream().max(Integer::compare).orElse(1);
                    return band(state.getValue(age) / (float) max, 4);
                }
            return 0f;
        }
    },
    /** Light at the face: the brighter of block light and daylight. Reads its own lamp's light too, which is the trap. */
    PHOTOCELL("photocell") {
        @Override
        public float read(Level level, BlockPos pos, Direction face, SensorBlockEntity self) {
            BlockPos at = pos.relative(face);
            int sky = Math.max(0, level.getBrightness(LightLayer.SKY, at) - level.getSkyDarken());
            int block = level.getBrightness(LightLayer.BLOCK, at);
            return band(Math.max(sky, block) / 15f, 5);
        }
    },
    /** A creature or a dropped item standing in the face's block. */
    PRESENCE("presence_plate") {
        @Override
        public float read(Level level, BlockPos pos, Direction face, SensorBlockEntity self) {
            AABB box = new AABB(pos.relative(face));
            return level.getEntities((net.minecraft.world.entity.Entity) null, box,
                    e -> e instanceof net.minecraft.world.entity.LivingEntity || e instanceof net.minecraft.world.entity.item.ItemEntity).isEmpty() ? 0f : 1f;
        }
    },
    /** How full the inventory or tank at the face is. */
    FILL("fill_gauge") {
        @Override
        public float read(Level level, BlockPos pos, Direction face, SensorBlockEntity self) {
            BlockPos at = pos.relative(face);
            var items = level.getCapability(Capabilities.ItemHandler.BLOCK, at, face.getOpposite());
            if (items != null && items.getSlots() > 0) {
                float filled = 0f;
                for (int i = 0; i < items.getSlots(); i++) {
                    var stack = items.getStackInSlot(i);
                    if (!stack.isEmpty())
                        filled += stack.getCount() / (float) Math.min(items.getSlotLimit(i), stack.getMaxStackSize());
                }
                return band(filled / items.getSlots(), 8);
            }
            var fluids = level.getCapability(Capabilities.FluidHandler.BLOCK, at, face.getOpposite());
            if (fluids != null && fluids.getTanks() > 0) {
                float have = 0f, cap = 0f;
                for (int i = 0; i < fluids.getTanks(); i++) {
                    have += fluids.getFluidInTank(i).getAmount();
                    cap += fluids.getTankCapacity(i);
                }
                return cap <= 0f ? 0f : band(have / cap, 8);
            }
            return 0f;
        }
    },
    /** A change in the face block's state: high for a short time after it, and a tick late. The observer's role. */
    TELLTALE("tell_tale") {
        @Override
        public float read(Level level, BlockPos pos, Direction face, SensorBlockEntity self) {
            return self != null && self.pulse() > 0 ? 1f : 0f;
        }
    };

    private final String id;

    SensorKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public abstract float read(Level level, BlockPos pos, Direction face, SensorBlockEntity self);

    /** Rounds a 0-1 fraction to {@code steps} equal bands. */
    static float band(float fraction, int steps) {
        return Math.round(Math.max(0f, Math.min(1f, fraction)) * steps) / (float) steps;
    }
}
