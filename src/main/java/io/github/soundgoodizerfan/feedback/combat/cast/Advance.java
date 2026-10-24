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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Advance: march forward behind the guard and shove the line back by weight. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Advance {

    private Advance() {
    }

    private static final Map<UUID, Long> MARCHING = new HashMap<>();

    /** Advance: for three seconds walk forward behind a held guard; everything in front is shoved back by the ratio of your mass to its, the lighter ones staggered. Needs a shield-type item. */
    static boolean act(ServerPlayer p) {
        if (p.getMainHandItem().getUseAnimation() != UseAnim.BLOCK && p.getOffhandItem().getUseAnimation() != UseAnim.BLOCK)
            return false;
        MARCHING.put(p.getUUID(), p.level().getGameTime() + 60);
        CastFx.sound(p.serverLevel(), p.position(), SoundEvents.IRON_GOLEM_REPAIR, 0.9f, 0.6f);
        CombatCues.everyone(p, FSounds.FEEL_HEAVY.get(), FeelTuning.HEAVY_VOLUME, 0.8f);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !MARCHING.containsKey(p.getUUID()))
            return;
        long now = p.level().getGameTime();
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        if (now >= MARCHING.get(p.getUUID()) || look.lengthSqr() < 1e-6 || p.isPassenger()) {
            MARCHING.remove(p.getUUID());
            return;
        }
        Vec3 d = look.normalize();
        p.setDeltaMovement(d.x * 0.16, p.getDeltaMovement().y, d.z * 0.16);
        p.hurtMarked = true;
        float mine = BodyMass.of(p) + BodyMass.armour(p);
        ServerLevel lv = p.serverLevel();
        for (LivingEntity e : near(p, p.position().add(d.scale(1.2)).add(0, 0.9, 0), 1.2)) {
            float theirs = Math.max(1f, BodyMass.of(e) + BodyMass.armour(e));
            double shove = Mth.clamp(mine / theirs, 0.2, 2.0) * 0.35;
            e.push(d.x * shove, 0.05, d.z * shove);
            e.hurtMarked = true;
            if (now % 10 == 0 && mine > theirs * 0.6)
                stagger(p, e, mine * 9.8f * 0.5f);
        }
        if (now % 3 == 0) {
            Vec3 front = p.position().add(d.scale(0.9)).add(0, 0.8, 0);
            CastFx.ring(lv, CastFx.dust(CastFx.STEEL, 1.1f), front, 0.5, 8, 0);
            CastFx.sound(lv, p.position(), SoundEvents.IRON_GOLEM_STEP, 0.7f, 0.8f);
        }
    }
}
