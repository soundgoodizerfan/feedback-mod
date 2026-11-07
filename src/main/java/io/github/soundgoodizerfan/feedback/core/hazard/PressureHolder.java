// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.hazard;

import io.github.soundgoodizerfan.feedback.core.unit.Pu;

/**
 * A block that holds air or steam under pressure: the Pressure Vessel and the Boiler. It exists
 * so a pressure instrument can read either without naming one, the way {@code ThermalBody} lets
 * a thermometer read any hot block.
 */
public interface PressureHolder {

    Pu getPressure();
}
