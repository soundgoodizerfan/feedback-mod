// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * The muzzle flash of a gun held in first person: a star of rays and a forward cone, additive, drawn in the gun's own
 * space at the end of its barrel (past any brake or can), so it rides the kick and the aim. It lasts a blink from the
 * shot and is drawn as part of the gun, never as a particle, so it cannot sit off the barrel.
 */
final class GunFlash {

    private GunFlash() {
    }

    static final long FLASH_MS = 75;

    /** {@code pose} is in the gun's model pixels, as {@link GunDraw} draws; {@code kickDeg} is the shot's kick, standing in for its power. */
    static void draw(PoseStack pose, MultiBufferSource buffers, GunView view, GunMotion.Hand hand, long now) {
        float t = (now - hand.shotMs) / (float) FLASH_MS;
        if (t < 0 || t >= 1)
            return;
        float size = Mth.clamp(0.7f + hand.kickDeg / 5f, 0.7f, 1.8f) * (1.15f - 0.5f * t);
        float a = 1f - t * t;
        float z0 = -(view.muzzle() + view.muzzleReach());
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        // Rays in the plane across the bore, six of them, turned a little each shot.
        float turn = hand.twist * Mth.PI;
        for (int i = 0; i < 6; i++) {
            float ang = turn + i * Mth.TWO_PI / 6f;
            float len = size * (i % 2 == 0 ? 6.5f : 4f);
            float dx = Mth.cos(ang), dy = Mth.sin(ang);
            ray(m, vc, 0, 0, z0, dx * len, dy * len, z0, -dy * 0.7f * size, dx * 0.7f * size, a);
        }
        // The cone: two crossed diamonds pointing down the bore.
        float len = size * 11f;
        float w = size * 2.2f;
        ray(m, vc, 0, 0, z0 + 1f, 0, 0, z0 - len, w, 0, a);
        ray(m, vc, 0, 0, z0 + 1f, 0, 0, z0 - len, 0, w, a);
    }

    /** A thin diamond from (x0,y0,z0) to (x1,y1,z1), {@code (sx, sy)} wide at the base: hot white at the base, orange and clear at the tip. Both faces. */
    private static void ray(Matrix4f m, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1,
                            float sx, float sy, float a) {
        float[][] c = {{x0 - sx, y0 - sy, z0}, {x0 + sx, y0 + sy, z0}, {x1, y1, z1}, {x1, y1, z1}};
        for (int pass = 0; pass < 2; pass++)
            for (int k = 0; k < 4; k++) {
                float[] p = c[pass == 0 ? k : 3 - k];
                boolean tip = p == c[2] || p == c[3];
                if (tip)
                    vc.addVertex(m, p[0], p[1], p[2]).setColor(1f, 0.55f, 0.15f, 0f);
                else
                    vc.addVertex(m, p[0], p[1], p[2]).setColor(1f, 0.93f, 0.7f, a);
            }
    }
}
