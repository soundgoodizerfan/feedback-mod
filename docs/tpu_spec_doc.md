# What TPu Means

TPu \= Thermal Process Units.

A TPu value represents progress toward completing a particular thermal process.

It should not be interpreted as:

heat energy, temperature, time, joules, or any universal physical quantity.

For example:

steelmaking requires 500 TPu   
cooking beef requires 100 TPu

The 500 and 100 do not represent equivalent amounts of real physical energy. They are process-local progress quantities.

The abstraction exists because Minecraft has discrete item states while the real phenomena being modeled are continuous and varied.

A thermal transformation might involve:

- a chemical reaction  
- a phase change  
- structural changes in a material  
- cooking reactions in food  
- sintering  
- annealing  
- some combination of these

TPu allows all of these to be represented by the same internal mechanism without claiming that they share one real-world measurable quantity.

# 

# Why TPu Is Not Player-Facing

Feedback already has a player-reckonable deformation abstraction in Fu.

Fu represents a quantity of deformation that can meaningfully be observed and measured in-world. A player can apply deformation with their hands, and the Calipers can quantify it.

TPu is fundamentally different.

There is no single physical measurement that corresponds to "thermal process progress" across arbitrary processes. The same TPu concept may represent different real phenomena depending on the material and process.

For that reason:

there should be no TPu display, no TPu measurement tool, no TPu tooltip, and no player-facing TPu unit.

TPu exists primarily so the code can represent the state of an ongoing transformation.

The name is useful precisely because it explains its meaning in code without implying that it is a real physical measurement.

# The Problem With the Existing System

The current thermal process system uses holdTime as the fundamental completion quantity.

Conceptually, a process behaves like:

remain in the correct temperature range ↓ holdTime++ ↓ holdTime \>= required hold time ↓ complete

This makes time a recipe property.

That creates several problems.

## Fixed Process Duration

A recipe can effectively say:

steel \= 30 seconds

regardless of the thermal environment.

That is undesirable for Feedback because process quality should affect how quickly a transformation occurs.

A superior machine, better thermal environment, hotter useful conditions, or other improvements should be able to produce the same transformation more quickly.

## holdTime Does Not Represent Physical Progress

Elapsed time alone does not tell us how much useful transformation has occurred.

Ten seconds in an ideal thermal environment should not necessarily be equivalent to ten seconds in an extremely poor one.

## The Current Fallback Uses Fake Work

The existing fallback path effectively accumulates progress using temperature relative to ambient:

work \+= Math.max(0, temperature \- ambient)

That quantity has the dimensions of temperature multiplied by time. It is not actually Work.

Temperature is a state variable, not an energy quantity, so Tu × ticks should not be treated as universal energy.

The new system should not use temperature directly as fake Work.

# TPu Replaces holdTime

The new process model should replace:

holdTime

with:

requiredTPu

A process completes when:

accumulatedTPu \>= requiredTPu

The recipe therefore defines how much transformation is necessary, rather than how long the machine must run.

The machine instead determines how quickly that progress is produced.

Conceptually:

TPu/t \= process-dependent response to current thermal conditions

and:

time \= requiredTPu / TPu-per-tick

Time becomes an outcome rather than a recipe constant.

This means the same process can take different amounts of time under different conditions.

## TPu Accumulation

An active thermal process maintains an internal value:

currentTPu

Each tick, the machine calculates a progress rate:

TPu/t

and adds it to the process:

currentTPu \+= TPuPerTick

The process completes when:

currentTPu \>= requiredTPu

The accumulated TPu should be saved as part of the machine's persistent state so that progress survives chunk unloading and server restarts.

TPu belongs to the active thermal transformation rather than being a universal property of the item itself.

## Temperature Still Matters

Replacing holdTime with TPu does not make temperature irrelevant.

Temperature determines whether the process can occur and how efficiently it proceeds.

At a high level:

temperature \< minimum → no TPu accumulation

minimum \<= temperature \<= valid upper range → TPu can accumulate

temperature \> spoil ceiling → thermal failure

The process should also have an optimal temperature.

This separates three concepts:

minimum temperature \= below this, the process does not meaningfully occur

optimal temperature \= conditions under which the process advances most efficiently

spoil ceiling \= above this, the material suffers thermal failure

A process can therefore be valid over a range of temperatures while still having a preferred operating point.

For example:

600 Tu   → slow 700 Tu   → good 750 Tu   → optimal 800 Tu   → good 900 Tu   → poor 1000 Tu  → spoil

The exact curve is a tuning decision and should not be hardcoded as a universal physical law.

TPu Rate

The intended structure is:

TPu/t \= thermal input / useful thermal response × process-specific temperature suitability

At a high level:

thermal energy transfer ↓ current thermal state ↓ process suitability ↓ TPu/t

TPu should therefore not be calculated directly from absolute temperature.

Avoid models such as:

TPu \+= temperature

or:

TPu \+= temperature \- ambient

because those reduce the system to disguised temperature × time.

The exact TPu rate equation still needs to be designed.

The important architectural rule is that TPu is process progress generated by thermal conditions, rather than another form of energy.

## TPu and Work Are Not the Same Thing

Work remains Feedback's universal energy-accounting abstraction.

It exists so that different energy systems can be compared and conversions can obey conservation constraints.

The intended relationship is:

chemical / electrical / mechanical / thermal energy ↓ Work ↓ energy transfer ↓ Tu ↓ thermal process response ↓ TPu

Work answers:

*How much energy was transferred?*

TPu answers:

*How much progress has this particular transformation made?*

There should be no universal equation such as:

1 TPu \= 1 Work

or:

TPu \= some amount of energy

because that would turn TPu into a physical quantity that it is explicitly not intended to be.

A process may convert some amount of thermal energy transfer into some amount of TPu without creating a universal equivalence between the two.

## TPu May Decay

TPu should not necessarily be permanently retained when a process is interrupted.

The intended behavior is:

inside valid temperature conditions → TPu accumulates

outside useful conditions → TPu may dissipate

above spoil ceiling → thermal failure

This prevents a process from being trivially paused and resumed without consequence.

For example:

0 TPu → 40 TPu → 80 TPu remove heat → TPu begins to decay return later → process must rebuild some of the lost progress

The exact decay rule remains a tuning decision.

## TPu Should Not Necessarily Be Monotonic

Because TPu represents process state rather than elapsed time, it can move backward when conditions become unfavorable.

A process may therefore behave like:

0 → 40 → 80 → 30 → 70 → 100

This is intentional.

It allows poor thermal control to waste process progress without requiring every failure to immediately destroy the item.

# Vanilla Cooking Compatibility

VanillaFallback should continue to exist for mod compatibility.

The important change is what vanilla cooking data means.

Vanilla cooking recipes provide useful compatibility information:

input, output, baseline cooking time, recipe type.

They should not directly become Feedback machine instructions.

Instead, vanilla information should be converted into a generalized thermal process.

Vanilla cooking time can be used to establish a baseline amount of required TPu.

For example:

200 vanilla ticks → baseline TPu requirement 100 vanilla ticks → proportionally smaller baseline requirement 400 vanilla ticks → proportionally larger baseline requirement

Once converted, the machine does not count vanilla ticks.

The actual duration emerges from:

requiredTPu / actualTPu-per-tick 

## Vanilla Recipe Types Become Thermal Hints

The three vanilla cooking recipe types should be interpreted as semantic hints rather than hard machine rules.

### Smelting

A generic thermal transformation.

This provides the default assumption when no more specific thermal information is available.

### Smoking

A strong signal that the transformation is food-like and should generally prefer lower temperatures.

Fallback smoking processes should therefore receive:

an appropriate lower thermal operating range, a spoil ceiling, and process behavior that rewards stable, relatively low-temperature heating. 

### Blasting

A strong signal that the transformation is metallurgical or otherwise suited to higher temperatures.

Fallback blasting-compatible processes should therefore receive:

a higher preferred temperature, no food-style spoil ceiling by default, and process behavior that benefits from hotter thermal environments.

This is a heuristic for compatibility, not a claim that every modder used the vanilla recipe type for scientifically accurate reasons.

### Do Not Let the Shortest Vanilla Recipe Win

The current VanillaFallback behavior effectively chooses the shortest matching recipe among smelting, blasting, and smoking.

That is the wrong abstraction.

For example:

iron ore: smelting \= 200 ticks blasting \= 100 ticks

should not become:

iron ore requires the 100-tick process everywhere

Doing so makes blast-furnace specialization a hidden recipe rule.

Instead, the recipe types should be used to infer the thermal character of the transformation.

The actual speed should emerge from the thermal environment.

A hotter, more thermally efficient machine should process a high-temperature metallurgical transformation faster because the material reaches useful conditions sooner and remains there more effectively.

Furnace, Smoker, and Blast Furnace

The intended specialization of the vanilla-style thermal vessels is emergent.

## Furnace

The furnace provides a middle-of-the-road thermal environment.

It should be capable of handling both food-like and metallurgical processes, but should not be especially optimized for either.

## Smoker

The smoker provides a lower and more stable thermal environment.

This should naturally make it a reliable environment for food-like thermal processes:

lower operating temperature → less overheating → more time in the useful food-processing range → efficient TPu accumulation

The smoker should not need an arbitrary 2× food speed rule.

## Blast Furnace

The blast furnace provides a hotter, better-insulated thermal environment.

Its intended behavior is:

higher temperature \+ lower heat loss → faster heating into high-temperature ranges → faster TPu accumulation for suitable metallurgical processes

Food should generally exceed its spoil ceiling in a blast furnace and fail rather than becoming an ordinary cooked food.

Again, this should emerge from the thermal environment rather than an explicit:

if (isOre && vessel \== BLAST\_FURNACE) speed \*= 2; 

# Fuel and Work

The fuel system is intended to eventually participate in the universal Work accounting system.

The design goal is:

chemical energy   
↓   
conversion losses   
↓   
thermal energy / Work   
↓   
thermal transfer   
↓   
process

A fuel should therefore represent a finite energy budget.

For example, if a fuel contains some quantity of chemical energy and combustion is less than perfectly efficient, the resulting available thermal energy should be lower.

That allows the conservation rule:

No chain of conversions should produce more universal energy than was present in the original source.

The current fuel implementation primarily models burn duration and temperature. Its energy budget is not yet a complete Work-based accounting system.

The existing fallback Work calibration should therefore be treated as a tuned reference rather than as proof that the current system already conserves energy across fuel conversions.

# Work vs. Tu vs. TPu vs. Fu

Feedback now has four intentionally different conceptual layers.

### Physical / measurable quantities

Examples:

Tu Su RPM Eu XPu

These represent meaningful state or measurable quantities within the world model.

### Derived quantities

Examples:

throughput conductivity voltage amperage

These are relationships between other quantities rather than independent substances.

Qualitative process abstractions

Examples:

Fu TPu

These represent gameplay-relevant qualitative progress.

Fu describes mechanical deformation progress toward a discrete material state.

TPu describes thermal transformation progress toward a discrete process result.

### Universal Work

Work is the energy-accounting layer.

It exists primarily to make conversions auditable and prevent the system from creating free energy when moving between energy forms.

It is not intended to be a player-facing stat and should not be directly equated to qualitative progress abstractions.

# Final Intended Model

The completed thermal system should conceptually look like this:

```
                     ENERGY SYSTEM
                            │
                          Work
                            │
                  thermal energy transfer
                            │
                           Tu
                            │
             ┌────────────┴────────────┐
             │                              │
      thermal validity             thermal suitability
             │                              │
             │                 	         TPu/t
             │                              │
             └────────────┬────────────┘
                            │
                     TPu accumulation
                            │
              requiredTPu is reached
                            │
                    transformation
```

The important principle is:

*Recipes specify what transformation must happen; physical conditions determine how quickly it happens.*

TPu is the internal bridge between those two ideas.

It replaces fixed thermal recipe time with a quantity of required process progress, allowing furnace quality, temperature, insulation, fuel, and other physical conditions to affect the actual duration of a process without requiring appliance-specific speed multipliers.  
