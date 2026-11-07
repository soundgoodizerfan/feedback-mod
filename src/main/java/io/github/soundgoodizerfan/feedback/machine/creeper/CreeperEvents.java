// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.creeper;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Leashing a creeper (vanilla forbids it) and the world's ways of setting off creeper charge outside a pen: a bolt striking a pen, fire or lava taking a dropped sac
 * or charge, and a carrier hurt by fire, blast, lightning or a fall with sacs or charge on them. Sparks reach pens only
 * (see {@link CreeperPenBlockEntity}).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class CreeperEvents {

    private CreeperEvents() {
    }

    /** Charge units in a stack: a sac's reserve, or the items of cut charge. */
    static float units(ItemStack stack) {
        if (stack.getItem() instanceof CreeperSacItem)
            return CreeperSacItem.charge(stack) * stack.getCount();
        if (stack.is(FItems.CREEPER_CHARGE.get()))
            return stack.getCount() * FTuning.CREEPER_CHARGE_PER_ITEM;
        return 0f;
    }

    /** Vanilla refuses to leash a hostile mob; a creeper is the one exception, so it can be walked to a pen. */
    @SubscribeEvent
    public static void onLead(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof net.minecraft.world.entity.monster.Creeper creeper) || creeper.isLeashed()
                || !event.getItemStack().is(net.minecraft.world.item.Items.LEAD))
            return;
        if (!event.getLevel().isClientSide) {
            creeper.setLeashedTo(event.getEntity(), true);
            if (!event.getEntity().getAbilities().instabuild)
                event.getItemStack().shrink(1);
        }
        event.setCancellationResult(net.minecraft.world.InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBolt(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LightningBolt bolt) || !(event.getLevel() instanceof ServerLevel level))
            return;
        int r = FTuning.CREEPER_LIGHTNING_RADIUS;
        BlockPos c = bolt.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -r, -r), c.offset(r, r, r)))
            if (level.getBlockState(p).is(FBlocks.CREEPER_PEN.get()) && level.getBlockEntity(p) instanceof CreeperPenBlockEntity pen)
                pen.struck();
    }

    @SubscribeEvent
    public static void onItemTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity item) || !(item.level() instanceof ServerLevel level))
            return;
        if (!item.isOnFire() && !item.isInLava())
            return;
        float units = units(item.getItem());
        if (units <= 0f)
            return;
        item.discard();
        CreeperBlast.explode(level, item.position(), units, false);
    }

    @SubscribeEvent
    public static void onCarrierHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level))
            return;
        var type = event.getSource();
        if (!(type.is(DamageTypeTags.IS_FIRE) || type.is(DamageTypeTags.IS_EXPLOSION) || type.is(DamageTypeTags.IS_LIGHTNING)
                || type.is(DamageTypeTags.IS_FALL)))
            return;
        float total = 0f;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            float u = units(inv.getItem(i));
            if (u > 0f) {
                total += u;
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (total > 0f) {
            io.github.soundgoodizerfan.feedback.core.Awards.give(player, "progress/joke/aw_man");
            CreeperBlast.explode(level, player.position().add(0, 1, 0), total, false);
        }
    }
}
