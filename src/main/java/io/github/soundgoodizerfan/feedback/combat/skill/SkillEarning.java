// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.skill;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.hit.BareMode;
import io.github.soundgoodizerfan.feedback.combat.hit.WeaponProfile;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.StrikeSource;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Where skill points are earned (plan 2.7): each tree's pool by doing its own physical act, every award
 * diminished by how often that same target has paid lately ({@link SkillPools#awarded}). One pool per tree
 *.
 * <ul>
 * <li>Anchor: a parry that lands ({@link #parried}, called from the parry resolver), per attacker.</li>
 * <li>Veil: a roll that cancelled a blow struck by something ({@link #dodged}, called from the roll's i-frame
 * cancel), per attacker. Fire ticks and other sourceless damage do not pay.</li>
 * <li>Brute, Onslaught, Observation (and Veil): a landed blow, by the weapon's strongest axis and the swing's nature
 * ({@link #hit}). Either hand: the weapon is the one the damage source names ({@link StrikeSource}), so an off-hand blow pays
 * by the off-hand weapon. Arrows are paid in {@link BowCraft}.</li>
 * <li>Proficiency: deliberate acts only -- aid given to an ally, handling a hazard while protected from it, eating a
 * well-cooked meal. Taking a blow pays nothing.</li>
 * <li>Chaos: variety. A new kind of disorder left on a target ({@link #disorder}: burning, armour worn, weapon stripped,
 * knocked into another body, blinded, tripped...) pays once, and each kind decays per target, so one trick repeated on
 * one mob pays little.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class SkillEarning {

    public static final String ANCHOR = "anchor", VEIL = "veil", BRUTE = "brute", ONSLAUGHT = "onslaught",
            OBSERVATION = "observation", PROFICIENCY = "proficiency", CHAOS = "chaos";

    private SkillEarning() {
    }

    /** A parry landed against {@code source}. */
    public static void parried(LivingEntity defender, DamageSource source) {
        award(defender, ANCHOR, keyOf(source), SkillTuning.PARRY);
    }

    /** A roll's i-frames swallowed {@code source}. */
    public static void dodged(LivingEntity who, DamageSource source) {
        if (source.getEntity() != null)
            award(who, VEIL, keyOf(source), SkillTuning.EVASION);
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (event.getNewDamage() > 0f) {
            if (source.getEntity() instanceof ServerPlayer attacker)
                Rearms.fight(attacker);
            if (event.getEntity() instanceof ServerPlayer victim)
                Rearms.fight(victim);
        }
        if (!(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player
                || !source.is(DamageTypes.PLAYER_ATTACK) || event.getNewDamage() <= 0f)
            return;
        Rearms.on(player, SkillTree.Rearm.STRIKE);
        String target = event.getEntity().getStringUUID();
        // The weapon of the hand that struck; in barehanded mode either hand is a fist whatever it holds.
        ItemStack weapon = BareMode.on(player) ? ItemStack.EMPTY : StrikeSource.weapon(player, source);
        SkillPools before = SkillPools.of(player);
        hit(player, event.getEntity(), weapon, event.getNewDamage());
        if (event.getEntity().isDeadOrDying())
            wisp(player, event.getEntity(), before);
    }

    /** A kill that paid: one wisp in the colour of the tree that gained most, sent to the killer alone. */
    private static void wisp(ServerPlayer player, LivingEntity target, SkillPools before) {
        SkillPools after = SkillPools.of(player);
        String best = null;
        int most = 0;
        for (var e : after.points().entrySet())
            if (e.getValue() - before.points(e.getKey()) > most) {
                most = e.getValue() - before.points(e.getKey());
                best = e.getKey();
            }
        if (best != null)
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new io.github.soundgoodizerfan.feedback.net.SkillWispPayload(
                    TreeHue.of(best), target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ()));
    }

    /** Per player: the game tick of their last landed blow, so a quick follow-up reads as a chain. */
    private static final java.util.Map<java.util.UUID, Long> LAST_HIT = new java.util.HashMap<>();

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_HIT.remove(event.getEntity().getUUID());
    }

    /**
     * A landed blow, paid by the weapon and by the swing. The weapon's strongest of burst, tempo and precision names the home
     * pool (Brute, Onslaught, Observation), so a maul, a pair of daggers and a sniper rifle each have one; the swing adds the
     * pool of what it was: a blow of real energy (Brute), one in a quick chain (Onslaught), a placed hit -- from range, at
     * the tip of the reach, on a read, unaware or back-turned creature (Observation) -- and a blow on an unaware creature
     * pays Veil by the weapon's stealth. Each pool pays once, diminishing per target like any award.
     */
    private static void hit(ServerPlayer player, LivingEntity target, ItemStack weapon, float damage) {
        var profile = weapon.isEmpty() ? WeaponProfile.of(player) : WeaponProfile.of(weapon);
        String key = target.getStringUUID();
        long now = player.level().getGameTime();
        Long last = LAST_HIT.put(player.getUUID(), now);
        java.util.Set<String> paid = new java.util.LinkedHashSet<>();
        if (profile.burst() > 0 || profile.tempo() > 0 || profile.precision() > 0) {
            String home = profile.burst() >= profile.tempo() && profile.burst() >= profile.precision() ? BRUTE
                    : profile.tempo() >= profile.precision() ? ONSLAUGHT : OBSERVATION;
            award(player, home, key, SkillTuning.HIT);
            paid.add(home);
        }
        if (damage * io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning.JOULES_PER_DAMAGE >= SkillTuning.BRUTE_HIT_J && paid.add(BRUTE))
            award(player, BRUTE, key, SkillTuning.NATURE_HIT);
        if (last != null && now - last <= SkillTuning.CHAIN_TICKS && paid.add(ONSLAUGHT))
            award(player, ONSLAUGHT, key, SkillTuning.NATURE_HIT);
        double reach = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE);
        double dist = player.distanceTo(target);
        boolean unaware = Weakness.unaware(target);
        boolean placed = dist >= SkillTuning.OBSERVE_RANGE || dist >= reach * SkillTuning.REACH_TIP
                || Weakness.isRead(player, target) || unaware || Weakness.backTurned(player, target);
        if (placed && paid.add(OBSERVATION))
            award(player, OBSERVATION, key, SkillTuning.NATURE_HIT);
        int quiet = Math.round(SkillTuning.STEALTH_HIT * profile.stealth());
        if (unaware && quiet > 0)
            award(player, VEIL, key, quiet);
    }

    /** {@code player} left {@code target} in a {@code kind} of disorder: paid per kind per target, diminishing as that pair repeats. */
    public static void disorder(ServerPlayer player, LivingEntity target, String kind) {
        award(player, CHAOS, kind + "/" + target.getStringUUID(), SkillTuning.DISORDER);
    }

    /** {@code who} salvaged something (dismantled a worn part, brought something down with a placed charge): a small trickle, decaying per {@code key} so repeating one thing earns nothing. */
    public static void salvaged(ServerPlayer who, String key) {
        award(who, CHAOS, "salvage/" + key, SkillTuning.SALVAGE);
    }

    /** {@code who} has delivered a measure of work into the vehicle body {@code body}; repeats on one body decay, and a braked body takes no work at all. */
    public static void pushed(LivingEntity who, java.util.UUID body) {
        award(who, BRUTE, body.toString(), SkillTuning.PUSH);
    }

    /** {@code who} delivered aid (healing, a haul, stamina, a cut rope) to {@code ally}: a trickle per ally, decaying so standing by one friend farms little. */
    public static void aided(ServerPlayer who, ServerPlayer ally) {
        trickle(who, "aid/" + ally.getStringUUID(), SkillTuning.AID);
    }

    /** {@code who} struck a clean streak at the forge, shaping {@code variant}: a trickle per variant, decaying so one idle bar farms little. */
    public static void forged(ServerPlayer who, String variant) {
        award(who, ONSLAUGHT, "forge/" + variant, SkillTuning.FORGE_STREAK);
    }

    /**
     * {@code who} met a workshop hazard ({@code kind}: fume, radiation, food) with {@code protection} (0 to 1, the share of it kept
     * off them): paid in proportion, so an unprotected dose pays nothing, and capped so standing in it farms nothing.
     */
    public static void hazardHandled(ServerPlayer who, String kind, float protection) {
        int base = Math.round(SkillTuning.HAZARD * Math.max(0f, Math.min(1f, protection)));
        if (base > 0)
            trickle(who, "hazard/" + kind, base);
    }

    /** {@code who} ate a meal of {@code food} cooked to {@code quality} (0 to 1: cooked through, not charred, not spoiled): paid by quality, per food, capped. */
    public static void ate(ServerPlayer who, String food, float quality) {
        int base = Math.round(SkillTuning.EAT_COOKED * Math.max(0f, Math.min(1f, quality)));
        if (base > 0)
            trickle(who, "eat/" + food, base);
    }

    /** A repeatable act: diminishing like any award, and nothing at all once {@link SkillTuning#TRICKLE_CAP} recent payouts stand. */
    private static void trickle(ServerPlayer who, String key, int base) {
        if (!who.isSpectator())
            pay(who, PROFICIENCY, SkillPools.of(who).awarded(PROFICIENCY, key, base, who.level().getGameTime(), SkillTuning.TRICKLE_CAP));
    }

    /** {@code who} cranked a loaded crank at {@code pos}: a trickle per crank, decaying, so idling at one earns nothing. */
    public static void cranked(ServerPlayer who, net.minecraft.core.BlockPos pos) {
        award(who, ANCHOR, "crank/" + pos.asLong(), SkillTuning.CRANK);
    }

    /** {@code who} held footing against a tether {@code source}'s pull; paid per source and decaying, so a long reel pays once. */
    public static void heldPull(ServerPlayer who, java.util.UUID source) {
        award(who, ANCHOR, "pull/" + source, SkillTuning.HELD_PULL);
    }

    /** {@code who} passed a vibration listener at {@code pos} that would have heard them but for a hushed step or hand; per listener, decaying. */
    public static void hushed(ServerPlayer who, net.minecraft.world.phys.Vec3 pos) {
        award(who, VEIL, "hush/" + net.minecraft.core.BlockPos.containing(pos).asLong(), SkillTuning.HUSHED);
    }

    /** The baked family of a modular stack, or null for anything else. */
    public static ModularType.Family family(ItemStack stack) {
        return io.github.soundgoodizerfan.feedback.modular.ModularFaces.combat(stack);
    }

    /** The attacker's uuid, or the damage type's name when nothing struck. */
    private static String keyOf(DamageSource source) {
        return source.getEntity() != null ? source.getEntity().getStringUUID() : source.getMsgId();
    }

    static void award(LivingEntity who, String tree, String key, int base) {
        if (!(who instanceof ServerPlayer player) || player.isSpectator())
            return;
        pay(player, tree, SkillPools.of(player).awarded(tree, key, base, player.level().getGameTime()));
    }

    /** Stores {@code next}; when it holds more points in {@code tree} than before, a chime sounds for the player (the client's HUD shows the gain, and a kill sends a wisp). */
    private static void pay(ServerPlayer player, String tree, SkillPools next) {
        boolean gained = next.points(tree) > SkillPools.of(player).points(tree);
        SkillPools.set(player, next);
        if (!gained || !(player.level() instanceof ServerLevel level))
            return;
        // Each tree rings at its own pitch; only the earner hears it.
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.35f, 0.8f + Math.floorMod(tree.hashCode(), 8) * 0.08f);
    }
}
