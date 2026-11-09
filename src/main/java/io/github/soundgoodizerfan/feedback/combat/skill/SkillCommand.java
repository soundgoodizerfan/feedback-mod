// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.skill;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.github.soundgoodizerfan.feedback.Feedback;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /feedback skill grant <tree> <points>} fills a pool; {@code /feedback skill unlock <tree|all>} owns every node
 * of a tree (or of every tree) for free. Op test shortcuts, never a survival route.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class SkillCommand {

    private SkillCommand() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> unlock = Commands.literal("unlock")
                .then(Commands.literal("all").executes(c -> unlock(c, null)))
                .then(Commands.argument("tree", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                SkillTrees.get().trees().stream().map(SkillTree::tree), b))
                        .executes(c -> unlock(c, StringArgumentType.getString(c, "tree"))));
        event.getDispatcher().register(Commands.literal("feedback").requires(s -> s.hasPermission(2))
                .then(Commands.literal("skill").then(unlock).then(Commands.literal("grant")
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

    /**
     * Owns every node of {@code only} (null for every tree) without paying. Of nodes sharing an exclusive tag
     * only the first is taken, as buying could not own two.
     */
    private static int unlock(CommandContext<CommandSourceStack> c, String only) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        var trees = SkillTrees.get().trees().stream().filter(t -> only == null || t.tree().equals(only)).toList();
        if (trees.isEmpty()) {
            c.getSource().sendFailure(Component.translatable("commands.feedback.skill.no_tree", only));
            return 0;
        }
        Set<String> owned = new HashSet<>(SkillPools.of(p).unlocked());
        int added = 0;
        for (SkillTree t : trees) {
            Set<String> taken = new HashSet<>();
            for (SkillTree.Node n : t.nodes())
                if (SkillPools.of(p).has(t.tree(), n.id()))
                    n.exclusive().ifPresent(taken::add);
            for (SkillTree.Node n : t.nodes()) {
                if (n.exclusive().isPresent() && !taken.add(n.exclusive().get()))
                    continue;
                if (owned.add(SkillPools.key(t.tree(), n.id())))
                    added++;
            }
        }
        SkillPools.set(p, SkillPools.of(p).withUnlocked(owned));
        int count = added;
        c.getSource().sendSuccess(() -> Component.translatable("commands.feedback.skill.unlocked",
                count, trees.size()), false);
        return added;
    }
}
