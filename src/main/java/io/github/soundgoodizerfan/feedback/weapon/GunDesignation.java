/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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

    private GunDesignation() {
    }

    public static Optional<String> of(ItemStack stack, GunSpec spec) {
        var design = ModularStacks.design(stack);
        ModularType type = design == null ? null : ModularTypes.known().get(design.type());
        if (type == null)
            return Optional.empty();
        Set<String> traits = ModularTraits.allOf(stack).stream().map(ModularTraits.Active::id).collect(Collectors.toSet());
        ModularDesign.Part barrel = design.part("B");
        String calibre = barrel == null ? "" : type.roles().get("B").variant(barrel).map(ModularType.Variant::name).orElse("");
        return of(type, spec, calibre, traits);
    }

    /** As {@link #of(ItemStack, GunSpec)}, from the facts. */
    public static Optional<String> of(ModularType type, GunSpec spec, String calibre, Set<String> traits) {
        Map<String, String> facts = Map.of(
                "variant", spec.layout().type().getSerializedName(),
                "action", spec.layout().action().getSerializedName(),
                "feed", spec.layout().feed().getSerializedName(),
                "sight", spec.sight().name().toLowerCase(Locale.ROOT),
                "barrel", spec.barrelBand().name().toLowerCase(Locale.ROOT),
                "calibre", calibre);
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
