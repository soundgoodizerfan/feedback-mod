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
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FFluidTypes;
import io.github.soundgoodizerfan.feedback.registry.FMenus;

import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Client registration. The whole class is {@link Dist#CLIENT}, and has to be: Flywheel is a
 * client-only mod, so a dedicated server that so much as resolved these class references would
 * fail to start.
 *
 * <p>Every spinning block is named twice here — once for a visualizer, once for the fallback
 * renderer — and a third time on the block itself, as a {@code RenderShape} that keeps it out of
 * the chunk mesh. All three are required together. Two of them and the block is drawn twice, or
 * not at all.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FClientSetup {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FBlockEntities.MECHANICAL_HAMMER.get(), MechanicalHammerRenderer::new);

        event.registerBlockEntityRenderer(FBlockEntities.SHAFT.get(), RotatingRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.COG.get(), RotatingRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.HAND_CRANK.get(), RotatingRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.WATER_WHEEL.get(), RotatingRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.CLUTCH.get(), RotatingRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.OVERSPEED_TRIP.get(), RotatingRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.STEAM_ENGINE.get(), RotatingRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.PUMP.get(), RotatingRenderer::new);

        event.registerBlockEntityRenderer(FBlockEntities.PULLEY.get(), PulleyRenderer::new);

        event.registerBlockEntityRenderer(FBlockEntities.MECHANICAL_PRESS.get(), WorkpieceRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.ROLLING_MILL.get(), WorkpieceRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.WIRE_DRAWER.get(), WorkpieceRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.LATHE.get(), WorkpieceRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.DRILL_PRESS.get(), WorkpieceRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.GRINDING_WHEEL.get(), WorkpieceRenderer::new);

        // Every data node and fitting holder without a renderer of its own; the rest call
        // DataNodeRenderer.drawNodes from theirs.
        event.registerBlockEntityRenderer(FBlockEntities.CRUCIBLE.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.THERMAL_VESSEL.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.DAMPER.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.VALVE.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.CABLE.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.ELECTROMAGNET.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.DYNAMO.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.MOTOR.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.COIL.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.BREAKER.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.FUSE.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.LEYDEN_JAR.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.ELECTROLYTIC_CELL.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.GALVANIC_CELL.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.ELECTROSTATIC_GENERATOR.get(), DataNodeRenderer::new);
    }

    /**
     * Visualizers go in during client setup rather than alongside the renderers, because Flywheel's
     * registry is a plain map keyed by block entity type and has no registration event of its own.
     */
    @SubscribeEvent
    public static void registerVisualizers(FMLClientSetupEvent event) {
        rotatingVisual(FBlockEntities.SHAFT.get());
        rotatingVisual(FBlockEntities.COG.get());
        rotatingVisual(FBlockEntities.HAND_CRANK.get());
        rotatingVisual(FBlockEntities.WATER_WHEEL.get());
        rotatingVisual(FBlockEntities.CLUTCH.get());
        rotatingVisual(FBlockEntities.OVERSPEED_TRIP.get());
        rotatingVisual(FBlockEntities.STEAM_ENGINE.get());
        rotatingVisual(FBlockEntities.PUMP.get());
        rotatingVisual(FBlockEntities.PULLEY.get());
    }

    @SubscribeEvent
    public static void registerGuiLayers(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        GunHud.register(event);
        BuildToolHud.register(event);
    }

    @SubscribeEvent
    public static void registerKeys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(GunClient.RELOAD);
        event.register(GunClient.FIRE_MODE);
        event.register(GunClient.UNLOAD);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(FMenus.THERMAL_VESSEL.get(), ThermalVesselScreen::new);
        event.register(FMenus.PROGRAMMER.get(), ProgrammerScreen::new);
        event.register(FMenus.ENGINE_STAND.get(), EngineStandScreen::new);
        event.register(FMenus.GUN_STAND.get(), GunStandScreen::new);
        event.register(FMenus.RELOADING_PRESS.get(), ReloadingPressScreen::new);
        event.register(FMenus.ASSEMBLY_BENCH.get(), AssemblyBenchScreen::new);
        event.register(FMenus.MATERIAL_PROBE.get(), MaterialProbeScreen::new);
        event.register(FMenus.BLUEPRINT_TABLE.get(), BlueprintTableScreen::new);
    }

    /**
     * Placeholder art for every molten metal: one greyscale molten sprite, tinted per metal.
     * Malcolm Riley's fluid_molten, greyscaled -- see {@code THIRD-PARTY-LICENSES.md}.
     */
    @SubscribeEvent
    public static void registerFluidRendering(RegisterClientExtensionsEvent event) {
        event.registerFluidType(moltenFluid(0xFFFF8A4B), FFluidTypes.MOLTEN_COPPER.get());
        event.registerFluidType(moltenFluid(0xFFFFE066), FFluidTypes.MOLTEN_GOLD.get());
        event.registerFluidType(moltenFluid(0xFFE0E0E0), FFluidTypes.MOLTEN_IRON.get());
        event.registerFluidType(moltenFluid(0xFFB8B8C4), FFluidTypes.MOLTEN_TIN.get());
        event.registerFluidType(moltenFluid(0xFF8C97A8), FFluidTypes.MOLTEN_ZINC.get());
        event.registerFluidType(moltenFluid(0xFFCD8A4D), FFluidTypes.MOLTEN_BRONZE.get());
        event.registerFluidType(moltenFluid(0xFFE8C464), FFluidTypes.MOLTEN_BRASS.get());
        event.registerFluidType(moltenFluid(0xFFB9E4D8), FFluidTypes.MOLTEN_GLASS.get());
        event.registerFluidType(moltenFluid(0xFF9AA0A8), FFluidTypes.MOLTEN_LEAD.get());
        event.registerFluidType(moltenFluid(0xFFE2F2F5), FFluidTypes.MOLTEN_QUARTZ_GLASS.get());
        event.registerFluidType(steamFluid(), FFluidTypes.STEAM.get());
        event.registerFluidType(steamFluid(), FFluidTypes.SPENT_STEAM.get());
    }

    private static IClientFluidTypeExtensions moltenFluid(int tint) {
        return new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return Feedback.id("block/molten_metal");
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return Feedback.id("block/molten_metal");
            }

            @Override
            public int getTintColor() {
                return tint;
            }
        };
    }

    /** Vanilla water's own textures, pale and near-opaque -- placeholder art, same rule as
     * every molten metal above. */
    private static IClientFluidTypeExtensions steamFluid() {
        return new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_still");
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_flow");
            }

            @Override
            public int getTintColor() {
                return 0xFFEEEEEE;
            }
        };
    }

    private static <T extends RotationNode> void rotatingVisual(BlockEntityType<T> type) {
        SimpleBlockEntityVisualizer.builder(type)
                .factory(RotatingVisual.factory())
                .apply();
    }
}
