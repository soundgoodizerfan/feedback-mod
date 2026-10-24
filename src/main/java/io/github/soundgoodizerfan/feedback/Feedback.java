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
import io.github.soundgoodizerfan.feedback.registry.FVehicles;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import io.github.soundgoodizerfan.feedback.control.data.DataLinkManager;
import io.github.soundgoodizerfan.feedback.machine.pulley.BeltLinkManager;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerCommitPayload;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerLinkPayload;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerLinkedPayload;
import io.github.soundgoodizerfan.feedback.net.DeformationSyncPayload;
import io.github.soundgoodizerfan.feedback.net.MaterialSyncPayload;
import io.github.soundgoodizerfan.feedback.net.RemovalSyncPayload;
import io.github.soundgoodizerfan.feedback.net.AmbientSyncPayload;
import io.github.soundgoodizerfan.feedback.net.ThermalProcessSyncPayload;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.process.AlloyTable;
import io.github.soundgoodizerfan.feedback.process.CastingTable;
import io.github.soundgoodizerfan.feedback.process.DeformationTable;
import io.github.soundgoodizerfan.feedback.process.FormTable;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.process.DipTable;
import io.github.soundgoodizerfan.feedback.process.QuenchTable;
import io.github.soundgoodizerfan.feedback.process.RemovalTable;
import io.github.soundgoodizerfan.feedback.process.ThermalProcessTable;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import io.github.soundgoodizerfan.feedback.net.ToolScrollPayload;
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
        FVehicles.register(modBus);
        io.github.soundgoodizerfan.feedback.registry.FWeapons.register(modBus);
        io.github.soundgoodizerfan.feedback.registry.FSounds.register(modBus);
        FBlockEntities.register(modBus);
        FMenus.register(modBus);
        FCreativeTabs.register(modBus);
        FDataComponents.register(modBus);
        FCapabilities.register(modBus);
        FRecipes.register(modBus);
        io.github.soundgoodizerfan.feedback.registry.FModular.register(modBus);
        io.github.soundgoodizerfan.feedback.registry.FEntities.register(modBus);
        io.github.soundgoodizerfan.feedback.registry.FCombat.register(modBus);
        io.github.soundgoodizerfan.feedback.registry.FWorldgen.register(modBus);

        NeoForge.EVENT_BUS.addListener(Feedback::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(io.github.soundgoodizerfan.feedback.modular.ModularEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(io.github.soundgoodizerfan.feedback.modular.ModularEvents::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(io.github.soundgoodizerfan.feedback.combat.tether.Tethers::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(io.github.soundgoodizerfan.feedback.modular.ModularEvents::onShieldBlock);
        NeoForge.EVENT_BUS.addListener(io.github.soundgoodizerfan.feedback.modular.ModularEvents::onGrindstone);
        NeoForge.EVENT_BUS.addListener(io.github.soundgoodizerfan.feedback.modular.ModularEvents::onVillagerTrades);
        NeoForge.EVENT_BUS.addListener(io.github.soundgoodizerfan.feedback.modular.ModularEvents::onWandererTrades);
        NeoForge.EVENT_BUS.addListener(io.github.soundgoodizerfan.feedback.weapon.ArmedSkeletons::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(io.github.soundgoodizerfan.feedback.weapon.ArmedSkeletons::onDrops);
        NeoForge.EVENT_BUS.addListener(io.github.soundgoodizerfan.feedback.worldgen.VanillaVesselSwap::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(io.github.soundgoodizerfan.feedback.worldgen.VanillaVesselSwap::onBlockPlaced);
        NeoForge.EVENT_BUS.addListener(io.github.soundgoodizerfan.feedback.worldgen.VanillaVesselSwap::onServerTick);
        NeoForge.EVENT_BUS.addListener(Feedback::onDatapackSync);
        NeoForge.EVENT_BUS.addListener(Feedback::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(io.github.soundgoodizerfan.feedback.core.hazard.HazardEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(io.github.soundgoodizerfan.feedback.core.hazard.HazardEvents::onRightClickBlock);
        modBus.addListener(Feedback::registerPayloads);
        modBus.addListener(io.github.soundgoodizerfan.feedback.data.FeedbackData::gather);
    }

    /** Drops a half-made data link rather than let it outlive the session. */
    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        DataLinkManager.get().clear(event.getEntity());
        BeltLinkManager.get().clear(event.getEntity());
    }

    /**
     * The process tables are datapack data, so they reload with the server rather than with
     * resources. Three of them -- deformation, removal and thermal process -- are also sent to the
     * client (see {@link #onDatapackSync}), but only so JEI can print requirements; nothing on the
     * client computes with them.
     */
    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(AmbientTable.get());
        event.addListener(io.github.soundgoodizerfan.feedback.modular.ShieldSources.get());
        event.addListener(MaterialTable.get());
        event.addListener(FormTable.get());
        event.addListener(ThermalProcessTable.get());
        event.addListener(QuenchTable.get());
        event.addListener(io.github.soundgoodizerfan.feedback.process.Machining.get());
        event.addListener(CastingTable.get());
        event.addListener(AlloyTable.get());
        event.addListener(DipTable.get());
        event.addListener(io.github.soundgoodizerfan.feedback.process.PhaseChangeTable.get());
        event.addListener(io.github.soundgoodizerfan.feedback.worldgen.OreVeinTable.get());
        event.addListener(io.github.soundgoodizerfan.feedback.modular.ModularTypes.get());
        event.addListener(io.github.soundgoodizerfan.feedback.weapon.GunOptions.get());
        event.addListener(io.github.soundgoodizerfan.feedback.modular.Attunements.get());
        event.addListener(io.github.soundgoodizerfan.feedback.weapon.GunBlueprintPool.get());
        event.addListener(io.github.soundgoodizerfan.feedback.combat.skill.SkillTrees.get());
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("3")
                .playToClient(io.github.soundgoodizerfan.feedback.net.SoundingPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.SoundingPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.SoundingPayload::handle)
                .playToClient(AmbientSyncPayload.TYPE, AmbientSyncPayload.STREAM_CODEC,
                        AmbientSyncPayload::handle)
                .playToClient(MaterialSyncPayload.TYPE, MaterialSyncPayload.STREAM_CODEC,
                        MaterialSyncPayload::handle)
                .playToClient(DeformationSyncPayload.TYPE, DeformationSyncPayload.STREAM_CODEC,
                        DeformationSyncPayload::handle)
                .playToClient(RemovalSyncPayload.TYPE, RemovalSyncPayload.STREAM_CODEC,
                        RemovalSyncPayload::handle)
                .playToClient(io.github.soundgoodizerfan.feedback.net.CastingSyncPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.CastingSyncPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.CastingSyncPayload::handle)
                .playToClient(io.github.soundgoodizerfan.feedback.net.FrameSpinPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.FrameSpinPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.FrameSpinPayload::handle)
                .playToClient(io.github.soundgoodizerfan.feedback.net.QuenchSyncPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.QuenchSyncPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.QuenchSyncPayload::handle)
                .playToClient(io.github.soundgoodizerfan.feedback.net.AlloySyncPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.AlloySyncPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.AlloySyncPayload::handle)
                .playToClient(io.github.soundgoodizerfan.feedback.net.PhaseChangeSyncPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.PhaseChangeSyncPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.PhaseChangeSyncPayload::handle)
                .playToClient(io.github.soundgoodizerfan.feedback.net.DipSyncPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.DipSyncPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.DipSyncPayload::handle)
                .playToClient(ThermalProcessSyncPayload.TYPE, ThermalProcessSyncPayload.STREAM_CODEC,
                        ThermalProcessSyncPayload::handle)
                .playToClient(io.github.soundgoodizerfan.feedback.net.GunOptionSyncPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.GunOptionSyncPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.GunOptionSyncPayload::handle)
                .playToClient(io.github.soundgoodizerfan.feedback.net.AttunementSyncPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.AttunementSyncPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.AttunementSyncPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.net.EnchantBeginPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.EnchantBeginPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.EnchantBeginPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.net.EnchantFinishPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.EnchantFinishPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.EnchantFinishPayload::handle)
                .playToClient(io.github.soundgoodizerfan.feedback.net.ModularTypeSyncPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.ModularTypeSyncPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.ModularTypeSyncPayload::handle)
                .playToClient(io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.HudReadoutPayload::handle)
                .playToServer(ProgrammerCommitPayload.TYPE, ProgrammerCommitPayload.STREAM_CODEC,
                        ProgrammerCommitPayload::handle)
                .playToServer(ProgrammerLinkPayload.TYPE, ProgrammerLinkPayload.STREAM_CODEC,
                        ProgrammerLinkPayload::handle)
                .playToServer(ToolScrollPayload.TYPE, ToolScrollPayload.STREAM_CODEC,
                        ToolScrollPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.net.SwingStartPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.SwingStartPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.SwingStartPayload::handle)
                .playToClient(io.github.soundgoodizerfan.feedback.net.SwingPlayPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.SwingPlayPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.SwingPlayPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.net.MeleeSweepPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.MeleeSweepPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.MeleeSweepPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.net.OffhandStrikePayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.OffhandStrikePayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.OffhandStrikePayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.net.SpearChargePayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.SpearChargePayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.SpearChargePayload::handle)
                .playToClient(io.github.soundgoodizerfan.feedback.net.SpearStagePayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.SpearStagePayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.SpearStagePayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.net.FrameEditPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.FrameEditPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.FrameEditPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.net.CreativeSourcePayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.CreativeSourcePayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.CreativeSourcePayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.net.FrameMirrorPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.FrameMirrorPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.FrameMirrorPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.vehicle.engine.EngineStandPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.vehicle.engine.EngineStandPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.vehicle.engine.EngineStandPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.net.ModularBlueprintPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.ModularBlueprintPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.ModularBlueprintPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.net.PressPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.PressPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.PressPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.net.GunReloadPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.GunReloadPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.GunReloadPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.net.GunModePayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.GunModePayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.GunModePayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.net.GunUnloadPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.GunUnloadPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.GunUnloadPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.net.GunFirePayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.GunFirePayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.GunFirePayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.net.GunSwingPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.GunSwingPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.GunSwingPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.vehicle.seat.SeatKeyPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.vehicle.seat.SeatKeyPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.vehicle.seat.SeatKeyPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.net.ForgeBeginPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.ForgeBeginPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.ForgeBeginPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.net.ForgeFinishPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.ForgeFinishPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.ForgeFinishPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.net.SolderFinishPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.net.SolderFinishPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.net.SolderFinishPayload::handle)
                .playToClient(io.github.soundgoodizerfan.feedback.combat.feel.FeelPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.combat.feel.FeelPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.combat.feel.FeelPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.combat.parry.ParryPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.combat.parry.ParryPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.combat.parry.ParryPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.combat.roll.RollPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.combat.roll.RollPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.combat.roll.RollPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.combat.roll.WallKickPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.combat.roll.WallKickPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.combat.roll.WallKickPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.combat.kick.KickPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.combat.kick.KickPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.combat.kick.KickPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.combat.grab.GrabPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.combat.grab.GrabPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.combat.grab.GrabPayload::handle)
                .playToClient(io.github.soundgoodizerfan.feedback.combat.skill.SkillTreeSyncPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.combat.skill.SkillTreeSyncPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.combat.skill.SkillTreeSyncPayload::handle)
                .playToServer(io.github.soundgoodizerfan.feedback.combat.skill.SkillBuyPayload.TYPE,
                        io.github.soundgoodizerfan.feedback.combat.skill.SkillBuyPayload.STREAM_CODEC,
                        io.github.soundgoodizerfan.feedback.combat.skill.SkillBuyPayload::handle)
                .playToClient(ProgrammerLinkedPayload.TYPE, ProgrammerLinkedPayload.STREAM_CODEC,
                        ProgrammerLinkedPayload::handle);
    }

    /**
     * Hand the client the deformation, removal and thermal-process tables whenever the server's
     * copy could have changed.
     * <p>
     * The client needs it only to display requirements, which philosophy 8 gives away for free.
     * A null player means this was a {@code /reload} rather than a login, so everybody gets it.
     */
    private static void onDatapackSync(OnDatapackSyncEvent event) {
        send(event, new AmbientSyncPayload(AmbientTable.get().entries()));
        send(event, new MaterialSyncPayload(MaterialTable.get().materials()));
        send(event, new DeformationSyncPayload(DeformationTable.get().entries()));
        send(event, new RemovalSyncPayload(RemovalTable.get().entries()));
        send(event, new ThermalProcessSyncPayload(ThermalProcessTable.get().entries()));
        send(event, new io.github.soundgoodizerfan.feedback.net.GunOptionSyncPayload(
                io.github.soundgoodizerfan.feedback.weapon.GunOptions.get().options()));
        send(event, new io.github.soundgoodizerfan.feedback.net.AttunementSyncPayload(
                io.github.soundgoodizerfan.feedback.modular.Attunements.get().effects()));
        send(event, new io.github.soundgoodizerfan.feedback.net.ModularTypeSyncPayload(
                io.github.soundgoodizerfan.feedback.modular.ModularTypes.get().types()));
        send(event, new io.github.soundgoodizerfan.feedback.net.CastingSyncPayload(
                io.github.soundgoodizerfan.feedback.process.CastingTable.get().entries()));
        send(event, new io.github.soundgoodizerfan.feedback.net.QuenchSyncPayload(
                io.github.soundgoodizerfan.feedback.process.QuenchTable.get().entries()));
        send(event, new io.github.soundgoodizerfan.feedback.net.AlloySyncPayload(AlloyTable.get().entries()));
        send(event, new io.github.soundgoodizerfan.feedback.net.DipSyncPayload(DipTable.get().entries()));
        send(event, new io.github.soundgoodizerfan.feedback.net.PhaseChangeSyncPayload(
                io.github.soundgoodizerfan.feedback.process.PhaseChangeTable.get().entries()));
        send(event, new io.github.soundgoodizerfan.feedback.combat.skill.SkillTreeSyncPayload(
                io.github.soundgoodizerfan.feedback.combat.skill.SkillTrees.get().trees()));
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
