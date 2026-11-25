// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.Fitting;
import io.github.soundgoodizerfan.feedback.fitting.FittingKinds;
import io.github.soundgoodizerfan.feedback.fitting.FittingLooks;
import io.github.soundgoodizerfan.feedback.fitting.SidedFitting;
import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws the fittings of any {@link Fittable} holder, from the two look tables ({@link FittingLooks}): a fitting kind's model, put where
 * its holder's anchors say. A holder with no anchors row draws nothing, and a fitting kind with no model row is not drawn, so the
 * machinery costs nothing until a holder and a kind are given a row.
 * <p>
 * A holder's block entity type has one renderer, so a holder that already has its own (a shaft, a vessel, a crucible) calls
 * {@link #draw} from it; every other {@code Fittable} type is registered with this class directly. Models are the plain element models
 * under {@code assets/<ns>/models/fitting/}; each is loaded as a standalone model at model registration, so a file dropped there needs no
 * code. A model is authored as if standing on the top face of a block (its base centre at 8, 0, 8 of its own 16-unit box): a sided
 * fitting is turned onto its face about the block's centre, and an anchor's position is where that base centre goes.
 * <p>
 * Per frame a holder with no fittings costs one {@link Fittable#hasFittings} check and nothing else (no registry lookup). For one with
 * fittings the anchors row is cached per block entity type (cleared whenever the sync swaps the table), the kind of a fitting is cached
 * per class, the model id to standalone id is cached, and the loops are indexed. The render bounding box is the one value that is
 * built per fitted holder: a single-slot memo reuses it while the same holder is asked again.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FittingRenderer implements BlockEntityRenderer<BlockEntity> {

    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Map<ResourceLocation, ModelResourceLocation> STANDALONE = new HashMap<>();
    private static final RenderType MODEL_TYPE = RenderType.solid();

    public FittingRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** Every model under {@code models/fitting/} is a standalone model, so a data row can name it. */
    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        STANDALONE.clear();
        Minecraft.getInstance().getResourceManager().listResources("models/fitting", id -> id.getPath().endsWith(".json")).keySet().forEach(file -> {
            String path = file.getPath();
            ResourceLocation model = ResourceLocation.fromNamespaceAndPath(file.getNamespace(),
                    path.substring("models/".length(), path.length() - ".json".length()));
            event.register(standalone(model));
        });
    }

    private static ModelResourceLocation standalone(ResourceLocation model) {
        return STANDALONE.computeIfAbsent(model, ModelResourceLocation::standalone);
    }

    @Override
    public void render(BlockEntity holder, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        draw(holder, pose, buffers, light, overlay);
    }

    private static final net.minecraft.world.phys.AABB NO_BOX = new net.minecraft.world.phys.AABB(0, 0, 0, 0, 0, 0);
    private static BlockEntity boxHolder;
    private static BlockPos boxPos = BlockPos.ZERO;
    private static net.minecraft.world.phys.AABB box = NO_BOX;

    /**
     * A fitting stands up to a block clear of its holder, so a holder with fittings is drawn while any part of that is in view; one with
     * none has nothing to draw and gets an empty box.
     */
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(BlockEntity holder) {
        if (!(holder instanceof Fittable fittable) || !fittable.hasFittings())
            return NO_BOX;
        if (holder != boxHolder || !holder.getBlockPos().equals(boxPos)) {
            boxHolder = holder;
            boxPos = holder.getBlockPos();
            box = new net.minecraft.world.phys.AABB(boxPos).inflate(1.0);
        }
        return box;
    }

    private static final Map<BlockEntityType<?>, Object> ANCHOR_CACHE = new IdentityHashMap<>();
    private static final Object NO_ANCHORS = new Object();
    private static Map<ResourceLocation, FittingLooks.HolderAnchors> cachedFrom = Map.of();

    /** The mount points of {@code holder}'s type, cached per type; the cache is dropped whenever the synced table is replaced. */
    @Nullable
    private static FittingLooks.HolderAnchors anchorsOf(BlockEntity holder) {
        Map<ResourceLocation, FittingLooks.HolderAnchors> table = FittingLooks.anchors();
        if (table != cachedFrom) {
            ANCHOR_CACHE.clear();
            cachedFrom = table;
        }
        Object row = ANCHOR_CACHE.get(holder.getType());
        if (row == null) {
            row = table.get(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(holder.getType()));
            if (row == null)
                row = NO_ANCHORS;
            ANCHOR_CACHE.put(holder.getType(), row);
        }
        return row == NO_ANCHORS ? null : (FittingLooks.HolderAnchors) row;
    }

    /** Draws {@code holder}'s fittings in the pose of its block origin; nothing for a block entity that holds none or has no anchors row. */
    public static void draw(BlockEntity holder, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!(holder instanceof Fittable fittable) || !fittable.hasFittings())
            return;
        FittingLooks.HolderAnchors anchors = anchorsOf(holder);
        if (anchors == null)
            return;
        for (int i = 0; i < DIRECTIONS.length; i++) {
            Direction side = DIRECTIONS[i];
            SidedFitting fitting = fittable.getSidedFitting(side);
            if (fitting != null)
                drawSided(holder, fitting, side, anchors, pose, buffers, light, overlay);
        }
        List<UpgradeFitting> upgrades = fittable.getUpgrades();
        for (int i = 0; i < upgrades.size(); i++)
            drawUpgrade(holder, upgrades, i, anchors, pose, buffers, light, overlay);
    }

    private static void drawSided(BlockEntity holder, SidedFitting fitting, Direction side, FittingLooks.HolderAnchors anchors,
                                  PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        FittingLooks.Look look = FittingLooks.model(FittingKinds.of(fitting));
        if (look == null)
            return;
        pose.pushPose();
        FittingLooks.Anchor override = anchors.faces().get(side.getName());
        if (override != null)
            place(pose, override, 0);
        else {
            // The top-face placement: the model's base centre on (8, 16, 8) is the model shifted up one block, then turned onto its face.
            turnOntoFace(pose, side);
            pose.translate(0f, 1f, 0f);
        }
        drawLook(holder, fitting, look, pose, buffers, light, overlay);
        pose.popPose();
    }

    private static void drawUpgrade(BlockEntity holder, List<UpgradeFitting> upgrades, int index, FittingLooks.HolderAnchors anchors,
                                    PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        UpgradeFitting fitting = upgrades.get(index);
        ResourceLocation kind = FittingKinds.of(fitting);
        FittingLooks.Look look = FittingLooks.model(kind);
        if (look == null)
            return;
        pose.pushPose();
        if (look.placement() != FittingLooks.Placement.WRAP) {
            FittingLooks.Anchor anchor = anchors.anchors().get(look.anchor());
            if (anchor == null) {
                pose.popPose();
                return;
            }
            int copy = 0;
            if (look.placement() == FittingLooks.Placement.COUNTED)
                for (int j = 0; j < index; j++)
                    if (FittingKinds.of(upgrades.get(j)) == kind)
                        copy++;
            place(pose, anchor, copy);
        }
        drawLook(holder, fitting, look, pose, buffers, light, overlay);
        pose.popPose();
    }

    /** Puts a model's base centre on {@code anchor}, turned by its rotation about it, {@code copy} steps along. */
    private static void place(PoseStack pose, FittingLooks.Anchor anchor, int copy) {
        pose.translate((anchor.pos().x() + anchor.step().x() * copy) / 16f, (anchor.pos().y() + anchor.step().y() * copy) / 16f,
                (anchor.pos().z() + anchor.step().z() * copy) / 16f);
        if (anchor.rot().x() != 0f)
            pose.mulPose(Axis.XP.rotationDegrees(anchor.rot().x()));
        if (anchor.rot().y() != 0f)
            pose.mulPose(Axis.YP.rotationDegrees(anchor.rot().y()));
        if (anchor.rot().z() != 0f)
            pose.mulPose(Axis.ZP.rotationDegrees(anchor.rot().z()));
        pose.translate(-0.5f, 0f, -0.5f);
    }

    /** Turns the top-face placement onto {@code side}, about the block's centre. */
    private static void turnOntoFace(PoseStack pose, Direction side) {
        pose.translate(0.5f, 0.5f, 0.5f);
        switch (side) {
            case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(180f));
            case NORTH -> pose.mulPose(Axis.XP.rotationDegrees(-90f));
            case SOUTH -> pose.mulPose(Axis.XP.rotationDegrees(90f));
            case EAST -> pose.mulPose(Axis.ZP.rotationDegrees(-90f));
            case WEST -> pose.mulPose(Axis.ZP.rotationDegrees(90f));
            default -> {
            }
        }
        pose.translate(-0.5f, -0.5f, -0.5f);
    }

    private static void drawLook(BlockEntity holder, Fitting fitting, FittingLooks.Look look, PoseStack pose, MultiBufferSource buffers,
                                 int light, int overlay) {
        float r = 1f, g = 1f, b = 1f;
        if (look.tint()) {
            ResourceLocation id = fitting.tintMaterial();
            Material material = id == null ? null : ClientMaterials.on(holder.getLevel()).get(id);
            if (material != null) {
                int colour = material.color().orElse(0xFFFFFF);
                r = (colour >> 16 & 0xFF) / 255f;
                g = (colour >> 8 & 0xFF) / 255f;
                b = (colour & 0xFF) / 255f;
            }
        }
        drawModel(look.model(), pose, buffers, r, g, b, light, overlay);
        if (look.overlay().isPresent() && fitting.active())
            drawModel(look.overlay().get(), pose, buffers, 1f, 1f, 1f, light, overlay);
    }

    private static void drawModel(ResourceLocation model, PoseStack pose, MultiBufferSource buffers, float r, float g, float b, int light, int overlay) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderTypeHelper.getEntityRenderType(MODEL_TYPE, false)),
                null, minecraft.getModelManager().getModel(standalone(model)), r, g, b, light, overlay, ModelData.EMPTY, MODEL_TYPE);
    }
}
