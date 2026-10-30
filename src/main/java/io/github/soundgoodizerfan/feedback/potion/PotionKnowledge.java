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
package io.github.soundgoodizerfan.feedback.potion;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * What one player knows of the brewing map (potions spec 3.2, 3.4, 12.8): the nodes made at least once, the vortices whose landing
 * is revealed, and the residue marks failed brews left. A player attachment ({@link PotionAttachments}); nothing is shared between players.
 *
 * @param discovered effect node ids made.
 * @param vortices   vortex ids used, whose destination now shows.
 * @param residues   failed brews' end points, each aimed at the nearest node not yet discovered.
 * @param fog        the map's cells the marker has passed near, a bit each ({@link PotionTuning#FOG_N} squared bits in longs); everything else is hidden.
 */
public record PotionKnowledge(Set<String> discovered, Set<String> vortices, List<Residue> residues, List<Long> fog) {

    /** @param target the undiscovered node it leans toward; the mark clears when that node is made. Never sent to the client. */
    public record Residue(float x, float y, String target) {
        static final Codec<Residue> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.fieldOf("x").forGetter(Residue::x),
                Codec.FLOAT.fieldOf("y").forGetter(Residue::y),
                Codec.STRING.fieldOf("target").forGetter(Residue::target)
        ).apply(i, Residue::new));
    }

    public static final PotionKnowledge EMPTY = new PotionKnowledge(Set.of(), Set.of(), List.of(), List.of());

    public static final Codec<PotionKnowledge> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().xmap(l -> (Set<String>) new TreeSet<>(l), s -> List.copyOf(s)).optionalFieldOf("discovered", Set.of()).forGetter(PotionKnowledge::discovered),
            Codec.STRING.listOf().xmap(l -> (Set<String>) new TreeSet<>(l), s -> List.copyOf(s)).optionalFieldOf("vortices", Set.of()).forGetter(PotionKnowledge::vortices),
            Residue.CODEC.listOf().optionalFieldOf("residues", List.of()).forGetter(PotionKnowledge::residues),
            Codec.LONG.listOf().optionalFieldOf("fog", List.of()).forGetter(PotionKnowledge::fog)
    ).apply(i, PotionKnowledge::new));

    private static int cell(float v) {
        return Math.min(PotionTuning.FOG_N - 1, Math.max(0, (int) ((v + PotionTuning.FOG_EXTENT) / (2f * PotionTuning.FOG_EXTENT) * PotionTuning.FOG_N)));
    }

    /** The fog as bits, {@link PotionTuning#FOG_N} squared of them, row by row from the bottom-left. */
    public long[] bits() {
        long[] bits = new long[(PotionTuning.FOG_N * PotionTuning.FOG_N + 63) / 64];
        for (int i = 0; i < bits.length && i < fog.size(); i++)
            bits[i] = fog.get(i);
        return bits;
    }

    /** Whether the cell round (x, y) has been uncovered. */
    public boolean seen(float x, float y) {
        int c = cell(y) * PotionTuning.FOG_N + cell(x);
        return c / 64 < fog.size() && (fog.get(c / 64) >> (c % 64) & 1L) != 0L;
    }

    /** Uncover the cells within the reveal radius of (x, y); this record again if none was new. */
    public PotionKnowledge reveal(float x, float y) {
        long[] bits = bits();
        boolean changed = false;
        float cellSize = 2f * PotionTuning.FOG_EXTENT / PotionTuning.FOG_N;
        for (int cy = cell(y - PotionTuning.FOG_REVEAL); cy <= cell(y + PotionTuning.FOG_REVEAL); cy++)
            for (int cx = cell(x - PotionTuning.FOG_REVEAL); cx <= cell(x + PotionTuning.FOG_REVEAL); cx++) {
                float mx = -PotionTuning.FOG_EXTENT + (cx + 0.5f) * cellSize, my = -PotionTuning.FOG_EXTENT + (cy + 0.5f) * cellSize;
                int c = cy * PotionTuning.FOG_N + cx;
                if (Math.hypot(mx - x, my - y) <= PotionTuning.FOG_REVEAL && (bits[c / 64] >> (c % 64) & 1L) == 0L) {
                    bits[c / 64] |= 1L << (c % 64);
                    changed = true;
                }
            }
        if (!changed)
            return this;
        List<Long> out = new ArrayList<>(bits.length);
        for (long b : bits)
            out.add(b);
        return new PotionKnowledge(discovered, vortices, residues, out);
    }

    /** Everything known, for testing: the whole fog lifted, every node made, every vortex used, no marks left. */
    public static PotionKnowledge everything(List<? extends java.util.Map.Entry<ResourceLocation, PotionFeature>> map) {
        Set<String> nodes = new TreeSet<>(), vortices = new TreeSet<>();
        for (var e : map) {
            if (e.getValue().kind() == PotionFeature.Kind.EFFECT)
                nodes.add(e.getKey().toString());
            else if (e.getValue().kind() == PotionFeature.Kind.VORTEX)
                vortices.add(e.getKey().toString());
        }
        List<Long> fog = new ArrayList<>();
        for (int i = 0; i < (PotionTuning.FOG_N * PotionTuning.FOG_N + 63) / 64; i++)
            fog.add(-1L);
        return new PotionKnowledge(nodes, vortices, List.of(), fog);
    }

    /** A node made: discovered, and every mark that leaned toward it cleared. */
    public PotionKnowledge made(ResourceLocation node) {
        Set<String> d = new TreeSet<>(discovered);
        d.add(node.toString());
        List<Residue> r = new ArrayList<>(residues);
        r.removeIf(m -> m.target().equals(node.toString()));
        return new PotionKnowledge(d, vortices, r, fog);
    }

    public PotionKnowledge usedVortex(ResourceLocation vortex) {
        Set<String> v = new TreeSet<>(vortices);
        v.add(vortex.toString());
        return new PotionKnowledge(discovered, v, residues, fog);
    }

    /** A failed brew ended at (x, y): a mark toward the nearest node not yet discovered, if any is left. */
    public PotionKnowledge failedAt(float x, float y, List<? extends java.util.Map.Entry<ResourceLocation, PotionFeature>> map) {
        String nearest = null;
        float best = Float.MAX_VALUE;
        for (var e : map) {
            float d = e.getValue().distance(x, y);
            if (e.getValue().kind() == PotionFeature.Kind.EFFECT && !discovered.contains(e.getKey().toString()) && d < best) {
                best = d;
                nearest = e.getKey().toString();
            }
        }
        if (nearest == null)
            return this;
        List<Residue> r = new ArrayList<>(residues);
        r.add(new Residue(x, y, nearest));
        if (r.size() > 12)
            r.remove(0);
        return new PotionKnowledge(discovered, vortices, r, fog);
    }
}
