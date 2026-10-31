# Making Aluminum

A path from bauxite to molten aluminum: dissolve the ore in lye, then split the alumina in a molten cryolite bath. Bauxite does not smelt into aluminum directly.

1. **Get lye.** Dissolve Salt in water in a [Reactor](Reactor.md) to make [brine](Chemicals.md#brine), then split the brine in an [Electrolyser](Electrolyser.md) (2.2 V). Lye stays in the cell; draw it off a side face. The chlorine and hydrogen it also makes need somewhere to go ([Chemicals](Chemicals.md#chlorine)).
2. **Make alumina.** Put any bauxite item (see [Ore Deposits](Ore_Deposits.md)) and 250 mB lye in a Reactor at about 200 Tu. Two alumina come out per bauxite, and the lye is returned. One Lapis Lazuli gives one alumina instead, and its stored experience is lost.
3. **Melt cryolite.** Cryolite Ore melts in a Reactor at 1010 Tu into the [cryolite bath](Chemicals.md#cryolite-bath). The Electrolyser needs the bath at 950 Tu or hotter, and about 500 mB of it.
4. **Split the alumina.** Fill the Electrolyser with bath, connect power (4.0 V needed, 6 V rating) and feed alumina by hand or from a hopper. Each alumina gives **72 mB** of molten aluminum at the cathode (back) and oxygen at the anode (front). A cold cell takes current to warm before it splits anything.

**Failure points.** A bath with no alumina left is an **anode effect**: the bath drains, fumes poison anyone nearby and the cell loses condition. Keep alumina going in or cut the power. Full details are on [Electrolyser](Electrolyser.md#aluminum-bath).
