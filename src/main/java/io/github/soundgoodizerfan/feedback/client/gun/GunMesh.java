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
package io.github.soundgoodizerfan.feedback.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Immediate-mode boxes for the gun renderer, in centimetres, textured from one greyscale sheet
 * ({@code textures/item/gun/sheet.png}, 4 by 4 tiles of 32 px) and tinted by the material of the
 * part being drawn. Built per frame rather than baked: a gun's shape follows its parts and their
 * measures (a barrel is drawn at its own length and diameter), which no fixed model can do.
 *
 * <p>Texture density is 2 texels per centimetre; a face longer than a tile stretches, which is why
 * the long-part tiles (brushed, grain, coil) only vary across the stretch direction. On side and
 * top faces u runs along the bore (z), so streaks follow the barrel.
 */
final class GunMesh {

    /** A tile of the sheet. Tinted tiles take the part's material colour; the rest keep their own. */
    enum Tile {
        METAL(0, true), BRUSHED(1, true), RIBBED(2, true), PANEL(3, true),
        GRAIN(4, true), END_GRAIN(5, true), CHECKER(6, true), RUBBER(7, true),
        GLASS(8, false), BORE(9, false), COIL(10, true), KNURL(11, true),
        VENT(12, true), LAMP(13, true), LENS(14, false), LINKS(15, false);

        final int index;
        final boolean tinted;

        Tile(int index, boolean tinted) {
            this.index = index;
            this.tinted = tinted;
        }
    }

    private static final float TEXELS_PER_CM = 2f;
    private static final float TILE = 32f, SHEET = 128f;
    static final net.minecraft.resources.ResourceLocation SHEET_ID = Feedback.id("item/gun/sheet");

    final PoseStack pose;
    private final VertexConsumer out;
    private final TextureAtlasSprite sprite;
    private final int light, overlay;
    /** Current part colour, ARGB. */
    private int colour = 0xFFFFFFFF;
    /** Varies the patch of a tile each box samples, so neighbouring faces do not repeat. */
    private int seed;

    GunMesh(PoseStack pose, VertexConsumer out, int light, int overlay) {
        this(pose, out, light, overlay, Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(SHEET_ID));
    }

    /** {@code sprite} null writes sheet coordinates (0 to 1) as they are: an offline preview reads them. */
    GunMesh(PoseStack pose, VertexConsumer out, int light, int overlay, TextureAtlasSprite sprite) {
        this.pose = pose;
        this.out = out;
        this.light = light;
        this.overlay = overlay;
        this.sprite = sprite;
    }

    /** Draw what follows in this ARGB colour (the part's material). */
    GunMesh colour(int argb) {
        this.colour = argb;
        return this;
    }

    void push() {
        pose.pushPose();
    }

    void pop() {
        pose.popPose();
    }

    void move(float x, float y, float z) {
        pose.translate(x, y, z);
    }

    /** Rotate {@code deg} about an axis through (px, py, pz). */
    void turnX(float deg, float py, float pz) {
        pose.translate(0, py, pz);
        pose.mulPose(Axis.XP.rotationDegrees(deg));
        pose.translate(0, -py, -pz);
    }

    void turnY(float deg, float px, float pz) {
        pose.translate(px, 0, pz);
        pose.mulPose(Axis.YP.rotationDegrees(deg));
        pose.translate(-px, 0, -pz);
    }

    void turnZ(float deg, float px, float py) {
        pose.translate(px, py, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(deg));
        pose.translate(-px, -py, 0);
    }

    /** An axis-aligned box between two corners, all six faces. */
    void box(float x0, float y0, float z0, float x1, float y1, float z1, Tile t) {
        box(x0, y0, z0, x1, y1, z1, t, light);
    }

    /** A box that ignores the world's light: lamps and reticles. */
    void glow(float x0, float y0, float z0, float x1, float y1, float z1, Tile t) {
        box(x0, y0, z0, x1, y1, z1, t, 0xF000F0);
    }

    /** A box centred on (x, y) across, running z0 to z1 along the bore. */
    void bar(float x, float y, float hw, float hh, float z0, float z1, Tile t) {
        box(x - hw, y - hh, Math.min(z0, z1), x + hw, y + hh, Math.max(z0, z1), t);
    }

    /**
     * An octagonal rod on the axis (x, y) from z0 to z1, {@code r} from the axis to a flat: a square
     * and the same square turned 45 degrees. Reads as round at the sizes a gun is drawn at.
     */
    void rod(float x, float y, float r, float z0, float z1, Tile t) {
        float lo = Math.min(z0, z1), hi = Math.max(z0, z1);
        box(x - r, y - r * 0.414f, lo, x + r, y + r * 0.414f, hi, t);
        box(x - r * 0.414f, y - r, lo, x + r * 0.414f, y + r, hi, t);
        push();
        turnZ(45, x, y);
        box(x - r * 0.98f, y - r * 0.414f, lo, x + r * 0.98f, y + r * 0.414f, hi, t);
        box(x - r * 0.414f, y - r * 0.98f, lo, x + r * 0.414f, y + r * 0.98f, hi, t);
        pop();
    }

    /** Thin rings round an axis, {@code count} of them from z0 toward z1: fins, windings, lagging. */
    void rings(float x, float y, float r, float z0, float z1, int count, float width, Tile t) {
        if (count <= 0)
            return;
        float step = (z1 - z0) / count;
        for (int i = 0; i < count; i++) {
            float z = z0 + step * (i + 0.5f);
            rod(x, y, r, z - width / 2, z + width / 2, t);
        }
    }

    private void box(float x0, float y0, float z0, float x1, float y1, float z1, Tile t, int lit) {
        int argb = t.tinted ? colour : 0xFFFFFFFF;
        if (t == Tile.LAMP)
            argb = colour;
        seed = seed * 31 + 7;
        PoseStack.Pose p = pose.last();
        // Each face lists its corners counter-clockwise seen from outside (a x b = normal), so culling keeps it.
        face(p, t, argb, lit, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
        face(p, t, argb, lit, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        face(p, t, argb, lit, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        face(p, t, argb, lit, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        face(p, t, argb, lit, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        face(p, t, argb, lit, 0, 0, -1, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
    }

    private void face(PoseStack.Pose p, Tile t, int argb, int lit, float nx, float ny, float nz, float... c) {
        // u along z on side and top faces (x on end faces), v down the face.
        float uMin = Float.MAX_VALUE, uMax = -Float.MAX_VALUE, vMin = Float.MAX_VALUE, vMax = -Float.MAX_VALUE;
        float[] us = new float[4], vs = new float[4];
        for (int i = 0; i < 4; i++) {
            float x = c[i * 3], y = c[i * 3 + 1], z = c[i * 3 + 2];
            us[i] = nz != 0 ? x : z;
            vs[i] = ny != 0 ? x : -y;
            uMin = Math.min(uMin, us[i]);
            uMax = Math.max(uMax, us[i]);
            vMin = Math.min(vMin, vs[i]);
            vMax = Math.max(vMax, vs[i]);
        }
        float uSpan = Math.min((uMax - uMin) * TEXELS_PER_CM, TILE), vSpan = Math.min((vMax - vMin) * TEXELS_PER_CM, TILE);
        uSpan = Math.max(uSpan, 1f);
        vSpan = Math.max(vSpan, 1f);
        float uOff = Math.floorMod(seed, (int) (TILE - uSpan) + 1), vOff = Math.floorMod(seed >> 5, (int) (TILE - vSpan) + 1);
        float tx = (t.index % 4) * TILE, ty = (t.index / 4) * TILE;
        for (int i = 0; i < 4; i++) {
            float fu = uMax == uMin ? 0 : (us[i] - uMin) / (uMax - uMin);
            float fv = vMax == vMin ? 0 : (vs[i] - vMin) / (vMax - vMin);
            float u = (tx + uOff + fu * uSpan) / SHEET, v = (ty + vOff + fv * vSpan) / SHEET;
            if (sprite != null) {
                u = sprite.getU(u);
                v = sprite.getV(v);
            }
            out.addVertex(p, c[i * 3], c[i * 3 + 1], c[i * 3 + 2]).setColor(argb).setUv(u, v).setOverlay(overlay)
                    .setLight(lit).setNormal(p, nx, ny, nz);
        }
    }
}
