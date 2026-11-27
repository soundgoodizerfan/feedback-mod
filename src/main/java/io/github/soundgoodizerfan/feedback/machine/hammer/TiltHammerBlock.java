// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.hammer;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.blueprint.ToolDesigns;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;
import io.github.soundgoodizerfan.feedback.registry.FormItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A wooden helve rocking on posts over a cobblestone anvil, with a cast iron head fixed to its end. The same machine as the
 * Mechanical Hammer (the same block entity, linkage and cam), with one difference: the head is cast into the beam, so it is
 * seated when the block is placed, no head can be put in, and the head cannot be taken out. It wears and is welded like any head.
 */
public class TiltHammerBlock extends MechanicalHammerBlock {

    /** The metal the head is cast in: a bar of it, cast in the head mould, needs no forging. */
    private static final ResourceLocation HEAD_METAL = Feedback.id("cast_iron");

    public TiltHammerBlock(Properties properties) {
        super(properties);
    }

    @Override
    boolean fixedHead() {
        return true;
    }

    /** The head this hammer comes with: a hand hammer of {@link #HEAD_METAL}, or empty while the tables are not loaded. */
    static ItemStack castHead() {
        ResourceLocation type = Feedback.id("hand_hammer");
        var materials = FormItems.materials();
        var types = ModularTypes.known();
        var starter = ToolDesigns.starter(type, types, materials);
        if (starter.isEmpty() || !materials.containsKey(HEAD_METAL))
            return ItemStack.EMPTY;
        List<ModularDesign.Part> parts = new ArrayList<>();
        for (ModularDesign.Part p : starter.get().parts())
            parts.add(p.role().equals("H") ? new ModularDesign.Part(p.role(), HEAD_METAL, p.form(), p.count(), p.kind(), p.variant(), p.flaw(), p.measures()) : p);
        ItemStack head = new ItemStack(BuiltInRegistries.ITEM.get(type));
        return ModularStacks.assemble(head, new ModularDesign(type, parts, starter.get().edge()), materials, types) ? head : ItemStack.EMPTY;
    }
}
