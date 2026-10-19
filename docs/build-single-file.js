#!/usr/bin/env node
/*
 * Builds one self-contained HTML file from index.html, style.css and app.js (N164).
 *
 *     node docs/build-single-file.js [output]
 *
 * The output defaults to `dist/workout-import.html`, which is not committed: the
 * three files beside it are the source, and this is the portable copy. It refuses
 * to write a file that still names an external stylesheet or script, because the
 * whole point is that the copy has no neighbours.
 */
'use strict';

var fs = require('fs');
var path = require('path');

var root = __dirname;
var html = fs.readFileSync(path.join(root, 'index.html'), 'utf8');
var css = fs.readFileSync(path.join(root, 'style.css'), 'utf8');
var seed = fs.readFileSync(path.join(root, 'seed-catalog.js'), 'utf8');
var js = fs.readFileSync(path.join(root, 'app.js'), 'utf8');

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

var out = html
  .replace('<link rel="stylesheet" href="style.css">', '<style>\n' + css + '\n</style>')
  .replace('<script src="seed-catalog.js"></script>', '<script>\n' + seed + '\n</script>')
  .replace('<script src="app.js"></script>', '<script>\n' + js + '\n</script>');

if (/<link[^>]+href="(?!data:)/.test(out) || /<script[^>]+src=/.test(out)) {
  console.error('build failed: an external stylesheet or script is still in the output');
  process.exit(1);
}

var target = process.argv[2] || path.join(root, '..', 'dist', 'workout-import.html');
fs.mkdirSync(path.dirname(target), { recursive: true });
fs.writeFileSync(target, out);
console.log('wrote ' + path.relative(process.cwd(), target) + ' (' + out.length + ' bytes)');
