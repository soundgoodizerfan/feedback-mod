// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.modular.ModularBake;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A barrel jacket that stores the barrel's vibration: active on a gun whose barrel has its {@code jacket} slot filled. The jacket's material
 * states how ({@code mechanical.damping}): cold, the group tightens to {@code cold_spread}; as the barrel heats past {@code spoil_at}
 * (a share of the way from the room to the round's ignition) the stored vibration goes bad and the group opens to {@code spoiled_spread},
 * worse than bare. The reverse of the thermal sleeve, which wants a warm barrel. A jacket of a material with no damping does nothing.
 * Read live off the stack, so the client predicts the shot.
 */
public class GunJacket implements ModularTrait {

    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        ModularDesign design = ModularStacks.design(gun);
        var type = design == null ? null : ModularTypes.known(design.type());
        if (type == null)
            return f;
        ModularDesign.Part barrel = design.part(type.wearRole());
        Material jacket = barrel == null ? null
                : ModularBake.slotMaterial(type.roles().get(barrel.role()), barrel, "jacket", ClientMaterials.on(shooter.level()));
        Material.Mechanical.Damping d = jacket == null ? null : jacket.mechanical().damping().orElse(null);
        if (d == null)
            return f;
        float ambient = AmbientTable.at(shooter.level()).value();
        float heat = Math.max(0f, (GunCondition.temperature(gun, shooter.level()).value() - ambient)
                / (FTuning.GUN_PROPELLANT_IGNITION_TU - ambient));
        return f.withSpread(heat < d.spoilAt() ? d.coldSpread()
                : Mth.lerp(Math.min(1f, (heat - d.spoilAt()) / (1f - d.spoilAt())), 1f, d.spoiledSpread()));
    }
}
