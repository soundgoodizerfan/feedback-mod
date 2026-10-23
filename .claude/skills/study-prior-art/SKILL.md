---
name: study-prior-art
description: Study an open-source mod's solution before building a Feedback feature -- ask which mod, check its licence, clone it outside the repo on the 1.21.1 branch, have the prior-art-reader agent read its design, and record what was taken and rejected. Use before implementing anything non-trivial, or when the user names a mod to learn from.
argument-hint: <feature> [mod or repo URL]
---

# study-prior-art

CLAUDE.md "Prior art" as a procedure. Every step is required; the reading is cheapest before the code.

1. **Ask first.** Name the feature in one line and ask the user which open-source mod already solves it, unless `$ARGUMENTS` names one. Never pick the mod yourself.
2. **Licence before reading.** Find the repo's licence (`LICENSE*`, `gradle.properties`, `mods.toml`/`neoforge.mods.toml`). Check it against `THIRD-PARTY-LICENSES.md` and `LICENSING.md`: Feedback is GPL-3.0 code, so permissive, LGPL-3.0, GPL-3.0, MPL-2.0 without the incompatibility notice, and EUPL-1.2 via Article 5 are adaptable. Any other copyleft: stop and tell the user. **Assets are always no, from everyone.** Futureazoo's textures are CC BY-NC-SA and never usable.
3. **Clone outside the repo**, next to the others: `../<Mod>-reference` (check `ls ..` first; it may already be there). Never a dependency.
4. **Match the version.** `git ls-remote --heads <url>`, then check out the branch or tag for Minecraft **1.21.1** (`gradle.properties` `minecraft_version` confirms Feedback's). Only then read. A conclusion drawn from the default branch (often a newer Minecraft) can be false.
5. **Read with the `prior-art-reader` agent.** Give it the clone path, the branch, the feature, and the Feedback files the feature will touch. It returns the design, not code.
6. **Bring the report to the user** before writing code: what the mod does, what fits Feedback's hard rules, what clashes, and a recommendation. The user decides.
7. **Record at the site.** When code lands, the comment where the idea is used says which mod it came from, what was taken and what was deliberately not. Add or extend the mod's row and section in `THIRD-PARTY-LICENSES.md` in the same change -- including when nothing was taken (a design debt is still recorded; a rejection is worth as much as an adoption).
