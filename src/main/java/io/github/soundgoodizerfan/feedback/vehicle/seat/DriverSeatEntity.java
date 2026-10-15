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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * What a driver actually sits on: an invisible entity in the seat block, Create's seat shape (MIT,
 * design only). It lives in the vehicle's plot -- {@code sable:retain_in_sub_level} lists it, as
 * it lists Create's -- so Sable carries the rider with the body. Gone once nobody sits on it or its
 * block is gone.
 */
public class DriverSeatEntity extends Entity {

    public DriverSeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override
    public void tick() {
        if (level().isClientSide)
            return;
        if (!isVehicle() || !(level().getBlockState(blockPosition()).getBlock() instanceof DriverSeatBlock))
            discard();
    }

    @Override
    public void setDeltaMovement(Vec3 motion) {
    }

    @Override
    protected void positionRider(Entity rider, Entity.MoveFunction move) {
        if (!hasPassenger(rider))
            return;
        double offset = getPassengerRidingPosition(rider).y - rider.getVehicleAttachmentPoint(this).y;
        move.accept(rider, getX(), offset + 1.0 / 16, getZ());
    }

    @Override
    public void onPassengerTurned(Entity rider) {
        rider.setYHeadRot(rider.getYRot());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity rider) {
        return super.getDismountLocationForPassenger(rider).add(0, 0.5, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
