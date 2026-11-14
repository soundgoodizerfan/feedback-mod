// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.item;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.cable.CableBlockEntity;
import io.github.soundgoodizerfan.feedback.modular.type.MaterialColors;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.registry.FormItems;
import io.github.soundgoodizerfan.feedback.client.render.GlowColours;

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
    private static final Set<String> SPRITE_FORMS = Set.of("ingot", "ground_ingot", "nugget", "bullet", "round_ball", "flat_nose_bullet", "pointed_bullet", "hollow_point_bullet", "birdshot", "large_shot", "flechette", "case",
            "plate", "crystal_plate", "grip", "rod", "turned_rod", "wire", "fine_wire", "foil", "coin", "scrap", "swarf", "drilled_plate", "raw", "lump", "string", "dust");

    /** The metal of the machines drawn on the shared tank, block and coil-less sprites (their models tint layer 0). */
    private static final ResourceLocation MACHINE_METAL = Feedback.id("copper");

    private static List<Block> machineBlocks() {
        return List.of(FBlocks.LEYDEN_JAR.get(), FBlocks.CHARGER.get(), FBlocks.ELECTROSTATIC_GENERATOR.get(),
                FBlocks.CONDENSER.get(), FBlocks.PRESSURE_VESSEL.get(), FBlocks.VALVE.get(), FBlocks.STEAM_ENGINE.get(), FBlocks.STEAM_TURBINE.get(),
                FBlocks.GALVANIC_CELL.get(), FBlocks.GALVANIC_CELL_PACK.get(), FBlocks.BIMETALLIC_STRIP.get(), FBlocks.CLUTCH.get(),
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

    /**
     * Ore blocks and the material each is an ore of, from the material files' {@code ore} forms.
     * The ore model draws a grey overlay (tint index 0) on the host stone; the material's colour
     * is the overlay's tint. Layering idea from GregTech CEu Modern (see THIRD-PARTY-LICENSES.md).
     */
    private static Map<Block, ResourceLocation> oreBlocks() {
        Map<Block, ResourceLocation> ores = new HashMap<>();
        FormItems.materials().forEach((id, material) -> material.forms().forEach((form, item) -> {
            if (form.endsWith("ore") && item.getNamespace().equals(Feedback.MOD_ID))
                BuiltInRegistries.BLOCK.getOptional(item).ifPresent(b -> ores.put(b, id));
        }));
        for (FormItems.Generated g : FormItems.all())
            if (g.block() != null && io.github.soundgoodizerfan.feedback.process.FormType.overlaid(g.form()))
                ores.put(g.block().get(), g.material());
        return ores;
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
        oreBlocks().forEach((b, id) -> colors.put(b.asItem(), id));
        colors.forEach((item, id) -> {
            var material = FormItems.materials().get(id);
            boolean glows = material != null && io.github.soundgoodizerfan.feedback.core.thermal.Incandescence.glows(material);
            event.register((stack, layer) -> layer != 0 ? 0xFFFFFFFF
                    : 0xFF000000 | (glows ? GlowColours.heated(MaterialColors.of(id), stack) : MaterialColors.of(id)), item);
        });
        // A cable's metal rides on the stack, so its tint cannot be fixed per item.
        event.register((stack, layer) -> {
            MadeOf made = stack.get(FDataComponents.MADE_OF.get());
            return layer == 0 && made != null ? 0xFF000000 | MaterialColors.of(made.material()) : 0xFFFFFFFF;
        }, FItems.CABLE.get(), FItems.FENCE.get(), FItems.XP_WIRE.get(), FItems.LEVITATION_PLATE.get(), FItems.LEVITATION_SLEEVE.get(), FItems.PYRO_PLATE.get());
        // Combat gear: the plate's metal, on the shared plate sprite (placeholder art).
        event.register((stack, layer) -> {
            var gear = stack.get(FDataComponents.GEAR.get());
            return layer == 0 && gear != null ? 0xFF000000 | MaterialColors.of(gear.shell()) : 0xFFFFFFFF;
        }, FItems.BRACER.get(), FItems.FEEDBACKER.get(), FItems.SASH.get(), FItems.BACK_PIECE.get());
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        for (FormItems.Generated g : FormItems.all())
            if (g.block() != null && !io.github.soundgoodizerfan.feedback.process.FormType.overlaid(g.form())) {
                event.register((state, level, pos, tintIndex) -> 0xFF000000 | MaterialColors.of(g.material()),
                        (Block) g.block().get());
            }
        event.register((state, level, pos, tintIndex) -> 0xFF000000 | MaterialColors.of(MACHINE_METAL),
                machineBlocks().toArray(Block[]::new));
        oreBlocks().forEach((b, id) -> event.register(
                (state, level, pos, tintIndex) -> tintIndex == 0 ? 0xFF000000 | MaterialColors.of(id) : 0xFFFFFFFF, b));
        event.register((state, level, pos, tintIndex) -> level != null && pos != null
                && level.getBlockEntity(pos) instanceof CableBlockEntity cable && cable.material() != null
                ? 0xFF000000 | MaterialColors.of(cable.material()) : 0xFFFFFFFF, FBlocks.CABLE.get(), FBlocks.FENCE.get());
        // A placed bed: cover (tint 0) and frame (tint 1) take their materials' colours from the design it was built as.
        event.register((state, level, pos, tintIndex) -> {
            if (level == null || pos == null || !(level.getBlockEntity(pos) instanceof io.github.soundgoodizerfan.feedback.modular.part.ModularBedBlockEntity bed))
                return 0xFFFFFFFF;
            var design = io.github.soundgoodizerfan.feedback.modular.item.ModularStacks.design(bed.bed());
            var part = design == null ? null : design.part(tintIndex == 0
                    ? io.github.soundgoodizerfan.feedback.modular.part.ModularBed.MATTRESS : io.github.soundgoodizerfan.feedback.modular.part.ModularBed.FRAME);
            return part == null ? 0xFFFFFFFF : 0xFF000000 | MaterialColors.of(part.material());
        }, FModular.PLACED_BED.get());
        event.register((state, level, pos, tintIndex) -> level != null && pos != null
                && level.getBlockEntity(pos) instanceof io.github.soundgoodizerfan.feedback.machine.xp.XpWireBlockEntity wire
                && wire.material() != null ? 0xFF000000 | MaterialColors.of(wire.material()) : 0xFFFFFFFF, FBlocks.XP_WIRE.get());
        event.register((state, level, pos, tintIndex) -> level != null && pos != null
                && level.getBlockEntity(pos) instanceof io.github.soundgoodizerfan.feedback.machine.levitation.LevitationPlateBlockEntity plate
                && plate.material() != null ? 0xFF000000 | MaterialColors.of(plate.material()) : 0xFFFFFFFF, FBlocks.LEVITATION_PLATE.get());
        event.register((state, level, pos, tintIndex) -> level != null && pos != null
                && level.getBlockEntity(pos) instanceof io.github.soundgoodizerfan.feedback.machine.pyro.PyroPlateBlockEntity plate
                && plate.material() != null ? 0xFF000000 | MaterialColors.of(plate.material()) : 0xFFFFFFFF, FBlocks.PYRO_PLATE.get());
    }
}
