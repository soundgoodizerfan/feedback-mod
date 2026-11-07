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
