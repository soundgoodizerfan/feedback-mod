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
package io.github.soundgoodizerfan.feedback.client;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Draws the kicking leg of a player, lifted and straight, in the skin. {@link CombatPose} hides the
 * ordinary leg for the kick's frames (the model's own animation would overwrite any rotation set
 * before it), so this layer, which runs after that animation, puts it back in the kick pose. Armour
 * on the kicking leg stays hanging: placeholder.
 */
final class KickLegLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    KickLegLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer p, float limbSwing,
            float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        float k = CombatPose.kick(p, partialTicks);
        if (k <= 0f || p.isInvisible())
            return;
        PlayerModel<AbstractClientPlayer> m = getParentModel();
        boolean right = p.getMainArm() == HumanoidArm.RIGHT;
        var leg = right ? m.rightLeg : m.leftLeg;
        var pants = right ? m.rightPants : m.leftPants;
        leg.xRot = -CombatPoseTuning.KICK_LIFT_RAD * k;
        leg.yRot = 0f;
        leg.zRot = 0f;
        pants.copyFrom(leg);
        var vc = buffer.getBuffer(RenderType.entityCutoutNoCull(p.getSkin().texture()));
        leg.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
        pants.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
    }

    @EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    static final class Register {
        @SubscribeEvent
        static void addLayers(EntityRenderersEvent.AddLayers event) {
            for (var skin : event.getSkins())
                if (event.getSkin(skin) instanceof PlayerRenderer r)
                    r.addLayer(new KickLegLayer(r));
        }
    }
}
