// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.computer;

import io.github.soundgoodizerfan.feedback.control.controller.ControllerCore;
import io.github.soundgoodizerfan.feedback.control.controller.ControllerHost;
import io.github.soundgoodizerfan.feedback.control.programmer.EmbeddedProgrammer;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerBlockEntity;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerProvider;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Computer: an Electric Programmer and an Electric Controller in one block. It has two slots. The Controller slot (a medium put in by
 * hand) holds the program it runs; the Programmer's own slot (opened with an empty hand) is where a program is written, printed or, on a
 * Floppy Disk or USB Drive, erased and written again. Move the finished medium from one to the other. Powered as an electrical sink; the
 * Programmer works without power, the Controller only runs while the line delivers.
 */
public class ComputerBlockEntity extends ElectricNode implements ControllerHost, ProgrammerProvider {

    private final ControllerCore core = new ControllerCore(this, this::powered, stack -> stack.is(FTags.PUNCH_CARDS));
    private final EmbeddedProgrammer programmer = new EmbeddedProgrammer(this);

    public ComputerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.COMPUTER.get(), pos, state);
    }

    @Override
    public ControllerCore core() {
        return core;
    }

    @Override
    public ProgrammerBlockEntity programmer() {
        return programmer;
    }

    private boolean powered() {
        return ElectricRules.rate(getVolts(), getAmps(), getWantedAmps(), ratedBand()) >= 0.5f;
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        programmer.setLevel(level);
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return FTuning.COMPUTER_AMPS;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.MV;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        core.save(tag, registries);
        CompoundTag editor = new CompoundTag();
        programmer.saveEmbedded(editor, registries);
        tag.put("Programmer", editor);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        core.load(tag, registries);
        programmer.loadEmbedded(tag.getCompound("Programmer"), registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
