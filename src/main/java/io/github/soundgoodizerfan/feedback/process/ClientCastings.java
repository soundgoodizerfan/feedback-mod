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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.process;

import java.util.List;

/** The client's copy of the casting table, kept only so a recipe browser can print it. */
public final class ClientCastings {

    private static List<Casting> entries = List.of();
    private static Runnable listener = () -> {
    };

    private ClientCastings() {
    }

    public static List<Casting> get() {
        return entries;
    }

    public static void set(List<Casting> incoming) {
        entries = List.copyOf(incoming);
        listener.run();
    }

    public static void onChanged(Runnable onChanged) {
        listener = onChanged;
        onChanged.run();
    }
}
