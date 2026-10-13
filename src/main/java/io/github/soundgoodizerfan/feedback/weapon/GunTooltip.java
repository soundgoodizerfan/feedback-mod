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

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/** The design and figure lines a Gun and a Gun Blueprint both show. */
public final class GunTooltip {

    private GunTooltip() {
    }

    static void add(ItemStack stack, Item.TooltipContext context, List<Component> tooltip) {
        GunDesign design = stack.get(FWeapons.GUN_DESIGN.get());
        if (design == null)
            return;
        Map<ResourceLocation, Material> materials = context.level() == null ? Map.of()
                : ClientMaterials.on(context.level());
        tooltip.add(settings(design).withStyle(ChatFormatting.GRAY));
        if (stack.getItem() instanceof GunItem) {
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
        }
        GunFigures f = stack.get(FWeapons.GUN_FIGURES.get());
        if (f == null) {
            if (!materials.containsKey(design.material()))
                tooltip.add(Component.translatable("gun.feedback.unknown_material").withStyle(ChatFormatting.RED));
            return;
        }
        figures(f).forEach(tooltip::add);
    }

    static net.minecraft.network.chat.MutableComponent settings(GunDesign design) {
        return Component.translatable("gun.feedback.settings", design.boreMm(),
                Component.translatable("gun.feedback.barrel." + design.barrel().getSerializedName()),
                Component.translatable("gun.feedback.wall." + design.wall().getSerializedName()),
                Component.translatable("material." + design.material().getNamespace() + "." + design.material().getPath()),
                Component.translatable("gun.feedback.sight." + design.sight().getSerializedName()));
    }

    /**
     * A gun's spec sheet -- philosophy 8 gives equipment specs away free and exact: what the metal
     * holds and what the charge asks of it, in red when it asks too much.
     */
    public static List<Component> figures(GunFigures f) {
        return List.of(
                Component.translatable("gun.feedback.chamber", String.format("%.1f", f.peakPressure().value()),
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
