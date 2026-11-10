// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.registry.FModular;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Who may draw what. A type with a {@link School} is drawn at the Blueprint Table only by someone who holds that school's treatise: loose
 * in the inventory, inside a {@link TreatiseBinderItem} in the inventory, or in a {@link TreatiseShelfBlockEntity} beside the table. Nothing
 * is spent. Building to a blueprint is the blueprint's own gate.
 */
public final class Treatises {

    private Treatises() {
    }

    /** Whether {@code stack} is, or is a binder holding, a treatise of {@code school}. */
    public static boolean holds(ItemStack stack, School school) {
        if (stack.getItem() instanceof TreatiseItem t)
            return t.school() == school;
        if (stack.getItem() instanceof TreatiseBinderItem) {
            BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
            if (contents != null)
                for (ItemStack s : contents.items())
                    if (s.getItem() instanceof TreatiseItem t && t.school() == school)
                        return true;
        }
        return false;
    }

    /** Whether {@code player} can draw {@code school}'s types at the table {@code access} names. */
    public static boolean has(Player player, ContainerLevelAccess access, School school) {
        for (ItemStack s : player.getInventory().items)
            if (holds(s, school))
                return true;
        for (ItemStack s : player.getInventory().offhand)
            if (holds(s, school))
                return true;
        return access.evaluate((level, pos) -> shelved(level, pos, school)).orElse(false);
    }

    /** Whether a shelf touching {@code pos} holds the treatise. */
    private static boolean shelved(Level level, BlockPos pos, School school) {
        for (Direction d : Direction.values()) {
            BlockEntity be = level.getBlockEntity(pos.relative(d));
            if (be instanceof TreatiseShelfBlockEntity shelf)
                for (int i = 0; i < shelf.getContainerSize(); i++)
                    if (holds(shelf.getItem(i), school))
                        return true;
        }
        return false;
    }

    /** The treatise item of {@code school}. */
    public static ItemStack of(School school) {
        return new ItemStack(FModular.TREATISES.get(school).get());
    }

    /** Adds a treatise of a random school to a chest's loot; the chest tables and the chance are the modifier's conditions. */
    public static class Loot extends LootModifier {

        public static final MapCodec<Loot> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, Loot::new));

        public Loot(LootItemCondition[] conditions) {
            super(conditions);
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
            School[] all = School.values();
            loot.add(of(all[context.getRandom().nextInt(all.length)]));
            return loot;
        }

        @Override
        public MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec() {
            return CODEC;
        }
    }
}
