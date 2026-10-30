# feedback

a tech-themed total conversion of minecraft. it started as a tech mod about building and controlling physical processes, and it's turned into a rebuild of the whole game out of physical properties instead of fixed item lists.

## the pitch

machines don't know when a recipe is done. a furnace, a mixer, a stamp, whatever, it keeps doing its physical action for as long as it has power and input. it has no idea whether the thing inside is finished.

other tech mods already have machines that idle forever. that's not the new part. the new part is what happens if you let it keep going: **continued operation past completion acts on the already-finished output.** it doesn't just waste power, it degrades, burns, overworks, or ruins the thing you just made.

so you've actually got two problems, always:

1. make the process happen
2. notice when it's done, and stop or redirect it before it eats its own output

everything in the mod serves one of those two jobs. sensing is one system, control is another, actuation is a third, and none of them know about each other. a sensor doesn't control anything. an actuator doesn't know anything. the controller is a switch, not a dial, it can only start or stop a supply.

## what it isn't

not a tier ladder where the tier-3 furnace is a faster tier-2 furnace. a machine states a physical operation ("apply heat," "apply force"), never a recipe. there's no `if item == copper ingot` anywhere, sorting and processing work off real properties like density, magnetism, and particle size instead.

precision never locks a recipe behind a tier. if you have the raw physical ability, you can attempt it by hand, badly, at a tiny rate, forever. automation makes things practical. it doesn't unlock them.

mechanical, thermal, and chemical power are all usable on their own, none of them have to funnel through electricity first.

## the total conversion

the same rule, "what a thing does comes from what it's made of," is applied to the rest of the game:

- **tools, weapons, shields, bows and armour** are assembled from parts at a tool table. there is no iron sword item, there's a blade of some metal on a grip of something else, and every figure comes from the materials. vanilla's gear is converted on the spot.
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

code is GPL-3.0-or-later. assets are all rights reserved. see `LICENSING.md`.

## ai usage

i'm not gonna sit here and lie to you. i, soundgoodizerfan, just flat out don't code java at all. i've made modPACKS, but never a mod. so yeah, i think on paper, that means this is vibecoded. but i have been going out of my way to commit to optimization, infrastructure, and actual fun in the mod. i haven't just been, like, sitting here one-shotting feature after feature. i HAVE actually been sitting here, watching every output. making sure things are coded in a smart way (as far as i can tell), and making sure that the game mechanics are ACTUALLY fun to practice, not just fun on paper. still though, if you're a java dev looking at this repo, and you think you'd do better than claude is, PLEASE message me. i dont LIKE that this mod is vibecoded, but i dont want to learn java for this because then the code will probably suck.

The design documents (`feedback_philosophy.md`, `feedback_mechanics.md`, everything under `docs/`), the working notes are kept in a private repository and are not part of this one; names of them in this repository, including source comments, refer to those private files.
