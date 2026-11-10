// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import io.github.soundgoodizerfan.feedback.modular.Edge;
import io.github.soundgoodizerfan.feedback.modular.MaterialColors;
import io.github.soundgoodizerfan.feedback.modular.ModularBake;
import io.github.soundgoodizerfan.feedback.modular.ModularBlueprintItem;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.ModularModifiers;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTraits;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.ToolDesigns;
import io.github.soundgoodizerfan.feedback.modular.ToolTableShaping;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.weapon.GunDesignation;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.world.item.ItemStack;

/**
 * The Blueprint Table: a drafting board. The design is drawn as an exploded assembly of its own parts in a
 * roomy pan-and-zoom panel, and everything it offers comes from {@link ToolDesigns}, the same model the Tool
 * Table makes things from, so a drawing is exactly what the Tool Table would accept and the figures are the
 * ones of the item it would make (spec 6, 4.2: the blueprint predicts, instruments measure). Heat and flaws are
 * not drawn: a flaw is what forging adds. The printed paper, the menu and the payload are unchanged.
 *
 * <h2>The sheet</h2>
 * <ul>
 * <li><b>Drawer</b> (top): the types as icons, scrolled by the wheel; the search button opens the long list.</li>
 * <li><b>Canvas</b>: each part a node placed where the type's {@code patterns} picture puts its letter (the
 * pattern is a small drawing of the item); parts no pattern lays (the optional ones) sit in a tray beside or
 * under it, the ones that can be added as dashed slots. A link joins letters that touch in the pattern, drawn
 * by the design's {@code JOINT} figure (thick and cool with margin, amber near the limit, thin dashed red past
 * it). A drawing that cannot be made shows the failing link in red with the reason beside it. Drag to pan,
 * wheel to zoom about the pointer, click a part to select it, click a dashed slot to add one, the corner mark
 * to take one off. Qualities are chips along the bottom edge.</li>
 * <li><b>Part panel</b> (bottom left): the selected part's material swatches and shape thumbnails. A material
 * item carried on the cursor paints any part it is clicked on; the sample slot sets the selected part once.</li>
 * <li><b>Figures</b> (bottom right): one bar per figure with a tick for the first drawing; the arrow says it
 * has moved. Shift prints the figure. Hovering a bar lights the parts that changed it ({@link #traceFor}).
 * Hovering a swatch, a shape or a part with a material in hand shows the would-be figures as a ghost mark.
 * Pinned drafts (up to three, this session) add their own coloured tick to every bar.</li>
 * </ul>
 * Layout, icons and predictions are worked out when an edit is made ({@link #refresh}), never per frame.
 */
final class BlueprintBoard {

    /** What the board needs from whatever shows it: the stack on the cursor, and word that the selected part changed or that a sample slot is wanted. */
    interface Host {
        ItemStack carried();

        /** The selected part changed, so a sample meant for the old one should go back (the table clears its slot). */
        void selectionChanged();

        /** Whether a sample slot is wanted for the selected part (the table's), which it shows only while one is selected. */
        void sampleWanted(boolean wanted);

        /** Which kind of table this board is on: the types it offers are those that table draws. */
        default io.github.soundgoodizerfan.feedback.modular.Drafting drafting() {
            return io.github.soundgoodizerfan.feedback.modular.Drafting.ANY;
        }
    }

    /** The board's size, which its geometry below is laid out in. */
    static final int W = 360, H = 276;

    private final Host host;
    private final GuiSkin skin;
    private final net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
    /** Where the board's top-left is on the screen; set by the host before it draws or forwards input. */
    private int ox, oy;
    private final List<AbstractWidget> widgets = new ArrayList<>();


    // geometry, relative to the panel
    private static final int CAN_X = 8, CAN_Y = 24, CAN_W = 344, CAN_H = 116;
    private static final int PAN_Y = 144, PAN_H = 128;
    private static final int CALL_X = 8, CALL_W = 204;
    private static final int RES_X = 216, RES_W = 136;
    private static final int DRAWER_X = 8, DRAWER_SLOTS = 12, DRAWER_PITCH = 18;
    /** A node's size and the distance between nodes, in drawing units. */
    private static final float NODE = 26, PITCH = 40;
    private static final int ROW_H = 10, LABEL_W = 50, BAR_W = 62;
    private static final int SWATCH = 12, SWATCH_PITCH = 13, THUMB = 22, THUMB_PITCH = 24;
    private static final float MIN_ZOOM = 0.4f, MAX_ZOOM = 2.2f;
    private static final int MAX_PINS = 3;
    /** A design of more drawn parts than this folds the type's groups into clusters. */
    /** Height of the test rig at the top of the figures panel. */
    private static final int RIG_H = 58;
    /** The series colours of the pinned drafts: data marks on the bars, not text. */
    private static final int[] PIN_COLOURS = {0xFFF2C14E, 0xFF7FE0B0, 0xFFE08FE8};

    /** A part (or a slot for one) on the canvas, at a place in drawing units. */
    private record Node(String key, String role, boolean ghost, float x, float y, ItemStack icon, boolean faint) {
        Node(String key, String role, boolean ghost, float x, float y, ItemStack icon) {
            this(key, role, ghost, x, y, icon, false);
        }
    }

    /** A link between two nodes, and the pairs of parts it stands for (several where a closed group's members join the same node). */
    private record Link(int a, int b, List<String[]> pairs) {
    }

    /** Where a drawing fails: the roles at fault (none for the whole drawing) and why. */
    private record Fault(List<String> roles, Component reason) {
    }

    /** Everything about an edit that can be put back: a pinned draft. */
    private record Draft(int type, int form, Set<String> extras, Map<String, Integer> sizes, Map<String, Integer> shape,
                         Map<String, ResourceLocation> material, Edge edge, String selected, Map<String, Float> dims) {
    }

    private record Pin(Draft draft, ModularFigures figures, ModularDesign design) {
    }

    private final List<ResourceLocation> types;
    private final List<ItemStack> typeIcons = new ArrayList<>();
    private int type;
    private int form;
    private final Set<String> extras = new LinkedHashSet<>();
    /** The size of each ranged part (a charge's filler), beside the pattern: a part with a count is one part. */
    private final Map<String, Integer> sizes = new HashMap<>();
    /** The window that opens over the sheet to pick a type by name. */
    private final SkinPopup<Integer> typePopup;
    /** The part whose swatches and shapes are shown; null picks the first. */
    private String selected;
    private Edge edge = Edge.STANDARD;
    /** Per role letter, which of its shapes is drawn and which material (by id; absent is the type's default). */
    private final Map<String, Integer> shape = new HashMap<>();
    private final Map<String, ResourceLocation> matId = new HashMap<>();
    /** The scalar the drawing sets on a part, keyed {@code role:name} (an engine's bore, a gear's ratio); absent is the role's default. */
    private final Map<String, Float> dimVals = new HashMap<>();
    /** What the sample slot held when last read, so a sample sets its material once. */
    private ItemStack seen = ItemStack.EMPTY;
    /** The paper last seen, so only a blueprint newly put in is opened. */
    private ItemStack lastPaper = ItemStack.EMPTY;

    // --- worked out by refresh() -----------------------------------------------------------------
    private Optional<ModularDesign> baked = Optional.empty();
    private Optional<ModularFigures> figures = Optional.empty();
    private List<Fig> shown = List.of();
    private List<ModularModifiers.Modifier> modifiers = List.of();
    private List<String> chipText = List.of();
    private List<Component> uses = List.of();
    private List<Fault> faults = List.of();
    private final Map<String, ItemStack> icons = new HashMap<>();
    private List<Node> nodes = List.of();
    private final Map<String, Integer> nodeOf = new HashMap<>();
    /** Pairs of node indices that touch in the pattern. */
    private List<Link> links = List.of();
    /** Per link: the joint figure of its worst pair (1 is its limit) and that pair. */
    private float[] linkValue = new float[0];
    private String[][] linkWorst = new String[0][];
    /** The part whose slots are fanned open round it (a gun's attachments live on the part they fit), or null. */
    /** What the drawn gun is called (revolver, assault rifle...), or empty. */
    private Component designation = Component.empty();
    private String fanFor;
    /** An attachment's role to the role it fits, from the type's board joins; empty for a type that lays its parts flat. */
    private Map<String, String> parentOf = Map.of();
    /** Roles that still have an attachment to offer: they wear a plus. */
    private Set<String> fannable = Set.of();
    /** Whether the pointer moved the canvas since the button went down: a click that did not closes the fan. */
    private boolean dragged;
    private boolean rig;
    private float boundsX0, boundsY0, boundsX1, boundsY1;
    private boolean refit = true;
    /** The first drawing of this type or blueprint, which later swaps are compared with. */
    private Optional<ModularFigures> reference = Optional.empty();
    private Optional<ModularDesign> referenceDesign = Optional.empty();
    private boolean fresh = true;
    /** Per figure, what a full bar is: set from the first drawing and grown when a figure outruns it. */
    private final float[] tops = new float[Fig.values().length];
    private final Map<String, List<Material.Located>> materialLists = new HashMap<>();
    private Map<ResourceLocation, Material> listedFor;
    /** Every material for a part, the ones that fit first, narrowed by the search; and which of them fit. */
    private final Map<String, List<Material.Located>> swatchLists = new HashMap<>();
    private final Map<String, Set<ResourceLocation>> fitSets = new HashMap<>();
    private String query = "";
    private net.minecraft.client.gui.components.EditBox searchBox;

    // --- view state ------------------------------------------------------------------------------
    private double camX, camY;
    private float zoom = 1;
    private boolean panning;
    private final io.github.soundgoodizerfan.feedback.client.gun.GunPreview preview = new io.github.soundgoodizerfan.feedback.client.gun.GunPreview();
    private int drawerScroll, swatchScroll, figScroll;
    private final Map<String, Long> flashes = new HashMap<>();
    private long stampAt;
    private final List<Pin> pins = new ArrayList<>();

    // --- hover, set every frame in renderBg ------------------------------------------------------
    private int hoverLink = -1, hoverNode = -1, hoverSwatch = -1, hoverShape = -1, hoverFig = -1, hoverChip = -1, hoverDrawer = -1, hoverFault = -1;
    private boolean hoverCorner;
    /** The would-be drawing under a swatch, a shape or a carried material, as its parts and its figures. */
    private String ghostRole;
    private ResourceLocation ghostMat;
    private int ghostShape = -1;
    private Optional<ModularFigures> ghost = Optional.empty();
    private final Map<String, Optional<ModularFigures>> ghosts = new HashMap<>();
    /** The parts that changed from the first drawing, and what each does to a figure: filled on first hover of a bar. */
    private Map<String, ModularFigures> ablation;
    private Fig traceFor;
    private final Map<String, Float> trace = new HashMap<>();
    private final Map<Fig, String> labels = new java.util.EnumMap<>(Fig.class);

    BlueprintBoard(Host host, GuiSkin skin) {
        this.host = host;
        this.skin = skin;
        types = ToolDesigns.drawable(ModularTypes.known(), host.drafting());
        typePopup = new SkinPopup<>(skin, Component.translatable("gui.feedback.blueprint_table.pick_type"));
        for (ResourceLocation id : types)
            typeIcons.add(new ItemStack(BuiltInRegistries.ITEM.get(id)));
        selectType(0);
        refresh();
    }

    /** Puts the board's top-left at (x, y) on the screen. */
    void origin(int x, int y) {
        ox = x;
        oy = y;
    }

    private ModularType current() {
        return types.isEmpty() ? null : ModularTypes.known().get(types.get(type));
    }

    private boolean edged() {
        ModularType t = current();
        return t != null && t.edged();
    }

    // --- what is on the sheet ---------------------------------------------------------------------

    private Map<String, Integer> counts() {
        return ToolDesigns.counts(current(), form, extras, sizes);
    }

    /** The roles drawn, in the type's order. */
    private List<String> rows() {
        ModularType t = current();
        if (t == null)
            return List.of();
        if (rowsCache == null) {
            Map<String, Integer> counts = counts();
            rowsCache = t.order().stream().filter(counts::containsKey).toList();
        }
        return rowsCache;
    }

    /** {@link #rows} and {@link #choice(String)} worked out once: every node, hover and panel asks each frame. Dropped by {@link #dirty} on any edit. */
    private List<String> rowsCache;
    private final Map<String, Optional<Material.Located>> choiceCache = new HashMap<>();

    private void dirty() {
        rowsCache = null;
        choiceCache.clear();
    }

    /** The selected part: the one picked if it is still drawn, else the first. */
    private String sel() {
        List<String> rows = rows();
        if (selected == null || !rows.contains(selected))
            selected = rows.isEmpty() ? null : rows.get(0);
        return selected;
    }

    private Component roleName(String role) {
        return Component.translatable("modular.feedback.role." + current().roles().get(role).name());
    }

    /** What a part is for: its own line in the lang file, if it has one. */
    private List<Component> roleTip(String role) {
        String name = current().roles().get(role).name();
        List<Component> out = new ArrayList<>();
        out.add(roleName(role));
        if (I18n.exists("modular.feedback.role." + name + ".tip"))
            out.add(Component.translatable("modular.feedback.role." + name + ".tip"));
        // A part that only one kind of launcher takes says so; a part with no line goes on anything.
        if (I18n.exists("modular.feedback.role." + name + ".for"))
            out.add(Component.translatable("modular.feedback.role." + name + ".for").withStyle(net.minecraft.ChatFormatting.GOLD));
        return out;
    }

    // --- options: the form is never chosen, it follows from the parts added -----------------------------
    // An option is a key: an optional part's role letter, or "role:count" for an entry that is in some
    // forms of the type and not others (a gun's slide, bolt or cylinder; a handle of two). The sheet is
    // the options on; adding, removing or swapping one is allowed when some form has exactly that set.

    private static boolean pair(String key) {
        return key.indexOf(':') >= 0;
    }

    private static String roleOf(String key) {
        return pair(key) ? key.substring(0, key.indexOf(':')) : key;
    }

    private ModularType derivedFor;
    private List<Map<String, Integer>> forms = List.of();
    private List<Set<String>> varied = List.of();
    private List<String> keys = List.of();

    /** The type's forms and what varies in each, worked out once per type: the sheet asks every frame, and a charge has seventy-two forms. */
    private void derive() {
        ModularType t = current();
        if (t == derivedFor)
            return;
        derivedFor = t;
        forms = t == null ? List.of() : t.baseForms();
        varied = new ArrayList<>();
        Set<String> all = new LinkedHashSet<>();
        for (Map<String, Integer> f : forms) {
            Set<String> out = new LinkedHashSet<>();
            f.forEach((r, n) -> {
                if (!t.ranged(r) && !forms.stream().allMatch(o -> Objects.equals(o.get(r), n)))
                    out.add(r + ":" + n);
            });
            varied.add(out);
            all.addAll(out);
        }
        if (t != null)
            all.addAll(t.optionals());
        keys = List.copyOf(all);
    }

    private Set<String> varying(int i) {
        derive();
        return varied.get(i);
    }

    /** Every option the sheet has on. */
    private Set<String> now() {
        derive();
        Set<String> out = new LinkedHashSet<>(varying(Math.floorMod(form, forms.size())));
        out.addAll(extras);
        return out;
    }

    private int formFor(Set<String> target) {
        Set<String> want = new LinkedHashSet<>();
        target.stream().filter(BlueprintBoard::pair).forEach(want::add);
        derive();
        for (int i = 0; i < forms.size(); i++)
            if (varying(i).equals(want))
                return i;
        return -1;
    }

    private boolean feasible(Set<String> target) {
        return formFor(target) >= 0;
    }

    /** Turn the sheet to exactly {@code target}, if some form has it. */
    private void set(Set<String> target) {
        int f = formFor(target);
        if (f < 0)
            return;
        form = f;
        extras.clear();
        target.stream().filter(k -> !pair(k)).forEach(extras::add);
        dirty();
    }

    private List<String> allKeys() {
        derive();
        return keys;
    }

    /** The parts that can be added now. */
    private List<String> offered() {
        if (current() == null)
            return List.of();
        Set<String> cur = now();
        return allKeys().stream().filter(k -> !cur.contains(k)).filter(k -> {
            Set<String> s = new LinkedHashSet<>(cur);
            s.add(k);
            return feasible(s);
        }).toList();
    }

    /**
     * {@code key} and what it can be swapped for, keeping the sheet one some form has, in the one fixed order of
     * {@link #allKeys}: the arrows step round this ring, so the same list is walked both ways.
     */
    private List<String> swaps(String key) {
        Set<String> cur = now();
        List<String> out = new ArrayList<>();
        for (String k : allKeys()) {
            if (k.equals(key)) {
                out.add(k);
                continue;
            }
            if (cur.contains(k) || pair(k) != pair(key))
                continue;
            Set<String> s = new LinkedHashSet<>(cur);
            s.remove(key);
            s.add(k);
            if (feasible(s))
                out.add(k);
        }
        return out;
    }

    private boolean removable(String key) {
        Set<String> s = now();
        s.remove(key);
        return feasible(s);
    }

    /** The option a drawn role is, or null for a part every build of the type has. */
    private String keyOf(String role) {
        Set<String> cur = now();
        if (cur.contains(role))
            return role;
        String k = role + ":" + counts().getOrDefault(role, 1);
        return cur.contains(k) ? k : null;
    }

    /** What an option is called: the part, and how many where it is raw stock. */
    private String keyLabel(String key) {
        String role = roleOf(key);
        int n = pair(key) ? Integer.parseInt(key.substring(key.indexOf(':') + 1)) : 1;
        String name = roleName(role).getString();
        return n > 1 && !ToolDesigns.shaped(current().roles().get(role)) ? name + " x" + n : name;
    }

    private void swap(String key, String next) {
        Set<String> s = now();
        s.remove(key);
        s.add(next);
        set(s);
        selected = roleOf(next);
    }

    private boolean drawn(String role) {
        return counts().containsKey(role);
    }


    private void selectType(int index) {
        type = types.isEmpty() ? 0 : Math.floorMod(index, types.size());
        fresh = true;
        refit = true;
        selected = null;
        form = 0;
        swatchScroll = 0;
        figScroll = 0;
        extras.clear();
        fanFor = null;
        sizes.clear();
        shape.clear();
        matId.clear();
        dimVals.clear();
        clearLists();
        flashes.clear();
        drawerScroll = Mth.clamp(type - DRAWER_SLOTS / 2, 0, Math.max(0, types.size() - DRAWER_SLOTS));
    }

    /** Open a blueprint: its type, form and extras, and each part's shape and material where it is a choice. */
    private void open(ModularDesign design) {
        int index = types.indexOf(design.type());
        ModularType t = index < 0 ? null : ModularTypes.known().get(design.type());
        Optional<ToolDesigns.Opened> opened = t == null ? Optional.empty() : ToolDesigns.open(design, t);
        if (opened.isEmpty())
            return;
        selectType(index);
        form = opened.get().form();
        extras.addAll(opened.get().extras());
        sizes.putAll(opened.get().sizes());
        edge = design.edge();
        design.parts().forEach(p -> {
            List<ToolTableShaping.Option> options = shapes(p.role());
            for (int i = 0; i < options.size(); i++)
                if (options.get(i).variant().name().equals(p.variant())
                        && (p.kind().isEmpty() || options.get(i).shaping().item().getPath().equals(p.kind())))
                    shape.put(p.role(), i);
            matId.put(p.role(), p.material());
            p.dims().forEach((k, v) -> dimVals.put(p.role() + ":" + k, v));
        });        dirty();
    }

    /** Per role: its shapes, worked out once (the list walks every type, and every node asks several times a frame). */
    private final Map<String, List<ToolTableShaping.Option>> shapeLists = new HashMap<>();

    private List<ToolTableShaping.Option> shapes(String role) {
        ModularType t = current();
        return t == null ? List.of() : shapeLists.computeIfAbsent(role, r -> ToolDesigns.shapes(t.roles().get(r), ModularTypes.known()));
    }

    private int shapeIndex(String role) {
        int n = shapes(role).size();
        return n == 0 ? 0 : Math.floorMod(shape.getOrDefault(role, 0), n);
    }

    private ToolTableShaping.Option shapeAt(String role, int index) {
        List<ToolTableShaping.Option> s = shapes(role);
        return s.isEmpty() ? null : s.get(Math.floorMod(index, s.size()));
    }

    private ToolTableShaping.Option shapeOf(String role) {
        return shapeAt(role, shape.getOrDefault(role, 0));
    }

    /** The materials a role can be drawn in with shape {@code shapeIdx}, grouped by kind: worked out once per type and shape. */
    private List<Material.Located> materials(String role, int shapeIdx) {
        ModularType t = current();
        if (t == null)
            return List.of();
        Map<ResourceLocation, Material> all = ClientMaterials.get();
        if (all != listedFor) {
            clearLists();
            listedFor = all;
        }
        return materialLists.computeIfAbsent(role + "|" + shapeIdx, k -> {
            List<Material.Located> out = new ArrayList<>(ToolDesigns.materials(t, role, shapeAt(role, shapeIdx), all));
            out.sort(java.util.Comparator.<Material.Located>comparingInt(l -> l.material().kind().map(Enum::ordinal).orElse(99))
                    .thenComparing(l -> l.id().toString()));
            return List.copyOf(out);
        });
    }

    private void clearLists() {
        dirty();
        shapeLists.clear();
        materialLists.clear();
        swatchLists.clear();
        fitSets.clear();
    }

    private boolean matches(Material.Located l) {
        String q = query.trim().toLowerCase(java.util.Locale.ROOT);
        if (q.isEmpty() || materialLabel(l).getString().toLowerCase(java.util.Locale.ROOT).contains(q))
            return true;
        return l.material().kind().map(k -> k.name().toLowerCase(java.util.Locale.ROOT).contains(q)).orElse(false)
                || l.material().traits().stream().anyMatch(tr -> tr.getSerializedName().contains(q));
    }

    /** What the part panel lists: every material, the ones that fit the part first, then the rest, narrowed by the search. */
    private List<Material.Located> swatches(String role) {
        List<Material.Located> fit = materials(role);
        String key = role + "|" + shapeIndex(role);
        return swatchLists.computeIfAbsent(key, k -> {
            Set<ResourceLocation> ids = new java.util.HashSet<>();
            fit.forEach(l -> ids.add(l.id()));
            fitSets.put(k, ids);
            List<Material.Located> out = new ArrayList<>(fit);
            List<Material.Located> rest = new ArrayList<>();
            ClientMaterials.get().forEach((id, m) -> {
                if (!ids.contains(id) && !m.items(id).isEmpty())
                    rest.add(new Material.Located(id, m, m.items(id).keySet().iterator().next()));
            });
            rest.sort(java.util.Comparator.<Material.Located>comparingInt(l -> l.material().kind().map(Enum::ordinal).orElse(99))
                    .thenComparing(l -> l.id().toString()));
            out.addAll(rest);
            return List.copyOf(out.stream().filter(this::matches).toList());
        });
    }

    /** Whether the part takes {@code l}; the rest of the swatches are shown struck out. */
    private boolean usable(String role, Material.Located l) {
        swatches(role);
        Set<ResourceLocation> ids = fitSets.get(role + "|" + shapeIndex(role));
        return ids != null && ids.contains(l.id());
    }

    private List<Material.Located> materials(String role) {
        return materials(role, shapeIndex(role));
    }

    /** The material drawn for {@code role} with shape {@code shapeIdx}: the one chosen if it still fits, else the type's default, else the first. */
    private Optional<Material.Located> choice(String role, int shapeIdx) {
        List<Material.Located> c = materials(role, shapeIdx);
        ResourceLocation wanted = matId.getOrDefault(role, current().defaults().get(role));
        return c.stream().filter(l -> l.id().equals(wanted)).findFirst().or(() -> c.stream().findFirst());
    }

    private Optional<Material.Located> choice(String role) {
        materials(role); // drops the caches if the materials were reloaded
        return choiceCache.computeIfAbsent(role, r -> choice(r, shapeIndex(r)));
    }

    /** The type picker's choices: grouped under a heading each (tools, weapons, wearables, technology, food), a heading's types in name order. */
    private List<SkinPopup.Entry<Integer>> typeEntries() {
        String[] heads = {"tools", "weapons", "wearables", "technology", "food"};
        return IntStream.range(0, types.size())
                .mapToObj(i -> new SkinPopup.Entry<>(i, typeName(i), List.<Component>of(),
                        Component.translatable("gui.feedback.blueprint_table.cat." + heads[categoryOf(types.get(i))]).getString()))
                .sorted(java.util.Comparator.<SkinPopup.Entry<Integer>>comparingInt(e -> java.util.Arrays.asList(heads).indexOf(
                        categoryKeyOf(e.group(), heads))).thenComparing(e -> e.label().getString())).toList();
    }

    private static String categoryKeyOf(String shown, String[] heads) {
        for (String h : heads)
            if (Component.translatable("gui.feedback.blueprint_table.cat." + h).getString().equals(shown))
                return h;
        return heads[0];
    }

    /** Which heading a type is listed under: 0 tools, 1 weapons, 2 wearables, 3 technology, 4 food. */
    private static int categoryOf(ResourceLocation id) {
        ModularType t = ModularTypes.known().get(id);
        if (t == null)
            return 0;
        return switch (t.family()) {
            case BLADE, MACE, SPEAR, BOW, CROSSBOW, SHIELD, GUN, CHARGE -> 1;
            case ARMOUR, GLOVE, BACKPACK, BEDROLL -> 2;
            case CELL, MACHINE, BELT, ENGINE, GEARBOX, PROPELLER, HEAD, VESSEL -> 3;
            case DISH -> 4;
            default -> 0;
        };
    }

    private Component typeName(int i) {
        ResourceLocation id = types.get(Math.floorMod(i, types.size()));
        return Component.translatable("item." + id.getNamespace() + "." + id.getPath());
    }

    private static Component materialLabel(Material.Located l) {
        return Component.translatable("material." + l.id().getNamespace() + "." + l.id().getPath());
    }

    private String materialName(String role) {
        return choice(role).map(l -> materialLabel(l)).orElse(Component.translatable("gui.feedback.blueprint_table.nothing_fits")).getString();
    }

    /** What is drawn, as a design, with {@code ovRole} taken in the given material and shape instead; empty while a role has no material that fits it. */
    private Optional<ModularDesign> design(String ovRole, ResourceLocation ovMat, int ovShape) {
        ModularType t = current();
        if (t == null)
            return Optional.empty();
        Map<String, Integer> counts = counts();
        List<ModularDesign.Part> parts = new ArrayList<>();
        for (String role : t.order()) {
            if (!counts.containsKey(role))
                continue;
            boolean ov = role.equals(ovRole);
            int si = ov ? ovShape : shapeIndex(role);
            Optional<Material.Located> c;
            if (ov && ovMat != null)
                c = materials(role, si).stream().filter(l -> l.id().equals(ovMat)).findFirst();
            else
                c = choice(role, si);
            if (c.isEmpty())
                return Optional.empty();
            ModularDesign.Part made = ToolDesigns.part(role, c.get(), shapeAt(role, si), counts.get(role));
            Map<String, Float> dims = new HashMap<>();
            for (String name : t.roles().get(role).dims().keySet())
                dims.put(name, dimValue(role, name, made));
            parts.add(dims.isEmpty() ? made : new ModularDesign.Part(made.role(), made.material(), made.form(), made.count(), made.kind(), made.variant(), made.flaw(), dims));
        }
        return Optional.of(new ModularDesign(types.get(type), parts, edged() ? edge : Edge.STANDARD));
    }

    /** The scalars of {@code role} the sheet shows now: those whose {@code when} the shape in use meets, in name order. */
    private List<Map.Entry<String, ModularType.Dim>> dimsOf(String role) {
        ModularType t = current();
        if (t == null || role == null || t.roles().get(role) == null)
            return List.of();
        ModularType.Role r = t.roles().get(role);
        ToolTableShaping.Option o = shapeOf(role);
        return r.dims().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .filter(e -> e.getValue().when().isEmpty()
                        || (o != null && o.variant().params().getOrDefault(e.getValue().when(), 0f) >= e.getValue().atLeast()))
                .toList();
    }

    /** What the drawing has {@code name} of {@code role} at: the sheet's own setting, else the shape's nominal, else the role's default. */
    private float dimValue(String role, String name, ModularDesign.Part made) {
        ModularType.Role r = current().roles().get(role);
        Float set = dimVals.get(role + ":" + name);
        return r.dim(set == null ? made : new ModularDesign.Part(made.role(), made.material(), made.form(), made.count(), made.kind(),
                made.variant(), made.flaw(), Map.of(name, set)), name);
    }

    // --- worked out on an edit, never per frame -----------------------------------------------------

    /** Lay the drawing out, bake it, and work out everything the frame draws from it. */
    private void refresh() {
        ModularType t = current();
        ghosts.clear();
        ghostRole = null;
        ghost = Optional.empty();
        ablation = null;
        traceFor = null;
        trace.clear();
        icons.clear();
        dirty();
        shapeLists.clear(); // roles differ per type
        layout();
        Optional<ModularDesign> design = design(null, null, 0);
        baked = design;
        designation = design.flatMap(d -> t != null && t.family() == ModularType.Family.GUN
                        ? GunSpec.of(d, t, ClientMaterials.get()).flatMap(spec -> GunDesignation.of(t, spec,
                                ModularTraits.allOf(d, t).stream().map(ModularTraits.Active::id).collect(java.util.stream.Collectors.toSet())))
                        : Optional.empty())
                .<Component>map(n -> Component.translatable("gun.feedback.designation." + n)).orElse(Component.empty());
        measureLinks();
        figures = design.flatMap(d -> ToolDesigns.predict(d, ModularTypes.known(), ClientMaterials.get()));
        uses = design.map(d -> usesOf(d)).orElse(List.of());
        modifiers = design.isPresent() && figures.isPresent() ? ModularModifiers.of(design.get(), t, figures.get()) : List.of();
        chipText = modifiers.stream().map(m -> m.name().getString()).toList();
        shown = figures.map(ModularBlueprintItem::present).orElse(List.of());
        if (design.isPresent())
            for (ModularDesign.Part p : design.get().parts()) {
                List<ItemStack> made = ToolDesigns.items(p, t.roles().get(p.role()), ClientMaterials.get(), p.count());
                if (!made.isEmpty())
                    icons.put(p.role(), made.get(0));
            }
        if (fresh && figures.isPresent()) {
            reference = figures;
            referenceDesign = design;
            fresh = false;
            for (Fig f : Fig.values())
                tops[f.ordinal()] = Math.max(figures.get().get(f), 0f) * 2.5f;
        }
        if (figures.isPresent())
            for (Fig f : Fig.values())
                tops[f.ordinal()] = Math.max(tops[f.ordinal()], figures.get().get(f) * 1.25f);
        faults = faultsOf(design);
        if (refit) {
            fitCamera();
            refit = false;
        }
    }

    private List<Fault> faultsOf(Optional<ModularDesign> design) {
        ModularType t = current();
        List<Fault> out = new ArrayList<>();
        if (t == null)
            return out;
        if (design.isEmpty()) {
            for (String role : rows())
                if (choice(role).isEmpty())
                    out.add(new Fault(List.of(role), Component.translatable("gui.feedback.blueprint_table.nothing_fits")));
            return out;
        }
        if (figures.isPresent())
            return out;
        if (t.family() == ModularType.Family.GUN) {
            List<GunSpec.Fault> found = GunSpec.faults(design.get(), t, ClientMaterials.get());
            for (GunSpec.Fault f : found)
                out.add(new Fault(f.roles(), Component.translatable("message.feedback.blueprint_table.fault." + f.reason())));
            if (!found.isEmpty())
                return out;
            // The physics accepts the parts but the print still fails: say whether the table would not read them back or the bake has no figures.
            if (GunSpec.of(design.get(), t, ClientMaterials.get()).isPresent()) {
                boolean read = ToolDesigns.make(design.get(), ModularTypes.known(), ClientMaterials.get()).isPresent();
                out.add(new Fault(List.of(), Component.translatable(read ? "message.feedback.blueprint_table.cannot_print_gun_bake"
                        : "message.feedback.blueprint_table.cannot_print_gun_read")));
                return out;
            }
        }
        out.add(new Fault(List.of(), Component.translatable(t.family() == ModularType.Family.GUN
                ? "message.feedback.blueprint_table.cannot_print_gun" : "message.feedback.blueprint_table.cannot_print")));
        return out;
    }

    /**
     * Place the parts. The type's pattern is a small picture of the item, so a letter sits at the centre of its cells; the
     * type's {@code board} layout places the parts no pattern lays, and what neither places (a drawn part, the slots for
     * parts that could be added) goes to a tray: under a wide picture, beside a tall one. A big design folds each of the
     * type's groups into one cluster node that a click opens.
     */
    private void layout() {
        ModularType t = current();
        nodeOf.clear();
        if (t == null) {
            nodes = List.of();
            links = List.of();
            return;
        }
        List<List<String>> patterns = t.patterns();
        List<String> pat = patterns.get(Math.floorMod(form, patterns.size()));
        int cols = 1;
        for (String row : pat)
            cols = Math.max(cols, row.length());
        int prow = pat.size();
        Map<String, float[]> at = new LinkedHashMap<>();
        for (int r = 0; r < pat.size(); r++)
            for (int c = 0; c < pat.get(r).length(); c++) {
                char ch = pat.get(r).charAt(c);
                if (ch == ' ')
                    continue;
                float[] a = at.computeIfAbsent(String.valueOf(ch), k -> new float[3]);
                a[0] += c;
                a[1] += r;
                a[2]++;
            }
        ModularType.Board board = t.board();
        Set<String> inPattern = new java.util.HashSet<>(at.keySet());
        // A type whose board says what each part fits on hangs those parts off their parent instead of laying them flat.
        Map<String, List<String>> kidsOf = new LinkedHashMap<>();
        Map<String, String> parents = new HashMap<>();
        for (List<String> j : board.joins())
            if (j.size() == 2 && !inPattern.contains(j.get(0)) && !parents.containsKey(j.get(0))) {
                parents.put(j.get(0), j.get(1));
                kidsOf.computeIfAbsent(j.get(1), k -> new ArrayList<>()).add(j.get(0));
            }
        parentOf = parents;
        boolean attach = !parents.isEmpty();
        board.layout().forEach((letter, xy) -> {
            if (xy.size() == 2 && !parents.containsKey(letter))
                at.putIfAbsent(letter, new float[]{xy.get(0), xy.get(1), 1});
        });
        List<String> rows = rows();
        Set<String> cur = now();
        fannable = attach ? allKeys().stream().filter(k -> !cur.contains(k)).map(k -> parents.get(roleOf(k))).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet()) : Set.of();
        if (fanFor != null && !rows.contains(fanFor))
            fanFor = null;
        List<String> fanKeys = attach ? childKeys(fanFor) : List.of();
        if (attach) {
            Map<String, float[]> memo = new HashMap<>();
            Map<String, Integer> sides = new HashMap<>();
            List<String> want = new ArrayList<>(rows);
            fanKeys.forEach(k -> want.add(roleOf(k)));
            for (String role : want)
                if (parents.containsKey(role)) {
                    float[] px = attachedPos(role, at, kidsOf, memo, sides);
                    if (px != null)
                        at.put(role, new float[]{px[0] / PITCH, px[1] / PITCH, 1});
                }
        }
        List<Node> out = new ArrayList<>();
        List<String> tray = new ArrayList<>();
        for (String role : rows) {
            float[] a = at.get(role);
            if (a == null) {
                tray.add(role);
                continue;
            }
            nodeOf.put(role, out.size());
            out.add(new Node(role, role, false, a[0] / a[2] * PITCH, a[1] / a[2] * PITCH, ItemStack.EMPTY));
        }
        boolean wide = cols >= prow;
        int trayCols = wide ? Math.max(4, cols) : Math.max(3, prow);
        int placed = 0;
        for (int pass = 0; pass < 2; pass++) {
            List<String> keys = pass == 0 ? tray : attach
                    ? java.util.stream.Stream.concat(offered().stream().filter(k -> !parents.containsKey(roleOf(k))), fanKeys.stream()).toList()
                    : offered();
            for (String k : keys) {
                String role = roleOf(k);
                float[] a = pass == 1 ? at.get(role) : null;
                float x, y;
                if (a != null) { // a slot sits where the part would
                    x = a[0] / a[2] * PITCH;
                    y = a[1] / a[2] * PITCH;
                } else {
                    if (wide) {
                        x = placed % trayCols * PITCH;
                        y = (prow - 1) * PITCH + 30 + placed / trayCols * PITCH;
                    } else {
                        x = (cols - 1) * PITCH + 30 + placed / trayCols * PITCH;
                        y = placed % trayCols * PITCH;
                    }
                    placed++;
                }
                ModularType.Role r = t.roles().get(role);
                ItemStack icon = pass == 1 && r.part().isPresent() ? new ItemStack(BuiltInRegistries.ITEM.get(r.part().get().item())) : ItemStack.EMPTY;
                if (pass == 0)
                    nodeOf.put(role, out.size());
                boolean faint = pass == 1 && attach && parents.containsKey(role) && (!feasibleWith(k) || launcherMismatch(role));
                out.add(new Node(k, role, pass == 1, x, y, icon, faint));
            }
        }
        nodes = List.copyOf(out);
        List<Link> ls = new ArrayList<>();
        for (int r = 0; r < pat.size(); r++)
            for (int c = 0; c < pat.get(r).length(); c++) {
                char ch = pat.get(r).charAt(c);
                if (ch == ' ')
                    continue;
                if (c + 1 < pat.get(r).length())
                    link(ls, String.valueOf(ch), String.valueOf(pat.get(r).charAt(c + 1)));
                if (r + 1 < pat.size() && c < pat.get(r + 1).length())
                    link(ls, String.valueOf(ch), String.valueOf(pat.get(r + 1).charAt(c)));
            }
        for (List<String> j : board.joins())
            if (j.size() == 2 && (!attach || parents.containsKey(j.get(0)))) // a part the pattern lays is joined by the pattern, not by a line across it
                link(ls, j.get(0), j.get(1));
        links = List.copyOf(ls);
        boundsX0 = boundsY0 = Float.MAX_VALUE;
        boundsX1 = boundsY1 = -Float.MAX_VALUE;
        for (Node n : nodes) {
            boundsX0 = Math.min(boundsX0, n.x - NODE / 2);
            boundsY0 = Math.min(boundsY0, n.y - NODE / 2);
            boundsX1 = Math.max(boundsX1, n.x + NODE / 2);
            boundsY1 = Math.max(boundsY1, n.y + NODE / 2 + 10);
        }
        if (nodes.isEmpty())
            boundsX0 = boundsY0 = boundsX1 = boundsY1 = 0;
    }

    /** Where an attachment sits, in canvas units: on a ring round its parent, on one side of the row, its slot fixed by its place among its siblings. */
    private float[] attachedPos(String role, Map<String, float[]> at, Map<String, List<String>> kidsOf, Map<String, float[]> memo, Map<String, Integer> sides) {
        float[] done = memo.get(role);
        if (done != null)
            return done;
        String parent = parentOf.get(role);
        float[] out;
        if (parent == null) {
            float[] a = at.get(role);
            out = a == null ? null : new float[]{a[0] / a[2] * PITCH, a[1] / a[2] * PITCH};
        } else {
            float[] pp = attachedPos(parent, at, kidsOf, memo, sides);
            if (pp == null)
                return null;
            int k = kidsOf.get(parent).indexOf(role);
            boolean top = !parentOf.containsKey(parent);
            int side = top ? (k % 2 == 0 ? -1 : 1) : sides.getOrDefault(parent, -1);
            sides.put(role, side);
            float[] off = slot(top ? k / 2 : k, side);
            out = new float[]{pp[0] + off[0], pp[1] + off[1]};
        }
        if (out != null)
            memo.put(role, out);
        return out;
    }

    /** Slot {@code j} on one side ({@code -1} up, {@code 1} down) of a part: straight out first, then wider rings of 3, 5, 7 round it. */
    private static float[] slot(int j, int side) {
        int[] caps = {1, 3, 5, 7, 9};
        int left = j;
        for (int ring = 0; ring < caps.length; ring++) {
            if (left < caps[ring]) {
                float radius = 44 + 32 * ring, a = (left - (caps[ring] - 1) / 2f) * (30f / radius);
                return new float[]{Mth.sin(a) * radius, side * Mth.cos(a) * radius};
            }
            left -= caps[ring];
        }
        return new float[]{0, side * 220f};
    }

    /** The keys of the attachments that fit {@code parentRole} and are not fitted yet, the ones that cannot go on now included. */
    private List<String> childKeys(String parentRole) {
        if (parentRole == null)
            return List.of();
        Set<String> cur = now();
        return allKeys().stream().filter(k -> !cur.contains(k)).filter(k -> parentRole.equals(parentOf.get(roleOf(k)))).toList();
    }

    /** Whether {@code role} is a launcher's part (winding, main spring, steam chest) for a kind of launcher the drawn barrel is not. */
    private boolean launcherMismatch(String role) {
        ModularType t = current();
        GunSpec.Launcher mine = GunSpec.launcherRole(role);
        if (t == null || t.family() != ModularType.Family.GUN || mine == GunSpec.Launcher.CHAMBER)
            return false;
        ToolTableShaping.Option o = shapeOf("B");
        float code = o == null ? 0f : o.variant().params().getOrDefault("launcher", 0f);
        return Math.round(code) != mine.ordinal();
    }

    /** Whether the sheet would still be one some form has with {@code key} added. */
    private boolean feasibleWith(String key) {
        Set<String> s = new LinkedHashSet<>(now());
        s.add(key);
        return feasible(s);
    }

    /** Whether a part belongs to the open fan: the part itself, what is fitted to it, and the slots round it. */
    private boolean inFan(Node n) {
        return fanFor == null || n.role.equals(fanFor) || fanFor.equals(parentOf.get(n.role));
    }

    /** Joins two drawn parts' nodes (nothing for a part not drawn, or two parts folded into one node); a pair already joined adds its pair of parts. */
    private void link(List<Link> out, String a, String b) {
        Integer ia = nodeOf.get(a), ib = nodeOf.get(b);
        if (a.equals(b) || ia == null || ib == null || ia.equals(ib))
            return;
        for (Link l : out)
            if (l.a == ia && l.b == ib || l.a == ib && l.b == ia) {
                if (l.pairs.stream().noneMatch(p -> p[0].equals(a) && p[1].equals(b) || p[0].equals(b) && p[1].equals(a)))
                    l.pairs.add(new String[]{a, b});
                return;
            }
        List<String[]> pairs = new ArrayList<>();
        pairs.add(new String[]{a, b});
        out.add(new Link(ia, ib, pairs));
    }

    /** The joint figure of every link, each from the two parts it joins ({@link ModularBake#joints}); a link of several pairs reads its worst. */
    private void measureLinks() {
        linkValue = new float[links.size()];
        linkWorst = new String[links.size()][];
        ModularType t = current();
        if (t == null || baked.isEmpty())
            return;
        List<String[]> flat = new ArrayList<>();
        for (Link l : links)
            flat.addAll(l.pairs);
        float[] v = ModularBake.joints(baked.get(), t, ClientMaterials.get(), flat);
        int k = 0;
        for (int i = 0; i < links.size(); i++) {
            linkValue[i] = -1;
            for (String[] pair : links.get(i).pairs) {
                if (v[k] > linkValue[i]) {
                    linkValue[i] = v[k];
                    linkWorst[i] = pair;
                }
                k++;
            }
        }
    }

    /** Frame the drawn parts (not the empty slots) in the canvas. */
    private void fitCamera() {
        float x0 = Float.MAX_VALUE, y0 = Float.MAX_VALUE, x1 = -Float.MAX_VALUE, y1 = -Float.MAX_VALUE;
        for (Node n : nodes) {
            if (n.ghost)
                continue;
            x0 = Math.min(x0, n.x - NODE / 2);
            y0 = Math.min(y0, n.y - NODE / 2);
            x1 = Math.max(x1, n.x + NODE / 2);
            y1 = Math.max(y1, n.y + NODE / 2 + 10);
        }
        if (x0 > x1) {
            camX = camY = 0;
            zoom = 1;
            return;
        }
        zoom = Mth.clamp(Math.min((CAN_W - 24) / (x1 - x0), (CAN_H - 34) / (y1 - y0)), MIN_ZOOM, 1.6f);
        camX = (x0 + x1) / 2;
        camY = (y0 + y1) / 2 + 6 / zoom;
    }

    private void clampCamera() {
        camX = Mth.clamp(camX, boundsX0 - 40, boundsX1 + 40);
        camY = Mth.clamp(camY, boundsY0 - 40, boundsY1 + 40);
    }

    // --- view maths -------------------------------------------------------------------------------

    private float centreX() {
        return ox + CAN_X + CAN_W / 2f;
    }

    private float centreY() {
        return oy + CAN_Y + CAN_H / 2f;
    }

    private int screenX(float wx) {
        return Math.round(centreX() + (float) (wx - camX) * zoom);
    }

    private int screenY(float wy) {
        return Math.round(centreY() + (float) (wy - camY) * zoom);
    }

    private boolean overCanvas(double mx, double my) {
        return mx >= ox + CAN_X && mx < ox + CAN_X + CAN_W && my >= oy + CAN_Y && my < oy + CAN_Y + CAN_H;
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int nodeAt(double mx, double my) {
        if (!overCanvas(mx, my))
            return -1;
        float half = NODE * zoom / 2 + 1;
        for (int i = nodes.size() - 1; i >= 0; i--) {
            Node n = nodes.get(i);
            if (Math.abs(mx - screenX(n.x)) <= half && Math.abs(my - screenY(n.y)) <= half)
                return i;
        }
        return -1;
    }

    // --- links ---------------------------------------------------------------------------------------

    /** The link the pointer is on, or -1. */
    private int linkAt(double mx, double my) {
        int best = -1;
        double bestD = 4;
        for (int i = 0; i < links.size(); i++) {
            Node a = nodes.get(links.get(i).a), b = nodes.get(links.get(i).b);
            double ax = screenX(a.x), ay = screenY(a.y), dx = screenX(b.x) - ax, dy = screenY(b.y) - ay, len2 = dx * dx + dy * dy;
            if (len2 == 0)
                continue;
            double u = Mth.clamp(((mx - ax) * dx + (my - ay) * dy) / len2, 0, 1);
            double d = Math.hypot(mx - (ax + u * dx), my - (ay + u * dy));
            if (d < bestD && u > 0.15 && u < 0.85) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    // --- widgets ----------------------------------------------------------------------------------

    /** How many control rows the part panel has for the selected part: a stepper or the remove button, then the edge. */
    private int controlRows(String role) {
        if (role == null)
            return 0;
        ModularType t = current();
        String key = keyOf(role);
        int rows = t.ranged(role) || (key != null && (swaps(key).size() > 1 || removable(key))) ? 1 : 0;
        return rows + (edged() && role.equals(t.wearRole()) ? 1 : 0) + Math.min(MAX_DIM_ROWS, dimsOf(role).size());
    }

    /** A scalar's row: its name and figure, in its unit. */
    private String dimLabel(String role, String name, ModularType.Dim dim) {
        Optional<Material.Located> c = choice(role);
        float v = c.isEmpty() ? dim.def() : dimValue(role, name, ToolDesigns.part(role, c.get(), shapeOf(role), 1));
        String unit = dim.unit().isEmpty() ? "" : " " + (I18n.exists("modular.feedback.unit." + dim.unit())
                ? I18n.get("modular.feedback.unit." + dim.unit()) : dim.unit());
        return I18n.get("modular.feedback.dim." + name) + ": " + (dim.grain() < 1f ? String.format(java.util.Locale.ROOT, "%.2f", v) : String.valueOf(Math.round(v))) + unit;
    }

    private static final int SEARCH_H = 14;
    /** Scalar rows shown at once; a part with more scrolls them, so the panel never outgrows its box. */
    private static final int MAX_DIM_ROWS = 3;
    private int dimScroll;

    /** The part's stepper rows above its scalars: a count or swap or remove row, then the edge. */
    private int rowsAboveDims(String role) {
        ModularType t = current();
        String key = keyOf(role);
        return (t.ranged(role) || (key != null && (swaps(key).size() > 1 || removable(key))) ? 1 : 0)
                + (edged() && role.equals(t.wearRole()) ? 1 : 0);
    }

    private int swatchTop(String role) {
        return oy + PAN_Y + 15 + 15 * controlRows(role) + SEARCH_H;
    }

    private int shapesHeight(String role) {
        int n = role == null ? 0 : shapes(role).size();
        return n < 2 ? 0 : THUMB + 4 + (n > (CALL_W - 8) / THUMB_PITCH ? THUMB_PITCH : 0);
    }

    private int shapesTop(String role) {
        return oy + PAN_Y + PAN_H - 3 - shapesHeight(role);
    }

    private int swatchCols() {
        return (CALL_W - 8) / SWATCH_PITCH;
    }

    private int swatchRows(String role) {
        return Math.max(1, (shapesTop(role) - swatchTop(role) - 2) / SWATCH_PITCH);
    }

    /** Makes the board's controls (again after any edit that changes which are needed). */
    void build() {
        widgets.clear();
        searchBox = null;
        ModularType t = current();
        int top = oy + 4;
        widgetAdd(GuiStyle.button(skin, Component.literal("..."), b -> typePopup.open(
                typeEntries(),
                i -> change(() -> selectType(i))), ox + DRAWER_X + DRAWER_SLOTS * DRAWER_PITCH + 4, top, 16, 16));
        if (rigged())
            widgetAdd(GuiStyle.toggle(skin, Component.translatable("gui.feedback.blueprint_table.rig"), () -> rig, b -> rig = !rig,
                    ox + 244, top, 32));
        for (int i = 0; i < pins.size(); i++) {
            int pin = i;
            widgetAdd(GuiStyle.button(skin, Component.literal(String.valueOf(i + 1)), b -> {
                if (shiftDown())
                    pins.remove(pin);
                else
                    restore(pins.get(pin).draft());
                build();
            }, ox + 278 + i * 16, top, 14, 16));
        }
        widgetAdd(GuiStyle.button(skin, Component.translatable("gui.feedback.blueprint_table.pin"), b -> {
            if (figures.isEmpty()) {
                skin.cue(GuiSkin.Cue.REJECT);
                return;
            }
            if (pins.size() >= MAX_PINS)
                pins.remove(0);
            pins.add(new Pin(capture(), figures.get(), baked.get()));
            skin.cue(GuiSkin.Cue.SEAT, null, 1.1f);
            build();
        }, ox + 326, top, 26, 16));
        String role = t == null ? null : sel();
        host.sampleWanted(role != null);
        if (role == null)
            return;
        int y = oy + PAN_Y + 15;
        String key = keyOf(role);
        int x = ox + CALL_X + 4;
        boolean rem = key != null && removable(key);
        int sw = CALL_W - 8 - (rem ? 18 : 0);
        if (t.ranged(role)) {
            List<Integer> range = t.ranges().get(role);
            int lo = Math.max(1, range.get(0)), hi = range.get(1);
            widgetAdd(GuiStyle.stepper(skin, x, y, sw, 14, "<", ">",
                    () -> roleName(role).getString() + " x" + counts().getOrDefault(role, lo),
                    dir -> change(() -> sizes.put(role, lo + Math.floorMod(counts().get(role) - lo + dir, hi - lo + 1))), false, null));
        } else if (key != null && swaps(key).size() > 1) {
            List<String> alt = swaps(key);
            widgetAdd(GuiStyle.stepper(skin, x, y, sw, 14, "<", ">", () -> keyLabel(key),
                    dir -> change(() -> swap(key, alt.get(Math.floorMod(alt.indexOf(key) + dir, alt.size())))), false, null));
        }
        if (rem)
            widgetAdd(GuiStyle.button(skin, Component.literal("x"), b -> change(() -> {
                Set<String> rest = now();
                rest.remove(key);
                set(rest);
            }), x + CALL_W - 8 - 14, y, 14, 14));
        if (rem || t.ranged(role) || (key != null && swaps(key).size() > 1))
            y += 15;
        if (edged() && role.equals(t.wearRole())) {
            widgetAdd(GuiStyle.stepper(skin, x, y, CALL_W - 8, 14, "<", ">",
                    () -> Component.translatable("tooltip.feedback.modular.edge." + edge.getSerializedName()).getString(),
                    dir -> change(() -> edge = edge.step(dir)), false, null));
            y += 15;
        }
        List<Map.Entry<String, ModularType.Dim>> allDims = dimsOf(role);
        dimScroll = Mth.clamp(dimScroll, 0, Math.max(0, allDims.size() - MAX_DIM_ROWS));
        for (Map.Entry<String, ModularType.Dim> d : allDims.subList(dimScroll, Math.min(allDims.size(), dimScroll + MAX_DIM_ROWS))) {
            String name = d.getKey();
            ModularType.Dim dim = d.getValue();
            widgetAdd(GuiStyle.stepper(skin, x, y, CALL_W - 8, 14, "<", ">", () -> dimLabel(role, name, dim),
                    dir -> change(() -> {
                        float now = dimValue(role, name, ToolDesigns.part(role, choice(role).orElseThrow(), shapeOf(role), 1));
                        float next = Math.round((now + dir * dim.grain()) / dim.grain()) * dim.grain();
                        dimVals.put(role + ":" + name, dim.clamp(next));
                    }), false, null));
            y += 15;
        }
        searchBox = new net.minecraft.client.gui.components.EditBox(font, x + 2, swatchTop(role) - SEARCH_H + 3, CALL_W - 8 - 4, 9,
                Component.translatable("gui.feedback.popup.search"));
        searchBox.setBordered(false);
        searchBox.setTextColor(skin.ink());
        searchBox.setHint(Component.translatable("gui.feedback.popup.search").withColor(skin.dim()));
        searchBox.setValue(query);
        searchBox.setResponder(v -> {
            query = v;
            swatchLists.clear();
            swatchScroll = 0;
        });
        widgetAdd(searchBox);
    }

    private <T extends AbstractWidget> T widgetAdd(T widget) {
        widgets.add(widget);
        return widget;
    }

    private static boolean shiftDown() {
        return net.minecraft.client.gui.screens.Screen.hasShiftDown();
    }

    // --- drafts -----------------------------------------------------------------------------------

    private Draft capture() {
        return new Draft(type, form, Set.copyOf(extras), Map.copyOf(sizes), Map.copyOf(shape), Map.copyOf(matId), edge, selected, Map.copyOf(dimVals));
    }

    private void restore(Draft d) {
        change(() -> {
            selectType(d.type());
            form = d.form();
            extras.addAll(d.extras());
            sizes.putAll(d.sizes());
            shape.putAll(d.shape());
            matId.putAll(d.material());
            dimVals.putAll(d.dims());
            edge = d.edge();
            selected = d.selected();
            refit = true;
        });
    }

    /** Make an edit and rebuild the sheet; if it changed which part is selected, the sample meant another part, so it goes back to the player. */
    private void change(Runnable edit) {
        String before = String.valueOf(sel());
        int type0 = type;
        edit.run();
        dirty(); // the edit may have changed what sel() reads just below
        if (type != type0 || !before.equals(String.valueOf(sel()))) {
            swatchScroll = 0;
            dimScroll = 0;
            host.selectionChanged();
        }
        refresh();
        build();
    }

    /** Put {@code located} into {@code role} with the sound and flash of material meeting part. */
    private void paint(String role, Material.Located located) {
        if (located.id().equals(choice(role).map(Material.Located::id).orElse(null)))
            return;
        change(() -> {
            matId.put(role, located.id());
            selected = role;
        });
        skin.cue(GuiSkin.Cue.SEAT, located.material().kind().orElse(null), 1f);
        flashes.put(role, System.currentTimeMillis());
    }

    // --- input ------------------------------------------------------------------------------------

    /** What goes over everything the table draws: the type picker, and the gun's turning drawer. */
    void drawOverlay(GuiGraphics g, int mouseX, int mouseY) {
        typePopup.render(g, ox, oy, W, H, mouseX, mouseY);
        // The design turns in a drawer that pops out of the panel's left edge, when there is room.
        preview.draw(g, skin, baked.orElse(null), ox, oy);
        preview.tooltip(g, mouseX, mouseY);
    }

    boolean popupActive() {
        return typePopup.active();
    }

    boolean keyPressed(int key, int scan, int modifiers) {
        if (!typePopup.active() && searchBox != null && searchBox.isFocused() && key != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            searchBox.keyPressed(key, scan, modifiers);
            return true;
        }
        return typePopup.keyPressed(key, scan, modifiers);
    }

    boolean charTyped(char c, int modifiers) {
        if (!typePopup.active() && searchBox != null && searchBox.isFocused())
            return searchBox.charTyped(c, modifiers);
        return typePopup.charTyped(c, modifiers);
    }

    boolean mouseClicked(double mx, double my, int button) {
        if (typePopup.mouseClicked(mx, my, button))
            return true;
        if (preview.click(mx, my, button)) // also keeps the click from dropping what the cursor carries
            return true;
        boolean onWidget = widgets.stream().anyMatch(c -> c.visible && c.isMouseOver(mx, my));
        if (!onWidget && current() != null) {
            if (button == 0 && hoverDrawer >= 0 && hoverDrawer < types.size()) {
                GuiSkin.play(skin.click(), 1f);
                int pick = hoverDrawer;
                if (pick != type)
                    change(() -> selectType(pick));
                return true;
            }
            String role = sel();
            if (button == 0 && hoverSwatch >= 0 && role != null) {
                List<Material.Located> mats = swatches(role);
                if (hoverSwatch < mats.size()) {
                    if (usable(role, mats.get(hoverSwatch)))
                        paint(role, mats.get(hoverSwatch));
                    else
                        skin.cue(GuiSkin.Cue.REJECT);
                    return true;
                }
            }
            if (button == 0 && hoverShape >= 0 && role != null) {
                int pick = hoverShape;
                GuiSkin.play(skin.click(), 1f);
                change(() -> {
                    shape.put(role, pick);
                    // A long or machine gun frame takes a stock, so fit one as the frame is picked.
                    ModularType gt = current();
                    ToolTableShaping.Option o = shapeAt(role, pick);
                    if (gt != null && gt.family() == ModularType.Family.GUN && role.equals("R") && o != null
                            && !o.variant().name().equals("handgun") && gt.optionals().contains("i"))
                        extras.add("i");
                });
                return true;
            }
            if (overCanvas(mx, my)) {
                int n = nodeAt(mx, my);
                if (n >= 0 && button == 0) {
                    Node node = nodes.get(n);
                    ItemStack carried = host.carried();
                    if (node.ghost && node.faint) {
                        skin.cue(GuiSkin.Cue.REJECT);
                    } else if (node.ghost) {
                        GuiSkin.play(skin.click(), 1f);
                        change(() -> {
                            Set<String> on = now();
                            on.add(node.key);
                            set(on);
                            selected = node.role;
                        });
                    } else if (!carried.isEmpty()) {
                        Optional<Material.Located> named = Material.find(ClientMaterials.get(), carried);
                        Optional<Material.Located> fit = named.flatMap(m -> materials(node.role).stream().filter(l -> l.id().equals(m.id())).findFirst());
                        if (fit.isPresent())
                            paint(node.role, fit.get());
                        else
                            skin.cue(GuiSkin.Cue.REJECT);
                    } else if (hoverCorner && keyOf(node.role) != null && removable(keyOf(node.role))) {
                        String key = keyOf(node.role);
                        GuiSkin.play(skin.click(), 1f);
                        change(() -> {
                            Set<String> rest = now();
                            rest.remove(key);
                            set(rest);
                        });
                    } else {
                        GuiSkin.play(skin.click(), 1f);
                        change(() -> {
                            selected = node.role;
                            fanFor = fannable.contains(node.role) && !node.role.equals(fanFor) ? node.role : null;
                        });
                    }
                    return true;
                }
                panning = true;
                dragged = false;
                return true;
            }
        }
        for (int i = widgets.size() - 1; i >= 0; i--) {
            AbstractWidget wd = widgets.get(i);
            if (wd.visible && wd.active && wd.isMouseOver(mx, my) && wd.mouseClicked(mx, my, button)) {
                if (searchBox != null)
                    searchBox.setFocused(wd == searchBox);
                return true;
            }
        }
        if (searchBox != null)
            searchBox.setFocused(false);
        return false;
    }

    boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (preview.drag(dx, dy))
            return true;
        if (panning) {
            dragged = true;
            camX -= dx / zoom;
            camY -= dy / zoom;
            clampCamera();
            return true;
        }
        return false;
    }

    boolean mouseReleased(double mx, double my, int button) {
        preview.release();
        // A click on bare canvas, not a drag, puts the fan away.
        if (panning && !dragged && fanFor != null)
            change(() -> fanFor = null);
        boolean was = panning;
        panning = false;
        return was;
    }

    boolean mouseScrolled(double mx, double my, double scrollY) {
        if (typePopup.mouseScrolled(scrollY) || preview.scroll(mx, my, scrollY))
            return true;
        if (current() == null)
            return false;
        int dir = (int) -Math.signum(scrollY);
        if (in(mx, my, ox + DRAWER_X, oy + 3, DRAWER_SLOTS * DRAWER_PITCH, 18)) {
            drawerScroll = Mth.clamp(drawerScroll + dir, 0, Math.max(0, types.size() - DRAWER_SLOTS));
            return true;
        }
        if (overCanvas(mx, my)) {
            float old = zoom;
            double wx = camX + (mx - centreX()) / old, wy = camY + (my - centreY()) / old;
            zoom = (float) Mth.clamp(old * Math.pow(1.15, scrollY), MIN_ZOOM, MAX_ZOOM);
            camX = wx - (mx - centreX()) / zoom;
            camY = wy - (my - centreY()) / zoom;
            clampCamera();
            return true;
        }
        String role = sel();
        if (role != null && dimsOf(role).size() > MAX_DIM_ROWS
                && in(mx, my, ox + CALL_X, oy + PAN_Y + 15 + 15 * rowsAboveDims(role), CALL_W, 15 * MAX_DIM_ROWS)) {
            dimScroll = Mth.clamp(dimScroll + dir, 0, dimsOf(role).size() - MAX_DIM_ROWS);
            build();
            return true;
        }
        if (role != null && in(mx, my, ox + CALL_X, swatchTop(role), CALL_W, swatchRows(role) * SWATCH_PITCH)) {
            int rows = (swatches(role).size() + swatchCols() - 1) / swatchCols();
            swatchScroll = Mth.clamp(swatchScroll + dir, 0, Math.max(0, rows - swatchRows(role)));
            return true;
        }
        if (in(mx, my, ox + RES_X, oy + PAN_Y, RES_W, PAN_H)) {
            figScroll = Mth.clamp(figScroll + dir, 0, Math.max(0, shown.size() - figRows()));
            return true;
        }
        return false;
    }

    // --- samples and paper ------------------------------------------------------------------------

    /** A sample put in the slot sets the selected part's material, once: the swatches can change it after. */
    void applySample(ItemStack sample) {
        if (ItemStack.isSameItemSameComponents(sample, seen))
            return;
        seen = sample.copy();
        String role = sel();
        if (sample.isEmpty() || role == null)
            return;
        Optional<Material.Located> named = Material.find(ClientMaterials.get(), sample);
        if (named.isEmpty()) {
            skin.cue(GuiSkin.Cue.REJECT);
            return;
        }
        materials(role).stream().filter(l -> l.id().equals(named.get().id())).findFirst().ifPresent(l -> paint(role, l));
    }

    /** A blueprint put in the paper slot opens on the sheet. */
    void openPaper(ItemStack paper) {
        if (ItemStack.isSameItemSameComponents(paper, lastPaper))
            return;
        ModularDesign design = ModularStacks.design(paper);
        if (design != null) {
            List<ModularDesign> variants = paper.getOrDefault(FModular.VARIANTS.get(), List.of());
            change(() -> {
                pins.clear();
                for (ModularDesign v : variants) { // each pinned draft is opened on the sheet to be captured, then the paper's own is
                    open(v);
                    Optional<ModularFigures> f = ToolDesigns.predict(v, ModularTypes.known(), ClientMaterials.get());
                    if (f.isPresent() && pins.size() < MAX_PINS)
                        pins.add(new Pin(capture(), f.get(), v));
                }
                open(design);
            });
        }
        lastPaper = paper.copy();
    }

    /** What would be printed: the drawing, if it can be made, and the pinned drafts of the same type. */
    Optional<ModularDesign> drawing() {
        return baked.isPresent() && figures.isPresent() ? baked : Optional.empty();
    }

    List<ModularDesign> pinnedDesigns() {
        return baked.isEmpty() ? List.of() : pins.stream().filter(p -> p.design().type().equals(baked.get().type())).map(Pin::design).toList();
    }

    /** The selected part's name, or null; the host shows a sample slot while there is one. */
    boolean hasSelection() {
        return current() != null && sel() != null;
    }

    float centreXAbs() {
        return centreX();
    }

    float centreYAbs() {
        return centreY();
    }

    // --- drawing ----------------------------------------------------------------------------------

    private static int alpha(int colour, int a) {
        return a << 24 | colour & 0xFFFFFF;
    }

    private int figRows() {
        return Math.max(1, (PAN_H - 10 - rigH() - Math.min(uses.size(), 4) * 9) / ROW_H);
    }

    /** Whether the type has a test rig, and the height it takes off the top of the figures panel while it is on. */
    private boolean rigged() {
        ModularType t = current();
        return t != null && BlueprintRig.has(t.family());
    }

    private int rigH() {
        return rig && rigged() && figures.isPresent() ? RIG_H : 0;
    }

    /** What the rig is run by: the drawn item's own figures, each against a full bar. */
    private BlueprintRig.Inputs rigInputs() {
        ModularType t = current();
        ModularFigures f = figures.get();
        int head = choice(t.wearRole()).map(l -> MaterialColors.of(l.id())).orElse(0x808080);
        int body = rows().stream().filter(r -> t.roles().get(r).rod()).findFirst().flatMap(r -> choice(r)).map(l -> MaterialColors.of(l.id())).orElse(head);
        float capacity = 0;
        float charge = 0;
        int cells = 1;
        if (t.family() == ModularType.Family.CELL && baked.isPresent()) {
            Optional<ModularBake.CellSpec> c = ModularBake.cell(baked.get(), t, ClientMaterials.get());
            cells = c.map(ModularBake.CellSpec::cells).orElse(1);
            charge = c.map(x -> Mth.clamp((float) Math.log10(1 + x.capacity()) / 7f, 0f, 1f)).orElse(0f);
        } else
            capacity = f.get(Fig.CAPACITY);
        return new BlueprintRig.Inputs(t.family(), fill(Fig.SWING, f.get(Fig.SWING)), fill(Fig.REACH, f.get(Fig.REACH)),
                fill(Fig.IMPACT, f.get(Fig.IMPACT)), fill(Fig.DIG_SPEED, f.get(Fig.DIG_SPEED)), f.get(Fig.DAMAGE), f.get(Fig.JOINT),
                fill(Fig.DRAW, f.get(Fig.DRAW)) , fill(Fig.VELOCITY, f.get(Fig.VELOCITY)), capacity, charge, cells, head, body);
    }

    /** Which things the pointer is over; set once per frame, read by the drawing and the tooltips. */
    private void updateHover(int mx, int my) {
        hoverNode = hoverSwatch = hoverShape = hoverFig = hoverChip = hoverDrawer = hoverFault = hoverLink = -1;
        hoverCorner = false;
        if (current() == null)
            return;
        if (in(mx, my, ox + DRAWER_X, oy + 3, DRAWER_SLOTS * DRAWER_PITCH, 18)) {
            int i = drawerScroll + (mx - ox - DRAWER_X) / DRAWER_PITCH;
            hoverDrawer = i < types.size() ? i : -1;
        }
        if (overCanvas(mx, my)) {
            hoverNode = nodeAt(mx, my);
            if (hoverNode >= 0 && !nodes.get(hoverNode).ghost) {
                Node n = nodes.get(hoverNode);
                int half = Math.round(NODE * zoom / 2);
                hoverCorner = mx >= screenX(n.x) + half - 7 && my <= screenY(n.y) - half + 7;
            }
            if (hoverNode < 0)
                hoverLink = linkAt(mx, my);
            if (hoverNode < 0) {
                int cy = oy + CAN_Y + CAN_H - 13, x = ox + CAN_X + 4;
                for (int i = 0; i < chipText.size(); i++) {
                    int w = font.width(chipText.get(i)) + 6;
                    if (in(mx, my, x, cy, w, 11))
                        hoverChip = i;
                    x += w + 3;
                }
                for (int i = 0; i < faults.size() && hoverChip < 0; i++)
                    if (faultAnchor(faults.get(i)) != null) {
                        int[] a = faultAnchor(faults.get(i));
                        if (Math.abs(mx - a[0]) < 14 && Math.abs(my - a[1]) < 10)
                            hoverFault = i;
                    }
            }
        }
        String role = sel();
        if (role != null) {
            List<Material.Located> mats = swatches(role);
            int sx = ox + CALL_X + 4, sy = swatchTop(role), cols = swatchCols(), rows = swatchRows(role);
            if (in(mx, my, sx, sy, cols * SWATCH_PITCH, rows * SWATCH_PITCH)) {
                int i = (swatchScroll + (my - sy) / SWATCH_PITCH) * cols + (mx - sx) / SWATCH_PITCH;
                hoverSwatch = i < mats.size() ? i : -1;
            }
            int n = shapes(role).size();
            if (n > 1) {
                int ty = shapesTop(role), per = (CALL_W - 8) / THUMB_PITCH;
                if (in(mx, my, sx, ty, per * THUMB_PITCH, shapesHeight(role))) {
                    int i = (my - ty) / THUMB_PITCH * per + (mx - sx) / THUMB_PITCH;
                    hoverShape = i < n ? i : -1;
                }
            }
        }
        if (in(mx, my, ox + RES_X + 2, oy + PAN_Y + 5 + rigH(), RES_W - 4, figRows() * ROW_H)) {
            int i = figScroll + (my - oy - PAN_Y - 5 - rigH()) / ROW_H;
            hoverFig = i < shown.size() ? i : -1;
        }
        updateGhost();
    }

    /** What the pointer would do if it clicked: a swatch, a shape, or a carried material over a part. */
    private void updateGhost() {
        String role = sel();
        String gr = null;
        ResourceLocation gm = null;
        int gs = -1;
        if (hoverSwatch >= 0 && role != null && hoverSwatch < swatches(role).size() && usable(role, swatches(role).get(hoverSwatch))) {
            gr = role;
            gm = swatches(role).get(hoverSwatch).id();
            gs = shapeIndex(role);
        } else if (hoverShape >= 0 && role != null) {
            gr = role;
            gm = choice(role).map(Material.Located::id).orElse(null);
            gs = hoverShape;
        } else if (hoverNode >= 0 && !nodes.get(hoverNode).ghost && !host.carried().isEmpty()) {
            Optional<Material.Located> named = Material.find(ClientMaterials.get(), host.carried());
            if (named.isPresent()) {
                gr = nodes.get(hoverNode).role;
                gm = named.get().id();
                gs = shapeIndex(gr);
            }
        }
        if (Objects.equals(gr, ghostRole) && Objects.equals(gm, ghostMat) && gs == ghostShape)
            return;
        ghostRole = gr;
        ghostMat = gm;
        ghostShape = gs;
        if (gr == null || gm == null) {
            ghost = Optional.empty();
            return;
        }
        String r = gr;
        ResourceLocation m = gm;
        int s = gs;
        ghost = ghosts.computeIfAbsent(gr + "|" + gm + "|" + gs, k -> design(r, m, s)
                .flatMap(d -> ToolDesigns.predict(d, ModularTypes.known(), ClientMaterials.get())));
    }

    /** Draws the board and its controls; the host has drawn whatever it stands on. */
    void drawBoard(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        ModularType t = current();
        if (t == null)
            return;
        String role = sel();
        updateHover(mouseX, mouseY);
        drawDrawer(g);
        drawCanvas(g, mouseX, mouseY);
        drawParts(g, role);
        drawFigures(g);
        for (AbstractWidget widget : widgets)
            if (widget.visible)
                widget.render(g, mouseX, mouseY, partialTick);
    }

    private void drawDrawer(GuiGraphics g) {
        int x = ox + DRAWER_X, y = oy + 3;
        skin.inset(g, x - 1, y - 1, DRAWER_SLOTS * DRAWER_PITCH + 2, 20);
        for (int i = 0; i < DRAWER_SLOTS && drawerScroll + i < types.size(); i++) {
            int idx = drawerScroll + i;
            skin.row(g, x + i * DRAWER_PITCH, y, DRAWER_PITCH, 18, idx == type, idx == hoverDrawer);
            g.renderItem(typeIcons.get(idx), x + i * DRAWER_PITCH + 1, y + 1);
        }
        skin.scrollbar(g, x + DRAWER_SLOTS * DRAWER_PITCH - 2, y, 18, types.size(), DRAWER_SLOTS, drawerScroll);
    }

    private void drawCanvas(GuiGraphics g, int mx, int my) {
        int x0 = ox + CAN_X, y0 = oy + CAN_Y;
        skin.inset(g, x0, y0, CAN_W, CAN_H);
        g.enableScissor(x0 + 1, y0 + 1, x0 + CAN_W - 1, y0 + CAN_H - 1);
        // faint ruled grid that moves with the drawing
        float step = 40 * zoom;
        int grid = alpha(skin.ink(), 0x12);
        for (float gx = centreX() + (float) ((Math.floor(camX / 40) * 40 - camX) * zoom) - step * 20; gx < x0 + CAN_W; gx += step)
            if (gx > x0)
                g.fill((int) gx, y0 + 1, (int) gx + 1, y0 + CAN_H - 1, grid);
        for (float gy = centreY() + (float) ((Math.floor(camY / 40) * 40 - camY) * zoom) - step * 20; gy < y0 + CAN_H; gy += step)
            if (gy > y0)
                g.fill(x0 + 1, (int) gy, x0 + CAN_W - 1, (int) gy + 1, grid);
        float time = GuiStyle.time();
        boolean still = ClientSettings.reduceMotion();
        int half = Math.round(NODE * zoom / 2);
        // links, under the nodes, each by its own joint figure
        for (int i = 0; i < links.size(); i++) {
            Link l = links.get(i);
            Node a = nodes.get(l.a), b = nodes.get(l.b);
            boolean bad = faults.stream().anyMatch(f -> f.roles().size() == 2 && l.pairs.stream().anyMatch(p -> f.roles().contains(p[0]) && f.roles().contains(p[1])));
            if (bad)
                continue; // drawn with the faults
            float joint = linkValue[i];
            int th = joint < 0.7f ? 3 : joint <= 1f ? 2 : 1;
            int colour = joint > 1f ? skin.warn() : joint > 0.7f ? skin.accent() : alpha(skin.ink(), 0xC0);
            segment(g, a, b, half + 2, Math.max(1, Math.round(th * Math.max(1f, zoom))) + (i == hoverLink ? 1 : 0), colour, joint > 1f, time, still);
        }
        // a pair that does not work together: a red dashed link whether or not the pattern joins them
        for (Fault f : faults)
            if (f.roles().size() == 2 && nodeOf.containsKey(f.roles().get(0)) && nodeOf.containsKey(f.roles().get(1))
                    && !nodeOf.get(f.roles().get(0)).equals(nodeOf.get(f.roles().get(1))))
                segment(g, nodes.get(nodeOf.get(f.roles().get(0))), nodes.get(nodeOf.get(f.roles().get(1))), half + 2,
                        Math.max(2, Math.round(2 * zoom)), skin.warn(), true, time, still);
        for (int i = 0; i < nodes.size(); i++)
            drawNode(g, i, half, time, still);
        for (Fault f : faults) {
            int[] a = faultAnchor(f);
            if (a == null)
                continue;
            String text = f.reason().getString();
            int w = Math.min(font.width(text), 150) + 6;
            skin.plaque(g, a[0] - w / 2, a[1] + 4, w, 11);
            g.fill(a[0] - 3, a[1] - 1, a[0] + 4, a[1] + 1, skin.warn());
            g.fill(a[0] - 1, a[1] - 3, a[0] + 2, a[1] + 4, skin.warn());
            fit(g, text, a[0] - w / 2 + 3, a[1] + 6, w - 6, skin.warn());
        }
        // the type's name as the drawing's title block, and the qualities as stamped chips
        g.drawString(font, typeName(type), x0 + 5, y0 + 4, skin.ink(), false);
        if (!designation.getString().isEmpty())
            g.drawString(font, designation, x0 + 5 + font.width(typeName(type)) + 6, y0 + 4, skin.accent(), false);
        int cx = x0 + 4, cy = y0 + CAN_H - 13;
        for (int i = 0; i < chipText.size(); i++) {
            int w = font.width(chipText.get(i)) + 6;
            if (cx + w > x0 + CAN_W - 4)
                break;
            skin.plaque(g, cx, cy, w, 11);
            if (i == hoverChip)
                skin.row(g, cx, cy, w, 11, false, true);
            g.drawString(font, chipText.get(i), cx + 3, cy + 2, skin.ink(), false);
            cx += w + 3;
        }
        g.disableScissor();
    }

    /** Where a fault's mark and reason go on screen, or null if it has no part on the canvas. */
    private int[] faultAnchor(Fault f) {
        if (f.roles().isEmpty())
            return null;
        Integer a = nodeOf.get(f.roles().get(0)), b = f.roles().size() > 1 ? nodeOf.get(f.roles().get(1)) : a;
        if (a == null || b == null)
            return null;
        float wx = (nodes.get(a).x + nodes.get(b).x) / 2, wy = (nodes.get(a).y + nodes.get(b).y) / 2;
        return new int[]{screenX(wx), screenY(wy) + (f.roles().size() > 1 ? 0 : Math.round(NODE * zoom / 2) + 10)};
    }

    /** A link between the edges of two nodes, solid or dashed. */
    private void segment(GuiGraphics g, Node a, Node b, int trim, int th, int colour, boolean dashed, float time, boolean still) {
        float ax = screenX(a.x), ay = screenY(a.y), bx = screenX(b.x), by = screenY(b.y);
        float dx = bx - ax, dy = by - ay, len = Mth.sqrt(dx * dx + dy * dy);
        if (len <= trim * 2)
            return;
        float ux = dx / len, uy = dy / len;
        int x0 = Math.round(ax + ux * trim), y0 = Math.round(ay + uy * trim), x1 = Math.round(bx - ux * trim), y1 = Math.round(by - uy * trim);
        int h = th / 2;
        if (!dashed) {
            if (x0 == x1)
                g.fill(x0 - h, Math.min(y0, y1), x0 - h + th, Math.max(y0, y1) + 1, colour);
            else if (y0 == y1)
                g.fill(Math.min(x0, x1), y0 - h, Math.max(x0, x1) + 1, y0 - h + th, colour);
            else
                for (int o = 0; o < th; o++)
                    GuiStyle.line(g, x0, y0 + o - h, x1, y1 + o - h, colour);
            return;
        }
        int steps = Math.max(1, (int) ((len - trim * 2) / 6));
        int phase = still ? 0 : (int) (time * 0.3f) % 2;
        for (int i = phase; i < steps; i += 2) {
            int sx = x0 + (x1 - x0) * i / steps, sy = y0 + (y1 - y0) * i / steps;
            int ex = x0 + (x1 - x0) * (i + 1) / steps, ey = y0 + (y1 - y0) * (i + 1) / steps;
            for (int o = 0; o < th; o++)
                GuiStyle.line(g, sx, sy + o - h, ex, ey + o - h, colour);
        }
    }

    /** A part or slot, dimmed when a fan is open and it is not part of it, wearing a plus when it has attachments left to fit. */
    private void drawNode(GuiGraphics g, int i, int half, float time, boolean still) {
        Node n = nodes.get(i);
        drawNodeBody(g, i, half, time, still);
        int sx = screenX(n.x), sy = screenY(n.y);
        if (!n.ghost && fannable.contains(n.role)) {
            boolean open = n.role.equals(fanFor);
            g.drawString(font, open ? "-" : "+", sx + half - 5, sy + half - 9, open ? skin.accent() : skin.ink(), false);
        }
        if (!inFan(n))
            g.fill(sx - half - 1, sy - half - 1, sx + half + 1, sy + half + 1, 0xA0101010);
        else if (n.ghost && n.faint)
            g.fill(sx - half, sy - half, sx + half, sy + half, 0x90101010);
    }

    private void drawNodeBody(GuiGraphics g, int i, int half, float time, boolean still) {
        Node n = nodes.get(i);
        int sx = screenX(n.x), sy = screenY(n.y) + (still ? 0 : Math.round(Mth.sin(time * 0.07f + i * 1.7f) * 1.2f * zoom));
        if (sx < ox + CAN_X - half - 20 || sx > ox + CAN_X + CAN_W + half + 20 || sy < oy + CAN_Y - half - 20 || sy > oy + CAN_Y + CAN_H + half + 20)
            return;
        boolean hot = i == hoverNode, picked = !n.ghost && n.role.equals(sel());
        if (n.ghost) {
            Integer pi = nodeOf.get(parentOf.get(n.role));
            if (pi != null)
                segment(g, nodes.get(pi), n, half + 2, 1, alpha(skin.dim(), 0x90), true, time, still);
            int colour = hot ? skin.ink() : skin.dim();
            for (int from = 0; from < 8 * half; from += 8)
                GuiStyle.perimeter(g, sx - half, sy - half, sx + half, sy + half, from, 4, colour);
            if (!n.icon.isEmpty()) {
                pose(g, n.icon, sx, sy, half);
                g.fill(sx - half + 1, sy - half + 1, sx + half, sy + half, alpha(skin.dim(), hot ? 0x50 : 0x90));
            }
            g.drawCenteredString(font, "+", sx, sy - 4, colour);
            if (hot || zoom >= 1.2f)
                nodeLabel(g, roleName(n.role), sx, sy + half + 2, hot ? skin.ink() : skin.dim());
            return;
        }
        skin.plaque(g, sx - half, sy - half, 2 * half, 2 * half);
        Optional<Material.Located> c = choice(n.role);
        int rgb = c.map(l -> MaterialColors.of(l.id())).orElse(0x808080);
        ItemStack icon = icons.getOrDefault(n.role, ItemStack.EMPTY);
        if (icon.isEmpty())
            g.fill(sx - half + 3, sy - half + 3, sx + half - 3, sy + half - 5, 0xFF000000 | rgb);
        else
            pose(g, icon, sx, sy - 1, half);
        g.fill(sx - half + 2, sy + half - 3, sx + half - 2, sy + half - 1, 0xFF000000 | rgb);
        boolean bad = faults.stream().anyMatch(f -> f.roles().contains(n.role));
        if (bad)
            g.renderOutline(sx - half - 1, sy - half - 1, 2 * half + 2, 2 * half + 2, skin.warn());
        else if (picked)
            g.renderOutline(sx - half - 2, sy - half - 2, 2 * half + 4, 2 * half + 4, skin.accent());
        else if (hot)
            g.renderOutline(sx - half - 1, sy - half - 1, 2 * half + 2, 2 * half + 2, skin.ink());
        Float w = trace.get(n.role);
        if (w != null && w > 0.05f)
            g.renderOutline(sx - half - 3, sy - half - 3, 2 * half + 6, 2 * half + 6, alpha(skin.accent(), (int) (0x40 + 0xBF * w)));
        Long flash = flashes.get(n.role);
        if (flash != null && !still) {
            long age = System.currentTimeMillis() - flash;
            if (age < 400)
                GuiStyle.ring(g, sx, sy, half + (int) (age / 20), 1, alpha(0xFF000000 | rgb, (int) (0xC0 * (1 - age / 400f))));
        }
        if ((hot || picked) && keyOf(n.role) != null && removable(keyOf(n.role)))
            g.drawString(font, "x", sx + half - 6, sy - half + 1, hot && hoverCorner ? skin.warn() : skin.dim(), false);
        if (zoom >= 0.7f || hot || picked)
            nodeLabel(g, roleName(n.role), sx, sy + half + 2, picked ? skin.accent() : skin.dim());
    }

    /** Role name under a node, shrunk to fit the node pitch so neighbours' labels never overlap. */
    private void nodeLabel(GuiGraphics g, Component text, int cx, int y, int colour) {
        float room = PITCH * zoom - 4;
        float k = Math.min(1f, room / Math.max(1, font.width(text)));
        g.pose().pushPose();
        g.pose().translate(cx, y, 0);
        g.pose().scale(k, k, 1f);
        g.drawString(font, text, -font.width(text) / 2, 0, colour, false);
        g.pose().popPose();
    }

    /** {@code stack} filling a node of half-size {@code half} at (cx, cy). */
    private static void pose(GuiGraphics g, ItemStack stack, int cx, int cy, int half) {
        float s = Math.max(0.4f, (2 * half - 8) / 16f);
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(s, s, 1);
        g.renderItem(stack, -8, -8);
        g.pose().popPose();
    }

    private void drawParts(GuiGraphics g, String role) {
        int x = ox + CALL_X, y = oy + PAN_Y;
        skin.inset(g, x, y, CALL_W, PAN_H);
        if (role == null)
            return;
        ModularType t = current();
        List<Material.Located> mats = swatches(role);
        Optional<Material.Located> now = choice(role);
        skin.inset(g, x + 4, swatchTop(role) - SEARCH_H + 1, CALL_W - 8, SEARCH_H - 3);
        if (dimsOf(role).size() > MAX_DIM_ROWS)
            skin.scrollbar(g, x + CALL_W - 4, y + 15 + 15 * rowsAboveDims(role), 15 * MAX_DIM_ROWS, dimsOf(role).size(), MAX_DIM_ROWS, dimScroll);
        g.drawString(font, roleName(role), x + 5, y + 4, skin.ink(), false);
        String name = materialName(role);
        int nw = Math.min(font.width(name), 110);
        fit(g, name, x + CALL_W - 5 - nw, y + 4, 110, skin.dim());
        int sx = x + 4, sy = swatchTop(role), cols = swatchCols(), rows = swatchRows(role);
        int total = (mats.size() + cols - 1) / cols;
        swatchScroll = Mth.clamp(swatchScroll, 0, Math.max(0, total - rows));
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                int i = (swatchScroll + r) * cols + c;
                if (i >= mats.size())
                    break;
                int px = sx + c * SWATCH_PITCH, py = sy + r * SWATCH_PITCH;
                Material.Located l = mats.get(i);
                boolean fits = usable(role, l);
                g.fill(px, py, px + SWATCH, py + SWATCH, 0xFF000000 | MaterialColors.of(l.id()));
                if (!fits) {
                    g.fill(px, py, px + SWATCH, py + SWATCH, 0xB0202020);
                    g.drawCenteredString(font, "x", px + SWATCH / 2 + 1, py + (SWATCH - 8) / 2 + 1, skin.warn());
                }
                boolean on = now.isPresent() && now.get().id().equals(l.id());
                g.renderOutline(px - (on ? 1 : 0), py - (on ? 1 : 0), SWATCH + (on ? 2 : 0), SWATCH + (on ? 2 : 0),
                        on ? skin.accent() : i == hoverSwatch ? skin.ink() : alpha(skin.ink(), 0x40));
            }
        skin.scrollbar(g, x + CALL_W - 4, sy, rows * SWATCH_PITCH, total, rows, swatchScroll);
        List<ToolTableShaping.Option> options = shapes(role);
        if (options.size() > 1) {
            int per = (CALL_W - 8) / THUMB_PITCH, ty = shapesTop(role), cur = shapeIndex(role);
            int rgb = now.map(l -> MaterialColors.of(l.id())).orElse(0x808080);
            for (int i = 0; i < options.size() && i < per * 2; i++) {
                int px = sx + i % per * THUMB_PITCH, py = ty + i / per * THUMB_PITCH;
                skin.plaque(g, px, py, THUMB, THUMB);
                skin.row(g, px, py, THUMB, THUMB, i == cur, i == hoverShape);
                ToolTableShaping.Option o = options.get(i);
                List<String> grid = o.variant().shape().isEmpty() ? o.shaping().shape() : o.variant().shape();
                int gw = 1, gh = grid.size();
                for (String row : grid)
                    gw = Math.max(gw, row.length());
                int cell = Math.max(1, Math.min(3, (THUMB - 4) / Math.max(gw, gh)));
                int ox = px + (THUMB - gw * cell) / 2, oy = py + (THUMB - gh * cell) / 2;
                for (int r = 0; r < gh; r++)
                    for (int c = 0; c < grid.get(r).length(); c++)
                        if (grid.get(r).charAt(c) != ' ')
                            g.fill(ox + c * cell, oy + r * cell, ox + (c + 1) * cell, oy + (r + 1) * cell, 0xFF000000 | rgb);
                if (o.forged())
                    g.fill(px + THUMB - 5, py + 2, px + THUMB - 2, py + 5, skin.accent());
            }
        }
    }

    /** What a figure is; a family with its own line (key {@code .tip.<family>}) reads that instead of the generic tip. */
    private List<Component> figureTip(Fig f) {
        List<Component> out = new ArrayList<>(ModularBlueprintItem.explain(f));
        ModularType t = current();
        String key = "modular.feedback.fig." + f.name().toLowerCase(java.util.Locale.ROOT) + ".tip." + (t == null ? "" : t.family().name().toLowerCase(java.util.Locale.ROOT));
        if (t != null && I18n.exists(key))
            out.set(1, Component.translatable(key).withStyle(net.minecraft.ChatFormatting.GRAY));
        return out;
    }

    private String label(Fig f) {
        return labels.computeIfAbsent(f, k -> Component.translatable("modular.feedback.fig." + k.name().toLowerCase(java.util.Locale.ROOT))
                .getString().split(",", 2)[0]);
    }

    private float fill(Fig f, float v) {
        float top = f == Fig.JOINT ? 2f : tops[f.ordinal()];
        return top <= 0 ? 0 : Mth.clamp(v / top, 0f, 1f);
    }

    private void drawFigures(GuiGraphics g) {
        int x = ox + RES_X, y = oy + PAN_Y;
        skin.inset(g, x, y, RES_W, PAN_H);
        if (figures.isEmpty()) {
            int ty = y + 5;
            for (Fault f : faults)
                for (net.minecraft.util.FormattedCharSequence line : font.split(f.reason(), RES_W - 10)) {
                    g.drawString(font, line, x + 5, ty, skin.warn(), false);
                    ty += 10;
                }
            return;
        }
        ModularFigures cur = figures.get();
        int rows = figRows();
        int top = y + 5 + rigH();
        if (rigH() > 0) {
            BlueprintRig.draw(g, skin, rigInputs(), x + 2, y + 2, RES_W - 4, RIG_H - 4, GuiStyle.time(), ClientSettings.reduceMotion());
            g.fill(x + 3, y + RIG_H - 1, x + RES_W - 3, y + RIG_H, alpha(skin.ink(), 0x40));
        }
        figScroll = Mth.clamp(figScroll, 0, Math.max(0, shown.size() - rows));
        boolean shift = net.minecraft.client.gui.screens.Screen.hasShiftDown();
        int bx = x + 4 + LABEL_W + 4;
        for (int r = 0; r < rows && figScroll + r < shown.size(); r++) {
            Fig f = shown.get(figScroll + r);
            int ry = top + r * ROW_H;
            skin.row(g, x + 2, ry - 1, RES_W - 7, ROW_H, false, figScroll + r == hoverFig);
            fit(g, label(f), x + 5, ry + 1, LABEL_W, skin.dim());
            float v = cur.get(f);
            if (shift) {
                g.drawString(font, PartPresenter.num(v), bx, ry + 1, skin.ink(), false);
                continue;
            }
            skin.bar(g, bx, ry + 2, BAR_W, 5, fill(f, v), f == Fig.JOINT ? 0.5f : -1f);
            if (reference.isPresent())
                g.fill(bx + Math.round(fill(f, reference.get().get(f)) * BAR_W), ry, bx + Math.round(fill(f, reference.get().get(f)) * BAR_W) + 1, ry + 9,
                        skin.ink());
            for (int p = 0; p < pins.size(); p++)
                if (pins.get(p).draft().type() == type) {
                    int px = bx + Math.round(fill(f, pins.get(p).figures().get(f)) * BAR_W);
                    g.fill(px, ry + 1, px + 2, ry + 8, PIN_COLOURS[p % PIN_COLOURS.length]);
                }
            if (ghost.isPresent()) {
                int gx = bx + Math.round(fill(f, ghost.get().get(f)) * BAR_W), cx = bx + Math.round(fill(f, v) * BAR_W);
                if (gx != cx)
                    g.fill(Math.min(gx, cx), ry + 3, Math.max(gx, cx), ry + 6, alpha(skin.accent(), 0x90));
                g.fill(gx - 1, ry, gx + 1, ry + 9, skin.accent());
            }
            if (traceFor == f)
                g.renderOutline(bx - 1, ry + 1, BAR_W + 2, 7, alpha(skin.accent(), 0xA0));
            if (reference.isPresent()) {
                float was = reference.get().get(f);
                float share = was == 0 ? (v == 0 ? 0 : 1) : (v - was) / Math.abs(was);
                if (Math.abs(share) > 0.01f)
                    g.drawString(font, share > 0 ? "▲" : "▼", x + RES_W - 13, ry + 1, skin.accent(), false);
            }
        }
        skin.scrollbar(g, x + RES_W - 4, top, rows * ROW_H, shown.size(), rows, figScroll);
        int uy = y + PAN_H - 4 - Math.min(uses.size(), 4) * 9;
        for (int i = 0; i < uses.size() && i < 4; i++)
            g.drawString(font, uses.get(i), x + 5, uy + i * 9, skin.ink(), false);
        // the parts that changed a hovered figure, found once per figure
        Fig hf = hoverFig >= 0 ? shown.get(hoverFig) : null;
        if (hf != traceFor) {
            traceFor = hf;
            traceParts(hf);
        }
    }

    /** Which parts, changed from the first drawing, moved {@code fig}: each put back alone and the figure compared (cached per drawing). */
    private void traceParts(Fig fig) {
        trace.clear();
        if (fig == null || figures.isEmpty() || baked.isEmpty() || referenceDesign.isEmpty())
            return;
        if (ablation == null) {
            ablation = new HashMap<>();
            int tried = 0;
            for (ModularDesign.Part p : baked.get().parts()) {
                ModularDesign.Part was = referenceDesign.get().part(p.role());
                if (was == null || was.equals(p) || ++tried > 6)
                    continue;
                ToolDesigns.predict(baked.get().withPart(was), ModularTypes.known(), ClientMaterials.get()).ifPresent(f -> ablation.put(p.role(), f));
            }
        }
        float max = 0;
        for (Map.Entry<String, ModularFigures> e : ablation.entrySet()) {
            float d = Math.abs(figures.get().get(fig) - e.getValue().get(fig));
            trace.put(e.getKey(), d);
            max = Math.max(max, d);
        }
        if (max <= 0) {
            trace.clear();
            return;
        }
        float m = max;
        trace.replaceAll((k, d) -> d / m);
    }

    /** What the drawn item is for beyond the shared figures: a battery's cells and case, a vessel's experience ceiling. */
    private List<Component> usesOf(ModularDesign d) {
        ModularType t = current();
        if (t == null)
            return List.of();
        return switch (t.family()) {
            case CELL -> ModularBake.cell(d, t, ClientMaterials.get()).map(BlueprintBoard::cellLines).orElse(List.of());
            case MACHINE -> ModularBake.machine(d, t, ClientMaterials.get()).map(BlueprintBoard::machineLines).orElse(List.of());
            case VESSEL -> ModularBake.xpStore(d, t, ClientMaterials.get(), null).<List<Component>>map(x -> List.of(
                    Component.translatable("gui.feedback.blueprint_table.vessel.points", x.maxPoints()),
                    Component.translatable("gui.feedback.blueprint_table.vessel.level", String.format(java.util.Locale.ROOT, "%.0f", x.maxPoints() / x.perLevel()))))
                    .orElse(List.of());
            case BACKPACK -> ModularBake.bake(d, t, ClientMaterials.get(), 0).<List<Component>>map(f -> List.of(
                    Component.translatable("gui.feedback.blueprint_table.pack.slots", Math.round(f.get(ModularFigures.Fig.SLOTS))),
                    Component.translatable("gui.feedback.blueprint_table.pack.carry", String.format(java.util.Locale.ROOT, "%.1f", f.get(ModularFigures.Fig.CARRY))),
                    Component.translatable("gui.feedback.blueprint_table.pack.points", Math.round(f.get(ModularFigures.Fig.POINTS))),
                    Component.translatable("gui.feedback.blueprint_table.pack.stores", Math.round(f.get(ModularFigures.Fig.STORES)))))
                    .orElse(List.of());
            case BEDROLL -> ModularBake.bake(d, t, ClientMaterials.get(), 0).<List<Component>>map(f -> List.of(
                    Component.translatable("gui.feedback.blueprint_table.bedroll.comfort", Math.round(100 * f.get(ModularFigures.Fig.COMFORT)))))
                    .orElse(List.of());
            default -> List.of();
        };
    }

    private static List<Component> machineLines(ModularBake.MachineSpec m) {
        return List.of(Component.translatable("gui.feedback.blueprint_table.machine.frame", Math.round(m.frameSu())),
                Component.translatable("gui.feedback.blueprint_table.machine.volts", String.format(java.util.Locale.ROOT, "%.3g", m.voltsPerRpm())),
                Component.translatable("gui.feedback.blueprint_table.machine.amps", Math.round(m.ratedAmps())),
                Component.translatable("gui.feedback.blueprint_table.machine.band", Component.translatable(m.band().key())));
    }

    private static List<Component> cellLines(ModularBake.CellSpec c) {
        return List.of(Component.translatable("gui.feedback.blueprint_table.cell.cells", c.cells()),
                Component.translatable("gui.feedback.blueprint_table.cell.volts", String.format(java.util.Locale.ROOT, "%.1f", c.cells() * c.plates().voltsFull())),
                Component.translatable("gui.feedback.blueprint_table.cell.capacity", Math.round(c.capacity())),
                Component.translatable("gui.feedback.blueprint_table.cell.case", Component.translatable(c.caseBand().key())));
    }


    private void fit(GuiGraphics g, String s, int x, int y, int w, int colour) {
        g.drawString(font, font.width(s) <= w ? s : font.plainSubstrByWidth(s, w - font.width("...")) + "...", x, y, colour, false);
    }

    /** Hovering says what a thing is: a part, a swatch, a figure, a chip, a fault, a type. */
    void tooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (typePopup.active())
            return;
        List<Component> lines = hoverLines(mouseX, mouseY);
        if (lines != null && !lines.isEmpty())
            g.renderTooltip(font, lines.stream().flatMap(c -> font.split(c, 190).stream()).toList(), mouseX, mouseY);
    }

    private List<Component> hoverLines(int mx, int my) {
        ModularType t = current();
        if (t == null)
            return null;
        if (hoverDrawer >= 0)
            return List.of(typeName(hoverDrawer));
        if (in(mx, my, ox + DRAWER_X + DRAWER_SLOTS * DRAWER_PITCH + 4, oy + 4, 16, 16))
            return List.of(Component.translatable("gui.feedback.blueprint_table.search"));
        if (rigged() && in(mx, my, ox + 244, oy + 4, 32, 16))
            return List.of(Component.translatable("gui.feedback.blueprint_table.rig.tip"));
        if (in(mx, my, ox + 326, oy + 4, 26, 16))
            return List.of(Component.translatable("gui.feedback.blueprint_table.pin.tip"));
        if (in(mx, my, ox + 278, oy + 4, pins.size() * 16, 16))
            return List.of(Component.translatable("gui.feedback.blueprint_table.unpin"));
        if (hoverFault >= 0)
            return List.of(faults.get(hoverFault).reason());
        if (hoverChip >= 0)
            return List.of(modifiers.get(hoverChip).line());
        if (hoverLink >= 0 && hoverNode < 0 && linkWorst[hoverLink] != null) {
            String[] pair = linkWorst[hoverLink];
            return List.of(Component.translatable("gui.feedback.blueprint_table.joint", roleName(pair[0]), roleName(pair[1]),
                    Math.round(linkValue[hoverLink] * 100)));
        }
        if (hoverNode >= 0) {
            Node n = nodes.get(hoverNode);
            if (n.ghost) {
                List<Component> out = new ArrayList<>(roleTip(n.role));
                out.set(0, Component.translatable("gui.feedback.blueprint_table.add_tip", roleName(n.role)));
                if (n.faint)
                    out.add(Component.translatable("gui.feedback.blueprint_table.conflict").withStyle(net.minecraft.ChatFormatting.RED));
                return out;
            }
            if (hoverCorner && keyOf(n.role) != null && removable(keyOf(n.role)))
                return List.of(Component.translatable("gui.feedback.blueprint_table.remove"));
            List<Component> out = new ArrayList<>(roleTip(n.role));
            out.add(Component.literal(materialName(n.role)));
            for (Fault f : faults)
                if (f.roles().contains(n.role))
                    out.add(f.reason());
            return out;
        }
        if (hoverSwatch >= 0 && sel() != null)
        {
            Material.Located l = swatches(sel()).get(hoverSwatch);
            return usable(sel(), l) ? List.of(materialLabel(l))
                    : List.of(materialLabel(l), Component.translatable("gui.feedback.blueprint_table.unfit").withStyle(net.minecraft.ChatFormatting.RED));
        }
        if (hoverShape >= 0 && sel() != null)
            return PartPresenter.describe(shapes(sel()).get(hoverShape));
        if (hoverFig >= 0) {
            Fig f = shown.get(hoverFig);
            List<Component> out = figureTip(f);
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown() && reference.isPresent() && figures.isPresent())
                out.add(FigureCompare.line(f, reference.get().get(f), figures.get().get(f)));
            else
                out.add(Component.translatable("gui.feedback.blueprint_table.rows.hint").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            return out;
        }
        return null;
    }
}
