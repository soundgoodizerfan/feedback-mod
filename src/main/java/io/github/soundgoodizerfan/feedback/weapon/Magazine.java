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
package io.github.soundgoodizerfan.feedback.weapon;

import com.mojang.serialization.Codec;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Rounds in a magazine, or in a gun's well: a stack with the next round to fire last. It is the
 * component on both a Magazine item and a Gun, so a reload is moving this record from one to the
 * other. The rounds of one magazine share a bore -- a different one is refused on the way in.
 */
public record Magazine(List<Cartridge> rounds) {

    public static final Magazine EMPTY = new Magazine(List.of());

    public static final Codec<Magazine> CODEC = Cartridge.CODEC.listOf().xmap(Magazine::new, Magazine::rounds);

    public static final StreamCodec<RegistryFriendlyByteBuf, Magazine> STREAM_CODEC =
            Cartridge.STREAM_CODEC.apply(ByteBufCodecs.list()).map(Magazine::new, Magazine::rounds);

    public Magazine {
        rounds = List.copyOf(rounds);
    }

    public boolean isEmpty() {
        return rounds.isEmpty();
    }

    public boolean isFull() {
        return rounds.size() >= FTuning.GUN_MAGAZINE_CAPACITY;
    }

    /** The next round to fire. */
    public Cartridge top() {
        return rounds.get(rounds.size() - 1);
    }

    public Magazine pop() {
        return new Magazine(rounds.subList(0, rounds.size() - 1));
    }

    /** Whether {@code round} may go in: there is room, and it is the bore of what is already here. */
    public boolean accepts(Cartridge round) {
        return !isFull() && (isEmpty() || rounds.get(0).boreMm() == round.boreMm());
    }

    public Magazine push(Cartridge round) {
        List<Cartridge> out = new ArrayList<>(rounds);
        out.add(round);
        return new Magazine(out);
    }

    /** The bore of the rounds, or -1 if there are none. */
    public int boreMm() {
        return rounds.isEmpty() ? -1 : rounds.get(0).boreMm();
    }
}
