// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.thaumaturge;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;

/**
 * Draws vis from a Thaumaturge relay network as a consumer, when the mod is installed; draws nothing when not.
 *
 * <p>Same shape as {@link ThaumaturgeFlux}: no compile dependency, the static
 * {@code api.aura.VisRelayHelper.drainCentivis(ServerLevel, BlockPos, ResourceKey, int, boolean)} reached through a method handle
 * looked up once, a mismatch turning the binding off with one log line. The primal is named by a vanilla {@link ResourceKey} of the
 * mod's aspect registry id ({@code thaumaturge:aspect}), so no Thaumaturge type appears here. The call finds a linked relay in range
 * of the consumer's position, walks to its node and drains the node's buffer; it returns 0 with no relay in range.
 *
 * <p>Taken from Thaumaturge (source-available, publishing barred, MIT on dormancy): the name and signature of that one public method, and the
 * registry id of its aspects, as a reader of its source. Rejected: its code, values and its flat vis-to-energy rate.
 */
public final class ThaumaturgeVis {

    private static final String HELPER = "com.leclowndu93150.thaumaturge.api.aura.VisRelayHelper";
    private static final ResourceKey<? extends Registry<Object>> ASPECTS =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("thaumaturge", "aspect"));

    private static boolean ready;
    private static boolean usable;
    private static MethodHandle drain;

    private ThaumaturgeVis() {
    }

    /** Whether the mod is installed and its relay API answered. */
    public static boolean present() {
        init();
        return usable;
    }

    /** Centivis drained (or drainable when {@code simulate}) of the primal {@code aspect} (a path such as {@code ignis}); 0 with no mod or no relay. */
    public static int drainCentivis(ServerLevel level, BlockPos consumer, String aspect, int amount, boolean simulate) {
        if (!present())
            return 0;
        try {
            ResourceKey<Object> primal = ResourceKey.create(ASPECTS, ResourceLocation.fromNamespaceAndPath("thaumaturge", aspect));
            return (int) drain.invoke(level, consumer, primal, amount, simulate);
        } catch (Throwable t) {
            Feedback.LOGGER.warn("Thaumaturge vis drain failed, turning it off: {}", t.toString());
            usable = false;
            return 0;
        }
    }

    private static synchronized void init() {
        if (ready)
            return;
        ready = true;
        if (!ModList.get().isLoaded("thaumaturge"))
            return;
        try {
            Class<?> helper = Class.forName(HELPER);
            drain = MethodHandles.publicLookup().findStatic(helper, "drainCentivis",
                    MethodType.methodType(int.class, ServerLevel.class, BlockPos.class, ResourceKey.class, int.class, boolean.class));
            usable = true;
        } catch (ReflectiveOperationException | LinkageError e) {
            Feedback.LOGGER.warn("Thaumaturge is installed but its relay API did not match, vis compat is off: {}", e.toString());
        }
    }
}
