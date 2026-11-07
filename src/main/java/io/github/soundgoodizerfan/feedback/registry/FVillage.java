// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.registry;

import java.util.function.Supplier;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.village.Ledger;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** The village economy's registrations: the villager's ledger. */
public final class FVillage {

    private FVillage() {
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Feedback.MOD_ID);

    /** What a villager remembers of dealing: orders done today, goods sold today, saturation bins. */
    public static final Supplier<AttachmentType<Ledger>> LEDGER = ATTACHMENTS.register("ledger",
            () -> AttachmentType.builder(() -> Ledger.EMPTY).serialize(Ledger.CODEC).build());

    /** What a raider carries out of a robbed Counter, dropped where it dies. */
    public static final Supplier<AttachmentType<java.util.List<net.minecraft.world.item.ItemStack>>> LOOT = ATTACHMENTS.register("loot",
            () -> AttachmentType.<java.util.List<net.minecraft.world.item.ItemStack>>builder(() -> java.util.List.of())
                    .serialize(net.minecraft.world.item.ItemStack.CODEC.listOf()).build());

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
