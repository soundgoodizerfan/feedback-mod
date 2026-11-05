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

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * The look of a glowing block (spec incandescence): an emissive overlay on the block's model quads, tinted by its band from dull red
 * through orange and yellow to white, and eased between bands rather than stepping with the server's light level. The client
 * only reads the {@link Incandescence#GLOW} the server set; it decides nothing. Also draws the data-link nodes a block
 * entity would have drawn, since a type has one renderer.
 */
public class GlowRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

    /** The level shown at each glowing block, eased toward the real one every frame. */
    private static final Map<Long, Float> SHOWN = new HashMap<>();
    private static final float EASE = 0.08f;
    private static final double GROW = 0.003;
    private static final RandomSource RAND = RandomSource.create();

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
        // The overlay follows the block's own model quads, nudged out along each face normal so it sits on the surface.
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        Matrix4f m = pose.last().pose();
        VertexConsumer out = buffers.getBuffer(RenderType.lightning());
        for (int i = 0; i <= Direction.values().length; i++) {
            Direction side = i == Direction.values().length ? null : Direction.values()[i];
            RAND.setSeed(42L);
            for (BakedQuad q : model.getQuads(state, side, RAND, ModelData.EMPTY, null))
                quad(m, out, q, r, g, b, alpha);
        }
    }

    private static void quad(Matrix4f m, VertexConsumer c, BakedQuad q, float r, float g, float b, float a) {
        int[] v = q.getVertices();
        float nx = q.getDirection().getStepX() * (float) GROW, ny = q.getDirection().getStepY() * (float) GROW, nz = q.getDirection().getStepZ() * (float) GROW;
        for (int i = 0; i < 4; i++) {
            int o = i * 8; // vertex stride is 8 ints; position is the first three
            c.addVertex(m, Float.intBitsToFloat(v[o]) + nx, Float.intBitsToFloat(v[o + 1]) + ny, Float.intBitsToFloat(v[o + 2]) + nz).setColor(r, g, b, a);
        }
    }
}
