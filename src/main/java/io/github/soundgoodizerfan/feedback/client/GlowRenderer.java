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

import io.github.soundgoodizerfan.feedback.core.thermal.Incandescence;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * The look of a glowing block (spec incandescence): an emissive overlay on the block's shape, tinted by its band from dull red
 * through orange and yellow to white, and eased between bands rather than stepping with the server's light level. The client
 * only reads the {@link Incandescence#GLOW} the server set; it decides nothing. Also draws the data-link nodes a block
 * entity would have drawn, since a type has one renderer.
 */
public class GlowRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

    /** The level shown at each glowing block, eased toward the real one every frame. */
    private static final Map<Long, Float> SHOWN = new HashMap<>();
    private static final float EASE = 0.08f;
    private static final double GROW = 0.003;

    public GlowRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(T entity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        DataNodeRenderer.drawNodes(entity);
        BlockPos at = entity.getBlockPos();
        var state = entity.getBlockState();
        float target = state.hasProperty(Incandescence.GLOW) ? state.getValue(Incandescence.GLOW) : 0;
        float shown = SHOWN.getOrDefault(at.asLong(), 0f);
        shown += (target - shown) * EASE;
        if (shown < 0.05f && target == 0f) {
            SHOWN.remove(at.asLong());
            return;
        }
        SHOWN.put(at.asLong(), shown);
        if (shown < 1.5f)
            return;
        float t = GlowColours.along(shown);
        float[] rgb = GlowColours.rgb(t);
        float r = rgb[0], g = rgb[1], b = rgb[2];
        float alpha = 0.18f + 0.3f * t;
        AABB box = state.getShape(entity.getLevel(), at).isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : state.getShape(entity.getLevel(), at).bounds();
        box = box.inflate(GROW);
        VertexConsumer out = buffers.getBuffer(RenderType.lightning());
        box(pose.last().pose(), out, box, r, g, b, alpha);
    }

    private static void box(Matrix4f m, VertexConsumer c, AABB a, float r, float g, float b, float al) {
        float x0 = (float) a.minX, y0 = (float) a.minY, z0 = (float) a.minZ, x1 = (float) a.maxX, y1 = (float) a.maxY, z1 = (float) a.maxZ;
        quad(m, c, r, g, b, al, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(m, c, r, g, b, al, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(m, c, r, g, b, al, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(m, c, r, g, b, al, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(m, c, r, g, b, al, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(m, c, r, g, b, al, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    private static void quad(Matrix4f m, VertexConsumer c, float r, float g, float b, float a,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        c.addVertex(m, ax, ay, az).setColor(r, g, b, a);
        c.addVertex(m, bx, by, bz).setColor(r, g, b, a);
        c.addVertex(m, cx, cy, cz).setColor(r, g, b, a);
        c.addVertex(m, dx, dy, dz).setColor(r, g, b, a);
    }
}
