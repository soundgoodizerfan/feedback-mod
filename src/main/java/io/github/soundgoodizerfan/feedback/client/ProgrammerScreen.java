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

import io.github.soundgoodizerfan.feedback.registry.FSounds;

import io.github.soundgoodizerfan.feedback.control.programmer.NodePos;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerBlockEntity;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerCommitPayload;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerLinkPayload;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerLinkedPayload;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerMenu;
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
import io.github.soundgoodizerfan.feedback.control.program.LogicNode;
import io.github.soundgoodizerfan.feedback.control.program.ComparatorNode;
import io.github.soundgoodizerfan.feedback.control.program.ProgramEvaluator;
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
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The node canvas: a full-window, pannable, zoomable graph editor, with the Punch Card slot,
 * hotbar and Print in a strip along the top.
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
 * Every edit lands on {@link #graph}/{@link #positions}, this screen's own copy; the server sees
 * them only as one {@link ProgrammerCommitPayload} on Print or close. Linking a card is the
 * exception, since the Data Connector selection lives on the server -- see {@link ProgrammerLinkPayload}.
 */
public class ProgrammerScreen extends SkinnedScreen<ProgrammerMenu> {

    /** Height of the top strip; must clear the slot row at {@link ProgrammerMenu#STRIP_SLOT_Y}. */
    private static final int STRIP_H = 28;
    private static final int PRINT_X = ProgrammerMenu.HOTBAR_X + 9 * 18 + 8;

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

    private static final int PALETTE_W = 110;
    private static final int PALETTE_HEADER_H = 14;
    private static final int PALETTE_ROW_H = 13;

    private static final int COLOR_BACKGROUND = 0xFF15191E;
    private static final int COLOR_GRID_MINOR = 0xFF1C2228;
    private static final int COLOR_GRID_MAJOR = 0xFF28303A;
    private static final int COLOR_NODE_BODY = 0xFF1C232B;
    private static final int COLOR_NODE_BORDER = 0xFF39434F;
    private static final int COLOR_SELECTED = 0xFF60A5FA;
    private static final int COLOR_PIN_BORDER = 0xFF10151A;
    private static final int COLOR_WIRE_HOVER = 0xFFFF6B6B;
    private static final int COLOR_BOX_FILL = 0x3060A5FA;
    private static final int COLOR_TEXT = 0xFFE6EDF3;
    private static final int COLOR_MUTED = 0xFFA7B2BD;
    private static final int COLOR_ERROR = 0xFFFF6B6B;
    private static final int COLOR_PALETTE = 0xFF202833;
    private static final int COLOR_PALETTE_HOVER = 0xFF2E3947;
    private static final int COLOR_BOOLEAN = 0xFFCC5555;
    private static final int COLOR_NUMBER_UNKNOWN = 0xFF55CC55;

    /** The palette's card kinds -- Greater/Less are one {@link ComparatorNode} shape with two labels; Logic and Math each one card whose operator cycles. */
    private enum Kind {
        SENSOR("sensor", 0xFFB8612F),
        GREATER("greater", 0xFF7449B8),
        LESS("less", 0xFF7449B8),
        LOGIC("logic", 0xFF7449B8),
        MATH("math", 0xFF3E8F5A),
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

        ProgramNode create(int id) {
            return switch (this) {
                case SENSOR -> new SensorNode(id, null, null);
                case GREATER -> new ComparatorNode(id, ComparatorNode.Compare.GREATER, ProgramNode.UNWIRED, ProgramNode.UNWIRED, 0f);
                case LESS -> new ComparatorNode(id, ComparatorNode.Compare.LESS, ProgramNode.UNWIRED, ProgramNode.UNWIRED, 0f);
                case LOGIC -> new LogicNode(id, LogicNode.Gate.AND, ProgramNode.UNWIRED, ProgramNode.UNWIRED);
                case MATH -> new MathNode(id, MathNode.Op.ADD, ProgramNode.UNWIRED, ProgramNode.UNWIRED, 0f);
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

    private Button printButton;
    private Component status = Component.empty();
    private long statusUntil;

    public ProgrammerScreen(ProgrammerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GuiSkin.PANEL);
        ProgrammerBlockEntity programmer = menu.getProgrammer();
        graph = programmer != null ? programmer.getDisplayedGraph() : ProgramGraph.EMPTY;
        positions = new HashMap<>(programmer != null ? programmer.getPositions() : Map.of());
        hadCard = !menu.getSlot(0).getItem().isEmpty();
    }

    @Override
    protected void init() {
        imageWidth = width;
        imageHeight = height;
        super.init();
        printButton = addRenderableWidget(GuiStyle.primary(GuiSkin.PANEL, Component.translatable("feedback.programmer.print"), b -> print(),
                PRINT_X, 4, 60, 20, FSounds.UI_PUNCH::get));
        if (literalBox != null)
            addRenderableWidget(literalBox);
        if (!framed) {
            framed = true;
            frameAll();
        }
    }

    // --- state ----------------------------------------------------------------------------

    private ProgramGraph printedGraph() {
        return menu.getSlot(0).getItem().getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY);
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
        int rows = node instanceof SensorNode s && s.quantity() == Quantity.CONTROL
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
        dirty = true;
    }

    private void replaceNode(ProgramNode replacement) {
        graph = replaced(replacement);
    }

    private ProgramGraph replaced(ProgramNode replacement) {
        List<ProgramNode> nodes = new ArrayList<>(graph.nodes());
        nodes.replaceAll(n -> n.id() == replacement.id() ? replacement : n);
        return new ProgramGraph(nodes);
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
        graph = new ProgramGraph(nodes);
    }

    private void setStatus(Component message) {
        status = message;
        statusUntil = Util.getMillis() + 4000;
    }

    private void setError(String key) {
        setStatus(Component.translatable("feedback.programmer.error." + key));
    }

    private void commit(boolean print) {
        PacketDistributor.sendToServer(new ProgrammerCommitPayload(menu.getPos(), graph, Map.copyOf(positions), print));
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
    @Override
    public void onClose() {
        commitLiteral();
        if (dirty && !isPrinted())
            commit(false);
        super.onClose();
    }

    /** The server closed the menu (out of reach, block broken): an unsent edit is lost -- say so. */
    @Override
    public void removed() {
        if (dirty && !isPrinted() && minecraft != null && minecraft.player != null)
            minecraft.player.displayClientMessage(Component.translatable("feedback.programmer.unsaved"), true);
        super.removed();
    }

    /** Taking the card out discards the server's working copy ({@code ProgrammerBlockEntity#removeItem}); follow suit rather than resurrecting it on close. */
    @Override
    protected void containerTick() {
        super.containerTick();
        boolean hasCard = !menu.getSlot(0).getItem().isEmpty();
        if (hadCard && !hasCard) {
            graph = ProgramGraph.EMPTY;
            positions.clear();
            undo.clear();
            redo.clear();
            selection.clear();
            closeLiteralBox();
            dirty = false;
        }
        hadCard = hasCard;
        printButton.active = !isPrinted();

        // The block entity's sync can land after the menu opens, so until the first edit keep
        // following it rather than trusting the snapshot taken in the constructor.
        ProgrammerBlockEntity programmer = menu.getProgrammer();
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
        return STRIP_H + (height - STRIP_H) / 2.0;
    }

    private double toScreenX(double canvasX) {
        return (canvasX - cameraX) * zoom + width / 2.0;
    }

    private double toScreenY(double canvasY) {
        return (canvasY - cameraY) * zoom + centerY();
    }

    private double toCanvasX(double screenX) {
        return (screenX - width / 2.0) / zoom + cameraX;
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
        zoom = Mth.clamp(Math.min(width / (maxX - minX + 80), (height - STRIP_H) / (maxY - minY + 80)), MIN_ZOOM, 1.5);
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
        graph = new ProgramGraph(nodes);
    }

    private void deleteSelection() {
        pushUndo();
        if (!selection.isEmpty())
            GuiSkin.play(FSounds.UI_DETENT.get(), 0.7f);
        graph = new ProgramGraph(graph.nodes().stream().filter(n -> !selection.contains(n.id())).toList());
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

    /** Pastes {@link #clipboard} at the cursor with fresh ids. Wires between pasted cards survive; wires from outside the copy don't. */
    private void paste() {
        if (clipboard == null)
            return;
        if (graph.nodes().size() + clipboard.graph().nodes().size() > FTuning.CONTROLLER_MAX_NODES) {
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
        graph = new ProgramGraph(nodes);
    }

    /** Whether a fresh {@code kind} card could attach to {@link #palettePin}, so the palette offers only what would wire up. */
    private boolean canAttach(Kind kind) {
        if (palettePin == null)
            return true;
        ProgramNode pinNode = graph.find(palettePin.nodeId()).orElse(null);
        if (pinNode == null)
            return true;
        ProgramNode candidate = kind.create(Integer.MIN_VALUE);
        if (palettePin.isOutput())
            return attachSlot(candidate, pinNode) >= 0;
        return kind != Kind.ACTUATOR && graph.wireError(pinNode, palettePin.slot(), candidate).isEmpty();
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
        if (graph.nodes().size() >= FTuning.CONTROLLER_MAX_NODES) {
            setError("too_many_nodes");
            return;
        }
        pushUndo();
        ProgramNode node = kind.create(nextId());
        List<ProgramNode> nodes = new ArrayList<>(graph.nodes());
        nodes.add(node);
        graph = new ProgramGraph(nodes);
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
        PacketDistributor.sendToServer(new ProgrammerLinkPayload(menu.getPos(), node.id(), node instanceof ActuatorNode, quiet, token));
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
        return node.target() != null && level != null
                && level.getBlockEntity(node.target().pos()) instanceof Analog analog ? analog.channelKeys() : new String[0];
    }

    private void cycleOp(LogicNode n) {
        pushUndo();
        replaceNode(new LogicNode(n.id(), n.op().next(), n.leftInput(), n.rightInput()));
    }

    private void cycleOp(MathNode n) {
        pushUndo();
        replaceNode(new MathNode(n.id(), n.op().next(), n.leftInput(), n.rightInput(), n.literalDefault()));
    }

    private static float literalOf(ProgramNode node) {
        return node instanceof MathNode m ? m.literalDefault() : node instanceof ComparatorNode c ? c.literalDefault() : 0f;
    }

    private void openLiteralBox(ProgramNode node) {
        commitLiteral();
        literalBox = new EditBox(font, 0, 0, 48, 12, Component.empty());
        literalBox.setValue(String.valueOf(literalOf(node)));
        literalNodeId = node.id();
        addRenderableWidget(literalBox);
        setFocused(literalBox);
    }

    private void commitLiteral() {
        if (literalBox == null)
            return;
        try {
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
        removeWidget(literalBox);
        literalBox = null;
    }

    private void openPalette(double mx, double my, @Nullable PinHit pin) {
        palettePin = pin;
        paletteCanvasX = toCanvasX(mx);
        paletteCanvasY = toCanvasY(my);
        int h = PALETTE_HEADER_H + paletteKinds().size() * PALETTE_ROW_H + 4;
        paletteX = (int) Math.min(mx, width - PALETTE_W - 4);
        paletteY = (int) Math.min(my, height - h - 4);
        paletteOpen = true;
    }

    // --- input ------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!settled())
            return true;
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
        // The strip holds real slots and widgets; AbstractContainerScreen#mouseClicked claims any
        // click inside imageWidth/imageHeight -- the whole window here -- so it only gets the strip.
        if (my < STRIP_H)
            return super.mouseClicked(mx, my, button);

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
            if (hasAltDown())
                cutPin(pin);
            else {
                dragMode = DragMode.WIRE;
                wireFrom = pin;
            }
            return true;
        }

        int index = findNode(mx, my);
        if (index < 0) {
            Wire wire = !printed && hasAltDown() ? findWire(mx, my) : null;
            if (wire != null) {
                cutPin(new PinHit(wire.toId(), wire.slot()));
                return true;
            }
            dragMode = DragMode.BOX;
            boxAdditive = hasShiftDown();
            boxStartX = boxEndX = toCanvasX(mx);
            boxStartY = boxEndY = toCanvasY(my);
            return true;
        }

        ProgramGraph g = shown();
        ProgramNode node = g.nodes().get(index);
        boolean additive = hasShiftDown() || hasControlDown();
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

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (!settled())
            return true;
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
                return super.mouseDragged(mx, my, button, dragX, dragY);
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (!settled())
            return true; // a drag let go while closing must not land an edit after the commit
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
                return super.mouseReleased(mx, my, button);
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

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (my < STRIP_H || scrollY == 0 || !settled())
            return super.mouseScrolled(mx, my, scrollX, scrollY);
        double focusX = toCanvasX(mx), focusY = toCanvasY(my);
        zoom = Mth.clamp(zoom * Math.pow(ZOOM_STEP, scrollY), MIN_ZOOM, MAX_ZOOM);
        cameraX = focusX - (mx - width / 2.0) / zoom;
        cameraY = focusY - (my - centerY()) / zoom;
        return true;
    }

    @Override
    public void mouseMoved(double mx, double my) {
        mouseX = mx;
        mouseY = my;
        hoveredWire = dragMode == DragMode.NONE && my >= STRIP_H && findPin(mx, my) == null && findNode(mx, my) < 0
                ? findWire(mx, my) : null;
        super.mouseMoved(mx, my);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (closing())
            return true;
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
            if (hasControlDown() && key == GLFW.GLFW_KEY_Z) {
                if (hasShiftDown())
                    restore(redo, undo);
                else
                    restore(undo, redo);
                return true;
            }
            if (hasControlDown() && key == GLFW.GLFW_KEY_Y) {
                restore(redo, undo);
                return true;
            }
            if (hasControlDown() && key == GLFW.GLFW_KEY_C) {
                copySelection();
                return true;
            }
            if (hasControlDown() && key == GLFW.GLFW_KEY_V) {
                paste();
                return true;
            }
            if ((key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) && !selection.isEmpty()) {
                deleteSelection();
                return true;
            }
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return closing() || (literalBox != null ? literalBox.charTyped(codePoint, modifiers) : super.charTyped(codePoint, modifiers));
    }

    // --- rendering --------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mx, int my, float partialTick) {
        placeLiteralBox();
        super.render(graphics, mx, my, partialTick);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        renderHud(graphics);
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
        literalBox.setY((int) toScreenY(pos.y() + rowTextY(1)) - 2);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mx, int my) {
        // Title and inventory label have no place in the strip layout.
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mx, int my) {
        graphics.fill(0, 0, width, height, COLOR_BACKGROUND);

        graphics.enableScissor(0, STRIP_H, width, height);
        renderGrid(graphics);
        ProgramGraph g = shown();
        for (Wire wire : wires(g)) {
            double[] e = wireEnds(g, wire);
            if (e != null)
                drawWire(graphics, e, wire.equals(hoveredWire) ? COLOR_WIRE_HOVER : outputColor(g.find(wire.fromId()).orElseThrow()));
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

        // The strip is the instrument panel's plate, its edge screws pushed off the window's corners.
        GuiSkin.PANEL.panel(graphics, -4, -4, width + 8, STRIP_H + 4);
        for (var slot : menu.slots)
            GuiSkin.PANEL.slot(graphics, slot.x, slot.y);
        graphics.drawString(font, title, PRINT_X + 68, 10, GuiSkin.PANEL.dim(), false);
        if (isPrinted())
            graphics.drawString(font, Component.translatable("feedback.programmer.printed_label"),
                    PRINT_X + 76 + font.width(title), 10, COLOR_ERROR, false);
    }

    private void renderGrid(GuiGraphics graphics) {
        double step = GRID_STEP;
        while (step * zoom < MIN_GRID_PIXELS)
            step *= 2;
        while (step * zoom >= MIN_GRID_PIXELS * 4)
            step /= 2;
        for (int gx = Mth.floor(toCanvasX(0) / step); gx <= Mth.ceil(toCanvasX(width) / step); gx++) {
            int x = (int) Math.round(toScreenX(gx * step));
            graphics.fill(x, STRIP_H, x + 1, height, Math.floorMod(gx, 5) == 0 ? COLOR_GRID_MAJOR : COLOR_GRID_MINOR);
        }
        for (int gy = Mth.floor(toCanvasY(STRIP_H) / step); gy <= Mth.ceil(toCanvasY(height) / step); gy++) {
            int y = (int) Math.round(toScreenY(gy * step));
            graphics.fill(0, y, width, y + 1, Math.floorMod(gy, 5) == 0 ? COLOR_GRID_MAJOR : COLOR_GRID_MINOR);
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
            drawWire(graphics, new double[]{toScreenX(pos.x() + NODE_W), toScreenY(pos.y() + pinY(-1)), mx, my}, outputColor(node));
        else
            drawWire(graphics, new double[]{mx, my, toScreenX(pos.x()), toScreenY(pos.y() + pinY(wireFrom.slot()))},
                    inputColor(g, node));
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
        if (sx > width || sy > height || sx + NODE_W * zoom < 0 || sy + h * zoom < STRIP_H)
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
                graphics.drawString(font, "B", 8, rowTextY(1), COLOR_MUTED, false);
            }
        }

        if (!(node instanceof ActuatorNode))
            drawPin(graphics, NODE_W, pinY(-1), outputColor(node), dragMode == DragMode.WIRE && wireTargetOk(g, node, -1));
        for (int slot = 0; slot < node.ports().size(); slot++)
            drawPin(graphics, 0, pinY(slot), inputColor(g, node), dragMode == DragMode.WIRE && wireTargetOk(g, node, slot));

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
            case HARDNESS, FIGURE, ORE, EXPERIENCE, DEPOSIT, RESPONSE, MOISTURE, PATHOGEN, TOXIN -> 0xFFCCCCCC;
        };
    }

    private static int outputColor(ProgramNode node) {
        return node instanceof SensorNode s ? quantityColor(s.quantity())
                : node instanceof MathNode ? COLOR_NUMBER_UNKNOWN : COLOR_BOOLEAN;
    }

    private static int inputColor(ProgramGraph g, ProgramNode node) {
        return node instanceof ComparatorNode n ? quantityColor(inputQuantity(g, n))
                : node instanceof MathNode ? COLOR_NUMBER_UNKNOWN : COLOR_BOOLEAN;
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

    private void renderHud(GuiGraphics graphics) {
        graphics.drawString(font, Component.translatable("feedback.programmer.help"), 6, height - 12, COLOR_MUTED, false);
        if (Util.getMillis() < statusUntil)
            graphics.drawString(font, status, 6, height - 24, COLOR_ERROR, false);
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
        if (paletteOpen || dragMode != DragMode.NONE || isPrinted() || my < STRIP_H)
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
