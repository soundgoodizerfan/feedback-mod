// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.attunement;

import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Which attunements one player knows (roadmap 4: discovery ends in knowing; a book teaches, it does not apply). A player attachment, kept
 * on death and sent to its own client, so the Enchanting Table can show an unknown effect as a blank. The starter effects ({@link
 * Attunements.Effect#starter}) are known to everyone and are not stored here.
 */
public record AttunementKnowledge(Set<ResourceLocation> known) {

    public static final AttunementKnowledge EMPTY = new AttunementKnowledge(Set.of());

    public static final Codec<AttunementKnowledge> CODEC = ResourceLocation.CODEC.listOf()
            .xmap(l -> new AttunementKnowledge(new TreeSet<>(l)), k -> java.util.List.copyOf(k.known()));

    public static final StreamCodec<RegistryFriendlyByteBuf, AttunementKnowledge> STREAM_CODEC =
            StreamCodec.<RegistryFriendlyByteBuf, java.util.List<ResourceLocation>>of(
                    (buf, list) -> {
                        buf.writeVarInt(list.size());
                        list.forEach(id -> buf.writeResourceLocation(id));
                    },
                    buf -> {
                        int n = buf.readVarInt();
                        java.util.List<ResourceLocation> list = new java.util.ArrayList<>(n);
                        for (int i = 0; i < n; i++)
                            list.add(buf.readResourceLocation());
                        return list;
                    }).map(l -> new AttunementKnowledge(new TreeSet<>(l)), k -> java.util.List.copyOf(k.known()));

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Feedback.MOD_ID);

    public static final Supplier<AttachmentType<AttunementKnowledge>> ATTACHMENT = ATTACHMENTS.register("attunement_knowledge",
            () -> AttachmentType.builder(() -> EMPTY).serialize(CODEC).sync(STREAM_CODEC).copyOnDeath().build());

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }

    /** Whether {@code player} may attempt {@code effect} with its guide: a starter effect, or one learned. */
    public static boolean knows(Player player, Attunements.Effect effect) {
        return effect.starter() || player.getData(ATTACHMENT.get()).known().contains(effect.id());
    }

    /** Teaches {@code effect} to {@code player}; false if already known. */
    public static boolean learn(ServerPlayer player, Attunements.Effect effect) {
        if (knows(player, effect))
            return false;
        Set<ResourceLocation> now = new TreeSet<>(player.getData(ATTACHMENT.get()).known());
        now.add(effect.id());
        player.setData(ATTACHMENT.get(), new AttunementKnowledge(now));
        return true;
    }
}
