// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.item;

import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.bake.FigureHolder;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem;
import io.github.soundgoodizerfan.feedback.modular.type.MaterialColors;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Tints a modular block by the materials it was built from. A block model marks the faces of one part with a tint index;
 * {@link #ROLES} says which part role that index is for that block, and the colour is that part's material colour
 * ({@link MaterialColors}), read from the placed block entity's design ({@link FigureHolder}) or from the item's design component.
 * A block with no such part (a creative copy, an addon not fitted) takes {@link #FALLBACK}. To tint a block's model: give its
 * faces a {@code tintindex}, add a row here, and have its block entity implement {@link FigureHolder} and send its update tag.
 * Models draw tinted faces in grey so the colour multiplies cleanly.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FigureTint {

    /** The colour of a part the design lacks: the machines' old fixed metal. */
    private static final ResourceLocation FALLBACK = Feedback.id("copper");

    /** Tint index to the part role it takes its material from, per block. */
    private static Map<Block, String[]> roles() {
        return Map.ofEntries(
                Map.entry(io.github.soundgoodizerfan.feedback.registry.FStore.RACK.get(), new String[]{"S", "D"}), // 0 the frame, 1 the shelves
                Map.entry(FBlocks.LEYDEN_JAR.get(), new String[]{"S", "F"}),   // 0 the glass, 1 the foil
                Map.entry(FBlocks.ELECTROMAGNET.get(), new String[]{"S"}),     // 0 the core and winding metal
                Map.entry(FBlocks.LAMP.get(), new String[]{"S"}),              // 0 the whole panel: the filament's colour
                Map.entry(FBlocks.MODULAR_PISTON.get(), PISTON),               // 0 the cylinder, 1 the rod
                Map.entry(FBlocks.GAS_TANK.get(), new String[]{"S"}),          // 0 the tank shell
                Map.entry(FBlocks.BOILER.get(), new String[]{"S"}),            // 0 the boiler shell
                Map.entry(FBlocks.COMPRESSOR.get(), new String[]{"S"}),        // 0 the compressor casing, the rotor's metal
                Map.entry(FBlocks.BURNER.get(), new String[]{"S"}),            // 0 the burner casing, the nozzle's metal
                Map.entry(FBlocks.GAS_CENTRIFUGE.get(), new String[]{"S"}),    // 0 the centrifuge casing, the rotor's metal
                Map.entry(FBlocks.HEAT_EXCHANGER.get(), new String[]{"S"}),    // 0 the exchanger core
                Map.entry(FBlocks.PUMP.get(), new String[]{"S"}),              // 0 the impeller (the intake and outlet faces)
                Map.entry(FBlocks.HEATING_COIL.get(), new String[]{"S"}),      // 0 the coil element's metal
                Map.entry(FBlocks.TRAY.get(), new String[]{"S"}),              // 0 the tray deck
                Map.entry(FBlocks.LINEAR_MOTOR.get(), new String[]{"S"}));     // 0 the motor winding's metal
    }

    /** The piston's tint roles: index 0 the cylinder's metal, index 1 the rod's (also the head plate and the rod blocks). */
    private static final String[] PISTON = {"S", "R"};

    private FigureTint() {
    }

    private static int colour(ModularDesign design, String[] roles, int tintIndex, boolean glows) {
        ModularDesign.Part part = design == null || tintIndex < 0 || tintIndex >= roles.length ? null : design.part(roles[tintIndex]);
        int rgb = MaterialColors.of(part == null ? FALLBACK : part.material());
        if (glows) {
            // A light source is never dim: keep the hue, scale until the strongest channel is full (a tint only multiplies).
            int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF, max = Math.max(1, Math.max(r, Math.max(g, b)));
            rgb = (r * 255 / max) << 16 | (g * 255 / max) << 8 | b * 255 / max;
        }
        return 0xFF000000 | rgb;
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        roles().forEach((block, roles) -> event.register((state, level, pos, tintIndex) -> {
            ModularDesign design = level != null && pos != null && level.getBlockEntity(pos) instanceof FigureHolder holder
                    ? holder.figures().design() : null;
            return colour(design, roles, tintIndex, block == FBlocks.LAMP.get());
        }, block));
        // The head and the rod blocks hold no design: they take the rod metal of the cylinder at the end of the line behind them.
        event.register((state, level, pos, tintIndex) -> {
            ModularDesign design = null;
            if (level != null && pos != null) {
                var back = state.getValue(io.github.soundgoodizerfan.feedback.machine.piston.ModularPistonHeadBlock.FACING).getOpposite();
                var at = pos.relative(back);
                for (int i = 0; i < 64 && level.getBlockState(at).is(FBlocks.MODULAR_PISTON_HEAD.get()); i++)
                    at = at.relative(back);
                if (level.getBlockEntity(at) instanceof FigureHolder holder)
                    design = holder.figures().design();
            }
            return colour(design, PISTON, tintIndex, false);
        }, FBlocks.MODULAR_PISTON_HEAD.get());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        Map<Block, String[]> roles = roles();
        for (Item item : BuiltInRegistries.ITEM)
            if (item instanceof ModularBlockItem block && roles.containsKey(block.getBlock())) {
                String[] r = roles.get(block.getBlock());
                event.register((stack, layer) -> colour(ModularStacks.design(stack), r, layer, block.getBlock() == FBlocks.LAMP.get()), item);
            }
    }
}
