# ore-vein-weights

Base: `main` (JSON only, no conflicts expected with the playtest-backlog worktree branch).

## Goal
Playtest backlog [9]: copper and iron veins are the large majority of veins found. Make vein weights close to each other so silver, lead, uranium, redstone, gold etc. turn up as often as copper and iron.

## Files
Read first: `src/main/resources/data/feedback/ore_vein/copper.json` (the `weight` field).
Edit: the `"weight"` field only, in each `src/main/resources/data/feedback/ore_vein/*.json` listed below.

## Spec
Set `weight` to exactly these values (current in brackets). Copper and iron families total about 11 each, the rest are comparable.
- copper 4 [40], copper_large 3 [29], copper_porphyry 4 [36]
- iron_upper 3 [24], iron_middle 2 [8], iron_small 2 [4], iron_gossan 2 [18], magnetite_stock 2 [12]
- coal_upper 6 [46], coal_lower 5 [31]
- gold 4 [3], gold_extra 4 [40], gold_lower 3 [1]
- silver_lode 8 [4], tin 8 [6], lead 6 [6], galena_ore 5 [1], zinc 6 [6], zinc_blende 5 [8]
- uranium_lode 6 [3], graphite 6 [1], pyrite_ore 6 [1], kaolinite_deposit 6 [4]
- redstone 6 [3], redstone_lower 5 [6], lapis 5 [1], lapis_buried 4 [3], emerald 6 [27]
- diamond_small 3 [3], diamond_medium 2 [1], diamond_large 2 [1], diamond_buried 2 [3]
- nether_quartz 10 [20], nether_gold 10 [9]

## Out of scope
Radius, thickness, density, y ranges, any Java, any other field.

## Done when
- [ ] `./gradlew compileJava` passes
- [ ] every file above has the new weight and the JSON is otherwise unchanged

## Questions

## Report
