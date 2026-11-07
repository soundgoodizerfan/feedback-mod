// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Sculk jacket: a barrel jacket that stores the barrel's vibration. Cold, it quiets the whole barrel and the group
 * tightens to {@code cold_spread}; as the barrel heats past {@code spoil_at} (a share of the way from the room to
 * the round's ignition) the stored vibration goes bad and the group opens to {@code spoiled_spread}, worse than bare.
 * The reverse of the thermal sleeve, which wants a warm barrel. Read live off the stack, so the client predicts the shot.
 */
public class GunSculkJacket implements ModularTrait {

    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        float ambient = AmbientTable.at(shooter.level()).value();
        float heat = Math.max(0f, (GunCondition.temperature(gun, shooter.level()).value() - ambient)
                / (FTuning.GUN_PROPELLANT_IGNITION_TU - ambient));
        float spoilAt = p.get("spoil_at", 0.35f);
        return f.withSpread(heat < spoilAt ? p.get("cold_spread", 0.65f)
                : Mth.lerp(Math.min(1f, (heat - spoilAt) / (1f - spoilAt)), 1f, p.get("spoiled_spread", 1.4f)));
    }
}
