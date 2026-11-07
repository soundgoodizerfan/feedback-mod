// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/**
 * Adds the mod's pieces (market stalls, a plaza centre with a Market Cross and a Ledger Board, guild yards) to the live vanilla village pools, so
 * a generated village shows the mod without anyone building anything. Inject, don't replace: nothing in a vanilla or another mod's pool file is
 * touched, and with the pieces absent a village is vanilla. What goes where is data: every {@code data/feedback/village_pieces/*.json} lists
 * {@code {pool, piece, weight}}, so a datapack adds or removes pieces without code. Existing villages are not retrofitted.
 */
public final class VillagePieces {

    private VillagePieces() {
    }

    private record Piece(ResourceLocation pool, ResourceLocation piece, int weight) {
        static final Codec<Piece> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("pool").forGetter(Piece::pool),
                ResourceLocation.CODEC.fieldOf("piece").forGetter(Piece::piece),
                Codec.intRange(1, 100).optionalFieldOf("weight", 1).forGetter(Piece::weight)).apply(i, Piece::new));
    }

    private record File(List<Piece> pieces) {
        static final Codec<File> CODEC = RecordCodecBuilder.create(i -> i.group(
                Piece.CODEC.listOf().fieldOf("pieces").forGetter(File::pieces)).apply(i, File::new));
    }

    /** Pieces already added to a pool object, so a second start of the same server never doubles them. */
    private static final Map<StructureTemplatePool, Set<ResourceLocation>> ADDED = new WeakHashMap<>();

    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        var pools = server.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        int added = 0;
        for (Map.Entry<ResourceLocation, Resource> f : server.getResourceManager().listResources("village_pieces", id -> id.getPath().endsWith(".json")).entrySet()) {
            try (Reader reader = f.getValue().openAsReader()) {
                var parsed = File.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader));
                var file = parsed.resultOrPartial(e -> Feedback.LOGGER.error("Bad village pieces {}: {}", f.getKey(), e));
                if (file.isEmpty())
                    continue;
                for (Piece p : file.get().pieces()) {
                    StructureTemplatePool pool = pools.get(p.pool());
                    if (pool == null || !ADDED.computeIfAbsent(pool, k -> new HashSet<>()).add(p.piece()))
                        continue;
                    StructurePoolElement element = StructurePoolElement.single(p.piece().toString()).apply(StructureTemplatePool.Projection.RIGID);
                    List<Pair<StructurePoolElement, Integer>> raw = new ArrayList<>(pool.rawTemplates);
                    raw.add(Pair.of(element, p.weight()));
                    pool.rawTemplates = raw;
                    for (int i = 0; i < p.weight(); i++)
                        pool.templates.add(element);
                    added++;
                }
            } catch (Exception e) {
                Feedback.LOGGER.error("Could not read village pieces {}", f.getKey(), e);
            }
        }
        Feedback.LOGGER.info("Added {} village pieces to the vanilla pools", added);
    }
}
