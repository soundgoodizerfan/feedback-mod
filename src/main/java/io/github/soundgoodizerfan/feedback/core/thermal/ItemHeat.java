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
package io.github.soundgoodizerfan.feedback.core.thermal;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.TuRate;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;

/**
 * The temperature of a loose workpiece -- in a machine, in a chest, in your hand, on the floor.
 *
 * <h2>Nothing here is ticked, and that is the whole design</h2>
 * Philosophy 9 says a heated item is hot <em>wherever it is</em>, cooling towards ambient in hand,
 * in a chest, in transit. Taking that literally rules out the obvious implementation. Items are
 * ticked only in a player's inventory and as dropped entities; a chest is not ticked, a hopper's
 * contents are not ticked, and an item passing through somebody else's pipe is certainly not. Any
 * scheme that cools by ticking has a list of places where heat quietly stops mattering, and the
 * player will find that list.
 * <p>
 * So a stack does not store its current temperature. It stores the temperature it was last
 * <em>set</em> to and the game tick that happened on, and the current figure is computed from
 * those whenever anybody asks. Decay is closed-form, so one evaluation is exact however long the
 * gap was, and "wherever it is" needs no enumerating: an ingot in an unloaded chunk, in a dropped
 * stack, or in a mod we have never heard of cools correctly because nothing had to remember to
 * cool it.
 *
 * <h2>Confirmed rather than invented</h2>
 * TerraFirmaCraft stores {@code (capacity, lastTemperature, lastTick)} on its own heat component
 * for exactly this reason. That is convergence on the one approach the problem admits, and it is
 * worth knowing the shape is load-tested rather than clever. Its licence means the code could not
 * have been borrowed even if we had wanted to -- see THIRD-PARTY-LICENSES.md.
 *
 * <h2>The stacking consequence, which is a feature</h2>
 * Components are part of a stack's identity, so two ingots heated at the same instant to the same
 * temperature merge and two heated a minute apart do not. That is correct -- they are not the same
 * object any more -- and it is why {@link #settle} matters: once a workpiece is close enough to
 * ambient its components are dropped entirely, so it becomes byte-identical to one that was never
 * heated and the player's chest does not fill with piles of one.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class ItemHeat {

    private ItemHeat() {
    }

    /**
     * An item that sheds heat at a rate of its own, not the loose workpiece's: a gun is thousands
     * of Work per Tu of metal, so a heavy barrel cools slowly where an ingot would not. Asked of
     * the stack's item, so {@link #get} stays the one place a temperature is read -- an
     * instrument and the item's own logic agree because there is no second reading.
     */
    public interface Cooling {
        TuRate coolingRate(ItemStack stack);

        /** A heat the stack never drops under (a skewer of blaze rod holds its dish warm), or null for none. */
        default Tu holdsAt(ItemStack stack) {
            return null;
        }
    }

    /** Stamp a stack as being at this temperature, now. */
    public static void set(ItemStack stack, Tu tu, Level level) {
        if (tu.value() <= FTuning.WARM_TU.value()) {
            clear(stack);
            return;
        }
        stack.set(FDataComponents.TEMPERATURE.get(), tu.value());
        stack.set(FDataComponents.HEATED_AT.get(), level.getGameTime());
        io.github.soundgoodizerfan.feedback.modular.Attunements.eraseIfOverheated(stack, tu, level);
    }

    public static void clear(ItemStack stack) {
        stack.remove(FDataComponents.TEMPERATURE.get());
        stack.remove(FDataComponents.HEATED_AT.get());
    }

    /** What this stack is at right now, having cooled since it was stamped. */
    public static Tu get(ItemStack stack, Level level) {
        // The component is a Float, and reading it boxes. That is the wire format's doing rather
        // than this package's, and it is a far bigger allocation than any unit wrapper.
        Float stamped = stack.get(FDataComponents.TEMPERATURE.get());
        Tu floor = stack.getItem() instanceof Cooling hold ? hold.holdsAt(stack) : null;
        if (stamped == null)
            return floor != null ? floor : AmbientTable.at(level);
        long at = stack.getOrDefault(FDataComponents.HEATED_AT.get(), level.getGameTime());
        float elapsed = Math.max(0, level.getGameTime() - at);
        TuRate rate = stack.getItem() instanceof Cooling c ? c.coolingRate(stack) : FTuning.ITEM_COOLING_TU_PER_TICK;
        Tu cooled = Heat.cooled(new Tu(stamped), elapsed, rate, AmbientTable.at(level));
        return floor != null && floor.value() > cooled.value() ? floor : cooled;
    }

    public static boolean isHot(ItemStack stack, Level level) {
        return stack.has(FDataComponents.TEMPERATURE.get())
                && get(stack, level).value() > FTuning.WARM_TU.value();
    }

    /**
     * Drop the heat components once a stack has cooled to nothing worth describing.
     * <p>
     * Called wherever a stack passes through our hands anyway -- a machine inserting or ejecting
     * one. There is deliberately no sweep looking for cold stacks to tidy: a stale component is
     * harmless, reads correctly if anybody asks, and clears itself the next time the item is
     * touched by anything of ours.
     */
    public static void settle(ItemStack stack, Level level) {
        if (stack.has(FDataComponents.TEMPERATURE.get())
                && get(stack, level).value() <= FTuning.WARM_TU.value())
            clear(stack);
    }

    /**
     * Re-stamp a stack at its current temperature, which restarts the clock without changing the
     * figure. Needed whenever a stack is split or copied across a tick boundary and the caller
     * wants the two halves to go on agreeing.
     */
    public static void restamp(ItemStack stack, Level level) {
        if (stack.has(FDataComponents.TEMPERATURE.get()))
            set(stack, get(stack, level), level);
    }

    /**
     * Bring two stacks of one item to their count-weighted mean temperature, stamped on this tick,
     * so they carry identical components and merge. Ingots taken from a heater one click at a time
     * differ by the ticks between clicks (1.5 Tu each), and no rounding can make that go away; a
     * bench that wants them as one pile blends them instead. Heat is conserved: the mean of equal
     * pieces, so nothing is made hotter or colder for free, only equalised as touching metal would.
     */
    public static void blend(ItemStack a, ItemStack b, Level level) {
        if (a.isEmpty() || b.isEmpty() || !ItemStack.isSameItem(a, b) || (!isHot(a, level) && !isHot(b, level)))
            return;
        float mean = (get(a, level).value() * a.getCount() + get(b, level).value() * b.getCount()) / (a.getCount() + b.getCount());
        set(a, new Tu(mean), level);
        set(b, new Tu(mean), level);
    }

    /**
     * The gesture: right-click a held stack onto a slot of the same item that differs only in heat, in any container, and
     * they blend and merge into the slot (as much as fits). Fires on both sides, so the client predicts what the server does.
     * Left-click stays vanilla. Done as a deliberate act, like a Bundle, so heat is never averaged behind the player's back.
     */
    @SubscribeEvent
    public static void onStackedOnOther(ItemStackedOnOtherEvent event) {
        ItemStack held = event.getCarriedItem();
        ItemStack onto = event.getStackedOnItem();
        var slot = event.getSlot();
        int room = onto.isEmpty() ? 0 : Math.min(slot.getMaxStackSize(onto), onto.getMaxStackSize()) - onto.getCount();
        if (event.getClickAction() != ClickAction.SECONDARY || held.isEmpty() || room <= 0 || !ItemStack.isSameItem(held, onto)
                || (!held.has(FDataComponents.TEMPERATURE.get()) && !onto.has(FDataComponents.TEMPERATURE.get()))
                || ItemStack.isSameItemSameComponents(held, onto) || !slot.mayPlace(held))
            return;
        // Same item and components bar the heat stamp, or it is a different workpiece and vanilla's rules stand.
        ItemStack a = held.copy();
        ItemStack b = onto.copy();
        clear(a);
        clear(b);
        if (!ItemStack.isSameItemSameComponents(a, b))
            return;
        var level = event.getPlayer().level();
        blend(held, onto, level);
        if (!ItemStack.isSameItemSameComponents(held, onto))
            return; // settled to cold on one side only; leave it to vanilla
        int moved = Math.min(room, held.getCount());
        onto.grow(moved);
        held.shrink(moved);
        slot.setChanged();
        event.setCanceled(true);
    }
}
