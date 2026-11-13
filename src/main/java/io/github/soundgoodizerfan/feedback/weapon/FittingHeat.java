// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A fitting takes the barrel's heat (a stamp the gun already keeps). Past its metal's service temperature it softens and what it
 * did fades ({@link FittingPhysics#soundness}); at its melting point it runs off the gun, and the part is gone until refitted.
 * No state of its own: the temperature is the barrel's, read when the shot is aimed and booked.
 */
public final class FittingHeat {

    private FittingHeat() {
    }

    /** Each gun's fittings' physics, computed once per spec: {@link #soften} runs on every aimed read. Cleared with {@link GunSpec}'s own cache. */
    private static final Map<GunSpec, List<FittingPhysics>> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    static void clearCache() {
        CACHE.clear();
    }

    /** {@code f} with what the fittings that have gone soft no longer do: their cut of the report, their extra heat, their fins' shedding. */
    public static GunFigures soften(Player shooter, ItemStack gun, GunFigures f) {
        var spec = GunSpec.of(gun);
        if (spec.isEmpty() || spec.get().fittings().isEmpty())
            return f;
        float tu = GunCondition.temperature(gun, shooter.level()).value();
        for (FittingPhysics fp : CACHE.computeIfAbsent(spec.get(), g -> List.copyOf(g.fitted(MaterialTable.get().materials()).values()))) {
            float gone = 1f - fp.soundness(tu);
            if (gone <= 0f)
                continue;
            f = f.withReport((float) Math.pow(1f / fp.blast(), gone)).withHeat((float) Math.pow(1f / fp.heat(), gone))
                    .withLeak((float) Math.pow(1f / fp.shed(), gone));
        }
        return f;
    }

    /**
     * Fittings whose metal the barrel's heat has taken past melting are taken off the gun. Returns the fittings lost, so the caller can
     * say so; the design is rewritten and the gun rebaked.
     */
    public static List<GunOption> melt(ItemStack gun, Level level) {
        var spec = GunSpec.of(gun);
        ModularDesign design = ModularStacks.design(gun);
        if (spec.isEmpty() || design == null || spec.get().fittings().isEmpty())
            return List.of();
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        float tu = GunCondition.temperature(gun, level).value();
        List<GunOption> lost = new ArrayList<>();
        for (var e : spec.get().fitted(materials).entrySet())
            if (tu >= e.getValue().meltTu())
                GunOptions.find(e.getKey()).ifPresent(o -> lost.add(o));
        if (lost.isEmpty())
            return lost;
        gun.set(FModular.DESIGN.get(), design.withParts(design.parts().stream().filter(p -> lost.stream().noneMatch(o -> o.form().equals(p.kind()))).toList()));
        ModularStacks.rebake(gun);
        return lost;
    }
}
