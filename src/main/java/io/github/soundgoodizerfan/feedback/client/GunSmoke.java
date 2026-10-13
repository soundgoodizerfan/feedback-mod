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
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * A hot gun smokes where it is held (spec 3.4: heat has to be something a hand and an eye can
 * tell). Every player on the client with a gun in their main hand, not only the local one, so a
 * companion's smoking gun is as visible as your own. The rate and the weight of the smoke follow
 * the barrel's heat as a fraction of its propellant's ignition temperature, so "smoking" on the
 * HUD and smoke in the world are the same figure read two ways.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class GunSmoke {

    private GunSmoke() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused())
            return;
        var materials = ClientMaterials.on(mc.level);
        for (Player player : mc.level.players()) {
            var gun = player.getMainHandItem();
            if (!(gun.getItem() instanceof GunItem))
                continue;
            float ignition = GunAmmo.next(gun).flatMap(r -> r.load(materials)).map(l -> l.ignitionTu())
                    .orElse(FTuning.GUN_PROPELLANT_IGNITION_TU);
            float heat = GunCondition.heatFraction(gun, mc.level, ignition);
            if (heat < 0.6f)
                continue;
            // Warm and a wisp now and then; at the ignition temperature a plume every tick.
            float chance = Mth.clamp((heat - 0.5f) * 2.2f, 0f, 1f);
            if (player.getRandom().nextFloat() > chance)
                continue;
            Vec3 at = hand(player);
            double drift = 0.01 + 0.02 * heat;
            mc.level.addParticle(heat >= 0.9f ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
                    at.x, at.y, at.z,
                    (player.getRandom().nextFloat() - 0.5) * drift, 0.02 + 0.03 * heat,
                    (player.getRandom().nextFloat() - 0.5) * drift);
        }
    }

    /** Where the main hand is: a little out to the side and forward of the shoulder, at waist height. */
    private static Vec3 hand(Player player) {
        float yaw = (float) Math.toRadians(player.yBodyRot);
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        double side = player.getMainArm() == HumanoidArm.RIGHT ? 0.32 : -0.32;
        // A gun held up to aim is higher and nearer the face: follow the pitch a little.
        double height = player.getBbHeight() * 0.62 - Mth.clamp(player.getXRot(), -90f, 90f) * 0.004;
        return player.position().add(0, height, 0).add(right.scale(side)).add(forward.scale(0.5));
    }
}
