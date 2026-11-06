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
package io.github.soundgoodizerfan.feedback.logistics.belt;

import java.util.List;

import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.logistics.LogisticsTuning;
import io.github.soundgoodizerfan.feedback.logistics.sort.Band;
import io.github.soundgoodizerfan.feedback.registry.FLogistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A run's drive. Its shaft network's {@code Rpm} times a roller constant is the belt speed, and its
 * {@code Su} load is a bare roller's plus a term per Mu of cargo on the run: a loaded belt is heavy to
 * turn, and an undersized shaft slows the whole run (the rotation ledger does that); the belt never fails from load.
 */
public class BeltHeadBlockEntity extends RotationNode implements CargoSave.Holder, ModuleHost {

    private List<Object[]> pending = List.of();
    private BeltModule module;
    private Direction face = Direction.NORTH;
    private boolean engaged = true, flip;

    public BeltHeadBlockEntity(BlockPos pos, BlockState state) {
        super(FLogistics.HEAD_ENTITY.get(), pos, state);
    }

    @Override
    public Su getLoadSu() {
        BeltRun run = level == null ? null : BeltRuns.of(level).runAt(worldPosition);
        float carried = run == null ? 0f : run.mass() * run.length();
        return new Su(LogisticsTuning.BELT_BASE_LOAD_SU + LogisticsTuning.BELT_LOAD_SU_PER_MU * carried);
    }

    // --- module (end-of-run kinds only, BeltModule#onHead) --------------------------------------

    @Override
    public BeltModule module() {
        return module;
    }

    @Override
    public Direction face() {
        return face;
    }

    @Override
    public Band band() {
        return Band.BRIGHT;
    }

    @Override
    public int aperture() {
        return 0;
    }

    @Override
    public boolean engaged() {
        return engaged;
    }

    @Override
    public boolean live() {
        return true;
    }

    @Override
    public boolean flip() {
        flip = !flip;
        return !flip;
    }

    @Override
    public void soil() {
    }

    @Override
    public boolean dirty() {
        return false;
    }

    @Override
    public void foul() {
    }

    @Override
    public boolean fouled() {
        return false;
    }

    @Override
    public void pass(long time) {
    }

    @Override
    public float jetPressure() {
        return 0f;
    }

    @Override
    public void install(BeltModule m, Direction side) {
        module = m;
        face = side;
        engaged = true;
        changedModule();
    }

    private void changedModule() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            BeltRuns.of(level).queue(worldPosition);
        }
    }

    public java.util.Optional<ItemStack> moduleDrop() {
        return module == null ? java.util.Optional.empty() : java.util.Optional.of(new ItemStack(FLogistics.MODULE_ITEMS.get(module).get()));
    }

    /** Sneaking with an empty hand pulls the module; a side click toggles a gate or diverter. */
    @Override
    public boolean handUse(Player player, BlockHitResult hit) {
        if (level == null || module == null)
            return false;
        if (player.isShiftKeyDown()) {
            moduleDrop().ifPresent(s -> {
                if (!player.getInventory().add(s))
                    player.drop(s, false);
            });
            module = null;
            changedModule();
            return true;
        }
        if (module != BeltModule.GATE && module != BeltModule.DIVERTER)
            return false;
        engaged = !engaged;
        changedModule();
        BeltRuns.of(level).wake(worldPosition);
        if (player instanceof ServerPlayer sp)
            io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.send(sp,
                    Component.translatable(engaged ? "message.feedback.belt.engaged" : "message.feedback.belt.released"), 40);
        return true;
    }

    @Override
    public List<Object[]> takePending() {
        List<Object[]> out = pending;
        pending = List.of();
        return out;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide)
            BeltRuns.of(level).queue(worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide)
            BeltRuns.of(level).queue(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        List<Object[]> over = level == null || level.isClientSide ? pending : BeltRuns.of(level).savedOver(worldPosition);
        if (over.isEmpty())
            over = pending;
        if (!over.isEmpty())
            tag.put("Cargo", CargoSave.write(over, registries, level == null ? 0L : level.getGameTime()));
        writeModule(tag);
    }

    private void writeModule(CompoundTag tag) {
        if (module != null) {
            tag.putInt("Module", module.ordinal());
            tag.putInt("Face", face.get3DDataValue());
            tag.putBoolean("Engaged", engaged);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Cargo"))
            pending = CargoSave.read(tag.getList("Cargo", 10), registries);
        module = tag.contains("Module") ? BeltModule.byOrdinal(tag.getInt("Module")) : null;
        face = Direction.from3DDataValue(tag.getInt("Face"));
        engaged = !tag.contains("Engaged") || tag.getBoolean("Engaged");
    }
}
