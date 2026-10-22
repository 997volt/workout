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

    t.check('a backup becomes one program document with only its own rows', function () {
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

    t.check('a table can say the program is a block, and when it starts', function () {
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

    t.check('a table that names two program kinds is refused', function () {
      var text = [
        'Day\tExercise\tSets\tReps\tKind',
        '1\tBench Press\t3\t3\tblock',
        '2\tSquat\t3\t6\tweekly'
      ].join('\n');
      var sheet = H.parseProgramSheet(text, {});

      t.ok(sheet.problems.length > 0, 'two kinds cannot both be true');
    });

    t.check('a load is weight or assistance, and reads back the same way', function () {
      t.eq(H.editorLoadText(82500, null), '82.5');
      t.eq(H.editorLoadText(0, 20000), '-20');
      t.eq(H.editorLoadText(null, null), '');
      t.eq(H.editorParseLoad('-20').assistanceGrams, 20000);
      t.eq(H.editorParseLoad('-20').weightGrams, 0);
      t.eq(H.editorParseLoad('82,5').weightGrams, 82500);
      t.eq(H.editorParseLoad('nonsense'), null);
    });

    t.check('adding a day writes a workout and its slot', function () {
      var document = baseDocument();
      H.editorAddDay(document);
      t.eq(document.slots.length, 2);
      t.eq(document.templates.length, 2);
      t.eq(JSON.parse(shared.validateJson(JSON.stringify(document))).problems.length, 0);
    });

    t.check('an exercise and its sets extend a workout and stay valid', function () {
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

    t.check('removing a day keeps a workout another day still uses', function () {
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

    t.check('a new program is a valid document with no edit marker', function () {
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

    t.check('an opened document plans and downloads with its base and its rows', function () {
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

    // Exact membership, not order: the planner writes categories in its own order, and the claim is
    // which changes it makes rather than how it sorts them.
    function listed(list) {
      return list.slice().sort().join(',');
    }

    cases.forEach(function (testCase) {
      t.check('fixture: ' + testCase.name, function () {
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
        } else {
          var codes = result.problems.map(function (problem) { return problem.code; });
          t.eq(listed(codes), listed(testCase.expected.codes || []), 'the refusals, exactly');
        }
      });
    });

    return cases.length;

    t.check('a day naming a workout the file does not carry is refused', function () {
      var broken = baseDocument();
      broken.templates = [];
      var problems = JSON.parse(shared.validateJson(JSON.stringify(broken))).problems;
      t.ok(problems.some(function (problem) { return problem.code === 'DANGLING_SLOT_TEMPLATE'; }));
    });

    t.check('a typed name resolves to the movement the library already holds (B187)', function () {
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

    t.check('a typo corrected before the download leaves no movement behind (B187)', function () {
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

    t.check('a program taken from a backup names the other program that shares its workouts (B191)', function () {
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

    t.check('a role change clears what it cannot carry and keeps what it can (B190)', function () {
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

    t.check('a rung refusal names its own cause, not one sentence for five (B186)', function () {
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

    t.check('a rest the app cannot store is its own refusal (B186)', function () {
      var broken = baseDocument();
      broken.templateExercises[0].restSeconds = 4000;
      var problems = JSON.parse(shared.validateJson(JSON.stringify(broken))).problems;
      t.ok(problems.some(function (problem) { return problem.code === 'INVALID_REST'; }),
        'a 4000-second rest is a rest problem, not a malformed file');
    });
  }

  return { run: run };
}));
