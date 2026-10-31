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
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.modular.ForgeRhythm;
import io.github.soundgoodizerfan.feedback.process.Dip;
import io.github.soundgoodizerfan.feedback.process.DipTable;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.process.MoltenVessel;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
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
 * No table, no recipe -- water snaps a held item to the dimension's ambient ({@link AmbientTable#at}), lava to {@link
 * FTuning#FIRE_TU_LAVA}, the same instant-on-contact model {@code Quenching} already uses for a
 * dropped ingot in water. This is deliberately the first crude, hands-on way to *add* heat to
 * something without any machine at all -- philosophy 7's manual route, arriving somewhere it was
 * never installed (§5's own test): unreliable (no rate control, no holding at a temperature) and
 * a little dangerous, exactly what "possible but not economical" should feel like.
 *
 * <h2>An item that cannot stand the bath is scorched</h2>
 * A made part or workpiece ({@code MadeOf}) dipped past its material's service temperature takes a
 * flaw for every {@link FTuning#DIP_FLAW_TU} of overshoot, up to the flaw cap (user ruling,
 * 2026-10-23): worse the further past, never destroyed. Plain stacks with no {@code MadeOf} are
 * only made hot. Electroplating, the other reason this table exists, is not built -- only named as
 * the reason the table is shaped the way it is.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public class Dipping {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        ItemStack held = event.getItemStack();
        // A bucket has its own, higher-priority reason to be right-clicked against a fluid.
        if (level.isClientSide || held.isEmpty() || held.getItem() instanceof BucketItem)
            return;

        if (level.getBlockEntity(event.getPos()) instanceof MoltenVessel vessel) {
            dipInVessel(vessel, held, event, level);
            return;
        }
        // A block that can be placed here is the player filling the water in, not dipping: let it place.
        if (held.getItem() instanceof BlockItem
                && new BlockPlaceContext(level, event.getEntity(), event.getHand(), held, event.getHitVec()).canPlace())
            return;
        if (dipInFluid(event.getEntity(), held, level)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** Looking at open water or lava with nothing solid behind it is a click on air. */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Level level = event.getLevel();
        ItemStack held = event.getItemStack();
        if (level.isClientSide || held.isEmpty() || held.getItem() instanceof BucketItem)
            return;
        if (dipInFluid(event.getEntity(), held, level)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /**
     * The block a right click reports is whatever is behind the fluid -- vanilla's pick ignores
     * fluids -- so the fluid is found with a clip of its own that stops at source blocks.
     */
    private static boolean dipInFluid(Player player, ItemStack held, Level level) {
        if (!(player.pick(player.blockInteractionRange(), 1f, true) instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK)
            return false;
        BlockPos pos = hit.getBlockPos();
        FluidState state = level.getFluidState(pos);
        if (!state.isSource())
            return false;

        Tu tu;
        if (state.is(FluidTags.WATER))
            tu = AmbientTable.at(level);
        else if (state.is(FluidTags.LAVA))
            tu = FTuning.FIRE_TU_LAVA;
        else
            return false;

        ItemHeat.set(held, tu, level);
        scorch(held, tu);
        level.playSound(null, pos, state.is(FluidTags.LAVA) ? SoundEvents.LAVA_POP : SoundEvents.GENERIC_SPLASH,
                SoundSource.BLOCKS, 0.6f, 1.0f);
        return true;
    }

    /** A made part held past its material's service temperature takes flaws by the overshoot. */
    private static void scorch(ItemStack stack, Tu bath) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        Material m = made == null ? null : MaterialTable.get().materials().get(made.material());
        if (m == null || m.thermal().serviceTemperature().isEmpty())
            return;
        float over = bath.value() - m.thermal().serviceTemperature().get().value();
        if (over <= 0f)
            return;
        int flaw = Math.min(ForgeRhythm.MAX_FLAW, made.flaw() + (int) Math.ceil(over / FTuning.DIP_FLAW_TU));
        if (flaw > made.flaw())
            stack.set(FDataComponents.MADE_OF.get(), new MadeOf(made.material(), made.stock(), made.cells(),
                    made.variant(), flaw, made.dims()));
    }

    private static void dipInVessel(MoltenVessel vessel, ItemStack held,
                                     PlayerInteractEvent.RightClickBlock event, Level level) {
        FluidTank tank = vessel.getTank();
        // A full dip or none: a tank with less than the entry's amount cannot coat or feed anything.
        FluidStack bath = tank.getFluid();
        if (bath.getAmount() < FTuning.DIP_MB)
            return;

        Optional<Dip> maybe = DipTable.get().find(bath, held, vessel.getTemperature());
        if (maybe.isEmpty())
            return;
        Dip dip = maybe.get();

        FluidStack drawn = tank.drain(dip.fluid().amount(), IFluidHandler.FluidAction.EXECUTE);
        vessel.onDrained(drawn);

        held.shrink(1);
        if (dip.hasFluidResult()) {
            vessel.addMolten(dip.resultFluid().copy());
            level.playSound(null, event.getPos(), SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.6f, 1.4f);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        ItemStack result = dip.result().copy();
        // It comes out at the bath's temperature, like anything leaving a vessel.
        ItemHeat.set(result, vessel.getTemperature(), level);
        scorch(result, vessel.getTemperature());
        Player player = event.getEntity();
        if (!player.getInventory().add(result))
            player.drop(result, false);

        level.playSound(null, event.getPos(), SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.6f, 1.4f);

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
