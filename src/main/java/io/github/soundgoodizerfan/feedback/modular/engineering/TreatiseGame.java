// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.engineering;

import io.github.soundgoodizerfan.feedback.modular.School;
import io.github.soundgoodizerfan.feedback.modular.Treatises;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The Treatise tab's learning step. Taking a specimen apart teaches its school's volume, written onto the blank treatise; today that is
 * immediate. A later learning minigame replaces {@link #onLearn} (it may return a worse copy, or none, by how the play went) without
 * touching the menu, which only asks what the blank becomes.
 */
public final class TreatiseGame {

    private TreatiseGame() {
    }

    /** What the blank treatise becomes when {@code player} takes {@code specimen} apart to learn volume {@code volume} of {@code school}. */
    public static ItemStack onLearn(ServerPlayer player, School school, int volume, ItemStack specimen) {
        return Treatises.of(school, volume);
    }
}
