// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.engineering;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbility;

/**
 * What a hand tool can do at the Engineering Table, as NeoForge item abilities so a modded tool can join by answering
 * {@code canPerformAction}. The modular hand tools answer by what they are: the hammer {@code hammer}, the sawblade {@code saw},
 * the file {@code file}, the screwdriver {@code driver}. Hand tools may always wear (design digest), so a use costs a point.
 */
public final class ToolAbilities {

    public static final ItemAbility HAMMER = ItemAbility.get("feedback:hammer");
    public static final ItemAbility SAW = ItemAbility.get("feedback:saw");
    public static final ItemAbility FILE = ItemAbility.get("feedback:file");
    public static final ItemAbility DRIVER = ItemAbility.get("feedback:driver");

    private static final List<ItemAbility> ALL = List.of(HAMMER, SAW, FILE, DRIVER);

    private ToolAbilities() {
    }

    /** The ability a modular hand tool has by its type, or null for any other stack. */
    @Nullable
    public static ItemAbility of(ItemStack stack) {
        if (stack.is(FModular.HAND_HAMMER.get()))
            return HAMMER;
        if (stack.is(FModular.SAWBLADE.get()))
            return SAW;
        if (stack.is(FModular.FILE.get()))
            return FILE;
        if (stack.is(FModular.SCREWDRIVER.get()))
            return DRIVER;
        return null;
    }

    /** An ability by its name ({@code feedback:hammer}), or null when none is known. */
    @Nullable
    public static ItemAbility byName(String name) {
        for (ItemAbility a : ALL)
            if (a.name().equals(name))
                return a;
        return ItemAbility.get(name);
    }

    /** The item tag that stands in for {@code ability} on an item that cannot answer {@code canPerformAction}: {@code feedback:hammer} is {@code #feedback:tool/hammer}. */
    public static TagKey<Item> tag(ItemAbility ability) {
        ResourceLocation id = ResourceLocation.parse(ability.name());
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "tool/" + id.getPath()));
    }

    /** Whether {@code stack} has {@code ability}, by answering it or by being in its item tag ({@link #tag}). */
    public static boolean can(ItemStack stack, ItemAbility ability) {
        return !stack.isEmpty() && (stack.canPerformAction(ability) || stack.is(tag(ability)));
    }

    /** Whether {@code stack} has any of the abilities a tool slot knows: what the table's tool slots accept. */
    public static boolean isTool(ItemStack stack) {
        for (ItemAbility a : ALL)
            if (can(stack, a))
                return true;
        return false;
    }

    /**
     * The slot of the first tool in {@code tools} with {@code ability}, preferring one that is not broken; -1 when none has it. A broken
     * one that is found alone is returned, so the caller can refuse the craft and say why.
     */
    public static int find(List<ItemStack> tools, ItemAbility ability) {
        int broken = -1;
        for (int i = 0; i < tools.size(); i++) {
            ItemStack t = tools.get(i);
            if (!can(t, ability))
                continue;
            if (!ModularStacks.broken(t))
                return i;
            if (broken < 0)
                broken = i;
        }
        return broken;
    }

    /** Wears {@code tool} by {@code points}, never past its last point (broken, not destroyed). Returns the points taken. */
    public static int wear(ItemStack tool, int points) {
        int taken = ModularStacks.cap(tool, Math.max(0, points));
        tool.setDamageValue(tool.getDamageValue() + taken);
        return taken;
    }
}
