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
        return onLearn(player, school, volume, specimen, 0.5f);
    }

    /** As above, the notes as good as {@code quality} (0 to 1) says: how well the specimen was read in the learning game. */
    public static ItemStack onLearn(ServerPlayer player, School school, int volume, ItemStack specimen, float quality) {
        ItemStack out = Treatises.of(school, volume);
        out.set(io.github.soundgoodizerfan.feedback.registry.FModular.NOTES.get(), Math.max(0f, Math.min(1f, quality)));
        return out;
    }
}
