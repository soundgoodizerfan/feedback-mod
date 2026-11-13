// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Spoor (Observation): tracking, the reading of real signs. While a tracker walks, the footprints of creatures near it
 * (set down on the ground, one every block or so) and the blocks broken or placed near it are noted for a minute; the
 * cast shows what was noted within {@link CastTuning#SPOOR_RANGE} blocks as scuffs of the ground's own dust, to the
 * tracker only, and for the same minute. No light, no line through walls: only signs that were left.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Spoor {

    private Spoor() {
    }

    /** Where a creature set a foot down, on what, and when. */
    private record Print(Vec3 at, BlockState ground, long when) {
    }

    /** A block broken or placed, and when. */
    private record Disturbed(BlockPos at, BlockState was, long when) {
    }

    // shortcut: signs are kept in one pool across dimensions (a print in the Nether shows at the same coordinates in the overworld), split per level if it is ever seen
    /** Creature id -> its footprints, oldest first. */
    private static final Map<UUID, ArrayDeque<Print>> PRINTS = new HashMap<>();
    private static final ArrayDeque<Disturbed> DISTURBED = new ArrayDeque<>();
    /** Player id -> game tick their shown signs run out, and what to show. */
    private static final Map<UUID, Long> SHOWING = new HashMap<>();

    /** Footprints one creature keeps, blocks it must travel for the next, and blocks around a tracker that creatures are noted. */
    private static final int PRINTS_MAX = 60, DISTURBED_MAX = 300;
    private static final double STRIDE = 1.0, NOTICE = 32;

    /** Spoor: shows the signs left within range for a minute. False when there are none. */
    static boolean act(ServerPlayer p) {
        if (signs(p).isEmpty())
            return false;
        SHOWING.put(p.getUUID(), p.level().getGameTime() + CastTuning.SPOOR_TICKS);
        p.serverLevel().playSound(null, p.getX(), p.getEyeY(), p.getZ(), SoundEvents.FOX_SNIFF, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.9f);
        return true;
    }

    private record Sign(Vec3 at, BlockState ground, boolean block) {
    }

    private static List<Sign> signs(ServerPlayer p) {
        long now = p.level().getGameTime();
        double r2 = CastTuning.SPOOR_RANGE * CastTuning.SPOOR_RANGE;
        List<Sign> out = new ArrayList<>();
        for (var q : PRINTS.entrySet())
            if (!q.getKey().equals(p.getUUID()))
                for (Print pr : q.getValue())
                    if (now - pr.when() <= CastTuning.SPOOR_TICKS && pr.at().distanceToSqr(p.position()) <= r2)
                        out.add(new Sign(pr.at(), pr.ground(), false));
        for (Disturbed d : DISTURBED)
            if (now - d.when() <= CastTuning.SPOOR_TICKS && d.at().getCenter().distanceToSqr(p.position()) <= r2)
                out.add(new Sign(d.at().getCenter(), d.was(), true));
        return out;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 5 != 0 || !Skills.owned(p, "spoor"))
            return;
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        // note: creatures near a tracker leave prints as they walk
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(NOTICE), e -> e != p && e.isAlive() && e.onGround())) {
            ArrayDeque<Print> trail = PRINTS.computeIfAbsent(e.getUUID(), k -> new ArrayDeque<>());
            while (!trail.isEmpty() && now - trail.peekFirst().when() > CastTuning.SPOOR_TICKS)
                trail.pollFirst();
            if (!trail.isEmpty() && trail.peekLast().at().distanceToSqr(e.position()) < STRIDE * STRIDE)
                continue;
            BlockState ground = level.getBlockState(e.blockPosition().below());
            if (ground.isAir())
                continue;
            trail.addLast(new Print(e.position(), ground, now));
            if (trail.size() > PRINTS_MAX)
                trail.pollFirst();
        }
        for (ArrayDeque<Print> trail : PRINTS.values())
            while (!trail.isEmpty() && now - trail.peekFirst().when() > CastTuning.SPOOR_TICKS)
                trail.pollFirst();
        PRINTS.values().removeIf(ArrayDeque::isEmpty);
        Long until = SHOWING.get(p.getUUID());
        if (until == null)
            return;
        if (now > until) {
            SHOWING.remove(p.getUUID());
            return;
        }
        for (Sign s : signs(p))
            level.sendParticles(p, new BlockParticleOption(ParticleTypes.BLOCK, s.ground()), true, s.at().x, s.at().y + (s.block() ? 0.6 : 0.05),
                    s.at().z, 1, 0.1, 0, 0.1, 0);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        note(event.getPlayer(), event.getPos(), event.getState());
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer p)
            note(p, event.getPos(), event.getPlacedBlock());
    }

    private static void note(net.minecraft.world.entity.player.Player who, BlockPos at, BlockState state) {
        if (who.level().isClientSide)
            return;
        DISTURBED.addLast(new Disturbed(at.immutable(), state, who.level().getGameTime()));
        if (DISTURBED.size() > DISTURBED_MAX)
            DISTURBED.pollFirst();
    }
}
