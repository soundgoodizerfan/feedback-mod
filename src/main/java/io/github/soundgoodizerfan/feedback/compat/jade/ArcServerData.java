// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.arc.ArcHeadBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.arc.ElectrodeItem;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/** What an arc head holds in each electrode slot, fetched for the one block looked at: slot, material and condition (an empty slot sends nothing but its number). */
public class ArcServerData implements IServerDataProvider<BlockAccessor> {

    public static final ArcServerData INSTANCE = new ArcServerData();

    static final String ELECTRODES = "ArcElectrodes";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof ArcHeadBlockEntity head))
            return;
        ListTag list = new ListTag();
        for (ItemStack held : head.electrodes()) {
            CompoundTag entry = new CompoundTag();
            MadeOf made = held.get(FDataComponents.MADE_OF.get());
            if (!held.isEmpty() && made != null) {
                entry.putString("Material", made.material().toString());
                entry.putFloat("Condition", ElectrodeItem.condition(held));
            }
            list.add(entry);
        }
        data.put(ELECTRODES, list);
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("arc");
    }
}
