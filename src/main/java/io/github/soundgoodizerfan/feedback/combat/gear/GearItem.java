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
package io.github.soundgoodizerfan.feedback.combat.gear;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;

import io.github.soundgoodizerfan.feedback.client.Readout;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Grip;
import io.github.soundgoodizerfan.feedback.core.hazard.Insulator;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A bracer, sash or back piece, worn in the Curios slot of its {@link GearKind}. The item holds no
 * figure: a blank one does nothing, and a crafted one carries {@link GearStats} baked from its
 * materials by {@link GearRecipe}. {@link CombatGear} is what reads them.
 */
public class GearItem extends Item implements Insulator {

    private final GearKind kind;

    public GearItem(Properties properties, GearKind kind) {
        super(properties.stacksTo(1));
        this.kind = kind;
    }

    public GearKind kind() {
        return kind;
    }

    /** A gauntlet is rated by its plate's own file, as the old glove was by its wool: cloth holds warm work, metal holds nothing back. */
    @Override
    public float handlesUpToTu(ItemStack stack, Level level) {
        GearStats g = stack.get(FDataComponents.GEAR.get());
        return kind != GearKind.GAUNTLET || g == null ? 0f : Grip.safeUpTo(level, g.shell(), FTuning.GLOVE_EXPOSURE);
    }

    @Override
    public float shockProofVolts(ItemStack stack, Level level) {
        GearStats g = stack.get(FDataComponents.GEAR.get());
        return kind != GearKind.GAUNTLET || g == null ? 0f
                : Grip.material(level, g.shell()).filter(Grip::insulates).isPresent() ? FTuning.GLOVE_SHOCK_PROOF_VOLTS : 0f;
    }

    /** "Steel Gauntlet", "Wool Glove": a gauntlet is a glove when its plate is fibrous cloth. Other pieces keep their name. */
    @Override
    public Component getName(ItemStack stack) {
        GearStats g = stack.get(FDataComponents.GEAR.get());
        if (kind != GearKind.GAUNTLET || g == null)
            return super.getName(stack);
        var materials = ClientMaterials.get().isEmpty() ? MaterialTable.get().materials() : ClientMaterials.get();
        Material plate = materials.get(g.shell());
        boolean glove = plate != null && plate.traits().contains(Material.Trait.FIBROUS);
        return Component.translatable("item.feedback.modular_name", name(g.shell()),
                Component.translatable(glove ? "item.feedback.glove" : "item.feedback.gauntlet"));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GearStats g = stack.get(FDataComponents.GEAR.get());
        if (g != null)
            stats(stack, tooltip, g);
        flavor(stack, tooltip);
    }

    /** Material lines grey, figures aqua, granted techniques gold: what it is made of, what that did, what it lets you do. */
    private static void stats(ItemStack stack, List<Component> tooltip, GearStats g) {
        tooltip.add(Component.translatable("item.feedback.gear.shell", name(g.shell())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.feedback.gear.strap", name(g.strap())).withStyle(ChatFormatting.GRAY));
        line(tooltip, "window", g.window());
        line(tooltip, "joint", g.joint());
        line(tooltip, "roll_distance", g.rollDistance());
        line(tooltip, "recovery_shave", g.recoveryShave());
        line(tooltip, "i_frames", g.iFrames());
        line(tooltip, "mass", g.mass());
        line(tooltip, "fist", g.fist());
        List<ResourceLocation> grants = stack.get(FDataComponents.GRANTS.get());
        if (grants != null)
            for (ResourceLocation id : grants)
                tooltip.add(Component.translatable("item.feedback.gear.grants", Component.translatable("technique." + id.getNamespace() + "." + id.getPath())).withStyle(ChatFormatting.GOLD));
    }

    /**
     * Flavour text, any piece: lang keys {@code <item descriptionId>.flavor.1}, {@code .2}, ... one line each, read
     * until a number is missing. Pure presentation, so a pack can add lore to any gear by lang alone.
     */
    private static void flavor(ItemStack stack, List<Component> tooltip) {
        String base = stack.getItem().getDescriptionId() + ".flavor.";
        if (!Language.getInstance().has(base + 1))
            return;
        tooltip.add(Component.empty());
        for (int i = 1; Language.getInstance().has(base + i); i++)
            tooltip.add(Component.translatable(base + i).withStyle(ChatFormatting.RED));
    }

    private static void line(List<Component> tooltip, String key, float value) {
        if (value > 0)
            tooltip.add(Component.translatable("item.feedback.gear." + key, Readout.figure(Quantity.FIGURE, value)).withStyle(ChatFormatting.AQUA));
    }

    private static Component name(ResourceLocation material) {
        return Component.translatable("material." + material.getNamespace() + "." + material.getPath());
    }
}
