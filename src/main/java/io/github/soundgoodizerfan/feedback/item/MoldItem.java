// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import java.util.List;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.process.Casting;
import io.github.soundgoodizerfan.feedback.process.CastingTable;
import io.github.soundgoodizerfan.feedback.process.MoltenVessel;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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

    /** The shape this mold casts, matched against {@link Casting#mold()}. */
    private final String shape;

    public MoldItem(Properties properties) {
        this(properties, Casting.INGOT_MOLD);
    }

    public MoldItem(Properties properties, String shape) {
        super(properties);
        this.shape = shape;
    }

    public String shape() {
        return shape;
    }

    public static SimpleFluidContent contentOf(ItemStack stack) {
        return stack.getOrDefault(FDataComponents.MOLTEN_CONTENT.get(), SimpleFluidContent.EMPTY);
    }

    /** What is in the mold, what it will make, and whether it has set. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SimpleFluidContent content = contentOf(stack);
        if (content.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.feedback.mold.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        FluidStack melt = content.copy();
        tooltip.add(Component.translatable("tooltip.feedback.mold.holds", melt.getHoverName(), melt.getAmount()).withStyle(ChatFormatting.GRAY));
        Optional<Casting> casting = CastingTable.get().find(melt, shape);
        if (casting.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.feedback.mold.nothing").withStyle(ChatFormatting.RED));
            return;
        }
        ItemStack result = casting.get().result();
        tooltip.add(Component.translatable("tooltip.feedback.mold.makes", result.getCount(), result.getHoverName()).withStyle(ChatFormatting.GRAY));
        if (context.level() != null)
            tooltip.add(Component.translatable(casting.get().isSolidAt(ItemHeat.get(stack, context.level()))
                    ? "tooltip.feedback.mold.set" : "tooltip.feedback.mold.molten").withStyle(ChatFormatting.GRAY));
    }

    /** Filling: an empty mold pressed against a vessel holding a melt takes what it can. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null)
            return InteractionResult.PASS;
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof MoltenVessel vessel))
            return InteractionResult.PASS;
        if (!contentOf(stack).isEmpty())
            return InteractionResult.PASS;
        // Consume the click on the client too, or use() fires on the same click and casts.
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        // Only a full mold's worth: a part-filled mold could neither cast nor be topped up.
        FluidTank tank = vessel.getTank();
        FluidStack drawn = tank.drain(FTuning.MOLD_CAPACITY_MB, IFluidHandler.FluidAction.SIMULATE);
        if (drawn.getAmount() < FTuning.MOLD_CAPACITY_MB)
            return InteractionResult.FAIL;

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

        Optional<Casting> maybe = CastingTable.get().find(content.copy(), shape);
        if (maybe.isEmpty() || !maybe.get().isSolidAt(ItemHeat.get(stack, level)))
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
