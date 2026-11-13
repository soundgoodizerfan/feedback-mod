// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.item;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.bench.Edge;
import io.github.soundgoodizerfan.feedback.modular.combat.RangedDispatch;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Where vanilla's tools, weapons and armour arrive in the world, and each is made modular
 * ({@link ModularConvert}): loot (below, a global loot modifier), what a mob spawns holding, what a
 * -- for anything that got past those -- what a player finds in a pocket,
 * looked at once a second.
 */
public final class ModularEvents {

    private ModularEvents() {
    }

    /** Once a second is plenty: a stack this rarely sits unconverted long enough to be used. */
    private static final int INVENTORY_INTERVAL_TICKS = 20;

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % INVENTORY_INTERVAL_TICKS != 0)
            return;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (ModularConvert.convertible(stack))
                inv.setItem(i, ModularConvert.convert(stack, player.getUUID().hashCode() * 41L + i));
        }
    }

    /** A mob's gear, as it enters the world: spawned with it, or loaded holding it. */
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Mob mob))
            return;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack held = mob.getItemBySlot(slot);
            if (ModularConvert.convertible(held))
                mob.setItemSlot(slot, ModularConvert.convert(held, mob.getUUID().hashCode() * 41L + slot.ordinal()));
        }
    }

    /**
     * An edge is ground at a grindstone (a block in {@code #feedback:grindstones}): shift-use with
     * an edged modular tool in hand sets the next edge and costs a little of the tool. It was
     * sneak-scroll anywhere, which made regrinding free and instant -- a keen edge for the dig and
     * a blunt one for the walk home with nothing to pay.
     */
    public static void onGrindstone(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND || !player.isShiftKeyDown()
                || !event.getLevel().getBlockState(event.getPos()).is(io.github.soundgoodizerfan.feedback.registry.FTags.GRINDSTONES)
                || !ModularStacks.edged(stack))
            return;
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (event.getLevel().isClientSide)
            return;
        Edge edge = ModularStacks.grind(stack, player);
        if (edge == null)
            return;
        event.getLevel().playSound(null, event.getPos(), net.minecraft.sounds.SoundEvents.GRINDSTONE_USE,
                net.minecraft.sounds.SoundSource.BLOCKS, 1f, 1f);
        if (player instanceof net.minecraft.server.level.ServerPlayer sp)
            io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.send(sp, net.minecraft.network.chat.Component.translatable("message.feedback.modular.edge",
                    net.minecraft.network.chat.Component.translatable("tooltip.feedback.modular.edge." + edge.getSerializedName())), 50);
    }

    /** A shield stops a blow up to what its face can take, and only that much of a harder one. */
    public static void onShieldBlock(net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent event) {
        ItemStack shield = event.getEntity().getUseItem();
        ModularFigures f = ModularStacks.figures(shield);
        if (!(shield.getItem() instanceof ModularShield))
            return;
        if (f == null || ModularStacks.broken(shield))
            return;
        event.setBlockedDamage(Math.min(event.getBlockedDamage(), RangedDispatch.capacity(event.getEntity(), shield)));
    }

    /** Converts what a loot table generated. */
    public static class ConvertLoot extends LootModifier {

        public static final MapCodec<ConvertLoot> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, ConvertLoot::new));

        public ConvertLoot(LootItemCondition[] conditions) {
            super(conditions);
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
            loot.replaceAll(s -> ModularConvert.convert(s, context.getRandom().nextLong()));
            return loot;
        }

        @Override
        public MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec() {
            return CODEC;
        }
    }
}
