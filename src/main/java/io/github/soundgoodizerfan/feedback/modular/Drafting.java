// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.Set;

/**
 * Which kind of drawing table a Blueprint Table is. Each table draws the families its descriptor lists; the menu, the screen and the
 * list of drawable types all ask {@link #draws}, so a new table is one constant and one block. {@link #ANY} is the table inside a
 * program, which draws everything. What a table can draw says nothing about what the player may: a type that must be learned still
 * needs a plan ({@link ModularBlueprints}).
 */
public enum Drafting {
    /** Tools, weapons, armour, guns and worn gear: everything not listed for the engineering table. */
    DRAFTING(Set.of()),
    /** Machines and what they are built from: engines, gearboxes, propellers, motors, cells, belts, and the modules of a powered suit. */
    ENGINEERING(Set.of(ModularType.Family.ENGINE, ModularType.Family.GEARBOX, ModularType.Family.PROPELLER, ModularType.Family.MACHINE,
            ModularType.Family.CELL, ModularType.Family.BELT, ModularType.Family.SUIT_MODULE)),
    ANY(Set.of());

    private final Set<ModularType.Family> engineering;

    Drafting(Set<ModularType.Family> engineering) {
        this.engineering = engineering;
    }

    public boolean draws(ModularType type) {
        return switch (this) {
            case ANY -> true;
            case ENGINEERING -> engineering.contains(type.family());
            case DRAFTING -> !ENGINEERING.engineering.contains(type.family());
        };
    }

    public static Drafting of(int ordinal) {
        Drafting[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : ANY;
    }
}
