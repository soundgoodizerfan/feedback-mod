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
package io.github.soundgoodizerfan.feedback.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularHeads;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.BarrelGeometry;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;
import io.github.soundgoodizerfan.feedback.weapon.Magazine;
import io.github.soundgoodizerfan.feedback.weapon.MagazineSize;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Test commands, op only: items that carry data no recipe browser can hand out -- a gun built
 * to a blueprint, the machined parts a blueprint lists, a barrel cut to any shape, rounds and
 * full magazines. A shortcut for testing crafting, never a route a survival player has.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class FCommands {

    private static final SimpleCommandExceptionType NO_BLUEPRINT =
            new SimpleCommandExceptionType(Component.translatable("commands.feedback.no_blueprint"));
    private static final SimpleCommandExceptionType NO_MATERIAL =
            new SimpleCommandExceptionType(Component.translatable("commands.feedback.no_material"));

    private FCommands() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("feedback").requires(s -> s.hasPermission(2))
                .then(Commands.literal("build").executes(FCommands::build)
                        .then(Commands.literal("parts").executes(FCommands::parts)))
                .then(Commands.literal("potion").then(Commands.literal("reveal").executes(c -> revealMap(c, List.of(c.getSource().getPlayerOrException())))
                        .then(Commands.argument("players", net.minecraft.commands.arguments.EntityArgument.players())
                                .executes(c -> revealMap(c, net.minecraft.commands.arguments.EntityArgument.getPlayers(c, "players"))))))
                .then(Commands.literal("summon").then(Commands.literal("gunskeleton").executes(c -> gunSkeleton(c, null))
                        .then(Commands.argument("gun", ResourceLocationArgument.id())
                                .suggests((c, b) -> SharedSuggestionProvider.suggestResource(
                                        io.github.soundgoodizerfan.feedback.weapon.GunBlueprintPool.get().earlyIds(), b))
                                .executes(c -> gunSkeleton(c, ResourceLocationArgument.getId(c, "gun"))))))
                .then(Commands.literal("barrel")
                        .then(Commands.argument("material", ResourceLocationArgument.id())
                                .suggests((c, b) -> SharedSuggestionProvider.suggestResource(MaterialTable.get().materials().keySet(), b))
                                .then(Commands.argument("bore", FloatArgumentType.floatArg(1f, 40f))
                                        .then(Commands.argument("wall", FloatArgumentType.floatArg(0.5f, 20f))
                                                .then(Commands.argument("length", FloatArgumentType.floatArg(0.05f, 2f))
                                                        .executes(FCommands::barrel))))))
                .then(Commands.literal("round")
                        .then(Commands.argument("bore", IntegerArgumentType.integer(GunSpec.MIN_BORE_MM, GunSpec.MAX_BORE_MM))
                                .executes(c -> round(c, false))
                                .then(Commands.argument("shot", BoolArgumentType.bool())
                                        .executes(c -> round(c, BoolArgumentType.getBool(c, "shot"))))))
                .then(Commands.literal("magazine")
                        .then(Commands.argument("size", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(MagazineSize.values()).map(MagazineSize::getSerializedName), b))
                                .then(Commands.argument("bore", IntegerArgumentType.integer(GunSpec.MIN_BORE_MM, GunSpec.MAX_BORE_MM))
                                        .executes(FCommands::magazine)))));
    }

    /** Testing: the brewing map fully known to each player (fog lifted, every node made and vortex used). */
    private static int revealMap(CommandContext<CommandSourceStack> c, java.util.Collection<ServerPlayer> players) {
        var all = io.github.soundgoodizerfan.feedback.potion.PotionKnowledge.everything(io.github.soundgoodizerfan.feedback.potion.PotionMap.get().on("water"));
        for (ServerPlayer p : players)
            p.setData(io.github.soundgoodizerfan.feedback.potion.PotionAttachments.KNOWLEDGE.get(), all);
        c.getSource().sendSuccess(() -> Component.translatable("commands.feedback.potion_revealed", players.size()), true);
        return players.size();
    }

    /** A skeleton at the source, always armed from the early pool (the named entry, else a random one); no chance roll. */
    private static int gunSkeleton(CommandContext<CommandSourceStack> c, ResourceLocation named) throws CommandSyntaxException {
        var level = c.getSource().getLevel();
        var pool = io.github.soundgoodizerfan.feedback.weapon.GunBlueprintPool.get();
        var picked = named == null ? pool.drawEarly(level.random)
                : pool.early(named);
        var skeleton = net.minecraft.world.entity.EntityType.SKELETON.create(level);
        if (skeleton == null || !io.github.soundgoodizerfan.feedback.weapon.ArmedSkeletons.equip(skeleton, picked))
            throw new SimpleCommandExceptionType(Component.translatable("commands.feedback.no_gun_skeleton")).create();
        skeleton.moveTo(c.getSource().getPosition());
        level.addFreshEntity(skeleton);
        c.getSource().sendSuccess(() -> Component.translatable("commands.feedback.gun_skeleton"), false);
        return 1;
    }

    private static ItemStack held(ServerPlayer player) throws CommandSyntaxException {
        ItemStack held = player.getMainHandItem();
        if (!held.is(io.github.soundgoodizerfan.feedback.registry.FModular.BLUEPRINT.get()) || ModularStacks.design(held) == null)
            throw NO_BLUEPRINT.create();
        return held;
    }

    /**
     * The item built exactly to the held blueprint, whatever it is: a gun, a pickaxe, a crossbow.
     * One blueprint item and one command, because a gun is a modular type like the rest (spec 8.3).
     */
    private static int build(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        ItemStack blueprint = held(player);
        GunAmmo.give(player, toolFor(blueprint));
        c.getSource().sendSuccess(() -> Component.translatable("commands.feedback.build"), false);
        return 1;
    }

    /** A modular item built exactly to {@code blueprint}'s design, figures baked, under its name. */
    public static ItemStack toolFor(ItemStack blueprint) throws CommandSyntaxException {
        ModularDesign design = ModularStacks.design(blueprint);
        // The item the Tool Table makes from this design's parts: the blueprint and the table cannot disagree.
        ItemStack out = design == null ? ItemStack.EMPTY
                : io.github.soundgoodizerfan.feedback.modular.ToolDesigns.build(design, ModularTypes.get().types(), MaterialTable.get().materials()).orElse(ItemStack.EMPTY);
        if (out.isEmpty())
            throw NO_BLUEPRINT.create();
        var named = blueprint.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if (named != null)
            out.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, named);
        return out;
    }

    /** The parts a modular design assembles from: a shaped part carries what it is made of, raw stock is its cells. */
    public static List<ItemStack> toolPartsFor(ModularDesign design) throws CommandSyntaxException {
        ModularType type = ModularTypes.get().types().get(design.type());
        var materials = MaterialTable.get().materials();
        List<ItemStack> out = new ArrayList<>();
        if (type == null)
            throw NO_BLUEPRINT.create();
        for (ModularDesign.Part p : design.parts()) {
            ModularType.Role role = type.roles().get(p.role());
            Material m = materials.get(p.material());
            if (role == null || m == null)
                throw NO_BLUEPRINT.create();
            if (role.part().isPresent() || role.striker()) {
                String kind = role.striker() ? ModularHeads.kindOf(p, role) : role.part().get().item().getPath();
                ItemStack part = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(Feedback.id(kind)));
                part.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get(),
                        new io.github.soundgoodizerfan.feedback.process.MadeOf(p.material(), p.form(), p.count(), p.variant(), p.flaw(), p.dims()));
                out.add(part);
            } else {
                ItemStack stock = m.stack(p.material(), p.form()).orElseThrow(NO_MATERIAL::create);
                for (int k = 0; k < p.count(); k++)
                    out.add(stock.copy());
            }
        }
        return out;
    }

    /** Every part the held blueprint lists, in its planned materials. */
    private static int parts(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        ItemStack blueprint = held(player);
        List<ItemStack> out = toolPartsFor(ModularStacks.design(blueprint));
        out.forEach(s -> GunAmmo.give(player, s));
        c.getSource().sendSuccess(() -> Component.translatable("commands.feedback.parts", out.size()), false);
        return out.size();
    }

    private static int barrel(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        ResourceLocation metal = ResourceLocationArgument.getId(c, "material");
        float bore = FloatArgumentType.getFloat(c, "bore"), wall = FloatArgumentType.getFloat(c, "wall");
        ItemStack barrel = form(MaterialTable.get().materials(), metal, "gun_barrel");
        barrel.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get(), io.github.soundgoodizerfan.feedback.process.Machining.withGeometry(
                barrel.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get()),
                new BarrelGeometry(bore, bore + 2 * wall, FloatArgumentType.getFloat(c, "length"))));
        GunAmmo.give(player, barrel);
        c.getSource().sendSuccess(() -> Component.translatable("commands.feedback.barrel"), false);
        return 1;
    }

    private static int round(CommandContext<CommandSourceStack> c, boolean shot) throws CommandSyntaxException {
        int bore = io.github.soundgoodizerfan.feedback.weapon.GunCalibres.nearest(IntegerArgumentType.getInteger(c, "bore")).boreMm();
        GunAmmo.give(c.getSource().getPlayerOrException(),
                GunAmmo.loose(shot ? Cartridge.referenceShot(bore) : Cartridge.reference(bore)).copyWithCount(16));
        return 1;
    }

    private static int magazine(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        MagazineSize size = Arrays.stream(MagazineSize.values())
                .filter(m -> m.getSerializedName().equals(StringArgumentType.getString(c, "size"))).findFirst()
                .orElseThrow(() -> new SimpleCommandExceptionType(Component.translatable("commands.feedback.no_size")).create());
        ItemStack mag = new ItemStack(FWeapons.MAGAZINE_ITEM.get());
        mag.set(FWeapons.MAGAZINE_SIZE.get(), size);
        mag.set(FWeapons.MAGAZINE.get(), new Magazine(Collections.nCopies(size.capacity(),
                Cartridge.reference(IntegerArgumentType.getInteger(c, "bore")))));
        GunAmmo.give(c.getSource().getPlayerOrException(), mag);
        return 1;
    }

    /** {@code metal}'s item in {@code form}, through the material system. */
    private static ItemStack form(java.util.Map<ResourceLocation, Material> materials, ResourceLocation metal, String form)
            throws CommandSyntaxException {
        Material m = materials.get(metal);
        var stack = m == null ? java.util.Optional.<ItemStack>empty() : m.stack(metal, form);
        if (stack.isEmpty())
            throw NO_MATERIAL.create();
        return stack.get();
    }
}
