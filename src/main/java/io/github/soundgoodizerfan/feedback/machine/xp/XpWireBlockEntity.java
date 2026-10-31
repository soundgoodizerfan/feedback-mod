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
package io.github.soundgoodizerfan.feedback.machine.xp;

import io.github.soundgoodizerfan.feedback.core.xp.XpNetwork;
import io.github.soundgoodizerfan.feedback.core.xp.XpNetworks;
import io.github.soundgoodizerfan.feedback.core.xp.XpNode;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** One length of XP wire. Its material (on the item as {@link MadeOf}) sets conductance and leak. */
public class XpWireBlockEntity extends BlockEntity implements XpNode {

    @Nullable
    private ResourceLocation material;
    private float conductance;
    private float leak;
    @Nullable
    private XpNetwork network;
    private boolean unloaded;

    public XpWireBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.XP_WIRE.get(), pos, state);
    }

    @Nullable
    public ResourceLocation material() {
        return material;
    }

    private void figures() {
        Material m = material == null ? null : MaterialTable.get().materials().get(material);
        var xp = m == null ? null : m.xpStore().orElse(null);
        conductance = xp == null ? 0f : xp.conductance();
        leak = xp == null ? 0f : xp.leak();
    }

    @Override
    public float conductance() {
        if (conductance == 0f && material != null)
            figures();
        return conductance;
    }

    @Override
    public float leak() {
        conductance();
        return leak;
    }

    @Nullable
    @Override
    public XpNetwork network() {
        return network;
    }

    @Override
    public void network(@Nullable XpNetwork network) {
        this.network = network;
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        MadeOf made = input.get(FDataComponents.MADE_OF.get());
        if (made != null)
            material = made.material();
        conductance = 0f;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (material != null)
            components.set(FDataComponents.MADE_OF.get(), new MadeOf(material, "wire", 1));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (material != null)
            tag.putString("Material", material.toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        material = tag.contains("Material") ? ResourceLocation.parse(tag.getString("Material")) : null;
        conductance = 0f;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide)
            XpNetworks.of(level).queue(worldPosition);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        unloaded = true;
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            if (unloaded)
                XpNetworks.of(level).unloaded(this);
            else
                XpNetworks.of(level).removed(this);
        }
        super.setRemoved();
    }
}
