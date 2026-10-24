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
 */
package io.github.soundgoodizerfan.feedback.modular.attunement;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.MaterialPerks;
import static io.github.soundgoodizerfan.feedback.modular.AttunementKinds.onStrike;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The runtime kinds of the Mark effect ({@code docs/feedback_enchant_roster.md} 22). {@code mark}: a blow marks its
 * target, which glows, and the next blow on it from anyone does extra. {@code taunt}: the mirror, a worn part that
 * marks its wearer, so hostile mobs nearby turn on them. Mark state lives on the target's persistent data (a
 * timestamped bonus), so it survives nothing worse than a reload.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class MarkKinds {

    /** Ticks a mark lasts. [OPEN] until played. */
    private static final int MARK_TICKS = 200;
    /** Extra damage share the next blow does, per level, and the most that can be. [OPEN] until played. */
    private static final float MARK_BONUS_PER_LEVEL = 0.15f, MARK_BONUS_CAP = 0.6f;
    /** Taunt: blocks around the wearer mobs are pulled from, base and per level; and ticks between pulls. [OPEN] until played. */
    private static final double TAUNT_RADIUS_BASE = 4.0, TAUNT_RADIUS_PER_LEVEL = 2.0, TAUNT_RADIUS_CAP = 14.0;
    private static final int TAUNT_PERIOD = 20;

    private static final String UNTIL = "feedback_mark_until", SET = "feedback_mark_set", BONUS = "feedback_mark_bonus";

    private MarkKinds() {
    }

    public static void register() {
        onStrike("mark", (event, a) -> {
            LivingEntity victim = event.getEntity();
            CompoundTag data = victim.getPersistentData();
            long now = victim.level().getGameTime();
            // Several marking parts on one blow add up; a fresh blow replaces an old mark.
            float have = data.getLong(SET) == now ? data.getFloat(BONUS) : 0f;
            data.putLong(UNTIL, now + MARK_TICKS);
            data.putLong(SET, now);
            data.putFloat(BONUS, Math.min(MARK_BONUS_CAP, have + a.level() * MARK_BONUS_PER_LEVEL));
            victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, MARK_TICKS, 0, false, false));
        });
    }

    /** The next blow on a marked target, from anyone, does extra and spends the mark. */
    @SubscribeEvent
    public static void onMarked(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide)
            return;
        CompoundTag data = victim.getPersistentData();
        if (!data.contains(UNTIL))
            return;
        long now = victim.level().getGameTime();
        if (now <= data.getLong(SET))
            return; // the marking blow itself
        float bonus = now <= data.getLong(UNTIL) ? data.getFloat(BONUS) : 0f;
        data.remove(UNTIL);
        data.remove(SET);
        data.remove(BONUS);
        if (bonus > 0f) {
            event.setAmount(event.getAmount() * (1f + bonus));
            victim.removeEffect(MobEffects.GLOWING);
        }
    }

    /** A worn taunt part turns hostile mobs in range onto the wearer. */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % TAUNT_PERIOD != 0
                || p.isCreative() || p.isSpectator())
            return;
        int level = 0;
        for (ItemStack worn : p.getArmorSlots())
            if (!worn.isEmpty())
                for (MaterialPerks.Attunement a : MaterialPerks.attunements(worn))
                    if (a.kind().equals("taunt"))
                        level += a.level();
        if (level == 0)
            return;
        double r = Math.min(TAUNT_RADIUS_CAP, TAUNT_RADIUS_BASE + TAUNT_RADIUS_PER_LEVEL * level);
        for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(r),
                m -> m instanceof Enemy && m.getTarget() != p && m.canAttack(p)))
            m.setTarget(p);
    }
}
