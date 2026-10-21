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

    public static final int CASE = 0, CHARGE = 1, CHARGE_B = 2, PROJECTILE = 3, PAYLOAD = 4, OUTPUT = 5;
    public static final int SLOTS = 6;

    /** The form names a charge or a payload may be in: a powder, a dust. A whole rod is not a charge. */
    private static final java.util.Set<String> LOOSE_FORMS = java.util.Set.of("powder", "dust");

    private final NonNullList<ItemStack> slots = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
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
        var material = found.get().material();
        boolean loose = LOOSE_FORMS.contains(found.get().form());
        return switch (slot) {
            case CASE -> found.get().form().equals("case");
            // A bullet seats as a bullet; a nugget is formed into shot by the die (spec 12).
            case PROJECTILE -> found.get().form().equals("bullet") || found.get().form().equals("nugget");
            // A charge is the powder of a propellant: a whole blaze rod is not one.
            case CHARGE, CHARGE_B -> material.propellant().isPresent() && loose;
            // A payload is read by what it is: a luminous powder makes a tracer, a flammable one
            // an incendiary. Nothing here names glowstone or blaze powder.
            case PAYLOAD -> loose && (material.traits().contains(Material.Trait.LUMINOUS)
                    || material.traits().contains(Material.Trait.FLAMMABLE));
            default -> false;
        };
    }

    /** The screen's setting, and with {@code seat} the lever. */
    public void commit(int bore, boolean seat, Player player) {
        boreMm = GunCalibres.nearest(bore).boreMm();
        setChanged();
        if (!seat)
            return;
        var materials = MaterialTable.get().materials();
        if (!slots.get(OUTPUT).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.feedback.press.output_full"), true);
            return;
        }
        var cas = located(materials, slots.get(CASE));
        var bullet = located(materials, slots.get(PROJECTILE));
        // No charge seats a dry round: a case and a slug are a small jar, charged later on a wire
        // (mechanics 6.6). The press does not know which was meant, only what it was given.
        boolean dry = slots.get(CHARGE).isEmpty() && slots.get(CHARGE_B).isEmpty();
        if (cas.isEmpty() || bullet.isEmpty() || !fits(CASE, slots.get(CASE), materials)
                || !fits(PROJECTILE, slots.get(PROJECTILE), materials)
                || (!slots.get(CHARGE).isEmpty() && !fits(CHARGE, slots.get(CHARGE), materials))
                || (!slots.get(CHARGE_B).isEmpty() && !fits(CHARGE_B, slots.get(CHARGE_B), materials))
                || (!slots.get(PAYLOAD).isEmpty() && !fits(PAYLOAD, slots.get(PAYLOAD), materials))) {
            player.displayClientMessage(Component.translatable("message.feedback.press.missing"), true);
            return;
        }
        // Two charge slots are the blend: what is in each is seated as is, in the ratio it is in.
        // There is no mixer recipe -- the machine that would do this does not exist yet, and a
        // blend belongs in one (TODO.md); until then the press takes the two by hand.
        java.util.List<Cartridge.Charge> charges = new java.util.ArrayList<>();
        for (int slot : new int[]{CHARGE, CHARGE_B}) {
            ItemStack stack = slots.get(slot);
            if (stack.isEmpty())
                continue;
            var id = located(materials, stack).get().id();
            var existing = charges.stream().filter(c -> c.material().equals(id)).findFirst();
            if (existing.isPresent()) {
                charges.set(charges.indexOf(existing.get()), new Cartridge.Charge(id, existing.get().count() + stack.getCount()));
            } else
                charges.add(new Cartridge.Charge(id, stack.getCount()));
        }
        int n = charges.stream().mapToInt(Cartridge.Charge::count).sum();
        if (n > FTuning.GUN_CHARGE_MAX) {
            player.displayClientMessage(Component.translatable("message.feedback.press.too_much", FTuning.GUN_CHARGE_MAX), true);
            return;
        }
        var payload = slots.get(PAYLOAD).isEmpty() ? java.util.Optional.<net.minecraft.resources.ResourceLocation>empty()
                : java.util.Optional.of(located(materials, slots.get(PAYLOAD)).get().id());
        Cartridge cartridge = new Cartridge(boreMm, cas.get().id(), charges, bullet.get().id(), payload,
                bullet.get().form().equals("nugget"), dry && level != null
                ? java.util.Optional.of(new Cartridge.Stored(0, level.getGameTime())) : java.util.Optional.empty());
        if (cartridge.load(materials).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.feedback.press.no_blend"), true);
            return;
        }
        ItemStack round = new ItemStack(FWeapons.CARTRIDGE_ITEM.get());
        round.set(FWeapons.CARTRIDGE.get(), cartridge);
        slots.get(CASE).shrink(1);
        slots.set(CHARGE, ItemStack.EMPTY);
        slots.set(CHARGE_B, ItemStack.EMPTY);
        slots.get(PROJECTILE).shrink(1);
        slots.get(PAYLOAD).shrink(1);
        slots.set(OUTPUT, round);
        setChanged();
        player.displayClientMessage(Component.translatable(dry ? "message.feedback.press.seated_dry" : "message.feedback.press.seated"), true);
    }

    // --- Container -----------------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return SLOTS;
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
