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
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.registry.FEntities;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A thrown demolition charge (guns spec 13.4a): the charge item itself in flight, its blast read off what it is packed
 * with by {@link Blast#packed}, the same rule as the placed block. The fuse is a part of the charge's construction (the charge's fuse part):
 * an <em>impact</em> fuse goes off on the first thing it hits; a <em>delay</em> fuse bounces and goes off when
 * its own burn time has run. A casing also throws fragments: everything alive within reach takes
 * damage that grows with the casing metal's hardness and falls with distance.
 */
public class GrenadeEntity extends ThrowableItemProjectile {

    private boolean impact;
    private int fuse = FTuning.DEMO_FUSE_TICKS;

    public GrenadeEntity(EntityType<? extends GrenadeEntity> type, Level level) {
        super(type, level);
    }

    public GrenadeEntity(Level level, LivingEntity thrower, boolean impact, int fuse) {
        super(FEntities.GRENADE.get(), thrower, level);
        this.impact = impact;
        this.fuse = fuse;
    }

    @Override
    protected Item getDefaultItem() {
        return FModular.CHARGE.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server && !impact) {
            if (fuse-- <= 0)
                explode(server);
            else
                server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.2, getZ(), 1, 0.03, 0.03, 0.03, 0);
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!(level() instanceof ServerLevel server))
            return;
        if (impact) {
            explode(server);
            return;
        }
        // A delay grenade rolls on: reflect off the face it struck and lose most of its speed.
        Vec3 v = getDeltaMovement();
        if (hit instanceof BlockHitResult block) {
            var n = block.getDirection();
            setDeltaMovement(n.getAxis() == net.minecraft.core.Direction.Axis.X ? -v.x * 0.4 : v.x * 0.6,
                    n.getAxis() == net.minecraft.core.Direction.Axis.Y ? -v.y * 0.3 : v.y * 0.6,
                    n.getAxis() == net.minecraft.core.Direction.Axis.Z ? -v.z * 0.4 : v.z * 0.6);
        } else if (hit instanceof EntityHitResult)
            setDeltaMovement(v.scale(-0.2));
    }

    private void explode(ServerLevel server) {
        var packed = Blast.packed(getItem());
        discard();
        if (packed.isEmpty() || packed.get().power() < FTuning.DEMO_POWER_MIN) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 12, 0.3, 0.3, 0.3, 0.02);
            server.playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1f, 0.8f);
            return;
        }
        var p = packed.get();
        server.explode(this, getX(), getY(), getZ(), p.power(), p.fire(), Level.ExplosionInteraction.BLOCK);
        Blast.scatter(server, this, getOwner(), position(), p.power(), p);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Impact", impact);
        tag.putInt("Fuse", fuse);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        impact = tag.getBoolean("Impact");
        fuse = tag.contains("Fuse") ? tag.getInt("Fuse") : FTuning.DEMO_FUSE_TICKS;
    }
}
