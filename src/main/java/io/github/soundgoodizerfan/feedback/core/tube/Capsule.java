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
package io.github.soundgoodizerfan.feedback.core.tube;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * One carrier (or pod) in a tube block: a ledger entry, never an entity. It entered the block through
 * the face {@link #from} at {@link #enteredAt} and is at the centre at {@link #arriveAt}; between those
 * game times a rider is moved along the line. {@link #wakeAt} is when the run next looks at it (the
 * arrival, or a retry while it is blocked).
 */
public final class Capsule {

    public ItemStack stack;
    public long enteredAt, arriveAt, wakeAt;
    public Direction from;
    @Nullable
    public UUID rider;

    public Capsule(ItemStack stack, Direction from, long enteredAt, long arriveAt, @Nullable UUID rider) {
        this.stack = stack;
        this.from = from;
        this.enteredAt = enteredAt;
        this.arriveAt = arriveAt;
        this.wakeAt = arriveAt;
        this.rider = rider;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Stack", stack.save(registries));
        tag.putLong("Entered", enteredAt);
        tag.putLong("Ready", arriveAt);
        tag.putLong("Wake", wakeAt);
        tag.putInt("From", from.get3DDataValue());
        if (rider != null)
            tag.putUUID("Rider", rider);
        return tag;
    }

    @Nullable
    public static Capsule load(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStack stack = ItemStack.parse(registries, tag.getCompound("Stack")).orElse(ItemStack.EMPTY);
        if (stack.isEmpty())
            return null;
        Capsule c = new Capsule(stack, Direction.from3DDataValue(tag.getInt("From")), tag.getLong("Entered"),
                tag.getLong("Ready"), tag.hasUUID("Rider") ? tag.getUUID("Rider") : null);
        c.wakeAt = tag.getLong("Wake");
        return c;
    }
}
