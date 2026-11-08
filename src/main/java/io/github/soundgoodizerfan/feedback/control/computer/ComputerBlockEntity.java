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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
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
public class ComputerBlockEntity extends ElectricNode implements ControllerHost, ProgrammerProvider, net.minecraft.world.MenuProvider {

    private final ControllerCore core = new ControllerCore(this, this::powered, stack -> stack.is(FTags.PUNCH_CARDS));
    private final EmbeddedProgrammer programmer = new EmbeddedProgrammer(this);
    private final ComputerBays bays = new ComputerBays(this::setChanged);
    /** The bay the medium in the editor came from, or -1. */
    private int editorDrive = -1;
    /** Open desktops; the block entity pushes a fresh state to clients every half second while any is. */
    private int watchers;
    /** The running program's last sensor readings, as the server sent them; for the monitor. */
    private float[] readings = new float[0];

    public ComputerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.COMPUTER.get(), pos, state);
    }

    @Override
    public ControllerCore core() {
        return core;
    }

    public ComputerBays bays() {
        return bays;
    }

    public float[] readings() {
        return readings;
    }

    public boolean powered() {
        return ElectricRules.rate(getVolts(), getAmps(), getWantedAmps(), ratedBand()) >= 0.5f;
    }

    public void watch(int delta) {
        watchers = Math.max(0, watchers + delta);
    }

    @Override
    public void tickServer() {
        core.tickServer();
        if (watchers > 0 && level != null && level.getGameTime() % 10 == 0) {
            readings = core.readings();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // --- the desktop's actions --------------------------------------------------------------------

    /** Starts the program in bay {@code drive}: it moves to the running slot, if that is free and the Controller has room for it. */
    public void run(int drive, ServerPlayer player) {
        if (drive < 0 || drive >= ComputerBays.SIZE || !core.getItem(0).isEmpty() || bays.getItem(drive).isEmpty())
            return;
        var refusal = core.refusalFor(bays.getItem(drive));
        if (refusal.isPresent()) {
            player.displayClientMessage(Component.translatable("feedback.controller.card_refused",
                    Component.translatable("feedback.programmer.error." + refusal.get())), true);
            return;
        }
        core.setItem(0, bays.removeItemNoUpdate(drive));
        bays.setChanged();
    }

    /** Stops the running program: its medium goes back to a free bay, or to the player. */
    public void stop(ServerPlayer player) {
        ItemStack medium = core.removeItemNoUpdate(0);
        if (!medium.isEmpty())
            shelve(medium, player, -1);
    }

    /** Opens the medium in bay {@code drive} in the editor. */
    public void edit(int drive, ServerPlayer player) {
        if (drive < 0 || drive >= ComputerBays.SIZE || bays.getItem(drive).isEmpty() || !programmer.getItem(0).isEmpty())
            return;
        programmer.setItem(0, bays.removeItemNoUpdate(drive));
        bays.setChanged();
        editorDrive = drive;
        programmer.onEditorClosed(this::editorClosed);
        programmer.sync();
        player.openMenu(programmer, worldPosition);
    }

    /** Takes the medium in bay {@code drive} into the player's hands. */
    public void eject(int drive, ServerPlayer player) {
        if (drive < 0 || drive >= ComputerBays.SIZE)
            return;
        ItemStack medium = bays.removeItemNoUpdate(drive);
        bays.setChanged();
        if (!medium.isEmpty() && !player.getInventory().add(medium))
            player.drop(medium, false);
    }

    private void editorClosed(ServerPlayer player) {
        ItemStack medium = programmer.removeItemNoUpdate(0);
        if (!medium.isEmpty())
            shelve(medium, player, editorDrive);
        editorDrive = -1;
        programmer.sync();
        if (player.connection != null && player.getServer() != null)
            player.getServer().execute(() -> {
                if (!isRemoved() && player.containerMenu == player.inventoryMenu)
                    player.openMenu(this, worldPosition);
            });
    }

    /** Puts a medium in bay {@code preferred} if that is free, else the first free bay, else gives it to the player. */
    private void shelve(ItemStack medium, ServerPlayer player, int preferred) {
        if (preferred >= 0 && bays.getItem(preferred).isEmpty())
            bays.setItem(preferred, medium);
        else {
            for (int i = 0; i < ComputerBays.SIZE; i++)
                if (bays.getItem(i).isEmpty()) {
                    bays.setItem(i, medium);
                    return;
                }
            if (!player.getInventory().add(medium))
                player.drop(medium, false);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.feedback.computer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ComputerMenu(id, inventory, worldPosition, this);
    }

    @Override
    public ProgrammerBlockEntity programmer() {
        return programmer;
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
        net.minecraft.world.ContainerHelper.saveAllItems(tag, bays.getItems(), true, registries);
        tag.putInt("EditorDrive", editorDrive);
        net.minecraft.nbt.ListTag seen = new net.minecraft.nbt.ListTag();
        for (float r : readings)
            seen.add(net.minecraft.nbt.FloatTag.valueOf(r));
        tag.put("Readings", seen);
        CompoundTag editor = new CompoundTag();
        programmer.saveEmbedded(editor, registries);
        tag.put("Programmer", editor);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        core.load(tag, registries);
        bays.getItems().clear();
        net.minecraft.world.ContainerHelper.loadAllItems(tag, bays.getItems(), registries);
        editorDrive = tag.contains("EditorDrive") ? tag.getInt("EditorDrive") : -1;
        net.minecraft.nbt.ListTag seen = tag.getList("Readings", net.minecraft.nbt.Tag.TAG_FLOAT);
        readings = new float[seen.size()];
        for (int i = 0; i < readings.length; i++)
            readings[i] = seen.getFloat(i);
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
