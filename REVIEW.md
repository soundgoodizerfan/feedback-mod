# Codebase review — 2026-10-09

A full pass over `src/main` (202 Java files, ~25k lines) and all resources, made by six parallel reviewers split by package, against the working tree at `4a1c2ba` plus uncommitted changes. The build passes. The top crash and dup findings were spot-checked by hand and confirmed. Everything else was checked against the code on disk by its reviewer but has not been re-verified. **Nothing has been fixed yet.** Tick items off or delete them as they close; delete this file once it is empty, and fold anything long-lived into `TODO.md`.

Java paths are relative to `src/main/java/io/github/soundgoodizerfan/feedback/`. Line numbers are as of this review and will drift.

## Design rulings (user, 2026-10-09)

The reviewers found these tensions with CLAUDE.md's hard rules. Rulings:

1. **Wear from use is OK** for the Grinding Wheel (consumable) and the Hand Hammer (durability). It is an accepted exception to "wear only from misuse". **Follow-up:** say so in `feedback_philosophy.md` and in the CLAUDE.md hard rule, so the next reviewer does not flag it again.
2. **Wasted force is NOT OK.** The Press, Wire Drawer, Rolling Mill and Drill Press must put force under the hardness floor (or on the wrong tool) into machine wear, the way the Hammer and the Lathe already do. The Press also needs `Wearing`/condition.
3. **A boiler that costs no heat is NOT OK.** Making steam must draw Work from the body, so steam output is limited by heat flow on the difference.
4. **Removal with no overrun is NOT OK.** `turned_steel_rod`, `drilled_steel_plate` and `ground_hardened_steel` need overrun entries. Running past the target must keep acting on the output (the core pitch).
5. **Generic swarf is NOT OK.** Swarf has to keep its source material, as a component or as one item per metal, instead of always smelting to `iron_nugget`.
6. **Identity checks are VERY NOT OK.** Ecosystem compatibility is a priority. Lava detection in `core/thermal/HeatSource.java:63-64` (`== Fluids.LAVA`, `is(Blocks.LAVA)`) must use `FluidTags.LAVA`. **Sweep the whole codebase** for other `== Items.X` / `== Blocks.X` / `== Fluids.X` / `is(Items.X)` checks and replace them with tags or properties.

## 1. Crashes, duplication, item/state loss (fix first)

- [ ] `machine/valve/ValveBlockEntity.java:51-56` — `addAir` forwards to the `Blown` it faces with no loop guard. Two open valves facing each other recurse until `StackOverflowError` (server crash). **Confirmed.**
- [ ] `core/rotation/RotationNode.java:274` — `setRemoved` runs `RotationPropagator.onRemoved` with no guard. `LevelChunk.clearAllBlockEntities` calls `setRemoved` on chunk unload and shutdown, so every unload triggers a full rebuild per node. That can `destroyBlock` and force-load chunks. Fix: set a flag in `onChunkUnloaded()` and skip propagation (Create's pattern). **Confirmed.**
- [ ] `core/rotation/RotationPropagator.java:119` — the source-conflict branch calls `destroyBlock` on wherever the rebuild started. That can be a neighbour, after `onRemoved` (:84) or a clutch toggle (`ClutchBlockEntity:83`). Pass the culprit in explicitly, or refuse to couple without destroying.
- [ ] `core/rotation/RotationNetworks.java:41` — `WeakHashMap<LevelAccessor,…>`: the value holds networks, which hold nodes, which hold the level (the key), so the entry is never collected. Every ServerLevel leaks. Clear on `LevelEvent.Unload` / `ServerStoppedEvent`.
- [ ] `control/controller/ControllerBlock.java:76` — `removeItemNoUpdate` never calls `setChanged`, so the card is duplicated on reload. It also leaves `lastGraph`/`lastSensorValues` stale. **Confirmed.**
- [ ] `machine/crucible/CrucibleBlockEntity.java:304-318`, `machine/boiler/BoilerBlockEntity.java:116-121`, `machine/firebox/FireboxBlockEntity.java:146-150` — the ticks change temperature, TPu and burnTicks without `setChanged`, so that state is lost on unload (burn time comes back).
- [ ] `machine/vessel/ThermalVesselBlockEntity.java:397-402` — melts with a plain `tank.fill` after shrinking the input. A second, different metal fills 0 and is destroyed. Use `AlloyMix` as the crucible does, or simulate first.
- [ ] `machine/firebox/FireboxBlockEntity.java:169`, `machine/vessel/ThermalVesselBlockEntity.java:306-308` — the lava bucket loses its bucket. The vessel checks the remainder after `shrink`, when the stack is already empty. Read the remainder before shrinking.
- [ ] `machine/wiredrawer/WireDrawerBlockEntity.java:101` — the fast-draw snap roll runs before `Deforming.apply`, so it destroys any item at 1%/tick, including items with no DRAW entry and finished wire. Roll only when a blow would land.
- [ ] `item/MoldItem.java:89-95` — a partial draw (< 144 mB) leaves the mold permanently stuck: it can't cast, and it can't be refilled while not empty. Draw only when the full `MOLD_CAPACITY_MB` is available.
- [ ] `item/MoldItem.java:81` — `onItemUseFirst` returns `PASS` on the client, so `use()` also fires on the same click (an instant cast).
- [ ] `machine/crucible/CrucibleBlock.java:79-85`, `machine/vessel/ThermalVesselBlock.java:108-116`, `fitting/Fitting.java:44,51` — breaking the block destroys its sided fittings and leaves dangling links. Nothing ever calls `getPickItem()` or `onRemoved()`.
- [ ] `machine/crucible/CrucibleBlockEntity.java:385-388` — `alloy.add` is uncapped and `FluidTank.setFluid` does not clamp, so the tank can exceed capacity. The "silently lost" comment at :381-384 is stale.
- [ ] `control/data/DataNode.java:136` (from `ControllerBlockEntity:114,124` every tick, `DataLinkManager:67`, `ProgrammerBlockEntity:185`) — `getBlockEntity` force-loads unloaded chunks, so the controller acts as a chunkloader. Guard with `level.isLoaded`.
- [ ] `machine/pulley/PulleyBlockEntity.java:125-127` — same issue for the belt partner. It also deletes the link when the partner is merely unloaded.
- [ ] `control/programmer/ProgrammerCommitPayload.java:59`, `ProgrammerLinkPayload.java:58` — `getBlockEntity(payload.pos())` runs before `stillValid`, so a client can force-load any chunk. Check `player.containerMenu instanceof ProgrammerMenu m && m.getPos().equals(pos)` first.
- [ ] `control/programmer/ProgrammerBlockEntity.java:137-145` — `commit` trusts any `DataNodeRef` the client sends. In multiplayer a modified client can drive or read any machine in the world. Keep a server-side set of refs handed out via `link()`.
- [ ] `process/Deformation.java:95`, `process/Removal.java:72` — `Operation.valueOf` inside `xmap` throws on a typo and fails the whole reload. `toUpperCase()` with no `Locale.ROOT` breaks "drill" under a Turkish locale. Use one shared `Operation.CODEC` (`StringRepresentable.fromEnum`).
- [ ] `process/Deforming.java:141,174`, `process/Removing.java:90,111` — `WORK_OPERATION` persists `ordinal()`. The REMOVE → TURN/DRILL/GRIND split already shifted the ordinals, so saved items decode wrong. Persist the name.
- [ ] `Feedback.java:112-113` — payload registrar version still `"1"` despite the payload changes. Bump it.
- [ ] `process/Deformation.java:157`, `process/Removal.java:96` — no `hardness > 0` validation. 0 gives Infinity, the round gives `MAX_VALUE`, and the work total overflows negative.
- [ ] `process/HandDeformationRecipe.java:70,86` — the `CRAFTING_LEVEL` ThreadLocal is never cleared, so it keeps the last Level reachable.

## 2. Mechanics that silently don't work

- [ ] `machine/pulley/PulleyBlockEntity.java:137,155` — the belt only sets a target on a 0-capacity network, so the driven side never leaves 0 RPM. Feedback spec §6a:413 claims it works.
- [ ] `machine/pulley/PulleyBlockEntity.java:141-155` — once that is fixed, the driven load is never charged to the driver, which makes free Su.
- [ ] `machine/pulley/PulleyBlockEntity.java:141,155` — reads the partner network's `getCurrentRpm()` and ignores both pulleys' ratio and sign. Gearing between pulley and network reference gives the wrong speed or direction.
- [ ] `machine/pulley/PulleyBlockEntity.java:91-96` — relinking never unlinks old partners. There is no distance check, and `BeltLinkManager:55` stores no dimension.
- [ ] `machine/pulley/PulleyBlockEntity.java:151` — belt condition wears, but nothing reads it, and there is no `setChanged`.
- [ ] `machine/pulley/PulleyBlockEntity.java:146-149` — `desired == 0` returns early and leaves a stale non-zero target.
- [ ] `process/Removal.java:96` (+ `FTuning.java:376-419`) — `max(1, round(St/hardness))` gives 1 Fu per cut for every feed, pass and grit, so slice 3's discrete choices do nothing (§5). Use fractional accumulation, or retune. **Confirmed.**
- [ ] `machine/lathe`, `grindingwheel`, `wiredrawer`, `rollingmill`, `press` tickServer — one step per tick at any RPM above stop, so speed and gearing are meaningless. The press does a full blow at 20/s and syncs every tick. Accumulate progress at rpm × constant, as the linkage's `strokeProgress` does.
- [ ] `machine/grindingwheel/GrindingWheelBlockEntity.java:127,136` — never heats at overrun (it returns early on NO_PROCESS). It also uses St as Fu (no division by hardness).
- [ ] `process/ThermalProcessTable.java:81` — `find` takes the first subset match in HashMap order, so iron + coal can resolve to graphite instead of cast iron. Prefer the most inputs, tie-broken by id, sorted once in `apply`. **Confirmed.**
- [ ] `process/AlloyMix.java:86-89` — `remove` rounds each metal separately, which creates or destroys metal. The ratio drifts out of the window and the tank locks. Use largest-remainder rounding.
- [ ] `process/Removing.java:98` — completing a cut drops the workpiece's heat (§9). Mirror `Deforming.java:157-159`.
- [ ] `machine/firebox/FireboxBlockEntity.java:146-151` — reads ambient for one tick between fuel pieces, which trips the rate limit and fakes cooling ticks for tempering. Relight in the same tick.
- [ ] `machine/dip/Dipping.java:102-103` — right-clicks don't raytrace fluids, so the lava branch never runs. Use a `ClipContext.Fluid.SOURCE_ONLY` clip.
- [ ] `machine/dip/Dipping.java:87` — the handler returns on the client, so the block's own use still runs client-side (ghost insert).
- [ ] `machine/dip/Dipping.java:125-137` — accepts a partial drain as a full dip, and the result is not heat-stamped.
- [ ] `machine/bellows/BellowsBlockEntity.java:93-98` — every neighbouring `Blown` gets the full stroke (N× air). Split it.
- [ ] `machine/linkage/CrankLinkageBlockEntity.java:167` — the load reads the machine in front live, but nothing recalculates when that machine is placed or removed. Add `neighborChanged` → `recalculate`.
- [ ] `core/rotation/RotationNetwork.java:232` — `speedingUp` is false at 0 RPM, so the first spin-up step uses braking force.
- [ ] `control/bimetallic/BimetallicStripBlockEntity.java:81` — `tripped` is not saved (the blockstate is). After a reload the heat never resumes.
- [ ] `control/programmer/ProgrammerBlockEntity.java:258-262` + `ProgrammerScreen.java:362` — a program built with no card is wiped when a card is inserted.
- [ ] `control/programmer/ProgrammerScreen.java:712-719` — `applyLink` matches on node id only, and ids are reused, so a late link response can overwrite the wrong card.
- [ ] `control/data/DataLinkManager.java:50`, `DataNodeRef.java:47` — refs carry no dimension, so they resolve at the same coordinates in the wrong level.
- [ ] `control/data/DataNode.java:106-125` — `unlink` is never called. Dead links count toward the cap of 4 forever.
- [ ] `machine/valve/ValveBlockEntity.java:44` — `Switchable` but not a `DataNode`, so Punch Card actuators can't target it (controller spec §3).
- [ ] `machine/clutch/ClutchBlockEntity.java:51-55` (and Damper) — pure actuators accept `linkTo` and burn link slots. Override `canLinkTo` → false.
- [ ] `machine/vessel/ThermalVesselBlockEntity.java:273-279` — while a process matches, every other slot's vanilla fallback freezes, contradicting the class doc :93-95.
- [ ] `machine/vessel/ThermalVesselBlockEntity.java:467-471` — `setItem`/`removeItem` never reset `tpu[slot]`, so a swapped item inherits progress.
- [ ] `machine/crucible/CrucibleBlockEntity.java:255-271,321-326` (vessel :271-274) — TPu is not tied to its process and carries over to a new match. `removeItem` (:290) zeroes it even for unrelated items.
- [ ] `machine/heatexchanger/HeatExchangerBlockEntity.java:70` — `lastDelta` comes only from `Heat.tick`, so exchanger cooling is invisible to the rate limit and to `requireCooling`. Compute it from the actual temperature change.
- [ ] `machine/vessel/ThermalVesselMenu.java:149,176-179` — shift-click stamps the remainder instead of the moved stack, so items arrive cold. Hopper extraction skips stamping entirely.
- [ ] `machine/vessel/ThermalVesselBlockEntity.java:457-471`, `control/programmer/ProgrammerBlockEntity.java:71`, `control/controller/ControllerBlockEntity.java:61` — bare `Container` with no `canPlaceItem`, so hoppers insert anything anywhere.
- [ ] `machine/crucible/CrucibleBlock.java:60`, `machine/firebox/FireboxBlock.java:71` — inserting runs on the client too (ghost contents).
- [ ] `machine/crucible/CrucibleBlock.java:81-83`, `machine/vessel/ThermalVesselBlock.java:111` — contents dropped on break keep their old stamp instead of the vessel's temperature (§9).
- [ ] `machine/crucible/CrucibleBlockEntity.java:501-508`, `machine/vessel/ThermalVesselBlockEntity.java:537-544` — `loadAdditional` never nulls absent fitting slots, and reallocates fittings on every sync.
- [ ] `machine/vessel/ThermalVesselBlockEntity.java:268-269` — the ceiling is a hard clamp, so heat above it vanishes. Use a stronger leak above the ceiling, or document the clamp as a material gate.
- [ ] `machine/steamengine/SteamEngineBlockEntity.java:113-131` + `registry/FCapabilities.java:63` — the tank is exposed as drainable, so adjacent engines siphon steam from each other. Expose a fill-only wrapper.
- [ ] `registry/FCapabilities.java:46-64` — the Press has no ItemHandler capability, so it can't be automated despite its doc.
- [ ] `machine/grindingwheel/GrindingWheelBlockEntity.java:67`, Pulley condition — condition lives on the BE and the loot drops a plain item, so mining and replacing resets wear. Copy it to a component.
- [ ] `machine/lathe/LatheBlockEntity.java:170-178` — `wearBit` doesn't `sync()`. At the 0.4 floor the bit's hardness is 8, below steel's 15, so wear becomes a hard gate.
- [ ] `machine/lathe/LatheBlockEntity.java:204-207` — the sensor reads WORK, which resets at completion, so Calipers → Clutch auto-stop can't tell done from not started.
- [ ] `process/Quenching.java:90` — mutates the ItemEntity stack in place, so clients don't see the change. `:93` hisses on long-cooled stamped stacks.

## 3. Rendering and resources

- [ ] `client/FClientSetup.java:57-82` — the Pulley is `ENTITYBLOCK_ANIMATED` with no renderer, so pulleys are invisible. Belts are not rendered either.
- [ ] `assets/feedback/models/item/` — 15 items have no item model: `tin_ingot`, `zinc_ingot`, `bronze_ingot`, `brass_ingot`, `lead_ingot`, `glass_ingot`, `quartz_glass_ingot`, `cast_iron`, `tempered_steel`, `graphite`, `ceramic`, `glazed_ceramic`, `kaolinite`, `firebrick`, `copper_tubing`.
- [ ] `models/item/thermometer.json:3` — `minecraft:item/clock` doesn't exist in 1.21.1. Use `item/clock_00`.
- [ ] `models/item/temperature_sensor.json:3` — `minecraft:item/lightning_rod` doesn't exist. Use `block/lightning_rod`.
- [ ] `models/block/pressure_vessel.json:4` — `minecraft:block/lodestone` doesn't exist. Use `lodestone_side` / `cube_column`.
- [ ] `client/HeatTooltip.java:55` + `client/ItemHeatTooltip.java:50` — duplicate listeners, so temperature shows twice. `ItemHeatTooltip` also null-derefs `getEntity()`. Delete `ItemHeatTooltip`. **Confirmed.**
- [ ] `client/FClientSetup.java:67-68` — `DataNodeRenderer` is registered only for the crucible and the debug controller. The Damper, Clutch, Lathe and vessel fittings never draw, and links whose lower end is one of those never draw.
- [ ] `client/DeferredDataRenderer.java:78` — the early return skips `queued.clear()`.
- [ ] `compat/jei/FeedbackJeiPlugin.java:139-147` — catalysts are missing `IMPROVED_FURNACE`, `KILN` and `ANNEALING_FURNACE`. The comment says "five".
- [ ] `data/feedback/advancement/recipes/` — 18 recipes are never unlocked: `bearing`, `belt`, `blast_furnace`, `copper_tubing`, `drill_press`, `furnace`, `grinding_wheel`, `large_pulley`, `lathe`, `lathe_tool_bit`, `mechanical_press`, `metal_swarf_smelting`, `rolling_mill`, `small_pulley`, `smoker`, `steel_cable`, `valve`, `wire_drawer`.
- [ ] `compat/jade/FeedbackJadePlugin.java:51` — only the hammer gets a workpiece/condition card. Generalise it to the shop machines.
- [ ] `client/MechanicalHammerRenderer.java` — the six machine-shop machines have no workpiece renderer.
- [ ] `compat/jei/RemovalCategory.java` — a near copy of `DeformationCategory`, so extract the shared helpers. `:126` should label hardness as the floor the tool must beat.
- [ ] `control/programmer/ProgrammerLinkedPayload.java:55` — common code references `ProgrammerScreen` from a lambda. Route it through a `client/` handler. `ProgrammerScreen` and `ThermalVesselScreen` live outside `client/`.

## 4. Unit-rule and quality slips

- [ ] `process/Casting.java:62,66,80` — bare `float solidifyTemperature`. Make it `Tu` via `Units.codec`.
- [ ] `process/VanillaFallback.java:87,120` — `find(..., float currentTemperature)` seam should take `Tu`.
- [ ] `core/FTuning.java:977,980` — `PRESSURE_VESSEL_DANGER_PU`/`CRITICAL_PU` should be `Pu`. `:1153,1162` — the resolution constants should be `Tu`.
- [ ] `core/thermal/Heat.java:93-94,108-109` — `ta`/`tb` and `conductance`/`leak` are unwrapped into same-typed local pairs, exactly the swapping hazard the rule names.
- [ ] `core/rotation/RotationPropagator.java:126,169` — single-float unwrap into locals. Inline them, or relax the rule's wording.
- [ ] `core/rotation/RotationNetwork.java:81,141` — `Map<RotationNode, Float>` boxes floats, its values are never read, and `updateLoadOf` has no callers. Make it a `Set`. `sources` caches capacity while load is read live, so the two are inconsistent.
- [ ] `core/rotation/RotationPropagator.java:84-88` — `onRemoved` re-flood-fills, and `onLoad` rebuilds per node, which is O(n²) on world load.
- [ ] `core/FTuning.java:1210,1238` — public mutable arrays `HEAT_BAND_TOPS` and `TIMER_SETTINGS`. Use `List.of`.
- [ ] `core/FTuning.java:31` — `core` imports `process.Removal` only for javadoc. Use a fully qualified link.
- [ ] `process/Quench.java:66`, `Fuel.java:75`, `Casting.java:70`, `Dip.java:84` — STREAM_CODECs that nothing uses. Delete them.
- [ ] `control/program/ProgramGraph.java:71-74` — an unwired input is reported as `dangling_wire`. Add an `unwired_input` key.
- [ ] `control/program/ComparatorNode.java:71`, `instrument/Quantity.java:78` — `values()[i]` throws on a bad ordinal (use `ByIdMap`), and node id -1 is not rejected.
- [ ] `control/programmer/ProgrammerScreen.java:756` — accepts NaN and Infinity literals.
- [ ] `control/programmer/ProgrammerBlockEntity.java:125`, `ProgrammerScreen.java:400` — the client never learns of a link dropped by `recheckLink`. `:373-376` — the closing commit is dropped silently when `stillValid` fails.
- [ ] `control/controller/ControllerBlockEntity.java:96,104` — sensors are resolved twice per changed tick.
- [ ] `machine/grindingwheel/GrindingWheelBlockEntity.java:105` — copy-pasted `CrankLinkageBlockEntity.isDriving` check.
- [ ] `process/Quenching.java:93` — return early when the stack is at ambient.

## 5. Stale docs and comments

- [ ] `core/unit/package-info.java:26,116-117` — says Pu is "not yet in any code" and leaves it out of the live table.
- [ ] `core/unit/package-info.java:122-127` — says the datapack records are still floats; they are typed.
- [ ] `core/unit/package-info.java:115` — lists hardness as untyped, but it is `St`. Pick one and document it (it makes `Fu = St/hardness` dimensionless).
- [ ] `Feedback.java:96-100` — `onAddReloadListeners` says nothing syncs to the client. `:129` names only the deformation table.
- [ ] `process/DeformationTable.java:44-49` — says the table is server-only with nothing to show in JEI; it is now synced and has a JEI category.
- [ ] `process/HandDeformationRecipe.java:37` and CLAUDE.md "Outstanding work" item 3 — say "four tables"; there are eight.
- [ ] `process/Deformation.java:99-117,129-133` — orphaned or stacked javadoc blocks.
- [ ] `process/ThermalProcess.java:87-95` — a sentence split by an inserted paragraph. `:163` says "eight" fields; there are eleven.
- [ ] `process/AlloyRange.java:42` — cites a tolerance that `AlloyMix` doesn't have.
- [ ] `process/Deforming.java:49` — "four ways"; there are three.
- [ ] `core/thermal/Heat.java:63-66` — the comment says "inline, not locals", and the next line is a local.
- [ ] `control/program/SensorNode.java:48`, `feedback_controller_spec.md:71` — reference the removed `ProgrammerBlockEntity#setInput`. That logic is now `ProgramGraph#wireError`.
- [ ] `control/data/DataNode.java:46-50` — says the overlay isn't built and the reference targets a newer Minecraft.
- [ ] `fitting/SensorFitting.java:28` — calls the bimetallic strip a sensor fitting.
- [ ] `machine/vessel/VesselKind.java:26-31`, `ThermalVesselBlockEntity.java:77-79` — name three vessels; there are six.
- [ ] `feedback_machine_shop_spec.md:317` — says `Operation.REMOVE` exists and `find` matches on input alone.
- [ ] `machine/lathe/LatheBlockEntity.java:71`, `registry/FItems.java:75`, `GrindingWheelBlockEntity.java:48`, Pulley doc — wrong "Nth implementer of Wearing" counts. The lathe bit doesn't implement it at all.
- [ ] `machine/pulley/PulleyBlockEntity.java:74` — the `transmitting` comment says "slipped".

## Verified clean

- No references remain to the deleted piston package or `ProgrammerAction`/`ProgrammerActionPayload`.
- Every block has a blockstate, a self-dropping loot table, a lang key, a block item and a creative-tab entry. Blockstate properties match the classes.
- No JSON parse errors. Recipes and advancements reference only existing ids.
- The JEI and Jade plugins can't load without their host mod.
- No Create assets: every texture is vanilla.
- The thermal Euler step is stable at the current constants (worst case 0.65/15).
- Payload handlers enqueue onto the main thread.
