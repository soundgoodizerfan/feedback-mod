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
package io.github.soundgoodizerfan.feedback.control.controller;

import io.github.soundgoodizerfan.feedback.registry.FTags;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.control.Analog;
import io.github.soundgoodizerfan.feedback.control.ChannelSource;
import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.control.program.ProgramEvaluator;
import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;
import io.github.soundgoodizerfan.feedback.control.program.ProgramNode;
import io.github.soundgoodizerfan.feedback.control.program.SensorNode;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.fitting.SensorFitting;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Reads sensors, compares numbers, switches actuators -- controller spec §1's whole allowed
 * list, run from whatever {@link ProgramGraph} its held Punch Card carries. Su-powered per the
 * spec's tier table: it must be turning to evaluate at all, the same "no motive force, no
 * switching" logic that already governs {@code ClutchBlockEntity}.
 * <p>
 * Not itself a {@link DataNode} -- see {@code control/program}'s class docs. A card carries its
 * own {@link DataNodeRef} directly, so nothing here needs to be the other end of a link.
 */
public class ControllerBlockEntity extends RotationNode implements Container {

    private static final String CARD = "Card";

    private ItemStack card = ItemStack.EMPTY;

    /**
     * The last quantised reading seen for each {@link SensorNode}, by node id -- spec §2.5: the
     * graph is only re-evaluated when one of these actually moves. Not persisted; losing it on
     * reload just costs one extra evaluation, which is harmless.
     */
    private final Map<Integer, Float> lastSensorValues = new HashMap<>();
    private ProgramGraph lastGraph = ProgramGraph.EMPTY;

    public ControllerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CONTROLLER.get(), pos, state);
    }

    @Override
    public Su getLoadSu() {
        return FTuning.CONTROLLER_LOAD_SU;
    }

    public void tickServer() {
        if (level == null || level.isClientSide || getRpm().value() == 0)
            return; // no motive force, no switching -- outputs hold their last state

        ProgramGraph graph = card.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY);
        if (graph.nodes().isEmpty())
            return;

        boolean sameGraph = graph.equals(lastGraph);
        Map<Integer, Float> current = new HashMap<>();
        for (ProgramNode node : graph.nodes())
            if (node instanceof SensorNode sensor)
                current.put(sensor.id(), readSensor(sensor));

        boolean changed = !sameGraph || !current.equals(lastSensorValues);
        if (!changed)
            return;

        // Evaluate on the readings just taken -- resolving every sensor a second time would cost
        // a second world lookup each, and could see a different value than the change test did.
        Optional<List<Integer>> order = ProgramEvaluator.topologicalOrder(graph);
        if (order.isPresent())
            ProgramEvaluator.evaluate(graph, order.get(),
                    sensor -> current.getOrDefault(sensor.id(), 0f), this::writeActuator);

        lastGraph = graph;
        lastSensorValues.clear();
        lastSensorValues.putAll(current);
    }

    private float readSensor(SensorNode sensor) {
        DataNodeRef ref = sensor.target();
        if (level == null || ref == null)
            return 0f;
        Optional<DataNode> node = DataNode.resolve(level, ref);
        if (node.orElse(null) instanceof ChannelSource source)
            return source.read(sensor.channel());
        // An actuator's own state, read back: 1 engaged, 0 not. ProgramEvaluator turns it into a boolean.
        if (node.orElse(null) instanceof Switchable sw)
            return sw.isEngaged() ? 1f : 0f;
        return node
                .filter(SensorFitting.class::isInstance)
                .map(SensorFitting.class::cast)
                .map(SensorFitting::readRaw)
                .orElse(0f);
    }

    /**
     * A Boolean switches a {@link Switchable}; a Float sets an {@link Analog} part's channel. A
     * number reaching a plain switch moves nothing -- a factory controller is a switch, never a
     * dial (philosophy 13) -- and a Boolean reaching a part that is only analog sets it full or off.
     */
    private void writeActuator(@Nullable DataNodeRef ref, int channel, Object value) {
        if (level == null || ref == null)
            return;
        DataNode target = DataNode.resolve(level, ref).orElse(null);
        if (value instanceof Float level && target instanceof Analog analog)
            analog.setLevel(channel, level);
        else if (value instanceof Boolean on) {
            if (target instanceof Switchable sw)
                sw.setEngaged(on);
            else if (target instanceof Analog analog)
                analog.setLevel(channel, on ? 1 : 0);
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
        cardChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return card.isEmpty() && stack.is(FTags.PUNCH_CARDS);
    }

    /** Saves, and forgets the cached graph so a new card is evaluated on its first turning tick. */
    private void cardChanged() {
        lastGraph = ProgramGraph.EMPTY;
        lastSensorValues.clear();
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        card = ItemStack.EMPTY;
        cardChanged();
    }

    // --- persistence --------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!card.isEmpty())
            tag.put(CARD, card.save(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        card = tag.contains(CARD)
                ? ItemStack.parse(registries, tag.getCompound(CARD)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
    }
}
