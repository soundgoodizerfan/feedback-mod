// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.data;

import net.neoforged.neoforge.data.event.GatherDataEvent;

/** Datagen entry point. Item models only so far; lang and the rest are still hand-authored. */
public final class FeedbackData {

    private FeedbackData() {}

    public static void gather(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeClient(),
                new FormItemModels(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
        var output = event.getGenerator().getPackOutput();
        var lookup = event.getLookupProvider();
        event.getGenerator().addProvider(event.includeClient(),
                new FormData.BlockStates(output, event.getExistingFileHelper()));
        event.getGenerator().addProvider(event.includeServer(), FormData.loot(output, lookup));
        event.getGenerator().addProvider(event.includeServer(),
                new FormData.BlockTagsGen(output, lookup, event.getExistingFileHelper()));
        event.getGenerator().addProvider(event.includeServer(), new FormData.Recipes(output, lookup));
    }
}
