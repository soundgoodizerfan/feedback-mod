// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.attunement;

import io.github.soundgoodizerfan.feedback.modular.bake.MaterialPerks;
import io.github.soundgoodizerfan.feedback.modular.combat.Throwing;
import io.github.soundgoodizerfan.feedback.modular.combat.ThrownWeapon;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * The runtime kinds of the Recall effect. {@code recall}: a thrown weapon
 * whose head or tip is attuned comes back as Loyalty does, but only if that part is magnetic -- its material has a
 * Curie temperature -- and is not heated past it. {@code anchor} (the mirror): the struck target is planted.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class RecallKinds {

    /** The return rate a recall gives a thrown weapon, before {@link Throwing} scales it by mass. [OPEN] until played. */
    private static final float RETURN = 0.6f;
    /** Ticks a struck target is held per level of anchor. [OPEN] until played. */
    private static final int ANCHOR_TICKS = 40;
    /** Slowness amplitude that stops a walker (past -100% speed). [OPEN] until played. */
    private static final int ANCHOR_SLOW = 6;
    /** Brass return: refund chance per level and its cap. [OPEN] until played. */
    private static final float BRASS_PER_LEVEL = 0.15f, BRASS_CAP = 0.6f;

    /** Ticks between position samples, and how many are kept: 12 x 10 t = 6 s of history. [OPEN] until played. */
    private static final int SAMPLE_EVERY = 10;
    private static final int SAMPLES = 12;
    /** Rewind cooldown in ticks (60 s) and wear per level on each rewinding part. [OPEN] until played. */
    private static final long REWIND_COOLDOWN = 1200;
    private static final int REWIND_WEAR_PER_LEVEL = 3;

    /** Where a wearer stood, newest last, and when they last rewound. Cleared on logout. */
    private record Spot(net.minecraft.resources.ResourceKey<Level> dimension, net.minecraft.world.phys.Vec3 pos) {
    }

    private static final class Trail {
        final java.util.ArrayDeque<Spot> spots = new java.util.ArrayDeque<>();
        long lastUse = Long.MIN_VALUE / 2;
    }

    private static final java.util.Map<java.util.UUID, Trail> TRAILS = new java.util.HashMap<>();

    private RecallKinds() {
    }

    private static int worn(net.minecraft.world.entity.player.Player player) {
        int n = 0;
        for (ItemStack stack : player.getArmorSlots())
            if (!stack.isEmpty())
                for (MaterialPerks.Attunement a : MaterialPerks.attunements(stack))
                    if (a.kind().equals("rewind"))
                        n += a.level();
        return n;
    }

    /** Samples the position every {@link #SAMPLE_EVERY} ticks, only for a wearer of a rewinding part. */
    @SubscribeEvent
    public static void onTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        net.minecraft.world.entity.player.Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % SAMPLE_EVERY != 0)
            return;
        if (worn(player) == 0) {
            TRAILS.remove(player.getUUID());
            return;
        }
        Trail trail = TRAILS.computeIfAbsent(player.getUUID(), id -> new Trail());
        if (trail.spots.size() >= SAMPLES)
            trail.spots.removeFirst();
        trail.spots.addLast(new Spot(player.level().dimension(), player.position()));
    }

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        TRAILS.remove(event.getEntity().getUUID());
    }

    /** Sneak-use snaps the wearer back to the oldest sample; refused (no cost) if it is another dimension or the spot is blocked. */
    @SubscribeEvent
    public static void onRewind(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        net.minecraft.world.entity.player.Player player = event.getEntity();
        if (player.level().isClientSide || !player.isShiftKeyDown() || worn(player) == 0)
            return;
        Trail trail = TRAILS.get(player.getUUID());
        Spot spot = trail == null ? null : trail.spots.peekFirst();
        long now = player.level().getGameTime();
        if (spot == null || now - trail.lastUse < REWIND_COOLDOWN || spot.dimension() != player.level().dimension())
            return;
        net.minecraft.world.phys.Vec3 d = spot.pos().subtract(player.position());
        if (d.lengthSqr() < 1 || !player.level().noCollision(player, player.getBoundingBox().move(d)))
            return;
        player.teleportTo(spot.pos().x, spot.pos().y, spot.pos().z);
        player.fallDistance = 0f;
        trail.lastUse = now;
        trail.spots.clear();
        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.isEmpty())
                continue;
            int level = 0;
            for (MaterialPerks.Attunement a : MaterialPerks.attunements(stack))
                if (a.kind().equals("rewind"))
                    level += a.level();
            if (level > 0)
                stack.setDamageValue(Math.min(stack.getMaxDamage() - 1, stack.getDamageValue() + level * REWIND_WEAR_PER_LEVEL));
        }
    }

    public static void register() {
        // A planted round holds the target: the same slowness a thrown anchor gives.
        io.github.soundgoodizerfan.feedback.modular.attunement.AttunementKinds.onStrike("anchor_round", (event, a) ->
                event.getEntity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ANCHOR_TICKS * a.level(), ANCHOR_SLOW, false, false)));
    }

    /** Brass return: after a shot, a magnetic receiver below its Curie point may put the spent round back (never above capacity). */
    public static void refund(ItemStack gun, Level level, io.github.soundgoodizerfan.feedback.weapon.Cartridge round) {
        float chance = 0f;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(gun))
            if (a.kind().equals("brass_return") && a.material().separation().magnetic()
                    && a.material().separation().curieTemperature()
                    .map(curie -> io.github.soundgoodizerfan.feedback.weapon.GunCondition.temperature(gun, level).value() <= curie.value()).orElse(true))
                chance += a.level() * BRASS_PER_LEVEL;
        if (chance > 0 && level.random.nextFloat() < Math.min(BRASS_CAP, chance)
                && io.github.soundgoodizerfan.feedback.weapon.GunAmmo.count(gun) < io.github.soundgoodizerfan.feedback.weapon.GunAmmo.capacity(gun))
            gun.set(io.github.soundgoodizerfan.feedback.registry.FWeapons.MAGAZINE.get(),
                    io.github.soundgoodizerfan.feedback.weapon.GunAmmo.magazine(gun).push(round));
    }

    /** The return rate attunement gives {@code weapon} when thrown: 0 unless a recalling part is magnetic and below its Curie point. */
    public static float returns(ItemStack weapon, Level level) {
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(weapon))
            if (a.kind().equals("recall") && a.material().separation().curieTemperature()
                    .filter(curie -> ItemHeat.get(weapon, level).value() <= curie.value()).isPresent())
                return RETURN;
        return 0f;
    }

    @SubscribeEvent
    public static void onStruck(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || !(event.getSource().getDirectEntity() instanceof ThrownWeapon thrown))
            return;
        int level = 0;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(thrown.stack()))
            if (a.kind().equals("anchor"))
                level = Math.max(level, a.level());
        if (level > 0)
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ANCHOR_TICKS * level, ANCHOR_SLOW, false, false));
    }

    /** The head of a machine, when it is taken off {@code at} by a break or a blast: with {@code recall}, it goes to {@code to} instead of falling loose. */
    private static void recall(net.minecraft.world.level.block.entity.BlockEntity at, net.minecraft.world.entity.player.Player to) {
        if (!(at instanceof io.github.soundgoodizerfan.feedback.machine.Headed headed) || headed.getHead().isEmpty()
                || TideKinds.level(headed.getHead().get(), "recall") == 0)
            return;
        ItemStack head = headed.getHead().take();
        at.setChanged();
        if (!to.getInventory().add(head))
            to.drop(head, false);
    }

    /** A machine broken by hand gives its recalling head to the one who broke it. */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void onBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide() && event.getLevel().getBlockEntity(event.getPos()) instanceof net.minecraft.world.level.block.entity.BlockEntity be)
            recall(be, event.getPlayer());
    }

    /** A machine blown up gives its recalling head to the nearest player within reach. */
    @SubscribeEvent
    public static void onBlast(net.neoforged.neoforge.event.level.ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level))
            return;
        for (var pos : event.getAffectedBlocks())
            if (level.getBlockEntity(pos) instanceof io.github.soundgoodizerfan.feedback.machine.Headed) {
                var near = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, RECALL_REACH, false);
                if (near != null)
                    recall(level.getBlockEntity(pos), near);
            }
    }

    /** Blocks within which a blast hands a recalling head to a player. [OPEN] until played */
    private static final double RECALL_REACH = 16.0;
}
