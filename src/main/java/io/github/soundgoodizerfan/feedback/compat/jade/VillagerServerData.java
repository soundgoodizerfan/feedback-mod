// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
