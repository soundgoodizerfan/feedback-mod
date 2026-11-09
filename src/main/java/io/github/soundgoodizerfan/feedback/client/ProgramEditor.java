// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.registry.FSounds;

import io.github.soundgoodizerfan.feedback.control.programmer.NodePos;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerBlockEntity;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerCommitPayload;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerLinkPayload;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerLinkedPayload;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.soundgoodizerfan.feedback.control.Analog;
import io.github.soundgoodizerfan.feedback.control.program.ActuatorNode;
import io.github.soundgoodizerfan.feedback.control.program.MathNode;
import io.github.soundgoodizerfan.feedback.control.program.DelayNode;
import io.github.soundgoodizerfan.feedback.control.program.LogicNode;
import io.github.soundgoodizerfan.feedback.control.program.ReadNode;
import io.github.soundgoodizerfan.feedback.control.program.WriteNode;
import io.github.soundgoodizerfan.feedback.control.program.ComparatorNode;
import io.github.soundgoodizerfan.feedback.control.program.ControllerLimits;
import io.github.soundgoodizerfan.feedback.control.program.ProgramEvaluator;
import io.github.soundgoodizerfan.feedback.control.program.ProgramVariable;
import io.github.soundgoodizerfan.feedback.control.program.ValueType;
import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;
import io.github.soundgoodizerfan.feedback.control.program.ProgramNode;
import io.github.soundgoodizerfan.feedback.control.program.SensorNode;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The node canvas: a pannable, zoomable graph editor with a toolbar (Print, Erase, Stamp, Save function) along the top and the
 * variables panel at the right. It fills whatever rectangle its host gives it ({@link #viewport}), which makes it a component, not a
 * screen: {@link ProgrammerScreen} hosts it full-window under a strip of slots, and the Computer's desktop hosts it in a resizable
 * window ({@code EditorWindow}). The host forwards input and a tick, calls {@link #flush} before it closes, and supplies the program
 * through a {@link Source}.
 * <p>
 * The interaction model is read from RetroSFM's blueprint editor (MIT, {@code breakinblocks/RetroSFM},
 * branch {@code 1.21.1}; see {@code THIRD-PARTY-LICENSES.md}): a camera with zoom about the
 * cursor, explicit pin positions per node rather than edge-band guessing, drag-from-pin wiring
 * with compatible pins highlighted, a right-click palette that -- dropped from a pin -- offers
 * only cards that can attach there and wires them in, box/shift selection, alt-click to cut,
 * bezier wires, and undo/copy/paste over a local copy committed on close. Written here, not
 * copied. Not taken: its inspector dock (a card here has at most one setting, edited in place),
 * its text-program preview and import (there's no text form of a {@link ProgramGraph}), and
 * snap-to-grid.
 * <p>
 * Every edit lands on {@link #graph}/{@link #positions}, this editor's own copy; the server sees
 * them only as one {@link ProgrammerCommitPayload} on Print or close. Linking a card is the
 * exception, since the Data Connector selection lives on the server -- see {@link ProgrammerLinkPayload}.
 */
final class ProgramEditor {

    /** Height of the toolbar along the top of the editor. */
    private static final int TOOL_H = 22;

    private static final int NODE_W = 100;
    private static final int HEADER_H = 12;
    private static final int ROW_H = 12;
    private static final int NODE_PAD = 4;

    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 3.0;
    private static final double ZOOM_STEP = 1.15;
    private static final double GRID_STEP = 32;
    private static final double MIN_GRID_PIXELS = 12;
    private static final double WIRE_HIT = 6;
    /** Screen pixels of movement before a press on a card turns into a drag instead of its click action. */
    private static final int DRAG_THRESHOLD = 4;
    private static final int MAX_HISTORY = 64;

    private static final int PANEL_W = 140;
    private static final int PANEL_ROW_H = 12;

    private static final int PALETTE_W = 110;
    private static final int PALETTE_HEADER_H = 14;
    private static final int PALETTE_ROW_H = 13;

    private final int COLOR_BACKGROUND;
    private final int COLOR_GRID_MINOR;
    private final int COLOR_GRID_MAJOR;
    private final int COLOR_NODE_BODY;
    private final int COLOR_NODE_BORDER;
    private final int COLOR_SELECTED;
    private static final int COLOR_PIN_BORDER = 0xFF10151A;
    private static final int COLOR_WIRE_HOVER = 0xFFFF6B6B;
    private final int COLOR_BOX_FILL;
    private final int COLOR_TEXT;
    private final int COLOR_MUTED;
    private static final int COLOR_ERROR = 0xFFFF6B6B;
    private final int COLOR_PALETTE;
    private final int COLOR_PALETTE_HOVER;
    private static final int COLOR_BOOLEAN = 0xFFCC5555;
    private static final int COLOR_NUMBER_UNKNOWN = 0xFF55CC55;

    /** The palette's card kinds -- Greater/Less are one {@link ComparatorNode} shape with two labels; Logic and Math each one card whose operator cycles. */
    private enum Kind {
        SENSOR("sensor", 0xFFB8612F),
        GREATER("greater", 0xFF7449B8),
        LESS("less", 0xFF7449B8),
        LOGIC("logic", 0xFF7449B8),
        MATH("math", 0xFF3E8F5A),
        DELAY("delay", 0xFF8F7A2F),
        READ("read", 0xFF2F8F8F),
        WRITE("write", 0xFF8F2F6E),
        ACTUATOR("actuator", 0xFF2F68B8);

        final String key;
        final int color;

        Kind(String key, int color) {
            this.key = key;
            this.color = color;
        }

        Component label() {
            return Component.translatable("feedback.programmer.card." + key);
        }

        ProgramNode create(int id, String variable) {
            return switch (this) {
                case SENSOR -> new SensorNode(id, null, null);
                case GREATER -> new ComparatorNode(id, ComparatorNode.Compare.GREATER, ProgramNode.UNWIRED, ProgramNode.UNWIRED, 0f);
                case LESS -> new ComparatorNode(id, ComparatorNode.Compare.LESS, ProgramNode.UNWIRED, ProgramNode.UNWIRED, 0f);
                case LOGIC -> new LogicNode(id, LogicNode.Gate.AND, ProgramNode.UNWIRED, ProgramNode.UNWIRED);
                case MATH -> new MathNode(id, MathNode.Op.ADD, ProgramNode.UNWIRED, ProgramNode.UNWIRED, 0f);
                case DELAY -> new DelayNode(id, ProgramNode.UNWIRED, ValueType.NUMBER, FTuning.DELAY_NAMED_TICKS[0]);
                case READ -> new ReadNode(id, variable);
                case WRITE -> new WriteNode(id, variable, ProgramNode.UNWIRED, ProgramNode.UNWIRED);
                case ACTUATOR -> new ActuatorNode(id, ProgramNode.UNWIRED, null);
            };
        }

        static Kind of(ProgramNode node) {
            return switch (node) {
                case SensorNode ignored -> SENSOR;
                case ComparatorNode n -> n.op() == ComparatorNode.Compare.GREATER ? GREATER : LESS;
                case LogicNode ignored -> LOGIC;
                case MathNode ignored -> MATH;
                case ActuatorNode ignored -> ACTUATOR;
                case DelayNode ignored -> DELAY;
                case ReadNode ignored -> READ;
                case WriteNode ignored -> WRITE;
            };
        }
    }

    private enum DragMode {NONE, PAN, NODES, WIRE, BOX}

    /** {@code slot} is a {@link ProgramNode#ports()} index, or -1 for the node's one output. */
    private record PinHit(int nodeId, int slot) {
        boolean isOutput() {
            return slot < 0;
        }
    }

    private record Wire(int toId, int slot, int fromId) {
    }

    private record Snapshot(ProgramGraph graph, Map<Integer, NodePos> positions) {
    }

    private ProgramGraph graph;
    private final Map<Integer, NodePos> positions;
    private boolean dirty;
    private boolean hadCard;
    private boolean framed;

    private final Deque<Snapshot> undo = new ArrayDeque<>();
    private final Deque<Snapshot> redo = new ArrayDeque<>();
    @Nullable
    private Snapshot clipboard;

    private double cameraX;
    private double cameraY;
    private double zoom = 1.0;

    private final Set<Integer> selection = new LinkedHashSet<>();
    private DragMode dragMode = DragMode.NONE;
    private double pressX, pressY;
    private double panCameraX, panCameraY;
    private boolean moved;
    private final Map<Integer, double[]> dragOffsets = new HashMap<>();
    /** A press on a card's control (link, op, literal) that runs on release unless the press became a drag. */
    @Nullable
    private Runnable pressAction;
    @Nullable
    private PinHit wireFrom;
    private double boxStartX, boxStartY, boxEndX, boxEndY;
    private boolean boxAdditive;
    private double mouseX, mouseY;
    @Nullable
    private Wire hoveredWire;

    private boolean paletteOpen;
    private int paletteX, paletteY;
    private double paletteCanvasX, paletteCanvasY;
    /** The pin a wire was dropped from to open the palette, which filters it and gets wired to the new card. */
    @Nullable
    private PinHit palettePin;

    @Nullable
    private EditBox literalBox;
    private int literalNodeId;

    // The variable panel: a name box, a type for the next variable, and a rename in progress (-1 for none).
    private EditBox nameBox;
    private Button typeButton;
    private Button addButton;
    private ValueType newType = ValueType.NUMBER;
    private int renameIndex = -1;
    private String nameText = "";

    private Button printButton;
    private Button eraseButton;
    private Button stampButton;
    private Button saveFunctionButton;
    private Component status = Component.empty();
    private long statusUntil;

    /** Where the editor reads its program from and sends its edits to: a Programmer's menu, or a Computer's desktop. */
    interface Source {
        BlockPos pos();

        @Nullable
        ProgrammerBlockEntity programmer();

        /** The medium being edited (the programmer's slot), or empty. */
        ItemStack medium();
    }

    private final Source src;
    private final GuiSkin skin;
    private final net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
    private final Minecraft minecraft = Minecraft.getInstance();
    /** The rectangle the editor fills, in screen coordinates. */
    private int vx, vy, vw, vh;
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private EditBox focusBox;

    ProgramEditor(Source src, GuiSkin skin) {
        this.src = src;
        this.skin = skin;
        if (skin == GuiSkin.PANEL) {
            COLOR_BACKGROUND = 0xFF15191E;
            COLOR_GRID_MINOR = 0xFF1C2228;
            COLOR_GRID_MAJOR = 0xFF28303A;
            COLOR_NODE_BODY = 0xFF1C232B;
            COLOR_NODE_BORDER = 0xFF39434F;
            COLOR_SELECTED = 0xFF60A5FA;
            COLOR_BOX_FILL = 0x3060A5FA;
            COLOR_TEXT = 0xFFE6EDF3;
            COLOR_MUTED = 0xFFA7B2BD;
            COLOR_PALETTE = 0xFF202833;
            COLOR_PALETTE_HOVER = 0xFF2E3947;
        } else {
            // a themed desktop: the canvas takes the skin's phosphor, the card headers keep their colours so a kind still reads at a glance
            COLOR_BACKGROUND = TerminalLook.shade(skin.ink(), 0.04f);
            COLOR_GRID_MINOR = TerminalLook.shade(skin.ink(), 0.1f);
            COLOR_GRID_MAJOR = TerminalLook.shade(skin.ink(), 0.2f);
            COLOR_NODE_BODY = TerminalLook.shade(skin.ink(), 0.08f);
            COLOR_NODE_BORDER = TerminalLook.shade(skin.ink(), 0.45f);
            COLOR_SELECTED = skin.accent();
            COLOR_BOX_FILL = 0x30000000 | skin.accent() & 0xFFFFFF;
            COLOR_TEXT = skin.ink();
            COLOR_MUTED = skin.dim();
            COLOR_PALETTE = TerminalLook.shade(skin.ink(), 0.12f);
            COLOR_PALETTE_HOVER = TerminalLook.shade(skin.ink(), 0.25f);
        }
        ProgrammerBlockEntity programmer = src.programmer();
        graph = programmer != null ? programmer.getDisplayedGraph() : ProgramGraph.EMPTY;
        positions = new HashMap<>(programmer != null ? programmer.getPositions() : Map.of());
        hadCard = !src.medium().isEmpty();
    }

    /** Tells the editor the rectangle it fills; call whenever it changes, before {@link #render}. */
    void viewport(int x, int y, int w, int h) {
        boolean first = vw == 0;
        vx = x;
        vy = y;
        vw = w;
        vh = h;
        if (!framed && w > 0) {
            framed = true;
            frameAll();
        } else if (first) {
            frameAll();
        }
    }

    private <T extends AbstractWidget> T add(T widget) {
        widgets.add(widget);
        return widget;
    }

    /** Makes the editor's controls. Safe to call again; the typed state survives. */
    void build() {
        widgets.clear();
        printButton = add(GuiStyle.primary(skin, Component.translatable("feedback.programmer.print"), b -> print(),
                0, 0, 54, 16, FSounds.UI_PUNCH::get));
        eraseButton = add(GuiStyle.button(skin, Component.translatable("feedback.programmer.erase"),
                b -> PacketDistributor.sendToServer(new io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerActionPayload(
                        src.pos(), io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerActionPayload.ERASE, ProgramGraph.EMPTY)),
                0, 0, 54, 16));
        stampButton = add(GuiStyle.button(skin, Component.translatable("feedback.programmer.stamp"), b -> stampFunction(),
                0, 0, 54, 16));
        saveFunctionButton = add(GuiStyle.button(skin, Component.translatable("feedback.programmer.save_function"),
                b -> saveFunction(), 0, 0, 54, 16));
        nameBox = add(new EditBox(font, 0, 0, 62, 12, Component.empty()));
        nameBox.setMaxLength(FTuning.VARIABLE_NAME_MAX);
        nameBox.setValue(nameText);
        nameBox.setResponder(text -> nameText = text);
        typeButton = add(GuiStyle.button(skin, typeLabel(newType), b -> {
            newType = newType.next();
            b.setMessage(typeLabel(newType));
        }, 0, 0, 44, 14));
        addButton = add(GuiStyle.button(skin, addLabel(), b -> submitVariable(), 0, 0, 22, 14));
        if (literalBox != null)
            widgets.add(literalBox);
    }

    /** Puts the toolbar and the variables panel's controls where the rectangle now wants them. */
    private void layoutWidgets() {
        int y = vy + 3, right = vx + vw - 4;
        printButton.setPosition(right - 54, y);
        eraseButton.setPosition(right - 54 - 58, y);
        stampButton.setPosition(right - 54 - 58 * 2, y);
        saveFunctionButton.setPosition(right - 54 - 58 * 3, y);
        nameBox.setPosition(panelX() + 4, panelY() + 15);
        typeButton.setPosition(panelX() + 68, panelY() + 14);
        addButton.setPosition(panelX() + 114, panelY() + 14);
    }

    private void focus(@Nullable EditBox box) {
        if (focusBox != null && focusBox != box)
            focusBox.setFocused(false);
        focusBox = box;
        if (box != null)
            box.setFocused(true);
    }

    /** Whether a text box has the keys. */
    boolean typing() {
        return literalBox != null || nameBox.isFocused();
    }

    // --- state ----------------------------------------------------------------------------

    private ProgramGraph printedGraph() {
        return src.medium().getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY);
    }

    private boolean isPrinted() {
        return !printedGraph().nodes().isEmpty();
    }

    /** A printed card shows its own graph, read-only; otherwise the local working copy. */
    private ProgramGraph shown() {
        ProgramGraph printed = printedGraph();
        return printed.nodes().isEmpty() ? graph : printed;
    }

    private NodePos posOf(ProgramGraph g, int index) {
        ProgramNode node = g.nodes().get(index);
        NodePos pos = positions.get(node.id());
        // Cascaded so a card with no stored position (a printed card carries none) is never hidden under another.
        return pos != null ? pos : new NodePos(20 + (index % 2) * 120, 20 + (index / 2) * 50);
    }

    @Nullable
    private NodePos posById(ProgramGraph g, int nodeId) {
        for (int i = 0; i < g.nodes().size(); i++)
            if (g.nodes().get(i).id() == nodeId)
                return posOf(g, i);
        return null;
    }

    private static int nodeHeight(ProgramNode node) {
        // A seat's key and an analog part's input get a row of their own.
        int rows = node instanceof DelayNode ? 3
                : node instanceof SensorNode s && s.quantity() == Quantity.CONTROL
                || node instanceof ActuatorNode a && a.target() != null ? 2 : node.ports().size();
        return HEADER_H + Math.max(1, rows) * ROW_H + NODE_PAD;
    }

    private static int pinY(int slot) {
        return HEADER_H + ROW_H / 2 + 2 + Math.max(slot, 0) * ROW_H;
    }

    private static int rowTextY(int row) {
        return HEADER_H + 4 + row * ROW_H;
    }

    private int nextId() {
        int max = 0;
        for (ProgramNode node : graph.nodes())
            max = Math.max(max, node.id());
        return max + 1;
    }

    private Snapshot snapshot() {
        return new Snapshot(graph, Map.copyOf(positions));
    }

    /** Call before every mutation of {@link #graph}/{@link #positions}. */
    private void pushUndo() {
        undo.push(snapshot());
        while (undo.size() > MAX_HISTORY)
            undo.removeLast();
        redo.clear();
        dirty = true;
    }

    private void restore(Deque<Snapshot> from, Deque<Snapshot> to) {
        if (from.isEmpty())
            return;
        to.push(snapshot());
        Snapshot s = from.pop();
        graph = s.graph();
        positions.clear();
        positions.putAll(s.positions());
        selection.removeIf(id -> graph.find(id).isEmpty());
        closeLiteralBox();
        endRename();
        dirty = true;
    }

    private void replaceNode(ProgramNode replacement) {
        graph = replaced(replacement);
    }

    private ProgramGraph replaced(ProgramNode replacement) {
        List<ProgramNode> nodes = new ArrayList<>(graph.nodes());
        nodes.replaceAll(n -> n.id() == replacement.id() ? replacement : n);
        return graph.withNodes(nodes);
    }

    /** Clears every port that {@link ProgramGraph#wireError} now refuses -- a relink can change a sensor's quantity under wires drawn against the old one. */
    private void pruneBadWires() {
        List<ProgramNode> nodes = new ArrayList<>(graph.nodes());
        for (int i = 0; i < nodes.size(); i++) {
            ProgramNode node = nodes.get(i);
            List<Integer> ports = node.ports();
            for (int slot = 0; slot < ports.size(); slot++) {
                Optional<ProgramNode> from = graph.find(ports.get(slot));
                if (from.isPresent() && graph.wireError(node, slot, from.get()).isPresent())
                    node = node.withInput(slot, ProgramNode.UNWIRED);
            }
            nodes.set(i, node);
        }
        graph = graph.withNodes(nodes);
    }

    private void setStatus(Component message) {
        status = message;
        statusUntil = Util.getMillis() + 4000;
    }

    private void setError(String key) {
        setStatus(Component.translatable("feedback.programmer.error." + key));
    }

    private void commit(boolean print) {
        PacketDistributor.sendToServer(new ProgrammerCommitPayload(src.pos(), graph, Map.copyOf(positions), print));
        dirty = false;
    }

    private void print() {
        commitLiteral();
        commit(true);
    }

    /**
     * Commit before closing, not in {@link #removed}: closing sends the close-container packet
     * first, and the server only takes a commit from a player whose menu is still this one.
     */
    void flush() {
        commitLiteral();
        if (dirty && !isPrinted())
            commit(false);
    }

    /** The server closed the menu (out of reach, block broken): an unsent edit is lost -- say so. */
    void lost() {
        if (dirty && !isPrinted() && minecraft.player != null)
            minecraft.player.displayClientMessage(Component.translatable("feedback.programmer.unsaved"), true);
    }

    /** Whether there is an unsent edit. */
    boolean dirty() {
        return dirty && !isPrinted();
    }

    /** Taking the card out discards the server's working copy ({@code ProgrammerBlockEntity#removeItem}); follow suit rather than resurrecting it on close. */
    void tick() {
        boolean hasCard = !src.medium().isEmpty();
        if (hadCard && !hasCard) {
            graph = ProgramGraph.EMPTY;
            positions.clear();
            undo.clear();
            redo.clear();
            selection.clear();
            closeLiteralBox();
            endRename();
            dirty = false;
        }
        hadCard = hasCard;
        printButton.active = !isPrinted();
        eraseButton.visible = isPrinted() && src.medium().is(io.github.soundgoodizerfan.feedback.registry.FTags.SOFT_CARDS);
        stampButton.active = !isPrinted() && heldFunction() != null;
        saveFunctionButton.active = !isPrinted() && !selection.isEmpty();
        nameBox.visible = typeButton.visible = addButton.visible = !isPrinted();

        // The block entity's sync can land after the menu opens, so until the first edit keep
        // following it rather than trusting the snapshot taken in the constructor.
        ProgrammerBlockEntity programmer = src.programmer();
        if (programmer != null && !dirty && undo.isEmpty() && !programmer.getDisplayedGraph().equals(graph)) {
            boolean wasEmpty = graph.nodes().isEmpty();
            graph = programmer.getDisplayedGraph();
            positions.clear();
            positions.putAll(programmer.getPositions());
            selection.clear();
            if (wasEmpty)
                frameAll();
        }
    }

    // --- camera -----------------------------------------------------------------------------

    private double centerY() {
        return vy + TOOL_H + (vh - TOOL_H) / 2.0;
    }

    private double toScreenX(double canvasX) {
        return (canvasX - cameraX) * zoom + vx + vw / 2.0;
    }

    private double toScreenY(double canvasY) {
        return (canvasY - cameraY) * zoom + centerY();
    }

    private double toCanvasX(double screenX) {
        return (screenX - vx - vw / 2.0) / zoom + cameraX;
    }

    private double toCanvasY(double screenY) {
        return (screenY - centerY()) / zoom + cameraY;
    }

    private void frameAll() {
        ProgramGraph g = shown();
        if (g.nodes().isEmpty()) {
            cameraX = NODE_W / 2.0;
            cameraY = 40;
            zoom = 1.0;
            return;
        }
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < g.nodes().size(); i++) {
            NodePos pos = posOf(g, i);
            minX = Math.min(minX, pos.x());
            minY = Math.min(minY, pos.y());
            maxX = Math.max(maxX, pos.x() + NODE_W);
            maxY = Math.max(maxY, pos.y() + nodeHeight(g.nodes().get(i)));
        }
        cameraX = (minX + maxX) / 2;
        cameraY = (minY + maxY) / 2;
        zoom = Mth.clamp(Math.min(vw / (maxX - minX + 80), (vh - TOOL_H) / (maxY - minY + 80)), MIN_ZOOM, 1.5);
    }

    // --- hit testing ------------------------------------------------------------------------

    @Nullable
    private PinHit findPin(double mx, double my) {
        ProgramGraph g = shown();
        double radius = Math.max(6, 5 * zoom);
        for (int i = g.nodes().size() - 1; i >= 0; i--) {
            ProgramNode node = g.nodes().get(i);
            NodePos pos = posOf(g, i);
            if (!(node instanceof ActuatorNode) && near(mx, my, pos.x() + NODE_W, pos.y() + pinY(-1), radius))
                return new PinHit(node.id(), -1);
            for (int slot = 0; slot < node.ports().size(); slot++)
                if (near(mx, my, pos.x(), pos.y() + pinY(slot), radius))
                    return new PinHit(node.id(), slot);
        }
        return null;
    }

    private boolean near(double mx, double my, double canvasX, double canvasY, double radius) {
        return Math.abs(mx - toScreenX(canvasX)) <= radius && Math.abs(my - toScreenY(canvasY)) <= radius;
    }

    /** Index into {@link #shown()}'s node list of the topmost card under the cursor, or -1. */
    private int findNode(double mx, double my) {
        ProgramGraph g = shown();
        double cx = toCanvasX(mx), cy = toCanvasY(my);
        for (int i = g.nodes().size() - 1; i >= 0; i--) {
            NodePos pos = posOf(g, i);
            if (cx >= pos.x() && cx <= pos.x() + NODE_W && cy >= pos.y() && cy <= pos.y() + nodeHeight(g.nodes().get(i)))
                return i;
        }
        return -1;
    }

    private List<Wire> wires(ProgramGraph g) {
        List<Wire> wires = new ArrayList<>();
        for (ProgramNode node : g.nodes()) {
            List<Integer> ports = node.ports();
            for (int slot = 0; slot < ports.size(); slot++)
                if (g.find(ports.get(slot)).isPresent())
                    wires.add(new Wire(node.id(), slot, ports.get(slot)));
        }
        return wires;
    }

    /** Screen-space {@code {x0, y0, x1, y1}} of a wire, output end first. */
    @Nullable
    private double[] wireEnds(ProgramGraph g, Wire wire) {
        NodePos from = posById(g, wire.fromId());
        NodePos to = posById(g, wire.toId());
        if (from == null || to == null)
            return null;
        return new double[]{toScreenX(from.x() + NODE_W), toScreenY(from.y() + pinY(-1)),
                toScreenX(to.x()), toScreenY(to.y() + pinY(wire.slot()))};
    }

    @Nullable
    private Wire findWire(double mx, double my) {
        ProgramGraph g = shown();
        Wire best = null;
        double bestDistance = WIRE_HIT;
        for (Wire wire : wires(g)) {
            double[] e = wireEnds(g, wire);
            if (e == null)
                continue;
            double previousX = e[0], previousY = e[1];
            for (int i = 1; i <= 24; i++) {
                double[] p = bezier(e, i / 24.0);
                double d = distanceToSegment(previousX, previousY, p[0], p[1], mx, my);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = wire;
                }
                previousX = p[0];
                previousY = p[1];
            }
        }
        return best;
    }

    private static double distanceToSegment(double x0, double y0, double x1, double y1, double px, double py) {
        double dx = x1 - x0, dy = y1 - y0;
        double lengthSquared = dx * dx + dy * dy;
        double t = lengthSquared == 0 ? 0 : Mth.clamp(((px - x0) * dx + (py - y0) * dy) / lengthSquared, 0, 1);
        return Math.hypot(px - (x0 + t * dx), py - (y0 + t * dy));
    }

    /** A point on the cubic from {@code e[0..1]} to {@code e[2..3]}, both ends leaving horizontally -- ports face sideways, so wires should too. */
    private double[] bezier(double[] e, double t) {
        double tangent = Mth.clamp(Math.abs(e[2] - e[0]) * 0.45, 30 * zoom, 120 * zoom);
        double c0x = e[0] + tangent, c1x = e[2] - tangent;
        double u = 1 - t;
        double x = u * u * u * e[0] + 3 * u * u * t * c0x + 3 * u * t * t * c1x + t * t * t * e[2];
        double y = u * u * u * e[1] + 3 * u * u * t * e[1] + 3 * u * t * t * e[3] + t * t * t * e[3];
        return new double[]{x, y};
    }

    // --- editing ----------------------------------------------------------------------------

    /** Wires {@code fromId}'s output into {@code toId}'s port {@code slot}, refusing anything {@link ProgramGraph#findError} would refuse at print. */
    private void connect(int toId, int slot, int fromId) {
        ProgramNode to = graph.find(toId).orElse(null);
        ProgramNode from = graph.find(fromId).orElse(null);
        if (to == null || from == null || toId == fromId)
            return;
        Optional<String> error = graph.wireError(to, slot, from);
        if (error.isPresent()) {
            setError(error.get());
            return;
        }
        ProgramGraph next = replaced(to.withInput(slot, fromId));
        if (ProgramEvaluator.topologicalOrder(next).isEmpty()) {
            setError("cycle");
            return;
        }
        pushUndo();
        graph = next;
        GuiSkin.play(FSounds.UI_DETENT.get(), 1.2f); // the lead plugged in
    }

    private void cutPin(PinHit pin) {
        pushUndo();
        GuiSkin.play(FSounds.UI_DETENT.get(), 0.7f);
        if (!pin.isOutput()) {
            graph.find(pin.nodeId()).ifPresent(n -> replaceNode(n.withInput(pin.slot(), ProgramNode.UNWIRED)));
            return;
        }
        unwireFrom(Set.of(pin.nodeId()));
    }

    private void unwireFrom(Set<Integer> fromIds) {
        List<ProgramNode> nodes = new ArrayList<>(graph.nodes());
        for (int i = 0; i < nodes.size(); i++) {
            ProgramNode node = nodes.get(i);
            List<Integer> ports = node.ports();
            for (int slot = 0; slot < ports.size(); slot++)
                if (fromIds.contains(ports.get(slot)))
                    node = node.withInput(slot, ProgramNode.UNWIRED);
            nodes.set(i, node);
        }
        graph = graph.withNodes(nodes);
    }

    private void deleteSelection() {
        pushUndo();
        if (!selection.isEmpty())
            GuiSkin.play(FSounds.UI_DETENT.get(), 0.7f);
        graph = graph.withNodes(graph.nodes().stream().filter(n -> !selection.contains(n.id())).toList());
        unwireFrom(selection);
        selection.forEach(positions::remove);
        selection.forEach(pendingLinks::remove);
        selection.clear();
        closeLiteralBox();
    }

    private void copySelection() {
        List<ProgramNode> nodes = graph.nodes().stream().filter(n -> selection.contains(n.id())).toList();
        if (nodes.isEmpty())
            return;
        Map<Integer, NodePos> copied = new HashMap<>();
        for (int i = 0; i < graph.nodes().size(); i++)
            if (selection.contains(graph.nodes().get(i).id()))
                copied.put(graph.nodes().get(i).id(), posOf(graph, i));
        clipboard = new Snapshot(new ProgramGraph(nodes), copied);
    }

    /** The program graph on a Function Card in either hand, or null. */
    @Nullable
    private ProgramGraph heldFunction() {
        if (minecraft == null || minecraft.player == null)
            return null;
        for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
            var held = minecraft.player.getItemInHand(hand);
            if (held.is(io.github.soundgoodizerfan.feedback.registry.FItems.FUNCTION_CARD.get())) {
                ProgramGraph g = held.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY);
                if (!g.nodes().isEmpty())
                    return g;
            }
        }
        return null;
    }

    /** Stamps the held Function Card into the working copy as plain cards, at the cursor; its inputs stay unwired for the player to wire. */
    private void stampFunction() {
        ProgramGraph fn = heldFunction();
        if (fn == null)
            return;
        Map<Integer, NodePos> laid = new HashMap<>();
        for (int i = 0; i < fn.nodes().size(); i++)
            laid.put(fn.nodes().get(i).id(), posOf(fn, i));
        List<io.github.soundgoodizerfan.feedback.control.program.ProgramVariable> merged = new ArrayList<>(graph.variables());
        for (var v : fn.variables())
            if (merged.stream().noneMatch(m -> m.name().equals(v.name())) && merged.size() < ControllerLimits.MAX.variables())
                merged.add(v);
        graph = new ProgramGraph(graph.nodes(), merged);
        clipboard = new Snapshot(fn, laid);
        paste();
    }

    /** Sends the selected cards to a blank Function Card in hand; wires from outside the selection are cut so the template stands alone. */
    private void saveFunction() {
        List<ProgramNode> chosen = graph.nodes().stream().filter(n -> selection.contains(n.id())).toList();
        if (chosen.isEmpty())
            return;
        List<ProgramNode> alone = new ArrayList<>();
        for (ProgramNode n : chosen) {
            ProgramNode copy = n;
            List<Integer> ports = n.ports();
            for (int slot = 0; slot < ports.size(); slot++)
                if (ports.get(slot) != ProgramNode.UNWIRED && !selection.contains(ports.get(slot)))
                    copy = copy.withInput(slot, ProgramNode.UNWIRED);
            alone.add(copy);
        }
        java.util.Set<String> used = new java.util.HashSet<>();
        for (ProgramNode n : alone) {
            if (n instanceof io.github.soundgoodizerfan.feedback.control.program.ReadNode r)
                used.add(r.variable());
            if (n instanceof io.github.soundgoodizerfan.feedback.control.program.WriteNode w)
                used.add(w.variable());
        }
        List<io.github.soundgoodizerfan.feedback.control.program.ProgramVariable> vars = graph.variables().stream()
                .filter(v -> used.contains(v.name())).toList();
        PacketDistributor.sendToServer(new io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerActionPayload(src.pos(),
                io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerActionPayload.SAVE_FUNCTION, new ProgramGraph(alone, vars)));
    }

    /** Pastes {@link #clipboard} at the cursor with fresh ids. Wires between pasted cards survive; wires from outside the copy don't. */
    private void paste() {
        if (clipboard == null)
            return;
        if (graph.nodes().size() + clipboard.graph().nodes().size() > ControllerLimits.MAX.nodes()) {
            setError("too_many_nodes");
            return;
        }
        int minX = clipboard.positions().values().stream().mapToInt(NodePos::x).min().orElse(0);
        int minY = clipboard.positions().values().stream().mapToInt(NodePos::y).min().orElse(0);
        int offsetX = (int) toCanvasX(mouseX) - minX, offsetY = (int) toCanvasY(mouseY) - minY;

        Map<Integer, Integer> remap = new HashMap<>();
        int id = nextId();
        for (ProgramNode node : clipboard.graph().nodes())
            remap.put(node.id(), id++);

        pushUndo();
        List<ProgramNode> nodes = new ArrayList<>(graph.nodes());
        selection.clear();
        for (ProgramNode source : clipboard.graph().nodes()) {
            ProgramNode copy = source.withId(remap.get(source.id()));
            List<Integer> ports = source.ports();
            for (int slot = 0; slot < ports.size(); slot++)
                copy = copy.withInput(slot, remap.getOrDefault(ports.get(slot), ProgramNode.UNWIRED));
            nodes.add(copy);
            NodePos pos = clipboard.positions().get(source.id());
            positions.put(copy.id(), new NodePos(pos.x() + offsetX, pos.y() + offsetY));
            selection.add(copy.id());
        }
        graph = graph.withNodes(nodes);
    }

    /** Whether a fresh {@code kind} card could attach to {@link #palettePin}, so the palette offers only what would wire up. */
    private boolean canAttach(Kind kind) {
        if (palettePin == null)
            return true;
        ProgramNode pinNode = graph.find(palettePin.nodeId()).orElse(null);
        if (pinNode == null)
            return true;
        ProgramNode candidate = kind.create(Integer.MIN_VALUE, firstVariable());
        if (palettePin.isOutput())
            return attachSlot(candidate, pinNode) >= 0;
        return kind != Kind.ACTUATOR && kind != Kind.WRITE && graph.wireError(pinNode, palettePin.slot(), candidate).isEmpty();
    }

    /** The first free port on {@code target} that {@code from} may feed, or -1. */
    private int attachSlot(ProgramNode target, ProgramNode from) {
        List<Integer> ports = target.ports();
        for (int slot = 0; slot < ports.size(); slot++)
            if (ports.get(slot) == ProgramNode.UNWIRED && graph.wireError(target, slot, from).isEmpty())
                return slot;
        return -1;
    }

    private List<Kind> paletteKinds() {
        List<Kind> kinds = new ArrayList<>();
        for (Kind kind : Kind.values())
            if (canAttach(kind))
                kinds.add(kind);
        return kinds;
    }

    private void addNode(Kind kind) {
        if (graph.nodes().size() >= ControllerLimits.MAX.nodes()) {
            setError("too_many_nodes");
            return;
        }
        pushUndo();
        ProgramNode node = kind.create(nextId(), firstVariable());
        List<ProgramNode> nodes = new ArrayList<>(graph.nodes());
        nodes.add(node);
        graph = graph.withNodes(nodes);
        positions.put(node.id(), new NodePos((int) paletteCanvasX, (int) paletteCanvasY));

        ProgramNode pinNode = palettePin != null ? graph.find(palettePin.nodeId()).orElse(null) : null;
        if (pinNode != null) {
            if (palettePin.isOutput()) {
                int slot = attachSlot(node, pinNode);
                if (slot >= 0)
                    replaceNode(node.withInput(slot, pinNode.id()));
            } else {
                replaceNode(pinNode.withInput(palettePin.slot(), node.id()));
            }
        }
        selection.clear();
        selection.add(node.id());
        GuiSkin.play(FSounds.UI_TOGGLE.get(), 1.2f);

        // The ordinary order is select in the world, then place the card -- so placing tries the link at once.
        if (node instanceof SensorNode || node instanceof ActuatorNode)
            requestLink(node, true);
    }

    /**
     * The latest request per card. Node ids are reused, so a reply is applied only if it answers
     * the newest request for that id -- a late reply to a deleted card must not land on its successor.
     */
    private final Map<Integer, Integer> pendingLinks = new HashMap<>();
    private int nextLinkToken;

    private void requestLink(ProgramNode node, boolean quiet) {
        int token = ++nextLinkToken;
        pendingLinks.put(node.id(), token);
        PacketDistributor.sendToServer(new ProgrammerLinkPayload(src.pos(), node.id(), node instanceof ActuatorNode, quiet, token));
    }

    void applyLink(ProgrammerLinkedPayload payload) {
        if (!Integer.valueOf(payload.token()).equals(pendingLinks.get(payload.nodeId())))
            return;
        pendingLinks.remove(payload.nodeId());
        ProgramNode node = graph.find(payload.nodeId()).orElse(null);
        if (node == null || isPrinted())
            return;
        pushUndo();
        replaceNode(node instanceof ActuatorNode a
                ? new ActuatorNode(a.id(), a.input(), payload.target(), payload.channel())
                : new SensorNode(node.id(), payload.target(), payload.quantity().orElse(null), payload.channel()));
        pruneBadWires();
    }

    private void toggleOp(ComparatorNode n) {
        pushUndo();
        replaceNode(new ComparatorNode(n.id(), n.op() == ComparatorNode.Compare.GREATER
                ? ComparatorNode.Compare.LESS : ComparatorNode.Compare.GREATER,
                n.leftInput(), n.rightInput(), n.literalDefault()));
    }

    /** What a click (not a drag) at canvas-local {@code lx, ly} on {@code node} does, or null if that spot is only a drag handle. */
    @Nullable
    private Runnable controlAt(ProgramNode node, double lx, double ly) {
        int row = ly < HEADER_H ? -1 : (int) ((ly - HEADER_H) / ROW_H);
        Runnable control = controlAt0(node, row, lx);
        return control != null ? control : extraControl(node, row);
    }

    @Nullable
    private Runnable controlAt0(ProgramNode node, int row, double lx) {
        return switch (node) {
            case SensorNode n -> row == 0 ? () -> requestLink(n, false) : null;
            case ActuatorNode n -> row == 0 && lx > 6 ? () -> requestLink(n, false) : null;
            case ComparatorNode n -> row == 0 && Math.abs(lx - NODE_W / 2.0) <= 9 ? () -> toggleOp(n)
                    : row == 1 && lx > 6 && n.rightInput() == ProgramNode.UNWIRED ? () -> openLiteralBox(n) : null;
            case LogicNode n -> row == 0 && Math.abs(lx - NODE_W / 2.0) <= 12 ? () -> cycleOp(n) : null;
            case DelayNode n -> row == 0 && Math.abs(lx - NODE_W / 2.0) <= 18 ? () -> toggleType(n)
                    : row == 1 && lx > 6 ? () -> cycleLength(n)
                    : row == 2 && lx > 6 ? () -> openLiteralBox(n) : null;
            case ReadNode n -> row == 0 && lx > 6 ? () -> cycleVariable(n) : null;
            case WriteNode n -> row == 0 && lx > 6 ? () -> cycleVariable(n) : null;
            case MathNode n -> row == 0 && Math.abs(lx - NODE_W / 2.0) <= 9 ? () -> cycleOp(n)
                    : row == 1 && lx > 6 && n.rightInput() == ProgramNode.UNWIRED ? () -> openLiteralBox(n) : null;
        };
    }

    /** The second row of a seat's Sensor card (which key) and an analog Actuator card (which input). */
    @Nullable
    private Runnable extraControl(ProgramNode node, int row) {
        if (row != 1)
            return null;
        if (node instanceof SensorNode s && s.quantity() == Quantity.CONTROL)
            return () -> awaitingKey = s.id();
        if (node instanceof ActuatorNode a && channelKeys(a).length > 1)
            return () -> {
                pushUndo();
                replaceNode(a.withChannel((a.channel() + 1) % channelKeys(a).length));
            };
        return null;
    }

    /** A Sensor card waiting for the next key pressed to read it from its seat; -1 for none. */
    private int awaitingKey = -1;

    /** The inputs of the analog part an actuator card names, looked up in the client's own world. */
    private static String[] channelKeys(ActuatorNode node) {
        var level = Minecraft.getInstance().level;
        if (node.target() == null || level == null)
            return new String[0];
        var be = level.getBlockEntity(node.target().linkedAt());
        return be instanceof Analog analog ? analog.channelKeys()
                : be instanceof io.github.soundgoodizerfan.feedback.control.Steppable stepper ? stepper.channelKeys() : new String[0];
    }

    private void cycleOp(LogicNode n) {
        pushUndo();
        LogicNode.Gate op = n.op().next();
        replaceNode(new LogicNode(n.id(), op, n.leftInput(), op.unary() ? ProgramNode.UNWIRED : n.rightInput()));
    }

    /** A Delay's type decides which wires it accepts, so wires the new type refuses are cut. */
    private void toggleType(DelayNode n) {
        pushUndo();
        replaceNode(new DelayNode(n.id(), n.input(), n.type().next(), n.ticks()));
        pruneBadWires();
    }

    private void cycleLength(DelayNode n) {
        int[] named = FTuning.DELAY_NAMED_TICKS;
        int next = 0;
        for (int i = 0; i < named.length; i++)
            if (named[i] == n.ticks())
                next = (i + 1) % named.length;
        pushUndo();
        replaceNode(new DelayNode(n.id(), n.input(), n.type(), named[next]));
    }

    /** Click steps a Read or Write card through the declared variables; its pin types follow, so refused wires are cut. */
    private void cycleVariable(ProgramNode n) {
        List<ProgramVariable> vars = graph.variables();
        if (vars.isEmpty()) {
            setError("no_variables");
            return;
        }
        String current = n instanceof ReadNode r ? r.variable() : ((WriteNode) n).variable();
        int next = 0;
        for (int i = 0; i < vars.size(); i++)
            if (vars.get(i).name().equals(current))
                next = (i + 1) % vars.size();
        String name = vars.get(next).name();
        pushUndo();
        replaceNode(n instanceof ReadNode r ? new ReadNode(r.id(), name)
                : new WriteNode(n.id(), name, ((WriteNode) n).valueInput(), ((WriteNode) n).triggerInput()));
        pruneBadWires();
    }

    private String firstVariable() {
        return graph.variables().isEmpty() ? "" : graph.variables().get(0).name();
    }

    // --- variables --------------------------------------------------------------------------

    private int panelY() {
        return vy + TOOL_H + 6;
    }

    private int panelListY() {
        return panelY() + 32;
    }

    private int panelX() {
        return vx + vw - PANEL_W - 6;
    }

    private Component addLabel() {
        return Component.translatable(renameIndex >= 0 ? "feedback.programmer.variable.ok" : "feedback.programmer.variable.add");
    }

    private static Component typeLabel(ValueType type) {
        return Component.translatable("feedback.programmer.type." + type.getSerializedName());
    }

    private boolean panelContains(double mx, double my) {
        return mx >= panelX() && mx < panelX() + PANEL_W && my >= panelY()
                && my < panelListY() + Math.max(1, shown().variables().size()) * PANEL_ROW_H + 4;
    }

    private void endRename() {
        renameIndex = -1;
        nameBox.setValue("");
        addButton.setMessage(addLabel());
    }

    private void setVariables(List<ProgramVariable> vars) {
        graph = new ProgramGraph(graph.nodes(), vars);
    }

    /** Adds the typed variable, or renames the one picked; a rename follows into every Read and Write card that names it. */
    private void submitVariable() {
        String name = nameBox.getValue().trim();
        List<ProgramVariable> vars = new ArrayList<>(graph.variables());
        int at = renameIndex < vars.size() ? renameIndex : -1;
        if (!ProgramVariable.validName(name)) {
            setError("bad_variable_name");
            return;
        }
        for (int i = 0; i < vars.size(); i++)
            if (i != at && vars.get(i).name().equals(name)) {
                setError("duplicate_variable");
                return;
            }
        if (at < 0 && vars.size() >= ControllerLimits.MAX.variables()) {
            setError("too_many_variables");
            return;
        }
        pushUndo();
        if (at < 0) {
            vars.add(new ProgramVariable(name, newType));
        } else {
            String old = vars.get(at).name();
            vars.set(at, new ProgramVariable(name, vars.get(at).type()));
            List<ProgramNode> nodes = new ArrayList<>(graph.nodes());
            nodes.replaceAll(n -> n instanceof ReadNode r && r.variable().equals(old) ? new ReadNode(r.id(), name)
                    : n instanceof WriteNode w && w.variable().equals(old)
                    ? new WriteNode(w.id(), name, w.valueInput(), w.triggerInput()) : n);
            graph = graph.withNodes(nodes);
        }
        setVariables(vars);
        endRename();
        focus(null);
    }

    private void toggleVariableType(int index) {
        pushUndo();
        List<ProgramVariable> vars = new ArrayList<>(graph.variables());
        vars.set(index, new ProgramVariable(vars.get(index).name(), vars.get(index).type().next()));
        setVariables(vars);
        pruneBadWires();
    }

    /** Refuses while a Read or Write card still names the variable: the player picks what those cards read instead. */
    private void deleteVariable(int index) {
        String name = graph.variables().get(index).name();
        if (graph.nodes().stream().anyMatch(n -> n instanceof ReadNode r && r.variable().equals(name)
                || n instanceof WriteNode w && w.variable().equals(name))) {
            setError("variable_in_use");
            return;
        }
        pushUndo();
        List<ProgramVariable> vars = new ArrayList<>(graph.variables());
        vars.remove(index);
        setVariables(vars);
        endRename();
    }

    private boolean clickPanel(double mx, double my, int button) {
        int row = (int) Math.floor((my - panelListY()) / PANEL_ROW_H);
        if (isPrinted() || my < panelListY() || row >= graph.variables().size() || button != GLFW.GLFW_MOUSE_BUTTON_LEFT)
            return true;
        double lx = mx - panelX();
        if (lx >= PANEL_W - 14)
            deleteVariable(row);
        else if (lx >= 82)
            toggleVariableType(row);
        else {
            renameIndex = row;
            nameBox.setValue(graph.variables().get(row).name());
            addButton.setMessage(addLabel());
            focus(nameBox);
        }
        GuiSkin.play(FSounds.UI_DETENT.get(), 1f);
        return true;
    }

    private void cycleOp(MathNode n) {
        pushUndo();
        replaceNode(new MathNode(n.id(), n.op().next(), n.leftInput(), n.rightInput(), n.literalDefault()));
    }

    private static float literalOf(ProgramNode node) {
        return node instanceof DelayNode d ? d.ticks() : node instanceof MathNode m ? m.literalDefault() : node instanceof ComparatorNode c ? c.literalDefault() : 0f;
    }

    private void openLiteralBox(ProgramNode node) {
        commitLiteral();
        literalBox = new EditBox(font, 0, 0, 48, 12, Component.empty());
        literalBox.setValue(node instanceof DelayNode d ? String.valueOf(d.ticks()) : String.valueOf(literalOf(node)));
        literalNodeId = node.id();
        widgets.add(literalBox);
        focus(literalBox);
    }

    private void commitLiteral() {
        if (literalBox == null)
            return;
        try {
            if (graph.find(literalNodeId).orElse(null) instanceof DelayNode d) {
                int ticks = Integer.parseInt(literalBox.getValue().trim());
                if (ticks < FTuning.DELAY_TYPED_MIN_TICKS || ticks > FTuning.DELAY_MAX_TICKS)
                    setError("bad_delay");
                else if (ticks != d.ticks()) {
                    pushUndo();
                    replaceNode(new DelayNode(d.id(), d.input(), d.type(), ticks));
                }
                closeLiteralBox();
                return;
            }
            float value = Float.parseFloat(literalBox.getValue());
            // NaN and Infinity parse, and compare as nothing a reading can ever satisfy.
            if (Float.isFinite(value) && graph.find(literalNodeId).orElse(null) instanceof ComparatorNode n
                    && n.literalDefault() != value) {
                pushUndo();
                replaceNode(new ComparatorNode(n.id(), n.op(), n.leftInput(), n.rightInput(), value));
            } else if (Float.isFinite(value) && graph.find(literalNodeId).orElse(null) instanceof MathNode n
                    && n.literalDefault() != value) {
                pushUndo();
                replaceNode(new MathNode(n.id(), n.op(), n.leftInput(), n.rightInput(), value));
            }
        } catch (NumberFormatException ignored) {
            // leave the literal as it was
        }
        closeLiteralBox();
    }

    private void closeLiteralBox() {
        if (literalBox == null)
            return;
        widgets.remove(literalBox);
        if (focusBox == literalBox)
            focusBox = null;
        literalBox = null;
    }

    private void openPalette(double mx, double my, @Nullable PinHit pin) {
        palettePin = pin;
        paletteCanvasX = toCanvasX(mx);
        paletteCanvasY = toCanvasY(my);
        int h = PALETTE_HEADER_H + paletteKinds().size() * PALETTE_ROW_H + 4;
        paletteX = (int) Math.min(mx, vx + vw - PALETTE_W - 4);
        paletteY = (int) Math.min(my, vy + vh - h - 4);
        paletteOpen = true;
    }

    // --- input ------------------------------------------------------------------------------

    boolean mouseClicked(double mx, double my, int button) {
        if (paletteOpen) {
            int index = (int) Math.floor((my - paletteY - PALETTE_HEADER_H) / PALETTE_ROW_H);
            List<Kind> kinds = paletteKinds();
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && mx >= paletteX && mx <= paletteX + PALETTE_W
                    && index >= 0 && index < kinds.size())
                addNode(kinds.get(index));
            paletteOpen = false;
            palettePin = null;
            return true;
        }
        if (literalBox != null) {
            if (literalBox.isMouseOver(mx, my))
                return literalBox.mouseClicked(mx, my, button);
            commitLiteral();
        }
        for (int i = widgets.size() - 1; i >= 0; i--) {
            AbstractWidget wd = widgets.get(i);
            if (wd != literalBox && wd.visible && wd.active && wd.isMouseOver(mx, my) && wd.mouseClicked(mx, my, button)) {
                focus(wd instanceof EditBox box ? box : null);
                return true;
            }
        }
        if (panelContains(mx, my))
            return clickPanel(mx, my, button);
        if (nameBox.isFocused())
            focus(null);
        // the toolbar is widgets only
        if (my < vy + TOOL_H || my >= vy + vh || mx < vx || mx >= vx + vw)
            return my >= vy && my < vy + vh && mx >= vx && mx < vx + vw;

        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            dragMode = DragMode.PAN;
            moved = false;
            pressX = mx;
            pressY = my;
            panCameraX = cameraX;
            panCameraY = cameraY;
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT)
            return true;

        boolean printed = isPrinted();
        PinHit pin = findPin(mx, my);
        if (pin != null) {
            if (printed)
                return true;
            if (Screen.hasAltDown())
                cutPin(pin);
            else {
                dragMode = DragMode.WIRE;
                wireFrom = pin;
            }
            return true;
        }

        int index = findNode(mx, my);
        if (index < 0) {
            Wire wire = !printed && Screen.hasAltDown() ? findWire(mx, my) : null;
            if (wire != null) {
                cutPin(new PinHit(wire.toId(), wire.slot()));
                return true;
            }
            dragMode = DragMode.BOX;
            boxAdditive = Screen.hasShiftDown();
            boxStartX = boxEndX = toCanvasX(mx);
            boxStartY = boxEndY = toCanvasY(my);
            return true;
        }

        ProgramGraph g = shown();
        ProgramNode node = g.nodes().get(index);
        boolean additive = Screen.hasShiftDown() || Screen.hasControlDown();
        if (additive) {
            if (!selection.remove(node.id()))
                selection.add(node.id());
        } else if (!selection.contains(node.id())) {
            selection.clear();
            selection.add(node.id());
        }
        NodePos pos = posOf(g, index);
        double cx = toCanvasX(mx), cy = toCanvasY(my);
        pressAction = additive || printed ? null : controlAt(node, cx - pos.x(), cy - pos.y());
        dragMode = DragMode.NODES;
        moved = false;
        pressX = mx;
        pressY = my;
        dragOffsets.clear();
        for (int i = 0; i < g.nodes().size(); i++)
            if (selection.contains(g.nodes().get(i).id())) {
                NodePos p = posOf(g, i);
                dragOffsets.put(g.nodes().get(i).id(), new double[]{p.x() - cx, p.y() - cy});
            }
        return true;
    }

    boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        mouseX = mx;
        mouseY = my;
        if (Math.abs(mx - pressX) + Math.abs(my - pressY) > DRAG_THRESHOLD && !moved) {
            moved = true;
            pressAction = null;
            if (dragMode == DragMode.NODES && !isPrinted())
                pushUndo();
        }
        switch (dragMode) {
            case PAN -> {
                cameraX = panCameraX - (mx - pressX) / zoom;
                cameraY = panCameraY - (my - pressY) / zoom;
            }
            case NODES -> {
                if (moved && !isPrinted()) {
                    double cx = toCanvasX(mx), cy = toCanvasY(my);
                    dragOffsets.forEach((id, offset) ->
                            positions.put(id, new NodePos((int) Math.round(cx + offset[0]), (int) Math.round(cy + offset[1]))));
                }
            }
            case BOX -> {
                boxEndX = toCanvasX(mx);
                boxEndY = toCanvasY(my);
            }
            case WIRE -> {
            }
            case NONE -> {
                return false;
            }
        }
        return true;
    }

    boolean mouseReleased(double mx, double my, int button) {
        DragMode mode = dragMode;
        dragMode = DragMode.NONE;
        switch (mode) {
            case PAN -> {
                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && !moved && !isPrinted())
                    openPalette(mx, my, null);
            }
            case NODES -> {
                if (pressAction != null)
                    pressAction.run();
                pressAction = null;
            }
            case WIRE -> finishWire(mx, my);
            case BOX -> finishBox();
            case NONE -> {
                return false;
            }
        }
        return true;
    }

    private void finishWire(double mx, double my) {
        PinHit from = wireFrom;
        wireFrom = null;
        if (from == null)
            return;
        PinHit target = findPin(mx, my);
        if (target == null) {
            if (findNode(mx, my) < 0)
                openPalette(mx, my, from);
            return;
        }
        if (from.isOutput() && !target.isOutput())
            connect(target.nodeId(), target.slot(), from.nodeId());
        else if (!from.isOutput() && target.isOutput())
            connect(from.nodeId(), from.slot(), target.nodeId());
    }

    private void finishBox() {
        if (!moved) {
            if (!boxAdditive)
                selection.clear();
            return;
        }
        double minX = Math.min(boxStartX, boxEndX), maxX = Math.max(boxStartX, boxEndX);
        double minY = Math.min(boxStartY, boxEndY), maxY = Math.max(boxStartY, boxEndY);
        if (!boxAdditive)
            selection.clear();
        ProgramGraph g = shown();
        for (int i = 0; i < g.nodes().size(); i++) {
            NodePos pos = posOf(g, i);
            if (pos.x() < maxX && pos.x() + NODE_W > minX && pos.y() < maxY && pos.y() + nodeHeight(g.nodes().get(i)) > minY)
                selection.add(g.nodes().get(i).id());
        }
    }

    boolean mouseScrolled(double mx, double my, double scrollY) {
        if (my < vy + TOOL_H || scrollY == 0)
            return false;
        double focusX = toCanvasX(mx), focusY = toCanvasY(my);
        zoom = Mth.clamp(zoom * Math.pow(ZOOM_STEP, scrollY), MIN_ZOOM, MAX_ZOOM);
        cameraX = focusX - (mx - vx - vw / 2.0) / zoom;
        cameraY = focusY - (my - centerY()) / zoom;
        return true;
    }

    void mouseMoved(double mx, double my) {
        mouseX = mx;
        mouseY = my;
        hoveredWire = dragMode == DragMode.NONE && my >= vy + TOOL_H && findPin(mx, my) == null && findNode(mx, my) < 0
                ? findWire(mx, my) : null;
    }

    boolean keyPressed(int key, int scanCode, int modifiers) {
        // While typing a literal, every key is the box's -- the inventory key must not close the screen.
        if (literalBox != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE)
                closeLiteralBox();
            else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)
                commitLiteral();
            else
                literalBox.keyPressed(key, scanCode, modifiers);
            return true;
        }
        if (nameBox.isFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                endRename();
                focus(null);
            } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)
                submitVariable();
            else
                nameBox.keyPressed(key, scanCode, modifiers);
            return true;
        }
        // A seat's Sensor card takes the next key as the one it reads; Escape cancels.
        if (awaitingKey >= 0) {
            if (key != GLFW.GLFW_KEY_ESCAPE && graph.find(awaitingKey).orElse(null) instanceof SensorNode n) {
                pushUndo();
                replaceNode(n.withChannel(key));
            }
            awaitingKey = -1;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && paletteOpen) {
            paletteOpen = false;
            palettePin = null;
            return true;
        }
        if (key == GLFW.GLFW_KEY_HOME) {
            frameAll();
            return true;
        }
        // Read-only once printed: copying out of a printed card would be a second way to author
        // the next one, which spec §4's hard-rewrite tier doesn't offer.
        if (!isPrinted()) {
            if (Screen.hasControlDown() && key == GLFW.GLFW_KEY_Z) {
                if (Screen.hasShiftDown())
                    restore(redo, undo);
                else
                    restore(undo, redo);
                return true;
            }
            if (Screen.hasControlDown() && key == GLFW.GLFW_KEY_Y) {
                restore(redo, undo);
                return true;
            }
            if (Screen.hasControlDown() && key == GLFW.GLFW_KEY_C) {
                copySelection();
                return true;
            }
            if (Screen.hasControlDown() && key == GLFW.GLFW_KEY_V) {
                paste();
                return true;
            }
            if ((key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) && !selection.isEmpty()) {
                deleteSelection();
                return true;
            }
        }
        return false;
    }

    boolean charTyped(char codePoint, int modifiers) {
        return literalBox != null ? literalBox.charTyped(codePoint, modifiers)
                : nameBox.isFocused() && nameBox.charTyped(codePoint, modifiers);
    }

    // --- rendering --------------------------------------------------------------------------

    void render(GuiGraphics graphics, int mx, int my, float partialTick) {
        mouseMoved(mx, my);
        layoutWidgets();
        placeLiteralBox();
        renderCanvas(graphics, mx, my);
        skin.plaque(graphics, vx, vy, vw, TOOL_H);
        if (isPrinted())
            graphics.drawString(font, Component.translatable("feedback.programmer.printed_label"), vx + 8, vy + 7, COLOR_ERROR, false);
        for (AbstractWidget widget : widgets)
            if (widget.visible)
                widget.render(graphics, mx, my, partialTick);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        renderHud(graphics, mx, my);
        renderPalette(graphics, mx, my);
        renderCanvasTooltip(graphics, mx, my);
        graphics.pose().popPose();
    }

    private void placeLiteralBox() {
        if (literalBox == null)
            return;
        ProgramGraph g = shown();
        NodePos pos = posById(g, literalNodeId);
        if (pos == null) {
            closeLiteralBox();
            return;
        }
        literalBox.setX((int) toScreenX(pos.x() + 8));
        literalBox.setY((int) toScreenY(pos.y() + rowTextY(graph.find(literalNodeId).orElse(null) instanceof DelayNode ? 2 : 1)) - 2);
    }

    private void renderCanvas(GuiGraphics graphics, int mx, int my) {
        graphics.fill(vx, vy, vx + vw, vy + vh, COLOR_BACKGROUND);

        graphics.enableScissor(vx, vy + TOOL_H, vx + vw, vy + vh);
        renderGrid(graphics);
        ProgramGraph g = shown();
        for (Wire wire : wires(g)) {
            double[] e = wireEnds(g, wire);
            if (e != null)
                drawWire(graphics, e, wire.equals(hoveredWire) ? COLOR_WIRE_HOVER : outputColor(g, g.find(wire.fromId()).orElseThrow()));
        }
        renderWireDrag(graphics, g, mx, my);
        for (int i = 0; i < g.nodes().size(); i++)
            renderNode(graphics, g, i);
        if (dragMode == DragMode.BOX && moved) {
            int x0 = (int) toScreenX(Math.min(boxStartX, boxEndX)), x1 = (int) toScreenX(Math.max(boxStartX, boxEndX));
            int y0 = (int) toScreenY(Math.min(boxStartY, boxEndY)), y1 = (int) toScreenY(Math.max(boxStartY, boxEndY));
            graphics.fill(x0, y0, x1, y1, COLOR_BOX_FILL);
            graphics.renderOutline(x0, y0, x1 - x0, y1 - y0, COLOR_SELECTED);
        }
        graphics.disableScissor();
        renderVariables(graphics, mx, my);
    }

    private void renderGrid(GuiGraphics graphics) {
        double step = GRID_STEP;
        while (step * zoom < MIN_GRID_PIXELS)
            step *= 2;
        while (step * zoom >= MIN_GRID_PIXELS * 4)
            step /= 2;
        for (int gx = Mth.floor(toCanvasX(vx) / step); gx <= Mth.ceil(toCanvasX(vx + vw) / step); gx++) {
            int x = (int) Math.round(toScreenX(gx * step));
            graphics.fill(x, vy + TOOL_H, x + 1, vy + vh, Math.floorMod(gx, 5) == 0 ? COLOR_GRID_MAJOR : COLOR_GRID_MINOR);
        }
        for (int gy = Mth.floor(toCanvasY(vy + TOOL_H) / step); gy <= Mth.ceil(toCanvasY(vy + vh) / step); gy++) {
            int y = (int) Math.round(toScreenY(gy * step));
            graphics.fill(vx, y, vx + vw, y + 1, Math.floorMod(gy, 5) == 0 ? COLOR_GRID_MAJOR : COLOR_GRID_MINOR);
        }
    }

    private void renderWireDrag(GuiGraphics graphics, ProgramGraph g, int mx, int my) {
        if (dragMode != DragMode.WIRE || wireFrom == null)
            return;
        NodePos pos = posById(g, wireFrom.nodeId());
        ProgramNode node = g.find(wireFrom.nodeId()).orElse(null);
        if (pos == null || node == null)
            return;
        if (wireFrom.isOutput())
            drawWire(graphics, new double[]{toScreenX(pos.x() + NODE_W), toScreenY(pos.y() + pinY(-1)), mx, my}, outputColor(g, node));
        else
            drawWire(graphics, new double[]{mx, my, toScreenX(pos.x()), toScreenY(pos.y() + pinY(wireFrom.slot()))},
                    inputColor(g, node, wireFrom.slot()));
    }

    private void drawWire(GuiGraphics graphics, double[] e, int color) {
        int steps = (int) Mth.clamp((Math.abs(e[2] - e[0]) + Math.abs(e[3] - e[1])) / 6, 10, 64);
        int thickness = Math.max(2, (int) Math.round(2 * zoom));
        int half = thickness / 2;
        for (int i = 0; i <= steps; i++) {
            double[] p = bezier(e, (double) i / steps);
            int x = (int) Math.round(p[0]) - half, y = (int) Math.round(p[1]) - half;
            graphics.fill(x, y, x + thickness, y + thickness, color);
        }
    }

    private void renderNode(GuiGraphics graphics, ProgramGraph g, int index) {
        ProgramNode node = g.nodes().get(index);
        NodePos pos = posOf(g, index);
        int h = nodeHeight(node);
        double sx = toScreenX(pos.x()), sy = toScreenY(pos.y());
        if (sx > vx + vw || sy > vy + vh || sx + NODE_W * zoom < vx || sy + h * zoom < vy + TOOL_H)
            return;

        graphics.pose().pushPose();
        graphics.pose().translate(sx, sy, 0);
        graphics.pose().scale((float) zoom, (float) zoom, 1);

        boolean selected = selection.contains(node.id());
        int border = selected ? 2 : 1;
        graphics.fill(-border, -border, NODE_W + border, h + border, selected ? COLOR_SELECTED : COLOR_NODE_BORDER);
        graphics.fill(0, 0, NODE_W, HEADER_H, Kind.of(node).color);
        graphics.fill(0, HEADER_H, NODE_W, h, COLOR_NODE_BODY);
        graphics.drawString(font, Kind.of(node).label(), 4, 2, COLOR_TEXT, false);

        switch (node) {
            case SensorNode n -> {
                graphics.drawString(font, linkText(n.target() != null), 8, rowTextY(0), COLOR_MUTED, false);
                if (n.quantity() == Quantity.CONTROL)
                    graphics.drawString(font, awaitingKey == n.id() ? Component.translatable("feedback.programmer.press_key")
                            : Component.translatable("feedback.programmer.key",
                            InputConstants.Type.KEYSYM.getOrCreate(n.channel()).getDisplayName()), 8, rowTextY(1), COLOR_TEXT, false);
            }
            case ActuatorNode n -> {
                graphics.drawString(font, linkText(n.target() != null), 8, rowTextY(0), COLOR_MUTED, false);
                String[] channels = channelKeys(n);
                if (channels.length > 0)
                    graphics.drawString(font, Component.translatable(channels[Math.floorMod(n.channel(), channels.length)]),
                            8, rowTextY(1), channels.length > 1 ? COLOR_TEXT : COLOR_MUTED, false);
            }
            case MathNode n -> {
                graphics.drawString(font, "A", 8, rowTextY(0), COLOR_MUTED, false);
                int cx = NODE_W / 2;
                graphics.fill(cx - 8, HEADER_H + 2, cx + 8, HEADER_H + ROW_H, COLOR_NODE_BORDER);
                graphics.drawCenteredString(font, switch (n.op()) {
                    case ADD -> "+";
                    case SUBTRACT -> "-";
                    case MULTIPLY -> "x";
                    case DIVIDE -> "/";
                    case MIN -> "min";
                    case MAX -> "max";
                }, cx, rowTextY(0), COLOR_TEXT);
                if (n.rightInput() != ProgramNode.UNWIRED)
                    graphics.drawString(font, "B", 8, rowTextY(1), COLOR_MUTED, false);
                else if (!(literalBox != null && literalNodeId == n.id())) {
                    String text = String.valueOf(n.literalDefault());
                    graphics.drawString(font, text, 8, rowTextY(1), COLOR_TEXT, false);
                    graphics.fill(8, rowTextY(1) + 9, 8 + font.width(text), rowTextY(1) + 10, COLOR_MUTED);
                }
            }
            case ComparatorNode n -> {
                graphics.drawString(font, "A", 8, rowTextY(0), COLOR_MUTED, false);
                int cx = NODE_W / 2;
                graphics.fill(cx - 8, HEADER_H + 2, cx + 8, HEADER_H + ROW_H, COLOR_NODE_BORDER);
                graphics.drawCenteredString(font, n.op() == ComparatorNode.Compare.GREATER ? ">" : "<", cx, rowTextY(0), COLOR_TEXT);
                if (n.rightInput() != ProgramNode.UNWIRED)
                    graphics.drawString(font, "B", 8, rowTextY(1), COLOR_MUTED, false);
                else if (!(literalBox != null && literalNodeId == n.id())) {
                    String text = (n.literalDefault() + " " + unitSuffix(inputQuantity(g, n))).trim();
                    graphics.drawString(font, text, 8, rowTextY(1), COLOR_TEXT, false);
                    graphics.fill(8, rowTextY(1) + 9, 8 + font.width(text), rowTextY(1) + 10, COLOR_MUTED);
                }
            }
            case LogicNode n -> {
                graphics.drawString(font, "A", 8, rowTextY(0), COLOR_MUTED, false);
                int cx = NODE_W / 2;
                graphics.fill(cx - 12, HEADER_H + 2, cx + 12, HEADER_H + ROW_H, COLOR_NODE_BORDER);
                graphics.drawCenteredString(font, n.op().name(), cx, rowTextY(0), COLOR_TEXT);
                if (!n.op().unary())
                    graphics.drawString(font, "B", 8, rowTextY(1), COLOR_MUTED, false);
            }
            case DelayNode n -> {
                graphics.drawString(font, "in", 8, rowTextY(0), COLOR_MUTED, false);
                int cx = NODE_W / 2;
                graphics.fill(cx - 18, HEADER_H + 2, cx + 18, HEADER_H + ROW_H, COLOR_NODE_BORDER);
                graphics.drawCenteredString(font, typeLabel(n.type()), cx, rowTextY(0), typeColor(n.type()));
                graphics.drawString(font, Component.translatable("feedback.programmer.delay_length", n.ticks(), seconds(n.ticks())),
                        8, rowTextY(1), COLOR_TEXT, false);
                if (!(literalBox != null && literalNodeId == n.id())) {
                    Component typed = Component.translatable("feedback.programmer.delay_typed");
                    graphics.drawString(font, typed, 8, rowTextY(2), COLOR_MUTED, false);
                    graphics.fill(8, rowTextY(2) + 9, 8 + font.width(typed), rowTextY(2) + 10, COLOR_MUTED);
                }
            }
            case ReadNode n -> variableButton(graphics, g, n.variable());
            case WriteNode n -> {
                variableButton(graphics, g, n.variable());
                graphics.drawString(font, Component.translatable("feedback.programmer.write_when"), 8, rowTextY(1), COLOR_MUTED, false);
            }
        }

        if (!(node instanceof ActuatorNode || node instanceof WriteNode))
            drawPin(graphics, NODE_W, pinY(-1), outputColor(g, node), dragMode == DragMode.WIRE && wireTargetOk(g, node, -1));
        for (int slot = 0; slot < node.ports().size(); slot++)
            drawPin(graphics, 0, pinY(slot), inputColor(g, node, slot), dragMode == DragMode.WIRE && wireTargetOk(g, node, slot));

        graphics.pose().popPose();
    }

    private void drawPin(GuiGraphics graphics, int x, int y, int color, boolean highlight) {
        if (highlight)
            graphics.fill(x - 5, y - 5, x + 5, y + 5, COLOR_SELECTED);
        graphics.fill(x - 4, y - 4, x + 4, y + 4, COLOR_PIN_BORDER);
        graphics.fill(x - 3, y - 3, x + 3, y + 3, color);
    }

    /** Whether releasing the wire being dragged on {@code node}'s pin {@code slot} would connect. */
    private boolean wireTargetOk(ProgramGraph g, ProgramNode node, int slot) {
        if (wireFrom == null || wireFrom.nodeId() == node.id() || wireFrom.isOutput() == (slot < 0))
            return false;
        ProgramNode other = g.find(wireFrom.nodeId()).orElse(null);
        if (other == null)
            return false;
        return wireFrom.isOutput()
                ? g.wireError(node, slot, other).isEmpty()
                : g.wireError(other, wireFrom.slot(), node).isEmpty();
    }

    private static Component linkText(boolean linked) {
        return Component.translatable(linked ? "feedback.programmer.linked" : "feedback.programmer.unlinked");
    }

    /** Distinct per {@link Quantity} so a wire's colour names what it carries, not just Number-vs-Boolean. */
    private static int quantityColor(@Nullable Quantity quantity) {
        if (quantity == null)
            return COLOR_NUMBER_UNKNOWN;
        return switch (quantity) {
            case TEMPERATURE -> 0xFFFF8833;
            case LOAD -> 0xFFAA55FF;
            case SPEED -> 0xFF55CCFF;
            case WORK -> 0xFFFFDD55;
            case CONDITION -> 0xFFAAAAAA;
            case VOLTAGE -> 0xFFFFEE66;
            case CURRENT -> 0xFF66FFAA;
            case LENGTH -> 0xFFCCCCCC;
            case STATE -> COLOR_BOOLEAN;
            case CONTROL -> 0xFF66DDDD;
            case MASS -> 0xFFCCCCCC;
            case PRESSURE -> 0xFFDDDDEE;
            case ALTITUDE, AIRSPEED, ATTITUDE -> 0xFF88DDFF;
            case HARDNESS, FIGURE, ORE, EXPERIENCE, XP_LEVEL, CHARGE, FLUX, DEPOSIT, RESPONSE, MOISTURE, PATHOGEN, TOXIN, RADIATION, DOSE -> 0xFFCCCCCC;
        };
    }

    private static int typeColor(@Nullable ValueType type) {
        return type == ValueType.BOOLEAN ? COLOR_BOOLEAN : COLOR_NUMBER_UNKNOWN;
    }

    private static int outputColor(ProgramGraph g, ProgramNode node) {
        return switch (node) {
            case SensorNode s -> quantityColor(s.quantity());
            case MathNode ignored -> COLOR_NUMBER_UNKNOWN;
            case DelayNode d -> typeColor(d.type());
            case ReadNode r -> typeColor(g.variableType(r.variable()).orElse(null));
            default -> COLOR_BOOLEAN;
        };
    }

    private static int inputColor(ProgramGraph g, ProgramNode node, int slot) {
        return switch (node) {
            case ComparatorNode n -> quantityColor(inputQuantity(g, n));
            case MathNode ignored -> COLOR_NUMBER_UNKNOWN;
            case DelayNode d -> typeColor(d.type());
            case WriteNode w -> slot == 0 ? typeColor(g.variableType(w.variable()).orElse(null)) : COLOR_BOOLEAN;
            default -> COLOR_BOOLEAN;
        };
    }

    /** Ticks as seconds: whole when they are, else one decimal. */
    private static String seconds(int ticks) {
        return ticks % 20 == 0 ? String.valueOf(ticks / 20) : String.format("%.1f", ticks / 20.0);
    }

    /** The row-0 control of a Read or Write card: the variable it names, coloured by that variable's type. */
    private void variableButton(GuiGraphics graphics, ProgramGraph g, String name) {
        graphics.fill(8, HEADER_H + 2, NODE_W - 8, HEADER_H + ROW_H, COLOR_NODE_BORDER);
        graphics.drawString(font, name.isEmpty() ? Component.translatable("feedback.programmer.variable.none") : Component.literal(name),
                11, rowTextY(0), name.isEmpty() ? COLOR_MUTED : typeColor(g.variableType(name).orElse(null)), false);
    }

    private void renderVariables(GuiGraphics graphics, int mx, int my) {
        List<ProgramVariable> vars = shown().variables();
        int x = panelX(), h = panelListY() - panelY() + Math.max(1, vars.size()) * PANEL_ROW_H + 4;
        skin.plaque(graphics, x, panelY(), PANEL_W, h);
        graphics.drawString(font, Component.translatable("feedback.programmer.variables"), x + 5, panelY() + 4, skin.dim(), false);
        Component count = Component.translatable("feedback.programmer.variable.count", vars.size(), ControllerLimits.MAX.variables());
        graphics.drawString(font, count, x + PANEL_W - 5 - font.width(count),
                panelY() + 4, vars.size() >= ControllerLimits.MAX.variables() ? skin.warn() : skin.ink(), false);
        for (int i = 0; i < vars.size(); i++) {
            int y = panelListY() + i * PANEL_ROW_H;
            boolean hover = !isPrinted() && mx >= x && mx < x + PANEL_W && my >= y && my < y + PANEL_ROW_H;
            skin.row(graphics, x + 2, y, PANEL_W - 4, PANEL_ROW_H, i == renameIndex, hover);
            graphics.drawString(font, vars.get(i).name(), x + 6, y + 2, skin.ink(), false);
            graphics.drawString(font, typeLabel(vars.get(i).type()), x + 84, y + 2, typeColor(vars.get(i).type()), false);
            if (!isPrinted())
                graphics.drawString(font, "x", x + PANEL_W - 11, y + 2, skin.warn(), false);
        }
        if (vars.isEmpty())
            graphics.drawString(font, Component.translatable("feedback.programmer.variable.none_declared"), x + 6, panelListY() + 2, skin.dim(), false);
    }

    /** The {@link Quantity} of whichever linked sensor feeds this comparator -- both sides must agree, so either will do. */
    @Nullable
    private static Quantity inputQuantity(ProgramGraph g, ComparatorNode n) {
        for (int id : n.ports())
            if (g.find(id).orElse(null) instanceof SensorNode s && s.quantity() != null)
                return s.quantity();
        return null;
    }

    private static String unitSuffix(@Nullable Quantity quantity) {
        return quantity == null ? "" : Component.translatable("feedback.programmer.unit." + quantity.getSerializedName()).getString();
    }

    private void renderHud(GuiGraphics graphics, int mx, int my) {
        Component help = Component.translatable("feedback.programmer.help");
        graphics.drawString(font, help, vx + 6, vy + vh - 12, COLOR_MUTED, false);
        if (mx >= vx + 6 && mx < vx + 6 + font.width(help) + 12 && my >= vy + vh - 14 && my < vy + vh - 2 && !paletteOpen)
            graphics.renderTooltip(font, font.split(Component.translatable("feedback.programmer.help.more"), 200), mx, my);
        graphics.drawString(font, "[?]", vx + 6 + font.width(help) + 4, vy + vh - 12, COLOR_MUTED, false);
        if (Util.getMillis() < statusUntil)
            graphics.drawString(font, status, vx + 6, vy + vh - 24, COLOR_ERROR, false);
    }

    private void renderPalette(GuiGraphics graphics, int mx, int my) {
        if (!paletteOpen)
            return;
        List<Kind> kinds = paletteKinds();
        int h = PALETTE_HEADER_H + kinds.size() * PALETTE_ROW_H + 4;
        graphics.fill(paletteX - 1, paletteY - 1, paletteX + PALETTE_W + 1, paletteY + h + 1, COLOR_NODE_BORDER);
        graphics.fill(paletteX, paletteY, paletteX + PALETTE_W, paletteY + h, COLOR_PALETTE);
        graphics.drawString(font, Component.translatable("feedback.programmer.add_card"), paletteX + 6, paletteY + 4, COLOR_MUTED, false);
        for (int i = 0; i < kinds.size(); i++) {
            int rowY = paletteY + PALETTE_HEADER_H + i * PALETTE_ROW_H;
            if (mx >= paletteX && mx <= paletteX + PALETTE_W && my >= rowY && my < rowY + PALETTE_ROW_H)
                graphics.fill(paletteX, rowY, paletteX + PALETTE_W, rowY + PALETTE_ROW_H, COLOR_PALETTE_HOVER);
            graphics.fill(paletteX + 6, rowY + 3, paletteX + 12, rowY + 9, kinds.get(i).color);
            graphics.drawString(font, kinds.get(i).label(), paletteX + 17, rowY + 2, COLOR_TEXT, false);
        }
    }

    /** An unlinked Sensor/Actuator card hints at the Data Connector step, since that's what a click on it does. */
    private void renderCanvasTooltip(GuiGraphics graphics, int mx, int my) {
        if (paletteOpen || dragMode != DragMode.NONE || isPrinted() || my < vy + TOOL_H)
            return;
        int index = findNode(mx, my);
        if (index < 0)
            return;
        ProgramNode node = graph.nodes().get(index);
        boolean unlinked = node instanceof SensorNode s && s.target() == null
                || node instanceof ActuatorNode a && a.target() == null;
        NodePos pos = posOf(graph, index);
        if (unlinked && controlAt(node, toCanvasX(mx) - pos.x(), toCanvasY(my) - pos.y()) != null)
            graphics.renderTooltip(font, Component.translatable("feedback.programmer.link_hint"), mx, my);
    }
}
