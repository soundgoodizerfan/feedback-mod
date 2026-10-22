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

import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.gear.CombatGear;
import io.github.soundgoodizerfan.feedback.combat.gear.GearStats;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.modular.MaterialColors;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Worn combat gear and gloves on the body, placeholder boxes: a bracer on the left forearm, a sash round the waist, the back piece on the back,
 * gloves on both hands. The Feedbacker is not a box: it shows the right arm of a skin ({@link FeedbackerArm},
 * and {@link FirstPerson} for the wearer's own view). Each box is one cube riding a limb of the humanoid model, so it follows
 * sneaking and swings, drawn in the shared grey plate sprite and tinted by the gear's shell material
 * ({@link MaterialColors}, as {@link FormTint} does for items). A non-humanoid wearer draws nothing.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CurioRenderers {

    private static final ResourceLocation TEXTURE = Feedback.id("textures/item/form/plate.png");
    private static final int LEATHER = 0x8F6B3C;

    private CurioRenderers() {
    }

    private static ModelPart box(float x, float y, float z, float w, float h, float d) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("p", CubeListBuilder.create().texOffs(0, 0).addBox(x, y, z, w, h, d), PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot().getChild("p");
    }

    @SubscribeEvent
    public static void register(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            reg(FItems.BRACER.get(), () -> new Worn(Limb.LEFT_ARM, () -> box(-1.5f, 5f, -2.5f, 5f, 4f, 5f)));
            reg(FItems.FEEDBACKER.get(), FeedbackerArm::new);
            reg(FItems.SASH.get(), () -> new Worn(Limb.BODY, () -> box(-4.4f, 9f, -2.4f, 8.8f, 2f, 4.8f)));
            reg(FItems.BACK_PIECE.get(), () -> new Worn(Limb.BODY, () -> box(-3f, 1f, 2f, 6f, 7f, 2f)));
            reg(FItems.LEATHER_GLOVES.get(), Gloves::new);
        });
    }

    private static void reg(Item item, Supplier<ICurioRenderer> renderer) {
        CuriosRendererRegistry.register(item, renderer);
    }

    private enum Limb { LEFT_ARM, BODY }

    private static void draw(ModelPart box, ModelPart limb, PoseStack pose, MultiBufferSource buffer, int light, int rgb) {
        pose.pushPose();
        limb.translateAndRotate(pose);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        box.render(pose, vc, light, OverlayTexture.NO_OVERLAY, 0xFF000000 | rgb);
        pose.popPose();
    }

    private static final class Worn implements ICurioRenderer {
        private final Limb limb;
        private final Supplier<ModelPart> boxSupplier;
        private ModelPart box;

        Worn(Limb limb, Supplier<ModelPart> box) {
            this.limb = limb;
            this.boxSupplier = box;
        }

        @Override
        public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slot, PoseStack pose,
                RenderLayerParent<T, M> parent, MultiBufferSource buffer, int light, float limbSwing, float limbSwingAmount,
                float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!(parent.getModel() instanceof HumanoidModel<?> m))
                return;
            if (box == null)
                box = boxSupplier.get();
            ModelPart part = switch (limb) {
                case LEFT_ARM -> m.leftArm;
                case BODY -> m.body;
            };
            GearStats g = stack.get(FDataComponents.GEAR.get());
            draw(box, part, pose, buffer, light, g == null ? 0xC0C0C0 : MaterialColors.of(g.shell()));
        }
    }

    private static final ResourceLocation SKIN = Feedback.id("textures/entity/feedbacker_skin.png");
    private static ModelPart skinArm;

    /**
     * The right arm of the Feedbacker's skin: vanilla's 4x12x4 arm box at the skin's right-arm UVs (base at
     * 40,16 grown 0.25, outer layer at 40,32 grown 0.5), so it covers the wearer's own arm and sleeve, slim
     * arms included. Pivot is left at zero: the caller rides it on the humanoid model's right arm.
     */
    private static ModelPart skinArm() {
        if (skinArm == null) {
            MeshDefinition mesh = new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("p", CubeListBuilder.create()
                    .texOffs(40, 16).addBox(-3f, -2f, -2f, 4f, 12f, 4f, new CubeDeformation(0.25f))
                    .texOffs(40, 32).addBox(-3f, -2f, -2f, 4f, 12f, 4f, new CubeDeformation(0.5f)), PartPose.ZERO);
            skinArm = LayerDefinition.create(mesh, 64, 64).bakeRoot().getChild("p");
        }
        return skinArm;
    }

    /** Draws the skin arm in the current pose space, translucent (the outer layer has clear pixels), full light, no tint. */
    private static void drawSkinArm(PoseStack pose, MultiBufferSource buffer, ModelPart limb) {
        pose.pushPose();
        limb.translateAndRotate(pose);
        skinArm().render(pose, buffer.getBuffer(RenderType.entityTranslucent(SKIN)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        pose.popPose();
    }

    /** Third person, self and others: the skin arm riding the right arm. */
    private static final class FeedbackerArm implements ICurioRenderer {
        @Override
        public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slot, PoseStack pose,
                RenderLayerParent<T, M> parent, MultiBufferSource buffer, int light, float limbSwing, float limbSwingAmount,
                float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (parent.getModel() instanceof HumanoidModel<?> m)
                drawSkinArm(pose, buffer, m.rightArm);
        }
    }

    /**
     * First person: the bare-arm draw is cancelled for the local player's right arm while the gauntlet is
     * worn and the skin arm is drawn in its place, posed as {@code PlayerRenderer.renderRightHand} poses the
     * vanilla one. The event only fires for the empty-hand arm, so a held item still renders by its own path.
     */
    @EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
    public static final class FirstPerson {
        @SubscribeEvent
        public static void onArm(RenderArmEvent event) {
            AbstractClientPlayer player = event.getPlayer();
            if (event.getArm() != HumanoidArm.RIGHT || player != Minecraft.getInstance().player
                    || CombatGear.granting(player, ParryTuning.PUNCH_PROJECTILES).isEmpty())
                return;
            if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer r))
                return;
            PlayerModel<AbstractClientPlayer> model = r.getModel();
            model.attackTime = 0f;
            model.crouching = false;
            model.swimAmount = 0f;
            model.setupAnim(player, 0f, 0f, 0f, 0f, 0f);
            model.rightArm.xRot = 0f;
            drawSkinArm(event.getPoseStack(), event.getMultiBufferSource(), model.rightArm);
            event.setCanceled(true);
        }
    }

    /** Hands slot index 0 is the right hand, 1 the left (Curios' own convention). */
    private static final class Gloves implements ICurioRenderer {
        private ModelPart right, left;

        @Override
        public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slot, PoseStack pose,
                RenderLayerParent<T, M> parent, MultiBufferSource buffer, int light, float limbSwing, float limbSwingAmount,
                float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!(parent.getModel() instanceof HumanoidModel<?> m))
                return;
            if (right == null) {
                right = box(-3.3f, 9f, -2.3f, 4.6f, 3.3f, 4.6f);
                left = box(-1.3f, 9f, -2.3f, 4.6f, 3.3f, 4.6f);
            }
            boolean isRight = slot.index() == 0;
            draw(isRight ? right : left, isRight ? m.rightArm : m.leftArm, pose, buffer, light, LEATHER);
        }
    }
}
