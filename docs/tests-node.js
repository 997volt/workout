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

editorTests.run(H, shared, t, globalThis.PROGRAM_EDIT_CASES);

console.log(passes + ' passed, ' + failures + ' failed');
process.exit(failures === 0 ? 0 : 1);
