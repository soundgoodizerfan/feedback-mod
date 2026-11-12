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

    /**
     * One layer in place: its sprite file (under {@code textures/item/hafted/}), the role whose material tints it, its shift in texels (y down),
     * and which side of the haft's line it keeps ({@link #SPLIT}): 0 all of it, -1 only up-left of the line, +1 only down-right.
     */
    public record Placement(String file, String role, int dx, int dy, int side) {
        public Placement(String file, String role, int dx, int dy) {
            this(file, role, dx, dy, 0);
        }

        /** Whether this layer draws its texel at canvas (x, y). */
        public boolean keeps(int x, int y) {
            int d = x + y - SPLIT;
            return side == 0 || (side < 0 ? d <= 0 : d >= 0);
        }
    }

    /**
     * The haft's line on the canvas, {@code x + y = SPLIT}: the haft runs along (+1,-1) through the socket, (6,9) at the reference pose, and
     * neither a haft step nor {@link #ORIGIN_X}/{@link #ORIGIN_Y} moves it off the line. With a front and a second face fitted, each head keeps
     * only its own side of it, so the two merge at the eye instead of each drawing its poll over the other's blade.
     */
    public static final int SPLIT = 15;

    /**
     * The tip's placement file stem: {@code tip} or {@code tip_<variant>}. The tip has no shape of its own; the renderer re-tints the front
     * head's own texels farthest from the socket (how deep by variant), so it always lands on the head's point, spike or bit whatever the
     * kind, variant or edge length. A sprite of that name, if there is one, is an overlay texture sampled at canvas position over those texels.
     */
    public static final String TIP = "tip";

    /** Canvas texel of the socket at the reference pose: the head side of the haft's base layer. */
    public static final int SOCKET_X = 6, SOCKET_Y = 9;

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

    /** Where a tool with no pommel sits on the canvas: up and right of the corner, as vanilla's tools do, so nothing is clipped at the edge. */
    public static final int ORIGIN_X = 2, ORIGIN_Y = -2;

    /**
     * How far off the haft's line a head's body sits, in texels, by head kind: vanilla seats its axe, pick and hoe heads with the body on the
     * upper-left side of the haft and the haft entering at the head's lower right. Applied to the front head (negated for a mirrored second
     * head); the same for every variant of the kind.
     */
    private static final Map<String, int[]> SEAT = Map.of("axe", new int[] {-1, -1}, "pickaxe", new int[] {-1, -1}, "hoe", new int[] {-1, -1});

    /** The layers of {@code design}, bottom to top; empty when it cannot be drawn in layers. A haft too long for the canvas is drawn as long as fits. */
    public static List<Placement> of(ModularDesign design, ModularType type, Sprites sprites) {
        Part haft = design.part("S");
        if (haft == null)
            return List.of();
        int sticks = Math.max(1, Math.min(4, haft.count()));
        List<Placement> last = List.of();
        for (int steps = STEPS_PER_STICK * (sticks - 1); steps >= 0; steps--) {
            last = shifted(place(design, type, sprites, steps, true));
            if (last.isEmpty() || fits(sprites, last))
                return last;
        }
        return last;
    }

    /**
     * The layers at the tool's true length: every stick and edge slice drawn, never shortened to fit the canvas, so layers may run past
     * it. For the tool held or lying in the world, where a longer tool takes up more space at the same texel size; {@link #of} is the
     * inventory icon, which only has to fit its slot.
     */
    public static List<Placement> full(ModularDesign design, ModularType type, Sprites sprites) {
        Part haft = design.part("S");
        if (haft == null)
            return List.of();
        int sticks = Math.max(1, Math.min(4, haft.count()));
        return shifted(place(design, type, sprites, STEPS_PER_STICK * (sticks - 1), false));
    }

    /** The layers moved to where the tool sits on the canvas: in the corner when a pommel is fitted (it sits flush there), else off it by the origin. */
    private static List<Placement> shifted(List<Placement> layers) {
        boolean pommel = layers.stream().anyMatch(p -> p.role().equals("P") || p.role().equals("Q"));
        int ox = pommel ? 0 : ORIGIN_X, oy = pommel ? 0 : ORIGIN_Y;
        List<Placement> out = new ArrayList<>(layers.size());
        for (Placement p : layers)
            out.add(new Placement(p.file(), p.role(), p.dx() + ox, p.dy() + oy, p.side()));
        return List.copyOf(out);
    }

    private static List<Placement> place(ModularDesign design, ModularType type, Sprites sprites, int steps, boolean clamp) {
        Part haft = design.part("S");
        if (haft == null)
            return List.of();
        String form = haft.form().isEmpty() ? "stick" : haft.form();
        if (!sprites.has("handle_" + form + "_base") || !sprites.has("handle_" + form + "_ext"))
            return List.of();
        List<Placement> out = new ArrayList<>();
        out.add(new Placement("handle_" + form + "_base", "S", 0, 0));
        for (int j = 1; j <= steps; j++)
            out.add(new Placement("handle_" + form + "_ext", "S", j, -j));
        // Parts at the butt stay; parts at the socket ride up the haft.
        String front = frontKind(design, type);
        part(out, sprites, design, "Z", "edge_guard", steps, front);
        part(out, sprites, design, "Y", "lanyard", 0);
        part(out, sprites, design, "R", "spring", 0);
        part(out, sprites, design, "P", "pommel", 0);
        part(out, sprites, design, "Q", "sword_pommel", 0);
        part(out, sprites, design, "G", "guard", steps);
        part(out, sprites, design, "B", "binding", steps);
        boolean any = false, twin = twin(design, type);
        for (String letter : ModularFaces.LETTERS) {
            Part face = design.part(letter);
            ModularType.Role role = type.roles().get(letter);
            if (face == null || role == null)
                continue;
            Optional<String> kind = kindFile(ModularHeads.kindOf(face, role));
            if (kind.isEmpty())
                continue;
            int side = !twin ? 0 : letter.equals("H") ? -1 : letter.equals("J") ? 1 : 0;
            any |= head(out, sprites, face, role, kind.get(), steps, !letter.equals("H"), side, clamp) >= 0;
        }
        if (!any)
            return List.of();
        Part tip = design.part("T");
        if (tip != null)
            out.add(new Placement(tip.variant().isEmpty() ? TIP : TIP + "_" + tip.variant(), "T", 0, 0));
        return List.copyOf(out);
    }

    /** A part's layer: its variant's file if there is one, else the plain file, else nothing. {@code lift} steps up the haft ride it with the socket. */
    private static void part(List<Placement> out, Sprites sprites, ModularDesign design, String role, String name, int lift) {
        part(out, sprites, design, role, name, lift, null);
    }

    /** As above, but a part that depends on the head ({@code kind}, a file kind name) looks for its kind's file first: {@code name_kind_variant}, {@code name_kind}. */
    private static void part(List<Placement> out, Sprites sprites, ModularDesign design, String role, String name, int lift, String kind) {
        Part p = design.part(role);
        if (p == null)
            return;
        String file = fileFor(sprites, name, p.variant(), kind);
        if (file != null)
            out.add(new Placement(file, role, lift, -lift));
    }

    private static String fileFor(Sprites sprites, String name, String variant, String kind) {
        List<String> tries = new ArrayList<>();
        if (kind != null) {
            if (!variant.isEmpty())
                tries.add(name + "_" + kind + "_" + variant);
            tries.add(name + "_" + kind);
        }
        if (!variant.isEmpty())
            tries.add(name + "_" + variant);
        tries.add(name);
        for (String t : tries)
            if (sprites.has(t))
                return t;
        return null;
    }

    /** The file kind of the front head, the one parts that depend on the head follow; null if it has no layer files. */
    private static String frontKind(ModularDesign design, ModularType type) {
        for (String letter : ModularFaces.LETTERS) {
            Part face = design.part(letter);
            ModularType.Role role = type.roles().get(letter);
            if (face != null && role != null)
                return kindFile(ModularHeads.kindOf(face, role)).orElse(null);
        }
        return null;
    }

    /** A head in its three cuts, lengthened by its edge axis as far as the canvas allows: the slices its far end moved out by, or -1 if its files are missing. */
    private static int head(List<Placement> out, Sprites sprites, Part face, ModularType.Role role, String kind, int steps, boolean mirrored, int side,
                            boolean clamp) {
        String stem = "head_" + kind;
        String named = face.variant().isEmpty() ? null : stem + "_" + face.variant();
        String use = named != null && cuts(sprites, named, mirrored) ? named : cuts(sprites, stem, mirrored) ? stem : null;
        if (use == null)
            return -1;
        String r = mirrored ? "_r" : "";
        int dirX = 1, dirY = ALONG.contains(kind) ? -1 : 1;
        int[] seat = SEAT.getOrDefault(kind, new int[] {0, 0});
        int sx = mirrored ? -seat[0] : seat[0], sy = mirrored ? -seat[1] : seat[1];
        for (int k = edgeSteps(face, role); k >= 0; k--) {
            // The back end steps out by half the extra slices, the front by the rest; the slices fill between.
            int a = (k + 1) / 2, c = k / 2;
            List<Placement> cut = new ArrayList<>();
            cut.add(new Placement(use + "_back" + r, face.role(), steps - a * dirX + sx, -steps - a * dirY + sy, side));
            for (int j = -a; j <= c; j++)
                cut.add(new Placement(use + "_mid" + r, face.role(), steps + j * dirX + sx, -steps + j * dirY + sy, side));
            cut.add(new Placement(use + "_front" + r, face.role(), steps + c * dirX + sx, -steps + c * dirY + sy, side));
            if (k == 0 || !clamp || fits(sprites, shifted(cut))) {
                out.addAll(cut);
                return c;
            }
        }
        return 0;
    }

    /** Whether the front and second faces are both fitted and both lie across the haft, so each keeps its own side (a blade along the haft is never halved). */
    private static boolean twin(ModularDesign design, ModularType type) {
        for (String letter : List.of("H", "J")) {
            Part face = design.part(letter);
            ModularType.Role role = type.roles().get(letter);
            if (face == null || role == null)
                return false;
            Optional<String> kind = kindFile(ModularHeads.kindOf(face, role));
            if (kind.isEmpty() || ALONG.contains(kind.get()))
                return false;
        }
        return true;
    }

    private static boolean cuts(Sprites sprites, String stem, boolean mirrored) {
        String r = mirrored ? "_r" : "";
        return sprites.has(stem + "_back" + r) && sprites.has(stem + "_mid" + r) && sprites.has(stem + "_front" + r);
    }

    /** Extra middle slices the head's edge axis asks for: none at the default, {@link #MAX_EDGE_STEPS} at the top of the range. */
    static int edgeSteps(Part face, ModularType.Role role) {
        for (Map.Entry<String, ModularType.Axis> e : role.axesFor(face).entrySet()) {
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

    /** Whether every layer, shifted, keeps all its opaque texels on the canvas (the tip is drawn on the head, so it always does). */
    private static boolean fits(Sprites sprites, List<Placement> layers) {
        for (Placement p : layers) {
            if (p.role().equals("T"))
                continue;
            int[] b = sprites.bounds(p.file());
            if (b != null && (b[0] + p.dx() < 0 || b[2] + p.dx() >= SIZE || b[1] + p.dy() < 0 || b[3] + p.dy() >= SIZE))
                return false;
        }
        return true;
    }
}
