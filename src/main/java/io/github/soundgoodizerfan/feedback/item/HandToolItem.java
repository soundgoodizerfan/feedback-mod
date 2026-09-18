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
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.process.Operation;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A tool a player works with by hand, in a crafting grid. One craft is one application.
 *
 * <h2>Why there is a base class for what is currently one tool</h2>
 * The same argument that built {@code Instrument} before any instrument existed, and for the same
 * payoff: a high fixed cost paid once beats no fixed cost and a medium variable cost paid per
 * feature, and the crossover arrives earlier than it feels like it will. A hand tool is three
 * things -- a strength, a noise, and the fact that it survives the craft one point worse -- and
 * every one of those is identical for every tool that will ever exist. Writing them again per tool
 * is how two tools end up with two different durability rules.
 *
 * <h2>What subclasses actually decide</h2>
 * Almost nothing, deliberately. {@link #getStrength()} is the whole of what makes one tool
 * different from another, because philosophy 17 says what an application accomplishes is
 * {@code St / material hardness} -- a property of the material, never a tool stat. A tool declares
 * how hard it hits and the material decides what that means.
 *
 * <p>A tool that does not strike things at all declares {@code 0} and needs no further opt-out:
 * every deformation lookup already refuses a blow below the material's hardness floor, so a strength
 * of zero lands nothing anywhere, forever.
 *
 * <h2>Correction: an operation had to be declared after all</h2>
 * This class used to say there should be no registry of which tools do which operations until an
 * operation existed that was not deformation. Wire drawing arrived and stayed deformation --
 * same work/hardness/result shape -- while still being a different physical action from a blow,
 * so the same input item can now have one entry a hammer reaches and a different entry a draw
 * plate reaches. That needed a second key, not a registry: {@link #getOperation()} is one more
 * method a tool declares about itself, the same shape {@link #getStrength()} already has, and
 * there is still nothing anywhere mapping tool classes to behaviour.
 *
 * <h2>Durability, and where it came from</h2>
 * The mechanism is GregTech CEu Modern's, read and rewritten (LGPL-3.0, conveyable under GPL-3.0):
 * a tool is an ordinary crafting ingredient whose <em>remainder</em> is itself, one point more
 * damaged, so vanilla's {@code Recipe#getRemainingItems} does the rest and no recipe needs
 * durability code. Deliberately not taken: GregTech's tag-and-symbol registry, which indexes nine
 * tools by a character in a recipe pattern. That is the right answer for nine tools and pure
 * overhead for a shared superclass.
 *
 * @see HandToolCrafting for the noise, which is a separate problem from the tool
 */
public abstract class HandToolItem extends Item {

    protected HandToolItem(Properties properties) {
        super(properties);
    }

    /**
     * How hard this lands, in St.
     *
     * <p>Divided by the material's hardness to find what one application accomplishes (§17), which
     * is why the same 3 St is a copper plate in five swings and is nothing whatsoever against
     * steel's hardness of 15. That is philosophy 7's hard gate arriving as arithmetic, with no rule
     * about hands written anywhere.
     *
     * @return {@code 0} for a tool that does not strike, which then lands nothing on any material
     */
    public abstract St getStrength();

    /** How this tool delivers its strength -- a blow or a draw. See {@link Operation}. */
    public abstract Operation getOperation();

    /** The noise one application makes. Played by {@link HandToolCrafting}, once per craft. */
    public abstract SoundEvent getWorkSound();

    /**
     * The tool survives the craft, one point worse.
     *
     * <p>A tool damaged past its limit returns empty and is destroyed, which is vanilla's own
     * behaviour for a broken tool rather than a rule invented here.
     */
    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack remainder = stack.copy();
        remainder.setDamageValue(remainder.getDamageValue() + 1);
        return remainder.getDamageValue() >= remainder.getMaxDamage() ? ItemStack.EMPTY : remainder;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }
}
