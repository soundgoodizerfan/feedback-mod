// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
        event.registerEntityRenderer(io.github.soundgoodizerfan.feedback.registry.FEntities.THROWN_WEAPON.get(),
                ThrownWeaponRenderer::new);
        event.registerEntityRenderer(io.github.soundgoodizerfan.feedback.registry.FEntities.GRAPPLE_HOOK.get(),
                GrappleHookRenderer::new);
        event.registerEntityRenderer(io.github.soundgoodizerfan.feedback.registry.FEntities.POTION_BOTTLE.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(io.github.soundgoodizerfan.feedback.registry.FEntities.GRENADE.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);

        event.registerEntityRenderer(io.github.soundgoodizerfan.feedback.registry.FEntities.STEAM_CLOUD.get(), SteamCloudRenderer::new);

        event.registerBlockEntityRenderer(FBlockEntities.SHAFT.get(), RotatingRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.COG.get(), RotatingRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.HAND_CRANK.get(), RotatingRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.WATER_WHEEL.get(), RotatingRenderer::new);
        event.registerBlockEntityRenderer(io.github.soundgoodizerfan.feedback.registry.FAutomation.FISH_WHEEL_ENTITY.get(), RotatingRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.CLUTCH.get(), RotatingRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.OVERSPEED_TRIP.get(), RotatingRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.STEAM_ENGINE.get(), RotatingRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.STEAM_TURBINE.get(), RotatingRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.PUMP.get(), RotatingRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.GEARBOX.get(), SpinningParts.Renderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.ROTATION_ADAPTER.get(), SpinningParts.Renderer::new);

        event.registerBlockEntityRenderer(FBlockEntities.PULLEY.get(), PulleyRenderer::new);

        event.registerBlockEntityRenderer(FBlockEntities.MECHANICAL_PRESS.get(), WorkpieceRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.ROLLING_MILL.get(), WorkpieceRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.WIRE_DRAWER.get(), WorkpieceRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.LATHE.get(), WorkpieceRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.DRILL_PRESS.get(), WorkpieceRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.GRINDING_WHEEL.get(), WorkpieceRenderer::new);

        // Every data node and fitting holder without a renderer of its own; the rest call
        // DataNodeRenderer.drawNodes from theirs.
        event.registerBlockEntityRenderer(FBlockEntities.SKILLET.get(), SkilletRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.STOCKPOT.get(), StockpotRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.CRUCIBLE.get(), GlowRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.THERMAL_VESSEL.get(), GlowRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.CASTING_BASIN.get(), GlowRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.DAMPER.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.VALVE.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.CABLE.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.ELECTROMAGNET.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.DYNAMOTOR.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.COIL.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.BREAKER.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.FUSE.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.LEYDEN_JAR.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.ELECTROLYTIC_CELL.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.GALVANIC_CELL.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(FBlockEntities.ELECTROSTATIC_GENERATOR.get(), DataNodeRenderer::new);
        event.registerBlockEntityRenderer(io.github.soundgoodizerfan.feedback.registry.FLogistics.BELT_ENTITY.get(), BeltModuleRenderer::new);
        event.registerBlockEntityRenderer(io.github.soundgoodizerfan.feedback.registry.FLogistics.HEAD_ENTITY.get(), BeltHeadRenderer::new);
        event.registerBlockEntityRenderer(io.github.soundgoodizerfan.feedback.registry.FAutomation.PIVOT_ENTITY.get(), PivotMastRenderer::new);
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
        rotatingVisual(io.github.soundgoodizerfan.feedback.registry.FAutomation.FISH_WHEEL_ENTITY.get());
        rotatingVisual(FBlockEntities.CLUTCH.get());
        rotatingVisual(FBlockEntities.OVERSPEED_TRIP.get());
        rotatingVisual(FBlockEntities.STEAM_ENGINE.get());
        rotatingVisual(FBlockEntities.STEAM_TURBINE.get());
        rotatingVisual(FBlockEntities.PUMP.get());
        rotatingVisual(FBlockEntities.PULLEY.get());
    }

    @SubscribeEvent
    public static void registerGuiLayers(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        GunHud.register(event);
        HudReadouts.register(event);
        AimHud.register(event);
        ItemInfoHud.register(event);
        FeelClient.register(event);
        ParryHud.register(event);
        RollReadyHud.register(event);
        OffhandHud.register(event);
        CastHud.register(event);
        SkillGainHud.register(event);
        SuitHud.register(event);
        ThermalOverlay.register(event);
        StoredHud.register(event);
        SizeUpHud.register(event);
    }

    @SubscribeEvent
    public static void registerKeys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(GunClient.RELOAD);
        event.register(GunClient.FIRE_MODE);
        event.register(GunClient.UNLOAD);
        event.register(OffhandStrikeClient.KEY);
        event.register(OffhandStrikeClient.BARE_TOGGLE);
        event.register(PackKey.KEY);
        event.register(SuitKeys.WHEEL);
        event.register(SuitKeys.DASH);
        event.register(SuitKeys.JET);
        event.register(SuitKeys.SCREEN);
        event.register(CombatKeys.PARRY);
        event.register(CombatKeys.ROLL);
        for (var k : CombatKeys.CAST)
            event.register(k);
        event.register(CombatKeys.RADIAL);
        event.register(GrappleSwing.PAY_OUT);
        event.register(ItemInfoHud.TOGGLE);
        event.register(SkillTreeScreen.KEY);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(FMenus.THERMAL_VESSEL.get(), ThermalVesselScreen::new);
        event.register(FMenus.CRUCIBLE.get(), CrucibleScreen::new);
        event.register(FMenus.LARGE_CRUCIBLE.get(), CrucibleScreen::new);
        event.register(FMenus.ARC_HEARTH.get(), CrucibleScreen::new);
        event.register(FMenus.MIXER.get(), MixerScreen::new);
        event.register(FMenus.BREWING_STAND.get(), StandScreen::new);
        event.register(FMenus.PROGRAMMER.get(), ProgrammerScreen::new);
        event.register(FMenus.COMPUTER.get(), ComputerScreen::new);
        event.register(FMenus.CASTING_BASIN.get(), CastingBasinScreen::new);
        event.register(FMenus.RELOADING_PRESS.get(), ReloadingPressScreen::new);
        event.register(FMenus.PREP_TABLE.get(), BenchScreen::new);
        event.register(FMenus.STATION.get(), BenchScreen::new);
        event.register(FMenus.ENGINEERING_TABLE.get(), EngineeringTableScreen::new);
        event.register(FMenus.MATERIAL_PROBE.get(), MaterialProbeScreen::new);
        event.register(FMenus.BLUEPRINT_TABLE.get(), BlueprintTableScreen::new);
        event.register(FMenus.ENCHANT.get(), EnchantScreen::new);
        event.register(FMenus.VILLAGER.get(), VillagerScreen::new);
        event.register(FMenus.COUNTER.get(), CounterScreen::new);
        event.register(FMenus.BOARD.get(), BoardScreen::new);
        event.register(FMenus.DEPOT.get(), DepotScreen::new);
    }

    /**
     * Placeholder art for every molten metal: one greyscale molten sprite, tinted per metal.
     * Malcolm Riley's fluid_molten, greyscaled -- see {@code THIRD-PARTY-LICENSES.md}.
     */
    @SubscribeEvent
    public static void registerFluidRendering(RegisterClientExtensionsEvent event) {
        // The Haste potion carries a vanilla Haste with showIcon off; the inventory screen ignores that flag, so it listed Haste twice.
        event.registerMobEffect(new net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions() {
            @Override
            public boolean isVisibleInInventory(net.minecraft.world.effect.MobEffectInstance instance) {
                return instance.showIcon();
            }
        }, net.minecraft.world.effect.MobEffects.DIG_SPEED);
        event.registerFluidType(moltenFluid(0xFFFF8A4B), FFluidTypes.MOLTEN_COPPER.get());
        event.registerFluidType(moltenFluid(0xFFFFE066), FFluidTypes.MOLTEN_GOLD.get());
        event.registerFluidType(moltenFluid(0xFFE0E0E0), FFluidTypes.MOLTEN_WROUGHT_IRON.get());
        event.registerFluidType(moltenFluid(0xFFEAEAEA), FFluidTypes.MOLTEN_IRON.get());
        event.registerFluidType(moltenFluid(0xFFB8B8C4), FFluidTypes.MOLTEN_TIN.get());
        event.registerFluidType(moltenFluid(0xFFE6EAF0), FFluidTypes.MOLTEN_SILVER.get());
        event.registerFluidType(moltenFluid(0xFFA9C28F), FFluidTypes.MOLTEN_URANIUM.get());
        event.registerFluidType(moltenFluid(0xFF3A2F3F), FFluidTypes.MOLTEN_CAST_NETHERITE.get());
        event.registerFluidType(moltenFluid(0xFF6E5A4A), FFluidTypes.MOLTEN_SLAG.get());
        event.registerFluidType(moltenFluid(0xFF8A8A92), FFluidTypes.MOLTEN_CAST_IRON.get());
        event.registerFluidType(moltenFluid(0xFF8C97A8), FFluidTypes.MOLTEN_ZINC.get());
        event.registerFluidType(moltenFluid(0xFFCD8A4D), FFluidTypes.MOLTEN_BRONZE.get());
        event.registerFluidType(moltenFluid(0xFFE8C464), FFluidTypes.MOLTEN_BRASS.get());
        event.registerFluidType(moltenFluid(0xFFA9B2C4), FFluidTypes.MOLTEN_SOLDER_LEADED.get());
        event.registerFluidType(moltenFluid(0xFFD2DADF), FFluidTypes.MOLTEN_SOLDER_FREE.get());
        event.registerFluidType(moltenFluid(0xFFB9E4D8), FFluidTypes.MOLTEN_GLASS.get());
        event.registerFluidType(moltenFluid(0xFF9AA0A8), FFluidTypes.MOLTEN_LEAD.get());
        event.registerFluidType(moltenFluid(0xFFE2F2F5), FFluidTypes.MOLTEN_QUARTZ_GLASS.get());
        io.github.soundgoodizerfan.feedback.registry.MoltenMetals.types().forEach((id, type) ->
                event.registerFluidType(moltenFluid(io.github.soundgoodizerfan.feedback.registry.MoltenMetals.tint(id)), type.get()));
        event.registerFluidType(steamFluid(), FFluidTypes.STEAM.get());
        // Petrochemistry: vanilla water sprites tinted by hand (fluid materials carry no colour field).
        event.registerFluidType(waterTinted(0xFF1A1410), FFluidTypes.CRUDE_OIL.get());
        event.registerFluidType(waterTinted(0xFF0E0B09), FFluidTypes.HEAVY_OIL.get());
        event.registerFluidType(waterTinted(0xFFE8D9A0), FFluidTypes.NAPHTHA.get());
        event.registerFluidType(waterTinted(0xFFD9C27A), FFluidTypes.KEROSENE.get());
        event.registerFluidType(waterTinted(0xFFB8924A), FFluidTypes.DIESEL.get());
        event.registerFluidType(waterTinted(0x99E8D9A0), FFluidTypes.NAPHTHA_VAPOUR.get());
        event.registerFluidType(waterTinted(0x99D9C27A), FFluidTypes.KEROSENE_VAPOUR.get());
        event.registerFluidType(waterTinted(0x99B8924A), FFluidTypes.DIESEL_VAPOUR.get());
        event.registerFluidType(waterTinted(0xFF2A1A3A), FFluidTypes.SOUL_OIL.get());
        event.registerFluidType(waterTinted(0xFFE6E6D8), FFluidTypes.POLYETHYLENE_MELT.get());
        event.registerFluidType(waterTinted(0xFF4A9A9A), FFluidTypes.SOUL_RESIN_MELT.get());
        event.registerFluidType(waterTinted(0x66DDE8EE), FFluidTypes.ETHYLENE.get());
        event.registerFluidType(waterTinted(0x8869E0E0), FFluidTypes.WISP_GAS.get());
        event.registerFluidType(waterTinted(0x33DDEEFF), FFluidTypes.AIR.get());
        event.registerFluidType(waterTinted(0x44AADDFF), FFluidTypes.OXYGEN.get());
        event.registerFluidType(waterTinted(0x33FFFFFF), FFluidTypes.HYDROGEN.get());
        event.registerFluidType(waterTinted(0xFFB4C4CE), FFluidTypes.BRINE.get());
        event.registerFluidType(waterTinted(0xFFE8E4D8), FFluidTypes.LYE.get());
        event.registerFluidType(waterTinted(0xFFD8E8A8), FFluidTypes.HYDROCHLORIC_ACID.get());
        event.registerFluidType(waterTinted(0xFFC8B878), FFluidTypes.SULFURIC_ACID.get());
        event.registerFluidType(waterTinted(0x33FFE8C8), FFluidTypes.METHANE.get());
        event.registerFluidType(waterTinted(0xFFE8F0E8), FFluidTypes.CHLOROFORM.get());
        event.registerFluidType(waterTinted(0x44E0F0F0), FFluidTypes.CHLORODIFLUOROMETHANE.get());
        event.registerFluidType(waterTinted(0xFFE0F0D0), FFluidTypes.HYDROFLUORIC_ACID.get());
        event.registerFluidType(waterTinted(0x66EEF2F4), FFluidTypes.TETRAFLUOROETHYLENE.get());
        event.registerFluidType(waterTinted(0xFFD0E070), FFluidTypes.HYPOCHLORITE.get());
        event.registerFluidType(waterTinted(0x88C8E060), FFluidTypes.CHLORINE.get());
        event.registerFluidType(waterTinted(0x66E8E8D0), FFluidTypes.SULFUR_DIOXIDE.get());
        event.registerFluidType(waterTinted(0x44E8F0FF), FFluidTypes.AMMONIA.get());
        event.registerFluidType(waterTinted(0x33CCDDEE), FFluidTypes.NITROGEN.get());
        event.registerFluidType(waterTinted(0x33DDCCEE), FFluidTypes.ARGON.get());
        event.registerFluidType(waterTinted(0xCCCFE6F2), FFluidTypes.LIQUID_AIR.get());
        event.registerFluidType(waterTinted(0xCCBFD8F0), FFluidTypes.LIQUID_OXYGEN.get());
        event.registerFluidType(moltenFluid(0xFFFFD8A0), FFluidTypes.CRYOLITE_BATH.get());
        event.registerFluidType(waterTinted(0xFFB070E8), FFluidTypes.TELEPORTANT.get());
        event.registerFluidType(waterTinted(0x88C9A0F0), FFluidTypes.UNSTABLE_SOLVENT_VAPOUR.get());
        io.github.soundgoodizerfan.feedback.registry.Liquors.types().forEach((id, type) ->
                event.registerFluidType(liquorFluid(io.github.soundgoodizerfan.feedback.registry.Liquors.tint(id)), type.get()));
        io.github.soundgoodizerfan.feedback.registry.DyeFluids.types().forEach((color, type) ->
                event.registerFluidType(waterTinted(0xFF000000 | color.getTextureDiffuseColor()), type.get()));
    }

    /** A leach liquor: the animated grey liquor sprite, tinted by the material's colour. */
    private static IClientFluidTypeExtensions liquorFluid(int tint) {
        return new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return Feedback.id("block/liquor");
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return Feedback.id("block/liquor");
            }

            @Override
            public int getTintColor() {
                return tint;
            }
        };
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

    private static IClientFluidTypeExtensions waterTinted(int tint) {
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
