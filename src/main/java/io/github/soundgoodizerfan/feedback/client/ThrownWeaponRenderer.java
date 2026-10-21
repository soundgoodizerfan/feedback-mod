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

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.modular.ThrownWeapon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * A thrown modular weapon: its own item model, laid along its flight rather than turned to face
 * the camera, with a harpoon's rope drawn from the thrower's hand.
 *
 * <p>The pose is Spartan Weaponry's {@code ThrowingWeaponRenderer} (Apache-2.0), the idea taken and
 * written again: yaw and pitch follow the velocity, and the sprite's diagonal is turned onto the
 * flight axis (a flat item is drawn corner to corner, so its tip is a diagonal, not an edge). What
 * is added: a tumbling weapon turns end over end by {@link ThrownWeapon#spin}, and one stuck in a
 * block keeps the turn it stuck at and shakes as a vanilla arrow does. The rope is
 * vanilla's fishing line, drawn the same way, brown, with a slack that grows with the span.
 */
public class ThrownWeaponRenderer extends EntityRenderer<ThrownWeapon> {

    private static final int ROPE_COLOUR = 0xFF7A5A32;
    private static final int SEGMENTS = 16;

    private final ItemRenderer items;

    public ThrownWeaponRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemRenderer();
    }

    @Override
    public boolean shouldRender(ThrownWeapon entity, Frustum frustum, double x, double y, double z) {
        return entity.ropeLength() > 0f || super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void render(ThrownWeapon entity, float entityYaw, float partial, PoseStack pose, MultiBufferSource buffer, int light) {
        ItemStack stack = entity.stack();
        if (!stack.isEmpty()) {
            pose.pushPose();
            pose.scale(2f, 2f, 2f);
            pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial, entity.yRotO, entity.getYRot()) - 90f));
            float shake = entity.shakeTime - partial;
            float wobble = shake > 0f ? -Mth.sin(shake * 3f) * shake : 0f;
            float pitch = Mth.lerp(partial, entity.xRotO, entity.getXRot());
            pose.mulPose(Axis.ZP.rotationDegrees(pitch - 45f - entity.spin(partial) + wobble));
            pose.mulPose(new Quaternionf().setAngleAxis(Mth.PI, Mth.SQRT_OF_TWO / 2f, Mth.SQRT_OF_TWO / 2f, 0f));
            pose.translate(-0.10, -0.20, 0.0);
            BakedModel model = items.getModel(stack, entity.level(), null, entity.getId());
            items.render(stack, ItemDisplayContext.GROUND, false, pose, buffer, light, OverlayTexture.NO_OVERLAY, model);
            pose.popPose();
        }
        if (entity.ropeLength() > 0f && entity.getOwner() != null)
            rope(entity, entity.getOwner(), partial, pose, buffer);
        super.render(entity, entityYaw, partial, pose, buffer, light);
    }

    /** The line from the thrower's hand to the weapon, as a sagging strip. */
    private void rope(ThrownWeapon entity, Entity owner, float partial, PoseStack pose, MultiBufferSource buffer) {
        Vec3 hand = handPosition(owner, partial);
        Vec3 end = entity.getPosition(partial).add(0, entity.getBbHeight() * 0.5, 0);
        float dx = (float) (hand.x - end.x);
        float dy = (float) (hand.y - end.y);
        float dz = (float) (hand.z - end.z);
        float sag = Math.min(1.2f, Mth.sqrt(dx * dx + dy * dy + dz * dz) * 0.08f);
        VertexConsumer lines = buffer.getBuffer(RenderType.lineStrip());
        pose.pushPose();
        pose.translate(0, entity.getBbHeight() * 0.5, 0);
        PoseStack.Pose last = pose.last();
        for (int i = 0; i <= SEGMENTS; i++)
            ropeVertex(dx, dy, dz, sag, lines, last, i / (float) SEGMENTS, (i + 1) / (float) SEGMENTS);
        pose.popPose();
    }

    private static void ropeVertex(float dx, float dy, float dz, float sag, VertexConsumer lines, PoseStack.Pose pose, float t, float next) {
        float x = dx * t;
        float y = dy * t - sag * 4f * t * (1f - t);
        float z = dz * t;
        float nx = dx * next - x;
        float ny = dy * next - sag * 4f * next * (1f - next) - y;
        float nz = dz * next - z;
        float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0f) {
            nx /= len;
            ny /= len;
            nz /= len;
        } else {
            nx = 0f;
            ny = 1f;
            nz = 0f;
        }
        lines.addVertex(pose, x, y, z).setColor(ROPE_COLOUR).setNormal(pose, nx, ny, nz);
    }

    /**
     * Where the thrower's hand is: the camera's near plane for the local player in first person,
     * else a point beside and ahead of the body. Vanilla's {@code FishingHookRenderer} has the same
     * two cases; nothing else in the game knows where a hand is.
     */
    private Vec3 handPosition(Entity owner, float partial) {
        if (!(owner instanceof Player player))
            return owner.getEyePosition(partial).add(0, -0.45, 0);
        int side = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        if (entityRenderDispatcher.options.getCameraType().isFirstPerson() && player == Minecraft.getInstance().player) {
            double fov = 960.0 / entityRenderDispatcher.options.fov().get().intValue();
            Vec3 near = entityRenderDispatcher.camera.getNearPlane().getPointOnPlane(side * 0.525f, -0.1f).scale(fov);
            return player.getEyePosition(partial).add(near);
        }
        float body = Mth.lerp(partial, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        double sin = Mth.sin(body);
        double cos = Mth.cos(body);
        float scale = player.getScale();
        double sideways = side * 0.35 * scale;
        double forward = 0.8 * scale;
        float crouch = player.isCrouching() ? -0.1875f : 0f;
        return player.getEyePosition(partial).add(-cos * sideways - sin * forward, crouch - 0.45 * scale, -sin * sideways + cos * forward);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownWeapon entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
