// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.soundgoodizerfan.feedback.weapon.GunLayout;
import io.github.soundgoodizerfan.feedback.weapon.MagazineSize;

/**
 * Dev tool, run offline by {@code scripts/gun_sheet.py}: reads gun specs from stdin, one per line, draws each through
 * {@link GunDraw} with a {@link GunMesh.Probe}, and prints every face so the script can paint any view of it.
 *
 * <p>A spec is space-separated {@code key=value}: {@code type} (handgun, long_gun, machine_gun), {@code action}, {@code slide} (0/1),
 * {@code electric} (0/1), {@code len}, {@code r}, {@code bore}, {@code len2}, {@code r2} (a second barrel), {@code mag}
 * (none, small, standard, extended, drum, belt), {@code rounds}, {@code cell} (battery charge 0 to 1, -1 none), {@code lit} (chambers
 * or bank charge 0 to 6), {@code heat}, {@code charge} (hold or pump charge 0 to 1, -1 none), {@code parts} (comma-separated roles fitted besides R), {@code open} (a break or core-breech pose, 0 to 1), {@code var.X=name} (role X's variant).
 * Output per gun: {@code gun N}, then {@code role tile level nx ny nz x y z x y z x y z x y z} per face (model pixels, bore along -z).
 */
public final class GunSheetDump {

    private GunSheetDump() {
    }

    public static void main(String[] args) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        StringBuilder out = new StringBuilder();
        int n = 0;
        for (String line; (line = in.readLine()) != null; ) {
            if (line.isBlank())
                continue;
            out.append("gun ").append(n++).append('\n');
            GunView v = parse(line);
            GunMesh m = new GunMesh(new PoseStack(), image -> NOWHERE, 0xF000F0, 0, null);
            m.probe = (box, level, role, tile, nm, c) -> {
                out.append(role.isEmpty() ? "-" : role).append(' ').append(tile).append(' ').append(level);
                for (float f : nm)
                    out.append(' ').append(f);
                for (float[] p : c)
                    out.append(' ').append(p[0]).append(' ').append(p[1]).append(' ').append(p[2]);
                out.append('\n');
            };
            java.util.regex.Matcher open = java.util.regex.Pattern.compile("\\bopen=([0-9.]+)").matcher(line);
            float o = open.find() ? Float.parseFloat(open.group(1)) : 0f; // a break pose, 0 to 1
            GunDraw.draw(m, v, new GunMotion.Parts(0, 0, 0, 0, 0, o, 0, 0, 0, false, 0, 0, -1, -1, 0, -1, 0, -1));
        }
        System.out.print(out);
    }

    private static GunView parse(String line) {
        Map<String, String> kv = new HashMap<>();
        Map<String, String> variants = new HashMap<>();
        for (String tok : line.trim().split("\\s+")) {
            int eq = tok.indexOf('=');
            if (tok.startsWith("var."))
                variants.put(tok.substring(4, eq), tok.substring(eq + 1));
            else
                kv.put(tok.substring(0, eq), tok.substring(eq + 1));
        }
        GunLayout.Type type = GunLayout.Type.valueOf(kv.getOrDefault("type", "long_gun").toUpperCase());
        boolean electric = kv.getOrDefault("electric", "0").equals("1");
        boolean slide = kv.getOrDefault("slide", "0").equals("1");
        Map<String, Integer> colours = new HashMap<>();
        colours.put("R", 0xFF808080);
        colours.put("B", 0xFF707070);
        String parts = kv.getOrDefault("parts", "");
        for (String r : parts.isEmpty() ? new String[0] : parts.split(","))
            colours.put(r, 0xFF909090);
        String mag = kv.getOrDefault("mag", "none");
        MagazineSize size = mag.equals("none") ? null : MagazineSize.valueOf(mag.toUpperCase());
        GunView.Pulse pulse = electric ? new GunView.Pulse(Float.parseFloat(kv.getOrDefault("angle", "0")), Float.parseFloat(kv.getOrDefault("lit", "6")),
                Float.parseFloat(kv.getOrDefault("heat", "0")), 0x3F7FFF, Float.parseFloat(kv.getOrDefault("charge", "-1")), kv.getOrDefault("hot", "0").equals("1")) : null;
        return new GunView(type, kv.getOrDefault("action", "none"), slide, f(kv, "len", 14f), f(kv, "r", 0.75f), f(kv, "bore", 0.375f),
                f(kv, "len2", 0f), f(kv, "r2", 0f), colours, variants, Map.of(), Map.of(), 0, size, Integer.parseInt(kv.getOrDefault("rounds", "5")),
                electric, pulse, f(kv, "cell", electric ? 0.6f : -1f));
    }

    private static float f(Map<String, String> kv, String k, float d) {
        return kv.containsKey(k) ? Float.parseFloat(kv.get(k)) : d;
    }

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
