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
package io.github.soundgoodizerfan.feedback.modular;

import net.minecraft.world.inventory.DataSlot;

/**
 * The selected folder tab of a menu: a small synced number, chosen by menu buttons {@code first .. first + count - 1}, so the server decides which
 * tab is open and the client's {@code FolderTabs} strip only reads it. Any menu adopts it in three lines: build one, {@code addDataSlot(tabs.slot())},
 * and offer button ids to {@link #click} from {@code clickMenuButton}. Server-safe (no client classes).
 */
public final class MenuTabs {

    private final DataSlot selected = DataSlot.standalone();
    private final int first, count;

    /** {@code count} tabs, selected by menu buttons {@code first} to {@code first + count - 1}. */
    public MenuTabs(int first, int count) {
        this.first = first;
        this.count = count;
    }

    /** The synced value, for the menu to register with {@code addDataSlot}. */
    public DataSlot slot() {
        return selected;
    }

    /** The open tab's index. */
    public int get() {
        return count == 0 ? 0 : Math.min(selected.get(), count - 1);
    }

    /** The button that selects tab {@code index}. */
    public int button(int index) {
        return first + index;
    }

    /** Select the tab {@code id} names, if it names one; true if it did (the menu then resets whatever depends on the tab). */
    public boolean click(int id) {
        if (id < first || id >= first + count)
            return false;
        selected.set(id - first);
        return true;
    }
}
