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
package io.github.soundgoodizerfan.feedback.vehicle.engine;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FTags;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Engine Stand's two slots and the design being worked on. Slot 0 is paper: printing writes
 * the design onto it, turning it into a Blueprint, and a Blueprint put in loads its design to edit
 * and print again. Slot 1 is a sample: any form of a buildable metal sets what the engine is made
 * of, and stays a sample -- it is never used up; the ingots are paid at the crafting table. The
 * Programmer's shape ({@code control/programmer/}), for an engine.
 */
public class EngineStandBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int PAPER = 0, SAMPLE = 1;

    private ItemStack paper = ItemStack.EMPTY;
    private ItemStack sample = ItemStack.EMPTY;
    private EngineDesign design = EngineDesign.starter(Feedback.id("steel"));
    private io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign transmission = io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign.starter(Feedback.id("steel"));
    /** Which design the stand is showing and printing: the engine's, or the transmission's. */
    private boolean transmissionMode;

    public EngineStandBlockEntity(BlockPos pos, BlockState state) {
        super(FVehicles.ENGINE_STAND_ENTITY.get(), pos, state);
    }

    public EngineDesign design() {
        return design;
    }

    public io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign transmission() {
        return transmission;
    }

    public boolean transmissionMode() {
        return transmissionMode;
    }

    /** The screen's edit of the engine. With {@code print}, it also goes onto the paper in the slot. */
    public void commit(EngineDesign edited, boolean print, Player player) {
        commit(edited, transmission, false, print, player);
    }

    /** The screen's edit of either design, and which is being shown; printing writes the shown one. */
    public void commit(EngineDesign edited, io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign gearbox, boolean gearboxMode, boolean print, Player player) {
        design = edited;
        transmission = gearbox;
        transmissionMode = gearboxMode;
        setChanged();
        if (!print)
            return;
        if (paper.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.feedback.engine_stand.no_paper"), true);
            return;
        }
        ItemStack blueprint;
        if (transmissionMode) {
            blueprint = new ItemStack(FVehicles.TRANSMISSION_BLUEPRINT.get());
            blueprint.set(FVehicles.TRANSMISSION_DESIGN.get(), transmission);
        } else {
            blueprint = new ItemStack(FVehicles.ENGINE_BLUEPRINT.get());
            blueprint.set(FVehicles.ENGINE_DESIGN.get(), design);
        }
        paper = blueprint;
        setChanged();
        player.displayClientMessage(Component.translatable("message.feedback.engine_stand.printed"), true);
    }

    // --- Container: one slot -------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return paper.isEmpty() && sample.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == SAMPLE ? sample : paper;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack out = getItem(slot).split(amount);
        setChanged();
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack out = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        return out;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == SAMPLE) {
            sample = stack;
            EngineDesign.sampleMaterial(io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials(), stack)
                    .ifPresent(m -> {
                        design = design.withMaterial(m);
                        transmission = transmission.withMaterial(m);
                    });
        } else {
            paper = stack;
            EngineDesign loaded = stack.get(FVehicles.ENGINE_DESIGN.get());
            if (loaded != null) {
                design = loaded; // a blueprint put in is opened for editing
                transmissionMode = false;
            }
            io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign gearbox = stack.get(FVehicles.TRANSMISSION_DESIGN.get());
            if (gearbox != null) {
                transmission = gearbox;
                transmissionMode = true;
            }
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SAMPLE
                ? EngineDesign.sampleMaterial(io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials(), stack).isPresent()
                : stack.is(FTags.BLUEPRINT_PAPER);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        paper = ItemStack.EMPTY;
        sample = ItemStack.EMPTY;
    }

    // --- MenuProvider --------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.feedback.engine_stand");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new EngineStandMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!paper.isEmpty())
            tag.put("Paper", paper.save(registries));
        if (!sample.isEmpty())
            tag.put("Sample", sample.save(registries));
        EngineDesign.CODEC.encodeStart(NbtOps.INSTANCE, design).ifSuccess(t -> tag.put("Design", t));
        io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign.CODEC.encodeStart(NbtOps.INSTANCE, transmission).ifSuccess(t -> tag.put("Transmission", t));
        tag.putBoolean("TransmissionMode", transmissionMode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        paper = ItemStack.parseOptional(registries, tag.getCompound("Paper"));
        sample = ItemStack.parseOptional(registries, tag.getCompound("Sample"));
        if (tag.contains("Design"))
            EngineDesign.CODEC.parse(NbtOps.INSTANCE, tag.get("Design")).result().ifPresent(d -> design = d);
        if (tag.contains("Transmission"))
            io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign.CODEC.parse(NbtOps.INSTANCE, tag.get("Transmission")).result().ifPresent(d -> transmission = d);
        transmissionMode = tag.getBoolean("TransmissionMode");
    }
}
