// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import java.util.List;

/**
 * One thing a held weapon lets its wielder switch with the single "Gun option" key (a fire selector, a bipod's legs, a lit reticle's
 * shutter, a flamethrower's pilot flame). The item declares them ({@link GunMechanism#switches}) and carries the state itself; the key
 * only asks the server to set one. Not a {@link GunOption}, which is a part fitted to the gun.
 *
 * @param id     name, also the tail of the lang key {@code message.feedback.option.<id>} and of the wheel's heading {@code gui.feedback.gun_option.<id>}.
 * @param states the state names in order (two for an on/off switch, three for a fire selector), each the tail of {@code message.feedback.state.<name>}.
 * @param state  the index into {@code states} the switch stands at now.
 */
public record GunSwitch(String id, List<String> states, int state) {

    /** An on/off switch: {@code off} then {@code on}. */
    public static GunSwitch twoState(String id, String off, String on, boolean isOn) {
        return new GunSwitch(id, List.of(off, on), isOn ? 1 : 0);
    }

    /** The state after the current one, wrapping. */
    public int next() {
        return (state + 1) % states.size();
    }
}
