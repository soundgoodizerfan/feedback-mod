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

        /** The sum of red, green and blue of the texel (x, y) of a sprite, or -1 where it is transparent. */
        int lum(String file, int x, int y);
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

    /**
     * The edge guard's marker layer. It has no sprite: the guard is the outline texels of the heads that face away from the socket, drawn in the
     * guard's material, so it follows the edge of whatever head, variant and length is fitted (see {@link #flatten}).
     */
    public static final String EDGE_GUARD = "edge_guard";

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
    public static final int ORIGIN_X = 2, ORIGIN_Y = -1;

    /**
     * How far off the haft's line a head's body sits, in texels, by head kind: vanilla seats its axe, pick and hoe heads with the body on the
     * upper-left side of the haft and the haft entering at the head's lower right. Applied to the front head (negated for a mirrored second
     * head); the same for every variant of the kind.
     */
    private static final Map<String, int[]> SEAT = Map.of("axe", new int[] {-1, -1}, "pickaxe", new int[] {-1, -1}, "hoe", new int[] {-1, -1},
            "cleaver", new int[] {-1, 0}, "screwdriver", new int[] {1, 1});

    /** One head on the plan: its file kind ({@code pickaxe}), variant, extra edge slices, and the letter of the mount it sits on ({@code H}, {@code J}, {@code K}). */
    public record Face(String letter, String kind, String variant, int edge) {
    }

    /**
     * Everything the layer drawing reads of a build, as plain values: the haft's form and stick count, its faces in mount order (only those whose
     * head kind has layer files), and each addon part's variant by role letter ({@code Z} edge guard, {@code Y} lanyard, {@code R} spring,
     * {@code P} pommel, {@code G} guard, {@code B} binding, {@code T} tip). {@code frontKind} is the kind parts that depend
     * on the head follow, or null. Nothing here touches Minecraft, so {@code scripts/HaftedDump.java} runs the same placement as the game.
     */
    public record Plan(String haft, int sticks, List<Face> faces, Map<String, String> parts, String frontKind) {
    }

    /** The plan of {@code design}, or null when it has no haft. */
    public static Plan plan(ModularDesign design, ModularType type) {
        Part haft = design.part("S");
        if (haft == null)
            return null;
        List<Face> faces = new ArrayList<>();
        String front = null;
        boolean first = true;
        for (String letter : ModularFaces.LETTERS) {
            Part face = design.part(letter);
            ModularType.Role role = type.roles().get(letter);
            if (face == null || role == null)
                continue;
            Optional<String> kind = kindFile(ModularHeads.kindOf(face, role));
            if (first)
                front = kind.orElse(null);
            first = false;
            if (kind.isPresent())
                faces.add(new Face(letter, kind.get(), face.variant(), edgeSteps(face, role)));
        }
        Map<String, String> parts = new java.util.HashMap<>();
        for (String r : List.of("Z", "Y", "R", "P", "G", "B", "T")) {
            Part p = design.part(r);
            if (p != null)
                parts.put(r, p.variant());
        }
        return new Plan(haft.form().isEmpty() ? "stick" : haft.form(), Math.max(1, Math.min(4, haft.count())), List.copyOf(faces), Map.copyOf(parts), front);
    }

    /** The layers of {@code design}, bottom to top; empty when it cannot be drawn in layers. A haft too long for the canvas is drawn as long as fits. */
    public static List<Placement> of(ModularDesign design, ModularType type, Sprites sprites) {
        Plan plan = plan(design, type);
        return plan == null ? List.of() : of(plan, sprites);
    }

    /** As {@link #of(ModularDesign, ModularType, Sprites)}, from a plan. */
    public static List<Placement> of(Plan plan, Sprites sprites) {
        List<Placement> last = List.of();
        for (int steps = STEPS_PER_STICK * (plan.sticks() - 1); steps >= 0; steps--) {
            last = shifted(place(plan, sprites, steps, true));
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
        Plan plan = plan(design, type);
        return plan == null ? List.of() : full(plan, sprites);
    }

    /** As {@link #full(ModularDesign, ModularType, Sprites)}, from a plan. */
    public static List<Placement> full(Plan plan, Sprites sprites) {
        return shifted(place(plan, sprites, STEPS_PER_STICK * (plan.sticks() - 1), false));
    }

    /** The layers moved to where the tool sits on the canvas: in the corner when a pommel is fitted (it sits flush there), else off it by the origin. */
    private static List<Placement> shifted(List<Placement> layers) {
        boolean pommel = layers.stream().anyMatch(p -> p.role().equals("P"));
        int ox = pommel ? 0 : ORIGIN_X, oy = pommel ? 0 : ORIGIN_Y;
        List<Placement> out = new ArrayList<>(layers.size());
        for (Placement p : layers)
            out.add(new Placement(p.file(), p.role(), p.dx() + ox, p.dy() + oy, p.side()));
        return List.copyOf(out);
    }

    private static List<Placement> place(Plan plan, Sprites sprites, int steps, boolean clamp) {
        String form = plan.haft();
        if (!sprites.has("handle_" + form + "_base") || !sprites.has("handle_" + form + "_ext"))
            return List.of();
        // Heads that lie across the haft (the sides) share the socket; a head along it (a blade or spear) or on the top mount rides further up
        // the haft, just clear of whatever sits below it, so a halberd's axe and point read as two heads and not one tangle.
        List<Face> sides = new ArrayList<>(), tops = new ArrayList<>();
        for (Face face : plan.faces())
            (isTop(face) ? tops : sides).add(face);
        List<Placement> heads = new ArrayList<>();
        boolean any = false, twin = sides.size() == 2;
        for (int i = 0; i < sides.size(); i++) {
            // Two side heads face each other across the haft, each keeping its own side; one alone is never mirrored.
            int side = !twin ? 0 : i == 0 ? -1 : 1;
            any |= head(heads, sprites, sides.get(i), steps, twin && i == 1, side, clamp) >= 0;
        }
        int top = steps, below = heads.isEmpty() ? Integer.MIN_VALUE : reach(sprites, heads)[1];
        for (Face face : tops) {
            int lift = 0;
            if (below != Integer.MIN_VALUE) {
                List<Placement> probe = new ArrayList<>();
                head(probe, sprites, face, steps, false, 0, clamp);
                if (!probe.isEmpty())
                    lift = Math.max(0, Math.floorDiv(below + 1 - reach(sprites, probe)[0] + 1, 2));
            }
            List<Placement> mine = new ArrayList<>();
            any |= head(mine, sprites, face, steps + lift, false, 0, clamp) >= 0;
            if (!mine.isEmpty()) {
                // The highest head comes first in the list, so the tip (which goes on the first head layer) lands on the tool's far end.
                heads.addAll(0, mine);
                below = reach(sprites, mine)[1];
                top = Math.max(top, steps + lift);
            }
        }
        List<Placement> out = new ArrayList<>();
        out.add(new Placement("handle_" + form + "_base", "S", 0, 0));
        for (int j = 1; j <= top; j++)
            out.add(new Placement("handle_" + form + "_ext", "S", j, -j));
        // Parts at the butt stay; parts at the socket ride up the haft.
        part(out, sprites, plan, "Y", "lanyard", 0);
        part(out, sprites, plan, "R", "spring", 0);
        part(out, sprites, plan, "P", "pommel", 0);
        part(out, sprites, plan, "G", "guard", steps);
        part(out, sprites, plan, "B", "binding", steps);
        out.addAll(heads);
        if (!any)
            return List.of();
        if (plan.parts().containsKey("Z"))
            out.add(new Placement(EDGE_GUARD, "Z", 0, 0));
        String tip = plan.parts().get("T");
        if (tip != null)
            out.add(new Placement(tip.isEmpty() ? TIP : TIP + "_" + tip, "T", 0, 0));
        return List.copyOf(out);
    }

    /** A part's layer: its variant's file if there is one, else the plain file, else nothing. {@code lift} steps up the haft ride it with the socket. */
    private static void part(List<Placement> out, Sprites sprites, Plan plan, String role, String name, int lift) {
        part(out, sprites, plan, role, name, lift, null);
    }

    /** As above, but a part that depends on the head ({@code kind}, a file kind name) looks for its kind's file first: {@code name_kind_variant}, {@code name_kind}. */
    private static void part(List<Placement> out, Sprites sprites, Plan plan, String role, String name, int lift, String kind) {
        String variant = plan.parts().get(role);
        if (variant == null)
            return;
        String file = fileFor(sprites, name, variant, kind);
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

    /** A head in its three cuts, lengthened by its edge axis as far as the canvas allows: the slices its far end moved out by, or -1 if its files are missing. */
    private static int head(List<Placement> out, Sprites sprites, Face face, int steps, boolean mirrored, int side, boolean clamp) {
        String kind = face.kind();
        String stem = "head_" + kind;
        String named = face.variant().isEmpty() ? null : stem + "_" + face.variant();
        String use = named != null && cuts(sprites, named, mirrored) ? named : cuts(sprites, stem, mirrored) ? stem : null;
        if (use == null)
            return -1;
        String r = mirrored ? "_r" : "";
        int dirX = 1, dirY = ALONG.contains(kind) ? -1 : 1;
        int[] seat = SEAT.getOrDefault(kind, new int[] {0, 0});
        int sx = mirrored ? -seat[0] : seat[0], sy = mirrored ? -seat[1] : seat[1];
        for (int k = face.edge(); k >= 0; k--) {
            // The back end steps out by half the extra slices, the front by the rest; the slices fill between.
            int a = (k + 1) / 2, c = k / 2;
            List<Placement> cut = new ArrayList<>();
            cut.add(new Placement(use + "_back" + r, face.letter(), steps - a * dirX + sx, -steps - a * dirY + sy, side));
            for (int j = -a; j <= c; j++)
                cut.add(new Placement(use + "_mid" + r, face.letter(), steps + j * dirX + sx, -steps + j * dirY + sy, side));
            cut.add(new Placement(use + "_front" + r, face.letter(), steps + c * dirX + sx, -steps + c * dirY + sy, side));
            if (k == 0 || !clamp || fits(sprites, shifted(cut))) {
                out.addAll(cut);
                return c;
            }
        }
        return 0;
    }

    /** The least and greatest {@code x - y} of the layers' opaque texels: how far down and up the haft (which runs along {@code x - y}) they reach. */
    private static int[] reach(Sprites sprites, List<Placement> layers) {
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (Placement p : layers)
            for (int y = 0; y < SIZE; y++)
                for (int x = 0; x < SIZE; x++)
                    if (sprites.lum(p.file(), x, y) >= 0) {
                        int v = x + p.dx() - (y + p.dy());
                        lo = Math.min(lo, v);
                        hi = Math.max(hi, v);
                    }
        return new int[] {lo, hi};
    }

    /** Whether a head rides above the others: one along the haft, or on the top mount. */
    private static boolean isTop(Face face) {
        return ALONG.contains(face.kind()) || face.letter().equals("K");
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

    /** Whether every layer, shifted, keeps all its opaque texels on the canvas (the tip and the edge guard are drawn on the head, so they always do). */
    private static boolean fits(Sprites sprites, List<Placement> layers) {
        for (Placement p : layers) {
            if (p.role().equals("T") || p.role().equals("Z"))
                continue;
            int[] b = sprites.bounds(p.file());
            if (b != null && (b[0] + p.dx() < 0 || b[2] + p.dx() >= SIZE || b[1] + p.dy() < 0 || b[3] + p.dy() >= SIZE))
                return false;
        }
        return true;
    }

    /** Canvas texels the flattened grid reaches past the 16x16 canvas on every side: room for a full-length tool. */
    public static final int PAD = 16, GRID = SIZE + 2 * PAD;

    /** One visible texel of the flattened tool: its canvas position, the role whose material tints it, and the sprite texel it shows. */
    public record Texel(int x, int y, String role, String file, int ux, int uy) {
    }

    /**
     * The tool as the model draws it, one texel per canvas position: each position belongs to the topmost layer that draws it (so no two faces
     * share a plane and two heads merge into one outline), and the tip, if there is one, recolours the front head's texels farthest from the
     * socket. This is the whole of the drawing short of making quads, so the offline sheet and the game cannot differ.
     */
    public static List<Texel> flatten(List<Placement> layers, Sprites sprites) {
        int n = GRID;
        int[] owner = new int[n * n];
        java.util.Arrays.fill(owner, -1);
        Placement tip = null;
        boolean guarded = false;
        for (int i = 0; i < layers.size(); i++) {
            Placement p = layers.get(i);
            if (p.role().equals("T")) {
                tip = p;
                continue;
            }
            if (p.role().equals("Z")) {
                guarded = true;
                continue;
            }
            for (int sy = 0; sy < SIZE; sy++)
                for (int sx = 0; sx < SIZE; sx++) {
                    int x = sx + p.dx() + PAD, y = sy + p.dy() + PAD;
                    if (x >= 0 && x < n && y >= 0 && y < n && p.keeps(x - PAD, y - PAD) && sprites.lum(p.file(), sx, sy) >= 0)
                        owner[x + y * n] = i;
                }
        }
        boolean[] edged = guarded ? edgeMask(layers, owner) : null;
        boolean[] tipped = tip == null ? null : tipMask(layers, owner, tip);
        boolean overlay = tip != null && sprites.has(tip.file());
        List<Texel> out = new ArrayList<>();
        for (int y = 0; y < n; y++)
            for (int x = 0; x < n; x++) {
                int i = owner[x + y * n];
                if (i < 0)
                    continue;
                Placement p = layers.get(i);
                String role = p.role(), file = p.file();
                int cx = x - PAD, cy = y - PAD, ux = cx - p.dx(), uy = cy - p.dy();
                if (tipped != null && tipped[x + y * n]) {
                    // The tip is the head's own texel in the tip's material, or the tip overlay's texel at this canvas spot where it has one.
                    role = "T";
                    if (overlay && cx >= 0 && cx < SIZE && cy >= 0 && cy < SIZE && sprites.lum(tip.file(), cx, cy) >= 0) {
                        file = tip.file();
                        ux = cx;
                        uy = cy;
                    } else if (rim(owner, x + y * n)) {
                        // On the silhouette: the outline shade (the head sprite's darkest texel) in the tip's material, so the tip is outlined.
                        int[] dark = darkest(sprites, file);
                        ux = dark[0];
                        uy = dark[1];
                    }
                }
                if (edged != null && edged[x + y * n] && !role.equals("T")) {
                    // The guard's texel is the head's own at a plate-like light shade, in the guard's material.
                    role = "Z";
                    int[] light = shade(sprites, file, EDGE_SHADE);
                    ux = light[0];
                    uy = light[1];
                }
                out.add(new Texel(cx, cy, role, file, ux, uy));
            }
        return List.copyOf(out);
    }

    /**
     * The canvas texels the tip covers: the front head's own texels farthest from the socket, in a band as deep as the tip's variant
     * asks (a needle is the very point, a cap runs further back). Being picked from the head's texels, it cannot miss the head.
     */
    private static boolean[] tipMask(List<Placement> layers, int[] owner, Placement tip) {
        int n = GRID;
        String frontRole = null;
        int sx = SOCKET_X + PAD, sy = SOCKET_Y + PAD;
        for (Placement p : layers) {
            if (frontRole == null && p.file().startsWith("head_"))
                frontRole = p.role();
            if (p.file().startsWith("handle_")) {
                // The last haft layer drawn is the topmost step: the socket is its head end.
                sx = SOCKET_X + p.dx() + PAD;
                sy = SOCKET_Y + p.dy() + PAD;
            }
        }
        boolean[] out = new boolean[n * n];
        if (frontRole == null)
            return out;
        float[] dist = new float[n * n];
        float far = -1;
        for (int k = 0; k < n * n; k++) {
            if (owner[k] < 0 || !layers.get(owner[k]).role().equals(frontRole))
                continue;
            dist[k] = (float) Math.hypot(k % n - sx, k / n - sy);
            far = Math.max(far, dist[k]);
        }
        float depth = tip.file().endsWith("_needle") ? TIP_NEEDLE : tip.file().endsWith("_capped") ? TIP_CAPPED : TIP_PLAIN;
        for (int k = 0; k < n * n; k++)
            out[k] = owner[k] >= 0 && layers.get(owner[k]).role().equals(frontRole) && dist[k] >= far - depth;
        return out;
    }

    /**
     * The head texels the edge guard covers: those on the head's outline, in the band farthest from the socket, whose open side faces away from it (a texel to the right of
     * or above its own, from a socket below and left), so the guard runs along the leading edge of an axe's bit, a pick's prongs, a blade's
     * upper side and its point, whatever the kind, variant or length. Where the head meets the haft or another head there is no open side.
     */
    private static boolean[] edgeMask(List<Placement> layers, int[] owner) {
        int n = GRID;
        int sx = SOCKET_X + PAD, sy = SOCKET_Y + PAD;
        for (Placement p : layers)
            if (p.file().startsWith("handle_")) {
                sx = SOCKET_X + p.dx() + PAD;
                sy = SOCKET_Y + p.dy() + PAD;
            }
        boolean[] out = new boolean[n * n];
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        float far = 0, near = Float.MAX_VALUE;
        for (int k = 0; k < n * n; k++)
            if (owner[k] >= 0 && layers.get(owner[k]).file().startsWith("head_")) {
                float d = (float) Math.hypot(k % n - sx, k / n - sy);
                far = Math.max(far, d);
                near = Math.min(near, d);
            }
        float band = (far - near) * EDGE_SHARE;
        for (int y = 1; y < n - 1; y++)
            for (int x = 1; x < n - 1; x++) {
                int i = owner[x + y * n];
                if (i < 0 || !layers.get(i).file().startsWith("head_") || Math.hypot(x - sx, y - sy) < far - band)
                    continue;
                for (int[] d : dirs)
                    if (owner[x + d[0] + (y + d[1]) * n] < 0 && d[0] * (x - sx) + d[1] * (y - sy) > 0) {
                        out[x + y * n] = true;
                        break;
                    }
            }
        return out;
    }

    /** The share of the head's reach, from its nearest texel to its farthest from the socket, the guard runs back over. */
    private static final float EDGE_SHARE = 0.4f;

    /** The red-green-blue sum of the guard's shade in the grey ramp: a light plate shade, 178 a channel. */
    private static final int EDGE_SHADE = 3 * 178;

    /** The texel of a sprite whose red-green-blue sum is nearest {@code target}, {@code {x, y}}. */
    private static int[] shade(Sprites sprites, String file, int target) {
        int[] best = {0, 0};
        int gap = Integer.MAX_VALUE;
        for (int y = 0; y < SIZE; y++)
            for (int x = 0; x < SIZE; x++) {
                int lum = sprites.lum(file, x, y);
                if (lum >= 0 && Math.abs(lum - target) < gap) {
                    gap = Math.abs(lum - target);
                    best = new int[] {x, y};
                }
            }
        return best;
    }

    /** Whether texel {@code k} is on the silhouette's outline (a side neighbour empty or off the canvas). */
    private static boolean rim(int[] owner, int k) {
        int n = GRID, x = k % n, y = k / n;
        return x == 0 || y == 0 || x == n - 1 || y == n - 1
                || owner[k - 1] < 0 || owner[k + 1] < 0 || owner[k - n] < 0 || owner[k + n] < 0;
    }

    /** The darkest opaque texel of a sprite, {@code {x, y}}: the outline shade in this grey ramp. */
    private static int[] darkest(Sprites sprites, String file) {
        int[] best = {0, 0};
        int low = Integer.MAX_VALUE;
        for (int y = 0; y < SIZE; y++)
            for (int x = 0; x < SIZE; x++) {
                int lum = sprites.lum(file, x, y);
                if (lum >= 0 && lum < low) {
                    low = lum;
                    best = new int[] {x, y};
                }
            }
        return best;
    }

    /** How far back from the head's farthest texel each tip runs, in texels: tuned by eye. */
    private static final float TIP_NEEDLE = 1.5f, TIP_PLAIN = 2.5f, TIP_CAPPED = 3.5f;
}
