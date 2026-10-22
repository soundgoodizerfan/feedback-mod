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

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.cable.CableBlockEntity;
import io.github.soundgoodizerfan.feedback.modular.MaterialColors;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import io.github.soundgoodizerfan.feedback.registry.FormItems;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Every material in a form is one greyscale sprite per form ({@code textures/item/form/<form>.png})
 * tinted by {@link MaterialColors}: generated forms, storage blocks, and the hand-registered items a
 * material's {@code forms} names (copper plate, steel ingot). Vanilla's own items (iron ingot) keep
 * vanilla's sprite; their models are not ours to change.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FormTint {

    /** The forms that have a sprite under {@code textures/item/form/}. A new form type needs a file and an entry here. */
    private static final Set<String> SPRITE_FORMS = Set.of("ingot", "ground_ingot", "nugget", "bullet", "case",
            "plate", "grip", "rod", "turned_rod", "wire", "fine_wire", "foil", "scrap", "swarf", "drilled_plate");

    /** The metal of the machines drawn on the shared tank, block and coil-less sprites (their models tint layer 0). */
    private static final ResourceLocation MACHINE_METAL = Feedback.id("copper");

    private static List<Block> machineBlocks() {
        return List.of(FBlocks.LEYDEN_JAR.get(), FBlocks.CHARGER.get(), FBlocks.ELECTROSTATIC_GENERATOR.get(),
                FBlocks.CONDENSER.get(), FBlocks.PRESSURE_VESSEL.get(), FBlocks.VALVE.get(), FBlocks.STEAM_ENGINE.get(),
                FBlocks.GALVANIC_CELL.get(), FBlocks.BIMETALLIC_STRIP.get(), FBlocks.CLUTCH.get(),
                FBlocks.OVERSPEED_TRIP.get(), FVehicles.LIFT.get());
    }

    private FormTint() {
        // static only
    }

    static {
        MaterialColors.sampler = FormTint::sample;
    }

    @SubscribeEvent
    public static void registerReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) rm -> MaterialColors.clear());
    }

    /**
     * Average colour of an item's texture, by the usual naming: {@code textures/item/<path>.png},
     * else {@code textures/block/<path>.png}. Transparent pixels do not count. A mod whose models
     * name their sprites otherwise gets the kind's grey until its material file gives a colour.
     */
    private static OptionalInt sample(ResourceLocation item) {
        for (String dir : new String[] { "item", "block" }) {
            ResourceLocation tex = item.withPath("textures/" + dir + "/" + item.getPath() + ".png");
            var res = Minecraft.getInstance().getResourceManager().getResource(tex);
            if (res.isEmpty())
                continue;
            try (InputStream in = res.get().open(); NativeImage img = NativeImage.read(in)) {
                long r = 0, g = 0, b = 0, n = 0;
                for (int y = 0; y < img.getHeight(); y++)
                    for (int x = 0; x < img.getWidth(); x++) {
                        int px = img.getPixelRGBA(x, y); // ABGR
                        if ((px >>> 24) < 128)
                            continue;
                        r += px & 0xFF;
                        g += (px >> 8) & 0xFF;
                        b += (px >> 16) & 0xFF;
                        n++;
                    }
                if (n > 0)
                    return OptionalInt.of((int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n));
            } catch (Exception e) {
                // unreadable: try the next, then give up to the kind's grey
            }
        }
        return OptionalInt.empty();
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // Colours resolve per draw (cached in MaterialColors): textures are not readable yet.
        Map<Item, ResourceLocation> colors = new HashMap<>();
        for (FormItems.Generated g : FormItems.all())
            colors.put(g.item().get(), g.material());
        FormItems.materials().forEach((id, material) -> material.forms().forEach((form, item) -> {
            if (SPRITE_FORMS.contains(form) && item.getNamespace().equals(Feedback.MOD_ID))
                BuiltInRegistries.ITEM.getOptional(item)
                        .ifPresent(i -> colors.put(i, id));
        }));
        for (Block b : machineBlocks())
            colors.put(b.asItem(), MACHINE_METAL);
        colors.forEach((item, id) -> event.register((stack, layer) -> layer == 0 ? 0xFF000000 | MaterialColors.of(id) : 0xFFFFFFFF, item));
        // A cable's metal rides on the stack, so its tint cannot be fixed per item.
        event.register((stack, layer) -> {
            MadeOf made = stack.get(FDataComponents.MADE_OF.get());
            return layer == 0 && made != null ? 0xFF000000 | MaterialColors.of(made.material()) : 0xFFFFFFFF;
        }, FItems.CABLE.get());
        // Combat gear: the plate's metal, on the shared plate sprite (placeholder art).
        event.register((stack, layer) -> {
            var gear = stack.get(FDataComponents.GEAR.get());
            return layer == 0 && gear != null ? 0xFF000000 | MaterialColors.of(gear.shell()) : 0xFFFFFFFF;
        }, FItems.BRACER.get(), FItems.FEEDBACKER.get(), FItems.SASH.get(), FItems.BACK_PIECE.get());
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        for (FormItems.Generated g : FormItems.all())
            if (g.block() != null) {
                event.register((state, level, pos, tintIndex) -> 0xFF000000 | MaterialColors.of(g.material()),
                        (Block) g.block().get());
            }
        event.register((state, level, pos, tintIndex) -> 0xFF000000 | MaterialColors.of(MACHINE_METAL),
                machineBlocks().toArray(Block[]::new));
        event.register((state, level, pos, tintIndex) -> level != null && pos != null
                && level.getBlockEntity(pos) instanceof CableBlockEntity cable && cable.material() != null
                ? 0xFF000000 | MaterialColors.of(cable.material()) : 0xFFFFFFFF, FBlocks.CABLE.get());
    }
}
