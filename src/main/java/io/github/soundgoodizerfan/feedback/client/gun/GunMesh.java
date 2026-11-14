// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Immediate-mode boxes for the gun renderer, in model pixels (1/16 block), textured from one greyscale sheet
 * ({@code textures/item/gun/sheet.png}, 4 by 4 tiles of 32 px) and tinted by the material of the
 * part being drawn. Built per frame rather than baked: a gun's shape follows its parts and their
 * measures (a barrel is drawn at its own length and diameter), which no fixed model can do.
 *
 * <p>Texture density is 2 texels per pixel everywhere, so every part shares one texel size (the
 * half-pixel grid Scorched Guns' 32 px sheets use); boxes sit on a quarter-pixel grid. A face longer
 * than a tile (16 px) stretches, which is why the long-part tiles (brushed, grain) only vary
 * across the stretch direction. On side and top faces u runs along the bore (z).
 */
public final class GunMesh {

    /** A tile of the sheet. Tinted tiles take the part's material colour; the rest keep their own. */
    public enum Tile {
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

    /** Model pixels one texel spans: a gun's detail is half a pixel fine, worn armour's is vanilla's one. */
    private float texelPx = 0.5f;
    private static final float TILE = 32f, SHEET = 128f;
    static final net.minecraft.resources.ResourceLocation SHEET_ID = Feedback.id("item/gun/sheet");

    final PoseStack pose;
    /** The consumer for a texture: null is the gun sheet on the block atlas, else a wood image ({@link GunWood}). */
    private final java.util.function.Function<net.minecraft.resources.ResourceLocation, VertexConsumer> buffers;
    private VertexConsumer out;
    /** The wood image the current part draws with, null for the sheet; {@code bound} is the one {@link #out} writes to. */
    private net.minecraft.resources.ResourceLocation image, bound;
    private boolean anyBound;
    private final TextureAtlasSprite sprite;
    private final int baseLight, overlay;
    private int light;
    private GunView.Surface surface = GunView.Surface.METAL;
    /** Current part colour, ARGB. */
    private int colour = 0xFFFFFFFF;
    /** Varies the patch of a tile each box samples, so neighbouring faces do not repeat. */
    private int seed;
    /** Stands in for a texture: what follows goes to the translucent buffer. Never loaded. */
    static final net.minecraft.resources.ResourceLocation GLASS = Feedback.id("gun_glass");
    private boolean glassNow;
    /** How much of a glass part shows: mostly clear, the tile's texture still on it. */
    private static final int GLASS_ALPHA = 0x90000000;
    /** First person, aimed: the scope's lens and eyepiece plate are drawn. */
    boolean sighted;
    /**
     * Whether boxes are layered ({@link GunDraw} turns it on; worn plates leave it off): each box is drawn a hair
     * toward the eye by its level, its role's layer ({@link #LAYERS}, inner parts first) times {@link #PER_LAYER}
     * plus the boxes already drawn in that role (up to {@code PER_LAYER - 1}, so later detail is in front). Parts
     * touch and nest and many measures land on one plane (every sleeve on a barrel sits at its radius plus a
     * quarter); two faces turned the same way in one plane z-fight, and the level settles which is in front. It is
     * a depth bias, not a change of shape: under a perspective projection every vertex is scaled toward the eye
     * (the origin of render space), which leaves where it lands on screen exactly where it was; under an
     * orthographic one (the GUI) it moves along +z, which is toward the viewer there. Boxes of one level that
     * only touched still only touch, as growing them would not. {@link GunFaceCheck} lists faces left on one level.
     */
    boolean layered;
    /** Each role's layer, inner to outer: receiver, barrels, moving parts, sleeves, under-barrel, muzzle, furniture, sights. */
    private static final java.util.Map<String, Integer> LAYERS = layers("R", "B", "D", "K", "F", "A", "H", "J", "Z", "P", "Q",
            "I", "E", "C", "a", "V", "N", "M", "G", "i", "W", "T", "L", "d", "b", "Y", "S", "e", "s", "g", "h", "p", "l", "z", "u", "j", "k");
    /** The layer of a fixed colour (magazines, lamps, glass): over every part. */
    private static final int COLOUR_LAYER = LAYERS.size();
    /** Levels in a layer: one for each box a role draws, the rest share the last. */
    static final int PER_LAYER = 32;
    /**
     * A level's bias. Perspective: the share of the distance to the eye (1e-5 separates faces in a 24-bit depth
     * buffer out to about eight blocks with vanilla's near plane; a layer, 32 levels, past 200; the most is under
     * 1%). Orthographic: model pixels along +z, through the pose's scale (the GUI's depth buffer resolves about a
     * thousandth of a GUI unit; a layer is a 64th of a pixel).
     */
    private static final float PERSPECTIVE_STEP = 1e-5f, ORTHO_STEP_PX = 1f / 2048f;
    private int layer;
    /** The level key being drawn (a role, or {@code #} for every fixed colour) and the boxes drawn so far under each. */
    private String layerKey = "";
    private final java.util.Map<String, Integer> drawn = new java.util.HashMap<>();
    /** The level the current box draws at. */
    private int level;
    /** Whether the projection is orthographic (the GUI), read once when the mesh is made; null offline (no bias). */
    private final Boolean ortho;
    private final org.joml.Vector3f scratch = new org.joml.Vector3f();

    private static java.util.Map<String, Integer> layers(String... roles) {
        java.util.Map<String, Integer> m = new java.util.HashMap<>();
        for (int i = 0; i < roles.length; i++)
            m.put(roles[i], i);
        return java.util.Map.copyOf(m);
    }

    /** Dev check ({@link GunFaceCheck}): told every face of every box as drawn, or null. */
    Probe probe;
    /** The role being drawn, for the probe's report; a fixed colour shows as its hex. */
    private String role = "";
    private int boxes;

    /** Receives each face of each box in model space after the pose: the box's index and level, its role and tile, its normal and four corners. */
    interface Probe {
        void face(int box, int level, String role, Tile tile, float[] normal, float[][] corners);
    }

    GunMesh(PoseStack pose, java.util.function.Function<net.minecraft.resources.ResourceLocation, VertexConsumer> buffers,
            int light, int overlay) {
        this(pose, buffers, light, overlay, Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(SHEET_ID));
    }

    /**
     * {@code sprite} null writes sheet coordinates (0 to 1) as they are: an offline preview reads them.
     * Switching texture asks {@code buffers} again, which ends the previous batch, so a consumer is never kept across a switch.
     */
    GunMesh(PoseStack pose, java.util.function.Function<net.minecraft.resources.ResourceLocation, VertexConsumer> buffers,
            int light, int overlay, TextureAtlasSprite sprite) {
        this.pose = pose;
        this.buffers = buffers;
        this.light = light;
        this.baseLight = light;
        this.overlay = overlay;
        this.sprite = sprite;
        this.ortho = sprite == null ? null : com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix().m23() == 0f;
    }

    /** Texels this mesh draws a model pixel with: worn armour wants vanilla's one a pixel. */
    public GunMesh texelPx(float px) {
        this.texelPx = px;
        return this;
    }

    /** Draw what follows in this ARGB colour, as metal. */
    public GunMesh colour(int argb) {
        this.colour = argb;
        this.surface = GunView.Surface.METAL;
        this.image = null;
        this.role = String.format("#%08X", argb);
        layer = COLOUR_LAYER;
        layerKey = "#";
        return this;
    }

    /** Draw what follows as {@code role}'s part: its material's colour and surface. */
    GunMesh part(GunView v, String role) {
        this.colour = v.colour(role);
        this.surface = v.surface(role);
        this.image = v.wood(role);
        this.role = role;
        layer = LAYERS.getOrDefault(role, 0);
        layerKey = role;
        return this;
    }

    /** Light what follows at least at block light {@code level} (a glowing barrel), or back to the world's at 0. */
    void glowAt(int level) {
        int block = Math.max(baseLight & 0xFFFF, level << 4);
        light = (baseLight & 0xFFFF0000) | block;
    }

    /**
     * The tile a part is drawn with, for what it is made of: grain is for wood and brushing for metal,
     * so a steel stock is not drawn as timber nor a wooden forend as plate.
     */
    private Tile surfaced(Tile t) {
        return switch (surface) {
            case WOOD -> switch (t) {
                case METAL, BRUSHED, PANEL, RIBBED, VENT -> Tile.GRAIN;
                default -> t;
            };
            case POLYMER -> switch (t) {
                case GRAIN, BRUSHED, PANEL, END_GRAIN -> Tile.METAL;
                default -> t;
            };
            case METAL -> switch (t) {
                case GRAIN -> Tile.BRUSHED;
                case END_GRAIN -> Tile.METAL;
                default -> t;
            };
            case GLASS -> t;
            case OTHER -> switch (t) {
                case GRAIN, BRUSHED, END_GRAIN -> Tile.METAL;
                default -> t;
            };
        };
    }

    /** Draw what follows as this kind of surface (grain on wood, brushing on metal). */
    GunMesh surface(GunView.Surface surface) {
        this.surface = surface;
        this.image = null;
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
    public void box(float x0, float y0, float z0, float x1, float y1, float z1, Tile t) {
        box(x0, y0, z0, x1, y1, z1, t, light);
    }

    /** A see-through box tinted {@code argb} (its alpha counts), through the translucent buffer: draw these last. */
    void pane(float x0, float y0, float z0, float x1, float y1, float z1, int argb, boolean lit) {
        int old = colour;
        colour = argb;
        glassNow = true;
        box(x0, y0, z0, x1, y1, z1, Tile.LAMP, lit ? 0xF000F0 : light);
        glassNow = false;
        colour = old;
    }

    /** A square showing the scope's lens image ({@link GunLens}), facing the eye (+z), unlit. */
    void lens(float x0, float y0, float x1, float y1, float z) {
        out = buffers.apply(GunLens.TEXTURE);
        bound = GunLens.TEXTURE;
        anyBound = true;
        PoseStack.Pose p = pose.last();
        float[][] c = {{x0, y0, 0, 0}, {x1, y0, 1, 0}, {x1, y1, 1, 1}, {x0, y1, 0, 1}};
        for (float[] k : c)
            vertex(p, k[0], k[1], z).setColor(0xFFFFFFFF).setUv(k[2], k[3]).setOverlay(overlay).setLight(0xF000F0).setNormal(p, 0, 0, 1);
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
     * A rod on the axis (x, y) from z0 to z1, {@code r} from the axis to a face: square, as Minecraft
     * draws round things, with its corners stepped in an eighth of a pixel from 0.75 px up so a thick
     * one reads round without leaving the grid.
     */
    void rod(float x, float y, float r, float z0, float z1, Tile t) {
        float lo = Math.min(z0, z1), hi = Math.max(z0, z1);
        if (r < 0.75f) {
            box(x - r, y - r, lo, x + r, y + r, hi, t);
            return;
        }
        float c = r >= 1.25f ? 0.25f : 0.125f;
        box(x - r, y - r + c, lo, x + r, y + r - c, hi, t);
        box(x - r + c, y - r, lo + 0.003f, x + r - c, y + r, hi - 0.003f, t); // inset: shares no cap plane with the first box
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
        t = surfaced(t);
        // A part of glass is see-through: the translucent buffer, the glass tile, the material's colour at a part's opacity.
        boolean glassPart = surface == GunView.Surface.GLASS && t != Tile.LAMP;
        if (glassPart)
            t = Tile.GLASS;
        // Wood drawn with its own bark keeps the shape's special tiles (checkering, end grain, a pad) on the sheet.
        net.minecraft.resources.ResourceLocation want = glassNow || glassPart ? GLASS : image != null && t == Tile.GRAIN ? image : null;
        if (!anyBound || want != bound) {
            out = buffers.apply(want);
            bound = want;
            anyBound = true;
        }
        int argb = t.tinted && bound == null ? colour : 0xFFFFFFFF;
        if (t == Tile.LAMP)
            argb = colour;
        if (glassPart)
            argb = (colour & 0xFFFFFF) | GLASS_ALPHA;
        seed = seed * 31 + 7;
        level = layered ? layer * PER_LAYER + Math.min(drawn.merge(layerKey, 1, Integer::sum) - 1, PER_LAYER - 1) : 0;
        PoseStack.Pose p = pose.last();
        if (probe != null)
            probe(p, t, x0, y0, z0, x1, y1, z1);
        // Each face lists its corners counter-clockwise seen from outside (a x b = normal), so culling keeps it.
        face(p, t, argb, lit, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
        face(p, t, argb, lit, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        face(p, t, argb, lit, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        face(p, t, argb, lit, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        face(p, t, argb, lit, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        face(p, t, argb, lit, 0, 0, -1, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
    }

    /** Tells the probe the box's six faces, transformed by the pose. */
    private void probe(PoseStack.Pose p, Tile t, float x0, float y0, float z0, float x1, float y1, float z1) {
        int id = boxes++;
        float[][][] faces = {
                {{1, 0, 0}, {x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}, {x1, y0, z1}},
                {{-1, 0, 0}, {x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}, {x0, y1, z0}},
                {{0, 1, 0}, {x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}},
                {{0, -1, 0}, {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}},
                {{0, 0, 1}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}},
                {{0, 0, -1}, {x0, y0, z0}, {x0, y1, z0}, {x1, y1, z0}, {x1, y0, z0}}};
        for (float[][] f : faces) {
            org.joml.Vector3f n = p.normal().transform(new org.joml.Vector3f(f[0][0], f[0][1], f[0][2])).normalize();
            float[][] c = new float[4][];
            for (int i = 0; i < 4; i++) {
                org.joml.Vector3f v = p.pose().transformPosition(new org.joml.Vector3f(f[i + 1][0], f[i + 1][1], f[i + 1][2]));
                c[i] = new float[]{v.x, v.y, v.z};
            }
            probe.face(id, level, role, t, new float[]{n.x, n.y, n.z}, c);
        }
    }

    /**
     * One face, cut like a picture frame: a rim half a pixel wide (one texel) on each side samples the
     * tile's lit or shaded border, the middle a patch of its interior. Every box then has a crisp edge
     * at one texel whatever its size, which is what makes a block model read as hand-made. Faces under
     * 1.5 px across squeeze the rim to a third of the face.
     */
    private void face(PoseStack.Pose p, Tile t, int argb, int lit, float nx, float ny, float nz, float... c) {
        // u along z on side and top faces (x on end faces), v down the face.
        float[] us = new float[4], vs = new float[4];
        for (int i = 0; i < 4; i++) {
            float x = c[i * 3], y = c[i * 3 + 1], z = c[i * 3 + 2];
            us[i] = nz != 0 ? x : z;
            vs[i] = ny != 0 ? x : -y;
        }
        // Corners go (s0,t0) (s1,t0) (s1,t1) (s0,t1); u follows s or t, and so does v.
        boolean uAlongS = Math.abs(us[1] - us[0]) > Math.abs(us[3] - us[0]);
        float uMin = Math.min(us[0], us[2]), uMax = Math.max(us[0], us[2]);
        float vMin = Math.min(vs[0], vs[2]), vMax = Math.max(vs[0], vs[2]);
        float[] ub = breaks(uMax - uMin, texelPx), vb = breaks(vMax - vMin, texelPx);
        int uSeed = Math.floorMod(seed, 31), vSeed = Math.floorMod(seed >> 5, 31);
        float tx = (t.index % 4) * TILE, ty = (t.index / 4) * TILE;
        boolean own = bound != null && bound != GLASS;
        for (int iu = 0; iu < 3; iu++)
            for (int iv = 0; iv < 3; iv++) {
                float u0 = uMin + ub[iu], u1 = uMin + ub[iu + 1], v0 = vMin + vb[iv], v1 = vMin + vb[iv + 1];
                if (u1 - u0 < 1e-4f || v1 - v0 < 1e-4f)
                    continue;
                // An image has no rim texels, so its rim cells are shaded instead: the ends and the underside darker.
                int cellArgb = own ? shade(argb, (iu != 1 ? 0.82f : 1f) * (iv == 2 ? 0.62f : 1f)) : argb;
                for (int k = 0; k < 4; k++) {
                    // This corner's (u, v) inside the cell, kept on the same side as the face corner it stands for.
                    float cu = us[k] == uMin ? u0 : u1, cv = vs[k] == vMin ? v0 : v1;
                    float fu = (cu - uMin) / Math.max(uMax - uMin, 1e-6f), fv = (cv - vMin) / Math.max(vMax - vMin, 1e-6f);
                    // Back to the face's own (s, t), then the world point by interpolating its corners.
                    float fs = uAlongS ? (us[0] == uMin ? fu : 1 - fu) : (vs[0] == vMin ? fv : 1 - fv);
                    float ft = uAlongS ? (vs[0] == vMin ? fv : 1 - fv) : (us[0] == uMin ? fu : 1 - fu);
                    float x = c[0] + (c[3] - c[0]) * fs + (c[9] - c[0]) * ft;
                    float y = c[1] + (c[4] - c[1]) * fs + (c[10] - c[1]) * ft;
                    float z = c[2] + (c[5] - c[2]) * fs + (c[11] - c[2]) * ft;
                    float u, v;
                    if (own) {
                        // A log's bark runs down its texture; turned a quarter so the grain runs along the part.
                        v = whole(cu - uMin, uMax - uMin, uSeed, texelPx);
                        u = whole(cv - vMin, vMax - vMin, vSeed, texelPx);
                    } else {
                        u = (tx + texel(iu, cu - uMin - ub[iu], ub, uMax - uMin, uSeed, texelPx)) / SHEET;
                        v = (ty + texel(iv, cv - vMin - vb[iv], vb, vMax - vMin, vSeed, texelPx)) / SHEET;
                    }
                    if (sprite != null && !own) {
                        u = sprite.getU(u);
                        v = sprite.getV(v);
                    }
                    vertex(p, x, y, z).setColor(cellArgb).setUv(u, v).setOverlay(overlay).setLight(lit).setNormal(p, nx, ny, nz);
                }
            }
    }

    /** Starts a vertex at a model point, moved toward the eye by the box's level ({@link #layered}). */
    private VertexConsumer vertex(PoseStack.Pose p, float x, float y, float z) {
        org.joml.Vector3f v = p.pose().transformPosition(x, y, z, scratch);
        if (level > 0 && ortho != null) {
            if (ortho) {
                // A model pixel along z is this long in GUI units.
                float zLen = new org.joml.Vector3f(p.pose().m20(), p.pose().m21(), p.pose().m22()).length();
                v.z += level * ORTHO_STEP_PX * zLen;
            } else {
                v.mul(1f - level * PERSPECTIVE_STEP);
            }
        }
        return out.addVertex(v.x, v.y, v.z);
    }

    /** A whole image's coordinate (0 to 1) at {@code into} of a face {@code span} long: 2 texels a pixel of its 16, stretched past that. */
    private static float whole(float into, float span, int seed, float texelPx) {
        float texels = Math.min(span / texelPx, GunWood.SIZE);
        float start = Math.floorMod(seed, (int) (GunWood.SIZE - texels) + 1);
        return (start + into / Math.max(span, 1e-6f) * texels) / GunWood.SIZE;
    }

    private static int shade(int argb, float f) {
        int r = Math.round((argb >> 16 & 0xFF) * f), g = Math.round((argb >> 8 & 0xFF) * f), b = Math.round((argb & 0xFF) * f);
        return argb & 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** The four cuts across a face {@code span} long: rim, middle, rim. */
    private static float[] breaks(float span, float texelPx) {
        float rim = Math.min(texelPx, span / 3f);
        return new float[]{0, rim, span - rim, span};
    }

    /**
     * The tile coordinate (texels from the tile's edge) at {@code into} along piece {@code piece}: the
     * rims are the tile's outermost texel row or column, the middle a patch of the 30 texels inside,
     * stretched once the middle is longer than that.
     */
    private static float texel(int piece, float into, float[] cuts, float span, int seed, float texelPx) {
        float width = cuts[piece + 1] - cuts[piece];
        float f = width <= 0 ? 0 : into / width;
        if (piece == 0)
            return f;
        if (piece == 2)
            return TILE - 1 + f;
        float texels = Math.max(1f, Math.min(width / texelPx, TILE - 2));
        float start = 1 + Math.floorMod(seed, (int) (TILE - 2 - texels) + 1);
        return start + f * texels;
    }
}
