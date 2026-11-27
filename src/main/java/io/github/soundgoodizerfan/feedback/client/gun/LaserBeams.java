// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.weapon.SightCell;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

/**
 * The laser sight's beam: a thin red line from just ahead of the eye to the first block or entity within {@link #REACH} blocks, for
 * every player whose held gun has a laser with charge in its cell, with a red dust mote where it lands (the dust it lights).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class LaserBeams {

    private static final double REACH = 96;
    private static final int RED = 0xFFFF2020;
    private static final DustParticleOptions MOTE = new DustParticleOptions(new Vector3f(1f, 0.1f, 0.1f), 0.5f);

    private LaserBeams() {
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer lines = null;
        for (Player player : mc.level.players()) {
            ItemStack gun = SightCell.powered(player.getMainHandItem(), SightCell.LASER) ? player.getMainHandItem()
                    : SightCell.powered(player.getOffhandItem(), SightCell.LASER) ? player.getOffhandItem() : ItemStack.EMPTY;
            if (gun.isEmpty())
                continue;
            SightRay.Hit hit = SightRay.cast(player, REACH);
            Vec3 look = player.getViewVector(1f);
            Vec3 from = player.getEyePosition(1f).add(look.scale(0.5)).add(0, -0.12, 0);
            Vec3 to = hit.at() == null ? player.getEyePosition(1f).add(look.scale(hit.distance())) : hit.at();
            if (lines == null)
                lines = buffer.getBuffer(RenderType.lines());
            pose.pushPose();
            pose.translate(from.x - camera.x, from.y - camera.y, from.z - camera.z);
            Vec3 d = to.subtract(from);
            float len = (float) d.length();
            if (len > 0f) {
                float nx = (float) d.x / len, ny = (float) d.y / len, nz = (float) d.z / len;
                lines.addVertex(pose.last(), 0f, 0f, 0f).setColor(RED).setNormal(pose.last(), nx, ny, nz);
                lines.addVertex(pose.last(), (float) d.x, (float) d.y, (float) d.z).setColor(RED).setNormal(pose.last(), nx, ny, nz);
            }
            pose.popPose();
            if (hit.at() != null && mc.level.getGameTime() % 3 == 0)
                mc.level.addParticle(MOTE, hit.at().x, hit.at().y, hit.at().z, 0, 0, 0);
        }
        if (lines != null)
            buffer.endBatch(RenderType.lines());
    }
}
