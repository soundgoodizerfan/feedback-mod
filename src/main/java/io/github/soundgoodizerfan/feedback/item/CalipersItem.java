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
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.machine.Wearing;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Turns "kinda flattened" into a figure.
 *
 * <h2>An instrument that refines rather than reveals</h2>
 * The player could always see a workpiece flattening — beat 1 deliberately ships no measuring
 * tool, because eyes are sufficient for *avoiding* scrap. What eyes cannot do is <em>aim</em>.
 * <p>
 * Calipers exist because of the overrun band: foil is a genuinely useful material sitting in a
 * narrow window past plate, and hitting that window on purpose every time is the first thing in
 * the game that needs a number rather than a glance. So this is a yield technology and not a key
 * (§7). It unlocks nothing. It improves a conversion ratio, which is why anyone buys it.
 * <p>
 * It matters that this is the weaker of the slice's two instruments and arrives first. When the
 * thermometer turns up it will reveal a quantity the player cannot perceive at all, which is a
 * much larger event — and it lands harder because calipers already taught what an instrument is
 * for.
 *
 * <h2>Why holding it is enough — for an item</h2>
 * No right-click to read a workpiece. Applying calipers is a real action, but on something the
 * player is already holding it is a keystroke with no decision inside it, and §3 cuts mechanics
 * that are real, well-precedented and simply not fun. Holding them is the same information at the
 * same price.
 * <p>
 * <b>That argument is about items and does not extend to machines.</b> Reading a machine's wear
 * means getting down and putting the instrument on it, and a running hammer cannot be measured --
 * so the reading costs a stopped line, and there is a genuine decision in whether to pay it (§5).
 * The rule §8 states is about price rather than object: <em>a reading is passive when taking it
 * is free, and an action when taking it costs something.</em> One instrument, both behaviours,
 * and it is the same measurement either way -- a mushroomed head is a piece of metal that has
 * deformed, which is what calipers were already for.
 */
public class CalipersItem extends Item implements Instrument {

    public CalipersItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.WORK || quantity == Quantity.CONDITION || quantity == Quantity.LENGTH;
    }

    @Override
    public float resolution(Quantity quantity) {
        return switch (quantity) {
            case CONDITION -> FTuning.CALIPERS_RESOLUTION_CONDITION;
            case LENGTH -> FTuning.CALIPERS_RESOLUTION_MM;
            default -> 1f;   // Fu is whole numbers; there is nothing finer to resolve.
        };
    }

    /**
     * Puts the calipers on a machine.
     *
     * <h3>Why this runs before the block gets the click</h3>
     * NeoForge's {@code onItemUseFirst} is called ahead of any block interaction, which is the
     * only seam that works here: a hammer's own right-click accepts whatever is held as a
     * workpiece, so without this the first thing a player measuring a hammer would do is forge
     * their calipers into a plate. Taking the click here also means no machine block needs to know
     * that instruments exist -- a second wearing machine costs nothing.
     *
     * <h3>Server-side, and that is not a formality</h3>
     * Condition is only synced to the client when the <em>adjective</em> would change, because
     * wear happens blow by blow and a packet per blow would say nothing new. So the client's copy
     * is stale by up to a whole band, and a five-per-cent reading taken from it would be fiction.
     * The figure is read where it is true.
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof Wearing machine))
            return InteractionResult.PASS;

        if (!level.isClientSide)
            player.displayClientMessage(read(machine), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * The reading, or the reason there isn't one.
     *
     * <h3>The refusal is the price, and the price is the point</h3>
     * A running machine cannot be measured, so the figure costs a stopped line and there is a real
     * decision in whether to pay it (§5). A player who would rather keep producing declines to
     * know -- and that is the general rule §8 states: <em>a reading is passive when taking it is
     * free, and an action when taking it costs something.</em>
     */
    private Component read(Wearing machine) {
        if (machine.isRunning())
            return Component.translatable("feedback.readout.condition.running")
                    .withStyle(ChatFormatting.RED);

        float shown = Instruments.quantise(machine.getCondition(), resolution(Quantity.CONDITION));
        return Instruments.signed(this,
                Component.translatable("feedback.readout.condition.figure", Math.round(shown * 100)));
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.calipers");
    }
}
