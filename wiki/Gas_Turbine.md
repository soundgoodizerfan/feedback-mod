# Gas Turbine

A rotation source that burns fuel gas continuously. Put it on a [shaft](Shaft.md) and it drives your [mechanical network](Rotation.md) like a [Steam Engine](Steam_Engine.md), but it is much stronger: at full spool it turns **240 RPM** and supplies up to **300 Su**.

## Using it

- It is placed with its shaft along the face you click, and **both ends connect**, like an [Engine](Engine.md).
- **Fuel.** It takes fuel from any fluid tank or [Pipe](Pipe.md) touching it, and holds 2000 mB. A bucket works too. Any fluid that burns will do: it never filters. See [Fuels](Fuels.md). A hotter fuel gives **more power for less fuel**.
- **Blades.** It will not run without blades. Right-click with a metal part (any item made of a material; a plate is the usual choice) to fit one, and **sneak-right-click with an empty hand** to take it out. The blades' material is what the fuel's flame is held against, below.
- **Right-click with an empty hand** to read the *spool* (a percentage of full speed), the *blades'* condition and how much unburnt fuel is *flooded* in the burner.

## Starting it

A gas turbine **cannot light itself**. It lights only while something else on the same network is already turning it at **20 RPM or more**: a [Motor](Motor.md) or a [Hand Crank](Hand_Crank.md). Once it has spooled up to about **20%**, it holds itself and the starter can stop.

It spools up slowly after that, and when the fuel runs out it **coasts down very slowly**. Its output follows the spool, not the fuel gauge.

## Blades

The blades take two kinds of punishment. Both only wear them (down to 20% condition, with output falling with it); neither ever stops the turbine outright.

- **Speed.** The faster the shaft turns, and the heavier the blade metal, the harder the blades are pulled apart. The blade metal's **strength** has to hold: a light, strong metal survives a higher speed. A hot flame cuts that strength. A turbine geared up so its shaft spins too fast wears its blades; an [Overspeed Trip](Overspeed_Trip.md) on the shaft is what prevents it.
- **Heat.** A fuel whose flame is hotter than the blade material's *service temperature* wears the blades, faster the further over. Check the fuel's flame temperature against the metal ([Materials](Materials.md)): naphtha burns at 1400 Tu, well over what iron stands.

## Flameout

If the spool is under the self-sustaining level, and nothing else is turning the shaft at 20 RPM, while fuel is still coming (a turbine that coasted down with the line left open), the flame goes out and the fuel **floods the burner**: up to 400 mB of unburnt fuel collects, and it gives off **fuel vapor** around the turbine. It will not relight until the flood has cleared, which only happens once the **fuel tank has run dry**. Shut the line off.

## Notes

- Fuel is burned at 2 mB a tick at full spool for a standard fuel.
- A [Flare Stack](Flare_Stack.md) is the safe place to send gas the turbine does not need.
