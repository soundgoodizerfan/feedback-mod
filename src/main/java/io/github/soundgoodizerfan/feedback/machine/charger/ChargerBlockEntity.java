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
package io.github.soundgoodizerfan.feedback.machine.charger;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A clip on a wire that charges jar rounds ({@code docs/feedback_guns_spec.md}, the coil's electric
 * drive; mechanics 6.6). It is a sink: while a round in it holds less than the line, it asks
 * for {@link FTuning#GUN_CHARGER_AMPS}, and what it is delivered raises the stack's volts by amps
 * over the stack's capacitance -- a stack of sixteen fills sixteen times slower than one -- up to
 * the voltage it sees and never past it.
 *
 * <h2>Why it has no rating and never wears</h2>
 * It is a terminal, not a machine doing work. A line above a coil's nominal is not refused and
 * does nothing to the clip: it charges the rounds harder, and the consequence arrives where the
 * work is done, as field pressure against the winding's rating when the round is fired. That is
 * the overpressure rule a chamber already has, so a hand-cranked fine dynamo is a hot load and
 * the gun's spec sheet is what says how hot is too hot.
 *
 * <p>It does not discharge into the line: a round holding more than the line simply stops
 * charging. Rounds keep leaking while they sit here, as anywhere.
 */
public class ChargerBlockEntity extends ElectricNode {

    private static final String ROUNDS = "Rounds";

    private ItemStack rounds = ItemStack.EMPTY;

    public ChargerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CHARGER.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    /** The jar round held, as it is now; empty when it holds none. */
    private java.util.Optional<Cartridge> round() {
        Cartridge c = rounds.get(FWeapons.CARTRIDGE.get());
        return c == null || !c.charged() || level == null ? java.util.Optional.empty()
                : java.util.Optional.of(c.at(level.getGameTime()));
    }

    @Override
    public float wantedAmps() {
        return round().filter(c -> getVolts() > c.volts() + FTuning.LEYDEN_JAR_SWITCH_VOLTS).isPresent()
                ? FTuning.GUN_CHARGER_AMPS : 0;
    }

    public void tickServer() {
        if (level == null || getAmps() <= 0)
            return;
        round().ifPresent(c -> {
            float volts = Math.min(getVolts(), c.volts() + getAmps() / (FTuning.GUN_ROUND_CAPACITANCE * rounds.getCount()));
            rounds.set(FWeapons.CARTRIDGE.get(), c.withVolts(Math.max(c.volts(), volts), level.getGameTime()));
            setChanged();
        });
    }

    public ItemStack getRounds() {
        return rounds;
    }

    /** Takes jar rounds, the whole stack; anything else is refused. Returns what is left in hand. */
    public ItemStack insert(ItemStack stack) {
        Cartridge c = stack.get(FWeapons.CARTRIDGE.get());
        if (!rounds.isEmpty() || c == null || !c.charged())
            return stack;
        rounds = stack.copy();
        setChanged();
        return ItemStack.EMPTY;
    }

    public ItemStack remove() {
        ItemStack out = rounds;
        rounds = ItemStack.EMPTY;
        setChanged();
        return out;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!rounds.isEmpty())
            tag.put(ROUNDS, rounds.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        rounds = ItemStack.parseOptional(registries, tag.getCompound(ROUNDS));
    }
}
