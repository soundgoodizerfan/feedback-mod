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

    /** The named figure of a fitted trait ({@code gun_valve}, {@code gun_lagging}), or {@code fallback} with none fitted. */
    private static float fitted(ItemStack gun, String trait, String name, float fallback) {
        for (var a : io.github.soundgoodizerfan.feedback.modular.ModularTraits.allOf(gun))
            if (a.id().equals(trait))
                return a.params().get(name, fallback);
        return fallback;
    }

    /** mB a shot lets out: the standard figure times the chest valve's {@code steam_mult}. */
    public static int perShot(ItemStack gun) {
        return Math.max(1, Math.round(FTuning.GUN_STEAM_MB_PER_SHOT * fitted(gun, "gun_valve", "steam_mult", 1f)));
    }

    /** Whether the chest holds enough for one more shot. */
    public static boolean canFire(ItemStack gun) {
        return stored(gun) >= perShot(gun);
    }

    /** A shot lets its steam out. */
    public static void spend(ItemStack gun) {
        set(gun, stored(gun) - perShot(gun));
    }

    /** Carried steam condenses: the gun's own chest is not a boiler, and nothing keeps it hot. */
    public static void condense(ItemStack gun) {
        float mb = stored(gun);
        if (mb > 0)
            set(gun, mb - FTuning.GUN_STEAM_LEAK_MB_PER_TICK * fitted(gun, "gun_lagging", "leak_mult", 1f));
    }

    /**
     * Fill the chest from a steam source at {@code pressure}. Steam cannot be pushed in past the source's own
     * pressure, so the chest fills only to {@code pressure / GUN_STEAM_FULL_PU} of its capacity. A chest filled past
     * what its metal is rated for lets go: the gun is damaged and the one holding it hurt, as for an overpressure shot,
     * and the steam is lost. shortcut: only the Boiler asks; any other steam block fills through the plain capability,
     * uncapped, until sources share a pressure interface.
     */
    public static int fillFrom(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.player.Player player,
                              ItemStack gun, IFluidHandler source, io.github.soundgoodizerfan.feedback.core.unit.Pu pressure) {
        var spec = GunSpec.of(gun);
        float capacity = spec.map(GunSpec::steamCapacityMb).orElse(0f);
        if (capacity <= 0)
            return 0;
        float reach = capacity * Math.min(1f, pressure.value() / FTuning.GUN_STEAM_FULL_PU);
        int want = (int) Math.max(0, reach - stored(gun));
        FluidStack offered = source.drain(new FluidStack(FFluids.STEAM.get(), want), IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty() || !offered.is(FTags.STEAM))
            return 0;
        int moved = source.drain(offered.getAmount(), IFluidHandler.FluidAction.EXECUTE).getAmount();
        set(gun, stored(gun) + moved);
        float rating = spec.get().steamRating(io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials());
        float held = FTuning.GUN_STEAM_FULL_PU * stored(gun) / capacity;
        if (rating > 0 && held > rating) {
            float noise = 1 + (level.random.nextFloat() * 2 - 1) * FTuning.GUN_HAZARD_NOISE;
            GunHazard.apply(level, player, gun, GunHazard.assess(held, rating, Float.MAX_VALUE, noise));
            io.github.soundgoodizerfan.feedback.core.hazard.PressureBurst.scald(level, player.blockPosition(), 1.5f, FTuning.STEAM_SCALD_DAMAGE);
            set(gun, 0f);
        }
        return moved;
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
