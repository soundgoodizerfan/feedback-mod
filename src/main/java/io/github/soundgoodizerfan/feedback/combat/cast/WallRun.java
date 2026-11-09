// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.combat.gear.CombatGear;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.modular.ModularGlove;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Wall Run: sprint along a wall, held up by it. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class WallRun {

    private WallRun() {
    }

    /** Per runner: the game tick the run ends and how many ticks in a row no wall has been beside it. */
    private static final Map<UUID, long[]> RUNS = new HashMap<>();

    private static boolean wallBeside(ServerPlayer p) {
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            if (!p.level().noCollision(p, p.getBoundingBox().move(new Vec3(Math.cos(a), 0, Math.sin(a)).scale(0.7))))
                return true;
        }
        return false;
    }

    /** Whether the wall beside {@code p} is a soft one claws bite: wood, dirt or leaves. */
    private static boolean softWallBeside(ServerPlayer p) {
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            var state = p.level().getBlockState(net.minecraft.core.BlockPos.containing(p.position().add(Math.cos(a) * 0.9, 0.5, Math.sin(a) * 0.9)));
            if (state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE) || state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL)
                    || state.is(net.minecraft.tags.BlockTags.LEAVES))
                return true;
        }
        return false;
    }

    private static Vec3 along(ServerPlayer p) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        return look.lengthSqr() < 1e-6 ? Vec3.ZERO : look.normalize();
    }

    /** Wall run: with a wall beside you, run along it for most of a second, the wall taking your weight; look steers, crouch drops off. */
    static boolean act(ServerPlayer p) {
        if (p.isPassenger() || !p.isSprinting() && p.onGround() || !wallBeside(p) || along(p).equals(Vec3.ZERO))
            return false;
        // Clawhold: claws bite wood, dirt and leaves, and the run lasts twice as long on them.
        int ticks = Skills.unlocked(p, "clawhold") && ModularGlove.claws(CombatGear.platedGlove(p)) && softWallBeside(p) ? 36 : 18;
        RUNS.put(p.getUUID(), new long[]{p.level().getGameTime() + ticks, 0});
        Vec3 d = along(p);
        p.setDeltaMovement(d.x * 0.6, 0.3, d.z * 0.6);
        p.hurtMarked = true;
        CastFx.spray(p.serverLevel(), ParticleTypes.CLOUD, p.position().add(0, 0.3, 0), 8, 0.2);
        CombatCues.everyone(p, FSounds.ROLL_WHOOSH.get(), FeelTuning.ROLL_VOLUME, 1.3f);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !RUNS.containsKey(p.getUUID()))
            return;
        long[] run = RUNS.get(p.getUUID());
        run[1] = wallBeside(p) ? 0 : run[1] + 1;
        if (p.level().getGameTime() >= run[0] || run[1] > 2 || p.isShiftKeyDown() || p.isPassenger()) {
            RUNS.remove(p.getUUID());
            return;
        }
        Vec3 d = along(p);
        p.setDeltaMovement(d.x * 0.55, 0.02, d.z * 0.55);
        p.hurtMarked = true;
        p.fallDistance = 0f;
        var lv = p.serverLevel();
        lv.sendParticles(CastFx.dust(CastFx.STEEL, 0.9f), p.getX(), p.getY() + 0.1, p.getZ(), 3, 0.15, 0.05, 0.15, 0);
        lv.sendParticles(ParticleTypes.CRIT, p.getX(), p.getY() + 1, p.getZ(), 1, 0.3, 0.4, 0.3, 0);
        if (p.level().getGameTime() % 3 == 0)
            CastFx.sound(lv, p.position(), SoundEvents.STONE_STEP, 0.6f, 1.4f);
    }
}
