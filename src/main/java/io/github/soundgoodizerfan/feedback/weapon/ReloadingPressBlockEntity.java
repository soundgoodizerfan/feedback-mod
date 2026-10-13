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

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Optional;

/**
 * The Reloading Press: four slots (case, charge, projectile, and the round that comes out) and
 * a die set, which is the bore it seats for. Pulling the lever {@link #seat} <em>seats whatever
 * is in it</em> -- the press never names a cartridge (machines define operations, not recipes),
 * and never knows when a charge is enough: it seats the count that is in the slot, up to what a
 * case physically takes, and the round's own spec sheet says whether that was wise.
 *
 * <p>What fills a slot is read off the material system: a case is the {@code case} form of a
 * metal, a projectile its {@code bullet} form, a charge the item of any material that has a
 * {@link Material.Propellant}. No item id is named here.
 */
public class ReloadingPressBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int CASE = 0, CHARGE = 1, PROJECTILE = 2, OUTPUT = 3;

    private final NonNullList<ItemStack> slots = NonNullList.withSize(4, ItemStack.EMPTY);
    private int boreMm = 9;

    public ReloadingPressBlockEntity(BlockPos pos, BlockState state) {
        super(FWeapons.PRESS_ENTITY.get(), pos, state);
    }

    public int boreMm() {
        return boreMm;
    }

    /** What an item is, as a part of a round, in {@code materials}: the material and the form it was found as. */
    public static Optional<Material.Located> located(Map<net.minecraft.resources.ResourceLocation, Material> materials, ItemStack stack) {
        return stack.isEmpty() ? Optional.empty() : Material.find(materials, stack.getItem());
    }

    /** Whether {@code stack} may go in {@code slot}. */
    public static boolean fits(int slot, ItemStack stack, Map<net.minecraft.resources.ResourceLocation, Material> materials) {
        if (slot == OUTPUT)
            return false;
        var found = located(materials, stack);
        if (found.isEmpty())
            return false;
        return switch (slot) {
            case CASE -> found.get().form().equals("case");
            case PROJECTILE -> found.get().form().equals("bullet");
            case CHARGE -> found.get().material().propellant().isPresent();
            default -> false;
        };
    }

    /** The screen's setting, and with {@code seat} the lever. */
    public void commit(int bore, boolean seat, Player player) {
        boreMm = Mth.clamp(bore, GunDesign.MIN_BORE_MM, GunDesign.MAX_BORE_MM);
        setChanged();
        if (!seat)
            return;
        var materials = MaterialTable.get().materials();
        if (!slots.get(OUTPUT).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.feedback.press.output_full"), true);
            return;
        }
        var cas = located(materials, slots.get(CASE));
        var charge = located(materials, slots.get(CHARGE));
        var bullet = located(materials, slots.get(PROJECTILE));
        if (cas.isEmpty() || charge.isEmpty() || bullet.isEmpty() || !fits(CASE, slots.get(CASE), materials)
                || !fits(CHARGE, slots.get(CHARGE), materials) || !fits(PROJECTILE, slots.get(PROJECTILE), materials)) {
            player.displayClientMessage(Component.translatable("message.feedback.press.missing"), true);
            return;
        }
        int n = slots.get(CHARGE).getCount();
        if (n > FTuning.GUN_CHARGE_MAX) {
            player.displayClientMessage(Component.translatable("message.feedback.press.too_much", FTuning.GUN_CHARGE_MAX), true);
            return;
        }
        ItemStack round = new ItemStack(FWeapons.CARTRIDGE_ITEM.get());
        round.set(FWeapons.CARTRIDGE.get(), new Cartridge(boreMm, cas.get().id(), charge.get().id(), n, bullet.get().id()));
        slots.get(CASE).shrink(1);
        slots.set(CHARGE, ItemStack.EMPTY);
        slots.get(PROJECTILE).shrink(1);
        slots.set(OUTPUT, round);
        setChanged();
        player.displayClientMessage(Component.translatable("message.feedback.press.seated"), true);
    }

    // --- Container -----------------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return 4;
    }

    @Override
    public boolean isEmpty() {
        return slots.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return slots.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack out = ContainerHelper.removeItem(slots, slot, amount);
        setChanged();
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(slots, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        slots.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return fits(slot, stack, MaterialTable.get().materials());
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        slots.clear();
    }

    // --- MenuProvider --------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.feedback.reloading_press");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ReloadingPressMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, slots, registries);
        tag.putInt("Bore", boreMm);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        slots.clear();
        ContainerHelper.loadAllItems(tag, slots, registries);
        if (tag.contains("Bore"))
            boreMm = tag.getInt("Bore");
    }
}
