// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client registration for the vehicle system, kept beside {@link FVehicles}' own registers. */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class VehicleClientSetup {

    private VehicleClientSetup() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FVehicles.FRAME_ENTITY.get(), FrameRenderer::new);
        event.registerBlockEntityRenderer(FVehicles.PROPELLER_ENTITY.get(), PropRenderer::new);
        event.registerBlockEntityRenderer(FVehicles.CHASSIS_CELL_ENTITY.get(), ChassisCellRenderer::new);
        event.registerBlockEntityRenderer(FVehicles.MICRO_CELL_ENTITY.get(), MicroCellRenderer::new);
        event.registerEntityRenderer(FVehicles.SEAT_RIDE.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
    }
}
