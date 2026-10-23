# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repository is

This is the repository for **Feedback**, a Minecraft technology mod. It is a design-first project: the mod was specified before a line of it was written, and the specification documents live here alongside the source.

The design documents remain the spine of the project; the source is now being written against them.

**`docs/design_digest.md` is required reading before any implementation edit** (hard rules, conventions). The `feedback-dev` mod (`.claude/skills/feedback-dev/`, a function-hooks plugin the project skills folder auto-loads) blocks `Edit`/`Write`/`MultiEdit` under `src/main/` until this loop -- main thread or each subagent on its own -- has `Read` it. If blocked, read it, retry; don't route around. `feedback_philosophy.md` and `feedback_mechanics.md` are the full design: read the section a task touches.

The same mod compiles once per turn: every `.java` edit marks its checkout, and at Stop/SubagentStop `./gradlew compileJava` runs there; failure blocks the stop with errors. It also shows compile / game test / deploy freshness on the status line, the gate state above the prompt, and `/open-work` (`TODO.md` open items and `[OPEN]` count per document). Change it with the `plugin-authoring` skill; `claude plugin test .claude/skills/feedback-dev` runs its tests.

**Platform: NeoForge on Minecraft 1.21.1**, built with **ModDevGradle**. 1.21.1 was chosen over a newer version deliberately — the newer backend's data-driven item work suits a mod built on "properties and tags matter more than the id," but 1.21.1 is where the players are.

**Breaking changes are fine** (user ruling, 2026-10-20): the mod is not in survival play anywhere, so no save, world or pack depends on it. Rename or restructure registry ids, block states, NBT and data components freely; do not write migrations, aliases, data fixers or compatibility shims. Datapack and format changes need no deprecation path either.

**Never push unless explicitly asked by the user. When pushing, use `gh`, not git.** Plain `git push` has no credentials here and fails (`could not read Username for 'https://github.com'`); `gh` is logged in, so push through its credential helper: `git -c credential.helper= -c credential.helper='!gh auth git-credential' push origin main`. Otherwise commit locally and stop there.

## Build and run

```bash
./gradlew build          # jar -> build/libs/feedback-<version>.jar
./gradlew runClient      # dev client
./gradlew runData        # datagen -> src/generated/resources
./gradlew runGameTestServer
./build-and-deploy.sh    # build, then deploy into the feedback-test-pack packwiz repo
```

`build-and-deploy.sh` copies the jar into `../feedback-test-pack/mods/` (a packwiz repo), first clearing its own stale `feedback-*.jar` there, because two jars with one mod id is a launch crash -- it deletes nothing else in that folder. It then pins the runtime deps that are not jarJar'd in (Sable, Curios, JEI, Jade) via `packwiz mr add`, looked up on Modrinth by the exact filename `gradle.properties` compiles against, and runs `packwiz refresh`. Needs network for the Modrinth lookup.

**Verify by asking the user to play it, not by game tests.** `runGameTestServer` is heavy on usage, slow, and can hang at shutdown. Default after a change: `compileJava`, `./build-and-deploy.sh`, then hand the user a short in-game checklist (what to do, what they should see). Don't run the suite by reflex and don't write a game test per feature; run or add one only when the user asks (user ruling 2026-10-20: "if you want a test, ill test it") -- for a load-bearing mechanic only a test can see, offer one, don't write it. A plan or `TODO.md` step naming a game test does not count as asking.

**Always run `./build-and-deploy.sh` once a change is confirmed working** (build passes, tests/checks green) — so the user's instance always holds the latest verified jar. Don't run it on unconfirmed or half-finished work.

Java 21 is the machine default and the correct toolchain; no `JAVA_HOME` override, unlike the sibling ForgeGradle repos.

Versions live in `gradle.properties` — NeoForge `21.1.250`, ModDevGradle `2.0.147`, Parchment `2024.11.17`, Gradle `9.2.1` via the wrapper.

## Where things are

- `docs/design_digest.md` -- core pitch, hard design rules, conventions. **Read before any `src/main/` edit** (gate enforces it); the dev gate needs only this file now. Full design: `feedback_philosophy.md` (authoritative, why) and `feedback_mechanics.md` (how) -- read the section a task touches, grep headings first.
- `docs/source_layout.md` -- package-by-package source map, thermal and rotation model paragraphs, and the table of every design document. Read when locating code.
- `TODO.md` -- live checklist of open work only. Update it, not this file, when status changes.

## Prior art

Before building anything non-trivial, ask the user whether an open-source mod already solves it. Check licence first (`LICENSING.md`, `THIRD-PARTY-LICENSES.md`; assets always no). Clone outside the repo on the 1.21.1 branch, take the design and write the code, record taken and rejected at the site. Procedure: `study-prior-art` skill.

## New ideas

Brainstorms get a Minecraft lens (what the world changes about it, compendium-style). Procedure: `feedback-brainstorm` skill. Approved ideas go in `TODO.md`, not code.

## Response style

Reply in **caveman ultra** at all times in this repo: no articles, filler, pleasantries or hedging; fragments fine; no arrows, no invented abbreviations. Code, commit messages, PR text and security/irreversible-action warnings are written normally.

