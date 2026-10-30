/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
