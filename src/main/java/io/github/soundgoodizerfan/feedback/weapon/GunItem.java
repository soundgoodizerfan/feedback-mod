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

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;

/**
 * A gun in hand. It carries its {@link GunDesign} and its {@link GunFigures}; firing is driven
 * from the client (prediction, spec 7) and resolved on the server ({@link GunShots}), so the
 * item itself does nothing on use.
 *
 * <p><b>Step 1 shortcut:</b> the figures are baked here, on the server, the first time a held
 * gun is ticked without them -- because the item is registered before any datapack, and the
 * test pistol's default design cannot be baked until the material table exists. The Gun Stand
 * (spec section 4) writes design and figures together at print and this goes away.
 */
public class GunItem extends Item {

    public GunItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** The stack's baked figures, or null if it has none yet. */
    public static GunFigures figures(ItemStack stack) {
        return stack.get(FWeapons.GUN_FIGURES.get());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || stack.has(FWeapons.GUN_FIGURES.get()))
            return;
        GunDesign design = stack.get(FWeapons.GUN_DESIGN.get());
        if (design == null)
            return;
        var materials = io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials();
        Material barrel = materials.get(design.material());
        Material bullet = materials.get(Feedback.id("lead"));
        if (barrel == null || bullet == null)
            return;
        design.bake(barrel, bullet).ifPresent(f -> stack.set(FWeapons.GUN_FIGURES.get(), f));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GunDesign design = stack.get(FWeapons.GUN_DESIGN.get());
        if (design == null)
            return;
        Map<net.minecraft.resources.ResourceLocation, Material> materials = context.level() == null ? Map.of()
                : ClientMaterials.on(context.level());
        tooltip.add(Component.translatable("gun.feedback.settings", design.boreMm(),
                Component.translatable("gun.feedback.barrel." + design.barrel().getSerializedName()),
                Component.translatable("gun.feedback.wall." + design.wall().getSerializedName()),
                Component.translatable("material." + design.material().getNamespace() + "." + design.material().getPath()))
                .withStyle(ChatFormatting.GRAY));
        GunFigures f = figures(stack);
        if (f == null) {
            if (!materials.containsKey(design.material()))
                tooltip.add(Component.translatable("gun.feedback.unknown_material").withStyle(ChatFormatting.RED));
            return;
        }
        // An equipment spec is free and exact (philosophy 8): what the metal holds and what the charge asks of it.
        tooltip.add(Component.translatable("gun.feedback.chamber", String.format("%.1f", f.peakPressure().value()),
                String.format("%.1f", f.chamberRating().value())).withStyle(f.overpressure() > 1 ? ChatFormatting.RED : ChatFormatting.GRAY));
        tooltip.add(Component.translatable("gun.feedback.muzzle", Math.round(f.muzzleSpeed() * 20), Math.round(f.muzzleEnergy()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("gun.feedback.cycle", f.cycleMs(), String.format("%.1f", f.spreadDeg()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("gun.feedback.mass", String.format("%.2f", f.gunMass().value()),
                String.format("%.1f", f.projectileMass().value() * 1000)).withStyle(ChatFormatting.GRAY));
    }
}
