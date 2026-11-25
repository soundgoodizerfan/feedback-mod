// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.controller;

import java.util.List;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.control.program.ControllerLimits;
import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.SidedFitting;
import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A block entity that runs a program: the mechanical Controller, the Electric Controller and the Computer. They differ in what powers
 * them and which media they take; the card slot, the upgrades and the evaluation are one {@link ControllerCore}. Every {@link Container}
 * and {@link Fittable} call goes through to it, so code that holds a host (the block, the upgrade items, the pry bar) does not care which.
 */
public interface ControllerHost extends Container, Fittable, io.github.soundgoodizerfan.feedback.control.data.Amplifying, io.github.soundgoodizerfan.feedback.control.ChannelSource,
        io.github.soundgoodizerfan.feedback.control.Steppable, io.github.soundgoodizerfan.feedback.control.RemoteChannels {

    ControllerCore core();

    /** A host's circuit amplifies a data link that ends at it ({@code control/data/Amplifying}). */
    @Override
    default float amplifierGain() {
        return core().gain();
    }

    // --- remote variables: another controller reads and sets this one's declared variables through its ordinary Sensor and Actuator cards ---

    /** Channel {@code n} reads the {@code n}th declared variable. */
    @Override
    default float read(int channel) {
        return core().variableValue(channel);
    }

    @Override
    default String[] senseKeys() {
        var all = core().declared();
        String[] names = new String[all.size()];
        for (int i = 0; i < names.length; i++)
            names[i] = all.get(i).name();
        return names;
    }

    /** Channel 0 is no variable (the plain switch moves nothing here); channel {@code n} is the {@code n-1}th declared variable. */
    @Override
    default String[] channelKeys() {
        String[] names = senseKeys();
        String[] keys = new String[names.length + 1];
        keys[0] = "feedback.control.remote.none";
        System.arraycopy(names, 0, keys, 1, names.length);
        return keys;
    }

    @Override
    default void pulse(int channel, boolean on) {
        core().writeVariable(channel - 1, on);
    }

    @Override
    default void setEngaged(boolean engaged) {
    }

    @Override
    default boolean isEngaged() {
        return false;
    }

    default ControllerLimits limits() {
        return core().limits();
    }

    default Optional<String> refusalFor(ItemStack stack) {
        return core().refusalFor(stack);
    }

    @Override
    default int getContainerSize() {
        return core().getContainerSize();
    }

    @Override
    default boolean isEmpty() {
        return core().isEmpty();
    }

    @Override
    default ItemStack getItem(int slot) {
        return core().getItem(slot);
    }

    @Override
    default ItemStack removeItem(int slot, int count) {
        return core().removeItem(slot, count);
    }

    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        return core().removeItemNoUpdate(slot);
    }

    @Override
    default void setItem(int slot, ItemStack stack) {
        core().setItem(slot, stack);
    }

    @Override
    default boolean canPlaceItem(int slot, ItemStack stack) {
        return core().canPlaceItem(slot, stack);
    }

    @Override
    default boolean stillValid(Player player) {
        return core().stillValid(player);
    }

    @Override
    default void clearContent() {
        core().clearContent();
    }

    @Nullable
    @Override
    default SidedFitting getSidedFitting(Direction side) {
        return null;
    }

    @Override
    default void setSidedFitting(Direction side, @Nullable SidedFitting fitting) {
    }

    @Override
    default boolean canMount(SidedFitting fitting, Direction side) {
        return false;
    }

    @Override
    default boolean canAdd(UpgradeFitting upgrade) {
        return core().canAdd(upgrade);
    }

    @Override
    default boolean canRemove(UpgradeFitting upgrade) {
        return core().canRemove(upgrade);
    }

    @Override
    default List<UpgradeFitting> getUpgrades() {
        return core().getUpgrades();
    }

    @Override
    default boolean addUpgrade(UpgradeFitting upgrade) {
        boolean added = core().addUpgrade(upgrade);
        if (added)
            syncUpgrades();
        return added;
    }

    /** Tells the clients tracking this host its upgrades changed, so the fitting renderer draws what is on it now. */
    default void syncUpgrades() {
        if (this instanceof net.minecraft.world.level.block.entity.BlockEntity host && host.getLevel() != null && !host.getLevel().isClientSide) {
            host.setChanged();
            host.getLevel().sendBlockUpdated(host.getBlockPos(), host.getBlockState(), host.getBlockState(), 3);
        }
    }

    @Override
    default void removeUpgrade(UpgradeFitting upgrade) {
        core().removeUpgrade(upgrade);
        syncUpgrades();
    }

    default void tickServer() {
        core().tickServer();
    }
}
