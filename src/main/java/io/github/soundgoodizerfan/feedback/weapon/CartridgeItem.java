// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A loaded round: the record on it is the whole of what it is (spec 3.1). Shows its own spec sheet. */
public class CartridgeItem extends Item {

    public CartridgeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Cartridge round = stack.get(FWeapons.CARTRIDGE.get());
        if (round == null)
            return;
        tooltip.add(Component.translatable("cartridge.feedback.settings", round.boreMm(),
                name(round.caseMaterial()), name(round.projectile())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("gui.feedback.reloading_press.shape." + round.shape().getSerializedName())
                .withStyle(ChatFormatting.GRAY));
        round.core().ifPresent(c -> tooltip.add(Component.translatable("cartridge.feedback.core", name(c)).withStyle(ChatFormatting.GRAY)));
        if (round.shot())
            tooltip.add(Component.translatable("cartridge.feedback.shot", round.pellets()).withStyle(ChatFormatting.GRAY));
        var materials = context.level() == null ? java.util.Map.<net.minecraft.resources.ResourceLocation, io.github.soundgoodizerfan.feedback.process.Material>of()
                : ClientMaterials.on(context.level());
        // A shell has no powder: a launcher's seat gives the push.
        if (round.rocket())
            tooltip.add(Component.translatable("cartridge.feedback.rocket", round.charge()).withStyle(ChatFormatting.GRAY));
        else if (round.charges().isEmpty())
            tooltip.add(Component.translatable("cartridge.feedback.shell").withStyle(ChatFormatting.GRAY));
        else {
            var load = round.load(materials);
            var rating = round.caseRating(materials);
            if (load.isEmpty() || rating.isEmpty()) {
                tooltip.add(Component.translatable("gun.feedback.unknown_material").withStyle(ChatFormatting.RED));
                return;
            }
            // The round's spec against its own case: free and exact (philosophy 10), red when it asks too much.
            var charge = Component.empty();
            for (int k = 0; k < round.charges().size(); k++) {
                var c = round.charges().get(k);
                if (k > 0)
                    charge.append(" + ");
                charge.append(Component.translatable("cartridge.feedback.part", c.count(), name(c.material())));
            }
            tooltip.add(Component.translatable("cartridge.feedback.charge", charge,
                    String.format("%.1f", load.get().peakPressure()), String.format("%.1f", rating.get().value()))
                    .withStyle(load.get().peakPressure() > rating.get().value() ? ChatFormatting.RED : ChatFormatting.GRAY));
            // The load is named by how much of its case it asks: light, standard, heavy (+P), and over.
            float share = load.get().peakPressure() / Math.max(0.001f, rating.get().value());
            String band = share > 1f ? "over" : share >= GunTuning.LOAD_HEAVY_SHARE ? "heavy" : share >= GunTuning.LOAD_LIGHT_SHARE ? "standard" : "light";
            tooltip.add(Component.translatable("cartridge.feedback.band." + band).withStyle(share > 1f ? ChatFormatting.RED : ChatFormatting.GRAY));
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown())
                tooltip.add(Component.translatable("cartridge.feedback.band.tip", Math.round(GunTuning.LOAD_LIGHT_SHARE * 100), Math.round(GunTuning.LOAD_HEAVY_SHARE * 100)).withStyle(ChatFormatting.DARK_GRAY));
        }
        round.seed().ifPresent(sd -> tooltip.add(Component.translatable("cartridge.feedback.seed",
                net.minecraft.core.registries.BuiltInRegistries.ITEM.get(sd).getDescription()).withStyle(ChatFormatting.GRAY)));
        round.vial().ifPresent(v -> v.effect().ifPresent(e -> tooltip.add(Component.translatable("cartridge.feedback.vial",
                Component.translatable("effect." + e.getNamespace() + "." + e.getPath())).withStyle(ChatFormatting.GRAY))));
        round.payload().ifPresent(p -> {
            boolean tracer = round.tracer(materials), fire = round.incendiary(materials);
            float blast = round.blast(materials);
            tooltip.add(Component.translatable("cartridge.feedback.payload", name(p),
                    blast > 0 ? Component.translatable("cartridge.feedback.explosive", String.format("%.1f", blast))
                            : Component.translatable(tracer && fire ? "cartridge.feedback.tracer_incendiary"
                            : tracer ? "cartridge.feedback.tracer" : fire ? "cartridge.feedback.incendiary"
                            : round.payloadTraits(materials).contains(io.github.soundgoodizerfan.feedback.process.Material.Trait.STICKY) ? "cartridge.feedback.anchor"
                            : round.payloadTraits(materials).contains(io.github.soundgoodizerfan.feedback.process.Material.Trait.FIBROUS) ? "cartridge.feedback.tether"
                            : round.payloadPerks(materials).isEmpty() ? "cartridge.feedback.inert" : "cartridge.feedback.perks",
                            round.payloadPerks(materials).keySet().stream().map(k -> Component.translatable("feedback.jei.perk." + k.getSerializedName()).getString())
                                    .collect(java.util.stream.Collectors.joining(", ")))).withStyle(ChatFormatting.GRAY));
        });
    }

    public static Component name(net.minecraft.resources.ResourceLocation material) {
        return Component.translatable("material." + material.getNamespace() + "." + material.getPath());
    }
}
