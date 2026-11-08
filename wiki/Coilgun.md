# Coilgun

A [gun](Guns.md) that throws a magnetic slug with a wound coil instead of burning powder. You make it on the [Gun Table](Gun_Table.md) with a **Coil Barrel** and a **coil winding**.

## Building

A coilgun is assembled from [parts](Gun_Parts.md) like any gun, with a **Coil Barrel** ([Guns](Guns.md#barrels-and-launchers)) and a **winding**. The winding is required on a coil barrel, and any conducting metal in block or wire form will do: copper, silver, gold, **Redstone Chromium**. A powder or an insulator cannot hold the field and is shown struck out in the Blueprint Table.

The winding's metal sets two limits. The field it holds before it gives is its **yield strength times its ampacity**, so copper holds more than chromium; the barrel's own wall holds too, and the lower of the two counts. The temperature it quenches at is the metal's **service temperature**, so chromium runs hotter than copper before it quits.

## Ammunition

There are two ways to drive a coil.

### Redstone rounds

Seated at the [Reloading Press](Reloading_Press.md) like any other round, with two differences:

- **The charge is Redstone Dust**, alone. It can't be blended with gunpowder or blaze powder, and the press won't seat it if you try.
- **The bullet has to be magnetic: iron, in practice.** A copper, lead or gold slug couples at only a tenth of the strength; the coil just doesn't grip it.

**More redstone gives a much stronger field.** It goes as the square: in a 9 mm case the field pressure is **3 Pu × (dust ÷ 4)²**. Four dust makes 3 Pu, six make 6.75, eight make 12. The gun's tooltip shows the *Winding* line: the field a standard four-dust charge makes against what the winding can hold. Go past what the winding can hold and you damage the gun exactly as [overpressure](Guns.md#overpressure) damages a powder gun.

How hard a dust drives depends on the case, not the count: the drive is read against the case's room, so a wider case needs more dust for the same push. A 9 mm coilgun on the standard charge hits for about 4, 9 and 17 damage at 0.2, 0.45 and 0.8 m of barrel. **Length is power:** every stage along the barrel adds as much as the last, so a long coil throws a slug much harder than a short one. Only the winding's rating and its quench temperature stop it.

### Jar rounds

A round seated **dry** (no charge) is a **jar round**, and it's charged with electricity on a [Round Charger](Round_Charger.md). A jar round at **6 V** drives the coil exactly as hard as four redstone dust. Its energy rises in step with its voltage, and its pressure on the winding as the square. Jar rounds go flat over **5 minutes**.

You can still give coilgun rounds a [payload](Ammunition.md#payloads), so tracers and incendiary rounds both work.

## How it differs from a powder gun

- **No smoke, no cook-off.** There's no powder to catch, so a hot coilgun never fires on its own. It throws sparks at the muzzle instead of smoke.
- **Quench.** The winding stops working at its metal's service temperature (300 Tu for copper, 500 Tu for Redstone Chromium). If you keep firing, eventually the trigger does nothing and you get *"The winding has quenched: let it cool."* Wait, and it comes back.
- **Barrel length.** There's no powder to burn, so length is simply more stages: a short coilgun is weak, a long one hits hard.
- **No gas, little kick.** The only recoil is the slug's own momentum, with no muzzle blast.
- **Winding heat.** Every shot also heats the gun by the current its wire carries. A wire that carries less current (lower ampacity) heats the gun faster: Redstone Chromium (0.3) runs far hotter per shot than copper (1.0), but quenches at 500 Tu against copper's 300.
- **Hot slugs.** A slug stops being magnetic above its Curie temperature (nickel 355 Tu, iron 770 Tu). A barrel that hot throws it as a non-magnetic slug, at a tenth of the strength.
- Heat, wear and recoil otherwise work the same as on any gun.
