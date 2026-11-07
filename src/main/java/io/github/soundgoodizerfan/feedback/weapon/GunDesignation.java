// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTraits;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The real-world name of a gun (spec 8.4): the first of the type's ordered {@code designations}
 * ({@link ModularType.Designation}) whose conditions hold of the gun's variant, action, feed, sight, barrel,
 * calibre class and traits. A revolver is a handgun with a cylinder, a sawn-off a short double shotgun. Nothing in
 * play depends on it; it is how a player and the wiki call the thing, read by a rule exactly as an adjective is
 * read off a figure.
 */
public final class GunDesignation {

    /** A bore this wide or wider takes a shell: the line between a rifle and a shotgun, a pistol and a shot pistol. */
    private static final int WIDE_BORE_MM = 14;

    private GunDesignation() {
    }

    public static Optional<String> of(ItemStack stack, GunSpec spec) {
        var design = ModularStacks.design(stack);
        ModularType type = design == null ? null : ModularTypes.known().get(design.type());
        if (type == null)
            return Optional.empty();
        Set<String> traits = ModularTraits.allOf(stack).stream().map(ModularTraits.Active::id).collect(Collectors.toSet());
        return of(type, spec, traits);
    }

    /** As {@link #of(ItemStack, GunSpec)}, from the facts. */
    public static Optional<String> of(ModularType type, GunSpec spec, Set<String> traits) {
        int bore = Math.round(spec.barrel().boreMm());
        Map<String, String> facts = Map.of(
                "variant", spec.layout().type().getSerializedName(),
                "action", spec.layout().action().getSerializedName(),
                "feed", spec.layout().feed().getSerializedName(),
                "sight", spec.sight().name().toLowerCase(Locale.ROOT),
                "barrel", spec.barrelBand().name().toLowerCase(Locale.ROOT),
                "calibre", GunCalibres.nearest(bore).name(),
                "bore", String.valueOf(bore),
                // What throws the round and, for a chamber, how its bore is finished; and whether the bore is wide enough to take a shell.
                "finish", spec.launcher() != GunSpec.Launcher.CHAMBER ? spec.launcher().name().toLowerCase(Locale.ROOT)
                        : traits.contains("gun_rifling") ? "rifled" : "smooth",
                "gauge", bore >= WIDE_BORE_MM ? "wide" : "narrow");
        for (ModularType.Designation d : type.names().designations()) {
            boolean ok = d.traits().stream().allMatch(traits::contains) && d.without().stream().noneMatch(traits::contains);
            for (Map.Entry<String, String> e : d.is().entrySet())
                ok &= java.util.Arrays.asList(e.getValue().split("\\|")).contains(facts.getOrDefault(e.getKey(), ""));
            if (ok)
                return Optional.of(d.name());
        }
        return Optional.empty();
    }
}
