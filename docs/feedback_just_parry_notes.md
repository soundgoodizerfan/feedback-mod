# Just Parry: prior-art notes (design only)

Researched 2026-10-21 from public pages only. No code cloned or read.

## Identity and licence

- Author: Thebugger4. Spin-off of the author's mod "Movement Arrows" (MCreator-hosted; parry was part of it before being split out).
- CurseForge: https://www.curseforge.com/minecraft/mc-mods/just-parry
- Modrinth: https://modrinth.com/mod/just-parry
- Other listings: https://www.mado-hosting.com/minecraft-mods/just-parry, https://www.modpackindex.com/mod/72356/just-parry, https://mcreator.net/node/110022 (Movement Arrows)
- Versions: Minecraft 1.20.1 only. Loaders: Forge and NeoForge. Single file `just_parry-2.5-forg-1.20.1.jar`, 8 Aug 2025, about 103,700 downloads. Author states it is no longer updated. **No 1.21.1 NeoForge build exists.**
- Source repo: **none found.** No GitHub/GitLab link on CurseForge, Modrinth or the mirrors; only a Discord invite. Searches turned up nothing.
- Licence: CurseForge says "Creative Commons 4.0" with no variant; Modrinth shows "LicenseRef-Creative-Commons-4.0" and links to the CC BY 4.0 deed (best evidence: BY 4.0, but unconfirmed, no LICENSE file to check). Whether it covers code and assets is not stated; it reads as a blanket project licence.

## Can it go into Feedback (GPL-3.0-or-later code + ARR assets)?

- Code: nothing to adapt, because no source is published. Even if it were, CC licences are not designed for software (CC's own FAQ advises against it). CC BY 4.0 is one-way compatible into GPLv3 only per CC's compatibility notice, and the variant is unverified. Treat as unusable.
- Assets: not usable. ARR in this repo plus unclear terms here.
- Design/ideas: free. Ideas are not copyrightable. Follow the repo rule: read design, write own code, record in THIRD-PARTY-LICENSES.md only if code is ever adapted (it will not be).

## Design as described (pages only)

- Input: key G (default). Plays first- and third-person animation, opens a parry window.
- Timing: pages say 13-tick window and 60-tick animation (pages inconsistent; presumably 13-tick active window inside a 60-tick animation lockout). A hit during the window deals no damage; hits during the wider animation also deal no damage per CurseForge wording.
- Damage-type reactions: different damage types get custom reactions (example: parrying fall damage makes the player roll).
- Projectiles: pressing G with an airborne projectile, physics object, or salmon within 3 blocks deflects it along the player's look direction at 1.5x its original speed. Also boosts own projectiles. Source inspiration: ULTRAKILL projectile parry plus Souls-like timed parry.
- Config: global "enable parry" default; per-player attribute `just_parry:enableparry` (1 allow, 0 block, -1 reset to config); per-item properties via `/justparryitemregistry`, stored in `itempropreties.txt`: knockback, damage, projectile rules, redirect speed, inaccuracy, damage absorption, window, cooldown. Fists configured via `just_parry:justparryiteminhand`. Item properties apply only if "enable item properties" is true; changes apply server-wide at once.
- Not documented anywhere found: hit-stop/impact frames, camera shake, sounds, particles, what a parry does to a melee attacker (knockback/stagger is only implied by item knockback/damage fields), events-vs-mixins hooking, changelog contents.

## Most reusable ideas for Feedback

1. Per-weapon parry parameters (window, cooldown, knockback, damage absorbed, redirect speed, inaccuracy) as data on the held item. Fits Feedback: derive them from material/part figures of a modular weapon rather than a command registry.
2. Short active window nested in a longer animation commitment (whiff costs recovery), so timing is risk, not a free toggle.
3. Damage-type-specific parry reactions instead of one blanket negate (fall damage becomes a roll). Maps to Feedback hazards: a parry/guard that reacts differently to fire, shock, pressure burst.
4. Projectile redirect: radius check (3 blocks), aim along look vector, speed multiplier (1.5x), plus an inaccuracy term. Also redirects physics bodies (relevant to Sable bodies).
5. Per-entity enable attribute separate from global config default (lets datapacks or items grant or deny parry per player/mob).

## Caveats

- Everything above is from storefront text written by the author; figures conflict (13 vs 60 ticks). Do not treat as verified behaviour.
- Combat feel (hit-stop, shake, sound) must come from other sources if wanted; this mod's pages give none.
