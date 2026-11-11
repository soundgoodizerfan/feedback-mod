// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The one hand assembly a recipe data stick holds when it was recorded at the Engineering Table, for the Auto Engineer to play back.
 *
 * <p>{@code grid} is the nine grid slots in reading order, each the stock class of what sat there ({@code metal/plate}: material kind and
 * form, else {@code item:<id>} for something no material lists; empty for an empty slot), {@code counts} how many of it each slot held.
 * {@code tools} are the item abilities the work wore a point of ({@code feedback:hammer}), {@code fasteners} how many it spent and
 * {@code school} the treatise mask the maker held, which the machine borrows because the stick carries the knowledge. {@code result} is the
 * item the assembly made; a modular type is its own item, so it names the type too.
 */
public record RecordedAssembly(String station, String result, List<String> grid, List<Integer> counts, List<String> tools, int fasteners,
                               int school) {

    /** The {@code modular/Station}-style id of the bench it was recorded at. */
    public static final String STATION = "engineering";

    public static final Codec<RecordedAssembly> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("station").forGetter(RecordedAssembly::station),
            Codec.STRING.fieldOf("result").forGetter(RecordedAssembly::result),
            Codec.STRING.listOf().fieldOf("grid").forGetter(RecordedAssembly::grid),
            Codec.INT.listOf().fieldOf("counts").forGetter(RecordedAssembly::counts),
            Codec.STRING.listOf().fieldOf("tools").forGetter(RecordedAssembly::tools),
            Codec.INT.fieldOf("fasteners").forGetter(RecordedAssembly::fasteners),
            Codec.INT.fieldOf("school").forGetter(RecordedAssembly::school)
    ).apply(i, RecordedAssembly::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RecordedAssembly> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                buf.writeUtf(r.station);
                buf.writeUtf(r.result);
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, r.grid);
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).encode(buf, r.counts);
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, r.tools);
                buf.writeVarInt(r.fasteners);
                buf.writeVarInt(r.school);
            },
            buf -> new RecordedAssembly(buf.readUtf(), buf.readUtf(),
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf),
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).decode(buf),
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf), buf.readVarInt(), buf.readVarInt()));

    /** The distinct stock classes the grid holds, in slot order. */
    public List<String> classes() {
        List<String> out = new ArrayList<>();
        for (String c : grid)
            if (!c.isEmpty() && !out.contains(c))
                out.add(c);
        return out;
    }
}
