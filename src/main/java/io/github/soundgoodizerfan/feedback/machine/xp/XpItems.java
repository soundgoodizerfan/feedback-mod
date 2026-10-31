/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
