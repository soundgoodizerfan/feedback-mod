// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.xp;

import io.github.soundgoodizerfan.feedback.core.xp.XpStore;
import io.github.soundgoodizerfan.feedback.modular.attunement.ReservoirKinds;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

/** Anything that stores experience, read one way: a Reservoir-attuned tool derives its store, a container carries it. */
public final class XpItems {

    private XpItems() {
    }

    @Nullable
    public static XpStore store(ItemStack stack) {
        XpStore tool = ReservoirKinds.store(stack);
        return tool != null ? tool : stack.get(FDataComponents.XP_STORE.get());
    }

    public static void set(ItemStack stack, XpStore store) {
        stack.set(FDataComponents.XP_STORE.get(), store);
    }
}
