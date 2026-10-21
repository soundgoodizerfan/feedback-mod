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
package io.github.soundgoodizerfan.feedback.modular.trait;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.UseOnContext;

/**
 * The kama's row. Tilling a block also tills the {@code length} blocks beyond it, the way the
 * wielder faces; {@code ModularHoe} runs each as a tilling of its own, so each costs the point of
 * wear a tilling costs. Cost: that wear, and {@code cooldown_ticks} before the next use.
 * Sneaking tills one.
 */
public final class Kama implements ModularTrait {

    /** The blocks to till after the clicked one. */
    public static List<BlockPos> extra(UseOnContext context, Params params) {
        List<BlockPos> spots = new ArrayList<>();
        for (int i = 1; i <= (int) params.get("length", 2); i++)
            spots.add(context.getClickedPos().relative(context.getHorizontalDirection(), i));
        return spots;
    }
}
