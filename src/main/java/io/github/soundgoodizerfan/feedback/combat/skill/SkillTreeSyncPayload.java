// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
