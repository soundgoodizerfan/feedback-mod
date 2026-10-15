# Coilgun

A [gun](Guns.md) that throws a magnetic slug with a redstone-wound coil instead of burning powder. You design it on the [Gun Stand](Gun_Stand.md) by selecting the **Trigger** and setting **Launcher** to **coil**.

## Building

A coilgun is assembled from [parts](Gun_Parts.md) like any gun, **plus Blocks of Redstone** for the winding: **1, 2 or 3** for a short, medium or long coil. The blueprint lists both. A coilgun with a scope and a tube can need more than nine slots; use an [Assembly Bench](Assembly_Bench.md).

## Ammunition

There are two ways to drive a coil.

### Redstone rounds

Seated at the [Reloading Press](Reloading_Press.md) like any other round, with two differences:

- **The charge is Redstone Dust**, alone. It can't be blended with gunpowder or blaze powder, and the press won't seat it if you try.
- **The bullet has to be magnetic: iron, in practice.** A copper, lead or gold slug couples at only a tenth of the strength; the coil just doesn't grip it.

**More redstone gives a much stronger field.** It goes as the square: the field pressure is **3 Pu × (dust ÷ 4)²**. Four dust makes 3 Pu, six make 6.75, eight make 12. The gun's tooltip shows the *Winding* line: the field a standard four-dust charge makes against what the winding can hold. Go past what the winding can hold and you damage the gun exactly as [overpressure](Guns.md#overpressure) damages a powder gun.

A medium coil gives a magnetic slug about **110 Work per dust**, so four dust is about a pistol's shot. A long coil adds more, but each step less than the last: at most 1.6× a medium coil.

### Jar rounds

A round seated **dry** (no charge) is a **jar round**, and it's charged with electricity on a [Round Charger](Round_Charger.md). A jar round at **6 V** drives the coil exactly as hard as four redstone dust. Its energy rises in step with its voltage, and its pressure on the winding as the square. Jar rounds go flat over **5 minutes**.

You can still give coilgun rounds a [payload](Ammunition.md#payloads), so tracers and incendiary rounds both work.

## How it differs from a powder gun

- **No smoke, no cook-off.** There's no powder to catch, so a hot coilgun never fires on its own. It throws sparks at the muzzle instead of smoke.
- **Quench.** The winding stops working at **400 Tu**. If you keep firing, eventually the trigger does nothing and you get *"The winding has quenched: let it cool."* Wait, and it comes back.
- **Barrel length.** There's no powder that needs time to burn, so a short coilgun isn't wasting anything.
- Heat, wear and recoil otherwise work the same as on any gun.
