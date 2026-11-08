// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
 * <li><b>Swing or drag, by mass.</b> A block is infinitely heavy, and a creature is compared with the
 * thrower by {@link BodyMass}. With a barb, a lighter creature is reeled to the thrower. Otherwise the thrower
 * swings: the line is a fixed length (the distance at the bite, at most {@link #maxRope}),
 * nothing pulls, and when the thrower is taut the outward share of their velocity is cancelled and the
 * stretch drawn back, so gravity and what they carried make a pendulum.
 * <li><b>Fittings</b> ({@link #ROPE}, {@link #WINCH}, {@link #BARB}) come from the grapple that threw it. Without a barb a
 * creature is only an anchor to swing from; the thrower's own client winches the swing in while the use key is held
 * ({@code client/GrappleSwing}) and keeps the momentum, boosted, on letting go.
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

    /** Fittings (bit set, from the grapple's component): an extra coil of line. */
    public static final int ROPE = 1;
    /** A winch: the reel runs faster. */
    public static final int WINCH = 2;
    /** A barbed head: a lighter creature the hook bites is reeled in, not swung from. */
    public static final int BARB = 4;
    /** A second hook head: using the grapple again with a line out throws a second line. */
    public static final int MULTI = 8;

    private static final EntityDataAccessor<Integer> DATA_FITTINGS = SynchedEntityData.defineId(GrappleHook.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ANCHORED = SynchedEntityData.defineId(GrappleHook.class, EntityDataSerializers.BOOLEAN);

    /** The material the line is made of; the grapple's recipe takes string. */
    private static final ResourceLocation ROPE_MATERIAL = Feedback.id("wool");

    /** The fixed line length of a swing the owner is on (0 when the hook drags a creature or has not bitten): the owner's client runs the pendulum with it. */
    private static final EntityDataAccessor<Float> DATA_SWING_ROPE = SynchedEntityData.defineId(GrappleHook.class, EntityDataSerializers.FLOAT);

    /** Where the line bends round block edges, anchor outward (synced; the server lays them, clients read them). */
    private static final EntityDataAccessor<net.minecraft.nbt.CompoundTag> DATA_BENDS = SynchedEntityData.defineId(GrappleHook.class, EntityDataSerializers.COMPOUND_TAG);
    private final java.util.List<Vec3> bends = new java.util.ArrayList<>();

    /** The creature the hook bit, or -1 for a block. */
    private int anchorEntity = -1;
    private Vec3 anchorPoint = Vec3.ZERO;
    /** The pulled body, -1 before the hook bites. */
    private int heldId = -1;
    private int reelTicks;
    private float lastDistance;
    private float strain;
    private float rating;
    /** The fixed line length of a swing, in blocks. */
    private float rope;
    /** The block a swing is hooked to, or null (creature anchor, drag). */
    private BlockPos anchorBlock;
    /** Strain on what the hook bit into: over the block's holding figure it works loose and rips free. */
    private float holdStrain;
    private int lastCutter = -1;
    private int lastCutTick;

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
        builder.define(DATA_FITTINGS, 0);
        builder.define(DATA_SWING_ROPE, 0f);
        builder.define(DATA_BENDS, RopeWrap.toTag(java.util.List.of()));
    }

    /** The bends in the line, anchor outward (empty when it runs straight). Read by the owner's client for the pendulum and by every client to draw. */
    public java.util.List<Vec3> bends() {
        return RopeWrap.fromTag(entityData.get(DATA_BENDS));
    }

    /** The line length of the swing the owner is on, or 0 when there is none. Read by the owner's client, which moves them. */
    public float swingRope() {
        return entityData.get(DATA_SWING_ROPE);
    }

    /** Pendulum constraint on a velocity: past the rope's length, cancel the share running away from the anchor and draw the stretch back; slack line leaves it alone. */
    public static Vec3 constrain(Vec3 offset, float rope, Vec3 velocity) {
        double distance = offset.length();
        double stretch = distance - rope;
        if (stretch <= 0 || distance < 1.0e-4)
            return velocity;
        Vec3 r = offset.scale(1 / distance);
        double along = velocity.dot(r); // toward the anchor when positive
        Vec3 v = along < 0 ? velocity.subtract(r.scale(along)) : velocity;
        return v.add(r.scale(Math.min(stretch, ModularTuning.GRAPPLE_SWING_DRAW)));
    }

    public void setFittings(int fittings) {
        entityData.set(DATA_FITTINGS, fittings);
    }

    public boolean has(int fitting) {
        return (entityData.get(DATA_FITTINGS) & fitting) != 0;
    }

    /** The longest line this hook carries, blocks. */
    public float maxRope() {
        return has(ROPE) ? ModularTuning.GRAPPLE_ROPE_LONG : ModularTuning.GRAPPLE_ROPE;
    }

    /** Blocks per tick the reel draws in. */
    public float reelSpeed() {
        return ModularTuning.GRAPPLE_REEL * (has(WINCH) ? ModularTuning.GRAPPLE_WINCH_MULT : 1f);
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
            if (owner == null || !owner.isAlive() || owner.distanceTo(this) > maxRope()) {
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
        // The anchor sits a hair off the face so the line's first ray does not start inside its own block.
        bite(null, hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(ModularTuning.GRAPPLE_BEND_CLEARANCE)), hit.getBlockPos());
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
        boolean dragTarget = target != null && has(BARB) && BodyMass.of(target) < BodyMass.of(owner);
        LivingEntity held = dragTarget ? target : owner;
        Tether.Anchor anchor = dragTarget ? new Tether.Anchor.OnEntity(owner.getId())
                : target != null ? new Tether.Anchor.OnEntity(target.getId()) : new Tether.Anchor.OnBlock(block);
        anchorEntity = target != null ? target.getId() : -1;
        anchorPoint = at;
        anchorBlock = block;
        heldId = held.getId();
        rating = lineRating();
        lastDistance = (float) held.position().distanceTo(anchorPoint());
        rope = Math.min(maxRope(), Math.max(lastDistance, (float) ModularTuning.GRAPPLE_STOP_DISTANCE));
        Tethers.add(held, new Tether(getUUID(), anchor, maxRope(), dragTarget ? reelSpeed() : 0f, 1f,
                new Tether.Release(ModularTuning.GRAPPLE_STOP_DISTANCE, ModularTuning.GRAPPLE_MAX_TICKS)));
        setDeltaMovement(Vec3.ZERO);
        setPos(at);
        entityData.set(DATA_SWING_ROPE, !dragTarget && owner instanceof Player ? rope : 0f);
        entityData.set(DATA_ANCHORED, true);
        playSound(SoundEvents.CHAIN_HIT, 1f, 1f);
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
        if (anchorBlock != null && level().getBlockState(anchorBlock).isAir()) {
            discard();
            return;
        }
        Vec3 to = anchorPoint();
        setPos(to);
        Vec3 offset = to.subtract(living.position().add(0, living.getBbHeight() * 0.5, 0));
        float distance = (float) offset.length();
        boolean swing = living == owner;
        if (++reelTicks > ModularTuning.GRAPPLE_MAX_TICKS || !swing && distance <= ModularTuning.GRAPPLE_STOP_DISTANCE
                || reelTicks > ModularTuning.GRAPPLE_SNEAK_GRACE && owner.isShiftKeyDown()) {
            // A staggered body hauled all the way in is the Throw's moment (Throwing checks skill and stagger).
            if (living != owner && owner instanceof LivingEntity thrower && distance <= ModularTuning.GRAPPLE_STOP_DISTANCE)
                io.github.soundgoodizerfan.feedback.combat.grab.Throwing.attempt(thrower, living);
            discard();
            return;
        }
        boolean stalled = !swing && reelTicks > 2 && lastDistance - distance < reelSpeed() * ModularTuning.GRAPPLE_STALL_FRACTION;
        lastDistance = distance;
        if (swing && anchorEntity < 0 && owner instanceof Player) {
            Vec3 end = living.position().add(0, living.getBbHeight() * 0.5, 0);
            if (RopeWrap.update(level(), this, to, bends, end))
                entityData.set(DATA_BENDS, RopeWrap.toTag(bends));
        }
        // Each edge the line is dragged over saws at it: a bent line takes more than the body weighs.
        float load = BodyMass.of(living) * (stalled ? ModularTuning.GRAPPLE_STALL_LOAD : 1f)
                * (1f + bends.size() * ModularTuning.GRAPPLE_BEND_LOAD);
        if (load > rating)
            strain += (load / rating - 1f) * ModularTuning.GRAPPLE_STRAIN_PER_TICK;
        Vec3 end = living.position().add(0, living.getBbHeight() * 0.5, 0);
        java.util.List<Vec3> path = path(to, end);
        strain += burn(path) + cut(path, owner, living, rating);
        if (strain >= 1f) {
            playSound(SoundEvents.LEASH_KNOT_BREAK, 1f, 1f);
            if (owner instanceof Player p)
                GrappleItem.wear(p, ModularTuning.GRAPPLE_BREAK_WEAR);
            discard();
            return;
        }
        // A hook in soft or loose ground works out under the same load; a drag has no block to lose.
        if (anchorBlock != null) {
            float hold = holdingFigure(level().getBlockState(anchorBlock), level(), anchorBlock);
            if (load > hold) {
                holdStrain += (load / hold - 1f) * ModularTuning.GRAPPLE_STRAIN_PER_TICK;
                if (holdStrain >= 1f) {
                    level().playSound(null, anchorBlock, SoundEvents.CHAIN_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.8f);
                    discard();
                    return;
                }
            }
        }
        if (swing) {
            swing(living, offset, distance);
            return;
        }
        float speed = reelSpeed();
        speed *= 1f - (float) living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        // A crouched player with footing holds back a share of the pull (armour mass adds to it).
        if (living instanceof Player held2) {
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

    /**
     * Pendulum for a body that is not a player: {@link #constrain} on its last-step velocity. A player's own client runs this instead
     * (the server holds no real motion for one, and pushing its guess down made the swing bounce); here the server only keeps fall damage off.
     */
    private void swing(LivingEntity body, Vec3 offset, float distance) {
        body.fallDistance = 0f;
        if (body instanceof Player || distance <= rope)
            return;
        body.setDeltaMovement(constrain(offset, rope, new Vec3(body.getX() - body.xo, body.getY() - body.yo, body.getZ() - body.zo)));
        body.hurtMarked = true;
    }

    // --- what the world does to the line and the bite -------------------------------------------

    /**
     * The load, in Mu, the block the hook bit holds: its hardness times {@link ModularTuning#GRAPPLE_HOLD_PER_HARDNESS}.
     * A falling block (sand, gravel) holds half, and a slick one (ice) a third; an unbreakable block holds anything.
     */
    static float holdingFigure(net.minecraft.world.level.block.state.BlockState state, Level level, BlockPos pos) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0)
            return Float.MAX_VALUE;
        float hold = Math.max(hardness, 0.05f) * ModularTuning.GRAPPLE_HOLD_PER_HARDNESS;
        if (state.getBlock() instanceof net.minecraft.world.level.block.Fallable)
            hold *= ModularTuning.GRAPPLE_HOLD_LOOSE;
        if (state.getBlock().getFriction() >= ModularTuning.GRAPPLE_SLICK_FRICTION)
            hold *= ModularTuning.GRAPPLE_HOLD_SLICK;
        return hold;
    }

    /** The line as a polyline: the anchor, each bend, the held body. */
    private java.util.List<Vec3> path(Vec3 anchor, Vec3 end) {
        java.util.List<Vec3> pts = new java.util.ArrayList<>(bends.size() + 2);
        pts.add(anchor);
        pts.addAll(bends);
        pts.add(end);
        return pts;
    }

    private static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        double t = len2 < 1.0e-9 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len2));
        return p.distanceTo(a.add(ab.scale(t)));
    }

    private static double distanceToLine(Vec3 p, java.util.List<Vec3> path) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i + 1 < path.size(); i++)
            best = Math.min(best, distanceToSegment(p, path.get(i), path.get(i + 1)));
        return best;
    }

    /** Strain this tick from fire or lava touching the line, when it is hotter than the line's material oxidises. */
    private float burn(java.util.List<Vec3> path) {
        Material m = MaterialTable.get().materials().get(ROPE_MATERIAL);
        if (m == null || m.thermal().oxidationTemperature().isEmpty())
            return 0f;
        float oxidises = m.thermal().oxidationTemperature().get().value();
        for (int i = 0; i + 1 < path.size(); i++) {
            Vec3 a = path.get(i), b = path.get(i + 1);
            int steps = Math.max(1, (int) Math.ceil(a.distanceTo(b) / 2.0));
            for (int k = 0; k <= steps; k++) {
                Vec3 p = a.lerp(b, k / (double) steps);
                BlockPos at = BlockPos.containing(p);
                net.minecraft.world.level.block.state.BlockState state = level().getBlockState(at);
                float heat = state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA) ? io.github.soundgoodizerfan.feedback.core.FTuning.FIRE_TU_LAVA.value()
                        : state.is(net.minecraft.tags.BlockTags.FIRE) ? io.github.soundgoodizerfan.feedback.core.FTuning.FIRE_TU_CAMPFIRE.value() : 0f;
                if (heat > oxidises) {
                    ((net.minecraft.server.level.ServerLevel) level()).sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.01);
                    return ModularTuning.GRAPPLE_BURN_PER_TICK;
                }
            }
        }
        return 0f;
    }

    /**
     * Strain from a blade swung across the line or an arrow through it: the attacker's attack damage (arrow: speed times
     * base damage) over the line's rating, times {@link ModularTuning#GRAPPLE_CUT_PER_DAMAGE}. Your own arrows pass.
     */
    private float cut(java.util.List<Vec3> path, Entity owner, LivingEntity held, float rating) {
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(path.get(0), path.get(0));
        for (Vec3 p : path)
            box = box.minmax(new net.minecraft.world.phys.AABB(p, p));
        box = box.inflate(ModularTuning.GRAPPLE_CUT_REACH + 1.0);
        float edge = 0f;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, e -> e != owner && e != held && e.swinging && e.swingTime <= 0)) {
            if (lastCutter == e.getId() && tickCount - lastCutTick < 8)
                continue;
            Vec3 eye = e.getEyePosition(), look = e.getLookAngle();
            for (int k = 1; k <= 3; k++) {
                if (distanceToLine(eye.add(look.scale(k)), path) < ModularTuning.GRAPPLE_CUT_WIDTH) {
                    edge += (float) e.getAttributeValue(Attributes.ATTACK_DAMAGE);
                    lastCutter = e.getId();
                    lastCutTick = tickCount;
                    break;
                }
            }
        }
        for (net.minecraft.world.entity.projectile.AbstractArrow a : level().getEntitiesOfClass(net.minecraft.world.entity.projectile.AbstractArrow.class, box,
                a -> a.getOwner() != owner && !a.onGround() && a.getDeltaMovement().lengthSqr() > 0.04)) {
            if (distanceToLine(a.position(), path) < ModularTuning.GRAPPLE_CUT_WIDTH)
                edge += (float) (a.getDeltaMovement().length() * a.getBaseDamage());
        }
        return edge <= 0f ? 0f : edge / rating * ModularTuning.GRAPPLE_CUT_PER_DAMAGE;
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

    /** Every hook {@code owner} has out (at most two). */
    public static java.util.List<GrappleHook> allOf(Level level, LivingEntity owner) {
        return level.getEntitiesOfClass(GrappleHook.class, owner.getBoundingBox().inflate(ModularTuning.GRAPPLE_ROPE_LONG + 8), h -> h.getOwner() == owner);
    }

    /** The hook {@code owner} has out, or null. */
    public static GrappleHook of(Level level, LivingEntity owner) {
        for (GrappleHook h : level.getEntitiesOfClass(GrappleHook.class, owner.getBoundingBox().inflate(ModularTuning.GRAPPLE_ROPE_LONG + 8)))
            if (h.getOwner() == owner)
                return h;
        return null;
    }
}
