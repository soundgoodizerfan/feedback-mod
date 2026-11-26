// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import io.github.soundgoodizerfan.feedback.logistics.LogisticsTuning;
import io.github.soundgoodizerfan.feedback.logistics.belt.BeltModule;

import net.minecraft.nbt.CompoundTag;

/** A belt module fitted to a dock as a reader head, and how fouled it has become. */
public final class Head {

    public final BeltModule module;
    /** Loads it has read since it was last wiped; only an optical head and a screen mind. */
    public int used;

    public Head(BeltModule module) {
        this.module = module;
    }

    public boolean dirty() {
        return module == BeltModule.OPTICAL && used >= StoreTuning.HEAD_FOUL_LOADS;
    }

    /** A blinded screen records nothing. */
    public boolean fouled() {
        return module == BeltModule.SCREEN && used >= StoreTuning.HEAD_FOUL_LOADS;
    }

    public boolean wipeable() {
        return dirty() || fouled();
    }

    public float wantedAmps() {
        return module.powered ? LogisticsTuning.POWERED_AMPS : 0f;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Module", module.ordinal());
        tag.putInt("Used", used);
        return tag;
    }

    @org.jetbrains.annotations.Nullable
    public static Head load(CompoundTag tag) {
        BeltModule m = BeltModule.byOrdinal(tag.getInt("Module"));
        if (m == null || m.dockProperty() == null)
            return null;
        Head head = new Head(m);
        head.used = tag.getInt("Used");
        return head;
    }
}
