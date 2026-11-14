// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.fitting;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

/**
 * The face slots and upgrade list of a holder that keeps nothing else about its fittings: one sided slot per face, an upgrade list, and
 * the persistence of the one stateless sided fitting that lives here ({@link RedstoneAnalogFitting}, a flag per face). Used through
 * {@link StoredFittable} so a holder implements {@link Fittable} in one line.
 */
public final class FittingStore {

    private final SidedFitting[] sided = new SidedFitting[6];
    private final List<UpgradeFitting> upgrades = new ArrayList<>();

    @Nullable
    public SidedFitting get(Direction side) {
        return sided[side.get3DDataValue()];
    }

    public void set(Direction side, @Nullable SidedFitting fitting) {
        sided[side.get3DDataValue()] = fitting;
    }

    public List<UpgradeFitting> upgrades() {
        return upgrades;
    }

    public void save(CompoundTag tag) {
        for (UpgradeFitting u : upgrades)
            if (u instanceof io.github.soundgoodizerfan.feedback.fitting.upgrade.LevitationSleeveFitting sleeve)
                tag.putString("Sleeve", sleeve.material().toString());
        for (Direction d : Direction.values())
            if (sided[d.get3DDataValue()] instanceof RedstoneAnalogFitting)
                tag.putBoolean("RedstoneAnalog" + d.get3DDataValue(), true);
    }

    public void load(CompoundTag tag, BlockEntity holder) {
        upgrades.removeIf(u -> u instanceof io.github.soundgoodizerfan.feedback.fitting.upgrade.LevitationSleeveFitting);
        if (tag.contains("Sleeve"))
            upgrades.add(new io.github.soundgoodizerfan.feedback.fitting.upgrade.LevitationSleeveFitting(
                    net.minecraft.resources.ResourceLocation.parse(tag.getString("Sleeve"))));
        for (Direction d : Direction.values()) {
            int i = d.get3DDataValue();
            boolean has = tag.getBoolean("RedstoneAnalog" + i);
            if (has && !(sided[i] instanceof RedstoneAnalogFitting))
                sided[i] = new RedstoneAnalogFitting(holder, d);
            else if (!has && sided[i] instanceof RedstoneAnalogFitting)
                sided[i] = null;
        }
    }
}
