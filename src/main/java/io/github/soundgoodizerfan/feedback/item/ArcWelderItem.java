// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import java.util.List;
import java.util.function.Consumer;

import io.github.soundgoodizerfan.feedback.machine.Weldable;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The hand arc welder (roadmap E6): mends a worn part with filler metal and a battery's charge, and leaves a permanent weak spot.
 *
 * <h2>How it is used</h2>
 * Hold the welder with a modular item (tool, weapon, armour, gun, machine head) in the other hand and hold right click for
 * {@link ItemTuning#WELDER_USE_TICKS}; or right-click a placed {@link Weldable} machine (instant). A weld restores the part to its
 * maximum condition ({@link FDataComponents#MAX_CONDITION}) and then lowers that maximum by {@link ItemTuning#WELDER_MAX_STEP},
 * down to {@link ItemTuning#WELDER_MAX_FLOOR}: the part is mended to the new ceiling, never as good as new.
 *
 * <h2>What it costs</h2>
 * One ingot of the part's wearing material from the pack (told by material and form, never item id), and
 * {@link ItemTuning#WELDER_CHARGE_PER_WELD} Eu from the first charged battery item in the pack. The electrode is the wearing
 * part (hand tools may wear, digest): {@link FDataComponents#TOOL_CONDITION} falls by {@link ItemTuning#WELDER_ELECTRODE_WEAR} a weld
 * to {@link ItemTuning#WELDER_ELECTRODE_FLOOR} and the welder keeps working.
 * <p>
 * shortcut: power only from a battery in the pack, no cable-side supply; upgrade when a welder should plug into a line.
 * Condition lives in three places by item: a gun's bore ({@code GUN_WEAR}), a head's {@code TOOL_CONDITION}, and a tool's or
 * armour's vanilla durability (the max ceiling there is a starting damage, so wear needs no clamp).
 */
public class ArcWelderItem extends Item {

    /** Rounding slack when comparing a stored condition with its ceiling. */
    private static final float EPS = 1e-4f;

    public ArcWelderItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** A thing that can be welded right now: its ceiling, whether it is under it, the filler it wants, and how to write a new ceiling. */
    private record Job(float max, boolean needs, ResourceLocation filler, Consumer<Float> apply) {
    }

    /** The material of the part that wears on a modular stack, if its design names one. */
    public static ResourceLocation wearMaterial(Level level, ItemStack stack) {
        ModularDesign design = ModularStacks.design(stack);
        ModularType type = design == null ? null : ModularTypes.find(level, design.type()).orElse(null);
        ModularDesign.Part part = type == null ? null : design.part(type.wearRole());
        return part == null ? null : part.material();
    }

    private static Job jobOf(Level level, ItemStack stack) {
        ModularDesign design = ModularStacks.design(stack);
        ModularType type = design == null ? null : ModularTypes.find(level, design.type()).orElse(null);
        if (type == null)
            return null;
        float max = stack.getOrDefault(FDataComponents.MAX_CONDITION.get(), 1f);
        ResourceLocation filler = wearMaterial(level, stack);
        if (type.family() == ModularType.Family.GUN)
            return new Job(max, 1f - GunCondition.wear(stack) < max - EPS, filler, m -> {
                stack.set(FWeapons.GUN_WEAR.get(), 1f - m);
                stack.set(FDataComponents.MAX_CONDITION.get(), m);
            });
        if (type.family() == ModularType.Family.HEAD)
            return new Job(max, stack.getOrDefault(FDataComponents.TOOL_CONDITION.get(), 1f) < max - EPS, filler, m -> {
                stack.set(FDataComponents.TOOL_CONDITION.get(), m);
                stack.set(FDataComponents.MAX_CONDITION.get(), m);
            });
        if (!stack.isDamageableItem())
            return null;
        int top = stack.getMaxDamage();
        return new Job(max, stack.getDamageValue() > damageAt(top, max), filler, m -> {
            stack.setDamageValue(damageAt(top, m));
            stack.set(FDataComponents.MAX_CONDITION.get(), m);
            ModularStacks.refresh(stack);
        });
    }

    /** The damage a durability bar of {@code top} sits at when the part is at {@code condition}; never the spent point. */
    private static int damageAt(int top, float condition) {
        return Math.min(top - 1, (int) Math.ceil((1f - condition) * top - EPS));
    }

    private static Job jobOf(Weldable w) {
        return new Job(w.maxCondition(), w.getCondition() < w.maxCondition() - EPS, w.fillerMaterial(),
                m -> w.weld(m, m));
    }

    // --- use ------------------------------------------------------------------------------------

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof Weldable w))
            return InteractionResult.PASS;
        if (!context.getLevel().isClientSide)
            weld(player, context.getItemInHand(), jobOf(w));
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack welder = player.getItemInHand(hand);
        Job job = jobOf(level, player.getItemInHand(other(hand)));
        if (job == null || !job.needs()) {
            if (!level.isClientSide)
                player.displayClientMessage(Component.translatable("message.feedback.arc_welder.nothing"), true);
            return InteractionResultHolder.fail(welder);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(welder);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player)
            weld(player, stack, jobOf(level, player.getItemInHand(other(player.getUsedItemHand()))));
        return stack;
    }

    private static InteractionHand other(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return ItemTuning.WELDER_USE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    /** Server: pay for and apply one weld, or say why not. */
    private static void weld(Player player, ItemStack welder, Job job) {
        if (job == null || !job.needs()) {
            player.displayClientMessage(Component.translatable("message.feedback.arc_welder.nothing"), true);
            return;
        }
        ItemStack battery = chargedBattery(player);
        if (battery.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.feedback.arc_welder.no_power"), true);
            return;
        }
        ResourceLocation want = job.filler() == null ? ItemTuning.WELDER_DEFAULT_FILLER : job.filler();
        ItemStack filler = fillerIngot(player, want);
        if (filler.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.feedback.arc_welder.no_filler",
                    Component.translatable("material." + want.getNamespace() + "." + want.getPath())), true);
            return;
        }
        CompoundTag state = battery.get(FDataComponents.BATTERY.get()).copy();
        state.putFloat("Charge", state.getFloat("Charge") - ItemTuning.WELDER_CHARGE_PER_WELD);
        battery.set(FDataComponents.BATTERY.get(), state);
        filler.shrink(1);
        job.apply().accept(Math.max(ItemTuning.WELDER_MAX_FLOOR, job.max() - ItemTuning.WELDER_MAX_STEP));
        welder.set(FDataComponents.TOOL_CONDITION.get(), Math.max(ItemTuning.WELDER_ELECTRODE_FLOOR,
                welder.getOrDefault(FDataComponents.TOOL_CONDITION.get(), 1f) - ItemTuning.WELDER_ELECTRODE_WEAR));
        player.displayClientMessage(Component.translatable("message.feedback.arc_welder.done"), true);
    }

    /** The first battery item in the pack with a weld's charge in it (the live stack, so it can be drawn on). */
    private static ItemStack chargedBattery(Player player) {
        for (ItemStack s : player.getInventory().items) {
            CompoundTag state = s.get(FDataComponents.BATTERY.get());
            if (state != null && state.getFloat("Charge") >= ItemTuning.WELDER_CHARGE_PER_WELD)
                return s;
        }
        return ItemStack.EMPTY;
    }

    /** An ingot of {@code material} in the pack: told by the material table (material and form), never by item id. */
    private static ItemStack fillerIngot(Player player, ResourceLocation material) {
        var materials = MaterialTable.get().materials();
        for (ItemStack s : player.getInventory().items)
            if (!s.isEmpty() && Material.find(materials, s).filter(l -> l.form().equals("ingot") && l.id().equals(material)).isPresent())
                return s;
        return ItemStack.EMPTY;
    }

    // --- tooltip --------------------------------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.feedback.arc_welder.electrode."
                + FTuning.conditionBand(stack.getOrDefault(FDataComponents.TOOL_CONDITION.get(), 1f))));
    }
}
