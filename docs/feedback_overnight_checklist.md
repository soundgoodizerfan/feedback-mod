# Playtest checklist (live)

The overnight combat checklist was played and cleared by the author (2026-10-23). This file now holds what the next playtest should look at. Everything listed is **compile-only, not played** unless stated; items leave the list when the author confirms them.

## Host notes

- Pack: `../feedback-test-pack` (packwiz). `./build-and-deploy.sh` copies the jar in and pins Sable, Curios, JEI and Jade.
- Players need Sable, Curios, JEI and Jade; the pack carries them. The in-game guide is the wiki (`wiki/Home.md`, start at Metalworking then Heat); there are no quests.
- All art is placeholder, pulled from `../Texture-reference`. Tell players so.
- Vanilla's two huge iron and copper veins still generate vanilla ore (mechanics 9.6, accepted).
- Tuning numbers are placeholders everywhere; expect "too slow" and "too fast" reports, not bugs.

## First-hour route to walk (survival, no `/give`)

1. Wooden pickaxe, stone, cobblestone Furnace, copper ore (needs a stone pickaxe), smelt ingots.
2. Hand Hammer, copper plate by hand, Tool Table, first modular pick.
3. Crucible and Firebox; alloy bronze or brass; cast an ingot (Ingot Mold is craftable now).
4. Steel, then the machine shop (Drill Press, Lathe, Rolling Mill, Wire Drawer).
5. Shafts, Hand Crank, Dynamo, Cable, Galvanic Cell.

## New this round (2026-10-23), unplayed

- **Deposits:** galena and pyrite (stone, y -32 to 48), kaolinite (hills, y 40 to 90, digs by hand). Walk the surface: outcrops should show where veins sit. Fire kaolinite to firebrick. Heat galena past 950 Tu for silver, or melt it for lead.
- **Recipes:** Welder, Pivot Setter, Data Connector, Ingot Mold, Lift, Chassis Frame, Car Design Stand, Damper. Check each shows in JEI and crafts from items a player can have by then.
- **Sprites:** gun parts, instruments, tools, moulds and papers got new item sprites; ten machines and three ores got new block faces. Look for anything stretched, wrongly tinted, or still showing a vanilla sprite.

## Still unplayed from earlier (pick a spine, not all of it)

Enchanting table and attunements, basic electronics (resonator, coherer, whisker, relay, solder, boards, circuit, oscilloscope), galvanic cell pack, steam engine bands, jar baton, gun launchers (spring, steam), demolition charge, machine heads, vehicle brakes, differential, spring and transmission. Details: `TODO.md`.

## Input wanted

- Silver has no ore. Is the galena route acceptable, or should silver be a vein?
- Gun parts have no sprites made for them (no sight, suppressor or trigger in the collection); the current ones are the nearest metal part.
