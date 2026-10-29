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
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A material file as label and figure rows, grouped the way the file is, for the Material Probe. Absent
 * figures are skipped, because absent means nobody has measured it (see {@link Material}), and a missing
 * row says so better than "unknown" would. Labels and hover lines are lang keys
 * {@code screen.feedback.probe.row.<key>} and {@code .tip}.
 */
final class MaterialDescription {

    /** The probe's tabs, in order; the tab label is {@code screen.feedback.probe.tab.<name>}. */
    static final String[] TABS = {"basics", "heat", "force", "melt", "sort", "other"};

    /** One row: its tab, label, figure and hover line. */
    record Row(int tab, Component label, String figure, Component tip) {}

    private MaterialDescription() {}

    static List<Row> of(ResourceLocation id, Material m, String form) {
        List<Row> out = new ArrayList<>();
        row(out, 0, "form", form.replace('_', ' '));
        m.kind().ifPresent(k -> row(out, 0, "kind", lower(k)));
        if (!m.traits().isEmpty())
            row(out, 0, "traits", String.join(", ", m.traits().stream().map(MaterialDescription::lower).toList()));
        m.perks().forEach((perk, level) -> out.add(new Row(0, Component.literal(lower(perk).replace('_', ' ')), String.valueOf(level),
                Component.translatable("screen.feedback.probe.row.perk.tip"))));
        if (m.density().muPerMb() > 0)
            row(out, 0, "density", num(m.density().muPerMb()) + " Mu/mB");
        row(out, 0, "hardness", num(m.hardness().value()));
        if (m.minTemperature().value() > 0)
            row(out, 0, "work_min", num(m.minTemperature().value()) + " Tu");
        if (m.maxTemperature().value() < Float.MAX_VALUE)
            row(out, 0, "work_max", num(m.maxTemperature().value()) + " Tu");

        Material.Thermal t = m.thermal();
        t.meltingPoint().ifPresent(v -> row(out, 1, "melt", num(v.value()) + " Tu"));
        t.boilingPoint().ifPresent(v -> row(out, 1, "boil", num(v.value()) + " Tu"));
        t.annealTemperature().ifPresent(v -> row(out, 1, "anneal", num(v.value()) + " Tu"));
        t.serviceTemperature().ifPresent(v -> row(out, 1, "service", num(v.value()) + " Tu"));
        t.oxidationTemperature().ifPresent(v -> row(out, 1, m.kind().filter(k -> k == Material.Kind.ORGANIC).isPresent() ? "burns" : "scales",
                num(v.value()) + " Tu"));
        t.coolingRate().ifPresent(v -> row(out, 1, "cooling", num(v.tuPerTick()) + " Tu/t"));
        t.conductivity().ifPresent(v -> row(out, 1, "conductivity", num(v.workPerTickPerTu())));
        m.combustion().ifPresent(c -> c.flameTemperature().ifPresent(f -> c.burnTicks().ifPresent(n ->
                row(out, 1, "flame", num(f.value()) + " Tu, " + n + " t"))));
        m.molten().freezeTicks().ifPresent(n -> row(out, 1, "freeze", n + " t"));

        Material.Mechanical k = m.mechanical();
        k.toughness().ifPresent(v -> row(out, 2, v.value() < m.hardness().value() ? "toughness_brittle" : "toughness", num(v.value())));
        k.ductility().ifPresent(v -> row(out, 2, "ductility", String.valueOf(v)));
        k.workHardening().ifPresent(v -> row(out, 2, "work_hardening", num(v.value())));
        k.yieldStrength().ifPresent(v -> row(out, 2, "yield", num(v.value())));
        k.friction().ifPresent(v -> row(out, 2, "friction", num(v)));
        k.abrasiveness().ifPresent(v -> row(out, 2, "abrasiveness", percent(v)));
        row(out, 2, "quench", Component.translatable(k.hardenable() ? "screen.feedback.probe.hardens" : "screen.feedback.probe.cools").getString());

        m.molten().viscosity().ifPresent(v -> row(out, 3, "viscosity", num(v) + "x water"));
        m.molten().density().ifPresent(v -> row(out, 3, "melt_density", num(v.muPerMb()) + " Mu/mB"));

        Material.Separation s = m.separation();
        row(out, 4, "magnet", Component.translatable(s.magnetic() ? "screen.feedback.probe.pulled" : "screen.feedback.probe.ignored").getString());
        s.curieTemperature().filter(v -> s.magnetic()).ifPresent(v -> row(out, 4, "curie", num(v.value()) + " Tu"));
        s.reflectivity().ifPresent(v -> row(out, 4, "reflectivity", percent(v)));

        m.ampacity().ifPresent(v -> row(out, 5, "ampacity", num(v) + "x copper"));
        m.electrical().drop().ifPresent(v -> row(out, 5, "drop", num(v) + " V"));
        m.electrical().electrodePotential().ifPresent(v -> row(out, 5, "electrode", num(v) + " V"));
        m.electrical().insulation().ifPresent(b -> row(out, 5, "insulation", b.name()));
        m.corrosionResistance().ifPresent(v -> row(out, 5, "corrosion", percent(v)));
        m.propellant().ifPresent(p -> {
            row(out, 5, "propellant", Component.translatable(p.drive() == Material.Propellant.Drive.COIL
                    ? "screen.feedback.probe.coil" : "screen.feedback.probe.powder").getString());
            row(out, 5, "peak", num(p.peakPressure().value()) + " Pu");
            row(out, 5, "burn_rate", num(p.burnRate()));
            row(out, 5, p.drive() == Material.Propellant.Drive.COIL ? "quench_at" : "cook_off", num(p.ignitionTemperature().value()) + " Tu");
            row(out, 5, "oxygen", Component.translatable(p.oxygenBalance() > 0 ? "screen.feedback.probe.spare"
                    : p.oxygenBalance() < 0 ? "screen.feedback.probe.short" : "screen.feedback.probe.own").getString());
        });
        if (!m.composition().isEmpty())
            row(out, 5, "alloy", String.join(", ", m.constituents(id).entrySet().stream()
                    .map(e -> percent(e.getValue()) + " " + e.getKey().getPath().replace('_', ' ')).toList()));
        if (!m.forms().isEmpty())
            row(out, 5, "forms", String.join(", ", m.forms().keySet().stream().map(f -> f.replace('_', ' ')).toList()));
        if (!m.exclude().isEmpty())
            row(out, 5, "excluded", String.join(", ", m.exclude().stream().map(f -> f.replace('_', ' ')).toList()));
        return out;
    }

    private static void row(List<Row> out, int tab, String key, String figure) {
        out.add(new Row(tab, Component.translatable("screen.feedback.probe.row." + key),
                figure, Component.translatable("screen.feedback.probe.row." + key + ".tip")));
    }

    private static String lower(Enum<?> e) {
        return e.name().toLowerCase(Locale.ROOT);
    }

    private static String percent(float fraction) {
        return Math.round(fraction * 100f) + "%";
    }

    private static String num(float v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.2f", v).replaceAll("0+$", "");
    }
}
