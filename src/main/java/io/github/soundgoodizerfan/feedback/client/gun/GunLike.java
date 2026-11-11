// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import net.minecraft.world.item.Item;

import io.github.soundgoodizerfan.feedback.weapon.GunItem;

/** What is drawn, held and posed as a gun: a firearm, an electric gun or any other {@link GunItem}. */
public final class GunLike {

    private GunLike() {
    }

    public static boolean is(Item item) {
        return item instanceof GunItem;
    }
}
