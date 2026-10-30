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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.combat.tether;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.item.GrappleItem;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The grapple's hook, the second {@link Tether} source (plan 2.5). Flies on a short arc, bites a block
 * or a living thing and makes a tether on the lighter body:
 *
 * <ul>
 * <li><b>Reel by mass.</b> The lighter body moves toward the heavier along the line. A block is
 * infinitely heavy, so the thrower is pulled; a creature is compared with the thrower by
 * {@link BodyMass}, and a lighter one is dragged to the thrower, a heavier one drags the thrower.
 * <li><b>The line has a rating.</b> Its material's yield strength (string is wool's) times
 * {@link ModularTuning#GRAPPLE_RATING_PER_ST} is the load it takes. The load is the pulled body's
 * mass, tripled on a tick the reel stalls (caught on a corner, held by the ground). Over the rating
 * the line gains strain each tick and parts at 1 -- wear by misuse, not instant; a clean pull never
 * strains it.
 * <li><b>Release</b> on reaching the anchor, on sneaking, on using the grapple again, on the
 * line parting, or when either end is gone.
 * </ul>
 *
 * <p>The tether is keyed by this hook's uuid. Server-authoritative; the client only flies and draws it.
 */
public class GrappleHook extends Projectile {

    private static final EntityDataAccessor<Boolean> DATA_ANCHORED = SynchedEntityData.defineId(GrappleHook.class, EntityDataSerializers.BOOLEAN);

    /** The material the line is made of; the grapple's recipe takes string. */
    private static final ResourceLocation ROPE_MATERIAL = Feedback.id("wool");

    /** The creature the hook bit, or -1 for a block. */
    private int anchorEntity = -1;
    private Vec3 anchorPoint = Vec3.ZERO;
    /** The pulled body, -1 before the hook bites. */
    private int heldId = -1;
    private int reelTicks;
    private float lastDistance;
    private float strain;
    private float rating;

    public GrappleHook(EntityType<? extends GrappleHook> type, Level level) {
        super(type, level);
    }

    public GrappleHook(Level level, LivingEntity thrower) {
        super(FEntities.GRAPPLE_HOOK.get(), level);
        setOwner(thrower);
        setPos(thrower.getX(), thrower.getEyeY() - 0.1, thrower.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ANCHORED, false);
    }

    public boolean anchored() {
        return entityData.get(DATA_ANCHORED);
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }

    /** The load the line takes, in Mu. */
    private static float lineRating() {
        Material m = MaterialTable.get().materials().get(ROPE_MATERIAL);
        float st = m != null ? m.mechanical().yieldStrength().map(s -> s.value()).orElse(ModularTuning.GRAPPLE_FALLBACK_ST)
                : ModularTuning.GRAPPLE_FALLBACK_ST;
        return st * ModularTuning.GRAPPLE_RATING_PER_ST;
    }

    // --- flight ---------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (anchored()) {
            if (!level().isClientSide)
                reel();
            return;
        }
        Entity owner = getOwner();
        if (!level().isClientSide) {
            if (owner == null || !owner.isAlive() || owner.distanceTo(this) > ModularTuning.GRAPPLE_ROPE) {
                discard();
                return;
            }
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                onHit(hit);
                if (anchored() || isRemoved())
                    return;
            }
        }
        setPos(position().add(getDeltaMovement()));
        setDeltaMovement(getDeltaMovement().scale(0.99).add(0, -ModularTuning.GRAPPLE_GRAVITY, 0));
        ProjectileUtil.rotateTowardsMovement(this, 0.5f);
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        bite(null, hit.getLocation(), hit.getBlockPos());
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (hit.getEntity() instanceof LivingEntity target)
            bite(target, hit.getLocation(), null);
        else
            discard();
    }

    /** Fixes the hook and puts the tether on whichever body is lighter. */
    private void bite(LivingEntity target, Vec3 at, BlockPos block) {
        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }
        boolean dragTarget = target != null && BodyMass.of(target) < BodyMass.of(owner);
        LivingEntity held = dragTarget ? target : owner;
        Tether.Anchor anchor = dragTarget ? new Tether.Anchor.OnEntity(owner.getId())
                : target != null ? new Tether.Anchor.OnEntity(target.getId()) : new Tether.Anchor.OnBlock(block);
        anchorEntity = target != null ? target.getId() : -1;
        anchorPoint = at;
        heldId = held.getId();
        rating = lineRating();
        lastDistance = (float) held.position().distanceTo(anchorPoint());
        Tethers.add(held, new Tether(getUUID(), anchor, ModularTuning.GRAPPLE_ROPE, reelRate(), 1f,
                new Tether.Release(ModularTuning.GRAPPLE_STOP_DISTANCE, ModularTuning.GRAPPLE_MAX_TICKS)));
        setDeltaMovement(Vec3.ZERO);
        setPos(at);
        entityData.set(DATA_ANCHORED, true);
        playSound(SoundEvents.CHAIN_HIT, 1f, 1f);
    }

    /** Blocks per tick the line draws, the base figure scaled by the thrower's bought reel nodes. */
    private float reelRate() {
        return ModularTuning.GRAPPLE_REEL * (getOwner() instanceof LivingEntity o
                ? io.github.soundgoodizerfan.feedback.combat.skill.Skills.reelScale(o) : 1f);
    }

    /** Where the line is fixed: the hit point, or the middle of the creature it bit. */
    private Vec3 anchorPoint() {
        if (anchorEntity >= 0) {
            Entity e = level().getEntity(anchorEntity);
            if (e != null)
                return e.position().add(0, e.getBbHeight() * 0.5, 0);
        }
        return anchorPoint;
    }

    // --- the reel -------------------------------------------------------------------------------

    private void reel() {
        Entity owner = getOwner();
        Entity held = level().getEntity(heldId);
        if (owner == null || !owner.isAlive() || !(held instanceof LivingEntity living) || !living.isAlive()
                || anchorEntity >= 0 && level().getEntity(anchorEntity) == null) {
            discard();
            return;
        }
        Vec3 to = anchorPoint();
        setPos(to);
        Vec3 offset = to.subtract(living.position().add(0, living.getBbHeight() * 0.5, 0));
        float distance = (float) offset.length();
        if (++reelTicks > ModularTuning.GRAPPLE_MAX_TICKS || distance <= ModularTuning.GRAPPLE_STOP_DISTANCE
                || reelTicks > ModularTuning.GRAPPLE_SNEAK_GRACE && owner.isShiftKeyDown()) {
            // A staggered body hauled all the way in is the Throw's moment (Throwing checks skill and stagger).
            if (living != owner && owner instanceof LivingEntity thrower && distance <= ModularTuning.GRAPPLE_STOP_DISTANCE)
                io.github.soundgoodizerfan.feedback.combat.grab.Throwing.attempt(thrower, living);
            discard();
            return;
        }
        boolean stalled = reelTicks > 2 && lastDistance - distance < reelRate() * ModularTuning.GRAPPLE_STALL_FRACTION;
        lastDistance = distance;
        float load = BodyMass.of(living) * (stalled ? ModularTuning.GRAPPLE_STALL_LOAD : 1f);
        if (load > rating) {
            strain += (load / rating - 1f) * ModularTuning.GRAPPLE_STRAIN_PER_TICK;
            if (strain >= 1f) {
                playSound(SoundEvents.LEASH_KNOT_BREAK, 1f, 1f);
                if (owner instanceof Player p)
                    GrappleItem.wear(p, ModularTuning.GRAPPLE_BREAK_WEAR);
                discard();
                return;
            }
        }
        float speed = reelRate();
        if (living != owner)
            speed *= 1f - (float) living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        // A crouched player with footing holds back a share of the pull (armour mass adds to it).
        if (living instanceof Player held2 && living != owner) {
            float hold = io.github.soundgoodizerfan.feedback.combat.skill.Skills.tetherPullScale(held2);
            if (hold < 1f && held2 instanceof net.minecraft.server.level.ServerPlayer sp)
                io.github.soundgoodizerfan.feedback.combat.skill.SkillEarning.heldPull(sp, owner.getUUID());
            speed *= hold;
        }
        Vec3 pull = offset.normalize().scale(speed);
        living.setDeltaMovement(pull.x, pull.y + ModularTuning.REEL_LIFT * speed, pull.z);
        living.fallDistance = 0f;
        living.hurtMarked = true;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && heldId >= 0 && level().getEntity(heldId) instanceof LivingEntity held)
            Tethers.remove(held, getUUID());
        super.remove(reason);
    }

    /** Lets the line go. */
    public void release() {
        discard();
    }

    /** The hook {@code owner} has out, or null. */
    public static GrappleHook of(Level level, LivingEntity owner) {
        for (GrappleHook h : level.getEntitiesOfClass(GrappleHook.class, owner.getBoundingBox().inflate(ModularTuning.GRAPPLE_ROPE + 8)))
            if (h.getOwner() == owner)
                return h;
        return null;
    }
}
