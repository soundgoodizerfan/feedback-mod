// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * The retroactive name of an armour build (spec 3, naming). Armour has no fixed list of kinds, so a build is named after it is made:
 * the first of the type's ordered {@code designations} ({@link ModularType.Designation}) whose conditions hold of the plate's bands,
 * its variant and the roles filled, else an automatic name in the grammar <em>size, variant, material, designation or type</em>
 * ("Long Scale Iron Chestplate"). A word a designation's rule already requires is dropped from the automatic name (a designation that
 * needs the {@code long} band never says "long" twice), as is any a designation lists under {@code implies}. A name is a signpost, never a
 * bonus: what the piece does comes from its choices and their traits, so archetypes grant no set effect.
 *
 * <p>A rule's {@code is} keys are the names of an axis of the plate ({@code width: wide|narrow}), {@code variant}, or the name of a role
 * filled on the piece ({@code collar: fitted}). A value may list alternatives with {@code |}.
 */
public final class ArmourNames {

    /** The value a filled addon role has among a build's facts. */
    public static final String FITTED = "fitted";

    private ArmourNames() {
    }

    /** The facts a designation rule reads: the plate's axis bands, its variant, and the roles filled. */
    public static Map<String, String> facts(ModularDesign design, ModularType type) {
        Map<String, String> facts = new LinkedHashMap<>();
        ModularDesign.Part plate = design.part(type.wearRole());
        ModularType.Role role = type.roles().get(type.wearRole());
        if (plate != null && role != null) {
            for (Map.Entry<String, ModularType.Axis> a : role.axes().entrySet()) {
                ModularType.Band band = a.getValue().band(plate.axis(a.getKey(), a.getValue().def()));
                if (band != null)
                    facts.put(a.getKey(), band.name());
            }
            facts.put("variant", plate.variant());
        }
        for (ModularDesign.Part p : design.parts()) {
            ModularType.Role r = type.roles().get(p.role());
            if (r != null && !p.role().equals(type.wearRole()))
                facts.put(r.name(), FITTED);
        }
        return facts;
    }

    /** The designation of the build, if a rule of the type holds of it. */
    public static Optional<ModularType.Designation> designation(ModularDesign design, ModularType type) {
        Map<String, String> facts = facts(design, type);
        for (ModularType.Designation d : type.names().designations()) {
            boolean ok = true;
            for (Map.Entry<String, String> e : d.is().entrySet())
                ok &= java.util.Arrays.asList(e.getValue().split("\\|")).contains(facts.getOrDefault(e.getKey(), ""));
            if (ok)
                return Optional.of(d);
        }
        return Optional.empty();
    }

    /** The name of the build: its designation if it has one, else the automatic name; empty for a design with no plate. */
    public static Optional<Component> of(ModularDesign design, ModularType type, Component typeName) {
        ModularDesign.Part plate = design.part(type.wearRole());
        if (plate == null)
            return Optional.empty();
        Optional<ModularType.Designation> named = designation(design, type);
        Set<String> dropped = new HashSet<>();
        named.ifPresent(d -> {
            for (String v : d.is().values())
                dropped.addAll(List.of(v.split("\\|")));
            dropped.addAll(d.traits());
        });
        MutableComponent out = Component.empty();
        ModularType.Role role = type.roles().get(type.wearRole());
        // Size words: the bands the plate's axes sit in, but the unremarkable one.
        if (role != null)
            for (Map.Entry<String, ModularType.Axis> a : role.axes().entrySet()) {
                ModularType.Band band = a.getValue().band(plate.axis(a.getKey(), a.getValue().def()));
                if (band != null && !band.name().equals("normal") && !dropped.contains(band.name()))
                    out.append(Component.translatable("modular.feedback.band." + band.name())).append(" ");
            }
        if (!plate.variant().isEmpty() && !dropped.contains(plate.variant()))
            out.append(Component.translatable("modular.feedback.archetype." + plate.variant())).append(" ");
        out.append(Component.translatable("material." + plate.material().getNamespace() + "." + plate.material().getPath())).append(" ");
        out.append(named.<Component>map(d -> Component.translatable("armour.feedback.designation." + d.name())).orElse(typeName));
        return Optional.of(out);
    }
}
