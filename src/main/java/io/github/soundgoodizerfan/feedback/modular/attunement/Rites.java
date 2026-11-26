// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.attunement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.enchant.EnchantGame;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;

/**
 * Rites: what an Enchanting Table can attune is what its shelves hold. A rite ({@link RiteItem}) names one attunement and carries the quality of
 * its notes; a chiseled bookshelf of rites standing where a bookshelf would around the table makes that attunement available, and the quality
 * sets the highest level that can be written (five bands, the words a treatise's notes use). The same room's enchanting power, the vanilla
 * count, takes experience off an attempt and widens the band the written word may miss by. The four starter effects need no rite and reach level 1.
 *
 * <p>Taken from vanilla: the table's ring of shelf positions and the enchantment-power block tags, so another mod's shelves count. A rite is never
 * used up. The mirror of this is {@link #fromBook}: a vanilla enchanted book is one rite.
 */
public final class Rites {

    private Rites() {
    }

    /** What a rite item carries: the attunement and the quality of its notes, 0 to 1. */
    public record Rite(ResourceLocation effect, float quality) {
        public static final Codec<Rite> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("effect").forGetter(Rite::effect),
                Codec.floatRange(0f, 1f).fieldOf("quality").forGetter(Rite::quality)
        ).apply(i, Rite::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Rite> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Rite::effect, ByteBufCodecs.FLOAT, Rite::quality, Rite::new);
    }

    /** The quality at which the notes pass from guessed to rough to fair to clean to exact; the same words a treatise's notes use. */
    private static final float[] BANDS = {0.3f, 0.55f, 0.78f, 0.92f};

    /** The highest level a rite of this quality lets be written: 1 (guessed) to {@link ModularTuning#RITE_MAX_LEVEL} (exact). */
    public static int maxLevel(float quality) {
        int level = 1;
        for (float band : BANDS)
            if (quality >= band)
                level++;
        return Math.min(ModularTuning.RITE_MAX_LEVEL, level);
    }

    /**
     * How much of each level a part of this material gives: from {@link ModularTuning#RITE_STRENGTH_FLOOR} for one with no enchantability up to
     * full at {@link ModularTuning#RITE_STRENGTH_ENCHANTABILITY}. Lead's level 3 burns less than gold's.
     */
    public static float strength(Material material) {
        int e = material.enchantability().orElse(ModularTuning.ENCHANTABILITY_FALLBACK);
        float f = Math.max(0f, Math.min(1f, e / (float) ModularTuning.RITE_STRENGTH_ENCHANTABILITY));
        return ModularTuning.RITE_STRENGTH_FLOOR + (1f - ModularTuning.RITE_STRENGTH_FLOOR) * f;
    }

    /** A rite of {@code effect} at {@code quality}. */
    public static ItemStack of(ResourceLocation effect, float quality) {
        ItemStack stack = new ItemStack(FModular.RITE.get());
        stack.set(FDataComponents.RITE.get(), new Rite(effect, Math.max(0f, Math.min(1f, quality))));
        return stack;
    }

    /** What the stack is a rite of, or null when it is not a rite. */
    public static Rite of(ItemStack stack) {
        return stack.getItem() instanceof RiteItem ? stack.get(FDataComponents.RITE.get()) : null;
    }

    // --- the room ---------------------------------------------------------------------------------

    /** What stands around a table: its enchanting power and the best quality it holds of each effect. Read when the screen opens, never per tick. */
    public record Library(int power, Map<ResourceLocation, Float> rites) {

        public static final Library NONE = new Library(0, Map.of());

        public static final StreamCodec<RegistryFriendlyByteBuf, Library> STREAM_CODEC = StreamCodec.of(
                (buf, l) -> {
                    buf.writeVarInt(l.power);
                    buf.writeVarInt(l.rites.size());
                    l.rites.forEach((id, q) -> {
                        buf.writeResourceLocation(id);
                        buf.writeFloat(q);
                    });
                },
                buf -> {
                    int power = buf.readVarInt(), n = buf.readVarInt();
                    Map<ResourceLocation, Float> rites = new HashMap<>();
                    for (int i = 0; i < n; i++)
                        rites.put(buf.readResourceLocation(), buf.readFloat());
                    return new Library(power, rites);
                });

        /** The highest level that can be written of {@code effect} here: a shelved rite's, at least 1 for a starter, else 0 (none: it cannot be attempted). */
        public int top(Attunements.Effect effect) {
            Float quality = rites.get(effect.id());
            int level = quality == null ? 0 : maxLevel(quality);
            return effect.starter() ? Math.max(1, level) : level;
        }

        /** The room's power as a share of a fully fitted one, 0 to 1. */
        public float fitted() {
            return Math.min(1f, power / (float) ModularTuning.RITE_POWER_FULL);
        }

        /** What an attempt's experience is scaled by: less in a fitted room. */
        public float costFactor() {
            return 1f - ModularTuning.RITE_POWER_COST_CUT * fitted();
        }

        /** What an effect's band is scaled by: wider in a fitted room, so the word may miss by more. */
        public float bandFactor() {
            return 1f + ModularTuning.RITE_POWER_BAND_GAIN * fitted();
        }
    }

    /** Reads the shelves around the table at {@code table}: the vanilla ring of positions (a transmitter block between), power from the block tags, rites from chiseled bookshelves. */
    public static Library scan(Level level, BlockPos table) {
        int power = 0;
        Map<ResourceLocation, Float> rites = new HashMap<>();
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            BlockPos at = table.offset(offset);
            if (level.getBlockState(at).is(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEBUG_SHELF.get())) {
                // The debug shelf is a perfect room by itself: full power, every effect exact.
                Map<ResourceLocation, Float> all = new HashMap<>();
                Attunements.known().keySet().forEach(id -> all.put(id, 1f));
                return new Library(ModularTuning.RITE_POWER_FULL, all);
            }
            if (!level.getBlockState(table.offset(offset.getX() / 2, offset.getY(), offset.getZ() / 2)).is(BlockTags.ENCHANTMENT_POWER_TRANSMITTER))
                continue;
            power += Math.round(level.getBlockState(at).getEnchantPowerBonus(level, at));
            if (level.getBlockEntity(at) instanceof ChiseledBookShelfBlockEntity shelf)
                for (int slot = 0; slot < ChiseledBookShelfBlockEntity.MAX_BOOKS_IN_STORAGE; slot++) {
                    Rite rite = of(shelf.getItem(slot));
                    if (rite != null)
                        rites.merge(rite.effect(), rite.quality(), Math::max);
                }
        }
        return new Library(power, rites);
    }

    // --- vanilla ----------------------------------------------------------------------------------

    /** The attunement a vanilla enchantment is named by, if one is. */
    private static Optional<Attunements.Effect> effectOf(ResourceLocation enchantment) {
        for (Attunements.Effect e : Attunements.known().values())
            if (e.books().contains(enchantment))
                return Optional.of(e);
        return Optional.empty();
    }

    /**
     * The one rite a vanilla enchanted book becomes: of the enchantments it holds that an attunement names, the rarest (lowest vanilla weight);
     * enchantments within {@link #CLOSE} of that rarity are counted as equal and the highest level wins. Middling quality, lifted a little by how
     * far up its own scale the level was. Empty when the book names nothing an attunement does.
     */
    public static Optional<ItemStack> fromBook(ItemStack book) {
        ItemEnchantments held = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        record Pick(Attunements.Effect effect, int weight, int level, int max) {
        }
        List<Pick> picks = new ArrayList<>();
        for (var entry : held.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            ResourceLocation id = holder.unwrapKey().map(k -> k.location()).orElse(null);
            Optional<Attunements.Effect> effect = id == null ? Optional.empty() : effectOf(id);
            if (effect.isPresent())
                picks.add(new Pick(effect.get(), holder.value().getWeight(), entry.getIntValue(), holder.value().getMaxLevel()));
        }
        if (picks.isEmpty())
            return Optional.empty();
        int rarest = picks.stream().mapToInt(Pick::weight).min().getAsInt();
        Pick best = picks.stream().filter(p -> p.weight() - rarest <= CLOSE).max(java.util.Comparator.comparingInt(Pick::level)).get();
        float spread = best.max() <= 1 ? 1f : (best.level() - 1f) / (best.max() - 1f);
        float quality = Math.min(ModularTuning.RITE_BOOK_QUALITY_CAP, ModularTuning.RITE_FOUND_QUALITY + ModularTuning.RITE_BOOK_QUALITY_SPREAD * spread);
        return Optional.of(of(best.effect().id(), quality));
    }

    /** Vanilla weights within this of the rarest count as equally rare. */
    private static final int CLOSE = 2;

    /**
     * Whether {@code stack} is a rite standing for an enchanted book that holds {@code wanted}: its attunement is the one an asked enchantment
     * is named by, and its quality lets that level (up to the highest a rite carries) be written. Vanilla books cannot be had, so another
     * mod's ask for one (a research page wanting Silk Touch) takes the rite.
     */
    public static boolean standsForBook(ItemStack stack, ItemEnchantments wanted) {
        Rite rite = of(stack);
        if (rite == null)
            return false;
        for (var entry : wanted.entrySet()) {
            ResourceLocation id = entry.getKey().unwrapKey().map(k -> k.location()).orElse(null);
            Optional<Attunements.Effect> effect = id == null ? Optional.empty() : effectOf(id);
            if (effect.isPresent() && effect.get().id().equals(rite.effect())
                    && maxLevel(rite.quality()) >= Math.min(entry.getIntValue(), ModularTuning.RITE_MAX_LEVEL))
                return true;
        }
        return false;
    }

    /** Whether the stack is a vanilla enchanted book that converts to a rite. */
    public static boolean convertibleBook(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK);
    }

    /**
     * A vanilla enchanted tool's enchantments laid onto the parts of its converted design: each goes on the part whose role does the most good with
     * its effect (a row with the fewest flaws, a working face over any other, the first in the type's order), at the vanilla level up to five, while
     * the part has a slot left. An enchantment no attunement names, or no part fits, is dropped.
     */
    public static List<ModularDesign.Part> attuneConverted(List<ModularDesign.Part> parts, ModularType type, ItemEnchantments enchantments, Map<ResourceLocation, Material> materials) {
        List<ModularDesign.Part> out = new ArrayList<>(parts);
        for (var entry : enchantments.entrySet()) {
            ResourceLocation id = entry.getKey().unwrapKey().map(k -> k.location()).orElse(null);
            Attunements.Effect effect = id == null ? null : effectOf(id).orElse(null);
            if (effect == null)
                continue;
            int best = -1, bestScore = Integer.MIN_VALUE;
            for (int i = 0; i < out.size(); i++) {
                ModularDesign.Part p = out.get(i);
                ModularType.Role role = type.roles().get(p.role());
                Material m = materials.get(p.material());
                if (role == null || m == null)
                    continue;
                Attunements.Behaviour b = effect.on(role.name(), role.cls(), false);
                if (b == null || p.measures().attuned().size() >= EnchantGame.capacity(m) || p.measures().attuned().containsKey(effect.key()))
                    continue;
                int score = -10 * b.flaw() + ("working_face".equals(role.cls()) ? 5 : 0);
                if (score > bestScore) {
                    best = i;
                    bestScore = score;
                }
            }
            if (best >= 0) {
                ModularDesign.Part p = out.get(best);
                out.set(best, p.withMeasures(p.measures().withAttuned(effect.key(), Math.min(ModularTuning.RITE_MAX_LEVEL, entry.getIntValue()))));
            }
        }
        return out;
    }
}
