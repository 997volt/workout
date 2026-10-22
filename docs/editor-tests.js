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
      t.eq(document.formatVersion, 4);
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

    (cases || []).forEach(function (testCase) {
      t.check('fixture: ' + testCase.name, function () {
        var result = JSON.parse(shared.planJson(
          JSON.stringify(testCase.snapshot),
          JSON.stringify(testCase.edit)
        ));
        t.eq(result.ok, testCase.expected.ok, 'ok');
        var seen = testCase.expected.ok
          ? result.changes.map(function (change) { return change.kind; })
          : result.problems.map(function (problem) { return problem.code; });
        (testCase.expected.kinds || []).forEach(function (kind) {
          t.ok(seen.indexOf(kind) !== -1, 'the plan names ' + kind);
        });
        (testCase.expected.codes || []).forEach(function (code) {
          t.ok(seen.indexOf(code) !== -1, 'the plan refuses with ' + code);
        });
        var warnings = testCase.expected.ok
          ? result.warnings.map(function (warning) { return warning.code; })
          : [];
        (testCase.expected.warnings || []).forEach(function (code) {
          t.ok(warnings.indexOf(code) !== -1, 'the plan warns ' + code);
        });
      });
    });

    t.check('a day naming a workout the file does not carry is refused', function () {
      var broken = baseDocument();
      broken.templates = [];
      var problems = JSON.parse(shared.validateJson(JSON.stringify(broken))).problems;
      t.ok(problems.some(function (problem) { return problem.code === 'DANGLING_SLOT_TEMPLATE'; }));
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
