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

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /feedback skill grant <tree> <points>}: an op test shortcut to fill a pool. Never a survival route. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class SkillCommand {

    private SkillCommand() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("feedback").requires(s -> s.hasPermission(2))
                .then(Commands.literal("skill").then(Commands.literal("grant")
                        .then(Commands.argument("tree", StringArgumentType.word())
                                .then(Commands.argument("points", IntegerArgumentType.integer())
                                        .executes(c -> {
                                            ServerPlayer p = c.getSource().getPlayerOrException();
                                            String tree = StringArgumentType.getString(c, "tree");
                                            SkillPools.set(p, SkillPools.of(p).withPoints(tree,
                                                    SkillPools.of(p).points(tree) + IntegerArgumentType.getInteger(c, "points")));
                                            c.getSource().sendSuccess(() -> Component.translatable("commands.feedback.skill.points",
                                                    tree, SkillPools.of(p).points(tree)), false);
                                            return 1;
                                        }))))));
    }
}
