// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import io.github.soundgoodizerfan.feedback.logistics.sort.Band;
import io.github.soundgoodizerfan.feedback.modular.bake.FigureStore;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.registry.FStore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * One rack cell: eight pallet positions (2 across, 2 up, 2 deep) on a built frame and shelf. It holds stock and never
 * ticks; the aisle's dock is what moves things in and out. Broken, it keeps its stock on the item.
 */
public class RackBlockEntity extends BlockEntity implements io.github.soundgoodizerfan.feedback.modular.bake.FigureHolder {

    private final FigureStore figures = new FigureStore();
    private final Bay[] bays = new Bay[StoreTuning.BAYS_PER_CELL];

    public RackBlockEntity(BlockPos pos, BlockState state) {
        super(FStore.RACK_ENTITY.get(), pos, state);
        for (int i = 0; i < bays.length; i++)
            bays[i] = new Bay();
    }

    @Override
    public FigureStore figures() {
        return figures;
    }

    /** The only goods this rack takes: those recorded in this band, or null for any. */
    @Nullable
    private Band filter;

    @Nullable
    public Band filter() {
        return filter;
    }

    void setFilter(@Nullable Band to) {
        filter = to;
        changed();
    }

    /** Whether goods recorded as {@code marks} may be set down here. */
    public boolean takes(int marks) {
        return filter == null || Marks.has(marks, filter);
    }

    public Bay bay(int i) {
        return bays[i];
    }

    /** The share of the reference bay cap this rack carries: the weaker of frame and shelf, less what the shelf has worn. */
    public float rating() {
        return figures.get("Rating", 1f) * condition();
    }

    public float condition() {
        return figures.get("Condition", 1f);
    }

    /** The hottest cargo the shelf takes without wearing. */
    public float serviceTu() {
        return figures.get("ServiceTu", Float.MAX_VALUE);
    }

    /** Whether the frame burns. */
    boolean burns() {
        var design = figures.design();
        var frame = design == null ? null : design.part(io.github.soundgoodizerfan.feedback.modular.bake.BlockFigures.ROLE);
        var m = frame == null ? null
                : io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials().get(frame.material());
        return m != null && (m.combustion().isPresent()
                || m.traits().contains(io.github.soundgoodizerfan.feedback.process.Material.Trait.FLAMMABLE));
    }

    /** Drops every lot as loose stacks and empties the bays. */
    void spill() {
        for (Bay b : bays) {
            for (Lot l : b.lots)
                while (l.count > 0) {
                    int n = Math.min(l.count, l.item.getMaxStackSize());
                    net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(),
                            worldPosition.getZ(), l.item.copyWithCount(n));
                    l.count -= n;
                }
            b.lots.clear();
        }
        AisleChanges.bumpStock();
        setChanged();
    }

    /** Wears the shelf by a load set down {@code over} Tu past its rating. */
    void scorch(float over) {
        float c = Math.max(StoreTuning.CONDITION_FLOOR, condition() - over * StoreTuning.HOT_WEAR_PER_TU);
        if (c != condition()) {
            figures.put("Condition", c);
            setChanged();
        }
    }

    public boolean empty() {
        for (Bay b : bays)
            if (!b.empty())
                return false;
        return true;
    }

    /** Stock or fittings changed: saved, and shown to players near. */
    public void changed() {
        AisleChanges.bumpStock();
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        AisleChanges.bump();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        AisleChanges.bump();
    }

    private ListTag writeBays(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Bay b : bays)
            list.add(b.save(registries));
        return list;
    }

    private void readBays(ListTag list, HolderLookup.Provider registries) {
        for (int i = 0; i < bays.length; i++)
            bays[i].load(i < list.size() ? list.getCompound(i) : new CompoundTag(), registries);
    }

    private void writeFilter(CompoundTag tag) {
        if (filter != null)
            tag.putInt(RackStock.FILTER, filter.ordinal());
    }

    private void readFilter(CompoundTag tag) {
        filter = tag.contains(RackStock.FILTER) ? Band.byOrdinal(tag.getInt(RackStock.FILTER)) : null;
    }

    /** Whether a broken rack has anything of its own to keep: stock, a fitting in any bay, or a filter. */
    private boolean carries() {
        if (filter != null)
            return true;
        for (Bay b : bays)
            if (!b.empty() || b.fitting != Fitting.PALLET)
                return true;
        return false;
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        figures.set(input.get(FDataComponents.BLOCK_FIGURES.get()), input.get(FModular.DESIGN.get()), input.get(FModular.FIGURES.get()));
        CompoundTag stock = input.get(FDataComponents.RACK_STOCK.get());
        if (stock != null && level != null) {
            readBays(stock.getList(RackStock.BAYS, Tag.TAG_COMPOUND), level.registryAccess());
            readFilter(stock);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        figures.collect(components);
        if (level == null || !carries())
            return;
        CompoundTag stock = new CompoundTag();
        stock.put(RackStock.BAYS, writeBays(level.registryAccess()));
        float mass = 0f;
        for (Bay b : bays)
            mass += b.mass();
        stock.putFloat(RackStock.MASS, mass);
        writeFilter(stock);
        components.set(FDataComponents.RACK_STOCK.get(), stock);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        figures.save(tag, registries);
        tag.put(RackStock.BAYS, writeBays(registries));
        writeFilter(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        figures.load(tag, registries);
        readBays(tag.getList(RackStock.BAYS, Tag.TAG_COMPOUND), registries);
        readFilter(tag);
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
}
