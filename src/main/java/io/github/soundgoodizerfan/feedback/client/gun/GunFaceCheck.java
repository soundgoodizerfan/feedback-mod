// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.soundgoodizerfan.feedback.weapon.GunLayout;
import io.github.soundgoodizerfan.feedback.weapon.MagazineSize;

/**
 * Dev check, run offline by {@code scripts/check_gun_faces.sh}: draws a sweep of synthetic guns through
 * {@link GunDraw} with a {@link GunMesh.Probe} and lists every pair of faces from different boxes that lie in one
 * plane, face the same way, overlap and draw at one level ({@link GunMesh#layered}: a role's layer and its draw order
 * in that role). Those z-fight: the renderer culls back faces, so faces turned toward each other are fine, and faces
 * at different levels are biased apart in depth, but two faces at one level turned the same way in one plane both
 * draw. Exits 1 when any are found.
 *
 * <p>The sweep is each receiver and action, every magazine, three barrel sizes, and for each: no optional parts,
 * all of them, each alone, and all but each; with every variant of the grip, stock, sight and electric barrel.
 * Moving parts sit at rest and the scope is not sighted.
 */
public final class GunFaceCheck {

    private GunFaceCheck() {
    }

    private record Face(int box, int level, String role, GunMesh.Tile tile, float[] n, float[][] c) {
    }

    /** Optional roles a gun may fit (the receiver R is always there). */
    private static final String[] OPTIONAL = {"A", "K", "G", "T", "i", "S", "e", "F", "D", "H", "Z", "J", "Q", "P", "V",
            "N", "M", "E", "C", "I", "a", "L", "d", "b", "Y"};

    private record Base(String name, GunLayout.Type type, String action, boolean slide, boolean electric) {
    }

    private static final List<Base> BASES = List.of(
            new Base("pistol", GunLayout.Type.HANDGUN, "self_loading", true, false),
            new Base("revolver", GunLayout.Type.HANDGUN, "none", false, false),
            new Base("bolt rifle", GunLayout.Type.LONG_GUN, "bolt", false, false),
            new Base("lever rifle", GunLayout.Type.LONG_GUN, "lever", false, false),
            new Base("pump", GunLayout.Type.LONG_GUN, "pump", false, false),
            new Base("break", GunLayout.Type.LONG_GUN, "break", false, false),
            new Base("self-loading rifle", GunLayout.Type.LONG_GUN, "self_loading", true, false),
            new Base("automatic rifle", GunLayout.Type.LONG_GUN, "automatic", false, false),
            new Base("machine gun", GunLayout.Type.MACHINE_GUN, "automatic", false, false),
            new Base("electric pistol", GunLayout.Type.HANDGUN, "none", false, true),
            new Base("electric rifle", GunLayout.Type.LONG_GUN, "none", false, true),
            new Base("electric machine gun", GunLayout.Type.MACHINE_GUN, "none", false, true));

    private static final Map<String, String[]> VARIANTS = Map.of(
            "G", new String[]{"pistol_grip", "straight", "finger_groove", "palm_swell"},
            "i", new String[]{"fixed", "skeleton", "folding"},
            "S", new String[]{"reflex", "scope"},
            "P", new String[]{"thread_breech", "thread_middle", "thread_muzzle"},
            "B", new String[]{"coil", "rail", "plasma"});

    private static final float[][] BARRELS = {{2f, 0.5f, 0.25f}, {6f, 0.75f, 0.375f}, {14f, 1.25f, 0.5f}};

    public static void main(String[] args) {
        // problem key -> the first few guns it shows on
        Map<String, Set<String>> found = new TreeMap<>();
        int guns = 0;
        for (Base base : BASES)
            for (MagazineSize mag : withNull(MagazineSize.values()))
                for (float[] barrel : BARRELS)
                    for (Set<String> fitted : fittings())
                        for (Map<String, String> variants : variantSets()) {
                            guns++;
                            GunView v = view(base, mag, barrel, fitted, variants);
                            String label = base.name() + ", mag " + mag + ", barrel " + barrel[0] + "/" + barrel[1] + ", fitted " + fitted + " " + variants;
                            for (String problem : check(v))
                                found.computeIfAbsent(problem, k -> new LinkedHashSet<>()).add(label);
                        }
        for (var e : found.entrySet()) {
            System.out.println(e.getKey() + "  (" + e.getValue().size() + " guns)");
            e.getValue().stream().limit(2).forEach(g -> System.out.println("    e.g. " + g));
        }
        System.err.println(found.size() + " coplanar overlaps in " + guns + " guns");
        System.exit(found.isEmpty() ? 0 : 1);
    }

    private static MagazineSize[] withNull(MagazineSize[] all) {
        MagazineSize[] out = new MagazineSize[all.length + 1];
        System.arraycopy(all, 0, out, 1, all.length);
        return out;
    }

    private static List<Set<String>> fittings() {
        List<Set<String>> out = new ArrayList<>();
        Set<String> all = new LinkedHashSet<>(List.of(OPTIONAL));
        out.add(Set.of());
        out.add(all);
        for (String r : OPTIONAL) {
            out.add(Set.of(r));
            Set<String> but = new LinkedHashSet<>(all);
            but.remove(r);
            out.add(but);
        }
        return out;
    }

    /** One set per index up to the longest variant list: the i-th variant of each role (its last when it has fewer). */
    private static List<Map<String, String>> variantSets() {
        int most = VARIANTS.values().stream().mapToInt(a -> a.length).max().orElse(1);
        List<Map<String, String>> out = new ArrayList<>();
        for (int i = 0; i < most; i++) {
            Map<String, String> m = new TreeMap<>();
            for (var e : VARIANTS.entrySet())
                m.put(e.getKey(), e.getValue()[Math.min(i, e.getValue().length - 1)]);
            out.add(m);
        }
        return out;
    }

    private static GunView view(Base base, MagazineSize mag, float[] barrel, Set<String> fitted, Map<String, String> variants) {
        Map<String, Integer> colours = new HashMap<>();
        colours.put("R", 0xFF808080);
        colours.put("B", 0xFF707070);
        for (String r : fitted)
            colours.put(r, 0xFF909090);
        float len2 = fitted.contains("D") ? barrel[0] * 0.8f : 0, r2 = fitted.contains("D") ? 0.5f : 0;
        return new GunView(base.type(), base.action(), base.slide(), barrel[0], barrel[1], barrel[2], len2, r2, colours,
                variants, Map.of(), Map.of(), 0, mag, 5, base.electric(), base.electric() ? 4 : 0, base.electric() ? 0.6f : -1f);
    }

    /** Draws one gun and returns its problems, each as "role tile / role tile at plane". */
    private static List<String> check(GunView v) {
        List<Face> faces = new ArrayList<>();
        GunMesh m = new GunMesh(new PoseStack(), image -> NOWHERE, 0xF000F0, 0, null);
        m.probe = (box, level, role, tile, n, c) -> faces.add(new Face(box, level, role, tile, n, c));
        GunDraw.draw(m, v, GunMotion.Parts.REST);
        // Faces closer than this along their normal share a plane, unless their levels set them apart (GunMesh.layered).
        float near = 1f / 1024f;
        Map<String, List<Face>> byNormal = new HashMap<>();
        for (Face f : faces) {
            String key = Math.round(f.n[0] * 1000) + "," + Math.round(f.n[1] * 1000) + "," + Math.round(f.n[2] * 1000);
            byNormal.computeIfAbsent(key, k -> new ArrayList<>()).add(f);
        }
        List<String> out = new ArrayList<>();
        for (List<Face> group : byNormal.values()) {
            group.sort(java.util.Comparator.comparingDouble(GunFaceCheck::plane));
            for (int i = 0; i < group.size(); i++)
                for (int j = i + 1; j < group.size() && plane(group.get(j)) - plane(group.get(i)) < near; j++) {
                    Face a = group.get(i), b = group.get(j);
                    if (a.box == b.box || a.level != b.level || overlap(a, b) < 1e-4f)
                        continue;
                    String sa = a.role + " " + a.tile, sb = b.role + " " + b.tile;
                    String pair = sa.compareTo(sb) <= 0 ? sa + " / " + sb : sb + " / " + sa;
                    out.add(pair + " facing " + axis(a.n) + " at " + fmt(a.n, a.c[0]));
                }
        }
        return out;
    }

    private static float plane(Face f) {
        return f.n[0] * f.c[0][0] + f.n[1] * f.c[0][1] + f.n[2] * f.c[0][2];
    }

    private static String axis(float[] n) {
        String[] names = {"x", "y", "z"};
        int k = Math.abs(n[0]) >= Math.abs(n[1]) && Math.abs(n[0]) >= Math.abs(n[2]) ? 0 : Math.abs(n[1]) >= Math.abs(n[2]) ? 1 : 2;
        return (n[k] > 0 ? "+" : "-") + names[k] + (Math.abs(n[k]) < 0.999f ? " (tilted)" : "");
    }

    private static String fmt(float[] n, float[] p) {
        return String.format("%s=%.2f", axis(n).substring(1, 2), n[0] * p[0] + n[1] * p[1] + n[2] * p[2]);
    }

    /** Area two coplanar convex quads share: both projected onto the plane's two other axes, one clipped by the other. */
    private static float overlap(Face a, Face b) {
        float[] n = a.n;
        float[] u = Math.abs(n[0]) < 0.9f ? new float[]{1, 0, 0} : new float[]{0, 1, 0};
        float[] e1 = cross(n, u), e2 = cross(n, e1);
        List<float[]> pa = project(a.c, e1, e2), pb = project(b.c, e1, e2);
        List<float[]> clipped = clip(pa, pb);
        return Math.abs(area(clipped));
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static List<float[]> project(float[][] c, float[] e1, float[] e2) {
        List<float[]> out = new ArrayList<>();
        for (float[] p : c)
            out.add(new float[]{p[0] * e1[0] + p[1] * e1[1] + p[2] * e1[2], p[0] * e2[0] + p[1] * e2[1] + p[2] * e2[2]});
        if (area(out) < 0)
            java.util.Collections.reverse(out);
        return out;
    }

    private static float area(List<float[]> p) {
        float s = 0;
        for (int i = 0; i < p.size(); i++) {
            float[] a = p.get(i), b = p.get((i + 1) % p.size());
            s += a[0] * b[1] - b[0] * a[1];
        }
        return s / 2;
    }

    /** Sutherland-Hodgman: {@code subject} clipped by the convex, counter-clockwise {@code by}. */
    private static List<float[]> clip(List<float[]> subject, List<float[]> by) {
        List<float[]> out = subject;
        for (int i = 0; i < by.size() && !out.isEmpty(); i++) {
            float[] a = by.get(i), b = by.get((i + 1) % by.size());
            List<float[]> in = out;
            out = new ArrayList<>();
            for (int j = 0; j < in.size(); j++) {
                float[] p = in.get(j), q = in.get((j + 1) % in.size());
                float sp = side(a, b, p), sq = side(a, b, q);
                if (sp >= 0)
                    out.add(p);
                if ((sp >= 0) != (sq >= 0)) {
                    float t = sp / (sp - sq);
                    out.add(new float[]{p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t});
                }
            }
        }
        return out;
    }

    private static float side(float[] a, float[] b, float[] p) {
        return (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0]);
    }

    /** Swallows the vertices: the check reads the probe only. */
    private static final VertexConsumer NOWHERE = new VertexConsumer() {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }
    };
}
