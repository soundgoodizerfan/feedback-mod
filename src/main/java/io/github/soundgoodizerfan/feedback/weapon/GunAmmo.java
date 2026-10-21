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
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Where a gun's rounds are: one in the chamber and the rest in the magazine well. The chamber is
 * what a trigger pull fires and it refills itself from the magazine after, so a gun is empty
 * only when both are. All of it is components on the gun's stack; nothing here holds state.
 */
public final class GunAmmo {

    private GunAmmo() {
    }

    public static Magazine magazine(ItemStack gun) {
        return gun.getOrDefault(FWeapons.MAGAZINE.get(), Magazine.EMPTY);
    }

    /** The round the next pull fires: the chambered one, or failing that the magazine's top. */
    public static Optional<Cartridge> next(ItemStack gun) {
        Cartridge chambered = gun.get(FWeapons.CHAMBER.get());
        if (chambered != null)
            return Optional.of(chambered);
        Magazine m = magazine(gun);
        return m.isEmpty() ? Optional.empty() : Optional.of(m.top());
    }

    /** Rounds the gun holds, chamber and magazine together. */
    public static int count(ItemStack gun) {
        return magazine(gun).rounds().size() + (gun.has(FWeapons.CHAMBER.get()) ? 1 : 0);
    }

    /** Take the round {@link #next} named out of the gun, then feed the chamber. */
    public static void spend(ItemStack gun) {
        if (gun.has(FWeapons.CHAMBER.get()))
            gun.remove(FWeapons.CHAMBER.get());
        else if (!magazine(gun).isEmpty())
            gun.set(FWeapons.MAGAZINE.get(), magazine(gun).pop());
        feed(gun);
    }

    /** An empty chamber takes the magazine's top round. */
    public static void feed(ItemStack gun) {
        Magazine m = magazine(gun);
        if (!gun.has(FWeapons.CHAMBER.get()) && !m.isEmpty()) {
            gun.set(FWeapons.CHAMBER.get(), m.top());
            gun.set(FWeapons.MAGAZINE.get(), m.pop());
        }
    }

    /** How long swapping a magazine takes this gun, in ms: the reload, and a belt makes it longer. */
    public static long reloadMs(ItemStack gun) {
        return Math.round(FTuning.GUN_RELOAD_MS * io.github.soundgoodizerfan.feedback.modular.ModularTraits.gunReloadMult(gun));
    }

    /** A magazine item's size band; one made before bands is standard. */
    public static MagazineSize size(ItemStack magazine) {
        return magazine.getOrDefault(FWeapons.MAGAZINE_SIZE.get(), MagazineSize.STANDARD);
    }

    /** Rounds the gun can hold now, chamber included: its own feed's, or the magazine in it plus the chamber. */
    public static int capacity(ItemStack gun) {
        GunSpec design = GunSpec.of(gun).orElse(null);
        if (design != null && design.internalCapacity().isPresent())
            return design.internalCapacity().getAsInt();
        MagazineSize in = gun.get(FWeapons.MAGAZINE_SIZE.get());
        return 1 + (in == null ? 0 : in.capacity());
    }

    /**
     * Loose rounds of the gun's bore in {@code inventory} that its own feed has room for, in the
     * order they would go in. Empty for a gun fed from a magazine item. Client and server both
     * read it, so the reload the client waits out is the one the server books.
     */
    public static List<Integer> loadable(Inventory inventory, ItemStack gun, GunSpec design) {
        List<Integer> slots = new ArrayList<>();
        if (design.internalCapacity().isEmpty())
            return slots;
        int room = design.internalCapacity().getAsInt() - count(gun);
        // Rounds this gun's launcher drives go first; the rest only fill what is left.
        for (boolean preferred : new boolean[]{true, false})
            for (int i = 0; i < inventory.getContainerSize() && slots.size() < room; i++) {
                Cartridge round = inventory.getItem(i).get(FWeapons.CARTRIDGE.get());
                if (inventory.getItem(i).is(FWeapons.CARTRIDGE_ITEM.get()) && round != null && round.boreMm() == design.boreMm()
                        && prefers(design, round) == preferred)
                    for (int k = 0; k < inventory.getItem(i).getCount() && slots.size() < room; k++)
                        slots.add(i);
            }
        return slots;
    }

    /** Whether {@code round} is the kind {@code design}'s launcher drives: jar and redstone rounds for a coil, chemical for a chamber. */
    public static boolean prefers(GunSpec design, Cartridge round) {
        return round.coilDriven(io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials())
                == (design.launcher() == GunSpec.Launcher.COIL);
    }

    /** Push the {@link #loadable} rounds into the gun's own feed, one by one, and chamber. Returns how many went in. */
    public static int loadLoose(Inventory inventory, ItemStack gun, GunSpec design) {
        return loadLoose(inventory, gun, design, Integer.MAX_VALUE);
    }

    /** As above, at most {@code max} of them: a held reload loads one a beat. */
    public static int loadLoose(Inventory inventory, ItemStack gun, GunSpec design, int max) {
        List<Integer> slots = loadable(inventory, gun, design);
        if (slots.size() > max)
            slots = slots.subList(0, max);
        Magazine m = magazine(gun);
        for (int slot : slots) {
            m = m.push(inventory.getItem(slot).get(FWeapons.CARTRIDGE.get()));
            inventory.getItem(slot).shrink(1);
        }
        gun.set(FWeapons.MAGAZINE.get(), m);
        feed(gun);
        return slots.size();
    }

    /** Everything out of the gun and back to the player: a magazine as the magazine it was, a gun's own feed and the chamber loose. */
    public static void eject(Player player, ItemStack gun) {
        Magazine m = magazine(gun);
        MagazineSize in = gun.get(FWeapons.MAGAZINE_SIZE.get());
        if (in != null) {
            ItemStack mag = new ItemStack(FWeapons.MAGAZINE_ITEM.get());
            mag.set(FWeapons.MAGAZINE_SIZE.get(), in);
            if (!m.isEmpty())
                mag.set(FWeapons.MAGAZINE.get(), m);
            give(player, mag);
        } else
            for (Cartridge round : m.rounds())
                give(player, loose(round));
        Cartridge chambered = gun.get(FWeapons.CHAMBER.get());
        if (chambered != null)
            give(player, loose(chambered));
        gun.remove(FWeapons.MAGAZINE.get());
        gun.remove(FWeapons.MAGAZINE_SIZE.get());
        gun.remove(FWeapons.CHAMBER.get());
    }

    public static ItemStack loose(Cartridge round) {
        ItemStack out = new ItemStack(FWeapons.CARTRIDGE_ITEM.get());
        out.set(FWeapons.CARTRIDGE.get(), round);
        return out;
    }

    public static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack))
            player.drop(stack, false);
    }
}
