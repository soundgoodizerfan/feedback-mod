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
import io.github.soundgoodizerfan.feedback.registry.FFluids;
import io.github.soundgoodizerfan.feedback.registry.FTags;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * The steam in a steam gun's chest ({@code docs/specs/feedback_guns_spec.md} 13.4): a number of mB on
 * the stack, filled from a boiler by the fluid capability (right click the boiler with the gun),
 * let out a shot at a time, and condensing away while the gun is carried.
 */
public final class GunSteam {

    private GunSteam() {
    }

    /** mB of steam in {@code gun}'s chest. */
    public static float stored(ItemStack gun) {
        return gun.getOrDefault(FWeapons.GUN_STEAM.get(), 0f);
    }

    public static void set(ItemStack gun, float mb) {
        gun.set(FWeapons.GUN_STEAM.get(), Math.max(0f, mb));
    }

    /** Whether the chest holds enough for one more shot. */
    public static boolean canFire(ItemStack gun) {
        return stored(gun) >= FTuning.GUN_STEAM_MB_PER_SHOT;
    }

    /** A shot lets its steam out. */
    public static void spend(ItemStack gun) {
        set(gun, stored(gun) - FTuning.GUN_STEAM_MB_PER_SHOT);
    }

    /** Carried steam condenses: the gun's own chest is not a boiler, and nothing keeps it hot. */
    public static void condense(ItemStack gun) {
        float mb = stored(gun);
        if (mb > 0)
            set(gun, mb - FTuning.GUN_STEAM_LEAK_MB_PER_TICK);
    }

    /** The chest as a one-tank fluid handler, or null when the stack is not a steam gun. */
    public static IFluidHandlerItem handler(ItemStack gun) {
        float capacity = GunSpec.of(gun).map(GunSpec::steamCapacityMb).orElse(0f);
        return capacity <= 0 ? null : new Chest(gun, Math.round(capacity));
    }

    private record Chest(ItemStack gun, int capacity) implements IFluidHandlerItem {
        @Override
        public ItemStack getContainer() {
            return gun;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            int mb = (int) stored(gun);
            return mb <= 0 ? FluidStack.EMPTY : new FluidStack(FFluids.STEAM.get(), mb);
        }

        @Override
        public int getTankCapacity(int tank) {
            return capacity;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return stack.is(FTags.STEAM);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!resource.is(FTags.STEAM))
                return 0;
            int taken = Math.min(resource.getAmount(), capacity - (int) stored(gun));
            if (taken > 0 && action.execute())
                set(gun, stored(gun) + taken);
            return Math.max(0, taken);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return resource.is(FTags.STEAM) ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            int out = Math.min(maxDrain, (int) stored(gun));
            if (out <= 0)
                return FluidStack.EMPTY;
            if (action.execute())
                set(gun, stored(gun) - out);
            return new FluidStack(FFluids.STEAM.get(), out);
        }
    }
}
