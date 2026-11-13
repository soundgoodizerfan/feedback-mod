// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.suit;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The suit's upgrade modules, one per piece of armour, each an {@code upgrade} part held whole. An attachment is either a part of one
 * mechanism class (ids {@link #OPTIC} and up: a lens, spring, actuator, tread, filter), tuned by an axis whose share of range is
 * {@link #arg} and read off its material, or one of the named shapes below (the {@code module} param of its variant) not yet converted.
 * A module works only while the suit is powered and not cut out, except the plain physics of the plate and the springs, treads and
 * filter beds ({@link #passive}). Each reads {@code mod[id]}, its power (0.25 to 1.5), or 0 when not fitted; the array is null when none is.
 *
 * <p>Attribute modules are transient modifiers set when the fit or power changes; per-tick ones run in {@link #tick}; damage and landing
 * ones are events here. Whatever they cost is Eu added to the battery's pending draw.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class SuitModules {

    private SuitModules() {
    }

    public static final int REFLEX_LINK = 7,
            EFFICIENCY_BUS = 10, AUTODOC = 11,
            MAGNET_PICKUP = 16,
            // modules that give the wheel a segment of its own (see ability)
            EMP_COIL = 35;
    // mechanism classes: a part of one kind of mechanism, tuned by its axis, whatever the figures make of it
    public static final int OPTIC = 36, SPRING = 37, ACTUATOR = 38, TREAD = 39, FILTER = 40, THERMAL = 41, THRUSTER = 42, PLATE = 43;
    /** The variant name of each id, which is also its lang key suffix; index 0 is none. */
    private static final String[] KEYS = {"", "", "", "", "", "", "", "reflex_link", "",
            "", "efficiency_bus", "autodoc", "", "", "", "", "magnet_pickup", "",
            "", "", "", "", "", "", "", "", "", "",
            "", "", "", "", "", "", "", "emp_coil", "optic", "spring", "actuator", "tread", "filter", "thermal", "thruster", "plate"};

    /** The name key of upgrade {@code id} (translate {@code modular.feedback.variant.<key>}), or empty for none. */
    public static String key(int id) {
        return id > 0 && id < KEYS.length ? KEYS[id] : "";
    }

    /** Ids are 1-based; a module array holds each id's power at {@code [id]} and its tuned-axis share at {@code [COUNT + id]}. */
    public static final int COUNT = 44;

    /** A module array's length: powers, then tuned-axis shares. */
    static final int SLOTS = 2 * COUNT;

    /** The tuned-axis share (0 to 1) of module {@code id}: where its part's axis sits in its range, 0 for a module with none. */
    static float arg(float[] m, int id) {
        return m == null ? 0f : m[COUNT + id];
    }

    /**
     * The axis a mechanism part of class {@code id} is tuned by (the bake reads its share of range into the module's {@code arg}), or empty
     * for a class with none. A class is the kind of mechanism (a lens, a spring), not a named upgrade: what it does follows from the figures.
     */
    public static String argAxis(int id) {
        return switch (id) {
            case OPTIC -> "focal_length";
            case SPRING, ACTUATOR -> "stroke";
            case TREAD -> "span";
            case FILTER -> "bed";
            case THERMAL -> "thickness";
            case THRUSTER -> "bore";
            case PLATE -> "thickness";
            default -> "";
        };
    }

    /** Bits of the wheel segments module {@code id} gives when it holds the supports {@code sup} ({@code Fig.SUP_*}): what a part can do is what it gets a segment for. */
    static int gives(int id, int sup) {
        if (id == ACTUATOR)
            return Ability.STRIDE.bit() | Ability.SHOCK.bit();
        if (id == OPTIC)
            return Ability.LENS.bit() | ((sup & io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig.SUP_CONTROLLER) != 0 ? Ability.SCAN.bit() : 0);
        // a nozzle lifts by holding the jet key and fires a single lump of fuel from the wheel
        if (id == THRUSTER)
            return Ability.JET.bit() | Ability.BURST.bit();
        // a pump on a held motor can vent the suit's heat; a held circuit lets the plate follow the ambient, a thermal mask
        if (id == THERMAL)
            return ((sup & io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig.SUP_MOTOR) != 0 ? Ability.VENT.bit() : 0)
                    | ((sup & io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig.SUP_CONTROLLER) != 0 ? Ability.CLOAK.bit() : 0);
        Ability a = ability(id);
        return a == null ? 0 : a.bit();
    }

    /** The switch segment that must be on for module {@code id} to work, 0 for one that is passive or only pulses. */
    static int gates(int id) {
        if (id == OPTIC)
            return Ability.LENS.bit();
        if (id == ACTUATOR)
            return Ability.STRIDE.bit();
        Ability a = ability(id);
        return a == null || a.pulse() || a.builtIn() ? 0 : a.bit();
    }

    /** The module that sets how strongly {@code a} works (the first that gives it), or 0 for a built-in segment. */
    static int moduleOf(Ability a) {
        for (int id = 1; id < COUNT; id++)
            if ((gives(id, 7) & a.bit()) != 0)
                return id;
        return 0;
    }

    /** How strongly {@code a} works in {@code m}, 0 when no fitted module gives it. */
    static float power(float[] m, Ability a) {
        int id = moduleOf(a);
        if (id == 0 || m == null)
            return 0f;
        return switch (a) {
            // a scan's flash reaches as far as the lens is long-focus
            case SCAN -> m[id] * (SCAN_SHORT + SCAN_LONG * arg(m, id));
            // thin fins vent best
            case VENT -> m[id] * (1f - 0.5f * arg(m, id));
            default -> m[id];
        };
    }

    /** {@code m} with every module that needs the suit's power zeroed: the springs and plates that are mechanical work unpowered. Null when none is left. */
    static float[] passive(float[] m) {
        if (m == null)
            return null;
        float[] live = null;
        for (int id : new int[] {SPRING, TREAD, FILTER, THERMAL, PLATE}) {
            if (m[id] <= 0f)
                continue;
            if (live == null)
                live = new float[SLOTS];
            live[id] = m[id];
            live[COUNT + id] = m[COUNT + id];
        }
        return live;
    }

    /** The wheel segment of legacy module {@code id}, or null for one with none. A switch module works only while its segment is on; a pulse module's segment fires it; the heat sink-like ones stay passive. */
    private static Ability ability(int id) {
        return switch (id) {
            case EMP_COIL -> Ability.EMP;
            case MAGNET_PICKUP -> Ability.MAGNET;
            case AUTODOC -> Ability.HEAL;
            default -> null;
        };
    }

    // --- placeholder figures, tuned in play (the suit's own file is SuitTuning; these are per-module and live beside their code) ---
    private static final float OXYGEN = 3f, ATTACK_SPEED = 0.2f, KB_PLATING = 0.6f,
            STRIDE = 0.12f, STEP = 1f, WATER = 0.6f, FALL_CUT = 0.5f, EFFICIENCY = 0.7f, JUMP = 0.12f,
            SAFE_FALL = 5f, LOAD = 0.5f;
    private static final float BLAST_CUT = 0.4f, FIRE_CUT = 0.5f, LINING_MIN = 0.3f, SHOT_CUT = 0.3f, BUS_SAVING = 0.3f, SINK_GAIN = 3f, LENS_GAIN = 0.5f;
    /** Throat width against its widest, at the narrowest bore and at the middle one. */
    private static final float THROAT_MIN = 0.4f, THROAT_MID = 0.7f;
    private static final float STRIDE_AMPS = 0.3f, MAGNET_AMPS = 0.2f, FILTER_COST = 15f, LINING_COST = 5f, HEAL_COST = 80f,
            STOMP_COST = 50f, STOMP_PER_BLOCK = 0.5f, STOMP_MAX = 12f, MAGNET_BASE = 3f, MAGNET_PER = 3f;
    private static final int HEAL_TICKS = 40;
    /** A lens: the reach (blocks) a short-focus lens adds; the share of thermal range a long-focus lens adds; the share of its light a glass must turn back to keep glare out; a scan flash's reach at the short and long end of the focal length. */
    private static final float LENS_REACH_BLOCK = 1.5f, LENS_REACH_ENTITY = 1f, GLARE_MIN = 0.35f, FILTER_MIN = 0.3f, SCAN_SHORT = 0.6f, SCAN_LONG = 0.8f;

    private static ResourceLocation id(String name) {
        return Feedback.id("suit_mod_" + name);
    }

    private static final ResourceLocation OXY = id("oxygen"), BR = id("block_reach"), ER = id("entity_reach"), AS = id("attack_speed"), KB = id("knockback"),
            MS = id("speed"), SH = id("step"), WE = id("water"), FM = id("fall_mult"), ME = id("efficiency"), JS = id("jump"), SF = id("safe_fall");

    private static float p(float[] m, int id) {
        return m == null ? 0f : m[id];
    }

    // --- physics of the plate and the bus (read whether or not the suit is powered) -----------------------

    /** Multiplier on the rate the suit sheds heat: a thin conductor spreads its heat to the air. */
    static float cooling(float[] m) {
        return 1f + SINK_GAIN * p(m, THERMAL) * (1f - arg(m, THERMAL));
    }

    /** Multiplier on everything the suit draws. */
    static float efficiency(float[] m) {
        return 1f - BUS_SAVING * p(m, EFFICIENCY_BUS);
    }

    /** Multiplier on thermal sight's reach. */
    static float lens(float[] m) {
        return 1f + LENS_GAIN * p(m, OPTIC) * arg(m, OPTIC);
    }

    /** Added to the pack's carry factor. */
    static float carry(float[] m) {
        // a thick plate down the leg is a frame: it takes the weight of a pack to the ground
        return LOAD * p(m, PLATE) * arg(m, PLATE);
    }

    /** A nozzle's push and the fuel it burns both go with its throat area, against the mid-range throat (as the thruster block's nozzle does). */
    static float throat(float[] m) {
        float s = THROAT_MIN + (1f - THROAT_MIN) * arg(m, THRUSTER);
        return s * s / (THROAT_MID * THROAT_MID);
    }

    // --- attributes ------------------------------------------------------------------------------------------

    /** Sets (or, for null, takes off) every attribute modifier the modules give. Called when the fit or the power changes. */
    static void attributes(ServerPlayer pl, float[] m) {
        // A filter bed trades breath against scrubbing: a thin bed passes the air easily and the breath lasts, a deep one cleans it.
        add(pl, Attributes.OXYGEN_BONUS, OXY, OXYGEN * p(m, FILTER) * (1f - arg(m, FILTER)), AttributeModifier.Operation.ADD_VALUE);
        float near = p(m, OPTIC) * (1f - arg(m, OPTIC));
        add(pl, Attributes.BLOCK_INTERACTION_RANGE, BR, LENS_REACH_BLOCK * near, AttributeModifier.Operation.ADD_VALUE);
        add(pl, Attributes.ENTITY_INTERACTION_RANGE, ER, LENS_REACH_ENTITY * near, AttributeModifier.Operation.ADD_VALUE);
        add(pl, Attributes.ATTACK_SPEED, AS, ATTACK_SPEED * p(m, REFLEX_LINK), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        // a thick plate has the mass to take a blow without moving
        add(pl, Attributes.KNOCKBACK_RESISTANCE, KB, Math.min(1f, KB_PLATING * p(m, PLATE) * arg(m, PLATE)), AttributeModifier.Operation.ADD_VALUE);
        // An actuator's stroke trades cadence against reach: a short one strides fast, a long one lifts the foot over a step.
        float act = p(m, ACTUATOR), reach = arg(m, ACTUATOR);
        add(pl, Attributes.MOVEMENT_SPEED, MS, STRIDE * act * (1f - reach), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        add(pl, Attributes.STEP_HEIGHT, SH, STEP * act * reach, AttributeModifier.Operation.ADD_VALUE);
        // A tread's span trades grip against paddle: a narrow pad bites the ground, a wide one pushes water.
        float tread = p(m, TREAD), span = arg(m, TREAD);
        add(pl, Attributes.WATER_MOVEMENT_EFFICIENCY, WE, WATER * tread * span, AttributeModifier.Operation.ADD_VALUE);
        add(pl, Attributes.MOVEMENT_EFFICIENCY, ME, EFFICIENCY * tread * (1f - span), AttributeModifier.Operation.ADD_VALUE);
        // A spring's stroke trades kick against cushion: a stiff short one throws the jump, a soft long one takes the landing.
        float spring = p(m, SPRING), travel = arg(m, SPRING);
        add(pl, Attributes.FALL_DAMAGE_MULTIPLIER, FM, -FALL_CUT * spring * travel, AttributeModifier.Operation.ADD_VALUE);
        add(pl, Attributes.JUMP_STRENGTH, JS, JUMP * spring * (1f - travel), AttributeModifier.Operation.ADD_VALUE);
        add(pl, Attributes.SAFE_FALL_DISTANCE, SF, SAFE_FALL * spring * travel, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void add(ServerPlayer pl, Holder<Attribute> attribute, ResourceLocation id, float value, AttributeModifier.Operation op) {
        SuitServer.mod(pl, attribute, id, value, op);
    }

    // --- per tick -----------------------------------------------------------------------------------------------

    /** Runs the modules that act each tick. Returns Eu to draw this tick. */
    static float tick(ServerPlayer pl, float[] m) {
        if (m == null)
            return 0f;
        float draw = 0f;
        int t = pl.tickCount;
        float stride = p(m, ACTUATOR);
        if (stride > 0f && pl.onGround() && !pl.isShiftKeyDown()) {
            Vec3 v = pl.getDeltaMovement();
            if (v.x * v.x + v.z * v.z > 0.0025)
                draw += STRIDE_AMPS * stride;
        }
        if (t % 10 == 0) {
            if (p(m, FILTER) * arg(m, FILTER) >= FILTER_MIN)
                draw += clear(pl, MobEffects.POISON, MobEffects.CONFUSION, MobEffects.HUNGER);
            // a glass that turns light back keeps glare out: the less it passes, the more it rejects
            if (p(m, OPTIC) > 0f && 1f - p(m, OPTIC) >= GLARE_MIN)
                draw += clear(pl, MobEffects.BLINDNESS, MobEffects.DARKNESS);
            // a thick plate keeps the cold off the skin, while the suit warms it
            if (p(m, THERMAL) * arg(m, THERMAL) >= LINING_MIN && pl.getTicksFrozen() > 0) {
                pl.setTicksFrozen(0);
                draw += LINING_COST;
            }
        }
        float magnet = p(m, MAGNET_PICKUP);
        if (magnet > 0f && t % 2 == 0) {
            double r = MAGNET_BASE + MAGNET_PER * magnet;
            Vec3 to = pl.position().add(0, 0.5, 0);
            boolean any = false;
            for (ItemEntity e : pl.level().getEntitiesOfClass(ItemEntity.class, pl.getBoundingBox().inflate(r), i -> !i.hasPickUpDelay())) {
                Vec3 d = to.subtract(e.position());
                if (d.lengthSqr() > 0.25) {
                    e.setDeltaMovement(d.normalize().scale(0.3));
                    any = true;
                }
            }
            if (any)
                draw += MAGNET_AMPS * 2f;
        }
        return draw;
    }

    @SafeVarargs
    private static float clear(ServerPlayer pl, Holder<net.minecraft.world.effect.MobEffect>... effects) {
        float cost = 0f;
        for (Holder<net.minecraft.world.effect.MobEffect> e : effects)
            if (pl.hasEffect(e)) {
                pl.removeEffect(e);
                cost += FILTER_COST;
            }
        return cost;
    }

    // --- events ------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer pl))
            return;
        float[] m = SuitServer.mods(pl);
        if (m == null)
            return;
        var src = event.getSource();
        float k = 1f;
        // plate reads its material's hardness and toughness: a thin one turns a shot, a thick one soaks a blast
        if (src.is(DamageTypeTags.IS_EXPLOSION))
            k -= BLAST_CUT * m[PLATE] * arg(m, PLATE);
        if (src.is(DamageTypeTags.IS_FIRE))
            k -= FIRE_CUT * m[THERMAL] * arg(m, THERMAL);
        if (src.is(DamageTypeTags.IS_PROJECTILE))
            k -= SHOT_CUT * m[PLATE] * (1f - arg(m, PLATE));
        if (k < 1f)
            event.setAmount(event.getAmount() * Math.max(0.1f, k));
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer pl))
            return;
        float[] m = SuitServer.mods(pl);
        // the energy a spring takes up on a hard landing is the energy it gives back to the ground around it
        float rebound = m == null ? 0f : m[SPRING] * arg(m, SPRING);
        if (rebound <= 0f || event.getDistance() < 3f)
            return;
        float dmg = Math.min(STOMP_MAX, STOMP_PER_BLOCK * rebound * event.getDistance());
        AABB box = pl.getBoundingBox().inflate(3, 1, 3);
        for (LivingEntity e : pl.level().getEntitiesOfClass(LivingEntity.class, box, x -> x != pl && x.isAlive()))
            e.hurt(pl.damageSources().playerAttack(pl), dmg);
    }
}
