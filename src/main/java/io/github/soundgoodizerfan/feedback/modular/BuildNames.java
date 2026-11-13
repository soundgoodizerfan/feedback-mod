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
 * The retroactive name of a build (spec 3, naming), for every type but the gun (which has {@code GunDesignation}). A build has no fixed list
 * of kinds, so it is named after it is made: the first of the type's ordered {@code designations} ({@link ModularType.Designation}) whose
 * conditions hold of the wearing part's bands, its variant and the roles filled, else an automatic name in the grammar
 * <em>size, variant, material, designation or type</em> ("Long Scale Iron Chestplate", "Wide Serrated Iron Sword"). A word a designation's rule already requires is dropped from the automatic name (a designation that
 * needs the {@code long} band never says "long" twice), as is any a designation lists under {@code implies}. A name is a signpost, never a
 * bonus: what the piece does comes from its choices and their traits, so archetypes grant no set effect.
 *
 * <p>A rule's {@code is} keys are the names of an axis of the wearing part ({@code width: wide|narrow}), {@code variant}, or the name of a role
 * filled on the piece ({@code collar: fitted}). A value may list alternatives with {@code |}.
 */
public final class BuildNames {

    /** The value a filled addon role has among a build's facts. */
    public static final String FITTED = "fitted";

    private BuildNames() {
    }

    /** Whether {@code type} names its builds this way: it lists designations, or its wearing part has axes whose bands make size words. */
    public static boolean names(ModularType type) {
        if (type.family() == ModularType.Family.GUN)
            return false;
        ModularType.Role wear = type.roles().get(type.wearRole());
        return !type.names().designations().isEmpty() || wear != null && wear.axes().values().stream().anyMatch(a -> !a.bands().isEmpty());
    }

    /** The facts a designation rule reads: the plate's axis bands, its variant, the frame it is built into, and the roles filled. */
    public static Map<String, String> facts(ModularDesign design, ModularType type) {
        Map<String, String> facts = new LinkedHashMap<>();
        ModularDesign.Part plate = design.part(type.wearRole());
        ModularType.Role role = type.roles().get(type.wearRole());
        if (plate != null && role != null) {
            for (Map.Entry<String, ModularType.Axis> a : role.axesFor(plate).entrySet()) {
                ModularType.Band band = a.getValue().band(plate.axis(a.getKey(), a.getValue().def()));
                if (band != null)
                    facts.put(a.getKey(), band.name());
            }
            facts.put("variant", plate.variant());
            // A slot filled with another material than the part's own reads as a fitted layer (a bone back on a limb).
            plate.measures().slots().keySet().forEach(n -> facts.put(n, FITTED));
            if (role.striker())
                facts.put("face", ModularHeads.kindOf(plate, role));
        }
        // The body the wearing part is built into (an electric gun's receiver) gives the build's frame.
        ModularDesign.Part body = design.part(type.primary());
        if (body != null && !type.primary().equals(type.wearRole()) && !body.variant().isEmpty())
            facts.put("frame", body.variant());
        for (ModularDesign.Part p : design.parts()) {
            ModularType.Role r = type.roles().get(p.role());
            if (r != null && !p.role().equals(type.wearRole()))
                facts.put(r.name(), FITTED);
            // A pulse gun's seat is named by its variant, its power tier by the band of its tier axis.
            if (r != null && r.name().equals(ElectricGunSpec.SEAT)) {
                facts.put("seat", r.variant(p).map(ModularType.Variant::name).orElse(""));
                ModularType.Axis tier = r.axes().get("tier");
                ModularType.Band band = tier == null ? null : tier.band(p.axis("tier", tier.def()));
                if (band != null)
                    facts.put("tier", band.name());
            }
        }
        return facts;
    }

    /** The designation of the build, if a rule of the type holds of it. */
    public static Optional<ModularType.Designation> designation(ModularDesign design, ModularType type) {
        Map<String, String> facts = facts(design, type);
        for (ModularType.Designation d : type.names().designations()) {
            boolean ok = d.isHolds(facts);
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
            for (Map.Entry<String, ModularType.Axis> a : role.axesFor(plate).entrySet()) {
                ModularType.Band band = a.getValue().band(plate.axis(a.getKey(), a.getValue().def()));
                if (band != null && !band.name().equals("normal") && !dropped.contains(band.name()))
                    out.append(Component.translatable("modular.feedback.band." + band.name())).append(" ");
            }
        // A pulse gun's Light and Heavy power tiers are size words too; Standard says nothing.
        String tierWord = BuildNames.facts(design, type).get("tier");
        if (tierWord != null && !tierWord.equals("standard") && !dropped.contains(tierWord))
            out.append(Component.translatable("modular.feedback.band." + tierWord)).append(" ");
        // A shape that is an adjective stands beside the type's name; one that names the whole thing (a pitchfork) stands for it.
        boolean adjective = type.family() == ModularType.Family.ARMOUR || role != null && role.variant(plate).map(ModularType.Variant::adjective).orElse(false);
        Component shape = null;
        if (!plate.variant().isEmpty() && !dropped.contains(plate.variant())) {
            Component word = Component.translatable("modular.feedback.archetype." + plate.variant());
            if (adjective)
                out.append(word).append(" ");
            else
                shape = word;
        }
        MutableComponent material = Component.translatable("material." + plate.material().getNamespace() + "." + plate.material().getPath());
        Component noun = named.<Component>map(d -> Component.translatable("armour.feedback.designation." + d.name())).orElse(shape != null ? shape : typeName);
        // A hard piece set into the wearing part (a tip, studs) names the build as it names a plain one.
        for (ModularDesign.Part p : design.parts()) {
            ModularType.Role r = type.roles().get(p.role());
            if (r != null && r.set())
                return Optional.of(Component.translatable("item.feedback.modular_name." + r.name(),
                        Component.translatable("material." + p.material().getNamespace() + "." + p.material().getPath()),
                        out.append(material), noun));
        }
        out.append(material).append(" ").append(noun);
        return Optional.of(out);
    }
}
