/*
 * The program editor's assertions (ROADMAP N172, N173).
 *
 * The same cases run in the browser (`tests.html`) and under node (`tests-node.js`), because the editor
 * has two halves that must agree: the page's model operations and the shared core it previews with.
 * The DOM is not asserted here — the model is, and the model is what the app receives.
 */
(function (root, factory) {
  'use strict';
  if (typeof module !== 'undefined' && module.exports) {
    module.exports = factory();
  } else {
    root.EditorTests = factory();
  }
}(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict';

  function row(extra) {
    var base = { createdAt: 1, updatedAt: 2, deletedAt: null };
    Object.keys(extra || {}).forEach(function (key) { base[key] = extra[key]; });
    return base;
  }

  function exercise(id, name) {
    return row({
      id: id,
      name: name,
      primaryMuscle: 'CHEST',
      secondaryMuscles: [],
      equipment: 'BARBELL',
      movementPattern: 'PRESS',
      rowKind: 'MOVEMENT'
    });
  }

  function baseDocument() {
    return {
      formatVersion: 4,
      exportedAt: 1000,
      program: row({ id: 'pgm_1', name: 'Block', isActive: false, position: 0 }),
      slots: [row({ id: 'slot_a', programId: 'pgm_1', templateId: 'tpl_a', position: 0, weekday: 'MONDAY' })],
      templates: [row({ id: 'tpl_a', name: 'Upper A', versionOf: null })],
      templateExercises: [row({
        id: 'te_a',
        templateId: 'tpl_a',
        exerciseId: 'ex_a',
        position: 0,
        restSeconds: 180,
        techniqueNote: null,
        targetRpeHalves: 16,
        supersetGroup: null
      })],
      templateSets: [row({
        id: 'set_a',
        templateExerciseId: 'te_a',
        setIndex: 0,
        role: 'NORMAL',
        targetWeightGrams: 80000,
        targetAssistanceGrams: null,
        targetRepsMin: 5,
        targetRepsMax: 5,
        targetRepsCurrent: null,
        dropValueGrams: null,
        note: null
      })],
      exercises: [exercise('ex_a', 'Bench Press')],
      edit: { baseExportedAt: 1000 }
    };
  }

  function run(H, shared, t, cases) {
    // The fixture is a global the runner loads, so a renamed one used to leave the loop over nothing
    // and every case pass by being absent (N181). It is required rather than defaulted, and the
    // runner counts what was checked against what it declares.
    if (!Array.isArray(cases)) {
      throw new Error('the edit fixture did not load: PROGRAM_EDIT_CASES is not an array');
    }
    if (cases.length === 0) {
      throw new Error('the edit fixture is empty: no case would be checked');
    }

    // The number of checks that ran, rather than the number of cases handed in (B199): seven checks
    // once sat after an early return, and a runner comparing the fixture's own length called the run
    // complete while they never executed.
    var ran = 0;
    function check(name, body) {
      ran += 1;
      t.check(name, body);
    }

    check('a backup becomes one program document with only its own rows', function () {
      var backup = {
        exportedAt: 500,
        programs: [
          row({ id: 'pgm_1', name: 'Block', isActive: false, position: 0 }),
          row({ id: 'pgm_2', name: 'Other', isActive: false, position: 1 })
        ],
        programSlots: [
          row({ id: 'slot_a', programId: 'pgm_1', templateId: 'tpl_a', position: 0, weekday: 'MONDAY' }),
          row({ id: 'slot_gone', programId: 'pgm_1', templateId: 'tpl_gone', position: 1, weekday: null, deletedAt: 9 })
        ],
        templates: [
          row({ id: 'tpl_a', name: 'Upper A', versionOf: null }),
          row({ id: 'tpl_gone', name: 'Gone', versionOf: null, deletedAt: 9 }),
          row({ id: 'tpl_other', name: 'Other', versionOf: null })
        ],
        templateExercises: [
          row({ id: 'te_a', templateId: 'tpl_a', exerciseId: 'ex_a', position: 0 }),
          row({ id: 'te_gone', templateId: 'tpl_gone', exerciseId: 'ex_a', position: 0 })
        ],
        templateSets: [
          row({ id: 'set_a', templateExerciseId: 'te_a', setIndex: 0, role: 'NORMAL' }),
          row({ id: 'set_gone', templateExerciseId: 'te_gone', setIndex: 0, role: 'NORMAL' })
        ],
        exercises: [exercise('ex_a', 'Bench Press')]
      };

      var document = H.programDocumentFromBackup(backup, 'pgm_1');

      t.eq(document.program.name, 'Block');
      t.eq(document.slots.length, 1, 'the deleted slot is skipped');
      t.eq(document.templates.length, 1, 'the deleted workout is skipped');
      t.eq(document.templateExercises.length, 1);
      t.eq(document.templateSets.length, 1);
      t.eq(document.exercises.length, 1);
      t.eq(document.exportedAt, 500, 'the backup time is the base');
    });

    check('a table can say the program is a block, and when it starts', function () {
      var text = [
        'Day\tWeekday\tExercise\tSets\tReps\tLoad\tRest\tRPE\tKind\tStart',
        '1\tMonday\tBench Press\t3\t3\t97.5\t240\t8\tblock\t2026-10-05',
        '2\tFriday\tSquat\t3\t6\t100\t180\t7\tblock\t2026-10-05'
      ].join('\n');
      var sheet = H.parseProgramSheet(text, {});

      t.eq(sheet.problems.length, 0, 'the table parses');
      t.eq(sheet.kind, 'SEQUENCE');
      var document = H.buildProgramDocument(sheet, {}).document;
      t.eq(document.program.kind, 'SEQUENCE', 'the document says it is a block');
      t.eq(document.program.startDate, 20731, 'and the day it starts');
      t.eq(JSON.parse(shared.validateJson(JSON.stringify(document))).problems.length, 0);

      // A table without the column is what every table written before it is: weekly.
      var weekly = H.buildProgramDocument(
        H.parseProgramSheet('Day\tExercise\tSets\tReps\n1\tBench Press\t3\t3', {}), {}
      ).document;
      t.eq(weekly.program.kind, 'WEEKLY');
    });

    check('a dropped table is its own program, and a paste keeps the id it had (B266)', function () {
      var text = [
        'Program\tDay\tWeekday\tExercise\tSets\tReps\tLoad',
        'Push Pull Legs\tPush\tMonday\tBench Press\t3\t5\t100'
      ].join('\n');
      function build(fileName) {
        return H.buildProgramDocument(H.parseProgramSheet(text, { fileName: fileName }), {}).document;
      }

      var first = build('ppl.csv');
      var again = build('ppl.csv');
      var other = build('ppl-v2.csv');

      // The same file is the same document, which is what makes re-dropping it a no-op (B180).
      t.eq(again.program.id, first.program.id, 'one file, one program');
      t.eq(again.templates[0].id, first.templates[0].id, 'with the same workouts');
      t.eq(again.templateSets[0].id, first.templateSets[0].id, 'and the same sets');

      // A different spreadsheet that happens to name the same program is a different program — the
      // report that opened B266. Its ids must not be the first one's, or the app answers "already
      // here" to a program the device has never seen.
      t.ok(other.program.id !== first.program.id, 'another file, another program');
      t.ok(other.templates[0].id !== first.templates[0].id, 'with its own workouts');
      t.ok(other.templateSets[0].id !== first.templateSets[0].id, 'and its own sets');

      // A paste has no file to name it, so its seed is the name alone and the id is the one every
      // pasted program has carried: pinning it keeps programs imported before this change stable.
      t.eq(build('').program.id, 'pgm_219a652524971005', 'a pasted table keeps the id it had');
    });

    check('a table that names two program kinds is refused', function () {
      var text = [
        'Day\tExercise\tSets\tReps\tKind',
        '1\tBench Press\t3\t3\tblock',
        '2\tSquat\t3\t6\tweekly'
      ].join('\n');
      var sheet = H.parseProgramSheet(text, {});

      t.ok(sheet.problems.length > 0, 'two kinds cannot both be true');
    });

    check('a load is weight or assistance, and reads back the same way', function () {
      t.eq(H.editorLoadText(82500, null), '82.5');
      t.eq(H.editorLoadText(0, 20000), '-20');
      t.eq(H.editorLoadText(null, null), '');
      t.eq(H.editorParseLoad('-20').assistanceGrams, 20000);
      t.eq(H.editorParseLoad('-20').weightGrams, 0);
      t.eq(H.editorParseLoad('82,5').weightGrams, 82500);
      t.eq(H.editorParseLoad('nonsense'), null);
    });

    check('adding a day writes a workout and its slot', function () {
      var document = baseDocument();
      H.editorAddDay(document);
      t.eq(document.slots.length, 2);
      t.eq(document.templates.length, 2);
      t.eq(JSON.parse(shared.validateJson(JSON.stringify(document))).problems.length, 0);
    });

    check('an exercise and its sets extend a workout and stay valid', function () {
      var document = baseDocument();
      H.editorAddDay(document);
      var template = document.templates[document.templates.length - 1];
      var exerciseId = H.editorResolveExercise(document, 'Bench Press');
      t.ok(exerciseId, 'the name resolves');
      H.editorAddExercise(document, template.id, exerciseId);
      var planned = document.templateExercises[document.templateExercises.length - 1];
      H.editorAddSet(document, planned.id);
      H.editorAddSet(document, planned.id);
      var indexes = document.templateSets
        .filter(function (set) { return set.templateExerciseId === planned.id; })
        .map(function (set) { return set.setIndex; });
      t.eq(indexes.join(','), '0,1', 'the first set is index 0, as the app writes it');
      t.eq(JSON.parse(shared.validateJson(JSON.stringify(document))).problems.length, 0);
    });

    // A plan stores one row per set and the app reads one per set, but a lifter writes "3 × 8": the
    // editor draws a run of identical neighbours as one line with a count, and the count grows or
    // trims those rows. The document is what the app receives, so these check the rows.
    function setsOf(document) {
      return document.templateSets.slice().sort(function (a, b) { return a.setIndex - b.setIndex; });
    }

    check('neighbouring identical sets are one line, and one that differs breaks it', function () {
      var document = baseDocument();
      H.editorAddSet(document, 'te_a');
      H.editorAddSet(document, 'te_a');

      var runs = H.editorSetRuns(document, 'te_a');
      t.eq(runs.length, 1, 'three sets that say the same thing are one line');
      t.eq(runs[0].length, 3);

      setsOf(document)[2].note = 'last one heavy';
      runs = H.editorSetRuns(document, 'te_a');
      t.eq(runs.length, 2, 'a note is part of what a line stands for');
      t.eq(runs[0].length, 2);
      t.eq(runs[1].length, 1);
    });

    check('a count grows the line in place, and every copy says what the first said', function () {
      var document = baseDocument();
      var run = H.editorSetRunCount(document, 'set_a', '4');

      t.eq(run.length, 4, 'the line is four sets');
      t.eq(document.templateSets.length, 4);
      var sets = setsOf(document);
      t.eq(sets.map(function (set) { return set.setIndex; }).join(','), '0,1,2,3');
      t.ok(sets.every(function (set) {
        return set.role === 'NORMAL' && set.targetWeightGrams === 80000 &&
          set.targetRepsMin === 5 && set.targetRepsMax === 5;
      }), 'every copy says what the first set said');
      var ids = {};
      sets.forEach(function (set) { ids[set.id] = true; });
      t.eq(Object.keys(ids).length, 4, 'each row gets its own id');
      t.eq(JSON.parse(shared.validateJson(JSON.stringify(document))).problems.length, 0);
    });

    // B206, B207. Growing a line that is not the workout's last used to append its copies after the
    // last set, so a middle line's extra sets landed at the end and the line split in two on the next
    // render; and zero used to clamp to one and delete the rest of the line.
    check('a count grows a middle line in place, and the line after it stays after it', function () {
      var document = baseDocument();
      H.editorAddSet(document, 'te_a');
      setsOf(document)[1].note = 'back-off';
      t.eq(H.editorSetRuns(document, 'te_a').length, 2, 'two lines to start with');

      var grown = H.editorSetRunCount(document, 'set_a', '3');

      t.eq(grown.length, 3, 'the line the count was typed into is three sets');
      var sets = setsOf(document);
      t.eq(sets.length, 4);
      t.eq(sets.map(function (set) { return set.setIndex; }).join(','), '0,1,2,3',
        'the later line moved along rather than being overtaken');
      t.eq(sets[3].note, 'back-off', 'and it is still the last set');
      var after = H.editorSetRuns(document, 'te_a');
      t.eq(after.length, 2, 'the grown line did not split into two');
      t.eq(after[0].length, 3);
      t.eq(after[1].length, 1);
      t.eq(JSON.parse(shared.validateJson(JSON.stringify(document))).problems.length, 0);
    });

    check('a count of zero is not a count, on a line of any length', function () {
      var document = baseDocument();
      H.editorSetRunCount(document, 'set_a', '4');

      t.eq(H.editorSetRunCount(document, 'set_a', '0').length, 4, 'zero leaves the line alone');
      t.eq(document.templateSets.length, 4, 'and nothing was deleted');
    });

    check('a count trims the run at the back and leaves the rest of the plan alone', function () {
      var document = baseDocument();
      H.editorSetRunCount(document, 'set_a', '3');
      H.editorAddSet(document, 'te_a');
      setsOf(document)[3].note = 'back-off';

      var run = H.editorSetRunCount(document, 'set_a', '2');
      t.eq(run.length, 2);
      var left = setsOf(document);
      t.eq(left.length, 3, 'the trimmed rows went and the different set stayed');
      t.eq(left.map(function (set) { return set.setIndex; }).join(','), '0,1,3',
        'indexes are not renumbered: the gap is a set deleted on the phone (B157)');
      t.eq(left[2].note, 'back-off');
    });

    check('a count that is not a number leaves the run alone', function () {
      var document = baseDocument();
      t.eq(H.editorSetRunCount(document, 'set_a', '').length, 1, 'a blank field is not a deletion');
      t.eq(H.editorSetRunCount(document, 'set_a', '2.5').length, 1, 'nor is a fraction');
      t.eq(H.editorSetRunCount(document, 'set_a', '-1').length, 1, 'nor is a negative');
      t.eq(document.templateSets.length, 1, 'nothing was written');
    });

    check('a count stops where the line stops standing for sets', function () {
      var document = baseDocument();
      var run = H.editorSetRunCount(document, 'set_a', '999');
      t.eq(run.length, H.EDITOR_SET_COUNT_MAX, 'the cap is the line\'s, not a typo\'s');
      t.eq(document.templateSets.length, H.EDITOR_SET_COUNT_MAX);
      t.eq(JSON.parse(shared.validateJson(JSON.stringify(document))).problems.length, 0);
    });

    check('a line splits into one row per set, and the split is not in the document', function () {
      var document = baseDocument();
      H.editorSetRunCount(document, 'set_a', '3');

      var runs = H.editorSplitSetRun(document, 'set_a');
      t.eq(runs.length, 3, 'each set is a row of its own');
      t.ok(runs.every(function (run) { return run.length === 1; }));

      var written = JSON.stringify(document);
      t.eq(written.indexOf('editorAlone'), -1, 'the mark is the form\'s memory, not the file');

      // One row can now be written differently without its neighbours moving.
      document.templateSets[0].targetWeightGrams = 90000;
      t.eq(document.templateSets[1].targetWeightGrams, 80000, 'its neighbour kept what it had');
    });

    check('a split set merges back with the neighbours it still agrees with', function () {
      var document = baseDocument();
      H.editorSetRunCount(document, 'set_a', '3');
      H.editorSplitSetRun(document, 'set_a');
      document.templateSets[2].targetWeightGrams = 90000;

      var runs = H.editorMergeSetRun(document, document.templateSets[1].id);
      t.eq(runs.length, 2, 'the two that still agree are one line again');
      t.eq(runs[0].length, 2);
      t.eq(runs[1].length, 1, 'the set that changed keeps its own row');
    });

    check('a rung is never one of a run of identical sets', function () {
      var document = baseDocument();
      H.editorAddSet(document, 'te_a');
      H.editorAddSet(document, 'te_a');
      setsOf(document).forEach(function (set) {
        set.role = 'DROP';
        set.dropValueGrams = 20000;
      });

      var runs = H.editorSetRuns(document, 'te_a');
      t.eq(runs.length, 3, 'each drop is a place in the ladder, so each keeps its own row');
      t.ok(runs.every(function (run) { return run.length === 1; }));
    });

    check('removing a day keeps a workout another day still uses', function () {
      var document = baseDocument();
      document.slots.push(row({
        id: 'slot_b',
        programId: 'pgm_1',
        templateId: 'tpl_a',
        position: 1,
        weekday: 'FRIDAY'
      }));

      H.editorRemoveDay(document, document.slots[1]);

      t.eq(document.slots.length, 1);
      t.eq(document.templates.length, 1, 'the workout is still used');
      t.eq(document.templateExercises.length, 1);

      H.editorRemoveDay(document, document.slots[0]);

      t.eq(document.slots.length, 0);
      t.eq(document.templates.length, 0, 'the last day takes the workout with it');
      t.eq(document.templateExercises.length, 0);
      t.eq(document.templateSets.length, 0);
    });

    check('a new program is a valid document with no edit marker', function () {
      var document = H.newProgramDocument('New block', 'pgm_test', 1000);
      t.eq(document.formatVersion, shared.programFormatVersion(), 'the core writes the version');
      t.eq(document.program.name, 'New block');
      t.eq(document.program.id, 'pgm_test');
      t.eq(document.slots.length, 0);
      t.ok(!('edit' in document), 'a new program is a proposal to add');
      t.eq(JSON.parse(shared.validateJson(JSON.stringify(document))).problems.length, 0);

      H.editorAddDay(document);
      var template = document.templates[document.templates.length - 1];
      var exerciseId = H.editorResolveExercise(document, 'Bench Press');
      H.editorAddExercise(document, template.id, exerciseId);
      var planned = document.templateExercises[document.templateExercises.length - 1];
      H.editorAddSet(document, planned.id);
      t.eq(JSON.parse(shared.validateJson(JSON.stringify(document))).problems.length, 0);
    });

    check('an opened document plans and downloads with its base and its rows', function () {
      // The page's own half of the edit path: open a program, ask the core what it would do, and
      // write the file — through the functions the buttons call rather than through the core alone
      // (N181). A wrong base, a missing marker or a dropped field used to leave this green.
      var backup = {
        exportedAt: 4242,
        programs: [row({ id: 'pgm_9', name: 'Block', isActive: false, position: 0 })],
        programSlots: [row({ id: 'slot_9', programId: 'pgm_9', templateId: 'tpl_9', position: 0, weekday: 'FRIDAY' })],
        templates: [row({ id: 'tpl_9', name: 'Lower', versionOf: null })],
        templateExercises: [row({
          id: 'te_9',
          templateId: 'tpl_9',
          exerciseId: 'ex_9',
          position: 0,
          restSeconds: 180,
          techniqueNote: 'pause',
          targetRpeHalves: 17,
          supersetGroup: null
        })],
        templateSets: [row({
          id: 'set_9',
          templateExerciseId: 'te_9',
          setIndex: 0,
          role: 'TOP_SET',
          targetWeightGrams: 90000,
          targetAssistanceGrams: null,
          targetRepsMin: 3,
          targetRepsMax: 3,
          targetRepsCurrent: null,
          dropValueGrams: null,
          note: 'first'
        })],
        exercises: [exercise('ex_9', 'Back Squat')]
      };
      var document = H.programDocumentFromBackup(backup, 'pgm_9');
      var opened = H.editorOpenSnapshot(document, backup);

      t.eq(opened.working.edit.baseExportedAt, 4242, 'the marker names the export it was opened from');
      t.eq(opened.source, backup, 'the backup travels, so shared workouts are known');

      var planned = H.editorPlanEdit(shared, opened);
      t.eq(planned.ok, true, 'an untouched document plans clean');
      t.eq((planned.changes || []).length, 0, 'and changes nothing');

      var payload = H.editorDownloadPayload(opened);
      t.eq(payload.fileName, 'program-edit.json');
      var written = JSON.parse(payload.text);
      t.eq(written.edit.baseExportedAt, 4242, 'the download carries the base');
      t.eq(written.program.name, 'Block', 'the program travels');
      t.eq(written.slots.length, 1);
      t.eq(written.templates[0].name, 'Lower', 'the workout travels');
      t.eq(written.templateExercises[0].techniqueNote, 'pause', 'a field the UI shows travels');
      t.eq(written.templateSets[0].targetWeightGrams, 90000, 'the plan travels');
      t.eq(written.templateSets[0].role, 'TOP_SET');
      t.eq(written.exercises[0].name, 'Back Squat', 'the movement travels');
    });

    check('an opened program can also leave as an add, with no base and its ids unchanged', function () {
      // The program a receiving device does not hold yet: an edit names a base that is not there,
      // and the app refuses it rather than adding anything. The add is the same document with the
      // marker gone — the ids travel, so the app's insert-only merge adds it once (N172, B180).
      var document = baseDocument();
      var opened = H.editorOpenSnapshot(document, null);
      t.ok('edit' in opened.working, 'opening makes it an edit');

      var payload = H.editorAddPayload(opened);
      t.eq(payload.fileName, 'program-add.json');
      var written = JSON.parse(payload.text);
      t.ok(!('edit' in written), 'an add names no base');
      t.eq(written.formatVersion, shared.programFormatVersion(), 'the core writes the version');
      t.eq(written.program.id, 'pgm_1', 'the id is the identity, so a re-import adds nothing');
      t.eq(written.program.name, 'Block');
      t.eq(written.slots.length, 1);
      t.eq(written.templates[0].name, 'Upper A', 'the workout travels');
      t.eq(written.templateSets[0].targetWeightGrams, 80000, 'the plan travels');
      t.eq(written.exercises[0].name, 'Bench Press', 'the movement travels');
      t.eq(JSON.parse(shared.validateJson(payload.text)).problems.length, 0,
        'the app would add it');
      t.ok('edit' in opened.working, 'the edit is still open: the add was a copy');
    });

    // Exact membership, not order: the planner writes categories in its own order, and the claim is
    // which changes it makes rather than how it sorts them.
    function listed(list) {
      return list.slice().sort().join(',');
    }

    cases.forEach(function (testCase) {
      check('fixture: ' + testCase.name, function () {
        var result = JSON.parse(shared.planJson(
          JSON.stringify(testCase.snapshot),
          JSON.stringify(testCase.edit)
        ));
        t.eq(result.ok, testCase.expected.ok, 'ok');
        // Exactly what the fixture says, in its order — not "the expected kinds appear somewhere"
        // (N181): a planner that over-reported an addition the write would skip used to pass.
        if (testCase.expected.ok) {
          var kinds = result.changes.map(function (change) { return change.kind; });
          var warnings = result.warnings.map(function (warning) { return warning.code; });
          t.eq(listed(kinds), listed(testCase.expected.kinds || []), 'the changes, exactly');
          t.eq(listed(warnings), listed(testCase.expected.warnings || []), 'the warnings, exactly');
          if (testCase.expected.details) {
            var details = result.changes.map(function (change) { return String(change.detail); });
            t.eq(listed(details), listed(testCase.expected.details), 'the details, exactly');
          }
        } else {
          var codes = result.problems.map(function (problem) { return problem.code; });
          t.eq(listed(codes), listed(testCase.expected.codes || []), 'the refusals, exactly');
        }
      });
    });


    check('a day naming a workout the file does not carry is refused', function () {
      var broken = baseDocument();
      broken.templates = [];
      var problems = JSON.parse(shared.validateJson(JSON.stringify(broken))).problems;
      t.ok(problems.some(function (problem) { return problem.code === 'DANGLING_SLOT_TEMPLATE'; }));
    });

    check('a typed name resolves to the movement the library already holds (B187)', function () {
      var program = H.newProgramDocument('Block', 'pgm_1', 1000);
      var library = [row({
        id: 'ex_custom',
        name: 'Cable Fly',
        primaryMuscle: 'CHEST',
        secondaryMuscles: [],
        equipment: 'CABLE',
        movementPattern: 'FLY',
        rowKind: 'MOVEMENT'
      })];

      var id = H.editorResolveExercise(program, 'cable fly', library);

      t.eq(id, 'ex_custom', 'the library movement is the movement, not a second row');
      t.eq(program.exercises.length, 1, 'it is carried into the document');
      t.eq(program.exercises[0].id, 'ex_custom');
    });

    check('a typo corrected before the download leaves no movement behind (B187)', function () {
      var program = H.newProgramDocument('Block', 'pgm_1', 1000);
      H.editorAddDay(program);
      var template = program.templates[0];
      var typo = H.editorResolveExercise(program, 'Cabel Fly', []);
      H.editorAddExercise(program, template.id, typo);
      var planned = program.templateExercises[0];

      planned.exerciseId = H.editorResolveExercise(program, 'Bench Press', []);
      H.editorPruneExercises(program);

      t.eq(program.exercises.length, 1, 'only what the plan names is carried');
      t.eq(program.exercises[0].name, 'Bench Press');
      t.ok(!program.exercises.some(function (row) { return row.name === 'Cabel Fly'; }),
        'the minted typo is gone');
    });

    check('a program taken from a backup names the other program that shares its workouts (B191)', function () {
      var backup = {
        programs: [row({ id: 'pgm_1', name: 'Autumn block' }), row({ id: 'pgm_2', name: 'Conditioning' })],
        programSlots: [
          row({ id: 'slot_a', programId: 'pgm_1', templateId: 'tpl_a', position: 0 }),
          row({ id: 'slot_b', programId: 'pgm_2', templateId: 'tpl_a', position: 0 })
        ]
      };
      var working = {
        program: { id: 'pgm_1' },
        templates: [{ id: 'tpl_a' }]
      };

      t.eq(H.editorSharedProgramNames(working, backup).join(','), 'Conditioning');
      t.eq(H.editorSharedProgramNames(working, null).length, 0, 'no source, nothing to say');
    });

    check('a role change clears what it cannot carry and keeps what it can (B190)', function () {
      var set = row({
        id: 'set_a',
        role: 'NORMAL',
        targetWeightGrams: 80000,
        targetAssistanceGrams: null,
        targetRepsMin: 5,
        targetRepsMax: 5,
        targetRepsCurrent: null,
        dropValueGrams: null
      });

      set.role = 'DROP';
      H.editorApplyRole(set);
      t.eq(set.targetWeightGrams, null, 'a rung carries no load');
      t.eq(set.targetRepsMin, null, 'a rung carries no reps');
      t.eq(set.dropValueGrams, null, 'a drop names no value until one is typed');
      t.ok(!('editorCarried' in JSON.parse(JSON.stringify(set))),
        'the memory of what was cleared is not in the document the app receives');

      set.dropValueGrams = 60000;
      set.role = 'CLUSTER';
      H.editorApplyRole(set);
      t.eq(set.dropValueGrams, null, 'a cluster is not a drop and carries no value');

      set.role = 'NORMAL';
      H.editorApplyRole(set);
      t.eq(set.targetWeightGrams, 80000, 'the load the row had comes back');
      t.eq(set.targetRepsMin, 5, 'and so do its reps');
      t.eq(set.dropValueGrams, null);
    });

    check('a new program can be authored as a block (B196, N186)', function () {
      var program = H.newProgramDocument('Peak', 'pgm_block', 1000);
      H.editorSetKind(program, 'SEQUENCE');
      program.program.startDate = 20731;
      H.editorAddDay(program);
      var template = program.templates[0];
      var exerciseId = H.editorResolveExercise(program, 'Bench Press');
      H.editorAddExercise(program, template.id, exerciseId);
      H.editorAddSet(program, program.templateExercises[0].id);

      t.eq(program.formatVersion, 5);
      t.ok(!('edit' in program), 'a new program is a proposal to add');
      t.eq(JSON.parse(shared.validateJson(JSON.stringify(program))).problems.length, 0);
    });

    check('a Start without a block is refused, and leaving a block clears it (B205)', function () {
      var parsed = H.parseProgramSheet(
        'Day\tExercise\tSets\tReps\tStart\n' +
        'A\tSquat\t3\t5\t2026-10-05\n'
      );
      t.ok(parsed.problems.some(function (problem) {
        return problem.message.indexOf('Start needs') !== -1;
      }), 'the table is told why');

      t.eq(H.programStartOf({ kind: 'WEEKLY', start: 20731 }), null, 'a weekly program writes no start');
      t.eq(H.programStartOf({ kind: 'SEQUENCE', start: 20731 }), 20731);

      var working = H.newProgramDocument('Peak', 'pgm_1', 1000);
      working.program.kind = 'SEQUENCE';
      working.program.startDate = 20731;
      H.editorSetKind(working, 'WEEKLY');
      t.eq(working.program.startDate, null, 'the hidden date is gone');
      t.eq(working.program.kind, 'WEEKLY');
    });

    check('a date must be the day it says, and an unreadable one renders nothing (B204)', function () {
      t.eq(H.parseProgramDay('2026-10-05'), 20731, 'a real day is its epoch day');
      t.eq(H.parseProgramDay('2026-02-31'), null, 'February has no 31st');
      t.eq(H.parseProgramDay('2026-13-01'), null, 'there is no month 13');
      t.eq(H.parseProgramDay('0000-01-01'), null, 'year zero is not read as 1900');
      t.eq(H.editorParseDay('2026-04-31'), null);

      t.eq(H.editorDayText(1.79e12), '', 'a millis-valued start renders nothing');
      t.eq(H.editorDayText(20731), '2026-10-05');
    });

    check('a table authored as a block previews as one (B203)', function () {
      var stats = { days: 3, exercises: 9, sets: 27 };
      var block = H.programPanelSummary(
        { name: 'Peak', kind: 'SEQUENCE', start: 20731, unit: 'kg' },
        stats
      );
      var weekly = H.programPanelSummary({ name: 'Base', unit: 'kg' }, stats);

      t.ok(block.indexOf('one block') !== -1, block);
      t.ok(block.indexOf('starting') !== -1, block);
      t.ok(weekly.indexOf('every week') !== -1, weekly);
      t.ok(weekly.indexOf('starting') === -1, weekly);
    });

    check('the summary words a block change as a block (B202)', function () {
      var summary = H.editorSummary([
        { kind: 'PROGRAM_KIND', id: 'pgm_1', detail: 'SEQUENCE' },
        { kind: 'PROGRAM_START', id: 'pgm_1', detail: '20731' }
      ]);

      t.eq(summary.length, 2);
      t.ok(summary[0].indexOf('block') !== -1, summary[0]);
      t.ok(summary[0].indexOf('weekly') === -1, 'the sentence does not say the opposite: ' + summary[0]);
      t.ok(summary[1].indexOf('created') === -1, 'a named day is not the creation date: ' + summary[1]);
    });

    check('a block authored from a v4 document is written at the current version (B198)', function () {
      var old = baseDocument();
      old.formatVersion = 4;
      delete old.edit;

      var opened = H.editorOpenSnapshot(old, null);
      opened.working.program.kind = 'SEQUENCE';
      opened.working.program.startDate = 20731;

      var written = JSON.parse(H.editorDownloadPayload(opened).text);
      t.eq(written.formatVersion, 5, 'the file carries the version this build reads');
      t.eq(written.program.kind, 'SEQUENCE');
      t.eq(JSON.parse(shared.validateJson(JSON.stringify(written))).problems.length, 0);
    });

    check('a rung refusal names its own cause, not one sentence for five (B186)', function () {
      var broken = baseDocument();
      broken.templateSets.push(row({
        id: 'set_drop',
        templateExerciseId: 'te_a',
        setIndex: 1,
        role: 'DROP',
        targetWeightGrams: null,
        targetAssistanceGrams: null,
        targetRepsMin: null,
        targetRepsMax: null,
        targetRepsCurrent: null,
        dropValueGrams: null,
        note: null
      }));
      var problems = JSON.parse(shared.validateJson(JSON.stringify(broken))).problems;
      t.ok(problems.some(function (problem) { return problem.code === 'RUNG_WITHOUT_VALUE'; }),
        'the first drop of a run with no value is its own code');
      t.ok(!problems.some(function (problem) { return problem.code === 'INVALID_RUNG'; }),
        'no catch-all code survives');
      t.ok(problems.every(function (problem) { return !problem.detail || problem.code !== 'RUNG_WITHOUT_VALUE'; }),
        'the cause is the code, not an English detail');
    });

    check('a rest the app cannot store is its own refusal (B186)', function () {
      var broken = baseDocument();
      broken.templateExercises[0].restSeconds = 4000;
      var problems = JSON.parse(shared.validateJson(JSON.stringify(broken))).problems;
      t.ok(problems.some(function (problem) { return problem.code === 'INVALID_REST'; }),
        'a 4000-second rest is a rest problem, not a malformed file');
    });

    return ran;
  }

  return { run: run };
}));
