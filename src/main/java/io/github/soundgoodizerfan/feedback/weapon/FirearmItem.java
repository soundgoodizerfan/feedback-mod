// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import io.github.soundgoodizerfan.feedback.registry.FWeapons;

/**
 * A firearm: a gun that ignites a powder charge in a case to throw its projectile ({@link FirearmMechanism}). Everything a gun is that
 * is not powder is {@link GunItem}'s; this adds the cook-off of a round left chambered in a hot barrel, the designation name and the
 * firearm's tooltip.
 */
public class FirearmItem extends GunItem {

    public FirearmItem(Properties properties) {
        super(properties);
    }

    @Override
    public GunMechanism mechanism() {
        return FirearmMechanism.INSTANCE;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!(level instanceof net.minecraft.server.level.ServerLevel server))
            return;
        // A round left chambered in a barrel past its ignition temperature goes off by itself.
        if (entity instanceof net.minecraft.world.entity.player.Player player && stack.has(FWeapons.CHAMBER.get())
                && stack.has(io.github.soundgoodizerfan.feedback.registry.FDataComponents.TEMPERATURE.get()))
            GunShots.cookOff(server, player, stack, System.currentTimeMillis());
    }

    @Override
    public Component getName(ItemStack stack) {
        return GunSpec.of(stack).flatMap(spec -> GunDesignation.of(stack, spec).map(name ->
                (Component) Component.translatable("item.feedback.modular_name", GunTooltip.name(spec.material()),
                        Component.translatable("gun.feedback.designation." + name)))).orElseGet(() -> super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        GunTooltip.add(stack, context, tooltip);
    }
}
