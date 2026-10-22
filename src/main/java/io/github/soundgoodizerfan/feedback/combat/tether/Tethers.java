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
package io.github.soundgoodizerfan.feedback.combat.tether;

import io.github.soundgoodizerfan.feedback.registry.FCombat;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Every {@link Tether} on a living thing, an attachment (see {@link FCombat}). Server-authoritative,
 * synced to tracking clients, never persisted. Immutable: change it through {@link #add} and
 * {@link #remove}, which is what makes the attachment sync.
 *
 * <p>Vulnerability is the <b>max</b> over the tethers, not the product: two barbs in one mob worry
 * the wound no more than one, and one letting go leaves the other's bonus standing.
 */
public record Tethers(List<Tether> all) {

    public static final Tethers NONE = new Tethers(List.of());

    public static final StreamCodec<ByteBuf, Tethers> STREAM_CODEC =
            Tether.STREAM_CODEC.apply(ByteBufCodecs.list()).map(Tethers::new, Tethers::all);

    /** The damage-taken multiplier of the strongest tether, 1 when none holds. */
    public float vulnerability() {
        float v = 1f;
        for (Tether t : all)
            v = Math.max(v, t.vulnerability());
        return v;
    }

    /** Adds {@code tether}, replacing any from the same source. */
    public static void add(LivingEntity entity, Tether tether) {
        List<Tether> next = new ArrayList<>(entity.getData(FCombat.TETHERS).all());
        next.removeIf(t -> t.sourceId().equals(tether.sourceId()));
        next.add(tether);
        entity.setData(FCombat.TETHERS, new Tethers(List.copyOf(next)));
    }

    /** The tether {@code sourceId} made on {@code entity}, or null once it is gone. */
    public static Tether of(LivingEntity entity, UUID sourceId) {
        if (!entity.hasData(FCombat.TETHERS))
            return null;
        for (Tether t : entity.getData(FCombat.TETHERS).all())
            if (t.sourceId().equals(sourceId))
                return t;
        return null;
    }

    /** Drops the tether {@code sourceId} made, if any. */
    public static void remove(LivingEntity entity, UUID sourceId) {
        if (!entity.hasData(FCombat.TETHERS))
            return;
        List<Tether> next = new ArrayList<>(entity.getData(FCombat.TETHERS).all());
        if (next.removeIf(t -> t.sourceId().equals(sourceId)))
            entity.setData(FCombat.TETHERS, new Tethers(List.copyOf(next)));
    }

    /** A blow on a held body is worse: the strongest tether's {@link Tether#vulnerability}, from any source. */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.hasData(FCombat.TETHERS))
            event.setAmount(event.getAmount() * target.getData(FCombat.TETHERS).vulnerability());
    }
}
