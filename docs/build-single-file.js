#!/usr/bin/env node
/*
 * Builds one self-contained HTML file from index.html, style.css and app.js (N164).
 *
 *     node docs/build-single-file.js [output]
 *
 * The output defaults to `dist/workout-import.html`, which is not committed: the
 * files beside it are the source, and this is the portable copy. It refuses to
 * write a file that still names an external stylesheet or script, because the
 * whole point is that the copy has no neighbours.
 *
 * The program editor's core (ROADMAP N170, N172) is part of the copy too: its five
 * generated scripts are inlined after the seed catalog, in the order index.html
 * loads them, so the portable page plans an edit the same way the served one does.
 */
'use strict';

var fs = require('fs');
var path = require('path');

var root = __dirname;
var html = fs.readFileSync(path.join(root, 'index.html'), 'utf8');
var css = fs.readFileSync(path.join(root, 'style.css'), 'utf8');
var seed = fs.readFileSync(path.join(root, 'seed-catalog.js'), 'utf8');
var js = fs.readFileSync(path.join(root, 'app.js'), 'utf8');

var sharedFiles = [
  'kotlin-kotlin-stdlib.js',
  'kotlin_org_jetbrains_kotlin_kotlin_dom_api_compat.js',
  'kotlinx-serialization-kotlinx-serialization-core.js',
  'kotlinx-serialization-kotlinx-serialization-json.js',
  'workout-shared.js'
];

if (html.indexOf('<link rel="stylesheet" href="style.css">') === -1) {
  console.error('build failed: index.html no longer links style.css the way this script expects');
  process.exit(1);
}
if (html.indexOf('<script src="seed-catalog.js"></script>') === -1) {
  console.error('build failed: index.html no longer loads seed-catalog.js the way this script expects');
  process.exit(1);
}
if (html.indexOf('<script src="app.js"></script>') === -1) {
  console.error('build failed: index.html no longer loads app.js the way this script expects');
  process.exit(1);
}
sharedFiles.forEach(function (name) {
  var tag = '<script src="shared/' + name + '"></script>';
  if (html.indexOf(tag) === -1) {
    console.error('build failed: index.html no longer loads shared/' + name + ' the way this script expects');
    process.exit(1);
  }
});

// Replacements are functions: a Kotlin/JS bundle contains `$'` and `$&`, which a string
// replacement would read as substitution patterns and splice the rest of the page into itself.
var out = html
  .replace('<link rel="stylesheet" href="style.css">', function () {
    return '<style>\n' + css + '\n</style>';
  })
  .replace('<script src="seed-catalog.js"></script>', function () {
    return '<script>\n' + seed + '\n</script>';
  })
  .replace('<script src="app.js"></script>', function () {
    return '<script>\n' + js + '\n</script>';
  });

sharedFiles.forEach(function (name) {
  var tag = '<script src="shared/' + name + '"></script>';
  var source = fs.readFileSync(path.join(root, 'shared', name), 'utf8');
  out = out.replace(tag, function () {
    return '<script>\n' + source + '\n</script>';
  });
});

if (/<link[^>]+href="(?!data:)/.test(out) || /<script[^>]+src=/.test(out)) {
  console.error('build failed: an external stylesheet or script is still in the output');
  process.exit(1);
}

var target = process.argv[2] || path.join(root, '..', 'dist', 'workout-import.html');
fs.mkdirSync(path.dirname(target), { recursive: true });
fs.writeFileSync(target, out);
console.log('wrote ' + path.relative(process.cwd(), target) + ' (' + out.length + ' bytes)');
