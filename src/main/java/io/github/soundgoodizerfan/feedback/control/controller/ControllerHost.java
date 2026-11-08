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
 * and {@link Fittable} call goes through to it, so code that holds a host (the block, the upgrade items, the crowbar) does not care which.
 */
public interface ControllerHost extends Container, Fittable {

    ControllerCore core();

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
        return core().addUpgrade(upgrade);
    }

    @Override
    default void removeUpgrade(UpgradeFitting upgrade) {
        core().removeUpgrade(upgrade);
    }

    default void tickServer() {
        core().tickServer();
    }
}
