# kiyome 清め — domestic / janitorial cleaning robotics

> *清め* = purification / cleansing. The actor that frees the most invisible labour on Earth.

**LPS #3** (ADR-2606032100). ISIC **T/N81** · ISCO **9111/9112** · UNSPSC **76**. DID
`did:web:etzhayyim.com:actor:kiyome`.

## Why cleaning work

~75 M domestic workers + tens of millions of janitorial/sanitation workers — invisible, gendered,
dignity-poor, and entirely un-automated by any actor. Freeing it is among the highest-Wellbecoming
acts available (gate **G6**).

## Privacy is the defining constraint

Cleaning means entering homes. Gate **G9** makes kiyome the opposite of a surveillance product:
on-device only, **no cloud imagery, no sensor feed, no biometric/facial recognition** — enforced as
hard `const` invariants in two of the five lexicons — `cleaningPassAttestation`
(`onDeviceOnly: true`, `imageryRetained: false`) and `siteAssessmentRecord`
(`onDeviceOnly: true`, `biometricCapture: false`) — and at runtime by the
`surface_cleaning` state machine, which refuses to log a pass when `on_device_only`
is false. `sanitizationRecord` and `wasteSegregationRecord` carry a `siteId` but no
on-device invariant yet; see `docs/operator-quickstart.md` § 5 for the measured table. Displaced cleaners are registered for the tenure-weighted Displacement
Dividend (ADR-2606032130, gate **G2**).

## Running it

R0 means there is nothing to deploy. What you can actually do — read the corpus, run
the 10-test conformance suite, and watch it go red on a seeded G9 regression — is in
**`docs/operator-quickstart.md`**.

## Honest

Dexterous manipulation in unstructured homes (dishes, clutter) is `:research` maturity. R0 = design only.
