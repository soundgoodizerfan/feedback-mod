// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.thaumaturge;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapability;

/**
 * Reads and draws off the essentia stored in a Thaumaturge block (a reservoir, a jar) beside ours, through its public storage capability, and
 * reads nothing and draws nothing when the mod is absent. Method handles, as with {@link ThaumaturgeFlux}: no Thaumaturge type appears in a
 * signature here.
 *
 * <p>Taken from Thaumaturge (source-available, publishing barred, dormancy converts it to MIT): the name of the public {@code EssentiaCapabilities.STORAGE}
 * capability and the signatures of {@code IEssentiaStorage.contents/extract} and {@code AspectList.totalAmount/aspects}, as a reader of its source.
 * Rejected: its code, assets and values.
 */
public final class ThaumaturgeEssentia {

    private static boolean ready;
    private static boolean usable;
    private static BlockCapability<Object, Direction> storage;
    private static MethodHandle contents;
    private static MethodHandle total;
    private static MethodHandle aspects;
    private static MethodHandle extract;

    private ThaumaturgeEssentia() {
    }

    public static boolean present() {
        init();
        return usable;
    }

    /** The essentia stored in the block at {@code pos}, as seen from {@code face}; -1 when it stores none (not a store, or no mod). */
    public static int stored(Level level, BlockPos pos, Direction face) {
        if (!present())
            return -1;
        try {
            Object s = level.getCapability(storage, pos, face);
            return s == null ? -1 : (int) total.invoke(contents.invoke(s));
        } catch (Throwable t) {
            return fail(t);
        }
    }

    /** Draws up to {@code amount} essentia out of the store at {@code pos}, from the first aspect that gives any; returns what came out. */
    public static int draw(Level level, BlockPos pos, Direction face, int amount) {
        if (!present() || amount <= 0)
            return 0;
        try {
            Object s = level.getCapability(storage, pos, face);
            if (s == null)
                return 0;
            @SuppressWarnings("unchecked")
            Set<Holder<?>> held = (Set<Holder<?>>) aspects.invoke(contents.invoke(s));
            for (Holder<?> aspect : held) {
                int out = (int) extract.invoke(s, aspect, amount, false);
                if (out > 0)
                    return out;
            }
            return 0;
        } catch (Throwable t) {
            fail(t);
            return 0;
        }
    }

    /** What {@link #drawAspect} took: the aspect's path (such as {@code ignis}) and how much. */
    public record Drawn(String aspect, int amount) {
    }

    /**
     * As {@link #draw}, but naming the aspect it took, and only from an aspect {@code wanted} accepts (a path such as {@code aqua}); null when
     * nothing wanted came out. The adapter uses it to turn each aspect into its own gas and to leave every other aspect untouched.
     */
    public static Drawn drawAspect(Level level, BlockPos pos, Direction face, int amount, java.util.function.Predicate<String> wanted) {
        if (!present() || amount <= 0)
            return null;
        try {
            Object s = level.getCapability(storage, pos, face);
            if (s == null)
                return null;
            @SuppressWarnings("unchecked")
            Set<Holder<?>> held = (Set<Holder<?>>) aspects.invoke(contents.invoke(s));
            for (Holder<?> aspect : held) {
                String path = aspect.unwrapKey().map(k -> k.location().getPath()).orElse("");
                if (!wanted.test(path))
                    continue;
                int out = (int) extract.invoke(s, aspect, amount, false);
                if (out > 0)
                    return new Drawn(path, out);
            }
            return null;
        } catch (Throwable t) {
            fail(t);
            return null;
        }
    }

    private static int fail(Throwable t) {
        Feedback.LOGGER.warn("Thaumaturge essentia binding failed, turned off: {}", t.toString());
        usable = false;
        return -1;
    }

    @SuppressWarnings("unchecked")
    private static synchronized void init() {
        if (ready)
            return;
        ready = true;
        if (!ModList.get().isLoaded("thaumaturge"))
            return;
        try {
            Class<?> storageClass = Class.forName("com.leclowndu93150.thaumaturge.api.essentia.IEssentiaStorage");
            Class<?> listClass = Class.forName("com.leclowndu93150.thaumaturge.api.aspect.AspectList");
            Class<?> caps = Class.forName("com.leclowndu93150.thaumaturge.api.essentia.EssentiaCapabilities");
            storage = (BlockCapability<Object, Direction>) caps.getField("STORAGE").get(null);
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            contents = lookup.findVirtual(storageClass, "contents", MethodType.methodType(listClass))
                    .asType(MethodType.methodType(Object.class, Object.class));
            extract = lookup.findVirtual(storageClass, "extract", MethodType.methodType(int.class, Holder.class, int.class, boolean.class))
                    .asType(MethodType.methodType(int.class, Object.class, Object.class, int.class, boolean.class));
            total = lookup.findVirtual(listClass, "totalAmount", MethodType.methodType(int.class))
                    .asType(MethodType.methodType(int.class, Object.class));
            aspects = lookup.findVirtual(listClass, "aspects", MethodType.methodType(Set.class))
                    .asType(MethodType.methodType(Set.class, Object.class));
            usable = true;
        } catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
            Feedback.LOGGER.warn("Thaumaturge is installed but its essentia API did not match, essentia compat is off: {}", e.toString());
        }
    }
}
