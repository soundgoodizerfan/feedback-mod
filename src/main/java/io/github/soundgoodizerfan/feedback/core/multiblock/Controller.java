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
package io.github.soundgoodizerfan.feedback.core.multiblock;

import net.minecraft.resources.ResourceLocation;

/** Mode A: a part that owns a datapack pattern ({@code data/<ns>/multiblock/<path>.json}) and walks it. */
public interface Controller extends Part {

    /** Pattern id, {@code <ns>:<path>}. */
    ResourceLocation pattern();

    /** The shared-state holder for this machine; override with a subclass of {@link Structure}. */
    default Structure createStructure() {
        return new Structure();
    }

    @Override
    default String role() {
        return "controller";
    }
}
