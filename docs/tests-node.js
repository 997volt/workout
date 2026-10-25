#!/usr/bin/env node
/*
 * Runs the program editor's assertions under node (ROADMAP N173).
 *
 *     node docs/tests-node.js
 *
 * The browser page (`tests.html`) runs the same cases against the same scripts; this is the copy CI
 * can run without a browser. It loads the committed `docs/shared/` build, so a page that would not
 * load its core fails here rather than in someone's browser.
 */
'use strict';

// app.js decides browser-or-node by whether `window` exists, so it is required first; only then
// does the seed catalog get the `window` it assigns to.
var H = require('./app.js');

global.window = globalThis;
require('./seed-catalog.js');
require('./fixtures/program-edit.js');
var core = require('./shared/workout-shared.js');
var shared = core && core.com && core.com.example && core.com.example.androidapp
  ? core.com.example.androidapp.shared
  : null;
if (!shared) {
  console.error('the shared core did not load from docs/shared; run ./gradlew :shared:syncSharedJs');
  process.exit(1);
}
global.window['workout:shared'] = core;

var editorTests = require('./editor-tests.js');

/*
 * The declared count, the instrumented check's rule (N181): the fixture is a global, so a renamed one
 * left the loop over nothing and every case passed by being absent. Executed must equal declared, and
 * adding a case is a deliberate edit here as well as in the fixture.
 */
var EXPECTED_FIXTURE_CASES = 6;

/*
 * The checks that must run, named ones and the fixture loop together (N181, B199): the count is of
 * checks executed, not of cases handed in, so a block of `check` calls behind an early return is a
 * failure here rather than a number that quietly fell.
 */
var EXPECTED_CHECKS = 40;
if (!Array.isArray(globalThis.PROGRAM_EDIT_CASES)) {
  console.error('the edit fixture did not load: PROGRAM_EDIT_CASES is not an array');
  process.exit(1);
}
if (globalThis.PROGRAM_EDIT_CASES.length !== EXPECTED_FIXTURE_CASES) {
  console.error('the edit fixture holds ' + globalThis.PROGRAM_EDIT_CASES.length +
    ' cases; the runner declares ' + EXPECTED_FIXTURE_CASES);
  process.exit(1);
}

var passes = 0;
var failures = 0;

var t = {
  check: function (name, body) {
    try {
      body();
      passes += 1;
      console.log('PASS  ' + name);
    } catch (error) {
      failures += 1;
      console.log('FAIL  ' + name + ' \u2014 ' + (error && error.message ? error.message : error));
    }
  },
  ok: function (value, message) {
    if (!value) { throw new Error(message || 'expected a truthy value'); }
  },
  eq: function (actual, expected, message) {
    if (actual !== expected) {
      throw new Error((message ? message + ': ' : '') +
        'expected ' + JSON.stringify(expected) + ', got ' + JSON.stringify(actual));
    }
  }
};

var checked = editorTests.run(H, shared, t, globalThis.PROGRAM_EDIT_CASES);

/*
 * The body reader is not an editor case, but this runner is the gate CI runs, so its one regression lives
 * here as well (B220). A real Waistline export dates each day with `dateTime`; the day comes from its own
 * text, not through a local `Date`, which would move a UTC midnight to the day before in the Americas.
 */
t.check('a Waistline export dated with dateTime imports its days (B220)', function () {
  var parsed = H.parseWaistlineDoc({
    version: 34,
    settings: { units: { weight: 'kg' } },
    diary: [
      { id: 1, dateTime: '2026-01-02T00:00:00.000Z', items: [], stats: { weight: 80.5 } },
      { id: 2, dateTime: '2026-01-01T00:00:00.000Z', items: [], stats: { weight: 81 } },
      { id: 3, items: [], stats: { weight: 82 } }
    ]
  });
  t.eq(parsed.unit, 'kg');
  t.eq(parsed.days.length, 2, 'the two dated days are read');
  t.eq(parsed.days[0].date, '2026-01-01');
  t.eq(parsed.days[1].date, '2026-01-02');
  t.eq(parsed.problems.length, 1, 'and the undated one is reported rather than dropped');
  t.eq(
    H.waistlineMeasurements(parsed, parsed.unit, 1)[0].measuredAt,
    H.noonOfDate('2026-01-01'),
    'a day becomes local noon, from the day the file named'
  );
});
checked += 1;

if (checked !== EXPECTED_CHECKS) {
  console.error('the runner executed ' + checked + ' checks; it declares ' + EXPECTED_CHECKS +
    ' (' + EXPECTED_FIXTURE_CASES + ' fixture cases and the named ones)');
  process.exit(1);
}

console.log(passes + ' passed, ' + failures + ' failed (' + checked + ' checks, ' +
  EXPECTED_FIXTURE_CASES + ' of them fixture cases)');
process.exit(failures === 0 ? 0 : 1);
