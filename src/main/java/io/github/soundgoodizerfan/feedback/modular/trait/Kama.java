// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import java.util.ArrayList;
import java.util.List;


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
