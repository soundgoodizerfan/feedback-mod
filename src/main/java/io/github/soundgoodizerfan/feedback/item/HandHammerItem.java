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
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.process.Operation;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * The slowest way to make anything, and it never stops working.
 *
 * <h2>Why a hand hammer exists at all</h2>
 * Philosophy 7: <em>manual production stays theoretically possible for a surprising share of the
 * game, at tiny rates.</em> Automation makes a process practical; it does not unlock it. Without
 * this item that promise had nothing behind it -- a player's first copper plate required a
 * Mechanical Hammer, and a Mechanical Hammer required copper plates, which is a hard gate standing
 * exactly where the philosophy says there is no physical impossibility.
 *
 * <h2>It swings in a crafting grid, not at a block</h2>
 * Hammer and workpiece in any crafting table; one craft is one blow. The alternative -- right-click
 * a block in the world -- was considered and dropped for needing a surface to hit, which is the
 * bootstrap problem all over again, and for being a fifth sneak-click handler immediately after
 * four were deleted.
 *
 * <p>The grid route earns something the block route could not. A craft is atomic, so five crafts
 * make a plate and a sixth makes foil -- the {@code WORK} component accumulates on the ingot
 * exactly as it does on the anvil (see {@code HandDeformationRecipe}). Shift-clicking runs the
 * whole stack straight past plate into foil into scrap, which is beat 1's entire lesson delivered
 * by the player's own hand, at their own pace, on the material chosen for being forgiving.
 *
 * <h2>It does not wear on a wasted swing</h2>
 * The Mechanical Hammer absorbs force that cannot go into the work, because a machine geared past
 * what its head can take is a mistake the machine records. A hand hammer has no equivalent: a swing
 * that cannot land is a craft the grid never offers, so nothing is spent and nothing is damaged.
 * You cannot mis-swing at something you were never able to lift.
 *
 * @see HandToolItem for the durability rule and the reasoning behind the shared shape
 */
public class HandHammerItem extends HandToolItem {

    public HandHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public St getStrength() {
        return FTuning.HAND_HAMMER_ST;
    }

    @Override
    public Operation getOperation() {
        return Operation.BLOW;
    }

    /**
     * The Mechanical Hammer's own blow, quieter. Deliberately the same sound rather than a hand
     * version of it: the two are landing the same blow through the same code, and a player who
     * later builds the machine should recognise what it is doing from across the room.
     */
    @Override
    public SoundEvent getWorkSound() {
        return SoundEvents.ANVIL_LAND;
    }
}
