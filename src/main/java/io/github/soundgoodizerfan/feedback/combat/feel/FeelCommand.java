// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.feel;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.function.BiFunction;

/**
 * {@code /feedback feel}, op only: fires a {@link Feel} at yourself so the impact frames can be
 * tuned in game before any combat depends on them.
 *
 * <pre>
 * /feedback feel light | heavy                  a preset
 * /feedback feel light flash 1                  a preset with one figure replaced (flash, roll, fov, sparks)
 * /feedback feel custom &lt;flash&gt; &lt;roll&gt; &lt;fov&gt; &lt;sparks&gt; &lt;heavy&gt;
 * </pre>
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class FeelCommand {

    private FeelCommand() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var feel = Commands.literal("feel")
                .then(preset("light", FeelTuning.LIGHT))
                .then(preset("heavy", FeelTuning.HEAVY))
                .then(Commands.literal("custom")
                        .then(Commands.argument("flash", FloatArgumentType.floatArg(0f, 1f))
                                        .then(Commands.argument("roll", FloatArgumentType.floatArg(0f, FeelTuning.MAX_ROLL_DEG))
                                                .then(Commands.argument("fov", FloatArgumentType.floatArg(0f, FeelTuning.MAX_FOV_POP))
                                                        .then(Commands.argument("sparks", IntegerArgumentType.integer(0, FeelTuning.MAX_SPARKS))
                                                                .then(Commands.argument("heavy", BoolArgumentType.bool())
                                                                        .executes(c -> fire(c, new Feel(
                                                                                FloatArgumentType.getFloat(c, "flash"),
                                                                                FloatArgumentType.getFloat(c, "roll"),
                                                                                FloatArgumentType.getFloat(c, "fov"),
                                                                                BoolArgumentType.getBool(c, "heavy"),
                                                                                IntegerArgumentType.getInteger(c, "sparks"))))))))));
        dispatcher.register(Commands.literal("feedback").requires(s -> s.hasPermission(2)).then(feel));
    }

    /** A preset by itself, and with each figure replaceable. */
    private static ArgumentBuilder<CommandSourceStack, ?> preset(String name, Feel base) {
        var node = Commands.literal(name).executes(c -> fire(c, base));
        node.then(override("flash", FloatArgumentType.floatArg(0f, 1f), base,
                (f, c) -> new Feel(FloatArgumentType.getFloat(c, "value"), f.rollDegrees(), f.fovPop(), f.heavy(), f.sparks())));
        node.then(override("roll", FloatArgumentType.floatArg(0f, FeelTuning.MAX_ROLL_DEG), base,
                (f, c) -> new Feel(f.flash(), FloatArgumentType.getFloat(c, "value"), f.fovPop(), f.heavy(), f.sparks())));
        node.then(override("fov", FloatArgumentType.floatArg(0f, FeelTuning.MAX_FOV_POP), base,
                (f, c) -> new Feel(f.flash(), f.rollDegrees(), FloatArgumentType.getFloat(c, "value"), f.heavy(), f.sparks())));
        node.then(override("sparks", IntegerArgumentType.integer(0, FeelTuning.MAX_SPARKS), base,
                (f, c) -> new Feel(f.flash(), f.rollDegrees(), f.fovPop(), f.heavy(), IntegerArgumentType.getInteger(c, "value"))));
        return node;
    }

    private static <T> ArgumentBuilder<CommandSourceStack, ?> override(String field,
            com.mojang.brigadier.arguments.ArgumentType<T> type, Feel base,
            BiFunction<Feel, CommandContext<CommandSourceStack>, Feel> apply) {
        return Commands.literal(field).then(Commands.argument("value", type).executes(c -> fire(c, apply.apply(base, c))));
    }

    private static int fire(CommandContext<CommandSourceStack> c, Feel feel) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        feel.fire(player, player.getEyePosition().add(player.getLookAngle().scale(FeelTuning.COMMAND_DISTANCE)));
        c.getSource().sendSuccess(() -> Component.translatable("commands.feedback.feel", feel.flash(), feel.rollDegrees(), feel.fovPop(), feel.sparks(),
                Component.translatable(feel.heavy() ? "commands.feedback.feel.heavy" : "commands.feedback.feel.light")), false);
        return 1;
    }
}
