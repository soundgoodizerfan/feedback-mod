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
package io.github.soundgoodizerfan.feedback.machine.ender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Where every placed end of every bonded pair stands, kept in the overworld's saved data so a locator can find an end whose chunk is
 * not loaded (the live registry on {@link EnderPortBlockEntity} only knows loaded ends). A port records itself when it loads and
 * is removed when it is broken, never when its chunk merely unloads.
 */
public class PairBook extends SavedData {

    /** One placed end. */
    public record End(ResourceKey<Level> dimension, BlockPos pos) {}

    private final Map<UUID, List<End>> ends = new HashMap<>();

    public static PairBook get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(PairBook::new, PairBook::load), "feedback_pair_book");
    }

    public void put(UUID pair, ResourceKey<Level> dimension, BlockPos pos) {
        List<End> list = ends.computeIfAbsent(pair, k -> new ArrayList<>());
        End end = new End(dimension, pos.immutable());
        if (!list.contains(end)) {
            list.add(end);
            setDirty();
        }
    }

    public void remove(UUID pair, ResourceKey<Level> dimension, BlockPos pos) {
        List<End> list = ends.get(pair);
        if (list != null && list.remove(new End(dimension, pos))) {
            if (list.isEmpty())
                ends.remove(pair);
            setDirty();
        }
    }

    /** The end of {@code pair} that is not the one at {@code dimension}/{@code pos}, or null. */
    public End partner(UUID pair, ResourceKey<Level> dimension, BlockPos pos) {
        for (End end : ends.getOrDefault(pair, List.of()))
            if (!end.dimension().equals(dimension) || !end.pos().equals(pos))
                return end;
        return null;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        ends.forEach((pair, placed) -> placed.forEach(end -> {
            CompoundTag e = new CompoundTag();
            e.putUUID("Pair", pair);
            e.putString("Dim", end.dimension().location().toString());
            e.put("Pos", NbtUtils.writeBlockPos(end.pos()));
            list.add(e);
        }));
        tag.put("Ends", list);
        return tag;
    }

    private static PairBook load(CompoundTag tag, HolderLookup.Provider registries) {
        PairBook book = new PairBook();
        for (Tag t : tag.getList("Ends", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            NbtUtils.readBlockPos(e, "Pos").ifPresent(pos -> book.ends.computeIfAbsent(e.getUUID("Pair"), k -> new ArrayList<>())
                    .add(new End(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(e.getString("Dim"))), pos)));
        }
        return book;
    }
}
