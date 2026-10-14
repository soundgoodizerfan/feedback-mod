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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
        if (round.shot())
            tooltip.add(Component.translatable("cartridge.feedback.shot", round.pellets()).withStyle(ChatFormatting.GRAY));
        var materials = context.level() == null ? java.util.Map.<net.minecraft.resources.ResourceLocation, io.github.soundgoodizerfan.feedback.process.Material>of()
                : ClientMaterials.on(context.level());
        var load = round.load(materials);
        var rating = round.caseRating(materials);
        if (load.isEmpty() || rating.isEmpty()) {
            tooltip.add(Component.translatable("gun.feedback.unknown_material").withStyle(ChatFormatting.RED));
            return;
        }
        // The round's spec against its own case: free and exact (philosophy 8), red when it asks too much.
        var charge = Component.empty();
        for (int k = 0; k < round.charges().size(); k++) {
            var c = round.charges().get(k);
            if (k > 0)
                charge.append(" + ");
            charge.append(Component.translatable("cartridge.feedback.part", c.count(), name(c.material())));
        }
        boolean coil = load.get().coil();
        tooltip.add(Component.translatable(coil ? "cartridge.feedback.drive" : "cartridge.feedback.charge", charge,
                String.format("%.1f", load.get().peakPressure()), String.format("%.1f", rating.get().value()))
                .withStyle(!coil && load.get().peakPressure() > rating.get().value() ? ChatFormatting.RED : ChatFormatting.GRAY));
        round.payload().ifPresent(p -> {
            boolean tracer = round.tracer(materials), fire = round.incendiary(materials);
            tooltip.add(Component.translatable("cartridge.feedback.payload", name(p),
                    Component.translatable(tracer && fire ? "cartridge.feedback.tracer_incendiary"
                            : tracer ? "cartridge.feedback.tracer" : fire ? "cartridge.feedback.incendiary"
                            : "cartridge.feedback.inert")).withStyle(ChatFormatting.GRAY));
        });
    }

    static Component name(net.minecraft.resources.ResourceLocation material) {
        return Component.translatable("material." + material.getNamespace() + "." + material.getPath());
    }
}
