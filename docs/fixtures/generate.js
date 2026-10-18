#!/usr/bin/env node
/*
 * Regenerate the synthetic backup the app's JVM test decodes (ROADMAP N160).
 *
 * The fixture is produced by the tool rather than written by hand, so it is
 * exactly what `docs/` emits, and the app test fails when the two drift. Run
 * it after changing anything that affects the output:
 *
 *     node docs/fixtures/generate.js
 *
 * The data is invented: no real export is ever committed, and `.gitignore`
 * keeps `Gym-*.json` out of the repository for that reason.
 */
'use strict';

var path = require('path');
var fs = require('fs');
var api = require(path.join(__dirname, '..', 'app.js'));

/* Two sessions, three exercises, a warm-up, a pound row and an RPE half-point. */
var ROWS = [
  { date: '2024-03-04', exercise: 'Back Squat', reps: 10, weight: 40, unit: 'kg', type: 'W' },
  { exercise: 'Back Squat', reps: 5, weight: 100, unit: 'kg', rpe: 8, type: 'N' },
  { exercise: 'Back Squat', reps: 5, weight: 100, unit: 'kg', rpe: 8.5, type: 'N' },
  { exercise: 'Standing Calf Raise', reps: 12, weight: 150, unit: 'lb', rpe: 9, type: 'N' },
  { date: '2024-03-06', exercise: 'Bench Press', reps: 5, weight: 80, unit: 'kg', rpe: 9, type: 'N' },
  { exercise: 'Bench Press', reps: 5, weight: 80, unit: 'kg', type: 'N' }
];

var model = api.buildModel(ROWS, {
  seed: 'import-helper-fixture',
  sheet: 'fixture',
  dateOrder: 'dmy'
});

var result = api.buildExport(model, { exportedAt: 1709683200000 });
if (!result.ok) {
  console.error('the tool refused the fixture:');
  result.problems.forEach(function (problem) {
    console.error('  ' + problem.path + ': ' + problem.message);
  });
  process.exit(1);
}

var target = path.join(__dirname, 'import-helper-backup.json');
fs.writeFileSync(target, api.serializeExport(result.file));
console.log(
  'wrote ' + path.relative(process.cwd(), target) +
  ': ' + result.file.sessions.length + ' sessions, ' +
  result.file.sessionExercises.length + ' exercises, ' +
  result.file.sets.length + ' sets'
);
