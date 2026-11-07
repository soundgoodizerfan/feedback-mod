// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * A sleeping roll: a sheet of cloth or leather, a fibrous fill and a shell. Used on firm ground at night (or in a storm) with nothing
 * hostile near, it skips to morning wherever you are and gives back health by its comfort, which the fill's and the sheet's softness
 * set. It does not set the respawn point, so beds stay worth building. Each night wears it a little. Carried on a pack's bedroll lashing.
 */
public class ModularBedroll extends ModularItem {

    /** The sheet (shaped part), the fill and the shell roles' letters in the type file. */
    public static final String SHEET = "C", FILL = "F", SHELL = "S";

    public ModularBedroll(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack roll = player.getItemInHand(hand);
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp))
            return InteractionResultHolder.pass(roll);
        return rest(sp, roll) ? InteractionResultHolder.success(roll) : InteractionResultHolder.fail(roll);
    }

    /** Beds {@code player} down in {@code roll} (worn a little on success); false with a message when they cannot. */
    public static boolean rest(net.minecraft.server.level.ServerPlayer player, ItemStack roll) {
        if (!(player.level() instanceof ServerLevel server) || ModularStacks.broken(roll))
            return false;
        if (!player.onGround() || !server.dimensionType().natural() || !(server.isNight() || server.isThundering())) {
            player.displayClientMessage(Component.translatable("message.feedback.bedroll.no_sleep"), true);
            return false;
        }
        AABB near = player.getBoundingBox().inflate(ModularTuning.BEDROLL_SAFE_RADIUS, 5, ModularTuning.BEDROLL_SAFE_RADIUS);
        if (!server.getEntitiesOfClass(Monster.class, near, m -> m.isPreventingPlayerRest(player)).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.feedback.bedroll.not_safe"), true);
            return false;
        }
        ModularFigures f = ModularStacks.figures(roll);
        player.heal(player.getMaxHealth() * ModularTuning.BEDROLL_HEAL * (f == null ? 0f : f.get(Fig.COMFORT)));
        player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        // Morning: the next dawn, as a bed would bring it.
        long day = server.getDayTime();
        server.setDayTime(day + 24000L - day % 24000L);
        roll.setDamageValue(Math.min(roll.getMaxDamage() - 1, roll.getDamageValue() + ModularTuning.BEDROLL_WEAR));
        ModularStacks.refresh(roll);
        player.displayClientMessage(Component.translatable("message.feedback.bedroll.slept"), true);
        return true;
    }
}
