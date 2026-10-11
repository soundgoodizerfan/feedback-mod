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
 * The Engine Stand's paper slot and the design being worked on. Printing writes the design onto
 * the paper, turning it into a Blueprint; a Blueprint put in loads its design, to edit and print
 * again. The Programmer's shape ({@code control/programmer/}), for an engine.
 */
public class EngineStandBlockEntity extends BlockEntity implements Container, MenuProvider {

    private ItemStack paper = ItemStack.EMPTY;
    private EngineDesign design = EngineDesign.starter(Feedback.id("steel"));

    public EngineStandBlockEntity(BlockPos pos, BlockState state) {
        super(FVehicles.ENGINE_STAND_ENTITY.get(), pos, state);
    }

    public EngineDesign design() {
        return design;
    }

    /** The screen's edit. With {@code print}, it also goes onto the paper in the slot. */
    public void commit(EngineDesign edited, boolean print, Player player) {
        design = edited;
        setChanged();
        if (!print)
            return;
        if (paper.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.feedback.engine_stand.no_paper"), true);
            return;
        }
        ItemStack blueprint = new ItemStack(FVehicles.ENGINE_BLUEPRINT.get());
        blueprint.set(FVehicles.ENGINE_DESIGN.get(), design);
        paper = blueprint;
        setChanged();
        player.displayClientMessage(Component.translatable("message.feedback.engine_stand.printed"), true);
    }

    // --- Container: one slot -------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return paper.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return paper;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack out = paper.split(amount);
        setChanged();
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack out = paper;
        paper = ItemStack.EMPTY;
        return out;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        paper = stack;
        EngineDesign loaded = stack.get(FVehicles.ENGINE_DESIGN.get());
        if (loaded != null)
            design = loaded; // a blueprint put in is opened for editing
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(FTags.BLUEPRINT_PAPER);
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
        EngineDesign.CODEC.encodeStart(NbtOps.INSTANCE, design).ifSuccess(t -> tag.put("Design", t));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        paper = ItemStack.parseOptional(registries, tag.getCompound("Paper"));
        if (tag.contains("Design"))
            EngineDesign.CODEC.parse(NbtOps.INSTANCE, tag.get("Design")).result().ifPresent(d -> design = d);
    }
}
