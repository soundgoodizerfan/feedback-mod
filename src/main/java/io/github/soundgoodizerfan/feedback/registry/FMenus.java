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
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.registry;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerMenu;
import io.github.soundgoodizerfan.feedback.machine.crucible.CrucibleMenu;
import io.github.soundgoodizerfan.feedback.machine.vessel.ThermalVesselMenu;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** One menu type, shared by all three {@code VesselKind}s -- the layout never varies with kind. */
public class FMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, Feedback.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ThermalVesselMenu>> THERMAL_VESSEL =
            MENUS.register("thermal_vessel",
                    () -> IMenuTypeExtension.create((id, inv, buf) -> new ThermalVesselMenu(id, inv, buf.readVarInt())));

    public static final DeferredHolder<MenuType<?>, MenuType<CrucibleMenu>> ARC_HEARTH =
            MENUS.register("arc_hearth", () -> new MenuType<>(CrucibleMenu::arcHearth, FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<MenuType<?>, MenuType<CrucibleMenu>> LARGE_CRUCIBLE =
            MENUS.register("large_crucible", () -> new MenuType<>(CrucibleMenu::largeCrucible, FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<MenuType<?>, MenuType<CrucibleMenu>> CRUCIBLE =
            MENUS.register("crucible", () -> new MenuType<>(CrucibleMenu::new, FeatureFlags.VANILLA_SET));

    /** Extended, not plain -- the client needs the Programmer's {@code BlockPos} to open at. */
    public static final DeferredHolder<MenuType<?>, MenuType<ProgrammerMenu>> PROGRAMMER =
            MENUS.register("programmer",
                    () -> IMenuTypeExtension.create((id, inv, buf) -> new ProgrammerMenu(id, inv, buf.readBlockPos())));

    /** Extended: the table's position rides in the open packet; what it holds is synced as slots. */
    public static final DeferredHolder<MenuType<?>, MenuType<io.github.soundgoodizerfan.feedback.weapon.GunTableMenu>> GUN_TABLE =
            MENUS.register("gun_table",
                    () -> IMenuTypeExtension.create(io.github.soundgoodizerfan.feedback.weapon.GunTableMenu::new));

    /** Extended: as the Tool Table's. */
    public static final DeferredHolder<MenuType<?>, MenuType<io.github.soundgoodizerfan.feedback.modular.PrepTableMenu>> PREP_TABLE =
            MENUS.register("prep_table",
                    () -> IMenuTypeExtension.create(io.github.soundgoodizerfan.feedback.modular.PrepTableMenu::new));

    /** Extended: as the Tool Table's; the block at the position says which bench it is. */
    public static final DeferredHolder<MenuType<?>, MenuType<io.github.soundgoodizerfan.feedback.modular.StationMenu>> STATION =
            MENUS.register("station",
                    () -> IMenuTypeExtension.create(io.github.soundgoodizerfan.feedback.modular.StationMenu::new));

    /** Extended: the stand's position and its current design ride in the open packet. */
    public static final DeferredHolder<MenuType<?>, MenuType<io.github.soundgoodizerfan.feedback.weapon.ReloadingPressMenu>> RELOADING_PRESS =
            MENUS.register("reloading_press",
                    () -> IMenuTypeExtension.create(io.github.soundgoodizerfan.feedback.weapon.ReloadingPressMenu::new));


    /** Extended: the stand's position rides in the open packet; what it holds is synced as slots. */
    public static final DeferredHolder<MenuType<?>, MenuType<io.github.soundgoodizerfan.feedback.potion.BrewingStandMenu>> BREWING_STAND =
            MENUS.register("brewing_stand",
                    () -> IMenuTypeExtension.create(io.github.soundgoodizerfan.feedback.potion.BrewingStandMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<io.github.soundgoodizerfan.feedback.item.MaterialProbeMenu>> MATERIAL_PROBE =
            MENUS.register("material_probe",
                    () -> new MenuType<>(io.github.soundgoodizerfan.feedback.item.MaterialProbeMenu::new, FeatureFlags.VANILLA_SET));

    /** Plain: the table keeps nothing, so the client needs nothing to open it. */
    public static final DeferredHolder<MenuType<?>, MenuType<io.github.soundgoodizerfan.feedback.modular.BlueprintTableMenu>> BLUEPRINT_TABLE =
            MENUS.register("blueprint_table",
                    () -> new MenuType<>(io.github.soundgoodizerfan.feedback.modular.BlueprintTableMenu::new, FeatureFlags.VANILLA_SET));

    /** Plain: the table keeps nothing, so the client needs nothing to open it. */
    public static final DeferredHolder<MenuType<?>, MenuType<io.github.soundgoodizerfan.feedback.modular.EnchantMenu>> ENCHANT =
            MENUS.register("enchanting",
                    () -> new MenuType<>(io.github.soundgoodizerfan.feedback.modular.EnchantMenu::new, FeatureFlags.VANILLA_SET));

    /** Plain: the basin's slots and pool are synced as slots and data. */
    public static final DeferredHolder<MenuType<?>, MenuType<io.github.soundgoodizerfan.feedback.machine.basin.CastingBasinMenu>> CASTING_BASIN =
            MENUS.register("casting_basin",
                    () -> new MenuType<>(io.github.soundgoodizerfan.feedback.machine.basin.CastingBasinMenu::new, FeatureFlags.VANILLA_SET));

    /** Extended: the drum's position rides in the open packet; its slots are synced as slots, readings as data. */
    public static final DeferredHolder<MenuType<?>, MenuType<io.github.soundgoodizerfan.feedback.machine.mixer.MixerMenu>> MIXER =
            MENUS.register("mixer",
                    () -> IMenuTypeExtension.create(io.github.soundgoodizerfan.feedback.machine.mixer.MixerMenu::new));

    /** Extended: the worn pack's rows and the kind of each slot ride in the open packet; what it holds is synced as slots. */
    public static final DeferredHolder<MenuType<?>, MenuType<io.github.soundgoodizerfan.feedback.modular.PackMenu>> PACK =
            MENUS.register("pack",
                    () -> IMenuTypeExtension.create(io.github.soundgoodizerfan.feedback.modular.PackMenu::client));

    private FMenus() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }

    public static final DeferredHolder<MenuType<?>, MenuType<io.github.soundgoodizerfan.feedback.village.VillagerMenu>> VILLAGER =
            MENUS.register("villager",
                    () -> IMenuTypeExtension.create(io.github.soundgoodizerfan.feedback.village.VillagerMenu::new));
}
