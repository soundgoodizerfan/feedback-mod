#!/usr/bin/env python3
"""Simulate Feedback's chamber-gun bullet math (GunSpec.bake + GunShots damage) outside the game.

Reads GunTuning.java constants and data/feedback/material/*.json live, so editing either is
picked up on the next run. Override a constant without editing: --set GUN_DAMAGE_PER_WORK=0.02.

Port of: GunSpec.bake, Cartridge.Blend, GunHazard.assess, GunShots hit damage.
Not covered: options (fittings), wear, traits, perks, armour.
Keep in step with GunSpec.bake by hand; the Java is the truth.

  gun_sim.py                         default sweep (calibre x barrel material x length)
  gun_sim.py --barrel steel --bore 12 --outer 20 --length 0.45 --bullet lead --charge 4
  gun_sim.py --set GUN_WORK_PER_PU_M3=2e7 --sweep-bullets
"""
import argparse, json, math, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FT = ROOT / "src/main/java/io/github/soundgoodizerfan/feedback/weapon/GunTuning.java"
FT_SHARED = ROOT / "src/main/java/io/github/soundgoodizerfan/feedback/core/FTuning.java"
MATS = ROOT / "src/main/resources/data/feedback/material"

# BulletShape: name -> (drag, damage, pierce, wear, pelletMm, maxPellets)
SHAPES = {"bullet": (1, 1, 1, 1, 0, 1), "round_ball": (1.35, 1, .85, 1.4, 0, 1), "flat_nose": (1.15, 1.1, .9, .6, 0, 1),
          "pointed": (.65, .9, 1.4, .8, 0, 1), "hollow_point": (1, 1.45, .55, 1, 0, 1)}


def load_tuning(overrides):
    t = {}
    for m in re.finditer(r"\b(?:float|int|double)\s+([A-Z_0-9]+)\s*=\s*(-?[\d.]+(?:e-?\d+)?)f?\b", (FT.read_text() + FT_SHARED.read_text())):
        t[m.group(1)] = float(m.group(2))
    # multi-declarations: "float A = 1f, B = 2f;" -> pick up the trailing names too
    for m in re.finditer(r",\s*([A-Z_0-9]+)\s*=\s*(-?[\d.]+(?:e-?\d+)?)f?\b", (FT.read_text() + FT_SHARED.read_text())):
        t.setdefault(m.group(1), float(m.group(2)))
    for o in overrides:
        k, v = o.split("=")
        t[k] = float(v)
    return t


def mat(name):
    return json.loads((MATS / f"{name}.json").read_text())


def max_charge(T, bore):
    return max(1, min(T["GUN_CHARGE_CEILING"], round(T["GUN_CHARGE_MAX"] * (bore / T["GUN_CHARGE_REF_BORE_MM"]) ** T["GUN_CASE_ROOM_EXPONENT"])))


def bake(T, barrel, bore, outer, length, bullet, charge, shape="bullet", loose=0.0, case="brass", powder="gunpowder"):
    """Return dict of figures. bore/outer mm, length m, charge = count of nominal powder items."""
    bm, bl = mat(barrel), mat(bullet)
    yield_ = bm["mechanical"]["yield_strength"]
    wall = (outer - bore) / 2
    if wall < T["GUN_BARREL_MIN_WALL_MM"]:
        return None
    area = math.pi / 4 * (bore / 1000) ** 2
    rating = yield_ * wall / (bore / 2)
    p = mat(powder)["combustion"]["chamber"]
    scale = (bore / T["GUN_CHARGE_REF_BORE_MM"]) ** T["GUN_CASE_ROOM_EXPONENT"]  # a wider case holds more: the same count is thinner
    peak = p["peak_pressure"] * charge / T["GUN_CHARGE_NOMINAL"] / scale  # balanced blend: burned = 1
    peak *= 1 + max(0, -loose) * T["GUN_BORE_SQUEEZE_PER_MM"]
    leak = max(T["GUN_BORE_LEAK_FLOOR"], 1 - max(0, loose) * T["GUN_BORE_LEAK_PER_MM"])
    burned = min(1.0, p["burn_rate"] * length)
    geo = min(peak, rating) * T["GUN_PRESSURE_FILL"] * area * length * burned * T["GUN_WORK_PER_PU_M3"] * leak
    # the powder is the energy budget. Barrel geometry says how much of it a gun can use;
    # a soft min so a bigger bore or longer barrel never gains past what the charge holds.
    cap = T["GUN_WORK_PER_NOMINAL_CHARGE"] * (charge / T["GUN_CHARGE_NOMINAL"]) * p["peak_pressure"] / T["GUN_PROPELLANT_PEAK_PU"]
    work = geo * cap / (geo + cap) if cap > 0 else geo
    roundm = (bore - loose) / 1000
    bullet_mb = math.pi / 4 * roundm ** 2 * (roundm * T["GUN_PROJECTILE_LENGTH_PER_BORE"]) * 1000
    bullet_mu = bullet_mb * bl["density"]
    exit_ms = math.sqrt(2 * work / bullet_mu)
    impulse = bullet_mu * exit_ms * (1 + T["GUN_GAS_IMPULSE_FRACTION"])
    tube = math.pi / 4 * ((outer / 1000) ** 2 - (bore / 1000) ** 2) * (length + 0.1) * 1000 * bm["density"]
    frame = (T["GUN_RECEIVER_METAL_MB"] + T["GUN_SLIDE_METAL_MB"] + T["GUN_TRIGGER_METAL_MB"] + T["GUN_GRIP_METAL_MB"]) * 7.87
    gun_mu = tube + frame
    kick = T["GUN_KICK_DEG_PER_IMPULSE"] * impulse * (T["GUN_KICK_REFERENCE_MU"] / gun_mu)
    s = SHAPES[shape]
    case_rating = mat(case)["mechanical"]["yield_strength"] * T["GUN_CASE_WALL_PER_RADIUS"]
    hazard = max(peak / rating, peak / case_rating)
    return dict(work=work, speed=exit_ms / 20, damage=work * T["GUN_DAMAGE_PER_WORK"] * s[1], peak=peak, rating=rating,
                hazard=hazard, mass=gun_mu, bullet=bullet_mu, kick=kick, vented=peak > rating,
                hardness=bl["hardness"] * s[2])


HDR = f"{'barrel':<15}{'bore':>5}{'out':>5}{'len':>5} {'bullet':<10}{'chg':>4}{'work':>8}{'b/t':>6}{'dmg':>7}{'hp(2/h)':>8}{'peak':>6}{'rate':>7}{'haz':>6}{'gunMu':>7}{'kick':>6}"


def row(a, r):
    if r is None:
        return f"{a['barrel']:<15} (no wall)"
    flag = "!" if r["hazard"] > 1 else " "
    return (f"{a['barrel']:<15}{a['bore']:>5g}{a['outer']:>5g}{a['length']:>5g} {a['bullet']:<10}{a['charge']:>4g}"
            f"{r['work']:>8.1f}{r['speed']:>6.1f}{r['damage']:>7.2f}{r['damage'] / 2:>8.2f}{r['peak']:>6.2f}{r['rating']:>7.2f}"
            f"{r['hazard']:>5.2f}{flag}{r['gun_mu'] if 'gun_mu' in r else r['mass']:>7.2f}{r['kick']:>6.1f}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--set", action="append", default=[], metavar="NAME=VAL")
    ap.add_argument("--barrel"), ap.add_argument("--bore", type=float), ap.add_argument("--outer", type=float)
    ap.add_argument("--length", type=float), ap.add_argument("--bullet", default="lead")
    ap.add_argument("--charge", type=float, default=None), ap.add_argument("--shape", default="bullet"), ap.add_argument("--case", default="brass")
    ap.add_argument("--safe", action="store_true", help="hide rows where peak exceeds rating (hazard>1)")
    ap.add_argument("--sweep-bullets", action="store_true")
    ap.add_argument("--max-charge", action="store_true", help="use GUN_CHARGE_MAX instead of nominal")
    a = ap.parse_args()
    T = load_tuning(a.set)
    print(f"DAMAGE_PER_WORK={T['GUN_DAMAGE_PER_WORK']} WORK_PER_PU_M3={T['GUN_WORK_PER_PU_M3']} FILL={T['GUN_PRESSURE_FILL']} "
          f"charge={'max for bore' if a.max_charge else a.charge or 'nominal for bore'}  (dmg is hp; 'b/t' blocks per tick; haz>1 = overpressure, marked !)")
    print(HDR)
    barrels = [a.barrel] if a.barrel else ["lead", "iron", "bronze", "steel", "titanium", "tungsten", "netherite"]
    bores = [a.bore] if a.bore else [5, 9, 12, 20]
    lengths = [a.length] if a.length else [0.1, 0.2, 0.45, 0.8]
    bullets = ["lead", "iron", "tungsten", "netherite"] if a.sweep_bullets else [a.bullet]
    for b in barrels:
        for bore in bores:
            # outer: given, else a thick wall (5 mm) which is the strongest the thick variant gives
            outer = a.outer or min(40, bore + 2 * T["GUN_WALL_THICK_MM"])
            charge = a.charge or (max_charge(T, bore) if a.max_charge else round(T["GUN_CHARGE_NOMINAL"] * (bore / T["GUN_CHARGE_REF_BORE_MM"]) ** T["GUN_CASE_ROOM_EXPONENT"]))
            for ln in lengths:
                for bu in bullets:
                    args = dict(barrel=b, bore=bore, outer=outer, length=ln, bullet=bu, charge=charge)
                    r = bake(T, b, bore, outer, ln, bu, charge, a.shape, case=a.case)
                    if r and a.safe and r["hazard"] > 1:
                        continue
                    print(row(args, r))
        print()


if __name__ == "__main__":
    sys.exit(main())
