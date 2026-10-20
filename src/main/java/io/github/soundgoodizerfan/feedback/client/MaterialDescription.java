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
package io.github.soundgoodizerfan.feedback.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.resources.ResourceLocation;

/**
 * A material file read aloud: what the Material Probe shows instead of the JSON. One sentence per
 * fact, grouped the way the file is. Absent figures are skipped, because absent means nobody has
 * measured it (see {@link Material}), and a skipped line says so better than "unknown" would.
 */
final class MaterialDescription {

    private MaterialDescription() {}

    static List<String> of(ResourceLocation id, Material m, String form) {
        List<String> out = new ArrayList<>();
        String name = name(id);
        String kind = m.kind().map(MaterialDescription::lower).orElse("stuff");
        StringBuilder head = new StringBuilder("This is ").append(form.replace('_', ' ')).append(" of ")
                .append(name).append(", a ").append(kind);
        if (!m.traits().isEmpty())
            head.append(" that is ").append(join(m.traits().stream().map(MaterialDescription::lower).toList()));
        out.add(head.append('.').toString());
        if (m.density().muPerMb() > 0)
            out.add("It weighs " + num(m.density().muPerMb()) + " per litre.");
        out.add("A blow must carry at least " + num(m.hardness().value())
                + " strength to move it, and each blow does its strength divided by that in work.");
        if (m.minTemperature().value() > 0 || m.maxTemperature().value() < Float.MAX_VALUE)
            out.add("It can only be worked " + (m.minTemperature().value() > 0
                    ? "above " + num(m.minTemperature().value()) + " Tu" : "")
                    + (m.minTemperature().value() > 0 && m.maxTemperature().value() < Float.MAX_VALUE ? " and " : "")
                    + (m.maxTemperature().value() < Float.MAX_VALUE
                    ? "below " + num(m.maxTemperature().value()) + " Tu" : "") + ".");

        List<String> heat = new ArrayList<>();
        Material.Thermal t = m.thermal();
        t.meltingPoint().ifPresent(v -> heat.add("It melts at " + num(v.value()) + " Tu."));
        t.boilingPoint().ifPresent(v -> heat.add("The melt boils at " + num(v.value()) + " Tu."));
        t.annealTemperature().ifPresent(v -> heat.add("Heated past " + num(v.value()) + " Tu it forgets being worked."));
        t.serviceTemperature().ifPresent(v -> heat.add("A machine body of it softens and wears above "
                + num(v.value()) + " Tu."));
        t.oxidationTemperature().ifPresent(v -> heat.add("Held in air above " + num(v.value()) + " Tu it "
                + (m.kind().filter(k -> k == Material.Kind.ORGANIC).isPresent() ? "burns." : "scales.")));
        t.coolingRate().ifPresent(v -> heat.add("A loose piece cools " + num(v.tuPerTick()) + " Tu every tick."));
        t.conductivity().ifPresent(v -> heat.add("A one-block wall of it passes heat at " + num(v.workPerTickPerTu()) + "."));
        m.combustion().ifPresent(c -> c.flameTemperature().ifPresent(f -> c.burnTicks().ifPresent(n ->
                heat.add("A piece of it burns at " + num(f.value()) + " Tu for " + n + " ticks."))));
        m.molten().freezeTicks().ifPresent(n -> heat.add("A bucket of the melt holds its melting point for "
                + n + " ticks as it freezes."));
        section(out, "Heat", heat);

        List<String> force = new ArrayList<>();
        Material.Mechanical k = m.mechanical();
        k.toughness().ifPresent(v -> force.add("A single blow over " + num(v.value()) + " strength cracks it"
                + (v.value() < m.hardness().value() ? " -- and that is under its hardness, so it is brittle: "
                + "anything that could move it breaks it." : ".")));
        k.ductility().ifPresent(v -> force.add("It survives " + v + " cold working steps before cracking."));
        k.workHardening().ifPresent(v -> force.add("Each cold step makes it " + num(v.value())
                + " harder until it is annealed."));
        k.yieldStrength().ifPresent(v -> force.add("A part of it bends under " + num(v.value()) + " load."));
        k.friction().ifPresent(v -> force.add("Sliding on it has a friction of " + num(v) + "."));
        k.abrasiveness().ifPresent(v -> force.add("Cutting it spends grit at " + percent(v) + " of the worst rate."));
        force.add(k.hardenable() ? "Quenching hardens it." : "Quenching only cools it.");
        section(out, "Force", force);

        List<String> melt = new ArrayList<>();
        m.molten().viscosity().ifPresent(v -> melt.add("Molten, it flows " + (v < 1f ? "thinner than" : v > 1f
                ? num(v) + " times thicker than" : "like") + " water."));
        m.molten().density().ifPresent(v -> melt.add("The melt weighs " + num(v.muPerMb()) + " per litre."));
        section(out, "Liquid", melt);

        List<String> sort = new ArrayList<>();
        Material.Separation s = m.separation();
        if (s.magnetic())
            sort.add("A magnet pulls it" + s.curieTemperature().map(v -> ", until it passes " + num(v.value())
                    + " Tu and lets go.").orElse("."));
        else
            sort.add("A magnet ignores it.");
        s.reflectivity().ifPresent(v -> sort.add("It reflects " + percent(v) + " of the light on it."));
        section(out, "Sorting", sort);

        List<String> elec = new ArrayList<>();
        m.ampacity().ifPresent(v -> elec.add("A wire of it carries " + num(v) + " times copper's current before it melts."));
        m.electrical().drop().ifPresent(v -> elec.add("A wire loses " + num(v) + " volts per block per amp."));
        m.electrical().electrodePotential().ifPresent(v -> elec.add("As a cell electrode it sits at " + num(v)
                + " volts; a cell makes the difference between its two."));
        section(out, "Electricity", elec);

        m.corrosionResistance().ifPresent(v -> out.add("Water and acid leave it " + percent(v) + " intact."));

        m.propellant().ifPresent(p -> {
            List<String> burn = new ArrayList<>();
            burn.add("It is a propellant that " + (p.drive() == Material.Propellant.Drive.COIL
                    ? "drives a slug through a coil." : "burns to push a bullet."));
            burn.add("A nominal charge makes " + num(p.peakPressure().value()) + " pressure.");
            burn.add("It burns " + num(p.burnRate()) + " of a barrel's length per metre.");
            burn.add((p.drive() == Material.Propellant.Drive.COIL ? "The winding quenches at " : "It cooks off at ")
                    + num(p.ignitionTemperature().value()) + " Tu.");
            burn.add(p.oxygenBalance() > 0 ? "It carries spare oxygen, so it can burn another fuel."
                    : p.oxygenBalance() < 0 ? "It is short of oxygen and needs an oxidizer blended in."
                    : "It burns on its own oxygen.");
            section(out, "Propellant", burn);
        });

        if (!m.composition().isEmpty()) {
            Map<ResourceLocation, Float> shares = m.constituents(id);
            out.add("");
            out.add("It is an alloy of " + join(shares.entrySet().stream()
                    .map(e -> percent(e.getValue()) + " " + name(e.getKey())).toList()) + ".");
        }

        List<String> forms = new ArrayList<>();
        forms.addAll(m.forms().keySet().stream().map(f -> f.replace('_', ' ')).toList());
        if (!forms.isEmpty()) {
            out.add("");
            out.add("Also made as " + join(forms) + ".");
        }
        if (!m.exclude().isEmpty())
            out.add("It does not come as " + join(m.exclude().stream().map(f -> f.replace('_', ' ')).toList()) + ".");
        return out;
    }

    private static void section(List<String> out, String title, List<String> lines) {
        if (lines.isEmpty())
            return;
        out.add("");
        out.add(title + ":");
        out.addAll(lines);
    }

    private static String name(ResourceLocation id) {
        return id.getPath().replace('_', ' ');
    }

    private static String lower(Enum<?> e) {
        return e.name().toLowerCase(Locale.ROOT);
    }

    private static String join(List<String> items) {
        if (items.size() <= 1)
            return String.join("", items);
        return String.join(", ", items.subList(0, items.size() - 1)) + " and " + items.get(items.size() - 1);
    }

    private static String percent(float fraction) {
        return Math.round(fraction * 100f) + "%";
    }

    private static String num(float v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.2f", v).replaceAll("0+$", "");
    }
}
