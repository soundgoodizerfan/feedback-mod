// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.control.Analog;
import io.github.soundgoodizerfan.feedback.control.ChannelSource;
import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.control.program.ControllerLimits;
import io.github.soundgoodizerfan.feedback.control.program.DelayNode;
import io.github.soundgoodizerfan.feedback.control.program.ProgramEvaluator;
import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;
import io.github.soundgoodizerfan.feedback.control.program.ProgramNode;
import io.github.soundgoodizerfan.feedback.control.program.ProgramVariable;
import io.github.soundgoodizerfan.feedback.control.program.SensorNode;
import io.github.soundgoodizerfan.feedback.control.program.ValueType;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.SensorFitting;
import io.github.soundgoodizerfan.feedback.fitting.SidedFitting;
import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;
import io.github.soundgoodizerfan.feedback.fitting.upgrade.CardRackFitting;
import io.github.soundgoodizerfan.feedback.fitting.upgrade.EscapementFitting;
import io.github.soundgoodizerfan.feedback.fitting.upgrade.MemoryDrumFitting;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FTags;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameBlockEntity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.function.BooleanSupplier;

/**
 * The controller's whole behaviour, held apart from any block entity so the mechanical Controller, the Electric Controller and the
 * Computer share it: one Punch Card (or other medium) slot, the upgrades, the variables and delay history, and the evaluation of the
 * printed graph. The host says whether there is motive force ({@code powered}) and supplies the level, position and change
 * notification; everything else is here. Not itself a {@link DataNode}: a card carries its own {@link DataNodeRef}.
 */
public class ControllerCore implements Container, Fittable {

    private static final String CARD = "Card";
    private static final String VARIABLES = "Variables";
    private static final String DRUMS = "Drums";
    private static final String RACKS = "Racks";
    private static final String ESCAPEMENT = "Escapement";

    private ItemStack card = ItemStack.EMPTY;

    /** Fitted upgrades; stateless, so a count or flag per kind is the whole of their persistence. */
    private final List<UpgradeFitting> upgrades = new ArrayList<>();

    /**
     * The compiled {@link #lastGraph}, or null if it has a cycle or is not {@link #runnable}. Built
     * only when the graph changes, with the scratch arrays below sized to its sensors.
     */
    @Nullable
    private ProgramEvaluator.Plan plan;
    private SensorNode[] sensors = new SensorNode[0];
    /**
     * This tick's reading and the last evaluated reading for each sensor, by index into
     * {@link #sensors} -- spec §2.5: the graph is only re-evaluated when one of these actually
     * moves. Not persisted; losing them on reload just costs one extra evaluation, which is harmless.
     */
    private float[] current = new float[0];
    private float[] last = new float[0];
    private ProgramGraph lastGraph = ProgramGraph.EMPTY;
    /** Whether {@link #lastGraph} passes {@link ProgramGraph#findEditError} against {@link #limits()}; recomputed whenever the graph or upgrades change. */
    private boolean runnable;

    /** Variable values by name, each a Float or a Boolean. Persisted. */
    private final Map<String, Object> variables = new HashMap<>();
    /** Writes made this step; committed at its end so a Read sees them next step. */
    private final Map<String, Object> pendingWrites = new HashMap<>();
    /** Delay history by node id. Not persisted: after a reload each delay restarts from its current input. */
    private final Map<Integer, DelayHistory> delays = new HashMap<>();
    /** Ticks this controller has been turning; the delay clock. Frozen with the shaft. */
    private long clock;
    /** Clock at which a delay's output next changes; {@link Long#MAX_VALUE} if none is pending. */
    private long nextDue = Long.MAX_VALUE;
    /** A variable changed: run another step so Reads see it. */
    private boolean dirty;
    private boolean priming;

    private final BlockEntity host;
    private final BooleanSupplier powered;
    private final java.util.function.Predicate<ItemStack> takes;

    public ControllerCore(BlockEntity host, BooleanSupplier powered, java.util.function.Predicate<ItemStack> takes) {
        this.host = host;
        this.powered = powered;
        this.takes = takes;
    }

    private Level level() {
        return host.getLevel();
    }

    @Override
    public void setChanged() {
        host.setChanged();
    }

    /** The program's variables and their values (a Float or a Boolean each), as last saved or synced; for a monitor. */
    public Map<String, Object> variables() {
        return java.util.Collections.unmodifiableMap(variables);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /** The last reading of each sensor card of the running program, in the plan's sensor order; for a monitor. */
    public float[] readings() {
        return current.clone();
    }

    /** Whether this host takes the medium at all: a mechanical Controller takes the Punch Card only. */
    public boolean takesMedium(ItemStack stack) {
        return takes.test(stack);
    }

    /** What the fitted upgrades let a card use. */
    public ControllerLimits limits() {
        return limits(null);
    }

    private ControllerLimits limits(@Nullable UpgradeFitting without) {
        int drums = 0, racks = 0;
        boolean escapement = false;
        for (UpgradeFitting u : upgrades) {
            if (u == without)
                continue;
            if (u instanceof MemoryDrumFitting)
                drums++;
            else if (u instanceof CardRackFitting)
                racks++;
            else if (u instanceof EscapementFitting)
                escapement = true;
        }
        return ControllerLimits.of(drums, racks, escapement);
    }

    /** Why {@code stack} may not go in (an error key), or empty if it may. Blank cards always fit. */
    public Optional<String> refusalFor(ItemStack stack) {
        return limits().exceededBy(stack.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY));
    }

    public void tickServer() {
        if (level() == null || level().isClientSide || !powered.getAsBoolean())
            return; // no motive force, no switching -- outputs, variables and delays hold

        clock++;
        ProgramGraph graph = card.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY);
        if (graph.nodes().isEmpty())
            return;

        // Reference first: the card's component is the same object tick to tick, so the deep
        // compare runs only when something swapped it.
        boolean sameGraph = graph == lastGraph;
        if (!sameGraph && graph.equals(lastGraph)) {
            lastGraph = graph;
            sameGraph = true;
        }
        if (!sameGraph)
            loadGraph(graph);
        if (plan == null)
            return;

        boolean moved = false;
        for (int i = 0; i < sensors.length; i++) {
            float v = readSensor(sensors[i]);
            current[i] = v;
            if (Float.compare(v, last[i]) != 0)
                moved = true;
        }

        // The Controller already ticks every turning tick, so a due delay is a clock compare here;
        // a scheduled tick would add nothing.
        if (!(!sameGraph || dirty || clock >= nextDue || moved))
            return;
        dirty = false;

        // Evaluate on the readings just taken -- resolving every sensor a second time would cost
        // a second world lookup each, and could see a different value than the change test did.
        if (needsPriming(graph)) {
            priming = true;
            plan.evaluate(reader, NO_WRITE, state);
            priming = false;
        }
        plan.evaluate(reader, actuatorWriter, state);
        System.arraycopy(current, 0, last, 0, current.length);
    }

    /** Checks and compiles a graph new to this controller; leaves {@link #plan} null if it cannot run. */
    private void loadGraph(ProgramGraph graph) {
        lastGraph = graph;
        // The full edit check, not just the limits: a creative or datapack card never met the Programmer.
        runnable = graph.findEditError(limits()).isEmpty();
        Optional<List<Integer>> order = runnable ? ProgramEvaluator.topologicalOrder(graph) : Optional.empty();
        plan = order.map(o -> new ProgramEvaluator.Plan(graph, o)).orElse(null);
        sensors = plan == null ? new SensorNode[0] : plan.sensors();
        current = new float[sensors.length];
        last = new float[sensors.length];
    }

    private final ProgramEvaluator.SensorReader reader = (node, index) -> current[index];
    private final ProgramEvaluator.ActuatorWriter actuatorWriter = this::writeActuator;
    /** Told (key, argument) when the program switches something; the Computer's console listens. Never null. */
    private java.util.function.BiConsumer<String, String> events = (key, arg) -> { };

    public void events(java.util.function.BiConsumer<String, String> events) {
        this.events = events;
    }
    private static final ProgramEvaluator.ActuatorWriter NO_WRITE = (target, channel, value) -> { };

    private boolean needsPriming(ProgramGraph graph) {
        for (ProgramNode node : graph.nodes())
            if (node instanceof DelayNode d && !delays.containsKey(d.id()))
                return true;
        return false;
    }

    private final ProgramEvaluator.State state = new ProgramEvaluator.State() {
        @Override
        public Object read(String variable) {
            return variables.get(variable);
        }

        @Override
        public void write(String variable, Object value) {
            if (!priming)
                pendingWrites.put(variable, value);
        }

        @Override
        public Object delayed(DelayNode node) {
            DelayHistory h = delays.get(node.id());
            if (h == null)
                return node.type().zero();
            float v = h.valueAt(clock - node.ticks());
            return node.type() == ValueType.NUMBER ? (Object) v : (Object) (v > 0.5f);
        }

        @Override
        public void record(DelayNode node, Object input) {
            float v = input instanceof Float f ? f : input instanceof Boolean b && b ? 1f : 0f;
            DelayHistory h = delays.get(node.id());
            if (h == null) {
                // Seeded as if the input had always been this: the output starts at the current input.
                h = new DelayHistory();
                delays.put(node.id(), h);
                h.push(clock - node.ticks(), v);
            } else if (!priming) {
                h.pushIfChanged(clock, v);
            }
        }

        @Override
        public void endStep() {
            if (priming) {
                pendingWrites.clear();
                return;
            }
            boolean moved = false;
            for (Map.Entry<String, Object> write : pendingWrites.entrySet())
                if (!write.getValue().equals(variables.put(write.getKey(), write.getValue())))
                    moved = true;
            pendingWrites.clear();
            if (moved) {
                dirty = true;
                setChanged();
            }
            nextDue = Long.MAX_VALUE;
            for (Map.Entry<Integer, DelayHistory> entry : delays.entrySet()) {
                int ticks = ticksOf(entry.getKey());
                nextDue = Math.min(nextDue, entry.getValue().nextChange(clock, ticks));
            }
        }
    };

    private int ticksOf(int nodeId) {
        return lastGraph.find(nodeId).map(n -> n instanceof DelayNode d ? d.ticks() : 0).orElse(0);
    }

    private float readSensor(SensorNode sensor) {
        DataNodeRef ref = sensor.target() == null || level() == null ? null
                : FrameBlockEntity.located(level(), sensor.target(), host.getBlockPos());
        if (level() == null || ref == null)
            return 0f;
        DataNode node = DataNode.resolveOrNull(level(), ref);
        // An electric host reads only down a wire: no path of links, no reading; poor wire rounds it.
        if (node != null && node.acceptsLinks() && host instanceof DataNode self) {
            var wire = io.github.soundgoodizerfan.feedback.control.data.DataWires.reach(self, node);
            if (wire.isEmpty())
                return 0f;
            return io.github.soundgoodizerfan.feedback.control.data.DataWires.deliver(readNode(node, sensor), wire.get());
        }
        return readNode(node, sensor);
    }

    private float readNode(DataNode node, SensorNode sensor) {
        if (node instanceof ChannelSource source && source.offersChannels())
            return source.read(sensor.channel());
        // An actuator's own state, read back: 1 engaged, 0 not. ProgramEvaluator turns it into a boolean.
        if (node instanceof Switchable sw)
            return sw.isEngaged() ? 1f : 0f;
        return node instanceof SensorFitting fitting ? fitting.readRaw() : 0f;
    }

    /**
     * A Boolean switches a {@link Switchable}; a Float sets an {@link Analog} part's channel. A
     * number reaching a plain switch moves nothing -- a factory controller is a switch, never a
     * dial (philosophy 13) -- and a Boolean reaching a part that is only analog sets it full or off.
     */
    private void writeActuator(@Nullable DataNodeRef ref, int channel, Object value) {
        if (level() == null || ref == null)
            return;
        ref = FrameBlockEntity.located(level(), ref, host.getBlockPos());
        if (ref == null)
            return;
        DataNode target = DataNode.resolve(level(), ref).orElse(null);
        if (value instanceof Boolean pulse && channel > 0 && target instanceof io.github.soundgoodizerfan.feedback.control.Steppable stepper) {
            stepper.pulse(channel, pulse);
            return;
        }
        if (value instanceof Float amount && target instanceof Analog analog)
            analog.setLevel(channel, amount);
        else if (value instanceof Boolean on) {
            if (target instanceof Switchable sw) {
                boolean was = sw.isEngaged();
                sw.setEngaged(on);
                if (was != on)
                    events.accept(on ? "switch_on" : "switch_off", level().getBlockState(ref.linkedAt()).getBlock().getDescriptionId());
            } else if (target instanceof Analog analog)
                analog.setLevel(channel, on ? 1 : 0);
        }
    }

    /**
     * Change events of one delay's input, in a fixed ring: the input held {@code vals[i]} from
     * clock {@code times[i]} on. Changes only are stored, so a slow delay costs a few entries
     * rather than a sample per tick; a full ring forgets its oldest event (the output then holds that
     * event's value for earlier times -- shortcut: an input flipping more than the ring size inside
     * one delay length reads slightly wrong).
     */
    private static final class DelayHistory {
        private final long[] times = new long[FTuning.DELAY_HISTORY_EVENTS];
        private final float[] vals = new float[FTuning.DELAY_HISTORY_EVENTS];
        private int head, count;

        private int at(int i) {
            return (head + i) % times.length;
        }

        void push(long time, float value) {
            if (count == times.length) {
                head = (head + 1) % times.length;
                count--;
            }
            int slot = at(count++);
            times[slot] = time;
            vals[slot] = value;
        }

        void pushIfChanged(long time, float value) {
            if (count == 0 || Float.compare(vals[at(count - 1)], value) != 0)
                push(time, value);
        }

        /** The input as of {@code time}: the latest event at or before it, else the oldest known. */
        float valueAt(long time) {
            float result = vals[at(0)];
            for (int i = 0; i < count; i++) {
                if (times[at(i)] > time)
                    break;
                result = vals[at(i)];
            }
            return result;
        }

        /** The clock at which the output next changes, given a delay of {@code ticks}. */
        long nextChange(long now, int ticks) {
            for (int i = 0; i < count; i++)
                if (times[at(i)] + ticks > now)
                    return times[at(i)] + ticks;
            return Long.MAX_VALUE;
        }
    }

    // --- Container (one Punch Card slot) ---------------------------------------------------

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return card.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return card;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack result = card.split(count);
        if (!result.isEmpty())
            cardChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = card;
        card = ItemStack.EMPTY;
        cardChanged();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        card = stack;
        card.limitSize(getMaxStackSize(card));
        keepMatchingVariables();
        cardChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return card.isEmpty() && takes.test(stack) && refusalFor(stack).isEmpty();
    }

    /** A new card keeps the values of variables it declares with the same name and type; the rest are dropped. */
    private void keepMatchingVariables() {
        if (card.isEmpty())
            return; // taking the card out leaves the values for the same card coming back
        Map<String, Object> kept = new HashMap<>();
        for (ProgramVariable v : card.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY).variables()) {
            Object old = variables.get(v.name());
            if (v.type() == ValueType.NUMBER ? old instanceof Float : old instanceof Boolean)
                kept.put(v.name(), old);
        }
        variables.clear();
        variables.putAll(kept);
    }

    /** Saves, and forgets the cached graph so a new card is evaluated on its first turning tick. */
    private void cardChanged() {
        lastGraph = ProgramGraph.EMPTY;
        delays.clear();
        pendingWrites.clear();
        nextDue = Long.MAX_VALUE;
        dirty = false;
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(host, player);
    }

    @Override
    public void clearContent() {
        card = ItemStack.EMPTY;
        cardChanged();
    }

    // --- Fittable (upgrades only) -------------------------------------------------------------

    @Nullable
    @Override
    public SidedFitting getSidedFitting(Direction side) {
        return null;
    }

    @Override
    public void setSidedFitting(Direction side, @Nullable SidedFitting fitting) {
    }

    @Override
    public boolean canMount(SidedFitting fitting, Direction side) {
        return false;
    }

    @Override
    public boolean canAdd(UpgradeFitting upgrade) {
        long same = upgrades.stream().filter(u -> u.getClass() == upgrade.getClass()).count();
        if (upgrade instanceof MemoryDrumFitting)
            return same < FTuning.MEMORY_DRUM_MAX;
        if (upgrade instanceof CardRackFitting)
            return same < FTuning.CARD_RACK_MAX;
        return upgrade instanceof EscapementFitting && same < 1;
    }

    /** Refused while the inserted program needs what it would take away. */
    @Override
    public boolean canRemove(UpgradeFitting upgrade) {
        return limits(upgrade).exceededBy(card.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY)).isEmpty();
    }

    @Override
    public List<UpgradeFitting> getUpgrades() {
        return upgrades;
    }

    @Override
    public boolean addUpgrade(UpgradeFitting upgrade) {
        boolean added = upgrades.add(upgrade);
        if (added)
            upgradesChanged();
        return added;
    }

    @Override
    public void removeUpgrade(UpgradeFitting upgrade) {
        if (upgrades.remove(upgrade))
            upgradesChanged();
    }

    private void upgradesChanged() {
        lastGraph = ProgramGraph.EMPTY; // re-check the card against the new limits
        setChanged();
    }

    // --- persistence --------------------------------------------------------------------------

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        if (!card.isEmpty())
            tag.put(CARD, card.save(registries, new CompoundTag()));
        ListTag list = new ListTag();
        variables.forEach((name, value) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("N", name);
            if (value instanceof Float f)
                entry.putFloat("F", f);
            else
                entry.putBoolean("B", Boolean.TRUE.equals(value));
            list.add(entry);
        });
        tag.put(VARIABLES, list);
        tag.putInt(DRUMS, (int) upgrades.stream().filter(MemoryDrumFitting.class::isInstance).count());
        tag.putInt(RACKS, (int) upgrades.stream().filter(CardRackFitting.class::isInstance).count());
        tag.putBoolean(ESCAPEMENT, upgrades.stream().anyMatch(EscapementFitting.class::isInstance));
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        card = tag.contains(CARD)
                ? ItemStack.parse(registries, tag.getCompound(CARD)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        variables.clear();
        for (Tag t : tag.getList(VARIABLES, Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            variables.put(entry.getString("N"), entry.contains("F") ? (Object) entry.getFloat("F") : (Object) entry.getBoolean("B"));
        }
        upgrades.clear();
        for (int i = 0; i < Math.min(tag.getInt(DRUMS), FTuning.MEMORY_DRUM_MAX); i++)
            upgrades.add(new MemoryDrumFitting());
        for (int i = 0; i < Math.min(tag.getInt(RACKS), FTuning.CARD_RACK_MAX); i++)
            upgrades.add(new CardRackFitting());
        if (tag.getBoolean(ESCAPEMENT))
            upgrades.add(new EscapementFitting());
        lastGraph = ProgramGraph.EMPTY;
        delays.clear();
        nextDue = Long.MAX_VALUE;
    }
}
