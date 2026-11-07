// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/**
 * The spear's third-person arm pose: an added {@code HumanoidModel.ArmPose} (declared in
 * {@code META-INF/enumextensions.json}, built from this proxy). Held out in front while charging,
 * lowered when disengaged, a shake when tired.
 */
public final class SpearPose {

    private SpearPose() {
    }

    public static final EnumProxy<HumanoidModel.ArmPose> PROXY = new EnumProxy<>(HumanoidModel.ArmPose.class, false,
            (IArmPoseTransformer) (model, entity, arm) -> {
                var part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                float[] ph = SpearClient.phase(entity, 0f);
                float raise = ph[0] * ph[0] * (3 - 2 * ph[0]), tired = ph[1], fade = ph[2];
                float t = entity.tickCount;
                // Level and braced while engaged; the arm sags and wobbles as it tires, then falls and shakes as it gives out.
                part.xRot = -1.5f + model.head.xRot + 0.6f * (1 - raise) + 0.25f * tired + 0.7f * fade + Mth.sin(t * 4.1f) * 0.05f * fade;
                part.yRot = arm == HumanoidArm.RIGHT ? -0.15f : 0.15f;
                part.zRot = Mth.sin(t * 1.7f) * (0.02f + 0.06f * tired + 0.1f * fade);
            });
}
