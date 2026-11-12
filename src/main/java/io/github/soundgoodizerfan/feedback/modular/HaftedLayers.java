// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part;

/**
 * Where each layer of a hafted tool's sprite goes: the pure half of the layered drawing (the client turns the placements into quads).
 * Every layer is a 16x16 grey sprite drawn at the reference pose (one stick of haft, no extra edge); a placement moves it by a whole
 * number of texels and never scales it, so one texel stays 1/16 of a block face. The contract the sprites follow is
 * {@code docs/specs/hafted_sprite_layers_spec.md}.
 *
 * <p>Stick count moves the socket along the haft and stamps the haft's one-step slice to fill; the head's edge length stamps the head's
 * one-step middle slice between its two ends. A layer whose files are missing falls back (variant to plain), and a tool whose haft or head
 * has no files at all is not drawn in layers (the whole-tool sprite is).
 */
public final class HaftedLayers {

    private HaftedLayers() {
    }

    /** Texel size of every layer. */
    public static final int SIZE = 16;
    /** The most extra edge slices a head grows by. */
    public static final int MAX_EDGE_STEPS = 3;
    /** Steps of haft a stick adds beyond the first. */
    public static final int STEPS_PER_STICK = 2;

    /** What the layer drawing needs to know about the sprites: whether a file exists, and where its opaque texels are. */
    public interface Sprites {
        boolean has(String file);

        /** {@code {minX, minY, maxX, maxY}} of the opaque texels, or null if there are none. */
        int[] bounds(String file);
    }

    /** One layer in place: its sprite file (under {@code textures/item/hafted/}), the role whose material tints it, and its shift in texels (y down). */
    public record Placement(String file, String role, int dx, int dy) {
    }

    /** The head kinds' file names, by the form a head part records. */
    private static final Map<String, String> KIND_FILE = Map.ofEntries(
            Map.entry("pickaxe_head", "pickaxe"), Map.entry("axe_head", "axe"), Map.entry("shovel_head", "shovel"),
            Map.entry("hoe_head", "hoe"), Map.entry("maul_head", "maul"), Map.entry("sickle_head", "sickle"),
            Map.entry("cleaver_head", "cleaver"), Map.entry("sword_blade", "sword"), Map.entry("spear_head", "spear"),
            Map.entry("head_blank", "hand_hammer"), Map.entry("knife_blade", "knife"), Map.entry("file_head", "file"),
            Map.entry("driver_bit", "screwdriver"));
    /** The kinds whose edge runs with the haft rather than across it. */
    private static final Set<String> ALONG = Set.of("sword", "knife", "spear");

    /** The file name of a head kind (a form such as {@code pickaxe_head}), or empty. */
    public static Optional<String> kindFile(String form) {
        return Optional.ofNullable(KIND_FILE.get(form));
    }

    /** The layers of {@code design}, bottom to top; empty when it cannot be drawn in layers. */
    public static List<Placement> of(ModularDesign design, ModularType type, Sprites sprites) {
        Part haft = design.part("S");
        if (haft == null)
            return List.of();
        String form = haft.form().isEmpty() ? "stick" : haft.form();
        if (!sprites.has("handle_" + form + "_base") || !sprites.has("handle_" + form + "_ext"))
            return List.of();
        int sticks = Math.max(1, Math.min(4, haft.count()));
        int steps = STEPS_PER_STICK * (sticks - 1);
        List<Placement> out = new ArrayList<>();
        out.add(new Placement("handle_" + form + "_base", "S", 0, 0));
        for (int j = 1; j <= steps; j++)
            out.add(new Placement("handle_" + form + "_ext", "S", j, -j));
        // Parts at the butt stay; parts at the socket ride up the haft.
        part(out, sprites, design, "Z", "edge_guard", steps);
        part(out, sprites, design, "Y", "lanyard", 0);
        part(out, sprites, design, "R", "spring", 0);
        part(out, sprites, design, "P", "pommel", 0);
        part(out, sprites, design, "Q", "sword_pommel", 0);
        part(out, sprites, design, "G", "guard", steps);
        part(out, sprites, design, "B", "binding", steps);
        boolean any = false;
        int tipX = 0, tipY = 0;
        for (String letter : ModularFaces.LETTERS) {
            Part face = design.part(letter);
            ModularType.Role role = type.roles().get(letter);
            if (face == null || role == null)
                continue;
            Optional<String> kind = kindFile(ModularHeads.kindOf(face, role));
            if (kind.isEmpty())
                continue;
            int c = head(out, sprites, face, role, kind.get(), steps, !letter.equals("H"));
            if (c < 0)
                continue;
            if (!any) {
                // The tip rides the front head's far end: out by the slices that end took.
                tipX = c;
                tipY = ALONG.contains(kind.get()) ? -c : c;
            }
            any = true;
        }
        if (!any)
            return List.of();
        tip(out, sprites, design, steps, tipX, tipY);
        return List.copyOf(out);
    }

    /** A part's layer: its variant's file if there is one, else the plain file, else nothing. {@code lift} steps up the haft ride it with the socket. */
    private static void part(List<Placement> out, Sprites sprites, ModularDesign design, String role, String name, int lift) {
        Part p = design.part(role);
        if (p == null)
            return;
        String variant = p.variant().isEmpty() ? name : name + "_" + p.variant();
        String file = sprites.has(variant) ? variant : sprites.has(name) ? name : null;
        if (file != null)
            out.add(new Placement(file, role, lift, -lift));
    }

    /** The tip: its variant's file (else plain) at the socket, moved out by the head's lengthening if it still fits the canvas, else as far as it does. */
    private static void tip(List<Placement> out, Sprites sprites, ModularDesign design, int steps, int tipX, int tipY) {
        Part p = design.part("T");
        if (p == null)
            return;
        String variant = p.variant().isEmpty() ? "tip" : "tip_" + p.variant();
        String file = sprites.has(variant) ? variant : sprites.has("tip") ? "tip" : null;
        if (file == null)
            return;
        for (int back = 0; back <= Math.abs(tipX); back++) {
            int x = tipX - Integer.signum(tipX) * back, y = tipY - Integer.signum(tipY) * back;
            Placement at = new Placement(file, "T", steps + x, -steps + y);
            if (fits(sprites, List.of(at)) || back == Math.abs(tipX)) {
                out.add(at);
                return;
            }
        }
    }

    /** A head in its three cuts, lengthened by its edge axis as far as the canvas allows: the slices its far end moved out by, or -1 if its files are missing. */
    private static int head(List<Placement> out, Sprites sprites, Part face, ModularType.Role role, String kind, int steps, boolean mirrored) {
        String stem = "head_" + kind;
        String named = face.variant().isEmpty() ? null : stem + "_" + face.variant();
        String use = named != null && cuts(sprites, named, mirrored) ? named : cuts(sprites, stem, mirrored) ? stem : null;
        if (use == null)
            return -1;
        String r = mirrored ? "_r" : "";
        int dirX = 1, dirY = ALONG.contains(kind) ? -1 : 1;
        for (int k = edgeSteps(face, role); k >= 0; k--) {
            // The back end steps out by half the extra slices, the front by the rest; the slices fill between.
            int a = (k + 1) / 2, c = k / 2;
            List<Placement> cut = new ArrayList<>();
            cut.add(new Placement(use + "_back" + r, face.role(), steps - a * dirX, -steps - a * dirY));
            for (int j = -a; j <= c; j++)
                cut.add(new Placement(use + "_mid" + r, face.role(), steps + j * dirX, -steps + j * dirY));
            cut.add(new Placement(use + "_front" + r, face.role(), steps + c * dirX, -steps + c * dirY));
            if (k == 0 || fits(sprites, cut)) {
                out.addAll(cut);
                return c;
            }
        }
        return 0;
    }

    private static boolean cuts(Sprites sprites, String stem, boolean mirrored) {
        String r = mirrored ? "_r" : "";
        return sprites.has(stem + "_back" + r) && sprites.has(stem + "_mid" + r) && sprites.has(stem + "_front" + r);
    }

    /** Extra middle slices the head's edge axis asks for: none at the default, {@link #MAX_EDGE_STEPS} at the top of the range. */
    static int edgeSteps(Part face, ModularType.Role role) {
        for (Map.Entry<String, ModularType.Axis> e : role.axes().entrySet()) {
            if (!e.getKey().equals("edge_length") && !e.getKey().equals("length"))
                continue;
            ModularType.Axis axis = e.getValue();
            float value = face.measures().axis(e.getKey(), axis.def());
            float span = axis.max() - axis.def();
            if (span <= 0 || value <= axis.def())
                return 0;
            return Math.min(MAX_EDGE_STEPS, Math.round(MAX_EDGE_STEPS * (value - axis.def()) / span));
        }
        return 0;
    }

    /** Whether every layer, shifted, keeps all its opaque texels on the canvas. */
    private static boolean fits(Sprites sprites, List<Placement> layers) {
        for (Placement p : layers) {
            int[] b = sprites.bounds(p.file());
            if (b != null && (b[0] + p.dx() < 0 || b[2] + p.dx() >= SIZE || b[1] + p.dy() < 0 || b[3] + p.dy() >= SIZE))
                return false;
        }
        return true;
    }
}
