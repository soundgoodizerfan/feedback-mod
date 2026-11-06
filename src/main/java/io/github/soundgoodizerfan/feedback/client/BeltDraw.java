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

import net.minecraft.client.renderer.texture.OverlayTexture;

/** Small box and prism helpers for the belt's drawn-by-hand parts (head roller, cleats). Units are blocks, in the block's own frame. */
final class BeltDraw {

    private BeltDraw() {
    }

    /** An axis-aligned box, every face textured with the whole 16x16 sprite. */
    static void box(PoseStack pose, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, int light) {
        quad(pose, vc, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0, light);
        quad(pose, vc, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, 0, -1, 0, light);
        quad(pose, vc, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1, light);
        quad(pose, vc, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, 0, 0, 1, light);
        quad(pose, vc, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, -1, 0, 0, light);
        quad(pose, vc, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0, light);
    }

    /** A prism of {@code sides} faces along X from x0 to x1, centred (cy, cz) with radius r: a roller. */
    static void roller(PoseStack pose, VertexConsumer vc, float x0, float x1, float cy, float cz, float r, int sides, int light) {
        for (int k = 0; k < sides; k++) {
            double a0 = k * 2 * Math.PI / sides, a1 = (k + 1) * 2 * Math.PI / sides, mid = (a0 + a1) / 2;
            float y0 = cy + r * (float) Math.sin(a0), z0 = cz + r * (float) Math.cos(a0);
            float y1 = cy + r * (float) Math.sin(a1), z1 = cz + r * (float) Math.cos(a1);
            quad(pose, vc, x0, y0, z0, x1, y0, z0, x1, y1, z1, x0, y1, z1, 0, (float) Math.sin(mid), (float) Math.cos(mid), light);
            quad(pose, vc, x0, cy, cz, x0, y1, z1, x0, y0, z0, x0, cy, cz, -1, 0, 0, light);
            quad(pose, vc, x1, cy, cz, x1, y0, z0, x1, y1, z1, x1, cy, cz, 1, 0, 0, light);
        }
    }

    private static void quad(PoseStack pose, VertexConsumer vc, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, float nx, float ny, float nz, int light) {
        PoseStack.Pose last = pose.last();
        vertex(last, vc, ax, ay, az, 0, 0, nx, ny, nz, light);
        vertex(last, vc, bx, by, bz, 1, 0, nx, ny, nz, light);
        vertex(last, vc, cx, cy, cz, 1, 1, nx, ny, nz, light);
        vertex(last, vc, dx, dy, dz, 0, 1, nx, ny, nz, light);
    }

    private static void vertex(PoseStack.Pose last, VertexConsumer vc, float x, float y, float z, float u, float v,
                               float nx, float ny, float nz, int light) {
        vc.addVertex(last, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(last, nx, ny, nz);
    }
}
