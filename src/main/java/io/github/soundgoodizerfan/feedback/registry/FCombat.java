// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.registry;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Combat's registrations. The attachments here are the first data attachments in the mod. */
public final class FCombat {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Feedback.MOD_ID);

    /** Synced to tracking clients, no serializer: transient by construction. */
    public static final Supplier<AttachmentType<CombatState>> STATE = ATTACHMENTS.register("combat_state",
            () -> AttachmentType.builder(() -> CombatState.IDLE).sync(CombatState.STREAM_CODEC).build());

    /** Every line holding this entity; synced for the rope render, transient. */
    public static final Supplier<AttachmentType<io.github.soundgoodizerfan.feedback.combat.tether.Tethers>> TETHERS =
            ATTACHMENTS.register("tethers", () -> AttachmentType.builder(() -> io.github.soundgoodizerfan.feedback.combat.tether.Tethers.NONE)
                    .sync(io.github.soundgoodizerfan.feedback.combat.tether.Tethers.STREAM_CODEC).build());

    // --- skill pools (own block) ---
    /** Points per tree, bought nodes, recent awards: persisted, kept on death, synced to the owner only. */
    public static final Supplier<AttachmentType<io.github.soundgoodizerfan.feedback.combat.skill.SkillPools>> SKILLS =
            ATTACHMENTS.register("skill_pools", () -> AttachmentType.builder(() -> io.github.soundgoodizerfan.feedback.combat.skill.SkillPools.EMPTY)
                    .serialize(io.github.soundgoodizerfan.feedback.combat.skill.SkillPools.CODEC).copyOnDeath()
                    .sync((holder, to) -> holder == to, io.github.soundgoodizerfan.feedback.combat.skill.SkillPools.STREAM_CODEC).build());

    /** Cast slots and stamina: persisted, kept on death, synced to the owner only. */
    public static final Supplier<AttachmentType<io.github.soundgoodizerfan.feedback.combat.cast.CastState>> CAST =
            ATTACHMENTS.register("cast", () -> AttachmentType.builder(() -> io.github.soundgoodizerfan.feedback.combat.cast.CastState.EMPTY)
                    .serialize(io.github.soundgoodizerfan.feedback.combat.cast.CastState.CODEC).copyOnDeath()
                    .sync((holder, to) -> holder == to, io.github.soundgoodizerfan.feedback.combat.cast.CastState.STREAM_CODEC).build());

    /** Radiation taken (Ru x s): persisted, lost on death, synced to its owner for a dosimeter; a body without one shows it only as symptoms. */
    public static final Supplier<AttachmentType<Float>> RADIATION_DOSE = ATTACHMENTS.register("radiation_dose",
            () -> AttachmentType.builder(() -> 0f).serialize(com.mojang.serialization.Codec.FLOAT)
                    .sync((holder, to) -> holder == to, net.minecraft.network.codec.ByteBufCodecs.FLOAT).build());

    private FCombat() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
