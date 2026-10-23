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

import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * What a gun adds to the modular tooltip ({@code ModularTooltip}: parts, modifiers, wear): its class
 * line (the real-world designation, {@link GunDesignation}), its build, what is fitted, what it holds, its
 * condition, and the spec sheet. The adjectives, traits and Shift glosses are the core's.
 */
public final class GunTooltip {

    private GunTooltip() {
    }

    public static void add(ItemStack stack, Item.TooltipContext context, List<Component> tooltip) {
        GunSpec spec = GunSpec.of(stack).orElse(null);
        if (spec == null)
            return;
        GunDesignation.of(stack, spec).ifPresent(name ->
                tooltip.add(0, Component.translatable("gun.feedback.designation." + name).withStyle(ChatFormatting.AQUA)));
        tooltip.add(build(spec).withStyle(ChatFormatting.GRAY));
        fittings(spec).ifPresent(tooltip::add);
        tooltip.add(Component.translatable("gun.feedback.rounds", GunAmmo.magazine(stack).rounds().size(),
                stack.has(FWeapons.CHAMBER.get()) ? 1 : 0).withStyle(ChatFormatting.GRAY));
        // Damaged by overpressure is its own line, apart from wear (spec 6): evidence of a mistake.
        if (GunCondition.wear(stack) > 0)
            tooltip.add(Component.translatable("gun.feedback.wear",
                    Math.round(100 - 100 * GunCondition.condition(stack))).withStyle(ChatFormatting.YELLOW));
        float damage = GunHazard.damage(stack);
        if (GunHazard.isSpent(stack))
            tooltip.add(Component.translatable("gun.feedback.spent").withStyle(ChatFormatting.RED));
        else if (damage > 0)
            tooltip.add(Component.translatable("gun.feedback.overpressure_damage", Math.round(damage * 100))
                    .withStyle(ChatFormatting.RED));
        GunFigures f = GunItem.figures(stack);
        if (f != null)
            figures(f).forEach(tooltip::add);
        GunSpec.of(stack).filter(g -> g.launcher() == GunSpec.Launcher.STEAM).ifPresent(g ->
                tooltip.add(Component.translatable("gun.feedback.steam", Math.round(GunSteam.stored(stack)),
                        Math.round(g.steamCapacityMb())).withStyle(ChatFormatting.GRAY)));
    }

    /** "9.0 mm, 0.20 m barrel, 3.0 mm wall, iron sights, steel": the build, free to read off the barrel in your hand. */
    static net.minecraft.network.chat.MutableComponent build(GunSpec spec) {
        return Component.translatable("gun.feedback.build", String.format("%.1f", spec.barrel().boreMm()),
                String.format("%.2f", spec.barrel().lengthM()), String.format("%.1f", spec.barrel().wallMm()),
                Component.translatable("gun.feedback.sight." + spec.sight().name().toLowerCase(java.util.Locale.ROOT)),
                name(spec.material()),
                Component.translatable("gun.feedback.launcher." + spec.launcher().name().toLowerCase(java.util.Locale.ROOT)));
    }

    /** What is fitted, each in its metal: "Fitted: steel suppressor, iron barrel shroud". */
    public static java.util.Optional<Component> fittings(GunSpec spec) {
        if (spec.fittings().isEmpty())
            return java.util.Optional.empty();
        var list = Component.empty();
        for (var f : new java.util.TreeMap<>(spec.fittings()).entrySet()) {
            if (!list.getSiblings().isEmpty())
                list.append(", ");
            list.append(name(f.getValue())).append(" ").append(Component.translatable("gun.feedback.fitting." + f.getKey().getPath()));
        }
        return java.util.Optional.of(Component.translatable("gun.feedback.fittings", list).withStyle(ChatFormatting.GRAY));
    }

    public static Component name(ResourceLocation material) {
        return Component.translatable("material." + material.getNamespace() + "." + material.getPath());
    }

    /**
     * A gun's spec sheet -- philosophy 8 gives equipment specs away free and exact: what the metal
     * holds and what the charge asks of it, in red when it asks too much.
     */
    public static List<Component> figures(GunFigures f) {
        return List.of(
                f.launcher() == GunSpec.Launcher.SPRING
                        ? Component.translatable("gun.feedback.spring", Math.round(f.muzzleEnergy())).withStyle(ChatFormatting.GRAY)
                        : Component.translatable(f.coil() ? "gun.feedback.winding" : f.launcher() == GunSpec.Launcher.STEAM ? "gun.feedback.chest" : "gun.feedback.chamber",
                        String.format("%.1f", f.peakPressure().value()),
                        String.format("%.1f", f.chamberRating().value()))
                        .withStyle(f.overpressure() > 1 ? ChatFormatting.RED : ChatFormatting.GRAY),
                Component.translatable("gun.feedback.muzzle", Math.round(f.muzzleSpeed() * 20), Math.round(f.muzzleEnergy()))
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable("gun.feedback.cycle", f.cycleMs(), String.format("%.1f", f.spreadDeg()))
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable("gun.feedback.mass", String.format("%.2f", f.gunMass().value()),
                        String.format("%.1f", f.projectileMass().value() * 1000)).withStyle(ChatFormatting.GRAY));
    }
}
