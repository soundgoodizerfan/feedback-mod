// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Armour extras drawn with the gun sheet: boxes in a vanilla model part's own pixel space (y down), textured and
 * tinted like gun parts so metal is brushed and wood is grained. Worn armour's own pass draws the vanilla cubes;
 * these go through a second buffer after it.
 */
public final class PlateDraw {

    private static final ResourceLocation SHEET = Feedback.id("textures/item/gun/sheet.png");

    private final PoseStack pose;
    private final GunMesh mesh;

    /** {@code pose} sits at the vanilla part (translated and rotated, in blocks); boxes are then given in model pixels. */
    public PlateDraw(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        this.pose = pose;
        this.mesh = new GunMesh(pose, image -> buffers.getBuffer(RenderType.armorCutoutNoCull(SHEET)), light, overlay, null);
        pose.pushPose();
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        pose.mulPose(Axis.ZP.rotationDegrees(180f)); // y up, x mirrored: the gun frame
    }

    /** What the following boxes are made of and tinted. */
    public PlateDraw material(Material material, int argb) {
        mesh.colour(argb);
        mesh.surface(GunView.Surface.of(material));
        return this;
    }

    /** A box as {@code ModelPart.addBox} gives it (corner and size, y down). */
    public void box(float x, float y, float z, float w, float h, float d) {
        mesh.box(-(x + w), -(y + h), z, -x, -y, z + d, GunMesh.Tile.BRUSHED);
    }

    /** Finish the sheet's batch so it draws now, in step with the armour it follows. */
    public static void flush(net.minecraft.client.renderer.MultiBufferSource.BufferSource source) {
        source.endBatch(RenderType.armorCutoutNoCull(SHEET));
    }

    public void end() {
        pose.popPose();
    }
}
