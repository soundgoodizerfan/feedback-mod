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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.combat.skill;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Ships the skill trees so the client can draw them and predict figure modifiers. */
public record SkillTreeSyncPayload(List<SkillTree> trees) implements CustomPacketPayload {

    public static final Type<SkillTreeSyncPayload> TYPE = new Type<>(Feedback.id("skill_tree_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SkillTreeSyncPayload> STREAM_CODEC =
            StreamCodec.composite(SkillTree.STREAM_CODEC.apply(ByteBufCodecs.list()), SkillTreeSyncPayload::trees,
                    SkillTreeSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SkillTreeSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SkillTrees.get().set(payload.trees()));
    }
}
