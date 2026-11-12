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
 * Who may draw what. A type with a {@link School} is drawn at the Blueprint Table only by someone who holds that volume of the school's treatise: loose
 * in the inventory, inside a {@link TreatiseBinderItem} in the inventory, or in a {@link TreatiseShelfBlockEntity} beside the table. Nothing
 * is spent. Building to a blueprint is the blueprint's own gate.
 */
public final class Treatises {

    private Treatises() {
    }

    /** Whether {@code stack} is, or is a binder holding, volume {@code volume} of {@code school}'s treatise. */
    public static boolean holds(ItemStack stack, School school, int volume) {
        if (stack.getItem() instanceof CreativeTreatiseItem)
            return true;
        if (stack.getItem() instanceof TreatiseItem t)
            return t.school() == school && t.volume() == volume;
        if (stack.getItem() instanceof TreatiseBinderItem) {
            BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
            if (contents != null)
                for (ItemStack s : contents.items())
                    if (s.getItem() instanceof TreatiseItem t && t.school() == school && t.volume() == volume)
                        return true;
        }
        return false;
    }

    /** Whether {@code player} can draw the types of {@code school}'s volume {@code volume} at the table {@code access} names. */
    public static boolean has(Player player, ContainerLevelAccess access, School school, int volume) {
        for (ItemStack s : player.getInventory().items)
            if (holds(s, school, volume))
                return true;
        for (ItemStack s : player.getInventory().offhand)
            if (holds(s, school, volume))
                return true;
        return access.evaluate((level, pos) -> shelved(level, pos, school, volume)).orElse(false);
    }

    /** The quality of a held treatise's notes, 0 to 1, a stack with none counting for half (a found or bought volume): the best of the matching ones. */
    private static float quality(ItemStack stack, School school, int volume) {
        float best = 0f;
        if (stack.getItem() instanceof TreatiseItem t && t.school() == school && t.volume() == volume)
            best = stack.getOrDefault(FModular.NOTES.get(), 0.5f);
        else if (stack.getItem() instanceof TreatiseBinderItem) {
            BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
            if (contents != null)
                for (ItemStack s : contents.items())
                    best = Math.max(best, quality(s, school, volume));
        }
        return best;
    }

    /** The key a school's volume has among the notes: {@code weapons:2}. */
    public static String key(School school, int volume) {
        return school.name().toLowerCase(java.util.Locale.ROOT) + ":" + volume;
    }

    /** The notes quality of every volume the maker holds (inventory, binder slot, shelves beside the table), by {@link #key}: the best of each. */
    public static java.util.Map<String, Float> notes(Player player, ContainerLevelAccess access, ItemStack binderSlot) {
        java.util.Map<String, Float> out = new java.util.HashMap<>();
        java.util.List<ItemStack> held = new java.util.ArrayList<>(player.getInventory().items);
        held.addAll(player.getInventory().offhand);
        held.add(binderSlot);
        access.execute((level, pos) -> {
            for (Direction d : Direction.values())
                if (level.getBlockEntity(pos.relative(d)) instanceof TreatiseShelfBlockEntity shelf)
                    for (int i = 0; i < shelf.getContainerSize(); i++)
                        held.add(shelf.getItem(i));
        });
        for (School school : School.values())
            for (int v = 1; v <= school.volumes(); v++) {
                float best = -1f;
                for (ItemStack s : held)
                    if (holds(s, school, v))
                        best = Math.max(best, quality(s, school, v));
                if (best >= 0f)
                    out.put(key(school, v), best);
            }
        return out;
    }

    /** Whether a shelf touching {@code pos} holds the volume. */
    private static boolean shelved(Level level, BlockPos pos, School school, int volume) {
        for (Direction d : Direction.values()) {
            BlockEntity be = level.getBlockEntity(pos.relative(d));
            if (be instanceof TreatiseShelfBlockEntity shelf)
                for (int i = 0; i < shelf.getContainerSize(); i++)
                    if (holds(shelf.getItem(i), school, volume))
                        return true;
        }
        return false;
    }

    /** The volumes {@code player} holds at the table {@code access} names, one bit per school and volume ({@link School#bit}). */
    public static int mask(Player player, ContainerLevelAccess access) {
        int mask = 0;
        for (School school : School.values())
            for (int v = 1; v <= school.volumes(); v++)
                if (has(player, access, school, v))
                    mask |= 1 << school.bit(v);
        return mask;
    }

    /** As {@link #mask}, for the inventory alone: what a program on a computer can count, with no table beside it. */
    public static int mask(Player player) {
        return mask(player, ContainerLevelAccess.NULL);
    }

    /** Whether {@code mask} lets a player draw {@code type}: it has no school, or its school's volume bit is set. */
    public static boolean draws(int mask, ModularType type) {
        School school = School.of(type.school());
        return school == null || (mask & (1 << school.bit(School.volumeOf(type.school())))) != 0;
    }

    /**
     * The school gate for assembly as a predicate over types, for {@link ModularAssembly#infer}: the types whose school's volume bit is
     * in {@code mask}, plus every type with no school. A bench ORs the masks of every place it looks (inventory and shelf by
     * {@link #mask(Player, ContainerLevelAccess)}, its binder slot by {@link #mask(ItemStack)}).
     */
    public static java.util.function.Predicate<ModularType> assembles(int mask) {
        return type -> draws(mask, type);
    }

    /** The volumes {@code stack} is, or a binder in it holds, one bit per school and volume ({@link School#bit}). */
    public static int mask(ItemStack stack) {
        int mask = 0;
        for (School school : School.values())
            for (int v = 1; v <= school.volumes(); v++)
                if (holds(stack, school, v))
                    mask |= 1 << school.bit(v);
        return mask;
    }

    /** Volume {@code volume} of {@code school}'s treatise. */
    public static ItemStack of(School school, int volume) {
        return new ItemStack(FModular.TREATISES.get(school).get(Math.max(1, Math.min(volume, school.volumes())) - 1).get());
    }

    /** Adds a treatise (a random volume) of a random school to a chest's loot; the chest tables and the chance are the modifier's conditions. */
    public static class Loot extends LootModifier {

        public static final MapCodec<Loot> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, Loot::new));

        public Loot(LootItemCondition[] conditions) {
            super(conditions);
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
            School[] all = School.values();
            School school = all[context.getRandom().nextInt(all.length)];
            loot.add(of(school, 1 + context.getRandom().nextInt(school.volumes())));
            return loot;
        }

        @Override
        public MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec() {
            return CODEC;
        }
    }
}
