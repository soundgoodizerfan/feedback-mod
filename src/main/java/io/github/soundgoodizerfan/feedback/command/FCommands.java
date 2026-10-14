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
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.BarrelGeometry;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunDesign;
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
                .then(Commands.literal("gun").executes(FCommands::gun))
                .then(Commands.literal("parts").executes(FCommands::parts))
                .then(Commands.literal("barrel")
                        .then(Commands.argument("material", ResourceLocationArgument.id())
                                .suggests((c, b) -> SharedSuggestionProvider.suggestResource(MaterialTable.get().materials().keySet(), b))
                                .then(Commands.argument("bore", FloatArgumentType.floatArg(1f, 40f))
                                        .then(Commands.argument("wall", FloatArgumentType.floatArg(0.5f, 20f))
                                                .then(Commands.argument("length", FloatArgumentType.floatArg(0.05f, 2f))
                                                        .executes(FCommands::barrel))))))
                .then(Commands.literal("round")
                        .then(Commands.argument("bore", IntegerArgumentType.integer(GunDesign.MIN_BORE_MM, GunDesign.MAX_BORE_MM))
                                .executes(c -> round(c, false))
                                .then(Commands.argument("shot", BoolArgumentType.bool())
                                        .executes(c -> round(c, BoolArgumentType.getBool(c, "shot"))))))
                .then(Commands.literal("magazine")
                        .then(Commands.argument("size", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(MagazineSize.values()).map(MagazineSize::getSerializedName), b))
                                .then(Commands.argument("bore", IntegerArgumentType.integer(GunDesign.MIN_BORE_MM, GunDesign.MAX_BORE_MM))
                                        .executes(FCommands::magazine)))));
    }

    private static GunDesign heldBlueprint(ServerPlayer player) throws CommandSyntaxException {
        ItemStack held = player.getMainHandItem();
        GunDesign design = held.get(FWeapons.GUN_DESIGN.get());
        if (!held.is(FWeapons.GUN_BLUEPRINT.get()) || design == null)
            throw NO_BLUEPRINT.create();
        return design;
    }

    /** A gun built exactly to the held blueprint: every part in the metal it plans, the barrel cut to its targets. */
    private static int gun(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        heldBlueprint(player);
        GunAmmo.give(player, gunFor(player.getMainHandItem()));
        c.getSource().sendSuccess(() -> Component.translatable("commands.feedback.gun"), false);
        return 1;
    }

    /** A gun built exactly to {@code blueprint}, figures baked, under the blueprint's name. */
    public static ItemStack gunFor(ItemStack blueprint) {
        GunDesign design = blueprint.get(FWeapons.GUN_DESIGN.get());
        GunDesign built = design.withBuilt(design.target());
        ItemStack gun = new ItemStack(FWeapons.GUN.get());
        gun.set(FWeapons.GUN_DESIGN.get(), built);
        built.bake(MaterialTable.get().materials()).ifPresent(f -> gun.set(FWeapons.GUN_FIGURES.get(), f));
        var named = blueprint.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if (named != null)
            gun.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, named);
        return gun;
    }

    /** Every machined part the held blueprint lists, in its planned metals, the barrel cut to target, and a coil's winding. */
    private static int parts(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        List<ItemStack> out = partsFor(heldBlueprint(player));
        out.forEach(s -> GunAmmo.give(player, s));
        c.getSource().sendSuccess(() -> Component.translatable("commands.feedback.parts", out.size()), false);
        return out.size();
    }

    /** The parts {@code design} assembles from, one stack each. */
    public static List<ItemStack> partsFor(GunDesign design) throws CommandSyntaxException {
        var materials = MaterialTable.get().materials();
        List<ItemStack> out = new ArrayList<>();
        for (var e : design.assembly().entrySet()) {
            ResourceLocation metal = switch (e.getKey()) {
                case "gun_barrel" -> design.material();
                case "slide", "bolt", "plate" -> design.parts().slide();
                case "trigger_group" -> design.parts().trigger();
                case "grip" -> design.parts().grip();
                default -> design.parts().receiver(); // receiver, cylinder, magazine tube
            };
            ItemStack part = form(materials, metal, e.getKey());
            if (e.getKey().equals("gun_barrel"))
                part.set(FWeapons.BARREL_GEOMETRY.get(), design.target());
            for (int k = 0; k < e.getValue(); k++)
                out.add(part.copy());
        }
        if (design.windingBlocks() > 0)
            GunDesign.windingItem(materials).ifPresent(w -> {
                for (int k = 0; k < design.windingBlocks(); k++)
                    out.add(new ItemStack(w));
            });
        return out;
    }

    private static int barrel(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        ResourceLocation metal = ResourceLocationArgument.getId(c, "material");
        float bore = FloatArgumentType.getFloat(c, "bore"), wall = FloatArgumentType.getFloat(c, "wall");
        ItemStack barrel = form(MaterialTable.get().materials(), metal, "gun_barrel");
        barrel.set(FWeapons.BARREL_GEOMETRY.get(), new BarrelGeometry(bore, bore + 2 * wall, FloatArgumentType.getFloat(c, "length")));
        GunAmmo.give(player, barrel);
        c.getSource().sendSuccess(() -> Component.translatable("commands.feedback.barrel"), false);
        return 1;
    }

    private static int round(CommandContext<CommandSourceStack> c, boolean shot) throws CommandSyntaxException {
        int bore = IntegerArgumentType.getInteger(c, "bore");
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
        var item = m == null ? java.util.Optional.<net.minecraft.world.item.Item>empty() : m.item(metal, form);
        if (item.isEmpty())
            throw NO_MATERIAL.create();
        return new ItemStack(item.get());
    }
}
