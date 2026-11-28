// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.bake.MaterialPerks;
import io.github.soundgoodizerfan.feedback.modular.combat.StrikeSource;
import io.github.soundgoodizerfan.feedback.process.Material.Perk;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The events the perks that act on someone else's behaviour need, subscribed once here as {@link ModularTraits} does for the hooks
 * (a trait class never subscribes): a placated mob refuses its attacker as a target, a stifled one's projectiles never leave, a phasing
 * blow ignores the shield, a salvaging tool's drops are the block itself, and open bleeds tick. All do nothing until a perk has marked something.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class PerkEvents {

    private PerkEvents() {
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || event.getNewAboutToBeSetTarget() == null || mob.level().isClientSide)
            return;
        PerkEffects.Mark mark = PerkEffects.PLACATED.get(mob.getUUID());
        if (PerkEffects.active(mark, mob.level().getGameTime()) && event.getNewAboutToBeSetTarget().getUUID().equals(mark.by()))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Projectile projectile)
                || !(projectile.getOwner() instanceof LivingEntity owner))
            return;
        if (PerkEffects.active(PerkEffects.STIFLED.get(owner.getUUID()), event.getLevel().getGameTime()))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onShield(LivingShieldBlockEvent event) {
        if (event.getDamageSource().getEntity() instanceof LivingEntity attacker && event.getDamageSource().getDirectEntity() == attacker
                && ModularTraits.has(StrikeSource.weapon(attacker, event.getDamageSource()), "phase"))
            event.setBlocked(false);
    }

    /** A salvaging tool leaves the block itself where its loot would have been, for a block with no data of its own to lose. */
    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (event.getDrops().isEmpty() || event.getBlockEntity() != null || !(event.getBreaker() instanceof Player)
                || !MaterialPerks.of(tool).containsKey(Perk.SALVAGE))
            return;
        net.minecraft.world.level.block.Block block = event.getState().getBlock();
        if (event.getDrops().stream().anyMatch(drop -> drop.getItem().is(block.asItem())) || block.asItem() == net.minecraft.world.item.Items.AIR)
            return;
        ServerLevel level = event.getLevel();
        net.minecraft.core.BlockPos pos = event.getPos();
        event.getDrops().clear();
        event.getDrops().add(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(block)));
    }

    /** The swing's charge, taken as it starts (exact reads it when the damage lands). */
    @SubscribeEvent
    public static void onAttackStart(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        if (!event.getEntity().level().isClientSide)
            BlowPerks.Exact.capture(event.getEntity());
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        long now = event.getServer().overworld().getGameTime();
        PerkEffects.tickBleeds(now);
        if (now % 600 == 0)
            PerkEffects.prune(now);
    }

    @SubscribeEvent
    public static void onStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        PerkEffects.clearBleeds();
    }
}
