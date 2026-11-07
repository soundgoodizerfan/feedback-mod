// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.List;

/**
 * The client's copy of a table only a recipe browser reads -- a recipe browser's reasoning,
 * written once for any table rather than once per table.
 */
public final class ClientTable<T> {

    public static final ClientTable<Mixture> MIXTURES = new ClientTable<>();
    public static final ClientTable<io.github.soundgoodizerfan.feedback.village.Good> GOODS = new ClientTable<>();
    public static final ClientTable<io.github.soundgoodizerfan.feedback.village.Want> BUYS = new ClientTable<>();
    public static final ClientTable<io.github.soundgoodizerfan.feedback.worldgen.OreVein> ORE_VEINS = new ClientTable<>();
    public static final ClientTable<io.github.soundgoodizerfan.feedback.geology.Basin> BASINS = new ClientTable<>();

    public static final ClientTable<io.github.soundgoodizerfan.feedback.weapon.GunOption> GUN_OPTIONS = new ClientTable<>();
    public static final ClientTable<io.github.soundgoodizerfan.feedback.weapon.GunBlueprintPool.Listing> GUN_BLUEPRINTS = new ClientTable<>();

    private List<T> entries = List.of();
    private Runnable listener = () -> {
    };

    private ClientTable() {
    }

    public List<T> get() {
        return entries;
    }

    public void set(List<T> incoming) {
        entries = List.copyOf(incoming);
        listener.run();
    }

    /** One listener is enough; only JEI wants this. */
    public void onChanged(Runnable onChanged) {
        listener = onChanged;
        onChanged.run();
    }
}
