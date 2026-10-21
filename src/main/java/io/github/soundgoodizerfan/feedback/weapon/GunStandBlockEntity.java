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
 * form of a printable metal, never used up. Which part it sets is the screen's selection, so the
 * screen applies it and sends the edit; the server vouches for the metals at print.
 */
public class GunStandBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int PAPER = 0, SAMPLE = 1;
    /** Longest name a design may carry, in characters. */
    public static final int NAME_MAX = 40;

    private ItemStack paper = ItemStack.EMPTY;
    private ItemStack sample = ItemStack.EMPTY;
    private GunDesign design = GunDesign.starter(Feedback.id("steel"));
    /** The design's name, given to the blueprint printed and so to every gun built from it; empty for none. */
    private String name = "";

    public GunStandBlockEntity(BlockPos pos, BlockState state) {
        super(FWeapons.GUN_STAND_ENTITY.get(), pos, state);
    }

    public GunDesign design() {
        return design;
    }

    public String name() {
        return name;
    }

    /** An edit with no name change, as the game tests make. */
    public void commit(GunDesign edited, boolean print, Player player) {
        commit(edited, name, print, player);
    }

    /** The screen's edit. With {@code print}, it also goes onto the paper in the slot, named. */
    public void commit(GunDesign edited, String newName, boolean print, Player player) {
        design = edited;
        name = (newName.length() > NAME_MAX ? newName.substring(0, NAME_MAX) : newName).strip();
        setChanged();
        if (!print)
            return;
        if (paper.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.feedback.gun_stand.no_paper"), true);
            return;
        }
        // The server's own material table decides: a client names a material, it does not vouch for it.
        var materials = MaterialTable.get().materials();
        boolean windable = design.windingBlocks() == 0 || GunDesign.windingItem(materials).isPresent();
        var figures = windable && design.printable(materials)
                ? design.bake(materials) : java.util.Optional.<GunFigures>empty();
        if (figures.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.feedback.gun_stand.cannot_print"), true);
            return;
        }
        ItemStack blueprint = new ItemStack(io.github.soundgoodizerfan.feedback.registry.FModular.BLUEPRINT.get());
        blueprint.set(FWeapons.GUN_DESIGN.get(), design);
        blueprint.set(FWeapons.GUN_FIGURES.get(), figures.get());
        if (!name.isEmpty())
            blueprint.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(name));
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
        } else {
            paper = stack;
            GunDesign loaded = stack.get(FWeapons.GUN_DESIGN.get());
            if (loaded != null && stack.is(io.github.soundgoodizerfan.feedback.registry.FModular.BLUEPRINT.get())) {
                design = loaded; // a blueprint put in is opened for editing, under its name
                var named = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
                name = named == null ? "" : named.getString();
            }
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SAMPLE
                ? GunDesign.sampleMaterial(MaterialTable.get().materials(), stack, GunDesign.Part.GRIP).isPresent()
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
        if (!name.isEmpty())
            tag.putString("Name", name);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        paper = ItemStack.parseOptional(registries, tag.getCompound("Paper"));
        sample = ItemStack.parseOptional(registries, tag.getCompound("Sample"));
        if (tag.contains("Design"))
            GunDesign.CODEC.parse(NbtOps.INSTANCE, tag.get("Design")).result().ifPresent(d -> design = d);
        name = tag.getString("Name");
    }
}
