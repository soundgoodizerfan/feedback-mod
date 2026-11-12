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

    private final ControllerCore core = new ControllerCore(this, this::powered, stack -> stack.is(FTags.PUNCH_CARDS) && !ComputerApps.isApp(stack));
    private final EmbeddedProgrammer programmer = new EmbeddedProgrammer(this);
    private final ComputerBays bays = new ComputerBays(this::setChanged);
    /** The bay the medium in the editor came from, or -1. */
    private int editorDrive = -1;
    /** Who has it open in the editor; not saved (a medium left in the editor over a restart goes back to its bay when the next desktop opens). */
    private java.util.UUID editorOwner;
    /** Open desktops; the block entity pushes a fresh state to clients every half second while any is. */
    private int watchers;
    /** The running program's last sensor readings, as the server sent them; for the monitor. */
    private float[] readings = new float[0];
    /** Whether the cable was delivering at the last push; for the desktop's power lamp (the client does not simulate it). */
    private boolean poweredSeen;
    private ComputerSettings settings = ComputerSettings.DEFAULT;
    /** The Notes app's text, the arcade's best scores (longest snake, fastest mine field in seconds, 0 for none). */
    private String notes = "";
    private int snakeBest, minesBest;
    /** The cable's figures at the last push, for the Power app. */
    private float voltsSeen, ampsSeen, wantedSeen;
    private boolean lastPowered;

    /** One console line: the time of day it happened, a message key and its argument. */
    public record LogEntry(long time, String key, String arg) {
    }

    public static final int LOG_MAX = 48, NOTES_MAX = 2000;
    private final java.util.ArrayList<LogEntry> log = new java.util.ArrayList<>();

    public ComputerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.COMPUTER.get(), pos, state);
        core.events(this::log);
    }

    // --- the console, notes, scores, power ----------------------------------------------------

    /** Adds a line to the console. */
    public void log(String key, String arg) {
        if (level == null || level.isClientSide)
            return;
        if (log.size() >= LOG_MAX)
            log.remove(0);
        log.add(new LogEntry(level.getDayTime(), key, arg));
        setChanged();
    }

    /** Whether a disk with this app is in a bay. */
    public boolean hasApp(String id) {
        return ComputerApps.installed(bays, ComputerBays.SIZE, id);
    }

    public java.util.List<LogEntry> logEntries() {
        return log;
    }

    public String notes() {
        return notes;
    }

    /** The Notes text from a client: cut to length and cleaned of control characters other than a newline. */
    public void setNotes(String text) {
        if (!hasApp("notes"))
            return;
        StringBuilder clean = new StringBuilder();
        for (int i = 0; i < text.length() && clean.length() < NOTES_MAX; i++) {
            char c = text.charAt(i);
            if (c == '\n' || c >= 32 && c != 127)
                clean.append(c);
        }
        notes = clean.toString();
        setChanged();
    }

    public int snakeBest() {
        return snakeBest;
    }

    public int minesBest() {
        return minesBest;
    }

    /** A finished game: snake is better higher, mines (seconds) better lower. Clamped; purely a bragging figure. */
    public void score(int game, int value) {
        if (!hasApp("arcade"))
            return;
        if (game == 0 && value > snakeBest)
            snakeBest = Math.min(value, 9999);
        else if (game == 1 && value > 0 && (minesBest == 0 || value < minesBest))
            minesBest = Math.min(value, 99999);
        else
            return;
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public float voltsSeen() {
        return voltsSeen;
    }

    public float ampsSeen() {
        return ampsSeen;
    }

    public float wantedSeen() {
        return wantedSeen;
    }

    /** Copies the program in bay {@code drive} onto a blank soft medium (Floppy, USB) in another bay. */
    public void copy(int drive, ServerPlayer player) {
        if (!copyable(drive) || !hasApp("library"))
            return;
        for (int i = 0; i < ComputerBays.SIZE; i++)
            if (i != drive && blank(bays.getItem(i))) {
                write(drive, i);
                return;
            }
        io.github.soundgoodizerfan.feedback.net.AimPayloads.screen(player, Component.translatable("gui.feedback.computer.no_blank"));
    }

    /** Copies the program in bay {@code source} onto the blank soft medium in bay {@code target}: the Files app's drag, with the Library disk in. */
    public void copyTo(int source, int target, ServerPlayer player) {
        if (!copyable(source) || !hasApp("library") || target < 0 || target >= ComputerBays.SIZE || target == source)
            return;
        if (blank(bays.getItem(target)))
            write(source, target);
        else
            io.github.soundgoodizerfan.feedback.net.AimPayloads.screen(player, Component.translatable("gui.feedback.computer.no_blank_there", String.valueOf((char) ('A' + target))));
    }

    /** Wipes the program from the medium in bay {@code drive}, leaving it blank to be written again. */
    public void erase(int drive, ServerPlayer player) {
        if (drive < 0 || drive >= ComputerBays.SIZE || bays.getItem(drive).isEmpty() || ComputerApps.isApp(bays.getItem(drive)))
            return;
        bays.getItem(drive).set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.PROGRAM_GRAPH,
                io.github.soundgoodizerfan.feedback.control.program.ProgramGraph.EMPTY);
        bays.setChanged();
        log("erase", String.valueOf((char) ('A' + drive)));
    }

    /** Whether bay {@code drive} holds a program medium with something written on it. */
    private boolean copyable(int drive) {
        return drive >= 0 && drive < ComputerBays.SIZE && !bays.getItem(drive).isEmpty() && !ComputerApps.isApp(bays.getItem(drive))
                && !bays.getItem(drive).getOrDefault(io.github.soundgoodizerfan.feedback.registry.FDataComponents.PROGRAM_GRAPH,
                        io.github.soundgoodizerfan.feedback.control.program.ProgramGraph.EMPTY).nodes().isEmpty();
    }

    /** A lone Floppy or USB drive with nothing on it. */
    private static boolean blank(ItemStack target) {
        return target.getCount() == 1 && target.is(FTags.SOFT_CARDS) && !ComputerApps.isApp(target) && target.getOrDefault(
                io.github.soundgoodizerfan.feedback.registry.FDataComponents.PROGRAM_GRAPH,
                io.github.soundgoodizerfan.feedback.control.program.ProgramGraph.EMPTY).nodes().isEmpty();
    }

    private void write(int source, int target) {
        var graph = bays.getItem(source).getOrDefault(io.github.soundgoodizerfan.feedback.registry.FDataComponents.PROGRAM_GRAPH,
                io.github.soundgoodizerfan.feedback.control.program.ProgramGraph.EMPTY);
        ItemStack medium = bays.getItem(target);
        medium.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.PROGRAM_GRAPH, graph);
        // as a printed card: one of a kind
        medium.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 1);
        bays.setChanged();
        log("copy", String.valueOf((char) ('A' + source)));
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

    public ComputerSettings settings() {
        return settings;
    }

    public void setSettings(ComputerSettings settings) {
        this.settings = settings;
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Whether a medium is open in the editor window. */
    public boolean editing() {
        return editorDrive >= 0;
    }

    /** The power state as of the last push to an open desktop. */
    public boolean poweredSeen() {
        return poweredSeen;
    }

    public boolean powered() {
        return ElectricRules.rate(getVolts(), getAmps(), getWantedAmps(), ratedBand()) >= 0.5f;
    }

    /** How many desktops are open on this Computer. */
    public int watching() {
        return watchers;
    }

    public void watch(int delta) {
        watchers = Math.max(0, watchers + delta);
    }

    @Override
    public void tickServer() {
        core.tickServer();
        if (level != null && level.getGameTime() % 20 == 0) {
            boolean now = powered();
            if (now != lastPowered) {
                lastPowered = now;
                log(now ? "power_on" : "power_off", "");
            }
        }
        if (watchers > 0 && level != null && level.getGameTime() % 5 == 0) {
            readings = core.readings();
            poweredSeen = powered();
            voltsSeen = getVolts();
            ampsSeen = getAmps();
            wantedSeen = getWantedAmps();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // --- the desktop's actions --------------------------------------------------------------------

    /** Starts the program in bay {@code drive}: it moves to the running slot, if that is free and the Controller has room for it. */
    public void run(int drive, ServerPlayer player) {
        if (drive < 0 || drive >= ComputerBays.SIZE || !core.getItem(0).isEmpty() || bays.getItem(drive).isEmpty() || ComputerApps.isApp(bays.getItem(drive)))
            return;
        var refusal = core.refusalFor(bays.getItem(drive));
        if (refusal.isPresent()) {
            io.github.soundgoodizerfan.feedback.net.AimPayloads.screen(player, Component.translatable("feedback.controller.card_refused",
                    Component.translatable("feedback.programmer.error." + refusal.get())));
            log("refused", "feedback.programmer.error." + refusal.get());
            return;
        }
        core.setItem(0, bays.removeItemNoUpdate(drive));
        bays.setChanged();
        log("run", String.valueOf((char) ('A' + drive)));
    }

    /** Stops the running program: its medium goes back to a free bay, or to the player. */
    public void stop(ServerPlayer player) {
        ItemStack medium = core.removeItemNoUpdate(0);
        if (!medium.isEmpty()) {
            shelve(medium, player, -1);
            log("stop", "");
        }
    }

    /** Opens the medium in bay {@code drive} in the editor. */
    public void edit(int drive, ServerPlayer player) {
        if (drive < 0 || drive >= ComputerBays.SIZE || bays.getItem(drive).isEmpty() || ComputerApps.isApp(bays.getItem(drive)) || !programmer.getItem(0).isEmpty())
            return;
        programmer.setItem(0, bays.removeItemNoUpdate(drive));
        bays.setChanged();
        editorDrive = drive;
        editorOwner = player.getUUID();
        log("edit", String.valueOf((char) ('A' + drive)));
        programmer.sync();
    }

    /** The editor window was closed (or the desktop was): the medium goes back to its bay. */
    public void closeEditor(ServerPlayer player) {
        if (editorDrive < 0 || editorOwner != null && !editorOwner.equals(player.getUUID()))
            return;
        editorOwner = null;
        ItemStack medium = programmer.removeItemNoUpdate(0);
        if (!medium.isEmpty())
            shelve(medium, player, editorDrive);
        editorDrive = -1;
        programmer.sync();
        setChanged();
    }

    /** Takes the medium in bay {@code drive} into the player's hands. */
    public void eject(int drive, ServerPlayer player) {
        if (drive < 0 || drive >= ComputerBays.SIZE)
            return;
        ItemStack medium = bays.removeItemNoUpdate(drive);
        bays.setChanged();
        if (!medium.isEmpty()) {
            log("eject", String.valueOf((char) ('A' + drive)));
            if (!player.getInventory().add(medium))
                player.drop(medium, false);
        }
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
        tag.putBoolean("PoweredSeen", poweredSeen);
        tag.put("Settings", settings.save());
        tag.putString("Notes", notes);
        tag.putInt("SnakeBest", snakeBest);
        tag.putInt("MinesBest", minesBest);
        tag.putFloat("Volts", voltsSeen);
        tag.putFloat("Amps", ampsSeen);
        tag.putFloat("Wanted", wantedSeen);
        net.minecraft.nbt.ListTag lines = new net.minecraft.nbt.ListTag();
        for (LogEntry e : log) {
            CompoundTag line = new CompoundTag();
            line.putLong("T", e.time());
            line.putString("K", e.key());
            line.putString("A", e.arg());
            lines.add(line);
        }
        tag.put("Log", lines);
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
        poweredSeen = tag.getBoolean("PoweredSeen");
        settings = ComputerSettings.load(tag.getCompound("Settings"));
        notes = tag.getString("Notes");
        snakeBest = tag.getInt("SnakeBest");
        minesBest = tag.getInt("MinesBest");
        voltsSeen = tag.getFloat("Volts");
        ampsSeen = tag.getFloat("Amps");
        wantedSeen = tag.getFloat("Wanted");
        log.clear();
        net.minecraft.nbt.ListTag lines = tag.getList("Log", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < lines.size() && i < LOG_MAX; i++) {
            CompoundTag line = lines.getCompound(i);
            log.add(new LogEntry(line.getLong("T"), line.getString("K"), line.getString("A")));
        }
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
