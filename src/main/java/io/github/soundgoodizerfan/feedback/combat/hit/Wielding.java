// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.hit;

import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * How many hands a melee weapon takes, and what that does to the other. Better Combat's two-handed and
 * dual-wielding rules, decided here from the weapon's own baked figures instead of a weapon-attributes file,
 * so every weapon a player can build answers, and the numbers are ours to change.
 *
 * <ul>
 * <li><b>Two-handed:</b> a spear always, a mace or blade once its {@link ParryTuning#heaviness} passes a
 * threshold (a maul, a claymore; never a dagger). A two-handed weapon in either hand shuts the off hand:
 * no off-hand strike, no dual wielding, no off-hand parry.</li>
 * <li><b>Dual wielding:</b> an unbroken one-handed strike weapon in each hand. Left click then swings the
 * main hand and right click the off hand, always (client/UpswingSwing), and attack speed rises by {@link #DUAL_SPEED} (DualWieldBoost).</li>
 * </ul>
 * Every threshold is a placeholder until played.
 */
public final class Wielding {

    /** Heaviness above which a blade, and a mace, takes both hands. */
    public static final float TWO_HANDED_BLADE = 0.7f, TWO_HANDED_MACE = 0.6f;
    /** Attack speed multiplier while dual wielding (Better Combat's default is 1.2). */
    public static final float DUAL_SPEED = 1.2f;

    private Wielding() {
    }

    /** Whether {@code stack} is an unbroken modular blade, mace or spear that takes both hands. */
    public static boolean isTwoHanded(ItemStack stack) {
        var design = ModularStacks.design(stack);
        if (design == null || ModularStacks.broken(stack))
            return false;
        ModularType type = ModularTypes.known().get(design.type());
        if (type == null)
            return false;
        return switch (type.family()) {
            case SPEAR -> true;
            case BLADE -> heaviness(stack) > TWO_HANDED_BLADE;
            case MACE -> heaviness(stack) > TWO_HANDED_MACE;
            default -> false;
        };
    }

    /** The weapon's heaviness, 0 to 1: the parry window's own measure of mass against reach. */
    public static float heaviness(ItemStack stack) {
        var figures = ModularStacks.figures(stack);
        return figures == null ? 0.5f : ParryTuning.heaviness(figures.get(Fig.MASS), figures.get(Fig.REACH));
    }

    /** Whether {@code stack} can strike with one hand: a strike weapon that is not two-handed. */
    public static boolean isOneHandedStrike(ItemStack stack) {
        return OffhandStrike.strikes(stack) && !isTwoHanded(stack);
    }

    /** A one-handed strike weapon in each hand. */
    public static boolean dualWielding(Player player) {
        return !BareMode.on(player) && isOneHandedStrike(player.getMainHandItem()) && isOneHandedStrike(player.getOffhandItem());
    }

    /** Whether a two-handed weapon in either hand has shut the off hand. */
    public static boolean offHandBlocked(Player player) {
        return !BareMode.on(player) && (isTwoHanded(player.getMainHandItem()) || isTwoHanded(player.getOffhandItem()));
    }
}
