// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.machine.phosphor.PhosphorScreenBlock;
import io.github.soundgoodizerfan.feedback.machine.phosphor.PhosphorScreenBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * Draws an oscilloscope's row on the inside of a lit phosphor screen: a line of the last samples, oldest at the left, turned to face the
 * viewer (the bulb has no front), glowing at the screen's own glow level and fading toward the old end by
 * {@link ElectricMachineTuning#SCOPE_TRACE_PERSISTENCE}. Nothing is drawn while the screen is dark (unsealed, not evacuated, undriven or
 * burnt out), so the trace shows exactly when the glass would glow.
 */
public class PhosphorTraceRenderer implements BlockEntityRenderer<PhosphorScreenBlockEntity> {
    /** Half the width and the height of the trace area, in blocks: inside the belly of the bulb. */
    private static final float HALF_WIDTH = 0.28f;
    private static final float HEIGHT = 0.3f;
    private static final float CENTRE_Y = 0.3125f;

    public PhosphorTraceRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PhosphorScreenBlockEntity screen, float partial, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        int glow = screen.getBlockState().getValue(PhosphorScreenBlock.GLOW);
        if (glow == 0)
            return;
        float yaw = Minecraft.getInstance().gameRenderer.getMainCamera().getYRot() * Mth.DEG_TO_RAD;
        float rx = -Mth.cos(yaw);
        float rz = -Mth.sin(yaw);
        float[] trace = screen.trace();
        int n = trace.length;
        float level = glow / 3f;
        VertexConsumer lines = buffer.getBuffer(RenderType.lineStrip());
        PoseStack.Pose last = pose.last();
        for (int i = 0; i < n; i++) {
            float across = (i / (float) (n - 1) * 2f - 1f) * HALF_WIDTH;
            float x = 0.5f + rx * across;
            float z = 0.5f + rz * across;
            float y = CENTRE_Y + Mth.clamp(trace[i], 0f, 1f) * HEIGHT;
            // The segment's own direction is what the line shader widens along; the last point keeps the one before it.
            int next = Math.min(n - 1, i + 1);
            int from = next == i ? i - 1 : i;
            float dx = rx * 2f * HALF_WIDTH / (n - 1);
            float dz = rz * 2f * HALF_WIDTH / (n - 1);
            float dy = (Mth.clamp(trace[next], 0f, 1f) - Mth.clamp(trace[from], 0f, 1f)) * HEIGHT;
            float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            float fade = ElectricMachineTuning.SCOPE_TRACE_PERSISTENCE + (1f - ElectricMachineTuning.SCOPE_TRACE_PERSISTENCE) * (i / (float) (n - 1));
            lines.addVertex(last, x, y, z).setColor(0.55f, 1f, 0.65f, Mth.clamp(fade * level, 0f, 1f))
                    .setNormal(last, dx / len, dy / len, dz / len);
        }
    }
}
