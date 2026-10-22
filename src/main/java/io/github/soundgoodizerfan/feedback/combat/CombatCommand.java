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
package io.github.soundgoodizerfan.feedback.combat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryStats;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.roll.RollTuning;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /feedback combat}: the figures the held weapon gives the parry and the base roll, and the
 * fighter's current state, all at the truth tier (exact), so the curve can be tuned by playing.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class CombatCommand {

    private CombatCommand() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("feedback").requires(s -> s.hasPermission(2))
                .then(Commands.literal("combat").executes(c -> report(c.getSource()))));
    }

    private static int report(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ParryStats.of(player).ifPresentOrElse(st -> source.sendSuccess(() -> Component.translatable(
                "commands.feedback.combat.parry", st.window(), st.recovery(), ParryTuning.PING_CAP, st.coneHalfDeg() * 2,
                st.mass(), st.hand().name()), false),
                () -> source.sendSuccess(() -> Component.translatable("commands.feedback.combat.no_parry"), false));
        source.sendSuccess(() -> Component.translatable("commands.feedback.combat.roll",
                RollTuning.I_FRAMES, RollTuning.RECOVERY, RollTuning.COOLDOWN, RollTuning.SPEED / (1 - 0.546)), false);
        CombatState s = CombatState.of(player);
        source.sendSuccess(() -> Component.translatable("commands.feedback.combat.state",
                s.phase().name(), s.ticksLeft(), s.then(), s.iFrames()), false);
        return 1;
    }
}
