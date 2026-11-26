// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

/** Every invented figure the warehouse reads (all safe to retune). */
public final class StoreTuning {

    /** Longest rail run a dock reads, and how high racks stack beside it. */
    public static final int AISLE_MAX_LENGTH = 64;
    public static final int RACK_MAX_HEIGHT = 8;
    public static final int BAYS_PER_CELL = 8;

    /** A pallet or bulk bay on the reference shelf: mass cap, and a pallet's count cap. */
    public static final float BAY_MASS_MU = 1024f;
    public static final int BAY_MAX_ITEMS = 4096;
    /** A parts bin: kinds it holds, the heaviest single piece it takes, and its mass cap on the reference shelf. */
    public static final int BIN_KINDS = 8;
    public static final float BIN_UNIT_MAX_MU = 0.2f;
    public static final float BIN_MASS_MU = 128f;

    /** A cabinet: single pieces it holds and its mass cap on the reference shelf. */
    public static final int CABINET_PIECES = 16;
    public static final float CABINET_MASS_MU = 256f;

    /** Ticks between settling a rack's goods to its climate, and between rereads of an aisle that holds goods which change with time. */
    public static final int CLIMATE_SETTLE_TICKS = 100;
    public static final int DYNAMIC_REFRESH_TICKS = 200;

    /** The shelf yield strength and frame yield strength that give a bay exactly the reference cap, and the span the share may take. */
    public static final float REF_SHELF_YIELD = 250f;
    public static final float REF_FRAME_YIELD = 250f;
    public static final float RATING_MIN = 0.25f;
    public static final float RATING_MAX = 2f;

    /** Condition a shelf loses for each load set down on it, per Tu the cargo is past its rating, and the least it wears down to. */
    public static final float HOT_WEAR_PER_TU = 0.00005f;
    public static final float CONDITION_FLOOR = 0.25f;

    /** Most the carriage carries in one trip, and what each dock buffer holds. */
    public static final float SHUTTLE_MASS_MU = 256f;
    public static final float DOCK_BUFFER_MU = 128f;

    /** Cells a tick per RPM at the shuttle head; a cell climbed counts this many cells run. */
    public static final float CELLS_PER_TICK_PER_RPM = 1f / 160f;
    public static final float HEIGHT_COST = 2f;
    /** Shaft load of the bare carriage, and drag (Su per RPM) for each Mu carried. */
    public static final float SHUTTLE_BASE_LOAD_SU = 8f;
    public static final float SHUTTLE_DRAG_PER_MU = 0.02f;

    /** Ticks between looks for room after intake found none, and between pushes out of the front face. */
    public static final int RETRY_TICKS = 20;
    public static final int PUSH_TICKS = 8;
    /** Ticks a nearly empty intake waits for more before the carriage sets out, and between reads of a partly unloaded aisle. */
    public static final int SETTLE_TICKS = 40;
    public static final int PARTIAL_READ_TICKS = 100;
    /** Docks a catalogue desk reads with the slowest and the fastest circuit, and the LV current it draws. */
    public static final int DESK_MIN_DOCKS = 4;
    public static final int DESK_MAX_DOCKS = 32;
    public static final float DESK_AMPS = 0.5f;
    /** The share of its wanted work below which the desk is dark. */
    public static final float DESK_MIN_FEED = 0.5f;
    /** Longest name a dock takes. */
    public static final int DOCK_NAME_MAX = 32;
    /** Loads an optical head or a screen reads on a dock before it wants wiping. */
    public static final int HEAD_FOUL_LOADS = 64;
    /** Most retrievals a dock queues. */
    public static final int MAX_REQUESTS = 32;

    private StoreTuning() {
    }
}
