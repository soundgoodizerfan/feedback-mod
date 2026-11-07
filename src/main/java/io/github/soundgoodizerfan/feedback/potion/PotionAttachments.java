// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.potion;

import java.util.function.Supplier;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** The player attachments of the potion system. */
public final class PotionAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Feedback.MOD_ID);

    /** What this player knows of the brewing map; kept on death. */
    public static final Supplier<AttachmentType<PotionKnowledge>> KNOWLEDGE = ATTACHMENTS.register("potion_knowledge",
            () -> AttachmentType.builder(() -> PotionKnowledge.EMPTY).serialize(PotionKnowledge.CODEC).copyOnDeath().build());

    private PotionAttachments() {}

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }
}
