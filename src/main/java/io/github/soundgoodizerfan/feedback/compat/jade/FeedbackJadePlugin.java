// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.machine.hammer.MechanicalHammerBlock;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

import net.minecraft.world.level.block.Block;

/**
 * Hooks the mod's readouts onto Jade's HUD.
 *
 * <h2>Jade, carefully</h2>
 * A HUD that printed {@code 14.3 RPM} on a bare shaft would hand over the instrument layer for
 * free and gut philosophy 8. One that says <em>turning slowly</em> until an instrument is installed
 * is not a compromise with that rule, it <em>is</em> that rule, on a HUD. See the individual
 * providers for which lines are free and why.
 * <p>
 * This class is loaded only by Jade's own plugin scan, so nothing here runs when Jade is absent.
 */
@WailaPlugin(Feedback.MOD_ID)
public class FeedbackJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(RotationServerData.INSTANCE, RotationNode.class);
        // Against BlockEntity rather than ThermalBody: a firebox is a heat source and not a body,
        // and the strip is neither, yet all three have something to say on the same card.
        registration.registerBlockDataProvider(ThermalServerData.INSTANCE,
                net.minecraft.world.level.block.entity.BlockEntity.class);
        registration.registerBlockDataProvider(ElectricServerData.INSTANCE, ElectricNode.class);
        registration.registerBlockDataProvider(ProbeServerData.INSTANCE, ElectricNode.class);
        registration.registerBlockDataProvider(FoodServerData.INSTANCE, net.minecraft.world.level.block.entity.BlockEntity.class);
        registration.registerBlockDataProvider(WearingServerData.INSTANCE, net.minecraft.world.level.block.entity.BlockEntity.class);
        registration.registerBlockDataProvider(XpServerData.INSTANCE, net.minecraft.world.level.block.entity.BlockEntity.class);
        registration.registerBlockDataProvider(ArcServerData.INSTANCE, io.github.soundgoodizerfan.feedback.machine.arc.ArcHeadBlockEntity.class);
        registration.registerBlockDataProvider(PreheatServerData.INSTANCE, io.github.soundgoodizerfan.feedback.machine.induction.PreheatCoilBlockEntity.class);
        registration.registerBlockDataProvider(ReactorServerData.INSTANCE, net.minecraft.world.level.block.entity.BlockEntity.class);
        registration.registerBlockDataProvider(BeltServerData.INSTANCE, io.github.soundgoodizerfan.feedback.logistics.belt.BeltBlockEntity.class);
        registration.registerBlockDataProvider(BeltServerData.INSTANCE, io.github.soundgoodizerfan.feedback.logistics.belt.BeltHeadBlockEntity.class);
        registration.registerBlockDataProvider(AirServerData.INSTANCE, io.github.soundgoodizerfan.feedback.vehicle.air.GasCellBlockEntity.class);
        registration.registerBlockDataProvider(AirServerData.INSTANCE, io.github.soundgoodizerfan.feedback.vehicle.air.BallastTankBlockEntity.class);
        registration.registerBlockDataProvider(AirServerData.INSTANCE, io.github.soundgoodizerfan.feedback.vehicle.air.PropBlockEntity.class);
        registration.registerEntityDataProvider(VillagerServerData.INSTANCE, net.minecraft.world.entity.npc.Villager.class);
        registration.registerBlockDataProvider(EnderPortServerData.INSTANCE, io.github.soundgoodizerfan.feedback.machine.ender.EnderPortBlockEntity.class);
        registration.registerBlockDataProvider(CounterServerData.INSTANCE, io.github.soundgoodizerfan.feedback.village.CounterBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // Registered against Block rather than each rotating block in turn: what makes a block
        // rotate is its block entity, not its class, and a list here would silently miss the next
        // machine somebody adds. The provider checks the block entity itself.
        registration.registerBlockComponent(RotationComponent.INSTANCE, Block.class);
        registration.registerBlockComponent(HammerComponent.INSTANCE, MechanicalHammerBlock.class);
        registration.registerBlockComponent(WorkpieceComponent.INSTANCE, Block.class);
        registration.registerBlockComponent(ThermalComponent.INSTANCE, Block.class);
        registration.registerBlockComponent(ElectricComponent.INSTANCE, Block.class);
        registration.registerBlockComponent(FoodComponent.INSTANCE, Block.class);
        registration.registerBlockComponent(WearingComponent.INSTANCE, Block.class);
        registration.registerEntityComponent(VillagerComponent.INSTANCE, net.minecraft.world.entity.npc.Villager.class);
        registration.registerBlockComponent(CounterComponent.INSTANCE, io.github.soundgoodizerfan.feedback.village.CounterBlock.class);
        registration.registerBlockComponent(ProbeComponent.INSTANCE, Block.class);
        registration.registerBlockComponent(XpComponent.INSTANCE, Block.class);
        registration.registerBlockComponent(AirComponent.INSTANCE, Block.class);
        registration.registerBlockComponent(ArcComponent.INSTANCE, Block.class);
        registration.registerBlockComponent(RadiationComponent.INSTANCE, Block.class);
        registration.registerBlockComponent(ReactorComponent.INSTANCE, Block.class);
        registration.registerBlockComponent(PreheatComponent.INSTANCE, Block.class);
        registration.registerBlockComponent(BeltComponent.INSTANCE, Block.class);
        registration.registerBlockComponent(EnderPortComponent.INSTANCE, Block.class);
    }
}
