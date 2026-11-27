// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.world.item.ItemStack;

/**
 * The work done against the work required, kept on a pressed or ground form item: the forging rule ({@code ForgeRhythm.Work#ratio})
 * for a machine that works item to item. A stage finishes when its work is met and the surplus of the last stroke or pass is what it
 * overshot: over is thin and light, and a part cut from the stock carries it ({@code Measures.work}). A stage never finishes short,
 * so the ledger only ever reads over. Stamped when a BLOW or GRIND stage finishes, quantised so stacks still stack, and absent (1)
 * for work that came within {@link ProcessTuning#LEDGER_DEADBAND}.
 */
public final class WorkLedger {

    private WorkLedger() {
    }

    /** Whether {@code operation}'s stages are kept in the ledger. */
    static boolean keeps(Operation operation) {
        return operation == Operation.BLOW || operation == Operation.GRIND;
    }

    /** Stamps the ratio of {@code worked} Wu done to the {@code required} a finished stage asked, or clears it when it is within the deadband. */
    static void stamp(ItemStack stack, int worked, int required) {
        float r = required <= 0 ? 1f : Math.min(ProcessTuning.LEDGER_MAX, (float) worked / required);
        r = Math.round(r / ProcessTuning.LEDGER_STEP) * ProcessTuning.LEDGER_STEP;
        if (Math.abs(r - 1f) < ProcessTuning.LEDGER_DEADBAND)
            stack.remove(FDataComponents.WORK_RATIO.get());
        else
            stack.set(FDataComponents.WORK_RATIO.get(), r);
    }

    /** The ratio on {@code stack}: 1 when it carries none. */
    public static float of(ItemStack stack) {
        return stack.getOrDefault(FDataComponents.WORK_RATIO.get(), 1f);
    }
}
