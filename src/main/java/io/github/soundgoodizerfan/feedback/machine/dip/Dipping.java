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
package io.github.soundgoodizerfan.feedback.machine.dip;

import java.util.Optional;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.process.Dip;
import io.github.soundgoodizerfan.feedback.process.DipTable;
import io.github.soundgoodizerfan.feedback.process.MoltenVessel;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Right-click a fluid with an item in hand. Two genuinely different things happen depending on
 * what the fluid is, and neither needed the other's machinery.
 *
 * <h2>A vessel's tank: only a recipe does anything</h2>
 * Draining against {@link DipTable} the same way {@code MoldItem} drains against {@link
 * io.github.soundgoodizerfan.feedback.process.CastingTable} -- see {@link Dip}'s own doc for why
 * this deliberately does nothing when no entry matches, rather than falling back to a generic
 * effect. A click against a Crucible mid-melt with no matching {@link Dip} entry has to pass
 * through untouched, or it would steal the click from {@code MoldItem}'s own fill interaction,
 * which also runs against a {@link MoltenVessel}'s tank. No identity check on the vessel either --
 * a Crucible and a Thermal Vessel both answer {@link MoltenVessel} the same way, so both dip the
 * same, and {@link Dip}'s own {@code min_temperature}/{@code max_temperature} band is checked
 * against whichever one's live {@code getTemperature()}, not a fixed number.
 *
 * <h2>Plain water and lava: an unconditional heat change</h2>
 * No table, no recipe -- water snaps a held item to {@link FTuning#AMBIENT_TU}, lava to {@link
 * FTuning#FIRE_TU_LAVA}, the same instant-on-contact model {@code Quenching} already uses for a
 * dropped ingot in water. This is deliberately the first crude, hands-on way to *add* heat to
 * something without any machine at all -- philosophy 7's manual route, arriving somewhere it was
 * never installed (§5's own test): unreliable (no rate control, no holding at a temperature) and
 * a little dangerous, exactly what "possible but not economical" should feel like.
 *
 * <h2>What this deliberately does not do yet</h2>
 * Nothing here judges whether an item "can stand" the temperature it was just dipped into --
 * "assuming they can stand the temperature" is the user's own framing, and it is honest that
 * nothing enforces it. An item that cannot survive lava simply reads as very hot; whatever
 * consequence that should have is a property of what receives the reading (a {@link
 * io.github.soundgoodizerfan.feedback.process.ThermalProcess}'s own {@code spoil_temperature}, if
 * one is watching), not a rule this interaction invents. Electroplating, the other reason this
 * table exists, is not built -- only named as the reason the table is shaped the way it is.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public class Dipping {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide)
            return;

        ItemStack held = event.getItemStack();
        // A bucket has its own, higher-priority reason to be right-clicked against a fluid.
        if (held.isEmpty() || held.getItem() instanceof BucketItem)
            return;

        BlockPos pos = event.getPos();

        if (level.getBlockEntity(pos) instanceof MoltenVessel vessel) {
            dipInVessel(vessel, held, event, level);
            return;
        }

        FluidState state = level.getFluidState(pos);
        if (!state.isSource())
            return;

        Tu tu;
        if (state.is(FluidTags.WATER))
            tu = FTuning.AMBIENT_TU;
        else if (state.is(FluidTags.LAVA))
            tu = FTuning.FIRE_TU_LAVA;
        else
            return;

        ItemHeat.set(held, tu, level);
        level.playSound(null, pos, state.is(FluidTags.LAVA) ? SoundEvents.LAVA_POP : SoundEvents.GENERIC_SPLASH,
                SoundSource.BLOCKS, 0.6f, 1.0f);

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static void dipInVessel(MoltenVessel vessel, ItemStack held,
                                     PlayerInteractEvent.RightClickBlock event, Level level) {
        FluidTank tank = vessel.getTank();
        FluidStack drawn = tank.drain(FTuning.DIP_MB, IFluidHandler.FluidAction.SIMULATE);
        if (drawn.isEmpty())
            return;

        Optional<Dip> maybe = DipTable.get().find(drawn, held, vessel.getTemperature());
        if (maybe.isEmpty())
            return;

        tank.drain(drawn.getAmount(), IFluidHandler.FluidAction.EXECUTE);
        vessel.onDrained(drawn);

        held.shrink(1);
        ItemStack result = maybe.get().result().copy();
        Player player = event.getEntity();
        if (!player.getInventory().add(result))
            player.drop(result, false);

        level.playSound(null, event.getPos(), SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.6f, 1.4f);

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
