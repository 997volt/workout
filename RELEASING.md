# Releasing

One rule matters more than everything else here:

> **The tag must point at the commit that built the APK.**

Get that wrong and `git checkout v1.2` silently disagrees with the binary people downloaded.
Nothing tests it, no build fails, and it stays wrong. The rest of this file exists to keep that
true. It was written after the v1.2 release, which nearly went wrong twice — see
[Traps](#traps).

## The procedure

### 1. Decide the version

[`version.properties`](version.properties) is the single source of truth for both `versionCode`
and `versionName`; the build fails loudly if either is missing. **`versionCode` must increase** —
Android compares it and nothing else, so a lower one is refused by every phone.

### 2. Write the changelog *before* the bump

Add a [CHANGELOG.md](CHANGELOG.md) entry for the new version, listing **only what is actually in
this build**. `[Unreleased]` is where that work accumulates between releases — retitle it
`## [x.y] — date (versionCode n)` rather than adding a second section, and delete the two sentences that say
it is unreleased, or the new version ships with a note calling itself unreleased. Check which commits
landed before the commit you are about to tag:

```bash
git log --oneline <last-release-tag>..HEAD
```

An entry credited to the wrong release is the same class of error as a tag that does not match
its binary: invisible, and misleading exactly to the person who trusts it.

### 3. Bump, commit

```bash
# edit version.properties, then:
git add CHANGELOG.md ROADMAP.md version.properties
git commit -m "release: 1.3 (versionCode 4)"
```

**Name the files; never `git add -A`.** `-A` takes whatever is in the tree, and the 2.4 round
proved it: a scratch directory of HTTP header and JSON dumps from checking the API went into the
release commit, and only reading that commit's own `--stat` caught it before the push. Those three
files are also the complete set a release is allowed to touch, so the command is a check as well as
a command. A release that genuinely has to carry something else — a fix found while verifying —
stages that file **by name** too; the invariant is that the release commit's content is named rather
than swept. If `git status` after committing shows anything you did not name, stop and look at it
before tagging.

### 4. Build the signed APK

```bash
./tools/build-apk.sh
```

It refuses to hand over an unsigned APK, which would install for nobody, and reads the
gitignored `keystore.properties`; see
[Install on your own phone](README.md#install-on-your-own-phone) if that is missing. If a task
fails with an error that does not point at your own code, re-run before investigating —
[README](README.md#known-flakiness-intermittent-task-failures-under-memory-pressure) lists the
four that do this.

### 5. Verify, before tagging

**Dispatch the pipeline first** and wait for both jobs, including the instrumented one: CI no
longer runs on every push, so nothing else has confirmed this commit on a clean machine.

```sh
gh workflow run "Android CI" --ref main
gh run watch
```

The build job runs the gate set — unit tests, lint, detekt, R8 — and the instrumented job is the
only place the emulator suite runs outside a developer's machine, so a release is the wrong time
to discover either has stopped working. A dispatch is also the one run that uses the **shipping
API** rather than the nightly's fast API 34 image, so it is the run that would catch a behaviour
change on a newer Android. If it fails on the emulator rather than on a test, say so and decide
deliberately: the failure signatures and the API-level split are in
[DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md#n30-emulator).

```bash
./tools/verify-release.py
```

It asserts the four things by hand used to be, and prints the numbers the notes need:

```
version     1.3 (4) — matches version.properties
certificate matches the keystore
size        2455012 bytes
sha256      bdd4705ac9b0a8b3e92d20254afcd62827cc94c1b27272bb24f6b5a27f1287ea
```

**The certificate is the one that matters.** It is read from the keystore the build signs with
(the gitignored `keystore.properties`), and any other value means the APK is refused as an update
and the only fix is an uninstall — which deletes the workout history, because platform backup is
off. If it reports a mismatch, stop and work out why; do not publish.

None of that needs a token or the network, so it runs on CI and from a checkout that has never seen
a release. The commands behind it, when you want to see them yourself:

```bash
APK=app/build/outputs/apk/release/app-release.apk
BT=.toolchain/android-sdk/build-tools/36.0.0    # or the runner's build-tools

$BT/aapt2 dump badging "$APK" | grep '^package:'          # versionCode/versionName
$BT/apksigner verify --print-certs "$APK" | grep SHA-256  # must match the last release
sha256sum "$APK"                                          # goes in the release notes
```

### 6. Tag the commit you built

```bash
git tag -a v1.3 -m "Workout 1.3 (versionCode 4)

sha256  <from step 5>"
git push origin main
git push origin v1.3
```

**If `main` has moved, merge it in first** — `git merge origin/main`, never `git rebase`. A
rebase rewrites the release commit, changes its SHA, and orphans the tag; merging keeps the
commit identity, and therefore the tag, valid.

### 7. Publish the release

**Releases → Draft a new release** → pick the tag → title it `Workout 1.3` → paste the notes →
attach the APK → Publish. The attached file's name and the `sha256` in the notes must agree with
step 5:

```
**Version:** 1.3 (versionCode 4)
**File:** workout-1.3.apk — <bytes> bytes
**SHA-256:** <from step 5>
**Requires:** Android 8.0+ (API 26)

Install with `adb install -r workout-1.3.apk`, or copy it to the phone and tap it. Signed with
the same key as previous releases, so it upgrades in place — do not uninstall first, which would
delete the history.
```

Then check that what is published is the file the notes describe. This is the one step that needs
the network and a token, so it is opt-in — it downloads the asset with `gh` and compares it to the
APK just verified:

```bash
./tools/verify-release.py --remote v1.3
```

**Do not skip it because the upload looked right.** The two failures it catches are a wrong file
attached to the right release, and a right file attached under the wrong name; neither is visible
on the release page, and both are invisible to every other check here.

## Traps

Collected from the v1.2 release, all of them real:

- **A rejected `main` push.** `git push` fails as a non-fast-forward when someone merged to `main`
  while you were working, and the tag may already be up by then, pointing at a commit that is not
  on the branch. **Merge, don't rebase** (step 6) and re-push; the tag then becomes reachable
  without changing.
- **A changelog that credited the wrong version.** The v1.2 notes attributed two changes to 1.1
  that landed after the 1.1 build. Check with `git log` (step 2) rather than memory.
- **"Generate release notes" in the GitHub UI.** It builds notes from merged PRs and commit
  subjects, which here means `chore(deps): …` and internal refactors. Write notes for the person
  downloading the APK instead.
- **An unsigned APK in the output directory.** CI signs nothing, so
  `app/build/outputs/apk/release/` can hold `app-release-unsigned.apk` after a CI-shaped build.
  Check the filename before handing anything to anyone.
- **Deleting or re-pushing a published tag.** Safe only before anyone has fetched it. Once a
  release exists against a tag, treat it as immutable and cut a new version instead.

Why releases stay manual, and the automation that was rejected, is in
[DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md#releases).
