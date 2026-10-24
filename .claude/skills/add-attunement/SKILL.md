---
name: add-attunement
description: Add a new attunement (an enchant effect) or a new kind to Feedback's enchanting system. Use whenever the user wants a new enchant, a new behaviour for an existing one, a new mirror side, or to change what an effect does on a part role. The system is delicate; read this before touching any attunement file.
argument-hint: <effect or behaviour> [role(s)]
---

# add-attunement

An attunement is an enchant that lives on a **part**, not a tool. It is made at the Enchanting Table (`EnchantMenu`, `EnchantGame`: a hysteresis minigame) and stored as `attuned.<effectId> = level` in the part's `MadeOf.dims`, **signed**: a negative level is the effect's mirror side. Full design: `docs/feedback_enchant_roster.md` (the 12 effects, the novel four, section 3a build status) and `docs/feedback_enchanting_spec.md` (the minigame). Read the roster section for the effect you touch. `docs/design_digest.md` is required before any `src/main` edit.

## The model, in six rules

1. **An effect is a coupling; the role picks what it does.** One row per effect: `src/main/resources/data/feedback/attunement/<id>.json`, `{target, band, roles, mirror}`. `target` (0.05..1) is where the polarization must land, `band` its half-width (narrower = stronger). `roles` and `mirror` map a part role name to `{kind, flaw}`. A role with no entry does nothing. Never key on a tool or material id (digest rule 1).
2. **A mirror is the same coupling reversed, not offence versus defence.** Repel/attract, push/pull, emit/absorb, couple/decouple. A weapon and the armour made for the same job are the SAME side, split only by role (Bane on a blade and its protection on a plate are both +; the mirror is Kin, attraction to the undead). Before adding a mirror ask: is this the physical opposite, or just the defensive twin? If the latter, put it on the + side under another role. A mirror may be a real trade (Kin: stealth for danger). Effects with no sensible opposite have no `mirror`.
3. **Where P lands picks the side.** The gauge shows the band at `+target` and, if `mirror` is non-empty, at `-target`. The level is stored signed. Every reader takes the magnitude and resolves the behaviour from the sign through `Attunements.Effect.on(role, negative)`. `MaterialPerks.attunements(stack)` already does this: it returns `Attunement(kind, level>=1, material)` per attuned part, the side already in `kind`. Handlers never see the sign.
4. **A role takes ONE kind per polarity.** If you want two behaviours on a role you need two effects or a kind that does both. Armour roles are only `plate`, `lining`, `studs` (no sole, no glove): a boot's sole is its plate or lining, so a kind that must act only on boots checks the equipment slot in its handler. Role names per tool type are in `data/feedback/modular_type/*.json`; guns use letters, skip them.
5. **`flaw` is how an effect costs or buys toughness.** Added per level at bake (`Attunements.flawShift`): positive brittle, negative tougher. Use it for a trade; never invent a separate toughness stat.
6. **Bad pairings come from material figures** (hardness, conductivity, flame temperature, `curie_temperature`, `BodyMass`), never from authoring or an id. Check the roster's interaction line for the effect: an interaction must fall out of numbers the mod already has.

## Adding a behaviour (a new `kind`)

Files you touch, and only these:

1. **Row**: add the role to `roles` or `mirror` in the effect's JSON. New effect = new file (filename is the id).
2. **Kind class**: `modular/attunement/<Effect>Kinds.java` (one per effect, already wired: `AttunementKinds`' static block calls each `register()`). Register shared moments there with `AttunementKinds.onStrike` (the wielder's blow lands; event target is the victim), `onStruck` (the wearer was hit by a living attacker; the wearer is not passed), `onFlame` (heat the part burns its holder at). For anything else, subscribe yourself: `@EventBusSubscriber(modid = Feedback.MOD_ID)` on the class (make it `public final`), `@SubscribeEvent public static`, and read `MaterialPerks.attunements(stack)` filtered by `kind` (held tool: the stack; armour: `getArmorSlots()`; sum levels across parts). `TideKinds.level(stack, kind)` is the existing summing helper. Never edit `AttunementKinds`, `Attunements`, `MaterialPerks`, `EnchantGame` for a new kind; they are the shared core.
3. **Constants**: `private static final` at the top of the Kinds class, each commented `[OPEN] until played`. (They are not in `ModularTuning`, deliberately, so parallel work never collides.)
4. **Lang** (`assets/feedback/lang/en_us.json`): per effect `attunement.feedback.<id>` and, if it has a mirror, `attunement.feedback.<id>.mirror`. Per kind, BOTH `tooltip.feedback.attuned.<kind>` (takes the level as `%s`, shown on a finished tool) and `tooltip.feedback.attuned.desc.<kind>` (no argument, a lowercase fragment ending in a full stop; the effect button and the selection window show it as "On a blade/grip: <desc>"). A missing desc key shows its raw key in the tooltip. The tooltip groups roles with the same behaviour and says "On anything:" / "On anything but a plate:" itself.
5. **Verify**, from the repo root:
   - `./gradlew compileJava -q` (no game tests; ask the user to play it).
   - Every kind has both tooltip keys, every effect its name and mirror name (grep the lang file for each `"kind"` in the rows), the lang file parses as JSON, each row parses.
6. **Docs**: update the effect's section and the 3a build status in `docs/feedback_enchant_roster.md`, and the `TODO.md` roster line if scope changed. Hand the user an in-game checklist; deploy (`./build-and-deploy.sh`) only after they confirm.

## Pitfalls that have bitten

- **Compile can pass and a role do nothing**: the role name must exist in the type (armour has no `sole`). Check against the modular_type files.
- **Cancelled events must return what they took**: `ItemTossEvent` cancel does not restore the stack on every path. `Player.drop(...)` returns null when a toss was cancelled; only clear the hand when it is non-null.
- **Per-tick handlers run for every player**: gate with `tickCount % N` and bail early when the stack has no attuned part, since `MaterialPerks.attunements` allocates.
- **Spawn events fire again on reload** (arrows): mark what you handled in `getPersistentData()`, and note the leftover edge case as a `shortcut:`.
- **Do not stack hooks on one stat**: attribute effects (breath, sneak, water speed, submerged mining) use fixed-id transient modifiers synced every 10 ticks (`TideKinds.sync`); reuse that, do not add a second pattern.
- **Wear only from use or misuse, never uptime** (digest). Anything that costs durability does it per use (per kill, per blow), never per tick.
- **A part is never destroyed.** Cap wear at `getMaxDamage() - 1`, as `ModularStacks.cap` does.
- **Mirror names must differ from the effect's** and read as the real opposite ("Cold", "Free", "Kin", "Tithe"), not "Anti-X".

## Fanning out to agents

A first cut of several effects splits well, **at most 3 agents at once**, one effect group each, with these rules in every prompt: read the design digest first; the agent owns only its own `<Effect>Kinds.java`, its rows and its lang lines (insert after the `"attunement.feedback.ward"` line, and re-grep after the last edit because other agents share the file); it may not edit the shared core; it must compile at the end and list skipped roles; no commits, no deploys, no game tests. Paste the model section above into the prompt so the agent reads no more than it must. Collect the skipped roles into roster 3a afterwards. Do this only for new effects; a single behaviour is faster done inline.

## Hard-rule filter

Reject or rework a proposal that needs: an identity check, a free scalar the player sets (named options instead), wear from uptime, a precision hard gate, a stat bump with no physical cause, or a role that makes the part destroyable. Name the rule when you rework.
