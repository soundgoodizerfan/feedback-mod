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

import io.github.soundgoodizerfan.feedback.modular.ModularItem;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A gun in hand: a modular item of the gun type like a pick or a sword ({@code docs/feedback_modular_items_spec.md}
 * 8.3), so its name, parts, adjectives, traits and the Shift glosses are the core's. It carries a
 * {@link io.github.soundgoodizerfan.feedback.modular.ModularDesign} and the figures baked from it
 * ({@link GunFigures#of} is the shot logic's view of them); firing is driven from the client (prediction,
 * guns spec 7) and resolved on the server ({@link GunShots}), so the item itself does nothing on use.
 *
 * <p>A gun made at the Gun Table arrives with its figures. The one that does not is the creative tab's
 * starter gun, baked by the core the first time it is ticked ({@link ModularStacks#tick}).
 */
public class GunItem extends ModularItem implements io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat.Cooling {

    public GunItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** The stack's gun figures at the reference load, or null if it has none yet. */
    public static GunFigures figures(ItemStack stack) {
        return GunFigures.of(ModularStacks.figures(stack));
    }

    /** A gun cools at its own rate: its mass over the leak, from the figures it carries. */
    @Override
    public io.github.soundgoodizerfan.feedback.core.unit.TuRate coolingRate(ItemStack stack) {
        GunFigures f = figures(stack);
        return f == null ? io.github.soundgoodizerfan.feedback.core.FTuning.ITEM_COOLING_TU_PER_TICK : f.coolingRate();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!(level instanceof net.minecraft.server.level.ServerLevel server))
            return;
        if (stack.has(FWeapons.GUN_STEAM.get()))
            GunSteam.condense(stack);
        // A round left chambered in a barrel past its ignition temperature goes off by itself.
        if (entity instanceof net.minecraft.world.entity.player.Player player && stack.has(FWeapons.CHAMBER.get())
                && stack.has(io.github.soundgoodizerfan.feedback.registry.FDataComponents.TEMPERATURE.get()))
            GunShots.cookOff(server, player, stack, System.currentTimeMillis());
    }

    /** "Steel Revolver": the barrel's metal and the gun's real-world designation, else the type's own name. */
    @Override
    public Component getName(ItemStack stack) {
        return GunSpec.of(stack).flatMap(spec -> GunDesignation.of(stack, spec).map(name ->
                (Component) Component.translatable("item.feedback.modular_name", GunTooltip.name(spec.material()),
                        Component.translatable("gun.feedback.designation." + name)))).orElseGet(() -> super.getName(stack));
    }

    /** Whether {@code stack} is a gun made to be fired one-handed: the {@code off_hand} trait (a handgun's receiver). */
    public static boolean oneHanded(ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem))
            return false;
        // A handgun's receiver says so; any other gun is one-handed if its baked mass and recoil allow it.
        GunFigures f = GunFigures.of(io.github.soundgoodizerfan.feedback.modular.ModularStacks.figures(stack));
        return GunSpec.of(stack).map(g -> g.has("off_hand")).orElse(false) || f != null && f.oneHandable();
    }

    /** A gun in each hand: each is fired one-handed, and {@link GunFigures#oneHanded} is what that costs. */
    public static boolean dualWielding(net.minecraft.world.entity.player.Player player) {
        return player.getMainHandItem().getItem() instanceof GunItem && player.getOffhandItem().getItem() instanceof GunItem;
    }

    /** Whether the off-hand gun fires on the use key: it is one-handed, and the main hand holds no gun or a one-handed one. */
    public static boolean offHandFires(net.minecraft.world.entity.player.Player player) {
        ItemStack main = player.getMainHandItem();
        return oneHanded(player.getOffhandItem()) && (!(main.getItem() instanceof GunItem) || oneHanded(main));
    }

    /** Whether the off hand holds a gun at all. */
    public static boolean hasOffHandGun(net.minecraft.world.entity.player.Player player) {
        return player.getOffhandItem().getItem() instanceof GunItem;
    }

    /** The fire mode the gun is set to, as far as its action can fire it (spec 12). */
    public static FireMode mode(ItemStack stack) {
        FireMode set = stack.getOrDefault(FWeapons.FIRE_MODE.get(), FireMode.AUTO);
        return GunSpec.of(stack).map(g -> g.layout().action().allow(set)).orElse(set);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        GunTooltip.add(stack, context, tooltip);
    }
}
