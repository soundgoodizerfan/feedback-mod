// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.soundgoodizerfan.feedback.modular.MaterialColors;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * A modular shield drawn on vanilla's shield model: the plate and handle are vanilla's own cubes (stretched to the face
 * variant's size), tinted by the face and grip materials, with a rim frame of extra cubes round the plate in the rim's
 * material. Uses a greyscale copy of vanilla's plain shield texture (tinted per part); display transforms and the blocking pose are vanilla's
 * ({@code models/item/shield.json}, {@code shield_blocking.json}).
 */
public final class ShieldRenderer extends BlockEntityWithoutLevelRenderer {

    private static final ResourceLocation TEXTURE = io.github.soundgoodizerfan.feedback.Feedback.id("textures/entity/modular_shield.png");
    private static ShieldRenderer instance;
    private ShieldModel model;

    private ShieldRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    /** Lets the shield item draw through this renderer. */
    public static final class Extension implements IClientItemExtensions {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (instance == null)
                instance = new ShieldRenderer();
            return instance;
        }
    }

    /** Vanilla's plate is 12 by 22; a face variant gives it its outline and the face's {@code face_size} axis scales it. */
    private static float[] size(ModularDesign design, ModularDesign.Part face) {
        if (face == null)
            return new float[]{1f, 1f};
        float[] shape = switch (face.variant()) {
            case "kite" -> new float[]{0.9f, 1.15f};
            case "heater" -> new float[]{1f, 1.1f};
            case "rectangular" -> new float[]{0.95f, 1.2f};
            default -> new float[]{1f, 1f};
        };
        ModularType type = ModularTypes.known(design.type());
        ModularType.Role role = type == null ? null : type.roles().get("F");
        ModularType.Axis axis = role == null ? null : role.axes().get("face_size");
        float scale = axis == null ? 1f : Math.max(0.6f, Math.min(1.6f, axis.clamp(face.axis("face_size", axis.def())) / axis.def()));
        return new float[]{shape[0] * scale, shape[1] * scale};
    }

    /** A cube of the rim frame, drawn from the handle's plain region of the sheet. */
    private static ModelPart cube(float x, float y, float z, float w, float h, float d) {
        return CUBES.computeIfAbsent(java.util.List.of(x, y, z, w, h, d), k -> bake(x, y, z, w, h, d));
    }

    private static final java.util.Map<java.util.List<Float>, ModelPart> CUBES = new java.util.HashMap<>();

    private static ModelPart bake(float x, float y, float z, float w, float h, float d) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("p", CubeListBuilder.create().texOffs(26, 0).addBox(x, y, z, w, h, d), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64).bakeRoot().getChild("p");
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                             int light, int overlay) {
        if (model == null)
            model = new ShieldModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHIELD));
        ModularDesign design = ModularStacks.design(stack);
        ModularDesign.Part face = design == null ? null : design.part("F"), rim = design == null ? null : design.part("R"),
                grip = design == null ? null : design.part("G");
        int faceColour = face == null ? 0xFFFFFFFF : 0xFF000000 | MaterialColors.of(face.material());
        int rimColour = rim == null ? faceColour : 0xFF000000 | MaterialColors.of(rim.material());
        int gripColour = grip == null ? faceColour : 0xFF000000 | MaterialColors.of(grip.material());
        float[] size = size(design, face);
        float w = 12 * size[0], h = 22 * size[1];
        float t = rim != null && "flanged".equals(rim.variant()) ? 1.5f : 1f;

        pose.pushPose();
        pose.scale(1f, -1f, -1f);
        VertexConsumer buffer = ItemRenderer.getFoilBufferDirect(buffers, RenderType.entityCutoutNoCull(TEXTURE), true, stack.hasFoil());
        model.handle().render(pose, buffer, light, OverlayTexture.NO_OVERLAY, gripColour);
        pose.pushPose();
        pose.scale(size[0], size[1], 1f);
        model.plate().render(pose, buffer, light, OverlayTexture.NO_OVERLAY, faceColour);
        pose.popPose();
        // The rim frame stands proud of the plate by its band, deeper when flanged.
        float z0 = -2f - (t > 1f ? 0.75f : 0.25f), depth = t > 1f ? 2.5f : 1.5f;
        cube(-w / 2 - t, -h / 2 - t, z0, w + 2 * t, t, depth).render(pose, buffer, light, OverlayTexture.NO_OVERLAY, rimColour);
        cube(-w / 2 - t, h / 2, z0, w + 2 * t, t, depth).render(pose, buffer, light, OverlayTexture.NO_OVERLAY, rimColour);
        cube(-w / 2 - t, -h / 2, z0, t, h, depth).render(pose, buffer, light, OverlayTexture.NO_OVERLAY, rimColour);
        cube(w / 2, -h / 2, z0, t, h, depth).render(pose, buffer, light, OverlayTexture.NO_OVERLAY, rimColour);
        pose.popPose();
    }
}
