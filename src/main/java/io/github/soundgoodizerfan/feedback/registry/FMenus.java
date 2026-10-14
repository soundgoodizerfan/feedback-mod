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
                    () -> new MenuType<>(ThermalVesselMenu::new, FeatureFlags.VANILLA_SET));

    /** Extended, not plain -- the client needs the Programmer's {@code BlockPos} to open at. */
    public static final DeferredHolder<MenuType<?>, MenuType<ProgrammerMenu>> PROGRAMMER =
            MENUS.register("programmer",
                    () -> IMenuTypeExtension.create((id, inv, buf) -> new ProgrammerMenu(id, inv, buf.readBlockPos())));

    /** Plain: the bench keeps nothing, so the client needs nothing to open it. */
    public static final DeferredHolder<MenuType<?>, MenuType<io.github.soundgoodizerfan.feedback.weapon.AssemblyBenchMenu>> ASSEMBLY_BENCH =
            MENUS.register("assembly_bench",
                    () -> new MenuType<>(io.github.soundgoodizerfan.feedback.weapon.AssemblyBenchMenu::new, FeatureFlags.VANILLA_SET));

    /** Extended: the stand's position and its current design ride in the open packet. */
    public static final DeferredHolder<MenuType<?>, MenuType<io.github.soundgoodizerfan.feedback.weapon.ReloadingPressMenu>> RELOADING_PRESS =
            MENUS.register("reloading_press",
                    () -> IMenuTypeExtension.create(io.github.soundgoodizerfan.feedback.weapon.ReloadingPressMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<io.github.soundgoodizerfan.feedback.weapon.GunStandMenu>> GUN_STAND =
            MENUS.register("gun_stand",
                    () -> IMenuTypeExtension.create(io.github.soundgoodizerfan.feedback.weapon.GunStandMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<io.github.soundgoodizerfan.feedback.vehicle.engine.EngineStandMenu>> ENGINE_STAND =
            MENUS.register("engine_stand",
                    () -> IMenuTypeExtension.create(io.github.soundgoodizerfan.feedback.vehicle.engine.EngineStandMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<io.github.soundgoodizerfan.feedback.item.MaterialProbeMenu>> MATERIAL_PROBE =
            MENUS.register("material_probe",
                    () -> new MenuType<>(io.github.soundgoodizerfan.feedback.item.MaterialProbeMenu::new, FeatureFlags.VANILLA_SET));

    private FMenus() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
