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
package io.github.soundgoodizerfan.feedback.control.programmer;

import io.github.soundgoodizerfan.feedback.registry.FTags;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.control.data.DataLinkManager;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.control.program.ActuatorNode;
import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;
import io.github.soundgoodizerfan.feedback.control.program.ProgramNode;
import io.github.soundgoodizerfan.feedback.control.program.SensorNode;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.fitting.SensorFitting;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jetbrains.annotations.Nullable;

/**
 * Where a Punch Card's {@link ProgramGraph} is authored. Holds the card being worked on plus a
 * separate {@link #working} graph -- printing is the one moment they become the same thing.
 * <p>
 * A printed card (its own {@code PROGRAM_GRAPH} component non-empty) refuses every further
 * commit: spec §4's hard-rewrite tier is enforced here, not by the item.
 */
public class ProgrammerBlockEntity extends BlockEntity implements Container, MenuProvider {

    private static final String CARD = "Card";
    private static final String WORKING = "Working";
    private static final String POSITIONS = "Positions";

    private ItemStack card = ItemStack.EMPTY;
    private ProgramGraph working = ProgramGraph.EMPTY;

    /**
     * Where each card sits on the {@link ProgrammerScreen} canvas -- cosmetic only, never part
     * of {@link ProgramGraph} (a printed card carries no layout, only wiring). Keyed by node id,
     * same lifetime as {@link #working}.
     */
    private final Map<Integer, NodePos> positions = new HashMap<>();

    /**
     * Every target {@link #link} has handed out. A commit may only name these or targets already
     * on {@link #working} -- otherwise a modified client could write any position in the world
     * onto a card and drive or read a machine nobody linked. Not persisted: a link not committed
     * before a reload is simply lost.
     */
    private final Set<DataNodeRef> granted = new HashSet<>();

    public ProgrammerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.PROGRAMMER.get(), pos, state);
    }

    public boolean isPrinted() {
        return !card.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY).nodes().isEmpty();
    }

    public Map<Integer, NodePos> getPositions() {
        return Collections.unmodifiableMap(positions);
    }

    /** What a screen should draw: the printed graph if there is one, else the working copy. */
    public ProgramGraph getDisplayedGraph() {
        ProgramGraph printed = card.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY);
        return printed.nodes().isEmpty() ? working : printed;
    }

    /**
     * Replaces the working copy with what a {@link ProgrammerScreen} edited locally, then prints
     * if asked. The client is not trusted: {@link ProgramGraph#findEditError} refuses a graph whose
     * wiring could never have been drawn, and every link is re-resolved against the world where
     * its chunk is loaded, so a card can't claim a target of the wrong kind or a made-up quantity.
     * Last commit wins if two players edit the same Programmer at once.
     */
    public void commit(ProgramGraph graph, Map<Integer, NodePos> newPositions, boolean print, Player player) {
        if (level == null || level.isClientSide || !stillValid(player))
            return;
        if (isPrinted()) {
            player.displayClientMessage(Component.translatable("feedback.programmer.already_printed"), true);
            return;
        }
        Optional<String> error = graph.findEditError(FTuning.CONTROLLER_MAX_NODES);
        if (error.isPresent()) {
            player.displayClientMessage(Component.translatable("feedback.programmer.error." + error.get()), true);
            return;
        }

        Set<DataNodeRef> trusted = new HashSet<>(granted);
        for (ProgramNode node : working.nodes()) {
            DataNodeRef ref = targetOf(node);
            if (ref != null)
                trusted.add(ref);
        }
        working = new ProgramGraph(graph.nodes().stream().map(node -> recheckLink(node, trusted)).toList());
        positions.clear();
        newPositions.forEach((id, pos) -> {
            if (working.find(id).isPresent())
                positions.put(id, pos);
        });
        if (print)
            print(player);
        sync();
    }

    @Nullable
    private static DataNodeRef targetOf(ProgramNode node) {
        return node instanceof SensorNode s ? s.target() : node instanceof ActuatorNode a ? a.target() : null;
    }

    /**
     * A link the server never handed out, or whose loaded target no longer fits, is dropped; a
     * trusted one whose target is unloaded is kept as sent, since there's nothing to check it against.
     */
    private ProgramNode recheckLink(ProgramNode node, Set<DataNodeRef> trusted) {
        DataNodeRef ref = targetOf(node);
        if (ref == null || level == null)
            return node;
        if (trusted.contains(ref)) {
            if (!level.isLoaded(ref.pos()))
                return node;
            ProgramNode linked = resolveLink(node, ref);
            if (linked != null)
                return linked;
        }
        return node instanceof ActuatorNode a ? new ActuatorNode(a.id(), a.input(), null) : new SensorNode(node.id(), null, null);
    }

    /**
     * Consumes this player's pending Data Connector selection for card {@code nodeId} and answers
     * with a {@link ProgrammerLinkedPayload}. The card itself may exist only in the screen's local
     * copy so far, so nothing here touches {@link #working} -- the screen patches its copy and the
     * link reaches the server with the next commit, re-checked there by {@link #recheckLink}.
     */
    public void link(int nodeId, boolean actuator, boolean quiet, ServerPlayer player) {
        if (level == null || isPrinted() || !stillValid(player))
            return;
        Optional<DataNodeRef> pending = DataLinkManager.get().peekPending(player);
        if (pending.isEmpty()) {
            if (!quiet)
                player.displayClientMessage(Component.translatable("feedback.programmer.nothing_selected"), true);
            return;
        }
        ProgramNode probe = actuator ? new ActuatorNode(nodeId, ProgramNode.UNWIRED, null) : new SensorNode(nodeId, null, null);
        ProgramNode linked = resolveLink(probe, pending.get());
        if (linked == null) {
            if (!quiet)
                player.displayClientMessage(Component.translatable(actuator
                        ? "feedback.programmer.not_switchable" : "feedback.programmer.not_a_sensor"), true);
            return;
        }
        DataLinkManager.get().takePending(player);
        granted.add(pending.get());
        PacketDistributor.sendToPlayer(player, new ProgrammerLinkedPayload(worldPosition, nodeId, pending.get(),
                linked instanceof SensorNode s ? Optional.ofNullable(s.quantity()) : Optional.empty()));
    }

    /**
     * Whether {@code ref} fits {@code node}'s target type, and what to set on the node if so --
     * shared by {@link #link} and {@link #recheckLink}. Returns {@code null} without side effects
     * if it doesn't fit; the caller decides whether to consume the pending selection.
     */
    @Nullable
    private ProgramNode resolveLink(ProgramNode node, DataNodeRef ref) {
        if (level == null)
            return null;
        Optional<DataNode> resolved = DataNode.resolve(level, ref);
        if (node instanceof SensorNode sensor)
            return resolved.filter(SensorFitting.class::isInstance).map(SensorFitting.class::cast)
                    .<ProgramNode>map(fitting -> new SensorNode(sensor.id(), ref, resolveQuantity(fitting)))
                    .orElse(null);
        if (node instanceof ActuatorNode actuator)
            return resolved.filter(Switchable.class::isInstance).isPresent()
                    ? new ActuatorNode(actuator.id(), actuator.input(), ref) : null;
        return null;
    }

    /** The one {@link Quantity} a linked fitting reads -- every fitting so far reads exactly one. */
    @Nullable
    private static Quantity resolveQuantity(SensorFitting fitting) {
        for (Quantity quantity : Quantity.values())
            if (fitting.canRead(quantity))
                return quantity;
        return null;
    }

    private void print(Player player) {
        if (card.isEmpty()) {
            // card is ItemStack.EMPTY here -- the shared vanilla singleton. card.set(...) below
            // mutates whatever stack `card` refers to, so skipping this check would corrupt that
            // singleton's component map for every empty stack in the game, not just this one.
            player.displayClientMessage(Component.translatable("feedback.programmer.no_card"), true);
            return;
        }
        Optional<String> error = working.findError(FTuning.CONTROLLER_MAX_NODES);
        if (error.isPresent()) {
            player.displayClientMessage(Component.translatable("feedback.programmer.error." + error.get()), true);
            return;
        }
        card.set(FDataComponents.PROGRAM_GRAPH, working);
        player.displayClientMessage(Component.translatable("feedback.programmer.printed"), true);
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
        if (card.isEmpty())
            resetWorking();
        if (!result.isEmpty())
            sync(); // an open screen follows the block entity, so the reset has to reach it
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = card;
        card = ItemStack.EMPTY;
        resetWorking();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // No resetWorking: a program drafted before the card went in is kept for it.
        card = stack;
        card.limitSize(getMaxStackSize(card));
        sync();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return card.isEmpty() && stack.is(FTags.PUNCH_CARDS);
    }

    private void resetWorking() {
        working = ProgramGraph.EMPTY;
        positions.clear();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        card = ItemStack.EMPTY;
        resetWorking();
    }

    // --- MenuProvider ---------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("feedback.container.programmer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ProgrammerMenu(id, playerInventory, this);
    }

    // --- client sync ------------------------------------------------------------------------

    /** Also called from {@link ProgrammerBlock} right before opening the menu -- the screen reads {@link #working}/{@link #positions} straight off this block entity, not through a synced menu slot, so the opening player needs a fresh push rather than relying on whatever sync happened to fire during the last edit. */
    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // --- persistence --------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!card.isEmpty())
            tag.put(CARD, card.save(registries, new CompoundTag()));
        tag.put(WORKING, ProgramGraph.CODEC.encodeStart(NbtOps.INSTANCE, working).getOrThrow());

        ListTag positionList = new ListTag();
        positions.forEach((id, pos) -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Id", id);
            entry.putInt("X", pos.x());
            entry.putInt("Y", pos.y());
            positionList.add(entry);
        });
        tag.put(POSITIONS, positionList);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        card = tag.contains(CARD)
                ? ItemStack.parse(registries, tag.getCompound(CARD)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        working = tag.contains(WORKING)
                ? ProgramGraph.CODEC.parse(NbtOps.INSTANCE, tag.get(WORKING)).result().orElse(ProgramGraph.EMPTY)
                : ProgramGraph.EMPTY;

        positions.clear();
        for (Tag entry : tag.getList(POSITIONS, Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) entry;
            positions.put(c.getInt("Id"), new NodePos(c.getInt("X"), c.getInt("Y")));
        }
    }
}
