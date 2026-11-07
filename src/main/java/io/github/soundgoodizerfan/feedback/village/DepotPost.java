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
package io.github.soundgoodizerfan.feedback.village;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Goods on the road between two Depots. A convoy leaves one depot and the goods are kept here, addressed to the other, until they are due: the
 * destination collects them when it next ticks. So a transfer between villages that are never loaded together still arrives, and no chunk is ever
 * forced loaded. Stored once for the whole server, in the overworld's data storage.
 */
public final class DepotPost extends SavedData {

    private static final String NAME = "feedback_depot_post";

    private record Parcel(List<ItemStack> goods, long due) {
    }

    private final Map<String, List<Parcel>> byDestination = new HashMap<>();

    public static DepotPost get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(DepotPost::new, DepotPost::load), NAME);
    }

    private static String key(GlobalPos p) {
        return p.dimension().location() + "|" + p.pos().asLong();
    }

    /** Addresses {@code goods} to {@code destination}, due at game tick {@code due}. */
    public void send(GlobalPos destination, List<ItemStack> goods, long due) {
        if (goods.isEmpty())
            return;
        byDestination.computeIfAbsent(key(destination), k -> new ArrayList<>()).add(new Parcel(List.copyOf(goods), due));
        setDirty();
    }

    /** The goods due at {@code destination} by {@code now}, removed from the post. */
    public List<ItemStack> collect(GlobalPos destination, long now) {
        List<Parcel> parcels = byDestination.get(key(destination));
        List<ItemStack> out = new ArrayList<>();
        if (parcels == null)
            return out;
        var it = parcels.iterator();
        while (it.hasNext()) {
            Parcel p = it.next();
            if (p.due() <= now) {
                out.addAll(p.goods());
                it.remove();
            }
        }
        if (parcels.isEmpty())
            byDestination.remove(key(destination));
        if (!out.isEmpty())
            setDirty();
        return out;
    }

    /** How many parcels are on the road to {@code destination}. */
    public int onTheRoad(GlobalPos destination) {
        List<Parcel> parcels = byDestination.get(key(destination));
        return parcels == null ? 0 : parcels.size();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag all = new ListTag();
        byDestination.forEach((dest, parcels) -> {
            for (Parcel p : parcels) {
                CompoundTag t = new CompoundTag();
                t.putString("To", dest);
                t.putLong("Due", p.due());
                ListTag goods = new ListTag();
                for (ItemStack s : p.goods())
                    goods.add(s.save(registries));
                t.put("Goods", goods);
                all.add(t);
            }
        });
        tag.put("Parcels", all);
        return tag;
    }

    private static DepotPost load(CompoundTag tag, HolderLookup.Provider registries) {
        DepotPost post = new DepotPost();
        for (Tag e : tag.getList("Parcels", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) e;
            List<ItemStack> goods = new ArrayList<>();
            for (Tag g : t.getList("Goods", Tag.TAG_COMPOUND))
                ItemStack.parse(registries, g).ifPresent(goods::add);
            if (!goods.isEmpty())
                post.byDestination.computeIfAbsent(t.getString("To"), k -> new ArrayList<>()).add(new Parcel(goods, t.getLong("Due")));
        }
        return post;
    }
}
