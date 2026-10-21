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
package io.github.soundgoodizerfan.feedback.modular;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import io.github.soundgoodizerfan.feedback.registry.FEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A modular weapon in flight: the hatchet, and every trident-shaped weapon (javelin, harpoon,
 * pitchfork). It is a vanilla {@link AbstractArrow} the way Spartan Weaponry's throwing weapon is,
 * so flight, drag, sticking in a block, shaking and being picked up by touch are vanilla's own,
 * and it carries the one stack it was thrown as. What is its own:
 *
 * <ul>
 * <li><b>The blow</b> is the item's baked damage, grown by {@code per_block} for each block flown
 * (to {@code max_mult} times) and scaled by how hard the throw was (effort). A weapon that
 * <i>tumbles</i> (the hatchet) turns end over end at a fixed rate, so it lands head-first only at
 * some distances and side-on at the rest, dealing {@link ModularTuning#TUMBLE_FLOOR} of a head-first
 * blow when side-on; one that flies true (a javelin) has no such window.
 * <li><b>A blow that does not land</b> (blocked, immune) is not spent: the weapon bounces off and
 * keeps flying. One that lands is spent, and falls, returns or reels.
 * <li><b>Returning</b>: with a return rate above 0 it comes back to the thrower after it lands, as
 * vanilla's Loyalty does (damped steer to the thrower's eye, never a straight teleport, and
 * still falling under gravity so it arcs if it is on a rope; a Loyalty trident defies gravity), faster
 * for a light piece, and for a roped one {@link ModularTuning#RETURN_UNATTACHED_MULT} times as fast when it struck nothing.
 * <li><b>A rope</b> (the harpoon) limits how far it can fly, is drawn from the thrower's hand to it,
 * and when it strikes a living thing the weapon stays in it and reels it in: the target is dragged
 * toward the thrower until it is close, then let go, and the weapon comes back.
 * <li><b>Wear</b>: the stack is worn once at the throw, and again if it strikes a block harder than
 * its edge takes ({@link ModularStacks#misuse}); force that cannot go into the work goes into the tool.
 * </ul>
 *
 * <p>Design from Spartan Weaponry's {@code ThrowingWeaponEntity} (Apache-2.0, read at 1.20.1): the
 * arrow base, the hit-on-success-only rule and the bounce, the steered return, the renderer's
 * velocity-aligned pose. Not taken: its enchantment-keyed behaviours, its random crit, its
 * per-item damage constants, its ammo stacks. See {@code THIRD-PARTY-LICENSES.md}.
 */
public class ThrownWeapon extends AbstractArrow {

    /**
     * What a throw was thrown with, all of it derived from the baked design at the moment of the
     * throw ({@link Throwing}).
     *
     * @param damage   the blow at full distance-zero effort, already scaled by effort
     * @param perBlock damage gained per block flown
     * @param maxMult  cap on the distance bonus, as a multiple of {@code damage}
     * @param returns  loyalty-style return rate; 0 never returns
     * @param rope     the line's length in blocks; 0 for none
     * @param pull     reel speed in blocks per tick, for a weapon with a rope
     * @param waterMult how much harder the reel pulls in water
     * @param tumble   turns end over end rather than flying true
     */
    public record Flight(float damage, float perBlock, float maxMult, float returns, float rope, float pull,
                         float waterMult, boolean tumble) {
        static final Flight NONE = new Flight(0f, 0f, 1f, 0f, 0f, 0f, 1f, false);
    }

    private static final EntityDataAccessor<ItemStack> DATA_STACK = SynchedEntityData.defineId(ThrownWeapon.class, EntityDataSerializers.ITEM_STACK);
    /** The living thing the weapon is stuck in and reeling, or -1. */
    private static final EntityDataAccessor<Integer> DATA_HOST = SynchedEntityData.defineId(ThrownWeapon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_RETURNING = SynchedEntityData.defineId(ThrownWeapon.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TUMBLE = SynchedEntityData.defineId(ThrownWeapon.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_ROPE = SynchedEntityData.defineId(ThrownWeapon.class, EntityDataSerializers.FLOAT);
    /** The turn, in degrees, a tumbling weapon froze at when it stuck in a block. */
    private static final EntityDataAccessor<Float> DATA_SPIN = SynchedEntityData.defineId(ThrownWeapon.class, EntityDataSerializers.FLOAT);

    private Flight flight = Flight.NONE;
    private Vec3 origin;
    /** After a landed blow, nothing more is hit. */
    private boolean spent;
    private int reelTicks;
    private int stuckTicks;
    private int returnTicks;
    /** Which way the block it stuck in faces, so it can be lifted out of it when it lets go. */
    private Vec3 stuckNormal = new Vec3(0, 1, 0);
    private final IntOpenHashSet struck = new IntOpenHashSet();

    public ThrownWeapon(EntityType<? extends ThrownWeapon> type, Level level) {
        super(type, level);
    }

    public ThrownWeapon(Level level, LivingEntity thrower, ItemStack stack, Flight flight) {
        super(FEntities.THROWN_WEAPON.get(), thrower, level, stack, null);
        this.flight = flight;
        this.origin = position();
        entityData.set(DATA_STACK, stack.copy());
        entityData.set(DATA_ROPE, flight.rope());
        entityData.set(DATA_TUMBLE, flight.tumble());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STACK, ItemStack.EMPTY);
        builder.define(DATA_HOST, -1);
        builder.define(DATA_RETURNING, false);
        builder.define(DATA_TUMBLE, false);
        builder.define(DATA_ROPE, 0f);
        builder.define(DATA_SPIN, 0f);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.STICK);
    }

    @Override
    public ItemStack getWeaponItem() {
        return getPickupItemStackOrigin();
    }

    /** The thrown stack, as the client draws it. */
    public ItemStack stack() {
        return entityData.get(DATA_STACK);
    }

    public boolean tumbles() {
        return entityData.get(DATA_TUMBLE);
    }

    /** The line's length in blocks, 0 for a weapon with no rope. */
    public float ropeLength() {
        return entityData.get(DATA_ROPE);
    }

    public boolean isReturning() {
        return entityData.get(DATA_RETURNING);
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    protected float getWaterInertia() {
        return tumbles() ? ModularTuning.THROW_WATER_TUMBLE : ModularTuning.THROW_WATER_TRUE;
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }

    // --- the turn of a tumbling weapon ----------------------------------------------------------

    /** How far a tumbling weapon has turned, in degrees, {@code partial} ticks into this one; 0 for one that flies true. */
    public float spin(float partial) {
        if (!tumbles())
            return 0f;
        if (inGround)
            return entityData.get(DATA_SPIN);
        return (tickCount + partial) * ModularTuning.THROW_SPIN_DEG;
    }

    /** The share of a blow a tumbling weapon deals at a turn: full head-first, the floor side-on. */
    private float headFirst(float degrees) {
        if (!tumbles())
            return 1f;
        float c = Mth.cos(degrees * Mth.DEG_TO_RAD);
        return Mth.lerp(Math.max(0f, c), ModularTuning.TUMBLE_FLOOR, 1f);
    }

    // --- tick -----------------------------------------------------------------------------------

    @Override
    public void tick() {
        if (origin == null)
            origin = position();
        Entity owner = getOwner();
        Entity host = host();
        if (host != null) {
            if (!level().isClientSide)
                reel(owner, host);
            host = host();
            if (host != null) {
                rideHost(host);
                return;
            }
        } else if (entityData.get(DATA_HOST) != -1 && !level().isClientSide) {
            // What it was in is gone (killed, unloaded): let go and come back.
            detach();
            startReturn();
        }
        if (isReturning()) {
            if (!steer(owner))
                return;
            if (flight.rope() > 0f) {
                glide();
                return;
            }
        } else if (!level().isClientSide) {
            if (inGround && flight.returns() > 0f && ++stuckTicks > ModularTuning.THROW_STUCK_BEFORE_RETURN)
                startReturn();
            else if (flight.rope() > 0f && owner != null && owner.distanceTo(this) > flight.rope())
                startReturn();
        }
        super.tick();
    }

    /** The target the weapon is in, as this side sees it. */
    private Entity host() {
        int id = entityData.get(DATA_HOST);
        return id < 0 ? null : level().getEntity(id);
    }

    /** Held in a target: the weapon goes where it goes, and is not a thing that hits or is hit. */
    private void rideHost(Entity host) {
        setNoPhysics(true);
        inGround = false;
        float yaw = getYRot();
        float pitch = getXRot();
        Vec3 anchor = host.position().add(0, host.getBbHeight() * 0.6, 0);
        setDeltaMovement(anchor.subtract(position()));
        super.tick();
        setYRot(yaw);
        setXRot(pitch);
        yRotO = yaw;
        xRotO = pitch;
    }

    private void reel(Entity owner, Entity host) {
        if (owner == null || !owner.isAlive() || !host.isAlive() || host.isRemoved()
                || ++reelTicks > ModularTuning.REEL_MAX_TICKS || owner.distanceTo(host) <= ModularTuning.REEL_STOP_DISTANCE) {
            detach();
            startReturn();
            return;
        }
        float speed = flight.pull();
        if (host.isInWater() || isInWater())
            speed *= flight.waterMult();
        if (host instanceof LivingEntity living)
            speed *= 1f - (float) living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        Vec3 toward = owner.position().subtract(host.position()).normalize().scale(speed);
        host.setDeltaMovement(toward.x, toward.y + ModularTuning.REEL_LIFT * speed, toward.z);
        host.fallDistance = 0f;
        host.hurtMarked = true;
    }

    private void detach() {
        entityData.set(DATA_HOST, -1);
        reelTicks = 0;
    }

    private void startReturn() {
        if (isReturning())
            return;
        entityData.set(DATA_RETURNING, true);
        // A weapon on a rope is dragged back and so meets what is in the way; a Loyalty trident
        // passes through it, as vanilla's does.
        boolean roped = flight.rope() > 0f;
        setNoPhysics(!roped);
        if (inGround && roped)
            setPos(position().add(stuckNormal.scale(0.3)));
        inGround = false;
        playSound(SoundEvents.TRIDENT_RETURN, 1.0F, 1.0F);
    }

    /**
     * One tick of the way back, as vanilla's Loyalty does it: damped velocity plus a pull to the
     * thrower's eye, and a little drift in height. Both sides run it, so the arc reads as it flies.
     * Returns false when the weapon is finished (caught, or nobody to come back to).
     */
    private boolean steer(Entity owner) {
        if (owner == null || !owner.isAlive() || owner instanceof Player p && p.isSpectator()) {
            if (!level().isClientSide)
                fall();
            return false;
        }
        Vec3 to = owner.getEyePosition().subtract(position());
        if (!level().isClientSide && to.length() < ModularTuning.CATCH_DISTANCE) {
            catchBy(owner);
            return false;
        }
        boolean roped = flight.rope() > 0f;
        if (roped && ++returnTicks > ModularTuning.ROPE_RETURN_MAX_TICKS) {
            // Caught on a corner the line cannot drag it round: it drops where it is.
            if (!level().isClientSide)
                fall();
            return false;
        }
        float rate = flight.returns() * (roped && !spent ? ModularTuning.RETURN_UNATTACHED_MULT : 1f);
        if (!roped) {
            setPosRaw(getX(), getY() + to.y * 0.015 * rate, getZ());
            if (level().isClientSide)
                yOld = getY();
        }
        setDeltaMovement(getDeltaMovement().scale(0.95).add(to.normalize().scale(ModularTuning.RETURN_ACCEL * rate)));
        if (roped)
            applyGravity(); // a line is not magic: a roped weapon still falls on the way back, so it arcs (a Loyalty trident defies gravity, as vanilla's does)
        return true;
    }

    /**
     * A roped weapon's way back is a plain entity move, not the arrow's tick: the move collides with
     * blocks and slides along them, where the arrow's own tick sticks in whatever it touches (it sat
     * in the wall wriggling). It flies butt-first, since the line pulls from the butt.
     */
    private void glide() {
        setOldPosAndRot();
        move(MoverType.SELF, getDeltaMovement());
        Vec3 v = getDeltaMovement();
        float yaw = (float) (Mth.atan2(-v.x, -v.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (Mth.atan2(-v.y, v.horizontalDistance()) * Mth.RAD_TO_DEG);
        setYRot(lerpRotation(yRotO, yaw));
        setXRot(lerpRotation(xRotO, pitch));
    }

    private void catchBy(Entity owner) {
        if (owner instanceof Player player && pickup == Pickup.ALLOWED && !player.getInventory().add(getPickupItem()))
            player.drop(getPickupItem(), false);
        discard();
    }

    /** The weapon leaves the air as the item it is. */
    private void fall() {
        if (pickup == Pickup.ALLOWED)
            spawnAtLocation(getPickupItem(), 0.1F);
        discard();
    }

    @Override
    protected void tickDespawn() {
        if (++stuckLife >= ModularTuning.THROW_STUCK_LIFE)
            fall();
    }

    private int stuckLife;

    // --- hits -----------------------------------------------------------------------------------

    @Override
    protected net.minecraft.world.phys.EntityHitResult findHitEntity(Vec3 from, Vec3 to) {
        return spent || isReturning() ? null : super.findHitEntity(from, to);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return !spent && !isReturning() && !struck.contains(target.getId()) && super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide)
            return;
        Entity target = result.getEntity();
        Entity owner = getOwner();
        struck.add(target.getId());
        float turn = spin(0f);
        float distance = (float) position().distanceTo(origin);
        float damage = Math.min(flight.damage() + flight.perBlock() * distance, flight.damage() * flight.maxMult()) * headFirst(turn);
        DamageSource source = tumbles() ? damageSources().thrown(this, owner == null ? this : owner)
                : damageSources().trident(this, owner == null ? this : owner);
        if (level() instanceof ServerLevel server)
            damage = EnchantmentHelper.modifyDamage(server, getWeaponItem(), target, source, damage);
        if (!target.hurt(source, damage)) {
            // Blocked or shrugged off: it glances away and keeps flying, ready for the next thing.
            setDeltaMovement(getDeltaMovement().scale(-0.2));
            setYRot(getYRot() + 180f);
            yRotO += 180f;
            playSound(SoundEvents.SHIELD_BLOCK, 0.6F, 1.4F);
            return;
        }
        spent = true;
        if (level() instanceof ServerLevel server)
            EnchantmentHelper.doPostAttackEffectsWithItemSource(server, target, source, getWeaponItem());
        if (target instanceof LivingEntity living) {
            doKnockback(living, source);
            doPostHurtEffects(living);
        }
        playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
        if (flight.rope() > 0f && flight.pull() > 0f && target instanceof LivingEntity living && living.isAlive()) {
            entityData.set(DATA_HOST, target.getId());
            reelTicks = 0;
            setNoPhysics(true);
            inGround = false;
        } else if (flight.returns() > 0f) {
            startReturn();
        } else {
            setDeltaMovement(getDeltaMovement().multiply(-0.01, -0.1, -0.01));
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level().isClientSide)
            return;
        stuckNormal = Vec3.atLowerCornerOf(result.getDirection().getNormal());
        entityData.set(DATA_SPIN, spin(0f));
        ItemStack stack = getPickupItemStackOrigin();
        BlockState state = level().getBlockState(result.getBlockPos());
        int wear = ModularStacks.misuse(stack, level(), state, result.getBlockPos());
        if (wear > 0)
            stack.setDamageValue(stack.getDamageValue() + ModularStacks.cap(stack, wear));
    }

    @Override
    protected void hitBlockEnchantmentEffects(ServerLevel level, BlockHitResult hit, ItemStack stack) {
        Vec3 at = hit.getBlockPos().clampLocationWithin(hit.getLocation());
        EnchantmentHelper.onHitBlock(level, stack, getOwner() instanceof LivingEntity living ? living : null, this, null, at,
                level.getBlockState(hit.getBlockPos()), item -> kill());
    }

    // --- pickup ---------------------------------------------------------------------------------

    @Override
    public void playerTouch(Player player) {
        if (entityData.get(DATA_HOST) != -1)
            return;
        if (ownedBy(player) || getOwner() == null)
            super.playerTouch(player);
    }

    @Override
    protected boolean tryPickup(Player player) {
        return super.tryPickup(player) || isNoPhysics() && ownedBy(player) && player.getInventory().add(getPickupItem());
    }

    // --- save -----------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage_f", flight.damage());
        tag.putFloat("per_block", flight.perBlock());
        tag.putFloat("max_mult", flight.maxMult());
        tag.putFloat("returns", flight.returns());
        tag.putFloat("rope", flight.rope());
        tag.putFloat("pull", flight.pull());
        tag.putFloat("water_mult", flight.waterMult());
        tag.putBoolean("tumble", flight.tumble());
        tag.putBoolean("spent", spent);
        tag.putBoolean("returning", isReturning());
        tag.putFloat("spin", entityData.get(DATA_SPIN));
        if (origin != null) {
            tag.putDouble("origin_x", origin.x);
            tag.putDouble("origin_y", origin.y);
            tag.putDouble("origin_z", origin.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        flight = new Flight(tag.getFloat("damage_f"), tag.getFloat("per_block"), tag.getFloat("max_mult"),
                tag.getFloat("returns"), tag.getFloat("rope"), tag.getFloat("pull"), tag.getFloat("water_mult"),
                tag.getBoolean("tumble"));
        spent = tag.getBoolean("spent");
        entityData.set(DATA_RETURNING, tag.getBoolean("returning"));
        entityData.set(DATA_SPIN, tag.getFloat("spin"));
        entityData.set(DATA_ROPE, flight.rope());
        entityData.set(DATA_TUMBLE, flight.tumble());
        entityData.set(DATA_STACK, getPickupItemStackOrigin().copy());
        origin = tag.contains("origin_x") ? new Vec3(tag.getDouble("origin_x"), tag.getDouble("origin_y"), tag.getDouble("origin_z")) : position();
        if (isReturning())
            setNoPhysics(true);
    }
}
