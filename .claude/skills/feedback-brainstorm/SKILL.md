---
name: feedback-brainstorm
description: Brainstorm new ideas for Feedback (machines, materials, processes, phenomena, mechanics, slices) through two mandatory lenses -- realistic engineering, and how the Minecraft world changes it, compendium-style. Use whenever the user asks for ideas, "what if", new content, or design options for this mod, even if they don't say "brainstorm".
---

# feedback-brainstorm

Generate and test ideas for Feedback. Every idea goes through **both lenses**. Neither is optional; the Minecraft lens is the one that gets skipped, so it is the one to be strict about.

## 0. Ground first

Before generating, read what the idea touches. Don't brainstorm from memory of the docs.

- `feedback_philosophy.md` -- always §2 (Scientific, Not Realistic), §3 (anti-simulator), §5 (one-answer problems), §14 (axes, difficulty flat / novelty up), §20 (problem-first method). Plus whatever section the topic lives in.
- `docs/speculative_physics_inspo_doc.md` -- its Method and Ratings sections (top ~125 lines), then `grep -n` for every Minecraft thing the topic could touch (blaze, redstone, ender, sculk, XP, slime, glowstone, netherite, ghast...). Mindset and parts bin, **not canon**; its ideas may be lifted wholesale because they are the user's own.
- `TODO.md`, `docs/feedback_progression_roadmap.md`, and any spec for the area (`feedback_*_spec.md`) -- so ideas build on what exists instead of duplicating it or contradicting a ruling.
- `src/main/resources/data/feedback/material/` -- what materials and figures already exist.

Ask the user which open-source mod already solves the area (CLAUDE.md "Before building a feature, ask about prior art") when an idea heads toward implementation. Brainstorming itself needn't wait on the answer.

## 1. Start from a problem

Per philosophy §20: **problem -> process -> capability -> machine -> material -> recipe -> progression consequence**. Not "a cool machine" first. If the user hands a machine, work backwards to the problem it solves and say if there isn't one.

## 2. Realistic lens

What does real engineering actually do here? Use real science as the *method*, never the *target*.

- **Real analogue.** The real process, apparatus, or phenomenon. Name it. What variables actually govern it?
- **Abstraction cut.** Which of those variables creates a player decision? Keep those; cut the rest (§3: "does modeling this create a meaningful engineering choice?"). Say what was cut and why.
- **The perishable output.** What happens if the machine keeps running past completion? This is the mod's core pitch -- an idea with no overrun behaviour needs a reason.
- **Gates.** What is genuinely physically impossible (hard gate) vs. merely unreliable without instruments (soft gate)? Precision never hard-gates.
- **Two answers.** At least two correct approaches costing different things (capital vs attention, throughput vs reliability).
- **Sense / stop.** How is it measured, how is it stopped, and what is the cutoff's failure character (coasts, arcs, lingers, keeps reacting)?
- **Axis.** Which §14 axis or axes does it advance?
- **Units.** Express it in existing §17 units; derive rates, don't mint units.

## 3. Minecraft lens

Minecraft's rules are the setting's real laws of nature (§2). For every idea, ask: **does anything in this world make it work differently than it would on Earth?** Check deliberately against:

- **Exotic materials and organisms** -- redstone (flagged exception, granular superconductor in the compendium), blaze, ender/chorus, sculk and XP, glowstone, slime, ghast, netherite/ancient debris, phantoms, creepers, the Wither, amethyst, prismarine, honey, etc.
- **Dimensions and environment** -- Nether (no water, ambient by dimension via `AmbientTable`), End (void, endermen), biome temperature, rain, snow, lightning, day/night, height and bedrock, lava as a fixed 1200 Tu fire.
- **World mechanics as phenomena** -- enchanting, anvils and mending, brewing, beacons, conduits, portals, mob spawning by light level, crop growth, villagers and trade, block updates, gravity blocks, infinite water sources.
- **Modded evidence** -- the compendium treats other mods' phenomena as in-universe evidence too (AE2 certus/fluix, etc.).

When a twist exists, reason about it the compendium's way:

- **Smallest departure** from known reality that explains the observed behaviour.
- **Classify**: extrapolation, novel implementation, or open problem. Give P (worst single leap), L (independent leap count), U if unresolved.
- **Declare dependencies** on mechanisms already established (in the compendium or the mod). Reuse needs justification, not just convenience.
- **Boundary condition**: why doesn't this happen everywhere? The answer usually *is* the gameplay constraint.
- **Observe -> exploit -> characterize -> control -> reproduce -> engineer**: where on this ladder does the player first meet it? Exploitation before understanding is fine (§2).

Rules for the lens:

- **If a twist exists, it must be in the idea.** Not a footnote, not "could later" -- the design changes to include it.
- **If none exists, say so plainly** ("Minecraft lens: none -- iron wire drawing works as on Earth") with one line on what was checked. Never invent one to fill the slot. Humans already understand iron; not everything needs a twist. Ordinary metallurgy and ore separation (gold from gravel, say) usually have none -- the user's own calibration.
- **Contradictions are evidence.** If vanilla behaviour contradicts the realistic version, the vanilla behaviour wins and the mechanism must explain it.
- **Avoid real-world-loaded names** for invented mechanics where the real name implies physics the mechanic contradicts.

## 4. Hard-rule filter

Reject or rework any idea that needs: identity checks (sort on properties and tags only), a machine that knows a recipe, a continuous knob (named discrete options instead), a controller acting as a dial (vehicles excepted), wear from uptime, a precision hard gate, "requires tier N", electricity as the universal currency, a stat upgrade with no physical cause, or a physics simulation nobody makes decisions in. Name the rule when you rework.

## 5. Output

Pick depth from how much room the question has; don't ask.

- **Open question** ("what could we do with X", "ideas for slice N"): wide and shallow. 6-10 ideas, two lines each plus the Minecraft-lens verdict. User picks which to expand.
- **Narrow question** (one machine, one problem, "how should X work"): few and deep. 2-3 ideas in the full template below.

Full template, per idea:

```
### <name>
Problem: <what the player runs into>
Process: <real analogue, and what was abstracted away>
Minecraft lens: <the twist and how it changes the design, with P/L and dependencies -- or "none" plus what was checked>
Decision: <the choice it creates; the two-plus answers>
Overrun: <what continued running does to the output>
Sense / stop: <instrument, cutoff, failure character>
Axis: <§14 axes>
Open: <[OPEN] questions it raises>
```

Then a one-line ranking with a recommendation. Ideas are proposals: don't write them into design docs unless the user approves, and when they do, mark unsettled parts `[OPEN]` rather than resolving them silently.
