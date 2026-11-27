// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.thaumaturge;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/**
 * Reads and moves a chunk's flux in the Thaumaturge mod, when it is installed, and reads 0 and moves nothing when not.
 *
 * <p>Thaumaturge is not published as a jar, so Feedback has no compile dependency on it; its documented addon entry
 * point, the static methods of {@code api.aura.AuraHelper}, is reached through method handles looked up once. A missing
 * class or a changed signature turns the whole binding off with one log line rather than an error at each call: flux
 * then reads as none anywhere. No Thaumaturge type appears in any signature here.
 *
 * <p>Taken from Thaumaturge (source-available, publishing barred, dormancy converts it to MIT): the names and signatures of six public
 * {@code AuraHelper} methods, as a reader of its source. Rejected: its code, assets and values.
 */
public final class ThaumaturgeFlux {

    private static final String HELPER = "com.leclowndu93150.thaumaturge.api.aura.AuraHelper";

    private static boolean ready;
    private static boolean usable;
    private static MethodHandle saturation;
    private static MethodHandle flux;
    private static MethodHandle vis;
    private static MethodHandle base;
    private static MethodHandle drain;
    private static MethodHandle add;

    private ThaumaturgeFlux() {
    }

    /** Whether the mod is installed and its aura API answered. */
    public static boolean present() {
        init();
        return usable;
    }

    /** The chunk's flux over its base aura, 0 with no mod or no record: 1 is a chunk at its limit. */
    public static float saturation(Level level, BlockPos pos) {
        if (!present())
            return 0f;
        try {
            return (float) saturation.invoke(level, pos);
        } catch (Throwable t) {
            return fail(t, 0f);
        }
    }

    /** The chunk's flux, 0 with no mod. */
    public static float flux(Level level, BlockPos pos) {
        if (!present())
            return 0f;
        try {
            return (float) flux.invoke(level, pos);
        } catch (Throwable t) {
            return fail(t, 0f);
        }
    }

    /** The chunk's vis as a share of its base aura, 0 with no mod or no base: 1 is a chunk full of aura. */
    public static float visShare(Level level, BlockPos pos) {
        if (!present())
            return 0f;
        try {
            float b = (float) base.invoke(level, pos);
            return b <= 0f ? 0f : Math.max(0f, (float) vis.invoke(level, pos) / b);
        } catch (Throwable t) {
            return fail(t, 0f);
        }
    }

    /** Takes up to {@code amount} flux out of the chunk and returns how much came out (or would, when {@code simulate}). */
    public static float drain(Level level, BlockPos pos, float amount, boolean simulate) {
        if (!present())
            return 0f;
        try {
            return (float) drain.invoke(level, pos, amount, simulate);
        } catch (Throwable t) {
            return fail(t, 0f);
        }
    }

    /** Puts {@code amount} flux into the chunk. */
    public static void add(Level level, BlockPos pos, float amount) {
        if (!present())
            return;
        try {
            add.invoke(level, pos, amount);
        } catch (Throwable t) {
            fail(t, 0f);
        }
    }

    private static float fail(Throwable t, float value) {
        Feedback.LOGGER.warn("Thaumaturge flux binding failed, turned off: {}", t.toString());
        usable = false;
        return value;
    }

    private static synchronized void init() {
        if (ready)
            return;
        ready = true;
        if (!ModList.get().isLoaded("thaumaturge"))
            return;
        try {
            Class<?> helper = Class.forName(HELPER);
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            MethodType read = MethodType.methodType(float.class, Level.class, BlockPos.class);
            saturation = lookup.findStatic(helper, "getFluxSaturation", read);
            flux = lookup.findStatic(helper, "getFlux", read);
            vis = lookup.findStatic(helper, "getVis", read);
            base = lookup.findStatic(helper, "getAuraBase", MethodType.methodType(int.class, Level.class, BlockPos.class));
            drain = lookup.findStatic(helper, "drainFlux",
                    MethodType.methodType(float.class, Level.class, BlockPos.class, float.class, boolean.class));
            add = lookup.findStatic(helper, "addFlux",
                    MethodType.methodType(void.class, Level.class, BlockPos.class, float.class));
            usable = true;
        } catch (ReflectiveOperationException | LinkageError e) {
            Feedback.LOGGER.warn("Thaumaturge is installed but its aura API did not match, flux compat is off: {}", e.toString());
        }
    }
}
