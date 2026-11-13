// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.attunement;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.bake.MaterialPerks;

import net.minecraft.server.level.ServerPlayer;
import java.util.List;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * What an attuned container of a dish, soup, pie, bread, skewer or wrap does for the food in it. Rot runs through
 * {@link #rotShare} (the lazy clock in {@code Cooking.current}), and eating the dish runs the rest. Held, the container's
 * lure, glow and the like act through the main-hand readers like any stack's.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class FoodKinds {

    /** Share of the rot rate one level turns away (a seal, a pest ward, a cure field, a lamp's light) or adds (kin of pests, a warm container). Placeholder. */
    private static final float SLOW_PER_LEVEL = 0.15f, QUICKEN_PER_LEVEL = 0.15f, SLOW_FLOOR = 0.2f;
    /** Food points one level of nourish adds or thin takes; experience points one level of tithe pays a meal; durability one level of restore closes on worn gear. */
    private static final int FOOD_PER_LEVEL = 1, TITHE_XP_PER_LEVEL = 3, RESTORE_PER_LEVEL = 2;

    /** Ticks of effect a level gives; saturation per level; reaches in blocks; share of a level of echo. Placeholder. */
    private static final int EFFECT_TICKS = 200;
    private static final float SATURATION_PER_LEVEL = 1f, ECHO_PER_LEVEL = 0.25f, FORTUNE_PER_LEVEL = 0.3f;
    private static final double DRAW_REACH = 4.0, MARK_REACH = 16, SHARE_REACH = 8, RING_PER_LEVEL = 6, LAUNCH_PER_LEVEL = 0.45;

    private FoodKinds() {
    }

    private static int level(ItemStack stack, String kind) {
        int n = 0;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(stack))
            if (a.kind().equals(kind))
                n += a.level();
        return n;
    }

    /** Multiplier on a dish's rot rate from its container: sealed, pest-warded, cured or lit slows it; pests drawn or heat held speeds it. 1 for a plain container. */
    public static float rotShare(ItemStack dish) {
        if (dish.isEmpty())
            return 1f;
        int slow = 0, quick = 0, insulate = 0;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(dish))
            switch (a.kind()) {
                case "seal", "pest_ward", "cure", "glow", "insulate" -> slow += a.level();
                case "pest_lure", "burn_wielder" -> quick += a.level();
                default -> {
                }
            }
        return Math.max(SLOW_FLOOR, 1f - SLOW_PER_LEVEL * slow + QUICKEN_PER_LEVEL * quick);
    }

    /** Eating a dish: nourish and thin change the meal's food points, restore closes wear on what the eater wears, tithe pays experience. */
    @SubscribeEvent
    public static void onEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        ItemStack dish = event.getItem();
        if (dish.isEmpty() || MaterialPerks.attunements(dish).isEmpty())
            return;
        int nourish = level(dish, "nourish"), thin = level(dish, "thin"), restore = level(dish, "restore"), tithe = level(dish, "tithe_meal");
        if (nourish != thin)
            player.getFoodData().setFoodLevel(Math.max(0, Math.min(20, player.getFoodData().getFoodLevel() + (nourish - thin) * FOOD_PER_LEVEL)));
        if (restore > 0)
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack worn = player.getItemBySlot(slot);
                if (worn.isDamaged() && slot != EquipmentSlot.BODY)
                    worn.setDamageValue(Math.max(0, worn.getDamageValue() - restore * RESTORE_PER_LEVEL));
            }
        if (tithe > 0)
            ExperienceOrb.award((ServerLevel) player.level(), player.position(), tithe * TITHE_XP_PER_LEVEL);
        meal((ServerLevel) player.level(), player, dish);
    }

    /** The rest of what an eaten container does: effects on the eater, items drawn, creatures marked, a call, a coin. */
    private static void meal(ServerLevel level, ServerPlayer p, ItemStack dish) {
        var fx = effectTicks(dish);
        fx.forEach((e, ticks) -> p.addEffect(new MobEffectInstance(e, ticks)));
        int sat = level(dish, "meal_stick") - level(dish, "meal_slick");
        if (sat != 0)
            p.getFoodData().setSaturation(Math.max(0f, Math.min(p.getFoodData().getFoodLevel(), p.getFoodData().getSaturationLevel() + sat * SATURATION_PER_LEVEL)));
        int echo = level(dish, "meal_echo") - level(dish, "meal_lag");
        if (echo != 0)
            for (MobEffectInstance e : new java.util.ArrayList<>(p.getActiveEffects()))
                p.addEffect(new MobEffectInstance(e.getEffect(), Math.max(20, Math.round(e.getDuration() * (1f + ECHO_PER_LEVEL * echo))), e.getAmplifier(),
                        e.isAmbient(), e.isVisible(), e.showIcon()));
        if (level(dish, "meal_calm") > 0)
            for (var bad : List.of(MobEffects.POISON, MobEffects.CONFUSION, MobEffects.WEAKNESS, MobEffects.HUNGER))
                p.removeEffect(bad);
        int draw = level(dish, "meal_draw") - level(dish, "meal_fling");
        if (draw != 0)
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(DRAW_REACH * Math.abs(draw)))) {
                Vec3 to = p.position().subtract(item.position()).normalize().scale(draw > 0 ? 0.6 : -0.6);
                item.setDeltaMovement(to.x, 0.2, to.z);
            }
        int mark = level(dish, "meal_mark"), taunt = level(dish, "meal_taunt");
        if (mark + taunt > 0)
            for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(MARK_REACH), m -> m instanceof Enemy)) {
                if (mark > 0)
                    MarkKinds.mark(m, mark);
                if (taunt > 0)
                    m.setTarget(p);
            }
        int share = level(dish, "meal_share"), alone = level(dish, "meal_alone");
        if (share + alone > 0) {
            var others = level.getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(SHARE_REACH), o -> o != p);
            for (ServerPlayer o : others)
                o.getFoodData().eat(share * FOOD_PER_LEVEL, 0.3f);
            if (others.isEmpty() && alone > 0)
                p.getFoodData().eat(alone * FOOD_PER_LEVEL * 2, 0.3f);
        }
        int pulse = level(dish, "meal_pulse");
        if (pulse > 0)
            PiezoKinds.strain(level, p.blockPosition(), java.util.Map.of("pulse", pulse), 1f, 0f);
        int ring = level(dish, "meal_ring");
        if (ring > 0) {
            level.gameEvent(p, GameEvent.EXPLODE, p.position());
            for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(RING_PER_LEVEL * ring), m -> m instanceof Enemy && m.getTarget() == null))
                m.getNavigation().moveTo(p.getX(), p.getY(), p.getZ(), 1.0);
        }
        int release = level(dish, "meal_release");
        if (release > 0) {
            p.setDeltaMovement(p.getDeltaMovement().add(0, LAUNCH_PER_LEVEL * release, 0));
            p.hurtMarked = true;
        }
        int gamble = level(dish, "meal_gamble"), fortune = level(dish, "meal_fortune");
        if (gamble > 0 && level.random.nextFloat() < 0.5f)
            p.getFoodData().eat(gamble * FOOD_PER_LEVEL * 4, 0.3f);
        if (fortune > 0 && level.random.nextFloat() < FORTUNE_PER_LEVEL * fortune)
            p.getFoodData().eat(FOOD_PER_LEVEL * 4, 0.3f);
    }

    /** The potion effects an eaten container's kinds give, as effect to ticks. */
    private static java.util.Map<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>, Integer> effectTicks(ItemStack dish) {
        var out = new java.util.HashMap<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>, Integer>();
        for (var k : EFFECTS.entrySet()) {
            int n = level(dish, k.getKey());
            if (n > 0)
                out.merge(k.getValue(), EFFECT_TICKS * n, Integer::sum);
        }
        return out;
    }

    private static final java.util.Map<String, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> EFFECTS = java.util.Map.of(
            "meal_grow", MobEffects.REGENERATION, "meal_reap", MobEffects.HUNGER, "meal_edge", MobEffects.DAMAGE_BOOST,
            "meal_pad", MobEffects.ABSORPTION, "meal_grudge", MobEffects.DIG_SPEED, "meal_swift", MobEffects.MOVEMENT_SPEED,
            "meal_brace", MobEffects.DAMAGE_RESISTANCE, "meal_spring", MobEffects.JUMP);
}
