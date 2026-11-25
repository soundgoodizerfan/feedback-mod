// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import java.util.List;

import io.github.soundgoodizerfan.feedback.registry.FWeapons;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A cam for the Mechanical Hammer: a plate cut to the profile of one forged shape's pattern, carrying its name ({@code HAMMER_CAM}). Seated in the
 * hammer it makes the head strike that shape's notes on the cam's own tick; a part forged on the wrong cam is judged against the shape asked for,
 * so it comes out worse. A blank cam (no pattern) plays nothing.
 */
public class HammerCamItem extends Item {

    public HammerCamItem(Properties properties) {
        super(properties);
    }

    /** The shape name the cam is cut for, or empty. */
    public static String pattern(ItemStack cam) {
        return cam.getOrDefault(FWeapons.HAMMER_CAM.get(), "");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String p = pattern(stack);
        tooltip.add(p.isEmpty() ? Component.translatable("tooltip.feedback.hammer_cam.blank")
                : Component.translatable("tooltip.feedback.hammer_cam.pattern", Component.translatable("modular.feedback.variant." + p)));
    }
}
