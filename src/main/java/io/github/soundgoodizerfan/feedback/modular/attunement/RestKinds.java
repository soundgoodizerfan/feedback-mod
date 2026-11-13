// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.attunement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * What an attuned bed or bedroll does for the night's rest ({@link io.github.soundgoodizerfan.feedback.modular.part.ModularBed#soothe}): its
 * mattress and frame add to comfort and warmth, change the healing, and give the sleeper something on waking. The kinds that act on the world around
 * a placed bed (lure, scare, grow, ring, hush, bane, mark, grudge, reserve, coil, shed and the rest) run through {@link MachineAura}, which reads a
 * placed bed's design like a machine's.
 */
public final class RestKinds {

    /** Comfort or warmth a level adds or takes; share of the healing a level of echo adds; food points a level of feed gives or drain takes. Placeholder. */
    public static final float COMFORT_PER_LEVEL = 0.08f, WARMTH_PER_LEVEL = 0.15f, ECHO_PER_LEVEL = 0.25f;
    private static final int FEED_PER_LEVEL = 2, MEND_PER_LEVEL = 3, TITHE_XP_PER_LEVEL = 3, GLOW_TICKS = 400, SWIFT_TICKS = 1200, DOUSE_FIRE_TICKS = 1200;
    private static final double DRAW_REACH = 4.0, LAUNCH_PER_LEVEL = 0.45;
    /** Chance a level of fortune leaves the night unworn; chance of the gamble's win. */
    private static final float FORTUNE_PER_LEVEL = 0.3f, WIN = 0.5f;

    private RestKinds() {
    }

    private static int lv(ItemStack bed, String kind) {
        return TideKinds.level(bed, kind);
    }

    /** Comfort added to the bed's own: pad and snug add, a slipping frame takes. */
    public static float comfort(ItemStack bed) {
        return COMFORT_PER_LEVEL * (lv(bed, "bed_pad") + lv(bed, "bed_snug") - lv(bed, "bed_slip"));
    }

    /** Warmth added to the bed's own: a warm mattress keeps more in, a cool one less. */
    public static float warmth(ItemStack bed) {
        return WARMTH_PER_LEVEL * (lv(bed, "bed_warm") - lv(bed, "bed_cool"));
    }

    /** Multiplier on the night's healing: echo repeats the rest, a gamble doubles it or loses it. */
    public static float heal(ItemStack bed, ServerPlayer player) {
        float m = 1f + ECHO_PER_LEVEL * lv(bed, "bed_echo");
        if (lv(bed, "bed_gamble") > 0)
            m *= player.getRandom().nextFloat() < WIN ? 2f : 0f;
        return m;
    }

    /** Whether the night leaves the bed unworn (a fortune's roll). */
    public static boolean unworn(ItemStack bed, ServerPlayer player) {
        return player.getRandom().nextFloat() < FORTUNE_PER_LEVEL * lv(bed, "bed_fortune");
    }

    /** What the sleeper gets on waking. */
    public static void wake(ServerPlayer player, ServerLevel level, ItemStack bed) {
        int feed = lv(bed, "bed_feed") - lv(bed, "bed_drain");
        if (feed != 0)
            player.getFoodData().setFoodLevel(Math.max(0, Math.min(20, player.getFoodData().getFoodLevel() + feed * FEED_PER_LEVEL)));
        int mend = lv(bed, "bed_mend");
        if (mend > 0)
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack worn = player.getItemBySlot(slot);
                if (worn.isDamaged() && slot != EquipmentSlot.BODY)
                    worn.setDamageValue(Math.max(0, worn.getDamageValue() - mend * MEND_PER_LEVEL));
            }
        int tithe = lv(bed, "bed_tithe");
        if (tithe > 0)
            ExperienceOrb.award(level, player.position(), tithe * TITHE_XP_PER_LEVEL);
        int glow = lv(bed, "bed_glow");
        if (glow > 0)
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, GLOW_TICKS * glow));
        int swift = lv(bed, "bed_swift"), brace = lv(bed, "bed_brace");
        if (swift > 0)
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, SWIFT_TICKS * swift));
        if (brace > 0)
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, SWIFT_TICKS * brace));
        int douse = lv(bed, "bed_douse"), cool = lv(bed, "bed_cool");
        if (douse > 0 || cool > 0) {
            player.clearFire();
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, DOUSE_FIRE_TICKS * (douse + cool)));
        }
        int pulse = lv(bed, "bed_pulse");
        if (pulse > 0)
            PiezoKinds.strain(level, player.getSleepingPos().orElse(player.blockPosition()), java.util.Map.of("pulse", pulse), 1f, 0f);
        int draw = lv(bed, "bed_draw") - lv(bed, "bed_fling");
        if (draw != 0)
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(DRAW_REACH * Math.abs(draw)))) {
                Vec3 to = player.position().subtract(item.position()).normalize().scale(draw > 0 ? 0.6 : -0.6);
                item.setDeltaMovement(to.x, 0.2, to.z);
            }
        int launch = lv(bed, "bed_launch");
        if (launch > 0) {
            player.setDeltaMovement(player.getDeltaMovement().add(0, LAUNCH_PER_LEVEL * launch, 0));
            player.hurtMarked = true;
        }
    }
}
