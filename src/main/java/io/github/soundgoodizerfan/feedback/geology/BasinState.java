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
package io.github.soundgoodizerfan.feedback.geology;

import java.util.HashMap;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.core.unit.Pu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The live state of every basin touched so far in one dimension (petrochemistry spec 2.3): centre,
 * remaining mB, crude figures. An entry is created lazily from {@link BasinFeature#sitesNear} the first
 * time something asks about a position inside it, so unexplored basins cost nothing.
 *
 * <p>The API a derrick uses: {@link #at} to find the basin under a block, then {@link Entry#remainingMb},
 * {@link Entry#pressure}, {@link Entry#grade} and {@link Entry#draw}. Pressure is derived, not stored:
 * {@code P = P0 * (V / V0) ^ k} (spec 3.2). Server side only.
 */
public class BasinState extends SavedData {

    private static final String NAME = "feedback_basins";

    private final Map<String, Entry> entries = new HashMap<>();

    public static BasinState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(BasinState::new, BasinState::load, null), NAME);
    }

    /**
     * The basin whose circle holds column {@code pos} (the nearest centre if two overlap), or null. Height
     * is ignored: a derrick bores down from the surface to wherever the basin lies, and the depth is its problem.
     */
    public static Entry at(ServerLevel level, BlockPos pos) {
        BasinFeature.Site best = null;
        double bestDist = Double.MAX_VALUE;
        for (var site : BasinFeature.sitesNear(level.getSeed(), level.dimension().location(), pos.getX(), pos.getZ())) {
            double dx = pos.getX() - site.centre().getX(), dz = pos.getZ() - site.centre().getZ(), d = dx * dx + dz * dz;
            if (d <= (double) site.radius() * site.radius() && d < bestDist) {
                best = site;
                bestDist = d;
            }
        }
        if (best == null)
            return null;
        BasinState state = get(level);
        var site = best;
        return state.entries.computeIfAbsent(site.id(), id -> {
            state.setDirty();
            var b = site.basin();
            return state.new Entry(id, site.centre(), site.radius(), b.volumeMb(), b.volumeMb(), b.drivePu(), b.exponent(), site.grade());
        });
    }

    public final class Entry {
        private final String id;
        private final BlockPos centre;
        private final int radius, volumeMb;
        private int remainingMb;
        private final float drivePu, exponent;
        private final CrudeGrade grade;

        private Entry(String id, BlockPos centre, int radius, int volumeMb, int remainingMb, float drivePu, float exponent,
                      CrudeGrade grade) {
            this.id = id;
            this.centre = centre;
            this.radius = radius;
            this.volumeMb = volumeMb;
            this.remainingMb = remainingMb;
            this.drivePu = drivePu;
            this.exponent = exponent;
            this.grade = grade;
        }

        public String id() {
            return id;
        }

        public BlockPos centre() {
            return centre;
        }

        public int radius() {
            return radius;
        }

        public int remainingMb() {
            return remainingMb;
        }

        public int volumeMb() {
            return volumeMb;
        }

        /** Reservoir pressure now: {@code P0 * (V / V0) ^ k}. */
        public Pu pressure() {
            return new Pu(drivePu * (float) Math.pow(remainingMb / (double) volumeMb, exponent));
        }

        public CrudeGrade grade() {
            return grade;
        }

        /** Takes up to {@code mb} out of the basin and returns what came: less if the basin is nearly dry. */
        public int draw(int mb) {
            int taken = Math.max(0, Math.min(mb, remainingMb));
            if (taken > 0) {
                remainingMb -= taken;
                setDirty();
            }
            return taken;
        }
    }

    private static BasinState load(CompoundTag tag, HolderLookup.Provider registries) {
        BasinState state = new BasinState();
        for (Tag t : tag.getList("basins", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            String id = e.getString("id");
            state.entries.put(id, state.new Entry(id, new BlockPos(e.getInt("x"), e.getInt("y"), e.getInt("z")),
                    e.getInt("r"), e.getInt("v0"), e.getInt("v"), e.getFloat("p0"), e.getFloat("k"),
                    new CrudeGrade(e.getFloat("density"), e.getFloat("sulfur"), e.getFloat("light"))));
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry b : entries.values()) {
            CompoundTag e = new CompoundTag();
            e.putString("id", b.id);
            e.putInt("x", b.centre.getX());
            e.putInt("y", b.centre.getY());
            e.putInt("z", b.centre.getZ());
            e.putInt("r", b.radius);
            e.putInt("v0", b.volumeMb);
            e.putInt("v", b.remainingMb);
            e.putFloat("p0", b.drivePu);
            e.putFloat("k", b.exponent);
            e.putFloat("density", b.grade.density());
            e.putFloat("sulfur", b.grade.sulfur());
            e.putFloat("light", b.grade.lightFraction());
            list.add(e);
        }
        tag.put("basins", list);
        return tag;
    }
}
