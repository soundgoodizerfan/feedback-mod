// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
