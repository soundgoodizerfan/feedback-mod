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
package io.github.soundgoodizerfan.feedback.vehicle;

import java.util.HashSet;
import java.util.Set;

import org.joml.Vector3d;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.fitting.SensorFitting;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Reads where its vehicle is and how it sits, for a Controller's Input card. It is an unsided
 * {@link SensorFitting} (a node of its own, not mounted on a holder), so the card machinery that
 * links, reads and quantises fittings serves it unchanged. One reading at a time, a discrete
 * choice stepped by use; each has the quantity a comparator checks it against, so altitude is never
 * compared with speed. Readings come from the body's pose and the speed the physics step last
 * saw; there is no wind, so airspeed is speed over the ground.
 * <p>
 * It resolves like any instrument: quantised to its own resolution, a bad sensor costing
 * reproducibility and never changing what the world does.
 */
public class FlightSensorBlockEntity extends BlockEntity implements SensorFitting, BlockEntitySubLevelActor, VehiclePart {

    /** What it reads, with the quantity that reading is. */
    public enum Mode implements StringRepresentable {
        ALTITUDE("altitude", Quantity.ALTITUDE), AIRSPEED("airspeed", Quantity.AIRSPEED),
        PITCH("pitch", Quantity.ATTITUDE), ROLL("roll", Quantity.ATTITUDE);

        private final String key;
        private final Quantity quantity;

        Mode(String key, Quantity quantity) {
            this.key = key;
            this.quantity = quantity;
        }

        @Override
        public String getSerializedName() {
            return key;
        }

        public Quantity quantity() {
            return quantity;
        }
    }

    private Mode mode = Mode.ALTITUDE;
    /** The body's speed as of the last physics step; written there, read on the server tick. */
    private volatile double speed;
    private final Set<DataNodeRef> links = new HashSet<>();

    public FlightSensorBlockEntity(BlockPos pos, BlockState state) {
        super(FVehicles.FLIGHT_SENSOR_ENTITY.get(), pos, state);
    }

    public Mode mode() {
        return mode;
    }

    public void nextMode() {
        mode = Mode.values()[(mode.ordinal() + 1) % Mode.values().length];
        setChanged();
    }

    /** Every physics step: the body's speed, which only the physics thread may ask for. */
    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        speed = handle.getLinearVelocity(new Vector3d()).length();
    }

    /** The true figure for the current mode. */
    private float truth() {
        Direction facing = getBlockState().getValue(FlightSensorBlock.FACING);
        Vector3d front = new Vector3d(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        Vector3d up = new Vector3d(0, 1, 0);
        Vec3 at = Vec3.atCenterOf(worldPosition);
        if (SableCompanion.INSTANCE.getContaining(this) instanceof ServerSubLevel body) {
            at = body.logicalPose().transformPosition(at);
            body.logicalPose().transformNormal(front);
            body.logicalPose().transformNormal(up);
        }
        return switch (mode) {
            case ALTITUDE -> (float) (at.y - (level == null ? 0 : level.getMinBuildHeight()));
            case AIRSPEED -> (float) speed;
            case PITCH -> (float) Math.toDegrees(Math.asin(Math.clamp(front.y, -1, 1)));
            // Roll: how far the sensor's up has tipped sideways of the vertical plane through its front.
            case ROLL -> {
                Vector3d side = front.cross(new Vector3d(0, 1, 0), new Vector3d());
                yield side.lengthSquared() < 1e-6 ? 0f : (float) Math.toDegrees(Math.asin(Math.clamp(up.dot(side.normalize()), -1, 1)));
            }
        };
    }

    // --- Instrument ---------------------------------------------------------------------------

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == mode.quantity();
    }

    @Override
    public float resolution(Quantity quantity) {
        return switch (quantity) {
            case ALTITUDE -> FTuning.FLIGHT_ALTITUDE_RESOLUTION;
            case AIRSPEED -> FTuning.FLIGHT_AIRSPEED_RESOLUTION;
            default -> FTuning.FLIGHT_ATTITUDE_RESOLUTION;
        };
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.flight_sensor");
    }

    @Override
    public Component readValue() {
        return Instruments.signed(this, Component.translatable("feedback.flight_sensor.reading."
                + mode.getSerializedName(), Instruments.format(truth(), resolution(mode.quantity()))));
    }

    @Override
    public float readRaw() {
        return Instruments.quantise(truth(), resolution(mode.quantity()));
    }

    @Override
    public ItemStack getPickItem() {
        return new ItemStack(FVehicles.FLIGHT_SENSOR_ITEM.get());
    }

    // --- DataNode -----------------------------------------------------------------------------

    @Override
    public BlockPos getNodePos() {
        return worldPosition;
    }

    @org.jetbrains.annotations.Nullable
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Mode", mode.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mode = Mode.values()[Math.floorMod(tag.getInt("Mode"), Mode.values().length)];
    }
}
