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

import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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

    /** Everything out of the gun and back to the player: the magazine as a magazine, the chambered round loose. */
    public static void eject(Player player, ItemStack gun) {
        Magazine m = magazine(gun);
        if (!m.isEmpty()) {
            ItemStack mag = new ItemStack(FWeapons.MAGAZINE_ITEM.get());
            mag.set(FWeapons.MAGAZINE.get(), m);
            give(player, mag);
        }
        Cartridge chambered = gun.get(FWeapons.CHAMBER.get());
        if (chambered != null) {
            ItemStack round = new ItemStack(FWeapons.CARTRIDGE_ITEM.get());
            round.set(FWeapons.CARTRIDGE.get(), chambered);
            give(player, round);
        }
        gun.remove(FWeapons.MAGAZINE.get());
        gun.remove(FWeapons.CHAMBER.get());
    }

    public static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack))
            player.drop(stack, false);
    }
}
