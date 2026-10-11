# Codebase review — 2026-10-09

A full pass over `src/main` (202 Java files, ~25k lines) and all resources, made by six parallel reviewers split by package, against the working tree at `4a1c2ba` plus uncommitted changes. The build passes. The top crash and dup findings were spot-checked by hand and confirmed. Everything else was checked against the code on disk by its reviewer but has not been re-verified. Sections 1-2 and the six design rulings were fixed on 2026-10-11; what remains below is open. Tick items off or delete them as they close; delete this file once it is empty, and fold anything long-lived into `TODO.md`.

Java paths are relative to `src/main/java/io/github/soundgoodizerfan/feedback/`. Line numbers are as of this review and will drift.

## Design rulings (user, 2026-10-09)

All six are implemented (2026-10-11): wear-from-use exceptions documented in `feedback_philosophy.md` and CLAUDE.md; Press/Wire Drawer/Rolling Mill/Drill Press wear on wasted force (`machine/ShopMachineBlockEntity`); the boiler spends heat to make steam (`FTuning.BOILER_WORK_PER_MB_STEAM`); removal results have overrun entries; swarf is per-metal (`steel_swarf`, `Removal.swarf`); identity checks replaced with tags (`registry/FTags`, `FluidTags`).

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
