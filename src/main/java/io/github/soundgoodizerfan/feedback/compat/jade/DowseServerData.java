// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.attunement.DowseKinds;
import io.github.soundgoodizerfan.feedback.modular.attunement.MachineAura;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * The needles of a machine's dowse parts (attunement kinds {@code point_*} and {@code away_*}), read at the machine for the one block looked at:
 * per needle the property, whether it points away, and the compass direction of the nearest such block as an eighth (0 north, clockwise), -1
 * when none is near and -2 when it is right over it. Words only: no distance.
 */
public class DowseServerData implements IServerDataProvider<BlockAccessor> {

    public static final DowseServerData INSTANCE = new DowseServerData();

    static final String NEEDLES = "DowseNeedles";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        var be = accessor.getBlockEntity();
        if (be == null || be.getLevel() == null)
            return;
        ListTag list = new ListTag();
        MachineAura.levelsOf(be).forEach((kind, level) -> {
            String property = DowseKinds.property(kind);
            if (property == null || kind.startsWith("worn_"))
                return;
            BlockPos at = be.getBlockPos(), near = DowseKinds.nearest(be.getLevel(), at, property);
            CompoundTag entry = new CompoundTag();
            entry.putString("Property", property);
            boolean away = kind.startsWith("away_");
            entry.putBoolean("Away", away);
            int dir = -1;
            if (near != null) {
                double dx = near.getX() - at.getX(), dz = near.getZ() - at.getZ();
                if (dx * dx + dz * dz < 1.5)
                    dir = -2;
                else {
                    double angle = Math.toDegrees(Math.atan2(dx, -dz)) + (away ? 180 : 0);
                    dir = Math.floorMod((int) Math.round(angle / 45.0), 8);
                }
            }
            entry.putInt("Dir", dir);
            list.add(entry);
        });
        if (!list.isEmpty())
            data.put(NEEDLES, list);
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("dowse");
    }
}
