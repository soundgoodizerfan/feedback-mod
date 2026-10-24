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

    private FCombat() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
