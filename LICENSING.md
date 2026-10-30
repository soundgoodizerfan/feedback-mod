# Licensing

Two halves, and they are not the same licence.

| | Licence | Covers |
| :--- | :--- | :--- |
| **Code** | GPL-3.0-or-later | `src/main/java`, `src/generated`, build scripts |
| **Assets** | All Rights Reserved | original art, models, sounds under `src/main/resources/assets` |

This is the split Create uses, for the reason Create uses it: the code is meant to be
learned from and built on, and the art is not a commons.

---

## Why copyleft for the code

Two reasons, and the second is the operative one.

The first is disposition. This mod exists because Create, TerraFirmaCraft and GregTech were
readable. A permissive licence would take from that pool without putting anything back into
it; copyleft is the version of gratitude that has teeth.

The second is practical and it is the one that changed a decision. **A permissive or All
Rights Reserved licence cannot take copyleft code in.** GPL, LGPL and EUPL all require the
combined work to be distributed under their terms, and neither MIT nor ARR can satisfy that.
So the old `mod_license=All Rights Reserved` setting did not protect this project from
copyleft — it forbade every copyleft source outright, which is exactly backwards from what
was wanted.

## Why GPL-3.0 specifically

**Copyleft licences are not mutually compatible**, so "some form of copyleft" is not enough
on its own — the specific choice decides what may be read *into* the project. GPL-2.0-only
and Apache-2.0 cannot be combined at all; EUPL and GPL combine only through EUPL's explicit
compatibility clause.

GPL-3.0 opens every reference mod this project wants to learn from:

| Source | Its licence | Into a GPL-3.0 Feedback? | Route |
| :--- | :--- | :--- | :--- |
| Create | MIT | **Yes** | Permissive; compatible with everything |
| GregTech CEu Modern | LGPL-3.0 | **Yes** | LGPL-3.0 §2 permits conveying under GPL-3.0 |
| TerraFirmaCraft | EUPL-1.2 | **Yes** | EUPL Article 5 + Appendix, which lists *"GNU General Public License (GPL) v. 2, v. 3"* as a Compatible Licence. The derivative may be distributed under GPL-3.0 instead of the EUPL |

**LGPL-3.0 was considered and deliberately rejected**, and the reason it was rejected is the
reason to choose GPL. LGPL's weak copyleft exists so that other people's addons may stay
closed — which is a kindness to addon authors and a hole in the thing copyleft is *for*.
GregTech chose LGPL for exactly that ecosystem-friendliness.

This project chooses the stronger reciprocity on purpose. **Anything built on this code is
free too.** A mod whose whole argument is that systems should be legible, inspectable and
open to being taken apart would be saying something incoherent if the licence let people
build closed things on top of it. The politics and the design are the same politics.

The cost is real and accepted: a closed-source addon for Feedback is not possible, and some
people will not write an addon because of it. That is the trade, chosen with open eyes.

`-or-later` so a future GPL can be adopted without hunting down every contributor.

## What this does and does not permit

**Does:** adapt code from Create, GregTech CEu Modern and TerraFirmaCraft, subject to the
obligations below.

**Does:** oblige anything that links against Feedback — an addon, a fork, a coremod that
bundles it — to be GPL-compatible too. See above; this is the point.

**Does not:** stop owing attribution. Every one of those licences requires notices to be
preserved and the origin stated. Taking code is now *legally* available; it is still governed
by the author's private working rule (`CLAUDE.md`, not published), which is stricter than the law and deliberately so —
**prefer reading the design and writing the code.** A borrowed implementation is a borrowed
set of assumptions, and this mod departs from all three of these on purpose.

When code genuinely is adapted rather than reimplemented, three things are required:

1. Say so **at the call site**, naming the mod, the class and what was changed.
2. Record it in `THIRD-PARTY-LICENSES.md` with the licence it arrived under.
3. For EUPL-sourced code specifically, note that the Article 5 relicensing route was used.
   That is an election this project makes, and it should be traceable to the file that
   prompted it.

**Does not cover assets.** No texture, model or sound from any other mod may be used here,
ever, regardless of its code licence. Create's assets are All Rights Reserved, GregTech's are
third-party resource-pack imports with their own separate terms, and TFC's are TFC's.

## Who owns it, and one honest caveat

Copyright holder is **soundgoodizerfan** — see `AUTHORS`. The GitHub account handle now
matches the attribution name.

The caveat, recorded because it is genuinely unsettled rather than because it is a problem:
**parts of this implementation were written with AI assistance under the author's direction.**
Anthropic claims no ownership of that output, so there is no third party with a claim. The
open question in the abstract is whether purely machine-generated text is copyrightable at
all — US Copyright Office guidance and *Thaler v. Perlmutter* say material lacking human
authorship is unprotectable rather than owned by someone else.

That matters here only because **copyleft is enforced through copyright**, and you cannot
license what you do not own. In practice it is weak:

- The design is wholly the author's and predates the code — the private design documents
  (`feedback_philosophy.md` and the first-slice design) were written first and the source is written against them.
- The selection, direction, correction and arrangement throughout are human, which is the
  authorship that has ever mattered for compilations and derivative works.
- No GPL project has been successfully challenged on these grounds.

Recorded so nobody has to rediscover it. It is not a reason to license differently.

## Additional permission: Sable

Feedback requires **Sable** (ryanhcode) for physics bodies. Sable is licensed under the
**PolyForm Shield 1.0.0** — source-available with a noncompete clause, not GPL-compatible.
Without an exception, nobody could lawfully convey Feedback together with the library it
needs. So, under GPL-3.0 section 7:

> **Additional permission under GNU GPL version 3 section 7.** If you modify this Program,
> or any covered work, by linking or combining it with Sable (or a modified version of that
> library), containing parts covered by the terms of the PolyForm Shield License 1.0.0, the
> licensors of this Program grant you additional permission to convey the resulting work.
> Corresponding Source for a non-source form of such a combination shall not include the
> source code for the parts of Sable used as well as that of the covered work.

This is a narrow hole in the reciprocity argued above, and it was cut on purpose. The
exception names one library; it does not let anyone build *closed things on Feedback*, it
lets Feedback stand on one closed-ish thing. Sable was chosen anyway because arbitrary
physics bodies are something Minecraft does badly and Sable does well, and writing our own
is the physics simulator §3 warns against.

**The noncompete, read honestly.** Shield forbids providing a product that competes with
Sable. Feedback uses Sable; it does not offer physics bodies to other mods. If Feedback ever
grew its own physics-body system, it would only be because it does something Sable does
not — which is not a substitute and so not competition. Recorded so the next person does not
have to re-derive it.

Sable is a dependency, not a source: it is never vendored into this repository and nothing
is adapted from its code.

## Open

- [x] Per-file GPL notices on all 87 source files.
- [x] `AUTHORS`, naming soundgoodizerfan.
- [ ] **Update the copyright year and `AUTHORS` handle** when the GitHub account changes. The
      attribution is to the name, not the handle, precisely so this is a one-line fix.
