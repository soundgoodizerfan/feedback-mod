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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FTags;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
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
 * The Gun Stand's two slots and the design being worked on -- the Engine Stand's shape. Slot 0
 * is paper: printing bakes the design's figures and writes both onto it, turning it into a
 * Blueprint; a Blueprint put in loads its design to edit and print again. Slot 1 is a sample: any
 * form of a printable metal sets what the gun is made of, and is never used up.
 */
public class GunStandBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int PAPER = 0, SAMPLE = 1;

    private ItemStack paper = ItemStack.EMPTY;
    private ItemStack sample = ItemStack.EMPTY;
    private GunDesign design = GunDesign.starter(Feedback.id("steel"));

    public GunStandBlockEntity(BlockPos pos, BlockState state) {
        super(FWeapons.GUN_STAND_ENTITY.get(), pos, state);
    }

    public GunDesign design() {
        return design;
    }

    /** The screen's edit. With {@code print}, it also goes onto the paper in the slot. */
    public void commit(GunDesign edited, boolean print, Player player) {
        design = edited;
        setChanged();
        if (!print)
            return;
        if (paper.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.feedback.gun_stand.no_paper"), true);
            return;
        }
        // The server's own material table decides: a client names a material, it does not vouch for it.
        var materials = MaterialTable.get().materials();
        var metal = materials.get(design.material());
        var figures = metal != null && GunDesign.printable(design.material(), metal) ? design.bake(materials) : java.util.Optional.<GunFigures>empty();
        if (figures.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.feedback.gun_stand.cannot_print"), true);
            return;
        }
        ItemStack blueprint = new ItemStack(FWeapons.GUN_BLUEPRINT.get());
        blueprint.set(FWeapons.GUN_DESIGN.get(), design);
        blueprint.set(FWeapons.GUN_FIGURES.get(), figures.get());
        paper = blueprint;
        setChanged();
        player.displayClientMessage(Component.translatable("message.feedback.gun_stand.printed"), true);
    }

    // --- Container -----------------------------------------------------------------------------

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
            GunDesign.sampleMaterial(MaterialTable.get().materials(), stack).ifPresent(m -> design = design.withMaterial(m));
        } else {
            paper = stack;
            GunDesign loaded = stack.get(FWeapons.GUN_DESIGN.get());
            if (loaded != null && stack.is(FWeapons.GUN_BLUEPRINT.get()))
                design = loaded; // a blueprint put in is opened for editing
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SAMPLE
                ? GunDesign.sampleMaterial(MaterialTable.get().materials(), stack).isPresent()
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
        return Component.translatable("block.feedback.gun_stand");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new GunStandMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!paper.isEmpty())
            tag.put("Paper", paper.save(registries));
        if (!sample.isEmpty())
            tag.put("Sample", sample.save(registries));
        GunDesign.CODEC.encodeStart(NbtOps.INSTANCE, design).ifSuccess(t -> tag.put("Design", t));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        paper = ItemStack.parseOptional(registries, tag.getCompound("Paper"));
        sample = ItemStack.parseOptional(registries, tag.getCompound("Sample"));
        if (tag.contains("Design"))
            GunDesign.CODEC.parse(NbtOps.INSTANCE, tag.get("Design")).result().ifPresent(d -> design = d);
    }
}
