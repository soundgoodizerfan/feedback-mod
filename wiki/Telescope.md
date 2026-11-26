# Telescope

A hand-held telescope built from three parts: an objective lens, an eyepiece lens and a tube. Hold right click to look through it and see far things larger.

## Crafting

Shape the three parts at a part bench (see [Composed Parts](Composed_Parts.md)), then put them side by side in a crafting grid.

```
 Objective Lens | Eyepiece Lens | Telescope Tube
```

| Part | Axes | Stock |
| --- | --- | --- |
| Objective Lens | focal length 100 to 600 mm, lens width 20 to 80 mm | lens or gem stock |
| Eyepiece Lens | focal length 10 to 60 mm | lens or gem stock |
| Telescope Tube | length 110 to 660 mm | metal pipe or plate |

The lenses must be of a material that bends light: glass, quartz, amber or a gem (the materials with a **refractive index** on [Materials](Materials.md)). See [What a Part Slot Takes](What_a_Part_Slot_Takes.md) for what each slot accepts.

## Using it

Hold right click to look through it. The view narrows by the **magnification** inside a round scope mask, and a pale haze over the view shows the **blur**. Release to lower it. Looking does not wear it. The tooltip shows magnification, mass and the blur as built, for a tube at the Overworld's ambient temperature; the haze in the view also counts how far the tube's temperature is from that, so a steel tube is a little soft in a cold winter and much softer in the Nether or the Gap.

## How it works

- **Magnification** is the objective's focal length divided by the eyepiece's, up to 20x. A long objective with a short eyepiece magnifies most.
- **Blur** has three causes. A tube whose length is not the sum of the two focal lengths puts the lenses out of focus; the further off, the more haze. A wide objective of glass that bends light weakly blurs too: the weakness is the lens width over its focal length, over the refractive index less one. A longer focal length, a narrower lens or a material with a higher index lowers it. The third is heat: focus is set at the Overworld's ambient temperature, and a tube warmer or colder is the wrong length for its lenses. The blur added grows with the tube material's **thermal expansion** ([Materials](Materials.md#heat-and-other-figures)) and with how far the tube's temperature is from the Overworld's. The tube takes the telescope's own heat, or the ambient temperature of the dimension: the Nether is 40 Tu and the Gap 0 Tu. A steel tube turns soft in the Nether and the Gap, an aluminum tube much worse, an invar tube hardly at all.
- The tube's metal sets the mass. The tooltip lists its **Tube expansion** with the other figures.

## Sighting a planet

Three faint, unnamed points in the Overworld night sky ride the turning of the stars but drift among them from day to day: an acid cloud world, a frozen moon and a crystal world. They return every 23, 11 and 37 days, so each is up on some nights and not on others. To the eye they stay points.

Look through the telescope on a clear night (no rain or thunder) with open sky overhead, and keep the planet in the middle third of the view. A telescope strong and sharp enough resolves it as a lit disc; a poorer one shows only a haze, and nothing locks. The blur, heat included, must stay under 0.3, and the magnification must reach:

| Planet | Magnification |
| --- | --- |
| Acid cloud world | 8x |
| Crystal world | 12x |
| Frozen moon | 16x |

Your first resolved sighting plays a chime, earns the **A Light That Wanders** advancement ([Ages](Ages.md)) and gives you a finished Aerospace Treatise IV: The Wandering Lights ([Treatise](Treatise.md)) and a Sky Notebook.

### Sky Notebook

Each planet you resolve for the first time is written into your notes with a chime and the action-bar line "Noted in your sky notebook: <planet>". The notes belong to you and survive death. The notebook's tooltip lists each resolved planet in gold, with a gray line giving its color, its air and one fact about it:

| Planet | Tooltip |
| --- | --- |
| Acid cloud world | yellow-green disc in thick cloud; dense acid air where hulls corrode, wings bite and thrusters struggle |
| Frozen moon | small pale blue disc; seas of liquid gas, cryogenic coolant |
| Crystal world | violet disc with glints; geodes holding stored experience |

Under each resolved planet a last line gives its transfer window, the time until the planet lines up with the stars: "Transfer window in N days" counts down in world days, and "Transfer window open" shows for a short stretch around the alignment. Planets with longer periods come around less often.

A planet you have not seen shows a dark gray line, "An empty line, waiting for a planet."

Because the tube length is its own axis, match it to the lenses: for a 300 mm objective and a 30 mm eyepiece, a 330 mm tube.

See also [Optics](Optics.md) for lenses and the pyrometer, and [Camera](Camera.md), which takes a photograph through a telescope.
