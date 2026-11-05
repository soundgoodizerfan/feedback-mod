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
