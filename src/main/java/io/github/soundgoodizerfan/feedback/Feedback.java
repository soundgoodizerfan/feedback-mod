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
package io.github.soundgoodizerfan.feedback;

import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FCapabilities;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import io.github.soundgoodizerfan.feedback.registry.FCreativeTabs;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FFluidTypes;
import io.github.soundgoodizerfan.feedback.registry.FFluids;
import io.github.soundgoodizerfan.feedback.registry.FMenus;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import io.github.soundgoodizerfan.feedback.control.data.DataLinkManager;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerActionPayload;
import io.github.soundgoodizerfan.feedback.net.DeformationSyncPayload;
import io.github.soundgoodizerfan.feedback.net.ThermalProcessSyncPayload;
import io.github.soundgoodizerfan.feedback.process.AlloyTable;
import io.github.soundgoodizerfan.feedback.process.CastingTable;
import io.github.soundgoodizerfan.feedback.process.DeformationTable;
import io.github.soundgoodizerfan.feedback.process.DipTable;
import io.github.soundgoodizerfan.feedback.process.FuelTable;
import io.github.soundgoodizerfan.feedback.process.QuenchTable;
import io.github.soundgoodizerfan.feedback.process.ThermalProcessTable;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(Feedback.MOD_ID)
public class Feedback {

    public static final String MOD_ID = "feedback";

    public static final Logger LOGGER = LogUtils.getLogger();

    public Feedback(IEventBus modBus, ModContainer container) {
        FFluidTypes.register(modBus);
        FFluids.register(modBus);
        FBlocks.register(modBus);
        FItems.register(modBus);
        FBlockEntities.register(modBus);
        FMenus.register(modBus);
        FCreativeTabs.register(modBus);
        FDataComponents.register(modBus);
        FCapabilities.register(modBus);
        FRecipes.register(modBus);

        NeoForge.EVENT_BUS.addListener(Feedback::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(Feedback::onDatapackSync);
        NeoForge.EVENT_BUS.addListener(Feedback::onPlayerLoggedOut);
        modBus.addListener(Feedback::registerPayloads);
    }

    /** Drops a half-made data link rather than let it outlive the session. */
    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        DataLinkManager.get().clear(event.getEntity());
    }

    /**
     * The deformation table is datapack data, so it reloads with the server rather than with
     * resources. Only the server needs it: the hammer runs server-side and nothing on the client
     * asks what a material becomes.
     */
    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(DeformationTable.get());
        event.addListener(ThermalProcessTable.get());
        event.addListener(QuenchTable.get());
        event.addListener(FuelTable.get());
        event.addListener(CastingTable.get());
        event.addListener(AlloyTable.get());
        event.addListener(DipTable.get());
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(DeformationSyncPayload.TYPE, DeformationSyncPayload.STREAM_CODEC,
                        DeformationSyncPayload::handle)
                .playToClient(ThermalProcessSyncPayload.TYPE, ThermalProcessSyncPayload.STREAM_CODEC,
                        ThermalProcessSyncPayload::handle)
                .playToServer(ProgrammerActionPayload.TYPE, ProgrammerActionPayload.STREAM_CODEC,
                        ProgrammerActionPayload::handle);
    }

    /**
     * Hand the client the deformation table whenever the server's copy could have changed.
     * <p>
     * The client needs it only to display requirements, which philosophy 8 gives away for free.
     * A null player means this was a {@code /reload} rather than a login, so everybody gets it.
     */
    private static void onDatapackSync(OnDatapackSyncEvent event) {
        send(event, new DeformationSyncPayload(DeformationTable.get().entries()));
        send(event, new ThermalProcessSyncPayload(ThermalProcessTable.get().entries()));
    }

    private static void send(OnDatapackSyncEvent event,
                             net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (event.getPlayer() != null)
            PacketDistributor.sendToPlayer(event.getPlayer(), payload);
        else
            PacketDistributor.sendToAllPlayers(payload);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
