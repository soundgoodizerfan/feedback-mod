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
package io.github.soundgoodizerfan.feedback.vehicle.engine;

import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The design a placed Engine was built to. It comes off the item when placed and goes back on it
 * when broken (the loot table copies the component), so an engine is never re-designed by moving.
 */
public class EngineBlockEntity extends BlockEntity {

    @Nullable
    private EngineDesign design;

    public EngineBlockEntity(BlockPos pos, BlockState state) {
        super(FVehicles.ENGINE_ENTITY.get(), pos, state);
    }

    @Nullable
    public EngineDesign design() {
        return design;
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        design = input.get(FVehicles.ENGINE_DESIGN.get());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (design != null)
            components.set(FVehicles.ENGINE_DESIGN.get(), design);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (design != null)
            EngineDesign.CODEC.encodeStart(NbtOps.INSTANCE, design).ifSuccess(t -> tag.put("Design", t));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        design = tag.contains("Design") ? EngineDesign.CODEC.parse(NbtOps.INSTANCE, tag.get("Design")).result().orElse(null) : null;
    }
}
