# Coilgun

A [gun](Guns.md) that throws a magnetic slug with a redstone-wound coil instead of burning powder. You design it on the [Gun Stand](Gun_Stand.md) by setting **Launcher** to **coilgun**.

## Building

A coilgun costs ingots like any gun, **plus Blocks of Redstone** for the winding: more for a longer coil. The stand shows both counts. Craft the printed blueprint with exactly those amounts.

## Ammunition

Coilgun rounds are made at the [Reloading Press](Reloading_Press.md) like any other, with two differences:

- **The charge is Redstone Dust**, alone. It can't be blended with gunpowder or blaze powder, and the press won't seat it if you try.
- **The bullet has to be magnetic: iron, in practice.** A copper, lead or gold slug hardly moves; the coil just doesn't grip it.

**More redstone gives a much stronger field.** It climbs steeply, not evenly, so a few extra dust is a big jump. The gun's tooltip shows the *Winding* line: the field a standard charge makes against what the winding can hold. Go past what the winding can hold and you damage the gun exactly as [overpressure](Guns.md#overpressure) damages a powder gun.

You can still give coilgun rounds a [payload](Ammunition.md#payloads), so redstone or glowstone tracers and incendiary rounds both work.

## How it differs from a powder gun

- **No smoke, no cook-off.** There's no powder to catch, so a hot coilgun never fires on its own. It throws sparks at the muzzle instead of smoke.
- **Quench.** The winding stops working when it gets too hot. If you keep firing, eventually the trigger does nothing and you get *"The winding has quenched: let it cool."* Wait, and it comes back.
- **Barrel length.** A longer coil adds power, but each step adds less than the last. There's no powder that needs time to burn, so a short coilgun isn't wasting anything.
- Heat, wear and recoil otherwise work the same as on any gun.
