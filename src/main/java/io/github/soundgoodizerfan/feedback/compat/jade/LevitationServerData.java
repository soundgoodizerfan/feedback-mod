// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.upgrade.LevitationSleeves;
import io.github.soundgoodizerfan.feedback.machine.electromagnet.MagnetField;
import io.github.soundgoodizerfan.feedback.machine.levitation.LevitationPlateBlockEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/** Whether a levitation plate is held off by a field below it, and whether a sleeved shaft or bearing is cushioned by a magnet beside it. Words, free to read. */
public class LevitationServerData implements IServerDataProvider<BlockAccessor> {

    public static final LevitationServerData INSTANCE = new LevitationServerData();

    static final String PLATE = "LevitationPlate", SLEEVE = "LevitationSleeve", PYRO = "PyroRate";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        var level = accessor.getLevel();
        var pos = accessor.getPosition();
        if (accessor.getBlockEntity() instanceof LevitationPlateBlockEntity plate)
            data.putFloat(PLATE, plate.diamagnetism() * MagnetField.below(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        else if (accessor.getBlockEntity() instanceof io.github.soundgoodizerfan.feedback.machine.pyro.PyroPlateBlockEntity pyro)
            data.putFloat(PYRO, pyro.coupling() <= 0f || Math.abs(pyro.rate()) < pyro.resolution(io.github.soundgoodizerfan.feedback.instrument.Quantity.HEATING_RATE) / 2f ? 0f : pyro.rate());
        else if (accessor.getBlockEntity() instanceof Fittable holder && LevitationSleeves.of(holder) != null)
            data.putFloat(SLEEVE, LevitationSleeves.cut(level, pos, holder));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("levitation");
    }
}
