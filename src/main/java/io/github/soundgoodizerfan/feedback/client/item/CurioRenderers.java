// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.item;

import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.gear.CombatGear;
import io.github.soundgoodizerfan.feedback.combat.gear.GearStats;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.modular.type.MaterialColors;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
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

    private static final ResourceLocation PACK_TEXTURE = Feedback.id("textures/entity/pack_worn.png");

    /** A cube on the 64x32 pack sheet: {@code u} picks the stitched-fabric half (0) or the seamed half (32). */
    private static ModelPart fbox(int u, float x, float y, float z, float w, float h, float d) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("p", CubeListBuilder.create().texOffs(u, 0).addBox(x, y, z, w, h, d), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32).bakeRoot().getChild("p");
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
            reg(io.github.soundgoodizerfan.feedback.registry.FModular.GLOVE.get(), Gloves::new);
            reg(io.github.soundgoodizerfan.feedback.registry.FModular.BACKPACK.get(), Packs::new);
        });
    }

    private static void reg(Item item, Supplier<ICurioRenderer> renderer) {
        CuriosRendererRegistry.register(item, renderer);
    }

    private enum Limb { LEFT_ARM, BODY }

    private static void draw(ModelPart box, ModelPart limb, PoseStack pose, MultiBufferSource buffer, int light, int rgb) {
        draw(box, limb, pose, buffer, light, rgb, TEXTURE);
    }

    private static void draw(ModelPart box, ModelPart limb, PoseStack pose, MultiBufferSource buffer, int light, int rgb, ResourceLocation texture) {
        pose.pushPose();
        limb.translateAndRotate(pose);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
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
     * 40,16 grown 0.3 (off the player's own 0.25 sleeve, which z-fought at 0.25), outer layer at 40,32 grown 0.55), so it covers the wearer's own arm and sleeve, slim
     * arms included. Pivot is left at zero: the caller rides it on the humanoid model's right arm.
     */
    private static ModelPart skinArm() {
        if (skinArm == null) {
            MeshDefinition mesh = new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("p", CubeListBuilder.create()
                    .texOffs(40, 16).addBox(-3f, -2f, -2f, 4f, 12f, 4f, new CubeDeformation(0.3f))
                    .texOffs(40, 32).addBox(-3f, -2f, -2f, 4f, 12f, 4f, new CubeDeformation(0.55f)), PartPose.ZERO);
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

    /**
     * First person while a playerAnimator clip is playing: the library renders the third-person model in
     * place of the vanilla hand (so {@link RenderArmEvent} never fires) and keeps only layers that are a
     * {@code PlayerItemInHandLayer}, which drops the curio renderer too. This layer is one such, so the skin
     * arm survives the filter; it draws nothing else, and nothing outside that pass (third person is
     * {@link FeedbackerArm}'s).
     */
    private static final class FirstPersonSkinLayer extends PlayerItemInHandLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        FirstPersonSkinLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ItemInHandRenderer items) {
            super(parent, items);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player, float limbSwing,
                float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (FirstPersonMode.isFirstPersonPass() && player == Minecraft.getInstance().player
                    && !CombatGear.granting(player, ParryTuning.PUNCH_PROJECTILES).isEmpty())
                drawSkinArm(pose, buffer, getParentModel().rightArm);
        }
    }

    @EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    static final class Layers {
        @SubscribeEvent
        static void addLayers(EntityRenderersEvent.AddLayers event) {
            for (var skin : event.getSkins())
                if (event.getSkin(skin) instanceof PlayerRenderer r)
                    r.addLayer(new FirstPersonSkinLayer(r, event.getContext().getItemInHandRenderer()));
        }
    }

    /** One cube of the worn pack: drawn in the colour of the material of {@code role}, scaled by {@code shade}. */
    private record Piece(ModelPart part, String role, float shade) {}

    /**
     * The pack on the back: a body sized by its slots with a lid, a front pocket, shoulder straps and a lining, plus whatever is
     * fitted: frame rods up the sides, a side holster, bottle loops, a bandolier across the chest, a folded pad underneath,
     * and a bedroll on top when one rides the lashing. Cubes are cached by shape and fittings; colours come from the materials.
     */
    private static final class Packs implements ICurioRenderer {
        private final java.util.Map<Integer, java.util.List<Piece>> cache = new java.util.HashMap<>();

        private static final String ROLES = "FHBAWRPCQXK";

        private static java.util.List<Piece> build(int slots, int mask, boolean roll) {
            float w = slots <= 9 ? 4 : slots >= 27 ? 8 : 6, h = slots <= 9 ? 4 : slots >= 27 ? 10 : 7, d = slots <= 9 ? 2 : slots >= 27 ? 4 : 3;
            float x = -w / 2, y = slots <= 9 ? 6 : slots >= 27 ? 0 : 3;
            java.util.List<Piece> out = new java.util.ArrayList<>();
            out.add(new Piece(fbox(0, x, y, 2f, w, h, d), "S", 1f));
            out.add(new Piece(fbox(0, x - 0.3f, y - 0.5f, 1.8f, w + 0.6f, 2f, d + 0.4f), "S", 0.75f));
            out.add(new Piece(fbox(0, x + 1f, y + h * 0.45f, 2f + d, w - 2f, h * 0.4f, 0.8f), "S", 0.85f));
            out.add(new Piece(fbox(32, -3.2f, 0f, -2.4f, 1f, 7f, 0.6f), "T", 1f));
            out.add(new Piece(fbox(32, 2.2f, 0f, -2.4f, 1f, 7f, 0.6f), "T", 1f));
            for (int i = 0; i < ROLES.length(); i++) {
                if ((mask & (1 << i)) == 0)
                    continue;
                String r = String.valueOf(ROLES.charAt(i));
                switch (r) {
                    case "F" -> {
                        out.add(new Piece(fbox(32, x - 0.8f, y - 1f, 2f, 0.6f, h + 2f, 0.6f), r, 1f));
                        out.add(new Piece(fbox(32, -x + 0.2f, y - 1f, 2f, 0.6f, h + 2f, 0.6f), r, 1f));
                    }
                    case "H" -> out.add(new Piece(fbox(32, -x, y + 1f, 2.2f, 1.2f, h - 1f, 1.6f), r, 0.8f));
                    case "B" -> {
                        for (int k = 0; k < 3; k++)
                            out.add(new Piece(fbox(32, x - 1.4f, y + 0.5f + k * (h / 3.2f), 2.4f, 1.2f, Math.max(1.2f, h / 4f), 1.2f), r, 0.9f));
                    }
                    case "A" -> out.add(new Piece(fbox(32, -4.2f, 3f, -2.7f, 8.4f, 1f, 0.5f), r, 0.9f));
                    case "W" -> out.add(new Piece(fbox(32, -4.3f, 10f, -2.5f, 8.6f, 1.2f, 5f), r, 0.8f));
                    case "P" -> out.add(new Piece(fbox(32, x + 0.5f, y + 0.5f, 1.7f, w - 1f, h - 1f, 0.4f), r, 1f));
                    case "Q" -> out.add(new Piece(fbox(32, -0.8f, 3.5f, -2.9f, 1.6f, 1.6f, 0.6f), r, 1.4f));
                    case "X" -> out.add(new Piece(fbox(32, -x - 0.2f, y + h - 3f, 2.4f, 0.5f, 2.5f, 2.5f), r, 1.2f));
                    case "K" -> {
                        out.add(new Piece(fbox(32, x + 0.5f, y - 1.2f, 2f + d, 1.2f, 1.2f, 1.2f), r, 0.8f));
                        out.add(new Piece(fbox(32, x + 0.7f, y - 3.4f, 2.2f + d, 0.8f, 2.2f, 0.8f), "K", 1.6f));
                    }
                    case "C" -> out.add(new Piece(fbox(32, x + 0.5f, y + h, 2.2f, w - 1f, 0.8f, d + 0.5f), r, 0.9f));
                    default -> { }
                }
            }
            if (roll)
                out.add(new Piece(fbox(32, x - 0.6f, y - 3.2f, 2.2f, w + 1.2f, 3f, 3f), "R", 1f));
            return out;
        }

        @Override
        public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slot, PoseStack pose,
                RenderLayerParent<T, M> parent, MultiBufferSource buffer, int light, float limbSwing, float limbSwingAmount,
                float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!(parent.getModel() instanceof HumanoidModel<?> m))
                return;
            var design = io.github.soundgoodizerfan.feedback.modular.item.ModularStacks.design(stack);
            var figures = io.github.soundgoodizerfan.feedback.modular.item.ModularStacks.figures(stack);
            float slots = figures == null ? 18f : figures.get(io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig.SLOTS);
            int mask = 0;
            boolean roll = false;
            if (design != null) {
                for (int i = 0; i < Packs.ROLES.length(); i++)
                    if (design.part(String.valueOf(Packs.ROLES.charAt(i))) != null)
                        mask |= 1 << i;
                roll = design.part(io.github.soundgoodizerfan.feedback.modular.part.ModularBackpack.LASHING) != null
                        && stack.getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER,
                                net.minecraft.world.item.component.ItemContainerContents.EMPTY).stream()
                                .anyMatch(s -> s.getItem() instanceof io.github.soundgoodizerfan.feedback.modular.part.ModularBed);
            }
            int key = (slots <= 9 ? 0 : slots >= 27 ? 2 : 1) | mask << 2 | (roll ? 1 << 14 : 0);
            final int fm = mask;
            final boolean fr = roll;
            var pieces = cache.computeIfAbsent(key, k -> build((int) slots, fm, fr));
            for (Piece p : pieces) {
                var part = design == null ? null : design.part(p.role());
                int rgb = part == null ? LEATHER : io.github.soundgoodizerfan.feedback.modular.type.MaterialColors.of(part.material());
                if (p.role().equals("R"))
                    rgb = 0xB9A58A;
                else if (p.role().equals("K") && p.shade() > 1.5f)
                    rgb = 0xFFD070;
                draw(p.part(), m.body, pose, buffer, light, shade(rgb, p.shade()), PACK_TEXTURE);
            }
        }

        private static int shade(int rgb, float k) {
            int r = Math.min(255, (int) ((rgb >> 16 & 255) * k)), g = Math.min(255, (int) ((rgb >> 8 & 255) * k)), b = Math.min(255, (int) ((rgb & 255) * k));
            return r << 16 | g << 8 | b;
        }
    }

    /** The one hands slot holds one glove item, drawn on both hands. */
    private static final class Gloves implements ICurioRenderer {
        private ModelPart right, left;

        @Override
        public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slot, PoseStack pose,
                RenderLayerParent<T, M> parent, MultiBufferSource buffer, int light, float limbSwing, float limbSwingAmount,
                float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!(parent.getModel() instanceof HumanoidModel<?> m))
                return;
            if (right == null) {
                right = box(-3.3f, 7f, -2.3f, 4.6f, 3.3f, 4.6f);
                left = box(-1.3f, 7f, -2.3f, 4.6f, 3.3f, 4.6f);
            }
            var design = io.github.soundgoodizerfan.feedback.modular.item.ModularStacks.design(stack);
            var plate = design == null ? null : design.part(io.github.soundgoodizerfan.feedback.modular.item.ModularGlove.PLATE);
            if (plate == null && design != null)
                plate = design.part("B");
            int rgb = plate == null ? LEATHER : io.github.soundgoodizerfan.feedback.modular.type.MaterialColors.of(plate.material());
            draw(right, m.rightArm, pose, buffer, light, rgb);
            draw(left, m.leftArm, pose, buffer, light, rgb);
        }
    }
}
