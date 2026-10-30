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
package io.github.soundgoodizerfan.feedback.modular.attunement;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.AttunementKinds;
import io.github.soundgoodizerfan.feedback.modular.StrikeSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The runtime kinds of the Hunger effect, on a head, blade or tip.
 * {@code feed}: each blow costs the wielder a little hunger (exhaustion); a kill with it restores food and
 * saturation. {@code drain} (the mirror, Famine): each blow takes food from a player it hits, and weakens a mob.
 * Armour roles (6b): see {@link #onTick}. No wear of its own.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class HungerKinds {

    /** Exhaustion a blow costs per level of feed (4.0 is one food point). [OPEN] until played. */
    private static final float FEED_EXHAUSTION_PER_BLOW = 0.4f;
    /** Food points and saturation modifier a kill restores per level. [OPEN] until played. */
    private static final int FEED_FOOD_PER_LEVEL = 2;
    private static final float FEED_SATURATION_MODIFIER = 0.5f;
    /** Food points a blow takes from a player per level of drain, and the saturation it strips first. [OPEN] until played. */
    private static final int DRAIN_FOOD_PER_LEVEL = 1;
    private static final float DRAIN_SATURATION_PER_LEVEL = 1f;
    /** Ticks of weakness a blow leaves on a mob per level of drain (an [OPEN] pick: Hunger does nothing to a mob). */
    private static final int DRAIN_WEAKNESS_TICKS_PER_LEVEL = 60;

    /** Reserves: share of exhaustion turned away per level, the most that can be, and ticks between re-reading the armour. [OPEN] until played. */
    private static final float RESERVE_PER_LEVEL = 0.1f, RESERVE_CAP = 0.5f;
    private static final int RESERVE_RECHECK = 20;
    /** Famine thorns: same figures as drain, for a blow struck at the wearer. */
    private static final int FAMINE_FOOD_PER_LEVEL = 1;
    private static final float FAMINE_SATURATION_PER_LEVEL = 1f;
    private static final int FAMINE_WEAKNESS_TICKS_PER_LEVEL = 60;
    private static final String RESERVE_LEVEL = "feedback_reserve", LAST_EXHAUSTION = "feedback_reserve_last";

    private HungerKinds() {
    }

    public static void register() {
        AttunementKinds.onStrike("feed", (event, a) -> {
            if (event.getSource().getEntity() instanceof Player wielder && !wielder.level().isClientSide)
                wielder.causeFoodExhaustion(FEED_EXHAUSTION_PER_BLOW * a.level());
        });
        AttunementKinds.onStrike("drain", (event, a) -> {
            if (event.getEntity() instanceof Player victim) {
                var food = victim.getFoodData();
                food.setSaturation(Math.max(0f, food.getSaturationLevel() - DRAIN_SATURATION_PER_LEVEL * a.level()));
                food.setFoodLevel(Math.max(0, food.getFoodLevel() - DRAIN_FOOD_PER_LEVEL * a.level()));
            } else {
                event.getEntity().addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DRAIN_WEAKNESS_TICKS_PER_LEVEL * a.level()));
            }
        });
        AttunementKinds.onStruck("worn_famine", (attacker, a) -> {
            if (attacker instanceof Player p) {
                var food = p.getFoodData();
                food.setSaturation(Math.max(0f, food.getSaturationLevel() - FAMINE_SATURATION_PER_LEVEL * a.level()));
                food.setFoodLevel(Math.max(0, food.getFoodLevel() - FAMINE_FOOD_PER_LEVEL * a.level()));
            } else {
                attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, FAMINE_WEAKNESS_TICKS_PER_LEVEL * a.level()));
            }
        });
    }

    /**
     * Armour {@code plate}/{@code lining}/{@code studs}: {@code worn_reserve} turns away a share of the exhaustion the
     * wearer gains (sprinting, fighting, mining: every source, [OPEN] whether to narrow it), and {@code worn_famine}
     * (the mirror) takes food from, or weakens, whoever strikes the wearer. The worn level is cached on the player and
     * re-read once a second so the per-tick cost is two NBT reads.
     */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide)
            return;
        var data = p.getPersistentData();
        if (p.tickCount % RESERVE_RECHECK == 0)
            data.putInt(RESERVE_LEVEL, BaitKinds.worn(p, "worn_reserve"));
        int n = data.getInt(RESERVE_LEVEL);
        if (n == 0)
            return;
        var food = p.getFoodData();
        float now = food.getExhaustionLevel(), delta = now - data.getFloat(LAST_EXHAUSTION);
        if (delta > 0f) { // a fall (the bar rolling over) is never a gain
            now -= delta * Math.min(RESERVE_CAP, RESERVE_PER_LEVEL * n);
            food.setExhaustion(now);
        }
        data.putFloat(LAST_EXHAUSTION, now);
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player killer) || killer.level().isClientSide
                || event.getSource().getDirectEntity() != killer)
            return;
        ItemStack weapon = StrikeSource.weapon(killer, event.getSource());
        int n = TideKinds.level(weapon, "feed");
        if (n > 0)
            killer.getFoodData().eat(FEED_FOOD_PER_LEVEL * n, FEED_SATURATION_MODIFIER);
    }
}
