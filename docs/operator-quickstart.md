# kiyome — operator quickstart

Everything below was walked end to end on **2026-08-29** against commit `557c20e`,
on macOS 25.3.0. Each step prints a count or an exit code, so a step that could not
run is distinguishable from a step that ran and found nothing. Re-measure rather
than trusting these numbers: they are observations, not constants.

## What you are getting (read this before running anything)

kiyome is at **R0 — design plus one coded reference cell**. There is nothing here to
deploy, no service to start, no robot to drive. What you *can* do is read the corpus,
run its conformance suite, and check that the privacy gate the README claims is
actually pinned by a test. That is the whole operator surface today.

Concretely, of the five cells `manifest.edn` declares, exactly **one**
(`surface_cleaning`) has a coded state machine; the other four exist as `.edn`
descriptors only. Live deployment into homes or facilities is gated behind
Council Lv6+ and an operator (gate G7) and is not reachable from this repository.

## Prerequisites

Two runtimes, depending on which step you run:

| Step | Needs | Measured here on 2026-08-29 |
|---|---|---|
| Corpus check (2) | `nbb` | nbb v1.5.212 (Node v26.7.0) |
| Conformance suite (3) | `clojure` **or** `bb` | Clojure CLI 1.12.5.1654 / babashka v1.12.218 |

## 1. Get it

This repo is a west project of the `com-junkawasaki` superproject, so it is normally
already on disk:

```bash
cd orgs/cloud-itonami/kiyome
```

Standalone, clone it directly:

```bash
git clone git@github.com:cloud-itonami/kiyome && cd kiyome
```

`repository-contracts.edn` still names this repo `etzhayyim/com-etzhayyim-kiyome`.
That is the pre-rename name; GitHub still redirects it to `cloud-itonami/kiyome`,
so it resolves — it is stale, not broken.

## 2. Check the corpus parses

kiyome is mostly EDN: lexicons, cell descriptors, the manifest, the schema, the
citation table. Before trusting any of it, confirm every tracked `.edn` file reads.

```bash
cat > /tmp/kiyome-edn-check.cljs <<'EOF'
(ns c (:require [clojure.edn :as edn] ["node:fs" :as fs] ["node:child_process" :as cp]))
(def files (->> (-> (.execSync cp "git ls-files") str (.trim) (.split "\n"))
                (filter #(.endsWith % ".edn")) vec))
(def bad (atom []))
(doseq [f files]
  (try (edn/read-string (str "[" (fs/readFileSync f "utf8") "]"))
       (catch :default e (swap! bad conj [f (.-message e)]))))
(println "EDN-SCANNED" (count files))
(doseq [[f m] @bad] (println "PARSE-FAIL" f m))
(println (if (seq @bad) "FAIL" "OK"))
(.exit js/process (if (seq @bad) 1 0))
EOF
nbb /tmp/kiyome-edn-check.cljs
```

Expected on a clean tree: `EDN-SCANNED 16` then `OK`, exit 0. If the scanned count
is not 16, the walk is looking at a different tree than this document describes —
that mismatch matters more than the `OK`.

**Why the file content is wrapped in `[` `]`.** `edn/read-string` returns after the
*first* form and ignores whatever follows. Appending a truncated map to the end of
`manifest.edn` and reading it the obvious way still returns a value and still exits
0 — measured here on 2026-08-29, and it is the same trap the workspace CLAUDE.md
records for heredoc-authored EDN. Wrapping forces every top-level form to be read.
A corpus check that only reads the first form reports intact files and corrupt files
identically.

## 3. Run the conformance suite

Either command runs the same two suites and must print
`Ran 10 tests containing 35 assertions. 0 failures, 0 errors.`

```bash
# No babashka required (preferred — see "Known gaps" below):
clojure -Sdeps '{:paths ["."]}' -M -e "(require 'clojure.test 'kiyome.methods.test-charter-gates 'kiyome.cells.surface-cleaning.test-state-machine)(let [r (clojure.test/run-tests 'kiyome.methods.test-charter-gates 'kiyome.cells.surface-cleaning.test-state-machine)] (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1)))"
```

```bash
# The command repository-contracts.edn declares:
bb run_tests.clj
```

Both were run here on 2026-08-29 and both reported 10 tests / 35 assertions,
matching the `:tests {:count 10 :assertions 35}` recorded in
`repository-contracts.edn`, exit 0.

## 4. Prove the suite can fail (do not skip this)

Steps 2 and 3 mean nothing until you have seen them go red. A suite that has never
failed in front of you is indistinguishable from a suite that cannot fail.

```bash
cp -R . /tmp/kiyome-scratch && cd /tmp/kiyome-scratch
sed -i '' 's/:biometricCapture {:type "boolean" :const false/:biometricCapture {:type "boolean" :const true/' \
  kiyome/lex/siteAssessmentRecord.edn
clojure -Sdeps '{:paths ["."]}' -M -e "(require 'clojure.test 'kiyome.methods.test-charter-gates 'kiyome.cells.surface-cleaning.test-state-machine)(let [r (clojure.test/run-tests 'kiyome.methods.test-charter-gates 'kiyome.cells.surface-cleaning.test-state-machine)] (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1)))"
echo "EXIT=$?"
cd - && rm -rf /tmp/kiyome-scratch
```

Measured here on 2026-08-29: `EXIT=1`, and exactly one assertion turns red —

```
FAIL in (g9-privacy-on-device-no-imagery) (test_charter_gates.cljc:47)
G9: siteAssessment.biometricCapture const false
expected: (= false (const-of s :biometricCapture))
  actual: (not (= false true))
Ran 10 tests containing 35 assertions.
1 failures, 0 errors.
```

Check that the failure names the thing you broke. A red suite caused by something
other than your edit is not a demonstration that the check works.

**The `System/exit` in that command is load-bearing.** `clojure.test/run-tests`
reports failures on stdout but returns normally, so a bare
`-M -e "...(run-tests ...)"` exits **0 with a red suite** — measured here while
walking this document, which is how the earlier draft of this step came to quote the
wrong output. Anything that reads the exit code (CI, a shell `&&` chain, a gate)
would have recorded that red run as a pass. Keep the `System/exit`, and keep the
`echo "EXIT=$?"` on its own line rather than piping the command anywhere: `$?` is
the exit status of the *last* command in a pipeline, so `... | tail` would report
`tail`'s 0 no matter what the suite did.

## 5. Verify the privacy gate yourself, and see its exact scope

G9 is kiyome's defining constraint, so do not take it on the README's word.

```bash
grep -n 'onDeviceOnly\|imageryRetained\|biometricCapture' kiyome/lex/*.edn
```

Measured on 2026-08-29, the `const` invariants live in **two of the five** lexicons:

| Lexicon | `onDeviceOnly` | `imageryRetained` | `biometricCapture` |
|---|---|---|---|
| `cleaningPassAttestation` | const `true`, required | const `false` | — |
| `siteAssessmentRecord` | const `true`, required | — | const `false` |
| `sanitizationRecord` | — | — | — |
| `wasteSegregationRecord` | — | — | — |
| `linenLotAttestation` | — | — | — |

Read that table honestly. `sanitizationRecord` and `wasteSegregationRecord` both
carry a `siteId`, and `siteAssessmentRecord`'s own `siteClass` enum includes
`"home"` — so those two records can name a private dwelling while carrying no
on-device invariant. `linenLotAttestation` has no site reference at all (`lotId`
only), so its absence is the expected one. This is a real gap in coverage, not a
documentation nicety; see "Known gaps".

Runtime enforcement is separate from the schema consts and is stronger where it
exists: `kiyome/cells/surface_cleaning/state_machine.cljc` throws on a
`pass_logged` transition if `on_device_only` is false, and the same file bounds
`method` to `#{"sweep" "vacuum" "mop" "wipe"}` — there is no recording or scanning
verb a cleaning pass can express.

## 6. What you cannot do from here

- **Deploy anything.** R0. No firmware, no runtime, no endpoint. Gate G7 puts live
  home/facility deployment behind Council Lv6+ and an operator.
- **Run four of the five cells.** `site_assessment`, `sanitization`,
  `waste_segregation` and `linen_laundry` are `.edn` descriptors with no code.
- **Reach a model.** Gate G4 confines inference to Murakumo; nothing here dials out.

## Known gaps (measured 2026-08-29, not fixed here)

1. **`run_tests.clj` is a babashka script** (`#!/usr/bin/env bb`), and
   `repository-contracts.edn` declares `["bb" "run_tests.clj"]` as *the* test
   command. The workspace retired `bb` as a script host (ADR-2607173000). It still
   runs today, which is why it is documented above, but the `clojure` invocation in
   step 3 is the one that does not depend on a retired host. Neither the script nor
   the contract has been migrated.
2. **`kiyome/cells/social_post/` is an unregistered cell.** It has a real coded
   state machine — the actor's publication membrane, which drafts posts toward the
   mesh / AT-proto — and it appears **zero** times in `manifest.edn`,
   `run_tests.clj`, `repository-contracts.edn`, `README.md` and `CLAUDE.md`
   (measured with `grep -c`). So the one cell with outward reach is the one cell
   nothing in this repo describes or tests. `run_tests.clj` loads only
   `surface_cleaning` and the charter gates, so none of its R0 refusals
   (`no-server-key`, dry-run-only, ≥2 citations) is pinned by a test.
3. **G9 covers two of five lexicons** — the table in step 5, with
   `sanitizationRecord` and `wasteSegregationRecord` the two that carry a `siteId`
   without an on-device invariant.
4. **ISCO-08 and UNSPSC have no verified citation.** `facts.edn` records why: the
   ILO and UNSPSC hosts returned HTTP 403 to every request tried on 2026-08-29,
   including with a browser User-Agent. That is bot protection, not absence, and
   nothing was done to get around it. Read those as uncited, not as wrong.
