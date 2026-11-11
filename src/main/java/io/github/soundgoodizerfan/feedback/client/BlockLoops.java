// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Looping sounds that follow a block entity, driven by what the client already knows of it (its
 * synced speed), so no packet exists for sound.
 * <p>
 * The engine is SummerCraft's {@code CarSounds}: four recordings per family, equal-power crossfaded
 * either side of the current revs and pitched by them, so it runs smoothly from idle to the
 * limiter. Taken whole; not taken: starter, turbo, supercharger and backfire, which need parts
 * Feedback's engine does not have. Shafts and tires have no SummerCraft counterpart and borrow
 * vanilla's rolling-cart rumble until recordings replace it.
 * <p>
 * A loop starts when a sync shows the thing moving ({@link #ensure}-style calls from the block
 * entities' client sync) and stops itself once it has faded to nothing, so a stopped machine is
 * silent and costs nothing.
 */
public final class BlockLoops {

    private static final List<Loop> ACTIVE = new ArrayList<>();
    /** A run of shafts is one noise, not one per block: a whir within this many blocks of another is skipped. */
    private static final double WHIR_SPACING_SQ = 5 * 5;

    /** A shaft, cog, gearbox or other passive turning part: a rumble that rises with speed. */
    public static void whir(RotationNode node) {
        double rpm = Math.abs(node.getRpm().value());
        if (rpm < 1 || has(node, "whir"))
            return;
        for (Loop other : ACTIVE)
            if (other.tag.equals("whir") && other.be.getLevel() == node.getLevel()
                    && other.be.getBlockPos().distSqr(node.getBlockPos()) < WHIR_SPACING_SQ)
                return;
        start(new Loop(node, "whir", SoundEvents.MINECART_RIDING,
                () -> Mth.clamp(Math.abs(node.getRpm().value()) / 150, 0, 1) * 0.3,
                () -> Mth.clamp(0.5 + Math.abs(node.getRpm().value()) / 200, 0.5, 1.5)));
    }

    /** A running engine: every layer that has weight at the current revs. */
    public static void engine(EngineBlockEntity engine) {
        if (engine.design() == null)
            return;
        for (int layer = 0; layer < FSounds.ENGINE_LAYER_RPMS.length; layer++) {
            int l = layer;
            String tag = "engine" + l;
            if (has(engine, tag) || weight(l, rpm(engine)) < 0.01 || !engine.isEngaged())
                continue;
            start(new Loop(engine, tag, FSounds.engine(engine.design().cylinders(), l),
                    () -> engine.isEngaged() ? (0.45 + rpm(engine) / 8000) * weight(l, rpm(engine)) * 1.2 : 0,
                    () -> Mth.clamp(rpm(engine) / FSounds.ENGINE_LAYER_RPMS[l], 0.5, 2)));
        }
    }


    private static double rpm(EngineBlockEntity engine) {
        return Math.max(Math.abs(engine.getRpm().value()), 300);
    }

    /** Equal-power crossfade between the two recordings either side of {@code rpm}. */
    private static double weight(int layer, double rpm) {
        float[] rpms = FSounds.ENGINE_LAYER_RPMS;
        int last = rpms.length - 1;
        if (rpm <= rpms[0])
            return layer == 0 ? 1 : 0;
        if (rpm >= rpms[last])
            return layer == last ? 1 : 0;
        for (int i = 0; i < last; i++) {
            if (rpm <= rpms[i + 1]) {
                double t = Math.log(rpm / rpms[i]) / Math.log(rpms[i + 1] / rpms[i]);
                return layer == i ? Math.cos(t * Mth.HALF_PI) : layer == i + 1 ? Math.sin(t * Mth.HALF_PI) : 0;
            }
        }
        return 0;
    }

    private static boolean has(BlockEntity be, String tag) {
        ACTIVE.removeIf(l -> l.isStopped() || Minecraft.getInstance().level != l.be.getLevel());
        for (Loop l : ACTIVE)
            if (l.be == be && l.tag.equals(tag))
                return true;
        return false;
    }

    private static void start(Loop loop) {
        ACTIVE.add(loop);
        Minecraft.getInstance().getSoundManager().play(loop);
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        final BlockEntity be;
        final String tag;
        private final DoubleSupplier targetVolume;
        private final DoubleSupplier targetPitch;

        Loop(BlockEntity be, String tag, SoundEvent event, DoubleSupplier volume, DoubleSupplier pitch) {
            super(event, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.be = be;
            this.tag = tag;
            this.targetVolume = volume;
            this.targetPitch = pitch;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01f;
            follow();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (be.isRemoved() || be.getLevel() == null) {
                stop();
                return;
            }
            follow();
            float target = (float) targetVolume.getAsDouble();
            volume += (target - volume) * 0.3f;
            pitch = (float) targetPitch.getAsDouble();
            if (target < 0.01f && volume < 0.02f)
                stop();
        }

        /** Block centre, projected out of a vehicle's plot so a moving car is heard where it is. */
        private void follow() {
            Vec3 at = Vec3.atCenterOf(be.getBlockPos());
            if (be.getLevel() != null)
                at = dev.ryanhcode.sable.companion.SableCompanion.INSTANCE.projectOutOfSubLevel(be.getLevel(), at);
            x = at.x;
            y = at.y;
            z = at.z;
        }
    }

    private BlockLoops() {
    }
}
