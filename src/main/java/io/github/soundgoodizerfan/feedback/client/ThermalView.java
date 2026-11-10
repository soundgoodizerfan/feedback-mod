// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.util.List;

import io.github.soundgoodizerfan.feedback.net.ThermalFeedPayload.Entry;

public final class ThermalView {
    private static volatile List<Entry> entries = List.of();

    private ThermalView() {
    }

    public static void apply(List<Entry> list) {
        entries = list;
    }

    public static List<Entry> entries() {
        return entries;
    }
}
