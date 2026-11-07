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
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;

import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;

/** The viewing player's reputation with the villager looked at: the gossip lives on the server. */
public class VillagerServerData implements IServerDataProvider<EntityAccessor> {

    public static final VillagerServerData INSTANCE = new VillagerServerData();

    static final String STANDING = "VillageStanding", GUILD_PROFESSION = "GuildProfession", GUILD_SIZE = "GuildSize", GUILD_TIER = "GuildTier";

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (accessor.getEntity() instanceof Villager v) {
            data.putInt(STANDING, v.getPlayerReputation(accessor.getPlayer()));
            var guild = io.github.soundgoodizerfan.feedback.village.Guilds.of(v);
            if (guild != null) {
                data.putString(GUILD_PROFESSION, guild.profession().getPath());
                data.putInt(GUILD_SIZE, guild.members().size());
                data.putInt(GUILD_TIER, guild.tier());
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("villager");
    }
}
