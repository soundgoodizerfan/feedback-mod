// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.hit;

import io.github.soundgoodizerfan.feedback.combat.gear.CombatGear;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.item.ModularGlove;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.type.ModularFaces;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * What a weapon is good at, derived from the figures it was baked with and stored nowhere. Four shares, 0 to 1,
 * on one scale for every class so a slug gun and a maul compare directly (the way {@link ParryTuning#heaviness}
 * does for mass): <b>burst</b> (joules a hit carries), <b>tempo</b> (hits a second), <b>precision</b> (how exactly
 * a hit lands) and <b>stealth</b> (noise and size). The skill trees read these, not a weapon's class or name:
 * a node that empowers big hits serves a maul, a slug gun and a gauntlet alike.
 *
 * <p>Precision, by class: a gun's cone, a bow's flat flight, a melee weapon's lightness, reach and edge, bare hands
 * low and claws higher. Stealth: a gun's report and mass, a melee weapon's lightness and soft edge, a bow's quiet,
 * a hand plate's weight.
 */
public record WeaponProfile(float burst, float tempo, float precision, float stealth) {

    /** No weapon: every axis zero. */
    public static final WeaponProfile NONE = new WeaponProfile(0, 0, 0, 0);

    /** As {@link #of(LivingEntity)} for a blow struck by hand or weapon: a gun or bow in the hand reads as {@link #NONE}. */
    public static WeaponProfile meleeOf(LivingEntity who) {
        ItemStack hand = who instanceof Player p ? BareMode.mainHand(p) : who.getMainHandItem();
        if (hand.isEmpty())
            return bare(CombatGear.platedGlove(who));
        if (hand.getItem() instanceof GunItem)
            return NONE;
        var design = ModularStacks.design(hand);
        ModularType type = design == null ? null : ModularTypes.known(design.type());
        boolean strikes = type != null && switch (type.family()) {
            case BLADE, MACE, SPEAR, DIG -> true;
            default -> false;
        };
        return strikes ? of(hand) : NONE;
    }

    /** The profile of what {@code who} strikes or shoots with: the main hand, or the bare hand and its worn plated glove. */
    public static WeaponProfile of(LivingEntity who) {
        ItemStack hand = who instanceof Player p ? BareMode.mainHand(p) : who.getMainHandItem();
        if (hand.isEmpty())
            return bare(CombatGear.platedGlove(who));
        return of(hand);
    }

    /** The profile of {@code stack}: a gun, bow, crossbow or modular melee weapon; a plated glove reads as the hand it sits on. */
    public static WeaponProfile of(ItemStack stack) {
        if (stack.isEmpty())
            return NONE;
        if (stack.getItem() instanceof ModularGlove)
            return ModularGlove.plated(stack) ? bare(stack) : NONE;
        ModularFigures f = ModularStacks.figures(stack);
        if (f == null || ModularStacks.broken(stack))
            return NONE;
        GunFigures gun = stack.getItem() instanceof GunItem ? GunItem.figures(stack) : null;
        if (gun != null)
            return gun(gun);
        var design = ModularStacks.design(stack);
        ModularType type = design == null ? null : ModularTypes.known(design.type());
        if (type == null)
            return NONE;
        type = ModularFaces.view(type, design); // a hafted plan fights as its front face
        return switch (type.family()) {
            case BOW, CROSSBOW -> bow(f);
            case BLADE, MACE, SPEAR, DIG -> melee(f);
            default -> NONE;
        };
    }

    private static WeaponProfile gun(GunFigures g) {
        float burst = logShare(g.muzzleEnergy(), ProfileTuning.BURST_LOW_J, ProfileTuning.BURST_HIGH_J);
        float tempo = share(g.cycleMs() <= 0 ? 0 : 1000f / g.cycleMs(), ProfileTuning.TEMPO_SLOW, ProfileTuning.TEMPO_FAST);
        float precision = 1 - logShare(g.spreadDeg(), ProfileTuning.SPREAD_TIGHT_DEG, ProfileTuning.SPREAD_LOOSE_DEG);
        float quiet = 1 - Mth.clamp(g.report(), 0f, 1f);
        float stealth = ProfileTuning.STEALTH_REPORT_W * quiet + (1 - ProfileTuning.STEALTH_REPORT_W) * lightness(g.gunMass().value());
        return new WeaponProfile(burst, tempo, precision, stealth);
    }

    private static WeaponProfile bow(ModularFigures f) {
        float v = Math.max(1f, f.get(Fig.VELOCITY));
        float draw = f.get(Fig.DRAW) <= 0 ? 20f : f.get(Fig.DRAW);
        return new WeaponProfile(logShare(ProfileTuning.BOW_BASE_J * v * v, ProfileTuning.BURST_LOW_J, ProfileTuning.BURST_HIGH_J),
                share(20f / draw, ProfileTuning.TEMPO_SLOW, ProfileTuning.TEMPO_FAST),
                Mth.clamp(v - 1f, 0f, 1f), ProfileTuning.BOW_STEALTH);
    }

    private static WeaponProfile melee(ModularFigures f) {
        float light = lightness(f.get(Fig.MASS));
        float edge = Mth.clamp(f.get(Fig.HARDNESS) / ProfileTuning.EDGE_HARD, 0f, 1f);
        float precision = ProfileTuning.PRECISION_LIGHT_W * light
                + ProfileTuning.PRECISION_REACH_W * Mth.clamp(f.get(Fig.REACH) / ProfileTuning.REACH_LONG, 0f, 1f)
                + ProfileTuning.PRECISION_EDGE_W * edge;
        float stealth = ProfileTuning.STEALTH_MELEE_LIGHT_W * light + (1 - ProfileTuning.STEALTH_MELEE_LIGHT_W) * (1 - edge);
        return new WeaponProfile(logShare(f.get(Fig.IMPACT), ProfileTuning.BURST_LOW_J, ProfileTuning.BURST_HIGH_J),
                share(f.get(Fig.SWING), ProfileTuning.TEMPO_SLOW, ProfileTuning.TEMPO_FAST), precision, stealth);
    }

    /** Bare hands, with a plated glove (or none): the plate adds to the blow, weighs on the swing and the quiet, and claws sharpen it. */
    private static WeaponProfile bare(ItemStack glove) {
        float fist = glove.isEmpty() ? 0f : ModularGlove.fist(glove);
        float drag = 0f, plate = 0f;
        boolean claws = false;
        if (!glove.isEmpty()) {
            ModularFigures f = ModularStacks.figures(glove);
            if (f != null) {
                drag = f.get(Fig.SWING_DRAG);
                plate = f.get(Fig.MASS);
            }
            claws = ModularGlove.plateParams(glove).containsKey("bleed_damage");
        }
        float joules = ProfileTuning.BARE_JOULES + fist * ParryTuning.JOULES_PER_DAMAGE;
        return new WeaponProfile(logShare(joules, ProfileTuning.BURST_LOW_J, ProfileTuning.BURST_HIGH_J),
                share(ProfileTuning.BARE_SWINGS * (1 - drag), ProfileTuning.TEMPO_SLOW, ProfileTuning.TEMPO_FAST),
                Mth.clamp(ProfileTuning.BARE_PRECISION + (claws ? ProfileTuning.CLAW_PRECISION : 0f), 0f, 1f),
                1 - ProfileTuning.PLATE_LOUD_MAX * Mth.clamp(plate / ProfileTuning.PLATE_LOUD_MU, 0f, 1f));
    }

    /** 0 for a heavy weapon, 1 for a light one, by the parry's own heft scale. */
    private static float lightness(float mass) {
        return 1 - ParryTuning.heaviness(mass, 0f);
    }

    private static float share(float v, float low, float high) {
        return Mth.clamp((v - low) / (high - low), 0f, 1f);
    }

    private static float logShare(float v, float low, float high) {
        return v <= low ? 0f : Mth.clamp((float) (Math.log(v / low) / Math.log(high / low)), 0f, 1f);
    }
}
