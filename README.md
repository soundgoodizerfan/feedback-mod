# feedback

a science overhaul of minecraft, close to a total conversion where the physics demands it. it's still minecraft, not a new game running on minecraft's engine: it's what the game becomes when the player is an inventor in a world whose physics is the same everywhere.

## the pitch

the player builds everything out of parts they put together themselves. a sword, a furnace, an engine, a gun, a dish and a potion are all things you design from parts and materials, and the properties of those materials decide what they do.

the test for every decision: **does this let the player make something that doesn't exist in real life, or only pick from a short list of things other people already make?** the short menu of premade variants is the failure. so things are named after they're built, not built from names. parts have measurements to set, not just styles to pick. you can add to anything. behaviour is never chosen by a click, it falls out of what you built.

minecraft's world lets you watch things happen. if those things are scientifically consistent, you can invent ways to use them. so the oddities are real science here: blazes, ender, experience and sculk are magic to us and ordinary to the people who live in this world. it's hard magic, every rule has a reason and means exactly what it says, and there's no magic path and no exotic material.

it keeps what makes minecraft feel like minecraft. it's a sandbox first: getting from spawn to owning machines and instruments is a prologue, and after that you choose which sciences to chase, in any order. vanilla blocks and systems are replaced only where they contradict the physics.

## what it isn't

not a tech tree where the tier-3 furnace is a faster tier-2 furnace. progress means a growing ability to make, observe, control and reproduce physical things, and a stage that only does the same job faster hasn't advanced anything. old work gets easier, never harder, and what changes is the kind of hard.

not a tech mod with other systems bolted on. technology is one part of the world and a shrinking share of the mod: combat, survival, food, potions, enchanting and villages obey the same physics and the same inventor rule.

no item id checks. a machine states a physical operation ("apply heat," "apply force"), never a recipe, and sorting and processing work off real properties like density, magnetism and particle size. precision never locks anything behind a tier: if you have the raw physical ability you can attempt it by hand, badly, forever. automation makes things practical, it doesn't unlock them.

## how it plays

- **machines don't know when they're done.** a furnace, a mixer or a stamp keeps doing its physical action for as long as it has power and input, and continued operation past completion acts on the finished output: it degrades, burns or ruins it. so there are always two jobs, make the process happen and notice when to stop it. sensing, control and actuation are separate systems that know nothing about each other, and a controller is a switch, not a dial.
- **energies are plural.** mechanical, thermal, chemical, electrical (and magic) are each usable on their own and each fails and stops differently. electricity is the backbone from mid-game, but it isn't what everything runs on.
- **tools, weapons, shields, bows and armour** are shaped into parts at a bench for their stock (smithing table, woodworking table, stonecutter) and assembled in a crafting grid or at the engineering table. there's no iron sword item, there's a blade of some metal on a grip of something else, and every figure comes from the materials. vanilla's gear is converted on the spot.
- **guns** are the same kind of thing: a receiver, barrel, action and grip you machine yourself, ammunition you make yourself, and a pressure rating that decides whether it holds or lets go.
- **combat** is more than a click: swept hits, a parry and roll, kicks, grabs, a grapple, off-hand weapons, gun-fu, and a skill tree that unlocks technique, never capability. materials carry perks (silver smites the undead, a blaze rod sets things alight and burns your hand).
- **vehicles** are real physics bodies (via sable) built from a welded frame, with engines, tires and drivetrains you design.
- **hazards** are real: hot work burns bare hands, a shut boiler bursts, bare wire shocks. protection is a material you wear.
- **the vanilla furnace, crafting table and friends** are replaced by things that obey the same thermal and mechanical rules as everything else.

## status

design-first. the philosophy was written before the code. `feedback_philosophy.md` is the why-document, and it's still being actively revised as the build finds things the design got wrong. the mechanical and thermal core, electricity, modular gear, guns, vehicles and the combat system are all built to varying degrees of playtested, and a lot of figures are placeholders waiting on play. see the [wiki](wiki/Home.md) for how everything works in game.

platform is neoforge, minecraft 1.21.1.

## building

```
./gradlew build       # jar to build/libs
./gradlew runClient    # dev client
```

## license

code is GPL-3.0-or-later. original assets are CC0. third-party material keeps its own licence. see `LICENSING.md`.

## ai usage

i'm not gonna sit here and lie to you. i, soundgoodizerfan, just flat out don't code java at all. i've made modPACKS, but never a mod. so yeah, i think on paper, that means this is vibecoded. but i have been going out of my way to commit to optimization, infrastructure, and actual fun in the mod. i haven't just been, like, sitting here one-shotting feature after feature. i HAVE actually been sitting here, watching every output. making sure things are coded in a smart way (as far as i can tell), and making sure that the game mechanics are ACTUALLY fun to practice, not just fun on paper. still though, if you're a java dev looking at this repo, and you think you'd do better than claude is, PLEASE message me. i dont LIKE that this mod is vibecoded, but i dont want to learn java for this because then the code will probably suck.

The design documents (`feedback_philosophy.md`, `feedback_mechanics.md`, everything under `docs/`), the working notes are kept in a private repository and are not part of this one; names of them in this repository, including source comments, refer to those private files.

Sound effects: [soundza.com](https://soundza.com)
