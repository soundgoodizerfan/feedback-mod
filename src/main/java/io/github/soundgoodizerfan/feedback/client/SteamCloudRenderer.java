// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.weapon.SteamCloud;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** The steam of a forced vent: the entity draws nothing itself, its own tick sends the cloud particles. */
public class SteamCloudRenderer extends EntityRenderer<SteamCloud> {

    public SteamCloudRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SteamCloud entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }
}
