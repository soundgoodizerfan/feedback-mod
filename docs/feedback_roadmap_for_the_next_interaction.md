# roadmap: next development session

this document outlines the planned objectives for the next development session of feedback-mod.

## 1. import silent gear assets as placeholders

import assets from silent gear to serve as placeholders for the modular system.

- silent gear is mit-licensed, so the assets may be reused with appropriate attribution.
- the assets in question are, for the most part, recolored and categorized rips of vanilla assets.

//okay this one's actually already mostly done, but we need variants to have textures too. maces should look like maces, not tridents should be tridents, etc etc.

## 2. material traits

materials should provide qualitative traits in addition to their base statistics.

### design principle

numerical properties should not be implemented as traits. traits should be qualitative in nature. for example, lead being dense is a stat, not a trait.

### proposed material traits

| material | proposed trait |
| --- | --- |
| silver | grants one free level of smite. |
| blaze rod (handle) | grants one level of fire aspect, but burns the wielder if they are not wearing gloves (or alternatively just keeps the weapon at a heat)|
| prismarine (tip) | grants one free level of impaling, applicable to all targets. |
| echo shard | to be determined; should provide a distinctive and interesting effect. |

## 3. armed skeletons

add a mixin that allows skeletons to occasionally spawn with guns and to use them competently.

**note:** this item may be better deferred to the todo list. it should be implemented in the same pass that adds gun blueprints to chests as a rare loot item, since skeletons should draw from that same pool of blueprints.

## 4. combat system

decide whether to adopt a semi-novel combat system and write up a lil spec.

- the other maintainer favors adopting the [better combat](https://www.curseforge.com/minecraft/mc-mods/better-combat-by-daedelus) mod.
- i am more partial to an approach similar to [project war dance](https://www.curseforge.com/minecraft/mc-mods/project-war-dance).
- really, we ought to approach a middleground

## 5. make haste make you reload faster

and also make it up the speed of fan firing, and attack speed, and other things of that nature

## 6. make it so that javelin hooked entities take more damage

also make it so that you can one-hand a shotgun if its light enough #roadhog
or design it in whatever way.
like, imagine a one-handed shotgun in real life (for a guy as strong and dense as steve)
then imagine how you would represent that in our system

but yeah giving javelin some vulnerability would make synergies really cool
and i like the idea of emergent synergies

## 7. decide if this is good

get a look at this: https://github.com/sosadly/blockbench-mcp
we might want it for guns or something