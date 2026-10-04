#!/usr/bin/env python3
"""
Build a ProgramDocument JSON (formatVersion 1) from the "TEST PLAN" training CSV.

The CSV is a *log* of dated sessions; what this emits is the *plan* the app imports:

  - one program (the app itself forces it inactive on load),
  - five weekday-pinned templates, one per lifting day,
  - planned sets mirroring the log: w -> WARMUP, ts -> TOP_SET, s -> NORMAL, d -> DROP,
    carrying the logged weight, reps and RPE (RPE stored in half-points),
  - the log's "Nm break" as a rest timer, the remaining technique text as the cue,
  - a supersetGroup for the log's `Giantset`/`ss1` notations.

Exercise names map onto the app's seeded library wherever one clearly matches; the
rest become custom exercises under their own ids, carrying the CSV's own wording.

Fixed timestamp, so regenerating is byte-identical.
"""

import csv
import json
import re
import unicodedata
from collections import OrderedDict

CSV_PATH = (
    "/home/dev/.dsh/attachments/v1/files/a0/"
    "a09363193d70dcb3014bad72ce608dca803bcfc63b35d67fb7c34415d83ff96f/TEST PLAN.csv"
)
# Seeded taxonomy/ids are read from the app's own export of the library, so the
# carried definitions match the rows already on the device rather than a retyping.
LIBRARY_BACKUP = "/home/dev/android-app/Gym-2026-M6-backup.json"
OUT_PATH = "/home/dev/android-app/store/test-plan-program.json"

# Fixed stamp: 2026-10-04T12:00:00Z. Regenerating must not churn the file.
STAMP = 1791192000000
PROGRAM_ID = "csv-test-plan-2026-09"

# Days with no exercises in the log are skipped, as agreed.
LIFTING_DAYS = ("Mon", "Tue", "Fri", "Sat")
WEEKDAY = {"Mon": "MONDAY", "Tue": "TUESDAY", "Fri": "FRIDAY", "Sat": "SATURDAY"}
# Every weekday starts a new block, including the ones that ship nothing: otherwise
# Wednesday's and Thursday's rows would fall into the day before them.
DAY_NAMES = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}
DAY_SLUG = {"Mon": "mon", "Tue": "tue", "Fri": "fri", "Sat": "sat"}

# CSV heading -> the app exercise it means, by NAME. Every value is a row of the
# app's seeded library; the ids are read from the export below rather than retyped.
LIBRARY_ALIAS = {
    "competition bench press": "Competition Bench Press",
    "conventional deadlift": "Conventional Deadlift",
    "pullup": "Pull-Up",
    "machine row": "Machine Row",
    "standing calves": "Standing Calf Raise",
    "dumbell rotator - up and down": "Dumbbell Rotator Raise",
    "bench press - speed day": "Bench Press — Speed Day",
    "pushpress": "Push Press",
    "cable row": "Seated Cable Row",
    "db fly": "Dumbbell Fly",
    "incline db arm curl": "Incline Dumbbell Arm Curl",
    "db skullcrushers": "Dumbbell Skullcrusher",
    "rotator wyciag w bok": "Rotator Cable to Side",
    "3sec paused bench press": "3-Second Paused Bench Press",
    "paused back squat": "Paused Back Squat",
    "barbell row": "Barbell Row",
    "assisted pullup": "Assisted Pull-Up",
    "incline db press": "Incline Dumbbell Press",
    "lat pulldown": "Lat Pulldown",
    "super rom db lateral raise": "Lateral Raise",
    "two arm cable pushdown": "Two-Arm Cable Pushdown",
    "cable arm curl": "Cable Arm Curl",
}

# Headings the library lacks: id, display name, taxonomy.
CUSTOM = {
    "barbell curl": ("csv-barbell-curl", "Barbell Curl", "BICEPS", [], "BARBELL", "ISOLATION"),
}

# The log's own superset/circuit notation, keyed by raw heading. 0 is used for the
# arms circuit, which the CSV marks only as "Giantset" alongside `standing calves`.
SUPERSET = {
    "standing calves (giantset 1x3)": 1,
    "lat pulldown (max 10) ss1": 1,
    "super rom db lateral raise (max 15) ss1": 1,
    "two arm cable pushdown (giantset - 30rep)": 0,
    "cable arm curl (giantset - 30rep)": 0,
}

BREAK = re.compile(r"(\d+(?:\.\d+)?)\s*m\b")
REPS_ONLY = re.compile(r"^\d+$")
SET_LABEL = re.compile(r"^(w|ts|s|d)\d+$")
ROLE = {"w": "WARMUP", "ts": "TOP_SET", "s": "NORMAL", "d": "DROP"}

# A line's cue/break note rather than an exercise header. Every note in this log is
# either timed (`3m break`, `60/15s break`, `2m break`) or one of these tempo phrases;
# an exercise header never is. Needed because the notes also carry commas, so the
# populated-columns split alone cannot tell the two apart.
NOTE_HEAD = re.compile(r"^\d")
NOTE_PHRASES = (
    "heigth at",
    "seat at",
    "bench at",
    "from ground",
    "look forward",
    "narrow grip",
    "elbows forward",
    "chest at",
)


def normalize(text):
    """Heading -> a lookup key: drop the `(max N)` / `(giantset …)` / `(dropset)` noise.

    Accents are folded (`wyciąg` -> `wyciag`) so a Polish heading matches its key
    instead of degrading to a space where the diacritic was.
    """
    text = text.lower()
    text = re.sub(r"\([^)]*\)", " ", text)
    text = text.replace("ss1", " ")
    text = text.replace("—", "-")
    # NFKD splits `ą` into `a` + a combining mark, which is then dropped.
    text = "".join(c for c in unicodedata.normalize("NFKD", text) if not unicodedata.combining(c))
    text = re.sub(r"ł", "l", text)
    text = re.sub(r"[^a-z0-9 -]+", " ", text)
    return re.sub(r"\s+", " ", text).strip(" -")


def load_library():
    """Exercise id -> definition, straight from the app's own export."""
    with open(LIBRARY_BACKUP, encoding="utf-8") as handle:
        backup = json.load(handle)
    rows = {row["id"]: row for row in backup["exercises"]}
    return rows, {row["name"]: row for row in backup["exercises"]}


def resolve(heading, library, by_name):
    """CSV heading -> (exercise id, display name, is_custom, taxonomy)."""
    key = normalize(heading)
    name = LIBRARY_ALIAS.get(key)
    if name is not None and name in by_name:
        row = by_name[name]
        return row["id"], row["name"], False, row
    if key in CUSTOM:
        eid, display, primary, secondary, equipment, pattern = CUSTOM[key]
        return eid, display, True, {
            "primaryMuscle": primary,
            "secondaryMuscles": secondary,
            "equipment": equipment,
            "movementPattern": pattern,
        }
    raise KeyError("unmapped exercise heading: %r (key %r)" % (heading, key))


def split_note(note):
    """`3m break, straps, look forward` -> (180, 'straps, look forward').

    A note is a timing line plus optional technique cues. The timing fragment is
    dropped from the cue (`3m break`, `60/15s break`, `2 min break`); a part that
    mentions a break but carries no time (`no breaks`) is dropped too, because it is
    still a statement about rests rather than something to read while lifting.
    """
    if not note:
        return None, None
    match = BREAK.search(note)
    seconds = int(round(float(match.group(1)) * 60)) if match else None
    kept = []
    for part in (p.strip() for p in note.split(",")):
        if not part or "break" in part.lower():
            continue
        # A leading time fragment with no break word: `60/15s ...`, `2 min ...`.
        cleaned = re.sub(r"^[\d./\s]*(?:s|m|min|sec)\b\.?\s*", "", part, flags=re.IGNORECASE)
        cleaned = cleaned.strip(" ,.")
        if cleaned:
            kept.append(cleaned)
    cue = ", ".join(kept).strip(" ,")
    return seconds, cue or None


def rpe_halves(text):
    text = (text or "").strip().replace(",", ".")
    if not text:
        return None
    try:
        halves = int(round(float(text) * 2))
    except ValueError:
        return None
    return halves if 2 <= halves <= 20 else None


def grams(text):
    text = (text or "").strip()
    return int(round(float(text) * 1000)) if text else None


def parse_blocks(day_rows):
    """One day's rows -> [ {heading, note, rows} ] in log order.

    A block's `rows` is the merged set/continuation sequence: `("set", label, weight,
    reps, rpe)` for a labelled set, `("reps", None, reps, None, None)` for a bare
    continuation count. Keeping both in one list preserves the logged order, which is
    the only way to know which weight a continuation set was performed at — and it is
    why a dropset's drops stay next to their own set.

    The rows are not rectangular: a set's weight sits in column 1, but a header's
    break note sits in column 0 and the points rows put their label in column 2. So
    each row is classified by what it looks like, not by which column is populated.
    """
    blocks = []
    for row in day_rows:
        padded = (row + [""] * 5)[:5]
        first, second = padded[0].strip(), padded[1].strip()
        if not any(cell.strip() for cell in padded):
            continue
        # Day preamble and the exercise's own trailing comment line.
        if first.lower() in {"comment", "not recovered"}:
            continue
        if first.lower() in {"target muscle feel", "joints pain"}:
            continue
        # A labelled set comes before the rep-only check: `w1`/`s1` are one letter plus
        # a digit, and the rep-only row would otherwise swallow `w1,60,6`'s weight.
        if SET_LABEL.match(first):
            if blocks:
                blocks[-1]["rows"].append(
                    ("set", first, second, padded[2].strip(), padded[3].strip())
                )
            continue
        if first == "":
            # The points block's header, its feel/pain rows, and the column-0
            # commentary all land here too, so a continuation must look like a rep
            # count: the `1x3` circuits write `,,8,,` (column 2) and the
            # `giantset - 30rep` blocks write `,,6,,` the same way.
            for cell in (second, padded[2].strip()):
                if REPS_ONLY.match(cell):
                    if blocks:
                        blocks[-1]["rows"].append(("reps", None, cell, None, None))
                    break
            continue
        heading = ",".join(c.strip() for c in padded if c.strip())
        lowered = first.lower()
        if blocks and (
            NOTE_HEAD.match(first) or any(lowered.startswith(p) for p in NOTE_PHRASES)
        ):
            # A timed break or a tempo cue for the exercise above.
            blocks[-1]["note"] = heading
            continue
        # Otherwise this is the exercise's own header line. Its `(max N)` is a note
        # about the day, not a target, and the break/cue line follows it.
        blocks.append({"heading": heading, "note": "", "rows": []})
    return blocks


def build():
    library, by_name = load_library()
    with open(CSV_PATH, newline="", encoding="utf-8-sig") as handle:
        rows = list(csv.reader(handle))

    days = []
    for row in rows:
        padded = (row + [""] * 5)[:5]
        if padded[0].strip() in DAY_NAMES:
            days.append({"day": padded[0].strip(), "date": padded[2].strip(), "rows": []})
        elif days:
            days[-1]["rows"].append(padded)

    templates, template_exercises, template_sets, slots = [], [], [], []
    order = []
    seen = set()

    for day in days:
        if day["day"] not in LIFTING_DAYS:
            continue
        slug = DAY_SLUG[day["day"]]
        template_id = "csv-test-plan-%s" % slug
        templates.append(
            OrderedDict(
                id=template_id,
                name="%s — %s" % (WEEKDAY[day["day"]].capitalize(), day["date"]),
                weekday=WEEKDAY[day["day"]],
                createdAt=STAMP,
                updatedAt=STAMP,
                deletedAt=None,
            )
        )
        slots.append(
            OrderedDict(
                id="csv-test-plan-slot-%s" % slug,
                programId=PROGRAM_ID,
                templateId=template_id,
                position=len(slots),
                weekday=WEEKDAY[day["day"]],
                createdAt=STAMP,
                updatedAt=STAMP,
                deletedAt=None,
            )
        )

        for position, block in enumerate(parse_blocks(day["rows"])):
            exercise_id, name, is_custom, taxonomy = resolve(block["heading"], library, by_name)
            if exercise_id not in seen:
                seen.add(exercise_id)
                order.append((exercise_id, name, is_custom, taxonomy))
            rest, cue = split_note(block["note"])
            te_id = "%s-%s" % (template_id, exercise_id)
            template_exercises.append(
                OrderedDict(
                    id=te_id,
                    templateId=template_id,
                    exerciseId=exercise_id,
                    position=position,
                    restSeconds=rest,
                    techniqueNote=cue,
                    supersetGroup=SUPERSET.get(block["heading"]),
                    createdAt=STAMP,
                    updatedAt=STAMP,
                    deletedAt=None,
                )
            )

            # Walk the logged rows in order, so a continuation set inherits the weight
            # still in effect and each dropset's drops stay beside their own set.
            sets = []
            carried = None
            for kind, label, weight, reps, rpe in block["rows"]:
                if kind == "reps":
                    # A continuation row carries its count in `weight`; the set form is
                    # `(label, weight, reps, rpe)`, so the two share positions 1 and 2.
                    sets.append(("NORMAL", carried, int(weight), None))
                    continue
                weight_grams = grams(weight)
                if weight_grams is not None:
                    carried = weight_grams
                sets.append(
                    (
                        ROLE[SET_LABEL.match(label).group(1)],
                        carried,
                        int(reps) if REPS_ONLY.match(reps) else None,
                        rpe_halves(rpe),
                    )
                )

            for index, (role, weight_grams, reps, rpe_h) in enumerate(sets):
                template_sets.append(
                    OrderedDict(
                        id="%s-set-%d" % (te_id, index),
                        templateExerciseId=te_id,
                        setIndex=index,
                        role=role,
                        targetWeightGrams=weight_grams,
                        targetAssistanceGrams=None,
                        targetRepsMin=reps,
                        targetRepsMax=reps,
                        targetRpeHalves=rpe_h,
                        targetRpe=None,
                        note=None,
                        createdAt=STAMP,
                        updatedAt=STAMP,
                        deletedAt=None,
                    )
                )

    exercises = [
        OrderedDict(
            id=exercise_id,
            name=name,
            primaryMuscle=taxonomy["primaryMuscle"],
            secondaryMuscles=taxonomy["secondaryMuscles"],
            equipment=taxonomy["equipment"],
            movementPattern=taxonomy["movementPattern"],
            isCustom=is_custom,
            createdAt=STAMP,
            updatedAt=STAMP,
            deletedAt=None,
            restSeconds=None,
            techniqueNote=None,
        )
        for exercise_id, name, is_custom, taxonomy in order
    ]

    return OrderedDict(
        formatVersion=1,
        exportedAt=STAMP,
        program=OrderedDict(
            id=PROGRAM_ID,
            name="Test Plan",
            isActive=False,
            position=0,
            createdAt=STAMP,
            updatedAt=STAMP,
            deletedAt=None,
        ),
        slots=slots,
        templates=templates,
        templateExercises=template_exercises,
        templateSets=template_sets,
        slotExercises=[],
        slotSets=[],
        exercises=exercises,
    )


if __name__ == "__main__":
    document = build()
    with open(OUT_PATH, "w", encoding="utf-8") as handle:
        json.dump(document, handle, indent=2, ensure_ascii=False)
        handle.write("\n")
    print("wrote", OUT_PATH)
    for key in ("slots", "templates", "templateExercises", "templateSets", "exercises"):
        print("%-18s %d" % (key, len(document[key])))
