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

import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A gun in hand. It carries its {@link GunDesign} and its {@link GunFigures}; firing is driven
 * from the client (prediction, spec 7) and resolved on the server ({@link GunShots}), so the
 * item itself does nothing on use.
 *
 * <p>A gun made at the Stand arrives with its figures (the blueprint baked them at print). The
 * one that does not is the hand-authored test pistol: the item is registered before any
 * datapack, so its default design cannot be baked until the material table exists, and it is
 * baked here, server-side, the first time it is ticked without them.
 */
public class GunItem extends Item implements io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat.Cooling {

    public GunItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** The stack's baked figures, or null if it has none yet. */
    public static GunFigures figures(ItemStack stack) {
        return stack.get(FWeapons.GUN_FIGURES.get());
    }

    /** A gun cools at its own rate: its mass over the leak, from the figures it carries. */
    @Override
    public io.github.soundgoodizerfan.feedback.core.unit.TuRate coolingRate(ItemStack stack) {
        GunFigures f = figures(stack);
        return f == null ? io.github.soundgoodizerfan.feedback.core.FTuning.ITEM_COOLING_TU_PER_TICK : f.coolingRate();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server))
            return;
        if (!stack.has(FWeapons.GUN_FIGURES.get())) {
            GunDesign design = stack.get(FWeapons.GUN_DESIGN.get());
            if (design == null)
                return;
            design.bake(MaterialTable.get().materials()).ifPresent(f -> stack.set(FWeapons.GUN_FIGURES.get(), f));
        }
        // A round left chambered in a barrel past its ignition temperature goes off by itself.
        if (entity instanceof net.minecraft.world.entity.player.Player player && stack.has(FWeapons.CHAMBER.get())
                && stack.has(io.github.soundgoodizerfan.feedback.registry.FDataComponents.TEMPERATURE.get()))
            GunShots.cookOff(server, player, stack, System.currentTimeMillis());
    }

    /** The fire mode the gun is set to, as far as its action can fire it (spec 12). */
    public static FireMode mode(ItemStack stack) {
        FireMode set = stack.getOrDefault(FWeapons.FIRE_MODE.get(), FireMode.AUTO);
        GunDesign design = stack.get(FWeapons.GUN_DESIGN.get());
        return design == null ? set : design.layout().action().allow(set);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GunTooltip.add(stack, context, tooltip);
    }
}
