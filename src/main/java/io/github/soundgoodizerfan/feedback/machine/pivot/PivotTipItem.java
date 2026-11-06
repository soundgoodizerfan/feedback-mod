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
package io.github.soundgoodizerfan.feedback.machine.pivot;

import net.minecraft.world.item.Item;

/** A tip for the pivot mast's arm: one item per {@link PivotTip}. Seated by right-click, taken back with a sneaking empty hand. */
public class PivotTipItem extends Item {

    private final PivotTip tip;

    public PivotTipItem(PivotTip tip, Properties properties) {
        super(properties);
        this.tip = tip;
    }

    public PivotTip tip() {
        return tip;
    }
}
