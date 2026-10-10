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
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import io.github.soundgoodizerfan.feedback.vehicle.frame.Frame;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderSelection;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws a welded chassis: plates in their material's map colour, the frame's edges as lines, and
 * -- only while a Welder is held -- each vertex as a small cross, the selected one larger and
 * cyan, a carried one red. Placeholder look, like all the
 * art; the plates are flat-shaded by a fixed light so the shape reads.
 */
public class FrameRenderer implements BlockEntityRenderer<FrameBlockEntity> {

    private static final Vec3 LIGHT = new Vec3(0.4, 1.0, 0.25).normalize();
    private static final float MARK = 1f / 16;

    public FrameRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FrameBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int light, int overlay) {
        Frame frame = be.frame();
        Matrix4f m = poseStack.last().pose();

        VertexConsumer quads = buffers.getBuffer(RenderType.debugQuads());
        for (Frame.Plate p : frame.plates()) {
            Vec3 a = frame.pos(p.a()), b = frame.pos(p.b()), c = frame.pos(p.c());
            Vec3 n = frame.cross(p).normalize();
            int rgb = p.materialState().getMapColor(be.getLevel(), be.getBlockPos()).col;
            float shade = (float) (0.55 + 0.45 * Math.abs(n.dot(LIGHT)));
            float r = ((rgb >> 16) & 0xff) / 255f * shade, g = ((rgb >> 8) & 0xff) / 255f * shade, bl = (rgb & 0xff) / 255f * shade;
            quad(quads, m, a, b, c, r, g, bl);
            quad(quads, m, a, c, b, r, g, bl);
        }

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (long key : frame.edgeKeys())
            line(lines, poseStack, frame.pos(Frame.edgeA(key)), frame.pos(Frame.edgeB(key)), 0.15f, 0.15f, 0.15f);

        Player player = Minecraft.getInstance().player;
        if (player == null)
            return;
        ItemStack welder = player.getMainHandItem().is(FVehicles.WELDER.get()) ? player.getMainHandItem()
                : player.getOffhandItem().is(FVehicles.WELDER.get()) ? player.getOffhandItem() : null;
        if (welder == null)
            return;
        WelderSelection sel = welder.get(FVehicles.WELDER_SELECTION.get());
        int held = sel != null && sel.frame().equals(be.getBlockPos()) ? sel.vertex() : -1;
        for (Map.Entry<Integer, Frame.Vertex> e : frame.vertices().entrySet()) {
            Vec3 p = e.getValue().local();
            boolean isHeld = e.getKey() == held;
            float size = isHeld ? MARK * 2 : MARK;
            float r = 1f, g = 0.8f, bl = 0.2f;              // vertex
            if (isHeld && sel.grabbed()) { g = 0.2f; bl = 0.2f; }         // carried: red
            else if (isHeld) { r = 0.2f; g = 0.9f; bl = 1f; }             // selected: cyan
            line(lines, poseStack, p.add(-size, 0, 0), p.add(size, 0, 0), r, g, bl);
            line(lines, poseStack, p.add(0, -size, 0), p.add(0, size, 0), r, g, bl);
            line(lines, poseStack, p.add(0, 0, -size), p.add(0, 0, size), r, g, bl);
        }
    }

    private static void quad(VertexConsumer c, Matrix4f m, Vec3 a, Vec3 b, Vec3 d, float r, float g, float bl) {
        c.addVertex(m, (float) a.x, (float) a.y, (float) a.z).setColor(r, g, bl, 1f);
        c.addVertex(m, (float) b.x, (float) b.y, (float) b.z).setColor(r, g, bl, 1f);
        c.addVertex(m, (float) d.x, (float) d.y, (float) d.z).setColor(r, g, bl, 1f);
        c.addVertex(m, (float) d.x, (float) d.y, (float) d.z).setColor(r, g, bl, 1f);
    }

    private static void line(VertexConsumer c, PoseStack pose, Vec3 a, Vec3 b, float r, float g, float bl) {
        Vec3 n = b.subtract(a).normalize();
        PoseStack.Pose last = pose.last();
        c.addVertex(last, (float) a.x, (float) a.y, (float) a.z).setColor(r, g, bl, 1f).setNormal(last, (float) n.x, (float) n.y, (float) n.z);
        c.addVertex(last, (float) b.x, (float) b.y, (float) b.z).setColor(r, g, bl, 1f).setNormal(last, (float) n.x, (float) n.y, (float) n.z);
    }

    @Override
    public AABB getRenderBoundingBox(FrameBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(Frame.RANGE / (double) Frame.GRID + 1);
    }
}
