// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * The title a designer gave a blueprint and their name, written by the server when the blueprint is printed. An item made to a blueprint
 * carries the same record with {@code drawn} set to the drawing, so that a rework that departs from it can drop the credit.
 */
public record BlueprintCredit(String title, String author, Optional<ModularDesign> drawn) {

    public static final int MAX_TITLE = 32;

    public static final Codec<BlueprintCredit> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("title").forGetter(BlueprintCredit::title),
            Codec.STRING.fieldOf("author").forGetter(BlueprintCredit::author),
            ModularDesign.CODEC.optionalFieldOf("drawn").forGetter(BlueprintCredit::drawn)).apply(inst, BlueprintCredit::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintCredit> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BlueprintCredit::title,
            ByteBufCodecs.STRING_UTF8, BlueprintCredit::author,
            ByteBufCodecs.optional(ModularDesign.STREAM_CODEC), BlueprintCredit::drawn,
            BlueprintCredit::new);

    /** A title cleaned for storing: trimmed and cut to {@link #MAX_TITLE}. */
    public static String clean(String title) {
        String t = title == null ? "" : title.strip();
        return t.length() > MAX_TITLE ? t.substring(0, MAX_TITLE) : t;
    }

    /** The credit of {@code stack}, or null. */
    public static BlueprintCredit of(ItemStack stack) {
        return stack.get(FModular.CREDIT.get());
    }

    /** This credit as it rides on an item made to {@code drawing}. */
    public BlueprintCredit onItem(ModularDesign drawing) {
        return new BlueprintCredit(title, author, Optional.of(drawing));
    }

    /** The "Designed by" line. */
    public Component line() {
        return Component.translatable("tooltip.feedback.blueprint.credit", author).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
    }

    /** The title line, or null for an untitled one. */
    public Component titleLine() {
        return title.isEmpty() ? null : Component.literal(title).withStyle(ChatFormatting.GOLD);
    }
}
