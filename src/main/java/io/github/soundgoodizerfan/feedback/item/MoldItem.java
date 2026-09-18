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
package io.github.soundgoodizerfan.feedback.item;

import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.process.Casting;
import io.github.soundgoodizerfan.feedback.process.CastingTable;
import io.github.soundgoodizerfan.feedback.process.MoltenVessel;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * A reusable vessel for a single casting: empty, then filled, then a solid result once it has
 * cooled enough to give up.
 *
 * <h2>One item, not one per shape</h2>
 * There is exactly one output shape in the roster right now -- an ingot -- so one mold covers it.
 * {@link Casting} keys on the fluid it is holding, not on the mold, so a second mold shape (a
 * plate mold, say) is a second item plus new {@code Casting} entries whenever the roster actually
 * wants one; nothing here forces that decision early.
 *
 * <h2>It is a workpiece, not a second heat system</h2>
 * A filled mold's temperature is {@link ItemHeat}, the same component and the same linear cooling
 * every other loose item in the mod already has. TFC gives its mold item its own parallel heat
 * capability; this mold does not need one, because it is not a different kind of object, it is an
 * ingot-shaped chest for molten metal that happens to be hot.
 *
 * <h2>Two actions, two clicks</h2>
 * Filling ({@link #onItemUseFirst}, against a {@link MoltenVessel}) and casting ({@link #use}, in
 * the air) are both real, deliberate actions rather than something that happens to the mold while
 * it sits in a slot -- the same "priced by a click, not refused outright" shape {@code
 * ThermometerItem} and {@code CalipersItem} already use.
 */
public class MoldItem extends Item {

    public MoldItem(Properties properties) {
        super(properties);
    }

    private static SimpleFluidContent contentOf(ItemStack stack) {
        return stack.getOrDefault(FDataComponents.MOLTEN_CONTENT.get(), SimpleFluidContent.EMPTY);
    }

    /** Filling: an empty mold pressed against a vessel holding a melt takes what it can. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || level.isClientSide)
            return InteractionResult.PASS;
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof MoltenVessel vessel))
            return InteractionResult.PASS;
        if (!contentOf(stack).isEmpty())
            return InteractionResult.PASS;

        FluidTank tank = vessel.getTank();
        FluidStack drawn = tank.drain(FTuning.MOLD_CAPACITY_MB, IFluidHandler.FluidAction.SIMULATE);
        if (drawn.isEmpty())
            return InteractionResult.PASS;

        tank.drain(drawn.getAmount(), IFluidHandler.FluidAction.EXECUTE);
        vessel.onDrained(drawn);
        stack.set(FDataComponents.MOLTEN_CONTENT.get(), SimpleFluidContent.copyOf(drawn));
        // The mold is exactly as hot as what it just took -- a melt held in a vessel is at the
        // vessel's own temperature by definition, the same claim CrucibleBlockEntity#removeItem
        // already makes for an ordinary workpiece leaving a slot.
        ItemHeat.set(stack, vessel.getTemperature(), level);
        return InteractionResult.sidedSuccess(false);
    }

    /** Casting: a filled, cooled mold gives up its contents when used in the air. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        SimpleFluidContent content = contentOf(stack);
        if (content.isEmpty())
            return InteractionResultHolder.pass(stack);

        float tu = ItemHeat.get(stack, level).value();
        Optional<Casting> maybe = CastingTable.get().find(content.copy());
        if (maybe.isEmpty() || !maybe.get().isSolidAt(tu))
            return InteractionResultHolder.fail(stack);

        if (!level.isClientSide) {
            ItemStack result = maybe.get().result().copy();
            stack.remove(FDataComponents.MOLTEN_CONTENT.get());
            ItemHeat.clear(stack);
            if (!player.getInventory().add(result))
                player.drop(result, false);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
