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
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Machete (spec 5.4): {@code unarmoured_mult} against a target wearing no armour, only
 * {@code armoured_mult} against one that is; and breaking a leaf, crop or tree-replaceable plant
 * clears the plants within {@code sweep} blocks too. The cost is the tool's wear,
 * {@code wear_per_block} a block swept, and the poor showing against armour.
 */
public final class Hack implements ModularTrait {

    private static boolean sweeping;

    @Override
    public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
        event.setAmount(event.getAmount() * (event.getEntity().getArmorValue() == 0
                ? params.get("unarmoured_mult", 1.4f) : params.get("armoured_mult", 0.8f)));
    }

    private static boolean plant(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.CROPS) || state.is(BlockTags.REPLACEABLE_BY_TREES);
    }

    @Override
    public void onBlockBreak(BlockEvent.BreakEvent event, ItemStack weapon, Params params) {
        if (sweeping || event.isCanceled() || !plant(event.getState()) || !(event.getLevel() instanceof Level level))
            return;
        int r = Math.round(params.get("sweep", 2f));
        int broken = 0;
        sweeping = true;
        try {
            for (BlockPos p : BlockPos.betweenClosed(event.getPos().offset(-r, -r, -r), event.getPos().offset(r, r, r))) {
                if (!p.equals(event.getPos()) && plant(level.getBlockState(p)) && level.destroyBlock(p, true, event.getPlayer()))
                    broken++;
            }
        } finally {
            sweeping = false;
        }
        int wear = (int) Math.ceil(broken * params.get("wear_per_block", 1f / 3f));
        if (wear > 0)
            weapon.hurtAndBreak(ModularStacks.cap(weapon, wear), event.getPlayer(), EquipmentSlot.MAINHAND);
    }
}
