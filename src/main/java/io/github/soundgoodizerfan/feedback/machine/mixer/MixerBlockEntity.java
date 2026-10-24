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
package io.github.soundgoodizerfan.feedback.machine.mixer;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.Blast;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A drum of loose powders, turned by the shaft (guns spec 13.4b). The Reloading Press seats the
 * ratio it is given; this is what makes a ratio a blend.
 *
 * <p>Uniformity climbs with turns until {@link FTuning#MIXER_BLEND_TURNS}. Draw the batch early and
 * only the uniform share of it is usable (lumps and dead pockets do not burn): {@link #draw} keeps
 * that share of every component and the rest is lost, so a failed batch consumes its inputs.
 *
 * <p><b>The machine does not know it is done.</b> Keep turning and the drum grinds the finished
 * blend: grains break down and segregate by density, so uniformity falls toward its floor, and each
 * turn adds friction heat. When the heat above ambient reaches the lowest ignition temperature in
 * the load, the load goes up with {@link Blast}'s rule. Nothing here wears the body: the drum's
 * only victim is its own contents.
 *
 * <p><b>[OPEN]</b> Numbers are placeholders; whether the drum should cool when stopped.
 */
public class MixerBlockEntity extends RotationNode {

    private final List<Cartridge.Charge> contents = new ArrayList<>();
    private float turns;
    private float heat;

    public MixerBlockEntity(BlockPos pos, BlockState state) {
        super(FWeapons.MIXER_ENTITY.get(), pos, state);
    }

    @Override
    public Su getLoadSu() {
        return FTuning.MIXER_LOAD_SU;
    }

    private int total() {
        return contents.stream().mapToInt(Cartridge.Charge::count).sum();
    }

    /** Pours one item in; false if the drum is full or the load would not be a charge (a coil mixed with a gas). */
    public boolean pour(ItemStack stack) {
        var materials = MaterialTable.get().materials();
        var found = io.github.soundgoodizerfan.feedback.process.Material.find(materials, stack.getItem());
        if (found.isEmpty() || total() >= FTuning.GUN_CHARGE_MAX)
            return false;
        ResourceLocation id = found.get().id();
        List<Cartridge.Charge> next = new ArrayList<>(contents);
        int at = -1;
        for (int k = 0; k < next.size(); k++)
            if (next.get(k).material().equals(id))
                at = k;
        if (at >= 0)
            next.set(at, new Cartridge.Charge(id, next.get(at).count() + 1));
        else
            next.add(new Cartridge.Charge(id, 1));
        if (Cartridge.Blend.of(materials, next).isEmpty())
            return false;
        // Fresh powder on top of a blend is unmixed: the old turns spread over the larger load.
        turns *= total() / (float) (total() + 1);
        contents.clear();
        contents.addAll(next);
        stack.shrink(1);
        setChanged();
        return true;
    }

    /** What is in the drum: the drum is open, so anyone may look. */
    public List<Cartridge.Charge> contents() {
        return List.copyOf(contents);
    }

    /** Friction heat above ambient, Tu; state, so a HUD reads it through the instrument seam. */
    public float heat() {
        return heat;
    }

    /** Whether the drum has turned past the blend point and is grinding the finished mix. */
    public boolean overmixed() {
        return turns > FTuning.MIXER_BLEND_TURNS;
    }

    /** Fraction of the load that is uniformly mixed now. */
    public float uniformity() {
        if (turns <= FTuning.MIXER_BLEND_TURNS)
            return turns / FTuning.MIXER_BLEND_TURNS;
        float over = (turns - FTuning.MIXER_BLEND_TURNS) / FTuning.MIXER_SEGREGATE_TURNS;
        return Math.max(FTuning.MIXER_UNIFORMITY_FLOOR, 1f - over * (1f - FTuning.MIXER_UNIFORMITY_FLOOR));
    }

    /** Empties the drum into a blend item, keeping only the uniform share of each component; empty if nothing usable. */
    public ItemStack draw() {
        float u = uniformity();
        List<Cartridge.Charge> usable = new ArrayList<>();
        for (var c : contents) {
            int n = Math.round(c.count() * u);
            if (n > 0)
                usable.add(new Cartridge.Charge(c.material(), n));
        }
        contents.clear();
        turns = 0;
        heat = 0;
        setChanged();
        if (usable.isEmpty())
            return ItemStack.EMPTY;
        ItemStack out = new ItemStack(FWeapons.BLEND_ITEM.get());
        out.set(FWeapons.BLEND.get(), usable);
        return out;
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel server) || contents.isEmpty())
            return;
        float rpm = Math.abs(getRpm().value());
        if (rpm <= FTuning.STOPPED_RPM_THRESHOLD.value())
            return;
        float revs = rpm * FTuning.STROKES_PER_RPM_PER_TICK;
        turns += revs;
        if (turns > FTuning.MIXER_BLEND_TURNS)
            heat += revs * FTuning.MIXER_FRICTION_TU;
        setChanged();
        Optional<Cartridge.Blend> blend = Cartridge.Blend.of(MaterialTable.get().materials(), contents);
        if (blend.isPresent() && AmbientTable.at(server).value() + heat >= blend.get().ignitionTu())
            detonate(server, blend.get());
    }

    private void detonate(ServerLevel server, Cartridge.Blend blend) {
        float power = Blast.power(blend, null);
        boolean fire = Blast.fire(blend);
        BlockPos at = worldPosition;
        server.removeBlockEntity(at);
        server.removeBlock(at, false);
        server.explode(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, Math.max(power, FTuning.DEMO_POWER_MIN), fire,
                Level.ExplosionInteraction.BLOCK);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (var c : contents) {
            CompoundTag e = new CompoundTag();
            e.putString("material", c.material().toString());
            e.putInt("count", c.count());
            list.add(e);
        }
        tag.put("Contents", list);
        tag.putFloat("Turns", turns);
        tag.putFloat("Heat", heat);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        contents.clear();
        for (Tag t : tag.getList("Contents", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            contents.add(new Cartridge.Charge(ResourceLocation.parse(e.getString("material")), e.getInt("count")));
        }
        turns = tag.getFloat("Turns");
        heat = tag.getFloat("Heat");
    }
}
