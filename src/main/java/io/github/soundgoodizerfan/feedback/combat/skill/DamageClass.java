// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.skill;

import io.github.soundgoodizerfan.feedback.combat.gear.CombatGear;
import io.github.soundgoodizerfan.feedback.combat.hit.BareMode;
import io.github.soundgoodizerfan.feedback.core.hazard.Hazards;
import io.github.soundgoodizerfan.feedback.modular.ModularGlove;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.Set;

/**
 * The class of a blow and the classes a creature is weak to, for Observation's {@code right_tool}. Both are derived, never
 * tabled by mob: a blow's class comes from how it was delivered (a bullet or arrow pierces, a blast or a mace is blunt, a
 * blade slashes, fire and shock are their own), a creature's weaknesses from its state and what it is made of (bone,
 * a bare thin hide, soft worn cloth, wet skin, cold dry air).
 */
public final class DamageClass {

    public enum Kind {
        BLUNT, PIERCE, SLASH, FIRE, SHOCK
    }

    /** A creature with at most this much armour and none worn has a thin hide. */
    static final int THIN_HIDE_ARMOUR = 2;
    /** Mean worn-piece hardness at or under which a creature is dressed in cloth or soft hide. */
    static final float CLOTH_HARDNESS = 2.0f;

    private DamageClass() {
    }

    /** The class of {@code source} struck by {@code p}, or {@code null} if it has none. */
    static Kind of(DamageSource source, Player p) {
        if (source.is(Hazards.SHOCK))
            return Kind.SHOCK;
        if (source.is(DamageTypeTags.IS_FIRE))
            return Kind.FIRE;
        if (source.is(DamageTypeTags.IS_EXPLOSION))
            return Kind.BLUNT;
        if (source.is(DamageTypeTags.IS_PROJECTILE) || source.getDirectEntity() instanceof Projectile)
            return Kind.PIERCE;
        if (source.getDirectEntity() != p)
            return null;
        ItemStack hand = BareMode.mainHand(p);
        if (hand.isEmpty())
            return ModularGlove.claws(CombatGear.platedGlove(p)) ? Kind.SLASH : Kind.BLUNT;
        if (hand.getItem() instanceof GunItem)
            return Kind.BLUNT;
        var design = ModularStacks.design(hand);
        ModularType type = design == null ? null : ModularTypes.known(design.type());
        if (type == null)
            return null;
        return switch (type.family()) {
            case BLADE -> Kind.SLASH;
            case SPEAR -> Kind.PIERCE;
            case MACE, DIG -> Kind.BLUNT;
            default -> null;
        };
    }

    /** What {@code t} is weak to right now, from its state and what it wears. */
    static Set<Kind> weakTo(LivingEntity t, float wornHardness, int pieces) {
        Set<Kind> out = EnumSet.noneOf(Kind.class);
        boolean bone = t.getType().is(EntityTypeTags.SKELETONS);
        boolean wet = t.isInWaterOrRain();
        if (bone)
            out.add(Kind.BLUNT);
        if (wet)
            out.add(Kind.SHOCK);
        if (!bone && pieces == 0 && t.getArmorValue() <= THIN_HIDE_ARMOUR)
            out.add(Kind.PIERCE);
        if (pieces > 0 && wornHardness <= CLOTH_HARDNESS)
            out.add(Kind.SLASH);
        if (!wet && !t.fireImmune() && (t.getTicksFrozen() > 0 || t.level().getBiome(t.blockPosition()).value().coldEnoughToSnow(t.blockPosition())))
            out.add(Kind.FIRE);
        return out;
    }
}
