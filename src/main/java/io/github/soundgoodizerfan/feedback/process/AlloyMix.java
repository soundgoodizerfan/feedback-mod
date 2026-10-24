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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * What a crucible actually holds -- possibly several molten metals at once, tracked only by how
 * much of each is present. Individual metals are never extractable; {@link #resolve} is the only
 * way out, and it hands back a single named fluid or nothing at all.
 *
 * <h2>Why the tank can go empty while full</h2>
 * A mix that satisfies no {@link AlloyRecipe} is real molten metal sitting in the vessel -- it is
 * not lost -- but it is not anything with a name either, so {@link #resolve} reports empty and
 * nothing can be poured out until the ratio is corrected. Correcting it is dilution (user ruling,
 * 2026-10-23): melt more of the missing metal in. A crucible already full of a bad mix spills its
 * own mix in proportion to make room, so the correcting metal is never turned away. There is still
 * no partial extraction of one metal.
 * <p>
 * Prior art: the "track several fluids by amount, resolve to one output fluid" shape is adapted
 * from TerraFirmaCraft's {@code FluidAlloy} (EUPL-1.2) -- see {@link AlloyRange}'s doc for the
 * licence note this class shares with it.
 */
public final class AlloyMix {

    private final Map<Fluid, Integer> content = new LinkedHashMap<>();

    public void add(Fluid fluid, int amount) {
        if (fluid == Fluids.EMPTY || amount <= 0)
            return;
        content.merge(fluid, amount, Integer::sum);
    }

    public int amount() {
        int total = 0;
        for (int value : content.values())
            total += value;
        return total;
    }

    public float fraction(Fluid fluid) {
        int total = amount();
        return total <= 0 ? 0f : content.getOrDefault(fluid, 0) / (float) total;
    }

    /**
     * Something drained {@code amount} out of {@link #resolve}'s single named fluid. There is no
     * such thing as draining "just the tin," so every metal present shrinks by the same
     * proportion, keeping the ratio -- and therefore what {@link #resolve} reports next -- stable.
     */
    public void remove(int removedAmount) {
        int total = amount();
        if (removedAmount <= 0 || total <= 0)
            return;
        if (removedAmount >= total) {
            content.clear();
            return;
        }
        // Largest remainder: floor every share, then hand the leftover mB to the metals with the
        // biggest fractional parts. Rounding each share on its own creates or destroys metal and
        // walks the ratio out of its window over repeated draws.
        int remaining = total - removedAmount;
        List<Map.Entry<Fluid, Integer>> entries = new ArrayList<>(content.entrySet());
        double[] exact = new double[entries.size()];
        int assigned = 0;
        for (int i = 0; i < exact.length; i++) {
            exact[i] = entries.get(i).getValue() * (double) remaining / total;
            assigned += (int) exact[i];
        }
        Integer[] order = new Integer[exact.length];
        for (int i = 0; i < order.length; i++)
            order[i] = i;
        Arrays.sort(order, Comparator.comparingDouble(i -> -(exact[i] - Math.floor(exact[i]))));
        for (int i = 0; i < exact.length; i++)
            entries.get(i).setValue((int) exact[i]);
        for (int k = 0; k < remaining - assigned; k++)
            entries.get(order[k]).setValue(entries.get(order[k]).getValue() + 1);
        content.values().removeIf(value -> value <= 0);
    }

    public Map<Fluid, Integer> content() {
        return content;
    }

    /**
     * What the tank should currently show: the matched alloy if the ratio resolves to one, the
     * single metal itself if only one has ever been poured in (so an ordinary, non-alloyed melt
     * behaves exactly as it did before this class existed), or empty.
     */
    public FluidStack resolve() {
        int total = amount();
        if (total <= 0)
            return FluidStack.EMPTY;
        if (content.size() == 1) {
            Map.Entry<Fluid, Integer> only = content.entrySet().iterator().next();
            return new FluidStack(only.getKey(), only.getValue());
        }
        return AlloyTable.get().find(content, total)
                .map(fluid -> new FluidStack(fluid, total))
                .orElse(FluidStack.EMPTY);
    }

    public void writeNbt(CompoundTag tag) {
        ListTag list = new ListTag();
        content.forEach((fluid, amount) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Fluid", BuiltInRegistries.FLUID.getKey(fluid).toString());
            entry.putInt("Amount", amount);
            list.add(entry);
        });
        tag.put("Alloy", list);
    }

    public void readNbt(CompoundTag tag) {
        content.clear();
        if (!tag.contains("Alloy"))
            return;
        ListTag list = tag.getList("Alloy", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(entry.getString("Fluid")));
            int amount = entry.getInt("Amount");
            if (fluid != Fluids.EMPTY && amount > 0)
                content.put(fluid, amount);
        }
    }
}
