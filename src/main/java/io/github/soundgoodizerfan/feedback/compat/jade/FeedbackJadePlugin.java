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
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.control.debug.DebugControllerBlock;
import io.github.soundgoodizerfan.feedback.control.debug.DebugControllerBlockEntity;
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
        registration.registerBlockDataProvider(DebugControllerServerData.INSTANCE, DebugControllerBlockEntity.class);
        registration.registerBlockDataProvider(ElectricServerData.INSTANCE, ElectricNode.class);
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
        registration.registerBlockComponent(DebugControllerComponent.INSTANCE, DebugControllerBlock.class);
        registration.registerBlockComponent(ElectricComponent.INSTANCE, Block.class);
    }
}
