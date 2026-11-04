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
