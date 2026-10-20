/*
 * Workout import helper.
 *
 * Turns a flat list of rows (JSON, or CSV/TSV) into a workout-backup.json at
 * schemaVersion 5, the only file the Android app imports.
 *
 * Constraints this file keeps, because they are the product:
 *  - Zero dependencies. Classic script, not a module, so it runs from file://.
 *  - No request of any kind. No network API, no socket, no beacon.
 *  - Training data lives in memory only. localStorage holds the exercise-name
 *    mapping (configuration), and nothing else.
 *
 * The pure half of this file (everything before the UI section) has no DOM or
 * storage access, so tests.html can assert over it directly.
 */
(function (root, factory) {
  'use strict';
  var api = factory();
  if (typeof window !== 'undefined') {
    window.ImportHelper = api;
    if (typeof document !== 'undefined') {
      if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () { api.init(); });
      } else {
        api.init();
      }
    }
  } else if (typeof module !== 'undefined' && module.exports) {
    module.exports = api;
  }
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict';

  /* ------------------------------------------------------------------ *
   * Constants
   * ------------------------------------------------------------------ */

  var SCHEMA_VERSION = 5;
  var DEFAULT_DURATION_MINUTES = 60;
  var DEFAULT_HOUR = 12;
  var DEFAULT_MINUTE = 0;
  var STORAGE_KEY = 'importHelper.mapping.v1';
  var BACKUP_FILE_NAME = 'workout-backup.json';

  /* A row's accepted keys. Anything else is an unknown key and warns. */
  var ROW_FIELDS = [
    'date', 'exercise', 'reps', 'weight', 'unit', 'rpe', 'type', 'notes',
    'time', 'durationMinutes', 'zoneOffsetMinutes', 'weightKg', 'weightLb'
  ];
  var DOC_FIELDS = ['source', 'rows', 'sessions'];

  /* The prompt offered for a model. It is deliberately the shape the parser
   * wants, and deliberately not the app's backup format. */
  var MODEL_PROMPT = [
    'You are transcribing one training spreadsheet into JSON for an import tool.',
    'Return ONE JSON object and nothing else. No explanation, no code fence.',
    '',
    '{ "source": "<what this sheet is>", "rows": [ ... ] }',
    '',
    'Each row is one set, in the order it appears in the sheet:',
    '{',
    '  "date": "24-10-2025",',
    '  "exercise": "Bench Press",',
    '  "reps": 8,',
    '  "weight": 60,',
    '  "unit": "kg",',
    '  "rpe": 7.5,',
    '  "type": "N",',
    '  "notes": null,',
    '  "time": "18:30",',
    '  "durationMinutes": 60',
    '}',
    '',
    'Rules:',
    '- date: "DD-MM-YYYY" or ISO "YYYY-MM-DD", four-digit year. null when the sheet shows no new date.',
    '- exercise: the name exactly as written. null when the sheet shows no new exercise.',
    '- reps: whole number. weight: number. unit: "kg" or "lb". null when the sheet does not show it.',
    '- rpe: 1 to 10, half points allowed. null when absent.',
    '- type: "W" warm-up, "N" working, "top" top set, "failure", or null. Copy the sheet; do not guess.',
    '- notes: a comment on the set, or null. time: "HH:MM" on a 24-hour clock, or null.',
    '- durationMinutes: the length of the session in minutes, or null.',
    '- A blank value carries down from the row above. Never repeat a value just to fill a cell.',
    '- Write the date again only when a new session starts on the same day.',
    '- Copy values exactly. Do not convert units, round, reorder, invent or merge rows.',
    '- Do not output ids, timestamps, grams, half-points or any other derived value.',
    '- If a value cannot be read, use null rather than a guess.',
    '',
    'Output the JSON object now.'
  ].join('\n');

  var ROLE_LABELS = {
    NORMAL: 'Working',
    WARMUP: 'Warm-up',
    TOP_SET: 'Top set',
    FAILURE: 'Failure',
    DROP: 'Drop (cannot import)',
    CLUSTER: 'Cluster (cannot import)'
  };
  var EXPORTABLE_ROLES = ['NORMAL', 'WARMUP', 'TOP_SET', 'FAILURE'];
  var REFUSED_ROLES = ['DROP', 'CLUSTER'];
  var ROLE_NAMES = ['NORMAL', 'WARMUP', 'TOP_SET', 'FAILURE', 'DROP', 'CLUSTER'];

  var MONTH_NAMES = [
    'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
    'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'
  ];

  /* ------------------------------------------------------------------ *
   * Small utilities
   * ------------------------------------------------------------------ */

  function isObject(value) {
    return value !== null && typeof value === 'object' && !Array.isArray(value);
  }

  function has(object, key) {
    return Object.prototype.hasOwnProperty.call(object, key);
  }

  function pad2(value) {
    var s = String(value);
    return s.length < 2 ? '0' + s : s;
  }

  function pad4(value) {
    var s = String(value);
    while (s.length < 4) { s = '0' + s; }
    return s;
  }

  function isPresent(value) {
    if (value === null || value === undefined) { return false; }
    if (typeof value === 'string') { return value.trim() !== ''; }
    return true;
  }

  function asText(value) {
    if (value === null || value === undefined) { return null; }
    if (typeof value === 'string') {
      var trimmed = value.trim();
      return trimmed === '' ? null : trimmed;
    }
    if (typeof value === 'number' && isFinite(value)) { return String(value); }
    return null;
  }

  function formatInt(value) {
    var rounded = Math.round(Math.abs(value));
    var text = String(rounded).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    return (value < 0 ? '-' : '') + text;
  }

  /* Numeric strings become numbers. Anything else is "not a number" rather
   * than a silent NaN. */
  function coerceNumber(value) {
    if (value === null || value === undefined) { return { ok: true, value: null }; }
    if (typeof value === 'number') {
      return isFinite(value) ? { ok: true, value: value } : { ok: false, value: null };
    }
    if (typeof value === 'string') {
      var text = value.trim();
      if (text === '') { return { ok: true, value: null }; }
      if (!/^[+-]?(\d+(\.\d+)?|\.\d+)$/.test(text)) {
        return { ok: false, value: null };
      }
      return { ok: true, value: Number(text) };
    }
    return { ok: false, value: null };
  }

  function isWholeNumber(value) {
    return typeof value === 'number' && isFinite(value) && Math.floor(value) === value;
  }

  /* ------------------------------------------------------------------ *
   * UTF-8 and FNV-1a, 64 bit, in two 32-bit lanes
   * ------------------------------------------------------------------ */

  function utf8Bytes(text) {
    var bytes = [];
    var source = String(text);
    for (var i = 0; i < source.length; i++) {
      var code = source.charCodeAt(i);
      if (code >= 0xd800 && code <= 0xdbff && i + 1 < source.length) {
        var next = source.charCodeAt(i + 1);
        if (next >= 0xdc00 && next <= 0xdfff) {
          code = 0x10000 + ((code - 0xd800) << 10) + (next - 0xdc00);
          i += 1;
        }
      }
      if (code < 0x80) {
        bytes.push(code);
      } else if (code < 0x800) {
        bytes.push(0xc0 | (code >> 6), 0x80 | (code & 0x3f));
      } else if (code < 0x10000) {
        bytes.push(0xe0 | (code >> 12), 0x80 | ((code >> 6) & 0x3f), 0x80 | (code & 0x3f));
      } else {
        bytes.push(
          0xf0 | (code >> 18),
          0x80 | ((code >> 12) & 0x3f),
          0x80 | ((code >> 6) & 0x3f),
          0x80 | (code & 0x3f)
        );
      }
    }
    return bytes;
  }

  function hex8(value) {
    var text = (value >>> 0).toString(16);
    while (text.length < 8) { text = '0' + text; }
    return text;
  }

  /* FNV-1a 64: offset basis 0xcbf29ce484222325, prime 0x100000001b3. */
  function fnv1a64Hex(text) {
    var hi = 0xcbf29ce4 >>> 0;
    var lo = 0x84222325 >>> 0;
    var bytes = utf8Bytes(text);
    for (var i = 0; i < bytes.length; i++) {
      lo = (lo ^ bytes[i]) >>> 0;
      var product = lo * 0x1b3;
      var nextLo = product >>> 0;
      var carry = Math.floor(product / 4294967296);
      var nextHi = (hi * 0x1b3 + lo * 0x100 + carry) >>> 0;
      hi = nextHi;
      lo = nextLo;
    }
    return hex8(hi) + hex8(lo);
  }

  /* Whether a loaded row may be named by a logged set.
   *
   * The app states it (RowKind.isLoggable, N95): a logged set names a MOVEMENT, and a
   * CATEGORY is a head that groups movements. Five seed names are both — Deadlift, Hip
   * Thrust, Lat Pulldown, Lateral Raise, Overhead Press — and the export is ordered by
   * name, so a lookup by name was free to pick the head: "Lat Pulldown" mapped to
   * lat-pulldown-family and the file would have attached the sets to a family.
   *
   * An export written before N95 carries no rowKind at all, and every row in it is a
   * movement, so only an explicit CATEGORY is excluded.
   */
  function isLoggableRow(exercise) {
    return isObject(exercise) && exercise.rowKind !== 'CATEGORY';
  }

  var CREATE_LABEL = 'Create new exercise';

  /* Alphabetical for the person choosing: case- and accent-insensitive, so "É" sorts by its letter. */
  function compareExerciseNames(a, b) {
    return String(a).localeCompare(String(b), undefined, { sensitivity: 'base' });
  }

  function normalizeName(name) {
    var text = name === null || name === undefined ? '' : String(name);
    if (typeof text.normalize === 'function') {
      try { text = text.normalize('NFKC'); } catch (error) { /* older engines */ }
    }
    return text.replace(/\s+/g, ' ').trim().toLowerCase();
  }

  function hashParts(parts) {
    return 'imp_' + fnv1a64Hex(parts.join('|'));
  }

  /* id = "imp_" + fnv64(seed | kind | sheet | row | date)
   *
   * The date is part of a session's identity, not only its row. Two sheets loaded
   * under the same revision both start at row 1, and without the date the app's
   * merge would treat the second sheet's session as already present and skip it
   * (B173). A re-import of the same row still produces the same id, which is what
   * keeps a second import a no-op.
   */
  function makeId(seed, kind, sheet, row, dateKey) {
    return hashParts([
      String(seed), String(kind), String(sheet), String(row), String(dateKey || '')
    ]);
  }

  /* The date an id is keyed on: the session's own ISO date, or "none" for a row
   * with no date (which the validator refuses before an export is written). */
  function dateKeyOf(date) {
    return date && date.iso ? date.iso : 'none';
  }

  /* An exercise keys on its normalised name only, so the same name is one
   * exercise across files and revisions. `variant` exists only to avoid the id
   * of a library row the user deleted: a document never brings that row back. */
  function makeExerciseId(name, variant) {
    var normalized = normalizeName(name);
    var payload = variant ? ('ex|' + variant + '|' + normalized) : ('ex|' + normalized);
    return 'imp_' + fnv1a64Hex(payload);
  }

  function makeSessionId(seed, sheet, row, dateKey) {
    return makeId(seed, 'sess', sheet, row, dateKey);
  }

  function makeSetId(seed, sheet, row, dateKey) {
    return makeId(seed, 'set', sheet, row, dateKey);
  }

  /* A session exercise derives from its session and its 1-based position. */
  function makeSessionExerciseId(seed, sheet, sessionRow, dateKey, position) {
    return hashParts([
      String(seed), 'sess', String(sheet), String(sessionRow), String(dateKey || ''),
      'se', String(position)
    ]);
  }

  /* ------------------------------------------------------------------ *
   * Dates and times (hand-written: no Date.parse)
   * ------------------------------------------------------------------ */

  function isLeapYear(year) {
    return year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
  }

  function daysInMonth(year, month) {
    if (month === 2) { return isLeapYear(year) ? 29 : 28; }
    var lengths = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
    return lengths[month - 1] || 0;
  }

  function dateResult(year, month, day, ambiguous) {
    if (year < 1 || year > 9999) {
      return { ok: false, empty: false, error: 'the year is out of range' };
    }
    if (month < 1 || month > 12) {
      return { ok: false, empty: false, error: 'the month is out of range' };
    }
    if (day < 1 || day > daysInMonth(year, month)) {
      return { ok: false, empty: false, error: 'the day is out of range for that month' };
    }
    return {
      ok: true,
      empty: false,
      y: year,
      month: month,
      day: day,
      iso: pad4(year) + '-' + pad2(month) + '-' + pad2(day),
      ambiguous: !!ambiguous && day <= 12 && month <= 12,
      error: null
    };
  }

  /* order is "dmy" (default) or "mdy". ISO dates are order-independent. A number
   * is epoch milliseconds, the escape hatch for a model that emits timestamps. */
  function parseDateValue(value, order) {
    var mode = order === 'mdy' ? 'mdy' : 'dmy';
    if (value === null || value === undefined) {
      return { ok: false, empty: true, error: null };
    }
    if (typeof value === 'number') {
      // A plausible millisecond timestamp is 1973..2286; anything else (a packed
      // 20240304, say) is a number we cannot read rather than a date in 1970.
      if (!isFinite(value) || value < 1e11 || value > 1e13) {
        return { ok: false, empty: false, error: 'a numeric date must be epoch milliseconds' };
      }
      var epochDate = new Date(value);
      if (isNaN(epochDate.getTime())) {
        return { ok: false, empty: false, error: 'that timestamp is not a date' };
      }
      return dateResult(
        epochDate.getFullYear(), epochDate.getMonth() + 1, epochDate.getDate(), false
      );
    }
    if (typeof value !== 'string') {
      return { ok: false, empty: false, error: 'a date must be written as text' };
    }
    var text = value.trim();
    if (text === '') { return { ok: false, empty: true, error: null }; }

    var iso = /^(\d{4})-(\d{1,2})-(\d{1,2})$/.exec(text);
    if (iso) {
      return dateResult(Number(iso[1]), Number(iso[2]), Number(iso[3]), false);
    }

    if (/^\d{1,2}[-/.]\d{1,2}[-/.]\d{2}$/.test(text)) {
      return {
        ok: false,
        empty: false,
        error: 'two-digit years are refused; write a four-digit year'
      };
    }

    var parts = /^(\d{1,2})([-/.])(\d{1,2})\2(\d{4})$/.exec(text);
    if (parts) {
      var first = Number(parts[1]);
      var second = Number(parts[3]);
      var year = Number(parts[4]);
      var day = mode === 'mdy' ? second : first;
      var month = mode === 'mdy' ? first : second;
      return dateResult(year, month, day, true);
    }

    return { ok: false, empty: false, error: 'not a recognised date format' };
  }

  function parseTimeValue(value) {
    if (value === null || value === undefined) {
      return { ok: true, empty: true, hh: DEFAULT_HOUR, mm: DEFAULT_MINUTE, error: null };
    }
    if (typeof value !== 'string') {
      if (typeof value === 'number' && isFinite(value)) {
        return { ok: false, empty: false, hh: DEFAULT_HOUR, mm: DEFAULT_MINUTE, error: 'a time must be written as text' };
      }
      return { ok: false, empty: false, hh: DEFAULT_HOUR, mm: DEFAULT_MINUTE, error: 'a time must be written as text' };
    }
    var text = value.trim();
    if (text === '') {
      return { ok: true, empty: true, hh: DEFAULT_HOUR, mm: DEFAULT_MINUTE, error: null };
    }
    var match = /^(\d{1,2})(?::(\d{1,2}))(?::\d{1,2})?$/.exec(text);
    if (!match) {
      match = /^(\d{1,2})[.](\d{1,2})$/.exec(text);
    }
    if (!match) {
      return { ok: false, empty: false, hh: DEFAULT_HOUR, mm: DEFAULT_MINUTE, error: 'not a recognised time' };
    }
    var hh = Number(match[1]);
    var mm = Number(match[2]);
    if (hh < 0 || hh > 23 || mm < 0 || mm > 59) {
      return { ok: false, empty: false, hh: DEFAULT_HOUR, mm: DEFAULT_MINUTE, error: 'the time is out of range' };
    }
    return { ok: true, empty: false, hh: hh, mm: mm, error: null };
  }

  /* Local wall-clock time to epoch millis. setFullYear avoids the 0-99 -> 1900
   * quirk of the Date constructor, for a four-digit year. */
  function localEpochMillis(year, month, day, hh, mm) {
    var date = new Date(0);
    date.setFullYear(year, month - 1, day);
    date.setHours(hh, mm, 0, 0);
    return date.getTime();
  }

  /* Minutes east of UTC: getTimezoneOffset counts minutes BEHIND UTC. */
  function zoneOffsetFromGetTimezoneOffset(value) {
    return -value;
  }

  function zoneOffsetMinutesFor(epochMillis) {
    return zoneOffsetFromGetTimezoneOffset(new Date(epochMillis).getTimezoneOffset());
  }

  function todayParts(nowMillis) {
    var date = typeof nowMillis === 'number' ? new Date(nowMillis) : new Date();
    return { y: date.getFullYear(), m: date.getMonth() + 1, d: date.getDate() };
  }

  function isFutureDate(year, month, day, nowMillis) {
    var today = todayParts(nowMillis);
    if (year !== today.y) { return year > today.y; }
    if (month !== today.m) { return month > today.m; }
    return day > today.d;
  }

  function formatDateDisplay(date) {
    if (!date || !date.ok) { return 'No date'; }
    return date.day + ' ' + MONTH_NAMES[date.month - 1] + ' ' + date.y;
  }

  function formatTime(hh, mm) {
    return pad2(hh) + ':' + pad2(mm);
  }

  function formatOffset(minutes) {
    if (typeof minutes !== 'number' || !isFinite(minutes)) { return ''; }
    var absolute = Math.abs(minutes);
    var sign = minutes < 0 ? '-' : '+';
    return 'UTC' + sign + pad2(Math.floor(absolute / 60)) + ':' + pad2(absolute % 60);
  }

  /* ------------------------------------------------------------------ *
   * Units
   * ------------------------------------------------------------------ */

  function parseUnit(value) {
    if (value === null || value === undefined) { return null; }
    if (typeof value !== 'string') { return false; }
    var text = value.trim().toLowerCase().replace(/\./g, '');
    if (text === '') { return null; }
    if (text === 'kg' || text === 'kgs' || text === 'kilo' || text === 'kilos' ||
        text === 'kilogram' || text === 'kilograms') {
      return 'kg';
    }
    if (text === 'lb' || text === 'lbs' || text === 'pound' || text === 'pounds') {
      return 'lb';
    }
    return false;
  }

  function unitToGrams(weight, unit) {
    if (typeof weight !== 'number' || !isFinite(weight)) { return null; }
    var grams = unit === 'lb' ? weight * 453.59237 : weight * 1000;
    return Math.round(grams);
  }

  function gramsToUnit(grams, unit) {
    if (typeof grams !== 'number' || !isFinite(grams)) { return null; }
    return unit === 'lb' ? grams / 453.59237 : grams / 1000;
  }

  function formatWeight(grams, unit) {
    var value = gramsToUnit(grams, unit);
    if (value === null) { return ''; }
    return value.toFixed(1);
  }

  function formatGrams(grams) {
    if (typeof grams !== 'number' || !isFinite(grams)) { return ''; }
    return formatInt(grams) + ' g';
  }

  function formatTonnage(grams) {
    if (typeof grams !== 'number' || !isFinite(grams)) { return '0.0 kg'; }
    var kilograms = grams / 1000;
    var fixed = kilograms.toFixed(1).split('.');
    return formatInt(Number(fixed[0])) + '.' + fixed[1] + ' kg';
  }

  function weightUnitName(unit) {
    return unit === 'lb' ? 'POUNDS' : 'KILOGRAMS';
  }

  /* ------------------------------------------------------------------ *
   * Roles
   * ------------------------------------------------------------------ */

  function normalizeRole(value) {
    if (value === null || value === undefined) {
      return { role: 'NORMAL', recognised: true, refused: false, raw: null };
    }
    if (typeof value !== 'string' && typeof value !== 'number') {
      return { role: 'NORMAL', recognised: false, refused: false, raw: String(value) };
    }
    var raw = String(value).trim();
    if (raw === '') {
      return { role: 'NORMAL', recognised: true, refused: false, raw: null };
    }
    var key = raw.toLowerCase().replace(/[-\s]+/g, ' ');
    var table = {
      'w': 'WARMUP', 'u': 'WARMUP', 'warmup': 'WARMUP', 'warm up': 'WARMUP', 'warm': 'WARMUP',
      'n': 'NORMAL', 'work': 'NORMAL', 'working': 'NORMAL', 'normal': 'NORMAL',
      'top': 'TOP_SET', 'top set': 'TOP_SET', 'topset': 'TOP_SET', '1rm': 'TOP_SET', '1 rm': 'TOP_SET',
      'failure': 'FAILURE', 'f': 'FAILURE', 'amrap': 'FAILURE',
      'drop': 'DROP', 'drop set': 'DROP', 'dropset': 'DROP',
      'cluster': 'CLUSTER', 'cluster set': 'CLUSTER', 'clusterset': 'CLUSTER'
    };
    if (has(table, key)) {
      var role = table[key];
      return {
        role: role,
        recognised: true,
        refused: REFUSED_ROLES.indexOf(role) !== -1,
        raw: raw
      };
    }
    return { role: 'NORMAL', recognised: false, refused: false, raw: raw };
  }

  function roleLabel(role) {
    return ROLE_LABELS[role] || role;
  }

  function roleRecordsEffort(role) {
    return role === 'NORMAL' || role === 'TOP_SET' || role === 'FAILURE';
  }

  function roleIsWork(role) {
    return role !== 'WARMUP';
  }

  /* ------------------------------------------------------------------ *
   * One lift's progress (N165)
   * ------------------------------------------------------------------ */

  var MAX_ESTIMATED_REPS = 12;
  var ESTIMATE_STEP_GRAMS = 500;

  /* Epley's estimate, exactly as the app states it (ExerciseTrendPoint.kt): a single
   * rep is the weight itself, the answer is rounded to half a kilo, and a set past
   * twelve reps has no estimate rather than a confident guess. */
  function estimatedOneRepMax(weightGrams, reps) {
    if (typeof weightGrams !== 'number' || !(weightGrams > 0)) { return null; }
    if (!Number.isInteger(reps) || reps < 1) { return null; }
    if (reps === 1) { return weightGrams; }
    if (reps > MAX_ESTIMATED_REPS) { return null; }
    return Math.round(weightGrams * (1 + reps / 30) / ESTIMATE_STEP_GRAMS) * ESTIMATE_STEP_GRAMS;
  }

  /* One point per session that logged the movement: the heaviest working set and its
   * estimate, in the order they were trained. Warm-ups are excluded, as they are
   * everywhere the app reads a load; a rung still counts, because this filter is the
   * app's own `setType != WARMUP` (N17). */
  function liftTrendPoints(backup, exerciseId) {
    if (!isObject(backup) || !Array.isArray(backup.sessions)) { return []; }
    var sessionsById = {};
    backup.sessions.forEach(function (session) {
      if (isObject(session) && typeof session.id === 'string') { sessionsById[session.id] = session; }
    });
    var linkSession = {};
    var bySession = {};
    (Array.isArray(backup.sessionExercises) ? backup.sessionExercises : []).forEach(function (link) {
      if (!isObject(link) || link.exerciseId !== exerciseId) { return; }
      if (typeof link.id !== 'string' || typeof link.sessionId !== 'string') { return; }
      var session = sessionsById[link.sessionId];
      if (!session || typeof session.startedAt !== 'number') { return; }
      linkSession[link.id] = link.sessionId;
      if (!has(bySession, link.sessionId)) {
        bySession[link.sessionId] = { startedAt: session.startedAt, sets: [] };
      }
    });
    (Array.isArray(backup.sets) ? backup.sets : []).forEach(function (set) {
      if (!isObject(set) || !has(linkSession, set.sessionExerciseId)) { return; }
      var sessionId = linkSession[set.sessionExerciseId];
      if (!has(bySession, sessionId)) { return; }
      bySession[sessionId].sets.push(set);
    });

    return Object.keys(bySession).map(function (sessionId) {
      var bucket = bySession[sessionId];
      var heaviest = null;
      bucket.sets.forEach(function (set) {
        if (typeof set.weightGrams !== 'number' || !(set.weightGrams > 0)) { return; }
        if (!roleIsWork(set.setType || 'NORMAL')) { return; }
        if (heaviest === null || set.weightGrams > heaviest.weightGrams) { heaviest = set; }
      });
      return {
        startedAt: bucket.startedAt,
        heaviestSetGrams: heaviest ? heaviest.weightGrams : null,
        estimatedOneRepMaxGrams: heaviest
          ? estimatedOneRepMax(heaviest.weightGrams, heaviest.reps)
          : null
      };
    }).sort(function (a, b) { return a.startedAt - b.startedAt; });
  }

  /* ------------------------------------------------------------------ *
   * CSV / TSV
   * ------------------------------------------------------------------ */

  var HEADER_ALIASES = {
    date: ['date', 'day', 'session date', 'session day', 'trained', 'trained on',
      'performed', 'performed on', 'workout date', 'when'],
    exercise: ['exercise', 'exercise name', 'name', 'lift', 'movement', 'movement name'],
    reps: ['reps', 'rep', 'repetitions', 'reps done', 'repetition count', 'repeats'],
    weight: ['weight', 'load', 'weight used', 'load used', 'weight kg', 'weight lb',
      'weight lbs', 'load kg', 'load lb', 'kg', 'kgs', 'kilos', 'kilograms',
      'lb', 'lbs', 'pounds'],
    unit: ['unit', 'units', 'weight unit', 'uom', 'measure'],
    rpe: ['rpe', 'rpe 1 10', 'effort', 'effort 1 10', 'rpe score'],
    type: ['type', 'set type', 'set', 'set role', 'role', 'kind', 'warm up', 'w'],
    notes: ['notes', 'note', 'comment', 'comments', 'remark', 'remarks'],
    time: ['time', 'clock', 'start time', 'started', 'start'],
    durationMinutes: ['duration', 'duration minutes', 'duration min', 'minutes',
      'mins', 'length', 'session length'],
    zoneOffsetMinutes: ['zone offset', 'zone offset minutes', 'utc offset', 'offset',
      'tz offset', 'timezone offset']
  };

  var WARMUP_HEADERS = ['warm up', 'warmup', 'warm'];

  function normalizeHeader(value) {
    return String(value === null || value === undefined ? '' : value)
      .toLowerCase()
      .replace(/\([^)]*\)/g, ' ')
      .replace(/[^a-z0-9]+/g, ' ')
      .trim();
  }

  function headerInfo(rawHeader) {
    var lower = String(rawHeader === null || rawHeader === undefined ? '' : rawHeader).toLowerCase();
    var unit = null;
    if (/\bkg\b|\bkgs\b|\bkilos?\b|\bkilograms?\b/.test(lower)) { unit = 'kg'; }
    if (/\blbs?\b|\bpounds?\b/.test(lower)) { unit = 'lb'; }
    var normalized = normalizeHeader(rawHeader);
    if (WARMUP_HEADERS.indexOf(normalized) !== -1) {
      return { field: 'type', warmupFlag: true, unit: null };
    }
    var names = Object.keys(HEADER_ALIASES);
    for (var i = 0; i < names.length; i++) {
      var field = names[i];
      if (HEADER_ALIASES[field].indexOf(normalized) !== -1) {
        return { field: field, warmupFlag: false, unit: field === 'weight' ? unit : null };
      }
    }
    /* "Weight (kg)" normalises to "weight kg", which is in the alias list. */
    return null;
  }

  function detectDelimiter(text) {
    var inQuotes = false;
    var tabs = 0;
    var commas = 0;
    var semicolons = 0;
    for (var i = 0; i < text.length; i++) {
      var character = text.charAt(i);
      if (character === '"') {
        if (inQuotes && text.charAt(i + 1) === '"') { i += 1; continue; }
        inQuotes = !inQuotes;
      } else if (!inQuotes) {
        if (character === '\n') { break; }
        if (character === '\t') { tabs += 1; }
        else if (character === ',') { commas += 1; }
        else if (character === ';') { semicolons += 1; }
      }
    }
    if (tabs > commas && tabs >= semicolons) { return '\t'; }
    if (semicolons > commas && semicolons > tabs && commas === 0) { return ';'; }
    return ',';
  }

  function parseDelimitedCells(text, delimiter) {
    var source = String(text).replace(/^\uFEFF/, '').replace(/\r\n?/g, '\n');
    var rows = [];
    var row = [];
    var cell = '';
    var inQuotes = false;
    for (var i = 0; i < source.length; i++) {
      var character = source.charAt(i);
      if (inQuotes) {
        if (character === '"') {
          if (source.charAt(i + 1) === '"') {
            cell += '"';
            i += 1;
          } else {
            inQuotes = false;
          }
        } else {
          cell += character;
        }
      } else if (character === '"' && cell === '') {
        inQuotes = true;
      } else if (character === delimiter) {
        row.push(cell);
        cell = '';
      } else if (character === '\n') {
        row.push(cell);
        rows.push(row);
        row = [];
        cell = '';
      } else {
        cell += character;
      }
    }
    if (cell !== '' || row.length > 0) {
      row.push(cell);
      rows.push(row);
    }
    return rows;
  }

  function isTruthyFlag(value) {
    if (!isPresent(value)) { return false; }
    return /^(x|yes|y|true|1|w|warm|warmup|warm up|warm-up)$/i.test(String(value).trim());
  }

  /* Returns { rows, warnings, problems, format } or a problem-shaped failure. */
  function parseCsvText(text, options) {
    options = options || {};
    var delimiter = detectDelimiter(text);
    var cells = parseDelimitedCells(text, delimiter);
    var warnings = [];
    var problems = [];

    while (cells.length > 0 && cells[cells.length - 1].every(function (cell) {
      return String(cell).trim() === '';
    })) {
      cells.pop();
    }
    if (cells.length === 0) {
      return {
        kind: 'error',
        format: 'csv',
        rows: [],
        warnings: warnings,
        problems: [{ path: 'root', message: 'That file is empty.' }]
      };
    }

    var headers = cells[0];
    var columns = [];
    var usedFields = {};
    for (var index = 0; index < headers.length; index++) {
      var info = headerInfo(headers[index]);
      if (!info) {
        if (String(headers[index]).trim() !== '') {
          warnings.push({
            path: 'rows.' + String(headers[index]).trim(),
            message: 'Unrecognised column "' + String(headers[index]).trim() + '" was ignored.'
          });
        }
        continue;
      }
      if (info.field !== 'type' && has(usedFields, info.field)) {
        warnings.push({
          path: 'rows.' + String(headers[index]).trim(),
          message: 'Column "' + String(headers[index]).trim() + '" repeats ' + info.field + ' and was ignored.'
        });
        continue;
      }
      usedFields[info.field] = true;
      columns.push({ index: index, field: info.field, unit: info.unit, warmupFlag: info.warmupFlag });
    }

    if (columns.length === 0) {
      return {
        kind: 'error',
        format: 'csv',
        rows: [],
        warnings: warnings,
        problems: [{
          path: 'columns',
          message: 'The first line has no recognised column headers. Use date, exercise, reps and weight.'
        }]
      };
    }

    var rows = [];
    for (var rowIndex = 1; rowIndex < cells.length; rowIndex++) {
      var cellsRow = cells[rowIndex];
      var blank = true;
      for (var b = 0; b < cellsRow.length; b++) {
        if (String(cellsRow[b]).trim() !== '') { blank = false; break; }
      }
      if (blank) { continue; }
      var object = {};
      for (var c = 0; c < columns.length; c++) {
        var column = columns[c];
        var value = column.index < cellsRow.length ? cellsRow[column.index] : '';
        var trimmed = String(value).trim();
        if (column.warmupFlag) {
          if (isTruthyFlag(trimmed)) { object.type = 'W'; }
          continue;
        }
        if (trimmed === '') {
          if (column.field !== 'unit') { object[column.field] = null; }
        } else if (column.field === 'weight' && column.unit && object.unit === undefined) {
          object.weight = trimmed;
          object.unit = column.unit;
        } else {
          object[column.field] = trimmed;
        }
      }
      rows.push(object);
    }

    if (rows.length === 0) {
      problems.push({ path: 'rows', message: 'The file has headers but no data rows.' });
    }

    return {
      kind: 'rows',
      format: 'csv',
      rows: rows,
      warnings: warnings,
      problems: problems,
      source: options.fileName || 'pasted-csv'
    };
  }

  /* ------------------------------------------------------------------ *
   * Input documents: JSON rows, nested sessions, app backups
   * ------------------------------------------------------------------ */

  function flattenNestedSessions(doc, warnings, problems) {
    var rows = [];
    var sessions = doc.sessions;
    var sessionFields = ['date', 'sessionDate', 'startedAt', 'time', 'durationMinutes',
      'zoneOffsetMinutes', 'notes', 'exercises'];
    var exerciseFields = ['name', 'exercise', 'unit', 'sets'];
    var setFields = ROW_FIELDS;

    for (var si = 0; si < sessions.length; si++) {
      var session = sessions[si];
      if (!isObject(session)) {
        problems.push({ path: 'sessions[' + si + ']', message: 'A session must be an object.' });
        continue;
      }
      Object.keys(session).forEach(function (key) {
        if (sessionFields.indexOf(key) === -1) {
          warnings.push({
            path: 'sessions[' + si + '].' + key,
            message: 'Unrecognised key "' + key + '" in a session was ignored.'
          });
        }
      });

      var dateValue = null;
      if (isPresent(session.date)) {
        dateValue = session.date;
      } else if (isPresent(session.sessionDate)) {
        dateValue = session.sessionDate;
      }
      var timeValue = isPresent(session.time) ? session.time : null;
      if (typeof session.startedAt === 'number' && isFinite(session.startedAt)) {
        var started = new Date(session.startedAt);
        if (dateValue === null) {
          dateValue = pad4(started.getFullYear()) + '-' + pad2(started.getMonth() + 1) +
            '-' + pad2(started.getDate());
        }
        if (timeValue === null) {
          timeValue = formatTime(started.getHours(), started.getMinutes());
        }
      }

      if (!Array.isArray(session.exercises)) {
        problems.push({
          path: 'sessions[' + si + '].exercises',
          message: 'A session must carry an exercises list.'
        });
        continue;
      }

      for (var ei = 0; ei < session.exercises.length; ei++) {
        var exercise = session.exercises[ei];
        if (!isObject(exercise)) {
          problems.push({
            path: 'sessions[' + si + '].exercises[' + ei + ']',
            message: 'An exercise must be an object.'
          });
          continue;
        }
        Object.keys(exercise).forEach(function (key) {
          if (exerciseFields.indexOf(key) === -1) {
            warnings.push({
              path: 'sessions[' + si + '].exercises[' + ei + '].' + key,
              message: 'Unrecognised key "' + key + '" in an exercise was ignored.'
            });
          }
        });
        var name = isPresent(exercise.name) ? exercise.name : exercise.exercise;
        if (!Array.isArray(exercise.sets)) {
          problems.push({
            path: 'sessions[' + si + '].exercises[' + ei + '].sets',
            message: 'An exercise must carry a sets list.'
          });
          continue;
        }
        for (var ti = 0; ti < exercise.sets.length; ti++) {
          var set = exercise.sets[ti];
          if (!isObject(set)) {
            problems.push({
              path: 'sessions[' + si + '].exercises[' + ei + '].sets[' + ti + ']',
              message: 'A set must be an object.'
            });
            continue;
          }
          Object.keys(set).forEach(function (key) {
            if (setFields.indexOf(key) === -1) {
              warnings.push({
                path: 'sessions[' + si + '].exercises[' + ei + '].sets[' + ti + '].' + key,
                message: 'Unrecognised key "' + key + '" in a set was ignored.'
              });
            }
          });
          var row = {};
          if (ti === 0 && ei === 0 && isPresent(dateValue)) { row.date = dateValue; }
          if (ti === 0 && isPresent(name)) { row.exercise = name; }
          if (ti === 0 && ei === 0) {
            if (timeValue !== null) { row.time = timeValue; }
            if (isPresent(session.durationMinutes)) { row.durationMinutes = session.durationMinutes; }
            if (isPresent(session.zoneOffsetMinutes)) { row.zoneOffsetMinutes = session.zoneOffsetMinutes; }
          }
          ['reps', 'weight', 'unit', 'weightKg', 'weightLb', 'rpe', 'type', 'notes', 'time',
            'durationMinutes', 'zoneOffsetMinutes'].forEach(function (field) {
            if (has(set, field)) { row[field] = set[field]; }
          });
          rows.push(row);
        }
      }
    }
    return rows;
  }

  function documentSource(doc, options) {
    options = options || {};
    if (doc && isPresent(doc.source)) { return String(doc.source).trim(); }
    if (options.fileName) { return String(options.fileName); }
    return 'pasted-rows';
  }

  /* Reads pasted or dropped text. Returns:
   *   { kind: 'rows'|'backup'|'error', format, rows, source, warnings, problems, backup } */
  function parseInputText(text, options) {
    options = options || {};
    var warnings = [];
    var problems = [];
    var raw = String(text === null || text === undefined ? '' : text).replace(/^\uFEFF/, '');
    var trimmed = raw.trim();

    if (trimmed === '') {
      return {
        kind: 'error',
        format: 'none',
        rows: [],
        source: options.fileName || 'pasted-rows',
        warnings: warnings,
        problems: [{ path: 'root', message: 'Paste or drop JSON rows or a CSV file first.' }]
      };
    }

    var looksJson = trimmed.charAt(0) === '{' || trimmed.charAt(0) === '[';
    if (!looksJson) {
      return parseCsvText(raw, options);
    }

    var doc;
    try {
      doc = JSON.parse(trimmed);
    } catch (error) {
      return {
        kind: 'error',
        format: 'json',
        rows: [],
        source: options.fileName || 'pasted-rows',
        warnings: warnings,
        problems: [{
          path: 'root',
          message: 'The input could not be read as JSON: ' + (error && error.message ? error.message : 'invalid JSON') + '.'
        }]
      };
    }

    if (!isObject(doc)) {
      return {
        kind: 'error',
        format: 'json',
        rows: [],
        source: options.fileName || 'pasted-rows',
        warnings: warnings,
        problems: [{ path: 'root', message: 'The input must be a JSON object with a rows list.' }]
      };
    }

    /* An app backup, not a row list. Say so instead of failing; the caller can
     * load it as the exercise library. */
    if (typeof doc.schemaVersion === 'number') {
      return {
        kind: 'backup',
        format: 'json',
        rows: [],
        source: documentSource(doc, options),
        warnings: warnings,
        problems: problems,
        backup: doc
      };
    }

    Object.keys(doc).forEach(function (key) {
      if (DOC_FIELDS.indexOf(key) === -1) {
        warnings.push({
          path: 'document.' + key,
          message: 'Unrecognised key "' + key + '" at the top level was ignored.'
        });
      }
    });

    var hasRows = has(doc, 'rows');
    var hasSessions = has(doc, 'sessions');
    if (!hasRows && !hasSessions) {
      return {
        kind: 'error',
        format: 'json',
        rows: [],
        source: documentSource(doc, options),
        warnings: warnings,
        problems: [{
          path: 'rows',
          message: 'The input has neither a rows list nor a sessions list.'
        }]
      };
    }

    var rows = [];
    if (hasRows) {
      if (!Array.isArray(doc.rows)) {
        problems.push({ path: 'rows', message: 'The rows value must be a list.' });
      } else {
        if (hasSessions) {
          warnings.push({
            path: 'sessions',
            message: 'Both rows and sessions were present; the rows list was used.'
          });
        }
        for (var i = 0; i < doc.rows.length; i++) {
          if (!isObject(doc.rows[i])) {
            problems.push({
              path: 'rows[' + i + ']',
              message: 'Each row must be an object.'
            });
            continue;
          }
          rows.push(doc.rows[i]);
        }
      }
    } else if (hasSessions) {
      if (!Array.isArray(doc.sessions)) {
        problems.push({ path: 'sessions', message: 'The sessions value must be a list.' });
      } else {
        rows = flattenNestedSessions(doc, warnings, problems);
      }
    }

    if (rows.length === 0 && problems.length === 0) {
      problems.push({ path: 'rows', message: 'The input has no rows.' });
    }

    return {
      kind: 'rows',
      format: 'json',
      rows: rows,
      source: documentSource(doc, options),
      warnings: warnings,
      problems: problems
    };
  }

  /* ------------------------------------------------------------------ *
   * The import model
   *
   * Rows are grouped by the notebook's carry-forward rules. An explicit date
   * always starts a session (a repeated explicit date starts a second session
   * on the same day) and clears the carried exercise; a blank date carries the
   * current session, a blank exercise carries the current block.
   * ------------------------------------------------------------------ */

  function firstIssue(issues, field) {
    if (!issues) { return null; }
    for (var i = 0; i < issues.length; i++) {
      if (issues[i].field === field) { return issues[i]; }
    }
    return null;
  }

  function normalizeRow(raw, sourceRow, order) {
    var row = {
      sourceRow: sourceRow,
      dateRaw: null,
      date: null,
      dateIssue: null,
      timeRaw: null,
      time: { hh: DEFAULT_HOUR, mm: DEFAULT_MINUTE },
      timeIssue: null,
      exercise: null,
      /* Whether the row said anything that makes it a set (N162). */
      setFields: false,
      /* A stated zone offset, or null to derive it from the browser's zone (N163). */
      zoneOffset: null,
      zoneOffsetIssue: null,
      reps: null,
      repsIssue: null,
      weight: null,
      weightIssue: null,
      unit: null,
      unitRaw: null,
      rpe: null,
      rpeIssue: null,
      role: 'NORMAL',
      roleRaw: null,
      roleRecognised: true,
      roleRefused: false,
      notes: null,
      durationMinutes: null,
      durationIssue: null,
      issues: [],
      isBlank: false
    };

    /* Date */
    if (raw.date !== null && raw.date !== undefined) {
      if (typeof raw.date === 'string' || typeof raw.date === 'number') {
        var dateText = String(raw.date).trim();
        if (dateText !== '') {
          row.dateRaw = dateText;
          var parsedDate = parseDateValue(raw.date, order);
          if (parsedDate.ok) {
            row.date = parsedDate;
          } else {
            row.dateIssue = {
              field: 'date',
              kind: parsedDate.error && parsedDate.error.indexOf('two-digit') !== -1 ? 'two-digit' : 'unreadable',
              raw: dateText,
              detail: parsedDate.error
            };
            row.issues.push(row.dateIssue);
          }
        }
      } else {
        row.dateRaw = String(raw.date);
        row.dateIssue = {
          field: 'date',
          kind: 'unreadable',
          raw: row.dateRaw,
          detail: 'a date must be written as text'
        };
        row.issues.push(row.dateIssue);
      }
    }

    /* Time (the session's start, default 12:00) */
    var parsedTime = parseTimeValue(raw.time);
    row.timeRaw = isPresent(raw.time) ? String(raw.time).trim() : null;
    row.time = { hh: parsedTime.hh, mm: parsedTime.mm };
    if (!parsedTime.ok) {
      row.timeIssue = {
        field: 'time',
        kind: 'unreadable',
        raw: row.timeRaw,
        detail: parsedTime.error
      };
      row.issues.push(row.timeIssue);
    }

    /* Exercise */
    if (isPresent(raw.exercise)) {
      var exerciseText = asText(raw.exercise);
      row.exercise = exerciseText === null ? String(raw.exercise).trim() : exerciseText;
    }

    /* Whether the sheet said anything that makes this row a set. A row with a
     * date or an exercise but neither reps nor weight is context — a day header
     * or an exercise heading — and must not become an incomplete set (N162). */
    row.setFields = isPresent(raw.reps) || isPresent(raw.weight) ||
      isPresent(raw.weightKg) || isPresent(raw.weightLb);

    /* A stated zone offset wins over the browser's zone (N163). */
    if (isPresent(raw.zoneOffsetMinutes)) {
      var parsedOffset = coerceNumber(raw.zoneOffsetMinutes);
      if (parsedOffset.ok && parsedOffset.value !== null &&
          Math.abs(parsedOffset.value) <= 840) {
        row.zoneOffset = Math.round(parsedOffset.value);
      } else {
        row.zoneOffsetIssue = {
          field: 'zoneOffsetMinutes',
          kind: 'unreadable',
          raw: String(raw.zoneOffsetMinutes),
          detail: 'the offset must be whole minutes from UTC, within ±840'
        };
        row.issues.push(row.zoneOffsetIssue);
      }
    }

    /* Unit first, then the weight, because weightKg / weightLb state a unit. */
    if (isPresent(raw.unit)) {
      row.unitRaw = String(raw.unit).trim();
      var parsedUnit = parseUnit(raw.unit);
      if (parsedUnit === false) {
        row.unitRaw = String(raw.unit).trim();
        row.issues.push({ field: 'unit', kind: 'unknown', raw: row.unitRaw });
      } else if (parsedUnit) {
        row.unit = parsedUnit;
      }
    }

    var weightSource = null;
    if (isPresent(raw.weightKg)) {
      weightSource = { value: raw.weightKg, unit: 'kg' };
    } else if (isPresent(raw.weightLb)) {
      weightSource = { value: raw.weightLb, unit: 'lb' };
    } else if (isPresent(raw.weight)) {
      weightSource = { value: raw.weight, unit: null };
    }
    if (weightSource !== null) {
      var parsedWeight = coerceNumber(weightSource.value);
      if (parsedWeight.ok && parsedWeight.value !== null) {
        row.weight = parsedWeight.value;
        if (weightSource.unit !== null) { row.unit = weightSource.unit; }
      } else if (!parsedWeight.ok) {
        row.weightIssue = {
          field: 'weight',
          kind: 'unreadable',
          raw: String(weightSource.value)
        };
        row.issues.push(row.weightIssue);
      }
    }

    /* Reps */
    var parsedReps = coerceNumber(raw.reps);
    if (parsedReps.ok) {
      if (parsedReps.value !== null) {
        if (isWholeNumber(parsedReps.value)) {
          row.reps = parsedReps.value;
        } else {
          row.repsIssue = { field: 'reps', kind: 'fraction', raw: String(raw.reps) };
          row.issues.push(row.repsIssue);
        }
      }
    } else if (isPresent(raw.reps)) {
      row.repsIssue = { field: 'reps', kind: 'unreadable', raw: String(raw.reps) };
      row.issues.push(row.repsIssue);
    }

    /* RPE */
    var parsedRpe = coerceNumber(raw.rpe);
    if (parsedRpe.ok) {
      row.rpe = parsedRpe.value;
    } else if (isPresent(raw.rpe)) {
      row.rpeIssue = { field: 'rpe', kind: 'unreadable', raw: String(raw.rpe) };
      row.issues.push(row.rpeIssue);
    }

    /* Type / role */
    var parsedRole = normalizeRole(raw.type);
    row.role = parsedRole.role;
    row.roleRaw = parsedRole.raw;
    row.roleRecognised = parsedRole.recognised;
    row.roleRefused = parsedRole.refused;
    if (!parsedRole.recognised) {
      row.issues.push({ field: 'type', kind: 'unknown', raw: parsedRole.raw });
    } else if (parsedRole.refused) {
      row.issues.push({ field: 'type', kind: 'refused', raw: parsedRole.raw });
    }

    /* Notes */
    if (isPresent(raw.notes)) {
      if (typeof raw.notes === 'string') {
        row.notes = raw.notes.trim() === '' ? null : raw.notes.trim();
      } else if (typeof raw.notes === 'number' && isFinite(raw.notes)) {
        row.notes = String(raw.notes);
      }
    }

    /* Duration */
    var parsedDuration = coerceNumber(raw.durationMinutes);
    if (parsedDuration.ok) {
      if (parsedDuration.value !== null) {
        row.durationMinutes = Math.round(parsedDuration.value);
      }
    } else if (isPresent(raw.durationMinutes)) {
      row.durationIssue = {
        field: 'durationMinutes',
        kind: 'unreadable',
        raw: String(raw.durationMinutes)
      };
      row.issues.push(row.durationIssue);
    }

    /* zoneOffsetMinutes is accepted but not trusted: the offset is derived from
     * the date in this browser's zone, date by date, as the spec requires. */

    row.isBlank = row.dateRaw === null && row.exercise === null && row.reps === null &&
      row.weight === null && row.unitRaw === null && row.rpe === null &&
      row.roleRaw === null && row.notes === null && row.timeRaw === null &&
      row.durationMinutes === null;

    return row;
  }

  /* The app's shipped library, so a name maps to a slug before any export is loaded (B176).
   * Generated from SeedExercises.kt into seed-catalog.js and kept equal by SeedCatalogTest. */
  function seededCatalog() {
    var rows = typeof window !== 'undefined' ? window.SEEDED_EXERCISES : null;
    if (!Array.isArray(rows) || rows.length === 0) { return null; }
    return {
      schemaVersion: SCHEMA_VERSION,
      exportedAt: 0,
      seeded: true,
      exercises: rows,
      sessions: [],
      sessionExercises: [],
      sets: []
    };
  }

  function normalizeCatalog(file) {
    var catalog = {
      loaded: false,
      file: null,
      schemaVersion: null,
      exercises: [],
      byId: {},
      byName: {},
      deletedByName: {},
      names: []
    };
    if (!isObject(file) || !Array.isArray(file.exercises)) { return catalog; }
    catalog.loaded = true;
    catalog.file = file;
    catalog.schemaVersion = typeof file.schemaVersion === 'number' ? file.schemaVersion : null;
    file.exercises.forEach(function (exercise) {
      if (!isObject(exercise) || typeof exercise.id !== 'string' || exercise.id === '') { return; }
      if (!isLoggableRow(exercise)) { return; }
      catalog.byId[exercise.id] = exercise;
      var name = normalizeName(exercise.name);
      if (!name) { return; }
      if (exercise.deletedAt === null || exercise.deletedAt === undefined) {
        if (!has(catalog.byName, name)) {
          catalog.byName[name] = exercise;
          catalog.names.push(String(exercise.name));
        }
      } else if (!has(catalog.deletedByName, name)) {
        catalog.deletedByName[name] = exercise;
      }
    });
    return catalog;
  }

  function usedUnitsForName(model, normalizedName) {
    var units = [];
    model.sessions.forEach(function (session) {
      session.blocks.forEach(function (block) {
        if (normalizeName(block.name) !== normalizedName) { return; }
        block.sets.forEach(function (set) {
          if (units.indexOf(set.unit) === -1) { units.push(set.unit); }
        });
      });
    });
    return units;
  }

  function newExerciseId(name, catalog) {
    var variants = ['', 'new', 'new2', 'new3', 'new4', 'new5', 'new6', 'new7', 'new8', 'new9'];
    for (var i = 0; i < variants.length; i++) {
      var id = makeExerciseId(name, variants[i]);
      if (!has(catalog.byId, id)) { return id; }
    }
    return makeExerciseId(name, 'new9');
  }

  function resolveExercise(name, model) {
    var catalog = model.catalog || normalizeCatalog(null);
    var normalized = normalizeName(name);
    var displayName = String(name === null || name === undefined ? '' : name).trim();
    var defaultUnit = has(model.nameUnits, normalized) && model.nameUnits[normalized] === 'lb' ? 'lb' : 'kg';
    var target = has(model.nameTargets, normalized) ? model.nameTargets[normalized] : undefined;

    function newResolution(deletedOnly) {
      var units = usedUnitsForName(model, normalized);
      var unit = units.length === 1 ? units[0] : defaultUnit;
      return {
        kind: 'new',
        id: newExerciseId(displayName, catalog),
        name: displayName,
        unit: unit,
        usedUnits: units,
        weightUnit: weightUnitName(unit),
        deletedOnly: deletedOnly
      };
    }

    if (!catalog.loaded) {
      return newResolution(false);
    }
    /* An explicit "unmatched" choice blocks; "create" always creates. */
    if (target === '') {
      return { kind: 'unmatched', name: displayName, missingTarget: false };
    }
    if (target === 'create') {
      return newResolution(has(catalog.deletedByName, normalized));
    }
    if (target !== undefined) {
      var byId = has(catalog.byId, target) ? catalog.byId[target] : null;
      if (byId && (byId.deletedAt === null || byId.deletedAt === undefined)) {
        return {
          kind: 'library', id: byId.id, name: String(byId.name), dto: byId,
          unit: defaultUnit, deletedOnly: false
        };
      }
      return { kind: 'unmatched', name: displayName, missingTarget: true };
    }
    if (has(catalog.byName, normalized)) {
      var live = catalog.byName[normalized];
      return {
        kind: 'library', id: live.id, name: String(live.name), dto: live,
        unit: defaultUnit, deletedOnly: false
      };
    }
    if (has(catalog.deletedByName, normalized)) {
      /* A document never brings back what the user deleted. */
      return newResolution(true);
    }
    return { kind: 'unmatched', name: displayName, missingTarget: false };
  }

  /* One loaded list: a seed and a sheet name of its own, so a session's id names
   * the file it came from and two lists cannot collide (N161, B173). A single
   * list keeps the older seed/sheet pair. */
  function normalizeSources(options, seed, sheet) {
    var given = Array.isArray(options.sources) ? options.sources : null;
    if (!given || given.length === 0) {
      return [{
        seed: seed,
        sheet: sheet,
        name: isPresent(options.source) ? String(options.source) : seed
      }];
    }
    return given.map(function (source, index) {
      var item = isObject(source) ? source : {};
      var sourceSeed = isPresent(item.seed) ? String(item.seed) : seed + '#' + (index + 1);
      return {
        seed: sourceSeed,
        sheet: isPresent(item.sheet) ? String(item.sheet) : sourceSeed,
        name: isPresent(item.name) ? String(item.name) : sourceSeed
      };
    });
  }

  function buildModel(rawRows, options) {
    options = options || {};
    var seed = isPresent(options.seed) ? String(options.seed) : 'rows';
    var sheet = isPresent(options.sheet) ? String(options.sheet) : seed;
    var order = options.dateOrder === 'mdy' ? 'mdy' : 'dmy';
    var catalog = normalizeCatalog(options.catalog);
    var nameUnits = options.nameUnits && isObject(options.nameUnits) ? options.nameUnits : {};
    var nameTargets = options.nameTargets && isObject(options.nameTargets) ? options.nameTargets : {};
    var sources = normalizeSources(options, seed, sheet);
    var localCounters = sources.map(function () { return 0; });
    var combineDates = Array.isArray(options.combineDates) ? options.combineDates.slice() : [];
    var inputWarnings = [];
    var rows = [];
    var skippedBlank = 0;
    var totalRows = Array.isArray(rawRows) ? rawRows.length : 0;

    for (var i = 0; i < totalRows; i++) {
      var raw = rawRows[i];
      if (!isObject(raw)) {
        inputWarnings.push({
          path: 'rows[' + i + ']',
          message: 'A row must be an object; it was skipped.'
        });
        continue;
      }
      Object.keys(raw).forEach(function (key) {
        // `__source` / `__row` are the tool's own annotations on a merged row, not input.
        if (key.charAt(0) === '_') { return; }
        if (ROW_FIELDS.indexOf(key) === -1) {
          inputWarnings.push({
            path: 'rows[' + i + '].' + key,
            message: 'Unrecognised key "' + key + '" was ignored (source row ' + (i + 1) + ').'
          });
        }
      });
      if (isPresent(raw.weightKg) && isPresent(raw.weightLb)) {
        inputWarnings.push({
          path: 'rows[' + i + '].weightLb',
          message: 'Both weightKg and weightLb are set; weightKg was used (source row ' + (i + 1) + ').'
        });
      }
      if (isPresent(raw.weight) && (isPresent(raw.weightKg) || isPresent(raw.weightLb))) {
        inputWarnings.push({
          path: 'rows[' + i + '].weight',
          message: 'Both weight and a unit-specific weight are set; the unit-specific value was used (source row ' + (i + 1) + ').'
        });
      }
      var row = normalizeRow(raw, i + 1, order);
      // Which loaded list this row came from, and its own row number in it, so ids
      // name a file rather than a position in the concatenation (N161, B173).
      var sourceIndex = typeof raw.__source === 'number' ? raw.__source : 0;
      if (sourceIndex < 0 || sourceIndex >= sources.length) { sourceIndex = 0; }
      row.sourceIndex = sourceIndex;
      localCounters[sourceIndex] += 1;
      row.localRow = localCounters[sourceIndex];
      if (row.isBlank) {
        skippedBlank += 1;
        continue;
      }
      rows.push(row);
    }

    /* The ambiguity guard runs both readings over every date cell, so the UI
     * can report them side by side. */
    var dateStats = {
      total: 0,
      readable: 0,
      ambiguous: 0,
      future: 0,
      before1970: 0,
      unreadable: 0,
      readings: {
        dmy: { readable: 0, ambiguous: 0, future: 0, before1970: 0, unreadable: 0 },
        mdy: { readable: 0, ambiguous: 0, future: 0, before1970: 0, unreadable: 0 }
      }
    };
    rows.forEach(function (row) {
      if (row.dateRaw === null) { return; }
      dateStats.total += 1;
      ['dmy', 'mdy'].forEach(function (mode) {
        var reading = dateStats.readings[mode];
        var parsed = parseDateValue(row.dateRaw, mode);
        if (!parsed.ok) {
          reading.unreadable += 1;
          return;
        }
        reading.readable += 1;
        if (parsed.ambiguous) { reading.ambiguous += 1; }
        if (isFutureDate(parsed.y, parsed.month, parsed.day)) { reading.future += 1; }
        if (parsed.y < 1970) { reading.before1970 += 1; }
      });
    });
    dateStats.readable = dateStats.readings[order].readable;
    dateStats.ambiguous = dateStats.readings[order].ambiguous;
    dateStats.future = dateStats.readings[order].future;
    dateStats.before1970 = dateStats.readings[order].before1970;
    dateStats.unreadable = dateStats.readings[order].unreadable;

    /* A unit is a property of the exercise name. Infer a default from the first
     * explicit row unit, only where the mapping does not already say. */
    var firstExplicitUnit = {};
    var namesInOrder = [];
    rows.forEach(function (row) {
      if (!row.exercise) { return; }
      var name = normalizeName(row.exercise);
      if (!name) { return; }
      if (namesInOrder.indexOf(name) === -1) { namesInOrder.push(name); }
      if (row.unit && !has(firstExplicitUnit, name)) { firstExplicitUnit[name] = row.unit; }
    });
    namesInOrder.forEach(function (name) {
      if (!has(nameUnits, name) || (nameUnits[name] !== 'kg' && nameUnits[name] !== 'lb')) {
        nameUnits[name] = has(firstExplicitUnit, name) ? firstExplicitUnit[name] : 'kg';
      }
    });

    var sessions = [];
    var currentSession = null;
    var currentBlock = null;

    function refreshSessionTiming(session) {
      if (session.startedAt === null || typeof session.durationMinutes !== 'number') {
        session.finishedAt = null;
        return;
      }
      session.finishedAt = session.startedAt + session.durationMinutes * 60000;
    }

    function makeSession(row) {
      var source = sources[row.sourceIndex] || sources[0];
      var session = {
        index: sessions.length,
        sourceRow: row.sourceRow,
        sourceIndex: sources.indexOf(source),
        sourceSeed: source.seed,
        sourceSheet: source.sheet,
        localRow: row.localRow,
        dateKey: dateKeyOf(row.date),
        dateRaw: row.dateRaw,
        date: row.date,
        timeRaw: row.timeRaw,
        hh: row.time.hh,
        mm: row.time.mm,
        startedAt: null,
        durationMinutes: DEFAULT_DURATION_MINUTES,
        durationSet: false,
        finishedAt: null,
        zoneOffsetMinutes: null,
        /* A stated offset is the lifter's and is not recomputed (N163). */
        manualOffset: false,
        notes: null,
        issues: [],
        blocks: []
      };
      if (row.dateIssue) {
        session.issues.push(row.dateIssue);
      } else if (row.dateRaw === null) {
        session.issues.push({
          field: 'date',
          kind: 'missing',
          raw: null,
          message: 'No date yet for this session (source row ' + row.sourceRow + ').'
        });
      }
      if (row.timeIssue) { session.issues.push(row.timeIssue); }
      if (row.durationIssue) { session.issues.push(row.durationIssue); }
      if (row.zoneOffsetIssue) { session.issues.push(row.zoneOffsetIssue); }
      if (row.durationMinutes !== null) {
        session.durationMinutes = row.durationMinutes;
        session.durationSet = true;
      }
      if (row.date) {
        session.startedAt = localEpochMillis(
          row.date.y, row.date.month, row.date.day, row.time.hh, row.time.mm
        );
        if (row.zoneOffset !== null && row.zoneOffset !== undefined) {
          session.zoneOffsetMinutes = row.zoneOffset;
          session.manualOffset = true;
        } else {
          session.zoneOffsetMinutes = zoneOffsetMinutesFor(session.startedAt);
        }
      }
      refreshSessionTiming(session);
      return session;
    }

    function makeBlock(name, row) {
      var block = {
        index: 0,
        sourceRow: row.sourceRow,
        name: name === null || name === undefined ? '' : String(name).trim(),
        placeholder: name === null || name === undefined || String(name).trim() === '',
        unit: 'kg',
        manualUnit: false,
        issues: [],
        sets: []
      };
      var normalized = normalizeName(block.name);
      if (normalized && has(nameUnits, normalized)) {
        block.unit = nameUnits[normalized];
      } else if (!normalized && row.unit) {
        block.unit = row.unit;
      }
      return block;
    }

    function addSet(block, row) {
      var source = sources[row.sourceIndex] || sources[0];
      var set = {
        index: block.sets.length + 1,
        sourceRow: row.sourceRow,
        sourceIndex: row.sourceIndex,
        sourceSeed: source.seed,
        sourceSheet: source.sheet,
        localRow: row.localRow,
        reps: row.reps,
        weight: row.weight,
        unit: row.unit || block.unit || 'kg',
        rpe: row.rpe,
        role: row.role,
        roleRaw: row.roleRaw,
        roleRecognised: row.roleRecognised,
        roleRefused: row.roleRefused,
        notes: row.notes,
        issues: []
      };
      row.issues.forEach(function (issue) {
        if (issue.field === 'reps' || issue.field === 'weight' || issue.field === 'rpe' ||
            issue.field === 'type' || issue.field === 'unit') {
          set.issues.push(issue);
        }
      });
      block.sets.push(set);
    }

    /* A context row's note belongs to its session, not to an incomplete set (N162). */
    function appendSessionNote(session, note) {
      if (note === null || note === undefined) { return; }
      var text = String(note).trim();
      if (text === '') { return; }
      session.notes = session.notes ? session.notes + '\n' + text : text;
    }

    /* A list boundary is a new session unless the two lists are combined on this
     * date, which is how two sheets of one day become one workout (N161). */
    function combinesWith(row, session) {
      if (session === null || !session.date) { return false; }
      if (combineDates.indexOf(session.date.iso) === -1) { return false; }
      if (row.date && row.date.iso !== session.date.iso) { return false; }
      return true;
    }

    var currentSource = null;
    rows.forEach(function (row) {
      var sourceChanged = currentSource === null || row.sourceIndex !== currentSource;
      var combined = sourceChanged && combinesWith(row, currentSession);

      if (row.dateRaw !== null) {
        /* Every explicit date is a boundary. A repeated one starts a second
         * session on the same day, and a new session never inherits an
         * exercise from the one above it. A combined boundary keeps the session
         * it is already in. */
        if (!combined) {
          currentSession = makeSession(row);
          sessions.push(currentSession);
          currentBlock = null;
        }
        currentSource = row.sourceIndex;
      } else if (currentSession === null) {
        currentSession = makeSession(row);
        sessions.push(currentSession);
        currentBlock = null;
        currentSource = row.sourceIndex;
      } else if (sourceChanged && !combined) {
        /* A list that starts without a date is its own session: the missing date
         * is flagged rather than inherited from the file above it. */
        currentSession = makeSession(row);
        sessions.push(currentSession);
        currentBlock = null;
        currentSource = row.sourceIndex;
      } else if (!currentSession.durationSet && row.durationMinutes !== null) {
        currentSession.durationMinutes = row.durationMinutes;
        currentSession.durationSet = true;
        refreshSessionTiming(currentSession);
      }
      if (currentSource === null) { currentSource = row.sourceIndex; }

      var hasSet = row.setFields || row.reps !== null || row.weight !== null;

      if (row.exercise !== null) {
        var normalized = normalizeName(row.exercise);
        if (currentBlock === null || currentBlock.placeholder ||
            normalizeName(currentBlock.name) !== normalized) {
          currentBlock = makeBlock(row.exercise, row);
          currentBlock.index = currentSession.blocks.length;
          currentSession.blocks.push(currentBlock);
        }
      } else if (!hasSet) {
        /* A date-only row opens the session and carries its note; it is not a set. */
        appendSessionNote(currentSession, row.notes);
        return;
      } else if (currentBlock === null) {
        currentBlock = makeBlock('', row);
        currentBlock.index = currentSession.blocks.length;
        currentBlock.issues.push({
          field: 'name',
          kind: 'missing',
          message: 'No exercise yet for source row ' + row.sourceRow + '.'
        });
        currentSession.blocks.push(currentBlock);
      }

      if (hasSet) {
        addSet(currentBlock, row);
      } else {
        /* An exercise heading with no set: it names the block, and its note is the session's. */
        appendSessionNote(currentSession, row.notes);
      }
    });

    sessions.forEach(function (session, index) { session.index = index; });

    return {
      sessions: sessions,
      seed: seed,
      sheet: sheet,
      sources: sources,
      combineDates: combineDates,
      order: order,
      source: isPresent(options.source) ? String(options.source) : seed,
      catalog: catalog,
      catalogFile: options.catalog || null,
      nameUnits: nameUnits,
      nameTargets: nameTargets,
      inputWarnings: inputWarnings,
      dateStats: dateStats,
      totalRows: totalRows,
      rowCount: rows.length,
      skippedBlank: skippedBlank
    };
  }

  /* ------------------------------------------------------------------ *
   * Validation
   * ------------------------------------------------------------------ */

  function isBodyweightName(name) {
    return /bodyweight|push.?up|pull.?up|chin.?up|\bdips?\b|plank|sit.?up|crunch|burpee|handstand|muscle.?up|air squat/.test(normalizeName(name));
  }

  function revalidateModel(model) {
    var problems = [];
    var warnings = [];
    var catalog = model.catalog || normalizeCatalog(null);
    var resolutions = {};

    function resolve(name) {
      var normalized = normalizeName(name);
      if (!has(resolutions, normalized)) {
        resolutions[normalized] = resolveExercise(name, model);
      }
      return resolutions[normalized];
    }

    function add(list, path, message) { list.push({ path: path, message: message }); }

    model.inputWarnings.forEach(function (warning) {
      warnings.push({ path: warning.path, message: warning.message });
    });

    /* Units per exercise name, gathered once. */
    var unitsByName = {};
    model.sessions.forEach(function (session) {
      session.blocks.forEach(function (block) {
        var normalized = normalizeName(block.name);
        if (!normalized) { return; }
        if (!has(unitsByName, normalized)) { unitsByName[normalized] = []; }
        block.sets.forEach(function (set) {
          if (unitsByName[normalized].indexOf(set.unit) === -1) {
            unitsByName[normalized].push(set.unit);
          }
        });
      });
    });

    var unitWarned = {};

    model.sessions.forEach(function (session, sessionIndex) {
      var sessionPath = 'sessions[' + sessionIndex + ']';

      if (session.startedAt === null || session.date === null) {
        var dateIssue = firstIssue(session.issues, 'date');
        if (dateIssue && dateIssue.kind === 'two-digit') {
          add(problems, sessionPath + '.startedAt',
            'Source row ' + session.sourceRow + ' has a two-digit year in "' + dateIssue.raw +
            '". Write the year in full.');
        } else if (dateIssue && dateIssue.kind === 'unreadable') {
          var expectedFormat = model.order === 'mdy' ? 'MM-DD-YYYY' : 'DD-MM-YYYY';
          add(problems, sessionPath + '.startedAt',
            'Source row ' + session.sourceRow + ' has an unreadable date "' + dateIssue.raw +
            '". Use ' + expectedFormat + ' (or the same with / or .) or YYYY-MM-DD with a four-digit year.');
        } else {
          add(problems, sessionPath + '.startedAt',
            'No date yet for this session (source row ' + session.sourceRow + ').');
        }
      } else {
        if (isFutureDate(session.date.y, session.date.month, session.date.day)) {
          add(warnings, sessionPath + '.startedAt', 'This session\'s date is in the future.');
        }
        if (session.date.y < 1970) {
          add(warnings, sessionPath + '.startedAt', 'This session\'s date is before 1970.');
        }
      }

      if (typeof session.durationMinutes !== 'number' || !isFinite(session.durationMinutes)) {
        add(problems, sessionPath + '.durationMinutes', 'A session needs a duration in minutes.');
      } else if (session.durationMinutes < 0) {
        add(problems, sessionPath + '.durationMinutes',
          'Duration must not be negative (got ' + session.durationMinutes + ').');
      } else if (session.durationMinutes === 0 || session.durationMinutes > 360) {
        add(warnings, sessionPath + '.durationMinutes',
          'A session duration of ' + session.durationMinutes +
          ' minutes is unusual (zero or more than six hours).');
      }

      var timeIssue = firstIssue(session.issues, 'time');
      if (timeIssue) {
        add(warnings, sessionPath + '.time',
          'Time "' + timeIssue.raw + '" is not readable; 12:00 was used (source row ' +
          session.sourceRow + ').');
      }

      var durationIssue = firstIssue(session.issues, 'durationMinutes');
      if (durationIssue) {
        add(warnings, sessionPath + '.durationMinutes',
          'Duration "' + durationIssue.raw + '" is not a number; 60 minutes was used (source row ' +
          session.sourceRow + ').');
      }

      var offsetIssue = firstIssue(session.issues, 'zoneOffsetMinutes');
      if (offsetIssue) {
        add(problems, sessionPath + '.zoneOffsetMinutes',
          'The zone offset on source row ' + session.sourceRow +
          ' must be whole minutes from UTC, within ±840.');
      }

      /* An undated session has no startedAt to finish; the date problem above is
       * the actionable one. A dated session still needs its finishedAt. */
      if (session.startedAt !== null &&
          (session.finishedAt === null || session.finishedAt === undefined)) {
        add(problems, sessionPath + '.finishedAt',
          'finishedAt is required: a session without it is read as a workout in progress.');
      }

      var workSets = 0;
      if (!session.blocks || session.blocks.length === 0) {
        // A date-only session is a note, not a workout: the export leaves it out
        // and says so, rather than refusing the file (N162).
        add(warnings, sessionPath + '.exercises',
          'This session has no exercises and will be left out.');
      } else {
        session.blocks.forEach(function (block, blockIndex) {
          var blockPath = sessionPath + '.exercises[' + blockIndex + ']';
          var normalizedName = normalizeName(block.name);
          var resolution = null;
          var blockSets = block.sets || [];

          if (blockSets.length === 0) {
            add(problems, blockPath + '.sets', 'This exercise block has no sets.');
          }

          if (!normalizedName) {
            var nameIssue = firstIssue(block.issues, 'name');
            add(problems, blockPath + '.name',
              nameIssue ? nameIssue.message : 'This exercise block has no name.');
          } else {
            resolution = resolve(block.name);
            if (resolution.kind === 'unmatched') {
              add(problems, blockPath + '.name',
                '"' + String(block.name).trim() +
                '" does not match an exercise in the loaded export. Choose an exercise, or create a new one.');
            } else if (resolution.kind === 'new') {
              if (resolution.deletedOnly) {
                add(warnings, blockPath + '.name',
                  '"' + String(block.name).trim() +
                  '" matches only a deleted exercise in the loaded export; a new exercise will be created under this name.');
              }
              var used = unitsByName[normalizedName] || [];
              if (used.length > 1) {
                add(warnings, blockPath + '.unit',
                  'This new exercise is recorded in both ' + used.join(' and ') +
                  '; its display unit is set to ' + resolution.weightUnit + '.');
              }
            } else if (resolution.kind === 'library' && resolution.dto) {
              var libraryUnit = resolution.dto.weightUnit;
              var usedUnits = unitsByName[normalizedName] || [];
              if ((libraryUnit === 'KILOGRAMS' || libraryUnit === 'POUNDS') &&
                  usedUnits.length === 1 &&
                  weightUnitName(usedUnits[0]) !== libraryUnit &&
                  !has(unitWarned, normalizedName)) {
                unitWarned[normalizedName] = true;
                add(warnings, blockPath + '.unit',
                  '"' + String(block.name).trim() + '" is recorded in ' + usedUnits[0] +
                  ' here, but the library exercise displays ' + libraryUnit +
                  '. The stored grams are correct; the app shows the exercise\'s own unit.');
              }
            }
          }

          var blockUnits = [];
          var warmups = 0;
          blockSets.forEach(function (set) {
            if (blockUnits.indexOf(set.unit) === -1) { blockUnits.push(set.unit); }
          });
          if (blockUnits.length > 1) {
            add(problems, blockPath + '.unit',
              'Mixed units in this exercise block (' + blockUnits.join(', ') +
              '). Use one unit for the whole block.');
          }

          blockSets.forEach(function (set, setIndex) {
            var setPath = blockPath + '.sets[' + setIndex + ']';

            if (set.role === 'WARMUP') { warmups += 1; }
            if (roleIsWork(set.role)) { workSets += 1; }

            /* Reps */
            var repsIssue = firstIssue(set.issues, 'reps');
            if (set.reps === null || set.reps === undefined) {
              if (repsIssue && repsIssue.kind === 'unreadable') {
                add(problems, setPath + '.reps',
                  'Reps "' + repsIssue.raw + '" is not a number (source row ' + set.sourceRow + ').');
              } else if (repsIssue && repsIssue.kind === 'fraction') {
                add(problems, setPath + '.reps',
                  'Reps "' + repsIssue.raw + '" must be a whole number (source row ' + set.sourceRow + ').');
              } else {
                add(problems, setPath + '.reps',
                  'Set ' + (setIndex + 1) + ' has no reps (source row ' + set.sourceRow + ').');
              }
            } else if (!isWholeNumber(set.reps)) {
              add(problems, setPath + '.reps',
                'Reps must be a whole number (got ' + set.reps + ').');
            } else if (set.reps < 1) {
              add(problems, setPath + '.reps', 'Reps must be at least 1 (got ' + set.reps + ').');
            } else if (set.reps > 50) {
              add(warnings, setPath + '.reps',
                set.reps + ' reps is unusual (more than 50).');
            }

            /* Weight */
            var weightIssue = firstIssue(set.issues, 'weight');
            if (set.weight === null || set.weight === undefined) {
              if (weightIssue && weightIssue.kind === 'unreadable') {
                add(problems, setPath + '.weight',
                  'Weight "' + weightIssue.raw + '" is not a number (source row ' + set.sourceRow + ').');
              } else {
                add(problems, setPath + '.weight',
                  'Set ' + (setIndex + 1) + ' has no weight (source row ' + set.sourceRow + ').');
              }
            } else if (set.weight < 0) {
              add(problems, setPath + '.weight',
                'Weight must not be negative (got ' + set.weight + ').');
            } else {
              var grams = unitToGrams(set.weight, set.unit);
              if (grams > 500000) {
                add(warnings, setPath + '.weight',
                  formatWeight(grams, 'kg') + ' kg is unusual (more than 500 kg).');
              }
              var bodyweight = false;
              if (resolution && resolution.kind === 'library' && resolution.dto &&
                  resolution.dto.equipment === 'BODYWEIGHT') {
                bodyweight = true;
              }
              if (!bodyweight && isBodyweightName(block.name)) { bodyweight = true; }
              if (!bodyweight && grams >= 1 && grams <= 999) {
                add(warnings, setPath + '.weight',
                  'This weight is ' + formatGrams(grams) +
                  ', between 1 and 999 grams. Did you mean kilograms? The app stores whole grams.');
              }
            }

            /* RPE */
            var rpeIssue = firstIssue(set.issues, 'rpe');
            if (set.rpe !== null && set.rpe !== undefined) {
              if (set.rpe < 1 || set.rpe > 10) {
                add(problems, setPath + '.rpe',
                  'RPE must be between 1 and 10 (got ' + set.rpe + ').');
              }
            } else if (rpeIssue) {
              add(warnings, setPath + '.rpe',
                'RPE "' + rpeIssue.raw + '" is not a number (source row ' + set.sourceRow + ').');
            }

            /* Role */
            if (REFUSED_ROLES.indexOf(set.role) !== -1) {
              add(problems, setPath + '.setType',
                '"' + (set.roleRaw || set.role) +
                '" is a rung: its load is derived from the set above it, so it cannot be imported. Enter the set as working.');
            } else if (!set.roleRecognised) {
              add(warnings, setPath + '.setType',
                'Set type "' + set.roleRaw + '" was not recognised and was read as working (source row ' +
                set.sourceRow + ').');
            }

            /* Unit spelling */
            var unitIssue = firstIssue(set.issues, 'unit');
            if (unitIssue) {
              add(warnings, setPath + '.unit',
                'Unit "' + unitIssue.raw + '" was not recognised; the exercise\'s unit was used (source row ' +
                set.sourceRow + ').');
            }
          });

          if (warmups > 8) {
            add(warnings, blockPath + '.sets',
              warmups + ' warm-ups on this exercise; more than 8 is unusual.');
          }
        });
      }

      if (workSets === 0 && session.blocks && session.blocks.length > 0) {
        add(warnings, sessionPath + '.exercises',
          'This session has no work sets; every set is a warm-up.');
      }
    });

    /* Two sessions overlapping in time. */
    var timed = [];
    model.sessions.forEach(function (session, index) {
      if (session.startedAt !== null && session.finishedAt !== null) {
        timed.push({ session: session, index: index });
      }
    });
    timed.sort(function (a, b) { return a.session.startedAt - b.session.startedAt; });
    var overlapWarned = {};
    for (var firstIndex = 0; firstIndex < timed.length; firstIndex++) {
      for (var secondIndex = firstIndex + 1; secondIndex < timed.length; secondIndex++) {
        var first = timed[firstIndex];
        var second = timed[secondIndex];
        if (has(overlapWarned, second.index)) { continue; }
        if (second.session.startedAt < first.session.finishedAt &&
            first.session.startedAt < second.session.finishedAt) {
          overlapWarned[second.index] = true;
          add(warnings, 'sessions[' + second.index + '].startedAt',
            'This session overlaps another session (' +
            formatDateDisplay(first.session.date) + ' ' +
            formatTime(first.session.hh, first.session.mm) + ').');
        }
      }
    }

    return { problems: problems, warnings: warnings };
  }

  /* ------------------------------------------------------------------ *
   * The backup file
   * ------------------------------------------------------------------ */

  function exerciseCopy(dto, fallbackTimestamp) {
    var copy = JSON.parse(JSON.stringify(dto));
    if (copy.primaryMuscle === undefined) { copy.primaryMuscle = null; }
    if (!Array.isArray(copy.secondaryMuscles)) { copy.secondaryMuscles = []; }
    if (copy.equipment === undefined) { copy.equipment = null; }
    if (copy.movementPattern === undefined) { copy.movementPattern = null; }
    if (typeof copy.createdAt !== 'number') { copy.createdAt = fallbackTimestamp; }
    if (typeof copy.updatedAt !== 'number') { copy.updatedAt = fallbackTimestamp; }
    if (copy.deletedAt === null) { delete copy.deletedAt; }
    return copy;
  }

  function newExerciseDto(resolution, timestamp) {
    return {
      id: resolution.id,
      name: resolution.name,
      primaryMuscle: null,
      secondaryMuscles: [],
      equipment: null,
      movementPattern: null,
      createdAt: timestamp,
      updatedAt: timestamp,
      weightUnit: resolution.weightUnit
    };
  }

  function resolutionPlan(model) {
    var byName = {};
    var order = [];
    model.sessions.forEach(function (session) {
      session.blocks.forEach(function (block) {
        var normalized = normalizeName(block.name);
        if (!normalized) { return; }
        if (!has(byName, normalized)) {
          byName[normalized] = resolveExercise(block.name, model);
          order.push(normalized);
        }
      });
    });
    return { byName: byName, order: order };
  }

  function computeExportStats(model) {
    var catalog = model.catalog || normalizeCatalog(null);
    var plan = resolutionPlan(model);
    var exerciseIds = [];
    var exerciseNames = [];

    plan.order.forEach(function (normalized) {
      var resolution = plan.byName[normalized];
      if (resolution.kind !== 'library' && resolution.kind !== 'new') { return; }
      if (exerciseIds.indexOf(resolution.id) === -1) {
        exerciseIds.push(resolution.id);
        exerciseNames.push(resolution.name);
      }
    });

    var sessionTotal = 0;
    var setTotal = 0;
    var generatedSessions = [];
    var generatedSets = [];
    model.sessions.forEach(function (session) {
      if (session.blocks.length === 0) { return; }
      sessionTotal += 1;
      generatedSessions.push(makeSessionId(
        session.sourceSeed, session.sourceSheet, session.localRow, session.dateKey
      ));
      session.blocks.forEach(function (block) {
        block.sets.forEach(function (set) {
          setTotal += 1;
          generatedSets.push(makeSetId(
            set.sourceSeed, set.sourceSheet, set.localRow, session.dateKey
          ));
        });
      });
    });

    var existingSessionIds = {};
    var existingSetIds = {};
    var existingExerciseIds = {};
    if (catalog.file) {
      (Array.isArray(catalog.file.sessions) ? catalog.file.sessions : []).forEach(function (session) {
        if (session && typeof session.id === 'string') { existingSessionIds[session.id] = true; }
      });
      (Array.isArray(catalog.file.sets) ? catalog.file.sets : []).forEach(function (set) {
        if (set && typeof set.id === 'string') { existingSetIds[set.id] = true; }
      });
      (Array.isArray(catalog.file.exercises) ? catalog.file.exercises : []).forEach(function (exercise) {
        if (exercise && typeof exercise.id === 'string') { existingExerciseIds[exercise.id] = true; }
      });
    }

    var skippedSessions = 0;
    generatedSessions.forEach(function (id) {
      if (has(existingSessionIds, id)) { skippedSessions += 1; }
    });
    var skippedSets = 0;
    generatedSets.forEach(function (id) {
      if (has(existingSetIds, id)) { skippedSets += 1; }
    });
    var newExercises = 0;
    var newNames = [];
    exerciseIds.forEach(function (id, index) {
      if (!has(existingExerciseIds, id)) {
        newExercises += 1;
        newNames.push(exerciseNames[index]);
      }
    });

    var unmatchedNames = [];
    plan.order.forEach(function (normalized) {
      if (plan.byName[normalized].kind === 'unmatched') {
        unmatchedNames.push(plan.byName[normalized].name);
      }
    });

    return {
      sessions: sessionTotal,
      sets: setTotal,
      exercises: exerciseIds.length,
      newExercises: newExercises,
      existingExercises: exerciseIds.length - newExercises,
      skippedSessions: skippedSessions,
      skippedSets: skippedSets,
      addedSessions: sessionTotal - skippedSessions,
      addedSets: setTotal - skippedSets,
      newExerciseNames: newNames,
      unmatchedNames: unmatchedNames
    };
  }

  function buildExport(model, options) {
    options = options || {};
    var validation = revalidateModel(model);
    var stats = computeExportStats(model);
    if (validation.problems.length > 0) {
      return {
        ok: false,
        file: null,
        json: '',
        problems: validation.problems,
        warnings: validation.warnings,
        stats: stats
      };
    }

    var catalog = model.catalog || normalizeCatalog(null);
    var exportedAt = typeof options.exportedAt === 'number' ? options.exportedAt : Date.now();
    var plan = resolutionPlan(model);
    var exercises = [];
    var exerciseIds = {};
    var firstTimestamp = null;

    model.sessions.forEach(function (session) {
      if (firstTimestamp === null && session.startedAt !== null) {
        firstTimestamp = session.startedAt;
      }
    });
    if (firstTimestamp === null) { firstTimestamp = exportedAt; }

    plan.order.forEach(function (normalized) {
      var resolution = plan.byName[normalized];
      if (resolution.kind !== 'library' && resolution.kind !== 'new') { return; }
      if (has(exerciseIds, resolution.id)) { return; }
      exerciseIds[resolution.id] = true;
      if (resolution.kind === 'library') {
        exercises.push(exerciseCopy(resolution.dto, firstTimestamp));
      } else {
        exercises.push(newExerciseDto(resolution, firstTimestamp));
      }
    });

    var sessions = [];
    var sessionExercises = [];
    var sets = [];
    var emptySessions = [];

    /* Chronological, ties by source order then row (B174): the app sorts on
     * read, but the file should say what it means. */
    var orderedSessions = model.sessions.slice().sort(function (a, b) {
      if (a.startedAt === null && b.startedAt === null) { return a.sourceRow - b.sourceRow; }
      if (a.startedAt === null) { return 1; }
      if (b.startedAt === null) { return -1; }
      if (a.startedAt !== b.startedAt) { return a.startedAt - b.startedAt; }
      return a.sourceRow - b.sourceRow;
    });

    orderedSessions.forEach(function (session) {
      if (session.startedAt === null || typeof session.durationMinutes !== 'number') { return; }
      // A date-only session is a note, not a workout: the app would show an empty
      // one, so it is left out and said (N162).
      if (session.blocks.length === 0) { emptySessions.push(session); return; }
      var startedAt = session.startedAt;
      var finishedAt = startedAt + session.durationMinutes * 60000;
      var sessionId = makeSessionId(
        session.sourceSeed, session.sourceSheet, session.localRow, session.dateKey
      );
      sessions.push({
        id: sessionId,
        startedAt: startedAt,
        finishedAt: finishedAt,
        zoneOffsetMinutes: session.zoneOffsetMinutes,
        notes: session.notes,
        createdAt: startedAt,
        updatedAt: startedAt
      });

      session.blocks.forEach(function (block, blockIndex) {
        var normalized = normalizeName(block.name);
        var resolution = has(plan.byName, normalized) ? plan.byName[normalized] : null;
        if (!resolution || resolution.kind === 'unmatched') { return; }
        var sessionExerciseId = makeSessionExerciseId(
          session.sourceSeed, session.sourceSheet, session.localRow, session.dateKey, blockIndex + 1
        );
        sessionExercises.push({
          id: sessionExerciseId,
          sessionId: sessionId,
          exerciseId: resolution.id,
          position: blockIndex + 1,
          createdAt: startedAt,
          updatedAt: startedAt
        });
        block.sets.forEach(function (set, setIndex) {
          var item = {
            id: makeSetId(set.sourceSeed, set.sourceSheet, set.localRow, session.dateKey),
            sessionExerciseId: sessionExerciseId,
            setIndex: setIndex + 1,
            reps: set.reps,
            weightGrams: unitToGrams(set.weight, set.unit),
            setType: set.role,
            createdAt: startedAt,
            updatedAt: startedAt
          };
          if (set.rpe !== null && set.rpe !== undefined) {
            item.rpeHalves = Math.round(set.rpe * 2);
          }
          if (set.notes !== null && set.notes !== undefined) {
            item.note = set.notes;
          }
          sets.push(item);
        });
      });
    });

    /* Key order is fixed: schemaVersion, exportedAt, then the four collections. */
    var file = {
      schemaVersion: SCHEMA_VERSION,
      exportedAt: exportedAt,
      exercises: exercises,
      sessions: sessions,
      sessionExercises: sessionExercises,
      sets: sets
    };

    var structural = validateExportFile(file);
    var emptyWarnings = emptySessions.map(function (session) {
      return {
        path: 'sessions',
        message: 'The session on ' + (session.date ? session.date.iso : 'an unknown date') +
          ' has no sets and was left out.'
      };
    });
    if (structural.problems.length > 0) {
      return {
        ok: false,
        file: null,
        json: '',
        problems: structural.problems,
        warnings: validation.warnings.concat(structural.warnings, emptyWarnings),
        stats: stats
      };
    }

    return {
      ok: true,
      file: file,
      json: serializeExport(file),
      problems: [],
      warnings: validation.warnings.concat(structural.warnings, emptyWarnings),
      stats: stats
    };
  }

  /* Checks the shape the app's decoder requires, independently of the model.
   * A problem is {path, message}, where path addresses the file. */
  function validateExportFile(file) {
    var problems = [];
    var warnings = [];

    function add(list, path, message) { list.push({ path: path, message: message }); }
    function checkId(record, path) {
      if (!isObject(record)) { return; }
      if (typeof record.id !== 'string' || record.id === '') {
        add(problems, path + '.id', 'A row needs an id.');
        return;
      }
      if (has(globalIds, record.id)) {
        add(problems, path + '.id', 'Duplicate id "' + record.id + '".');
      }
      globalIds[record.id] = true;
    }

    if (!isObject(file)) {
      add(problems, 'root', 'A backup must be a JSON object.');
      return { problems: problems, warnings: warnings };
    }
    if (file.schemaVersion !== SCHEMA_VERSION) {
      add(problems, 'schemaVersion', 'schemaVersion must be ' + SCHEMA_VERSION + '.');
    }
    if (typeof file.exportedAt !== 'number' || !isFinite(file.exportedAt)) {
      add(problems, 'exportedAt', 'exportedAt must be epoch millis.');
    }
    ['exercises', 'sessions', 'sessionExercises', 'sets'].forEach(function (key) {
      if (!Array.isArray(file[key])) {
        add(problems, key, 'The ' + key + ' collection must be a list.');
      }
    });

    var globalIds = {};
    var exercises = Array.isArray(file.exercises) ? file.exercises : [];
    var sessions = Array.isArray(file.sessions) ? file.sessions : [];
    var sessionExercises = Array.isArray(file.sessionExercises) ? file.sessionExercises : [];
    var sets = Array.isArray(file.sets) ? file.sets : [];

    var exerciseIds = {};
    exercises.forEach(function (exercise, index) {
      var path = 'exercises[' + index + ']';
      if (!isObject(exercise)) {
        add(problems, path, 'An exercise must be an object.');
        return;
      }
      checkId(exercise, path);
      if (typeof exercise.id === 'string') { exerciseIds[exercise.id] = true; }
      if (typeof exercise.name !== 'string' || exercise.name.trim() === '') {
        add(problems, path + '.name', 'An exercise needs a name.');
      }
      ['primaryMuscle', 'secondaryMuscles', 'equipment', 'movementPattern'].forEach(function (key) {
        if (!has(exercise, key)) {
          add(problems, path + '.' + key,
            'The required field ' + key + ' is missing (null is allowed).');
        }
      });
      if (has(exercise, 'secondaryMuscles') && !Array.isArray(exercise.secondaryMuscles)) {
        add(problems, path + '.secondaryMuscles', 'secondaryMuscles must be a list.');
      }
      if (typeof exercise.createdAt !== 'number') {
        add(problems, path + '.createdAt', 'createdAt must be epoch millis.');
      }
      if (typeof exercise.updatedAt !== 'number') {
        add(problems, path + '.updatedAt', 'updatedAt must be epoch millis.');
      }
      if (exercise.deletedAt !== null && exercise.deletedAt !== undefined) {
        add(problems, path + '.deletedAt',
          'deletedAt must be omitted or null; a value imports the row invisible.');
      }
    });

    var sessionIds = {};
    sessions.forEach(function (session, index) {
      var path = 'sessions[' + index + ']';
      if (!isObject(session)) {
        add(problems, path, 'A session must be an object.');
        return;
      }
      checkId(session, path);
      if (typeof session.id === 'string') { sessionIds[session.id] = true; }
      if (typeof session.startedAt !== 'number' || !isFinite(session.startedAt)) {
        add(problems, path + '.startedAt', 'startedAt must be epoch millis.');
      }
      if (typeof session.finishedAt !== 'number' || !isFinite(session.finishedAt)) {
        add(problems, path + '.finishedAt',
          'finishedAt is required: a session without it is read as a workout in progress.');
      }
      if (typeof session.createdAt !== 'number') {
        add(problems, path + '.createdAt', 'createdAt must be epoch millis.');
      }
      if (typeof session.updatedAt !== 'number') {
        add(problems, path + '.updatedAt', 'updatedAt must be epoch millis.');
      }
      if (session.deletedAt !== null && session.deletedAt !== undefined) {
        add(problems, path + '.deletedAt', 'deletedAt must be omitted or null.');
      }
    });

    var sessionExerciseIds = {};
    sessionExercises.forEach(function (row, index) {
      var path = 'sessionExercises[' + index + ']';
      if (!isObject(row)) {
        add(problems, path, 'A session exercise must be an object.');
        return;
      }
      checkId(row, path);
      if (typeof row.id === 'string') { sessionExerciseIds[row.id] = true; }
      if (!has(sessionIds, row.sessionId)) {
        add(problems, path + '.sessionId',
          'This reference does not resolve: "' + row.sessionId + '".');
      }
      if (!has(exerciseIds, row.exerciseId)) {
        add(problems, path + '.exerciseId',
          'This reference does not resolve: "' + row.exerciseId + '".');
      }
      if (!isWholeNumber(row.position) || row.position < 1) {
        add(problems, path + '.position', 'position must be a whole number, 1 or more.');
      }
      if (typeof row.createdAt !== 'number') {
        add(problems, path + '.createdAt', 'createdAt must be epoch millis.');
      }
      if (typeof row.updatedAt !== 'number') {
        add(problems, path + '.updatedAt', 'updatedAt must be epoch millis.');
      }
      if (row.deletedAt !== null && row.deletedAt !== undefined) {
        add(problems, path + '.deletedAt', 'deletedAt must be omitted or null.');
      }
    });

    sets.forEach(function (set, index) {
      var path = 'sets[' + index + ']';
      if (!isObject(set)) {
        add(problems, path, 'A set must be an object.');
        return;
      }
      checkId(set, path);
      if (!has(sessionExerciseIds, set.sessionExerciseId)) {
        add(problems, path + '.sessionExerciseId',
          'This reference does not resolve: "' + set.sessionExerciseId + '".');
      }
      if (!isWholeNumber(set.setIndex) || set.setIndex < 1) {
        add(problems, path + '.setIndex', 'setIndex must be a whole number, 1 or more.');
      }
      if (!isWholeNumber(set.reps) || set.reps < 1) {
        add(problems, path + '.reps', 'reps must be a whole number of at least 1.');
      }
      if (!isWholeNumber(set.weightGrams) || set.weightGrams < 0) {
        add(problems, path + '.weightGrams',
          'weightGrams must be a whole number of grams, 0 or more.');
      }
      if (EXPORTABLE_ROLES.indexOf(set.setType) === -1) {
        if (REFUSED_ROLES.indexOf(set.setType) !== -1) {
          add(problems, path + '.setType', '"' + set.setType + '" is a rung and cannot be imported.');
        } else {
          add(problems, path + '.setType',
            'setType must be one of ' + EXPORTABLE_ROLES.join(', ') + '.');
        }
      }
      if (set.rpeHalves !== null && set.rpeHalves !== undefined) {
        if (!isWholeNumber(set.rpeHalves) || set.rpeHalves < 2 || set.rpeHalves > 20) {
          add(problems, path + '.rpeHalves', 'rpeHalves must be between 2 and 20, or null.');
        }
      }
      if (set.note !== null && set.note !== undefined && typeof set.note !== 'string') {
        add(problems, path + '.note', 'note must be text, or null.');
      }
      if (typeof set.createdAt !== 'number') {
        add(problems, path + '.createdAt', 'createdAt must be epoch millis.');
      }
      if (typeof set.updatedAt !== 'number') {
        add(problems, path + '.updatedAt', 'updatedAt must be epoch millis.');
      }
      if (set.deletedAt !== null && set.deletedAt !== undefined) {
        add(problems, path + '.deletedAt', 'deletedAt must be omitted or null.');
      }
    });

    return { problems: problems, warnings: warnings };
  }

  function serializeExport(file) {
    return JSON.stringify(file, null, 2) + '\n';
  }

  function parseBackupText(text) {
    var doc;
    try {
      doc = JSON.parse(String(text === null || text === undefined ? '' : text));
    } catch (error) {
      return { ok: false, error: 'That file is not JSON.' };
    }
    if (!isObject(doc)) {
      return { ok: false, error: 'That file is not an app backup.' };
    }
    if (typeof doc.schemaVersion !== 'number') {
      return { ok: false, error: 'That file has no schemaVersion, so it is not an app backup.' };
    }
    if (!Array.isArray(doc.exercises)) {
      return { ok: false, error: 'That backup carries no exercise list.' };
    }
    return { ok: true, file: doc };
  }

  /* ------------------------------------------------------------------ *
   * UI
   *
   * The panels are built once per load. Validation only refreshes badges,
   * summaries, the problem list and the export preview, so typing in a cell is
   * never interrupted by its own input being replaced.
   * ------------------------------------------------------------------ */

  var SAMPLE_DOC = {
    source: '2019-notebook.xlsx / October',
    rows: [
      { date: '24-10-2025', exercise: 'Bench Press', reps: 10, weight: 40, unit: 'kg', type: 'W' },
      { date: null, exercise: null, reps: 8, weight: 60, rpe: 7, type: 'W' },
      { date: null, exercise: null, reps: 5, weight: 80, rpe: 8, type: 'N' },
      { date: null, exercise: 'Squat', reps: 10, weight: 50, type: 'W' },
      { date: null, exercise: null, reps: 5, weight: 100, rpe: 8 }
    ]
  };

  var ui = {
    state: null,
    elements: null,
    mappingRows: null
  };

  function byId(id) { return document.getElementById(id); }

  function h(tag, className, text) {
    var node = document.createElement(tag);
    if (className) { node.className = className; }
    if (text !== null && text !== undefined) { node.textContent = String(text); }
    return node;
  }

  function setText(node, text) {
    if (!node) { return; }
    var value = text === null || text === undefined ? '' : String(text);
    if (node.textContent !== value) { node.textContent = value; }
  }

  function makeButton(text) {
    var button = document.createElement('button');
    button.type = 'button';
    button.textContent = text;
    return button;
  }

  function labeledField(labelText, input) {
    var label = document.createElement('label');
    label.className = 'field';
    label.appendChild(h('span', 'field-label', labelText));
    label.appendChild(input);
    return label;
  }

  function announce(message) {
    var live = ui.elements ? ui.elements.liveStatus : null;
    if (!live) { return; }
    live.textContent = '';
    window.setTimeout(function () { live.textContent = String(message); }, 30);
  }

  function loadMapping() {
    var empty = { units: {}, targets: {} };
    try {
      var raw = window.localStorage.getItem(STORAGE_KEY);
      if (!raw) { return empty; }
      var parsed = JSON.parse(raw);
      if (!isObject(parsed)) { return empty; }
      var units = {};
      if (isObject(parsed.units)) {
        Object.keys(parsed.units).forEach(function (key) {
          if (parsed.units[key] === 'kg' || parsed.units[key] === 'lb') {
            units[key] = parsed.units[key];
          }
        });
      }
      var targets = {};
      if (isObject(parsed.targets)) {
        Object.keys(parsed.targets).forEach(function (key) {
          if (typeof parsed.targets[key] === 'string') {
            targets[key] = parsed.targets[key];
          }
        });
      }
      return { units: units, targets: targets };
    } catch (error) {
      return empty;
    }
  }

  function saveMapping() {
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(ui.state.mapping));
    } catch (error) { /* private mode or an opaque origin: mapping is optional */ }
  }

  function forgetSavedMapping() {
    try { window.localStorage.removeItem(STORAGE_KEY); } catch (error) { /* ignore */ }
  }

  function init() {
    if (typeof document === 'undefined') { return; }
    if (!document.getElementById('importHelper')) { return; }
    if (ui.state) { return; }

    var state = {
      rows: [],
      /* The loaded lists, each with its own seed and sheet (N161). */
      sources: [],
      /* ISO dates whose sessions from different lists are joined into one. */
      combineDates: [],
      inputProblems: [],
      inputWarnings: [],
      source: '',
      fileName: '',
      seed: '',
      sheet: '',
      dateOrder: 'dmy',
      model: null,
      catalogFile: null,
      mapping: loadMapping(),
      validation: { problems: [], warnings: [] },
      problemIndex: { bySession: {}, global: [] },
      warningIndex: { bySession: {}, global: [] },
      dom: null,
      onlyProblems: false,
      /* The progress panel's chosen movement, and nothing else about it (N165). */
      progressExerciseId: null,
      exportedAt: Date.now(),
      exportResult: null,
      lastCounts: ''
    };
    ui.state = state;
    ui.mappingRows = {};

    var elements = {
      liveStatus: byId('liveStatus'),
      dropZone: byId('dropZone'),
      fileInput: byId('fileInput'),
      pasteBox: byId('pasteBox'),
      loadPaste: byId('loadPaste'),
      loadSample: byId('loadSample'),
      exportInput: byId('exportInput'),
      sourceList: byId('sourceList'),
      combineStatus: byId('combineStatus'),
      orderDmy: byId('orderDmy'),
      orderMdy: byId('orderMdy'),
      ambiguityNote: byId('ambiguityNote'),
      catalogStatus: byId('catalogStatus'),
      mappingBody: byId('mappingBody'),
      forgetMapping: byId('forgetMapping'),
      promptText: byId('promptText'),
      copyPrompt: byId('copyPrompt'),
      sessionsList: byId('sessionsList'),
      onlyProblems: byId('onlyProblems'),
      problemsCounts: byId('problemsCounts'),
      problemList: byId('problemList'),
      exportStats: byId('exportStats'),
      exportRefusal: byId('exportRefusal'),
      downloadExport: byId('downloadExport'),
      copyExport: byId('copyExport'),
      exportText: byId('exportText'),
      progressPanel: byId('panel-progress'),
      progressLift: byId('progressLift'),
      progressRange: byId('progressRange'),
      progressSummary: byId('progressSummary'),
      progressChart: byId('progressChart'),
      progressLifts: byId('progressLifts'),
      exerciseNames: byId('exerciseNames'),
      targetChoices: byId('targetChoices'),
      sourceStatus: byId('sourceStatus')
    };
    ui.elements = elements;

    elements.promptText.textContent = MODEL_PROMPT;
    var seeded = seededCatalog();
    if (seeded) {
      state.catalogFile = seeded;
      var seededCount = seeded.exercises.filter(function (row) {
        return isObject(row) && isLoggableRow(row) &&
          (row.deletedAt === null || row.deletedAt === undefined);
      }).length;
      elements.catalogStatus.textContent =
        'The app\'s seeded library is loaded (' + seededCount +
        ' movements), so a name links to its permanent slug. Load your export to add your own ' +
        'exercises, the dry-run comparison and the id check.';
    } else {
      elements.catalogStatus.textContent =
        'No exercise library loaded: seed-catalog.js is missing, so every name becomes a new exercise.';
    }
    elements.sourceStatus.textContent = 'Drop a file, choose one, or paste rows, then press "Load pasted rows". The example is the one from the specification.';

    elements.loadPaste.addEventListener('click', function () {
      loadText(elements.pasteBox.value, '');
    });
    elements.loadSample.addEventListener('click', function () {
      elements.pasteBox.value = JSON.stringify(SAMPLE_DOC, null, 2);
      loadText(elements.pasteBox.value, '');
    });
    elements.fileInput.addEventListener('change', function () {
      readFiles(elements.fileInput.files);
      elements.fileInput.value = '';
    });
    elements.exportInput.addEventListener('change', function () {
      if (elements.exportInput.files && elements.exportInput.files.length > 0) {
        readFile(elements.exportInput.files[0], 'export');
      }
      elements.exportInput.value = '';
    });
    elements.progressLift.addEventListener('change', function () {
      var typed = elements.progressLift.value.trim();
      state.progressExerciseId = null;
      renderProgress();
      if (typed !== '' &&
          normalizeName(elements.progressLift.value) !== normalizeName(typed)) {
        announce('"' + typed + '" has no logged sets in this export.');
      }
    });
    elements.progressRange.addEventListener('change', renderProgress);
    elements.orderDmy.addEventListener('change', onOrderChange);
    elements.orderMdy.addEventListener('change', onOrderChange);
    elements.onlyProblems.addEventListener('change', function () {
      state.onlyProblems = !!elements.onlyProblems.checked;
      refreshSessions();
    });
    elements.forgetMapping.addEventListener('click', function () {
      state.mapping = { units: {}, targets: {} };
      forgetSavedMapping();
      if (state.rows.length > 0) {
        rebuildModel();
        renderAll();
      } else {
        renderMapping();
        refresh();
      }
      announce('Forgot the saved exercise mapping.');
    });
    elements.copyPrompt.addEventListener('click', function () {
      copyText(MODEL_PROMPT, function (ok) { copied(ok, elements.promptText); });
    });
    elements.downloadExport.addEventListener('click', downloadExport);
    elements.copyExport.addEventListener('click', function () {
      if (state.exportResult && state.exportResult.ok) {
        copyText(state.exportResult.json, function (ok) { copied(ok, elements.exportText); });
      }
    });

    ['dragenter', 'dragover'].forEach(function (type) {
      elements.dropZone.addEventListener(type, function (event) {
        event.preventDefault();
        elements.dropZone.classList.add('is-over');
      });
    });
    ['dragleave', 'drop'].forEach(function (type) {
      elements.dropZone.addEventListener(type, function (event) {
        event.preventDefault();
        elements.dropZone.classList.remove('is-over');
      });
    });
    elements.dropZone.addEventListener('drop', function (event) {
      var transfer = event.dataTransfer;
      if (transfer && transfer.files && transfer.files.length > 0) {
        readFiles(transfer.files);
        return;
      }
      var text = transfer ? transfer.getData('text/plain') : '';
      if (text) {
        elements.pasteBox.value = text;
        loadText(text, '');
      }
    });

    renderAll();
  }

  function readFile(file, purpose) {
    var reader = new FileReader();
    reader.onload = function () {
      var text = typeof reader.result === 'string' ? reader.result : '';
      if (purpose === 'export') {
        loadExportText(text, file.name);
      } else {
        loadText(text, file.name);
      }
    };
    reader.onerror = function () { announce('That file could not be read.'); };
    reader.readAsText(file, 'UTF-8');
  }

  /* Reads several files in order, so the sources list keeps the order they were
   * chosen in and every append is visible (N161). */
  function readFiles(files) {
    if (!files || files.length === 0) { return; }
    var list = Array.prototype.slice.call(files);
    var index = 0;
    function next() {
      if (index >= list.length) { return; }
      var file = list[index];
      index += 1;
      var reader = new FileReader();
      reader.onload = function () {
        var text = typeof reader.result === 'string' ? reader.result : '';
        loadText(text, file.name);
        next();
      };
      reader.onerror = function () {
        announce('Could not read ' + file.name + '.');
        next();
      };
      reader.readAsText(file, 'UTF-8');
    }
    next();
  }

  function catalogStatusText(file, label) {
    var count = Array.isArray(file.exercises) ? file.exercises.length : 0;
    var text = 'Loaded ' + (label ? label + ': ' : '') + count + ' exercise row' + (count === 1 ? '' : 's');
    if (typeof file.schemaVersion === 'number') {
      text += ' (schemaVersion ' + file.schemaVersion + ')';
      if (file.schemaVersion !== SCHEMA_VERSION) {
        text += '. This export is schemaVersion ' + file.schemaVersion + ', not ' +
          SCHEMA_VERSION + '; its exercise rows are copied as they are and may carry values the ' +
          'current app cannot read. Re-export from the current app if you can.';
      } else {
        text += '.';
      }
    } else {
      text += '.';
    }
    return text;
  }

  function applyCatalog(file) {
    var state = ui.state;
    state.catalogFile = file;
    if (state.model) {
      state.model.catalog = normalizeCatalog(file);
      state.model.catalogFile = file;
    }
    renderDatalist();
    renderMapping();
    refresh();
    // The progress panel is built from this file, so loading one must show it at once.
    renderProgress();
  }

  function loadExportText(text, fileName) {
    var parsed = parseBackupText(text);
    if (!parsed.ok) {
      ui.elements.catalogStatus.textContent = parsed.error;
      announce(parsed.error);
      return;
    }
    applyCatalog(parsed.file);
    ui.elements.catalogStatus.textContent = catalogStatusText(parsed.file, fileName || 'app export');
    announce('Loaded the exercise library from ' + (fileName || 'the export') + '.');
  }

  function loadText(text, fileName) {
    var state = ui.state;
    var parsed = parseInputText(text, { fileName: fileName || '' });

    if (parsed.kind === 'backup') {
      applyCatalog(parsed.backup);
      ui.elements.catalogStatus.textContent = catalogStatusText(
        parsed.backup, fileName || 'pasted backup'
      );
      ui.elements.sourceStatus.textContent =
        'That is an app backup (schemaVersion ' + parsed.backup.schemaVersion +
        '), not a row list. Its exercise library is loaded; drop the rows separately.';
      announce('That is an app backup, not a row list. It was loaded as the exercise library.');
      return;
    }

    if (parsed.kind === 'error') {
      ui.elements.sourceStatus.textContent = parsed.problems && parsed.problems.length
        ? parsed.problems[0].message
        : 'That input could not be read.';
      announce(parsed.problems && parsed.problems.length
        ? parsed.problems[0].message
        : 'That input could not be read.');
      return;
    }

    var rows = parsed.rows || [];
    if (rows.length === 0) {
      ui.elements.sourceStatus.textContent = 'That input has no rows.';
      announce('That input has no rows.');
      return;
    }

    /* A list is loaded once: the same rows twice would double every session, and
     * the ids would not even collide to hide it (N161). */
    var signature = JSON.stringify(rows);
    for (var i = 0; i < state.sources.length; i++) {
      if (JSON.stringify(state.sources[i].rows) === signature) {
        ui.elements.sourceStatus.textContent =
          'That list is already loaded. Remove it first to replace it.';
        announce('That list is already loaded.');
        return;
      }
    }

    var name = parsed.source || fileName || ('Pasted rows ' + (state.sources.length + 1));
    state.sources.push({
      name: name,
      seed: name,
      sheet: name,
      rows: rows,
      problems: parsed.problems || [],
      warnings: parsed.warnings || []
    });
    state.exportedAt = Date.now();
    reconcileSources();
    rebuildModel();
    renderAll();
    ui.elements.sourceStatus.textContent =
      'Read ' + rows.length + ' row' + (rows.length === 1 ? '' : 's') + ' from ' + name +
      '. Each list starts its own first session; combine a date below when two lists share one.';
    announce('Loaded ' + rows.length + ' row' + (rows.length === 1 ? '' : 's') + ' from ' + name + '.');
  }

  /* Rebuilds the flat row list from the loaded lists, annotating each row with
   * the list it came from so a session's id names its file (N161, B173). */
  function reconcileSources() {
    var state = ui.state;
    var rows = [];
    var problems = [];
    var warnings = [];
    state.sources.forEach(function (source, index) {
      source.rows.forEach(function (row, rowIndex) {
        if (isObject(row)) {
          row.__source = index;
          row.__row = rowIndex + 1;
        }
        rows.push(row);
      });
      problems = problems.concat(source.problems || []);
      warnings = warnings.concat(source.warnings || []);
    });
    state.rows = rows;
    state.inputProblems = problems;
    state.inputWarnings = warnings;
    var first = state.sources[0];
    state.seed = first ? first.seed : 'rows';
    state.sheet = first ? first.sheet : state.seed;
    state.source = first ? first.name : 'pasted-rows';
    state.fileName = first ? first.name : '';

    /* A combine is only meaningful while the date is still in two lists. */
    var countByDate = {};
    state.sources.forEach(function (source) {
      var seen = {};
      source.rows.forEach(function (row) {
        if (!isObject(row) || !isPresent(row.date)) { return; }
        var parsed = parseDateValue(row.date, state.dateOrder);
        if (parsed.ok) { seen[parsed.iso] = true; }
      });
      Object.keys(seen).forEach(function (iso) {
        countByDate[iso] = (countByDate[iso] || 0) + 1;
      });
    });
    state.combineDates = state.combineDates.filter(function (iso) {
      return (countByDate[iso] || 0) > 1;
    });
  }

  function removeSource(index) {
    var state = ui.state;
    var source = state.sources[index];
    if (!source) { return; }
    state.sources.splice(index, 1);
    reconcileSources();
    rebuildModel();
    renderAll();
    announce('Removed ' + source.name + '.');
  }

  function rebuildModel() {
    var state = ui.state;
    if (!state.rows || state.rows.length === 0) {
      state.model = null;
      return;
    }
    state.model = buildModel(state.rows, {
      seed: state.seed,
      sheet: state.sheet,
      source: state.source,
      sources: state.sources.map(function (item) {
        return { seed: item.seed, sheet: item.sheet, name: item.name };
      }),
      combineDates: state.combineDates,
      dateOrder: state.dateOrder,
      catalog: state.catalogFile,
      nameUnits: state.mapping.units,
      nameTargets: state.mapping.targets
    });
  }

  function renderAll() {
    renderSources();
    renderDatalist();
    renderSessions();
    renderMapping();
    renderCombine();
    renderProgress();
    refresh();
    refreshOrderNote();
  }

  /* The loaded lists: one row each, with its own revision and a way to remove it
   * (N161). The revision keys that list's ids, so changing it re-keys only them. */
  function renderSources() {
    var state = ui.state;
    var host = ui.elements.sourceList;
    if (!host) { return; }
    host.textContent = '';
    if (state.sources.length === 0) {
      host.appendChild(h('p', 'hint', 'No lists loaded yet.'));
      return;
    }
    state.sources.forEach(function (source, index) {
      var row = h('div', 'source-row');

      var name = h('span', 'source-name', source.name);
      row.appendChild(name);
      row.appendChild(h('span', 'source-count',
        source.rows.length + (source.rows.length === 1 ? ' row' : ' rows')));

      var input = document.createElement('input');
      input.type = 'text';
      input.className = 'source-seed';
      input.value = source.seed;
      input.autocomplete = 'off';
      input.spellcheck = false;
      input.setAttribute('aria-label', 'Revision for ' + source.name);
      input.addEventListener('change', function () {
        var value = input.value.trim();
        if (value === '') {
          input.value = source.seed;
          return;
        }
        source.seed = value;
        rebuildModel();
        renderAll();
        announce('The revision for ' + source.name + ' is now ' + value + '.');
      });
      row.appendChild(labeledField('Revision', input));

      var remove = document.createElement('button');
      remove.type = 'button';
      remove.className = 'secondary source-remove';
      remove.textContent = 'Remove';
      remove.setAttribute('aria-label', 'Remove ' + source.name);
      remove.addEventListener('click', function () { removeSource(index); });
      row.appendChild(remove);

      host.appendChild(row);
    });
  }

  /* Dates two lists both trained, with a way to join them into one workout (N161). */
  function renderCombine() {
    var state = ui.state;
    var host = ui.elements.combineStatus;
    if (!host) { return; }
    host.textContent = '';
    if (state.sources.length < 2) { return; }

    var countByDate = {};
    state.sources.forEach(function (source) {
      var seen = {};
      source.rows.forEach(function (row) {
        if (!isObject(row) || !isPresent(row.date)) { return; }
        var parsed = parseDateValue(row.date, state.dateOrder);
        if (parsed.ok) { seen[parsed.iso] = true; }
      });
      Object.keys(seen).forEach(function (iso) {
        countByDate[iso] = (countByDate[iso] || 0) + 1;
      });
    });

    Object.keys(countByDate).sort().forEach(function (iso) {
      if (countByDate[iso] < 2) { return; }
      var combined = state.combineDates.indexOf(iso) !== -1;
      var line = h('p', 'combine-line');
      var button = document.createElement('button');
      button.type = 'button';
      button.className = 'secondary';
      button.textContent = (combined ? 'Uncombine ' : 'Combine ') + iso;
      button.addEventListener('click', function () {
        state.combineDates = combined
          ? state.combineDates.filter(function (date) { return date !== iso; })
          : state.combineDates.concat([iso]);
        rebuildModel();
        renderAll();
        announce((combined ? 'Uncombined ' : 'Combined ') + iso + '.');
      });
      line.appendChild(button);
      line.appendChild(document.createTextNode(
        '  ' + countByDate[iso] + ' lists trained on this date; joined, they become one workout.'
      ));
      host.appendChild(line);
    });
  }

  /* The library's loggable names, deduped and sorted, for every chooser on the page. */
  function libraryNames(catalogFile) {
    var names = [];
    var seen = {};
    if (!catalogFile || !Array.isArray(catalogFile.exercises)) { return names; }
    catalogFile.exercises.forEach(function (exercise) {
      if (!isObject(exercise) || exercise.deletedAt !== null && exercise.deletedAt !== undefined) { return; }
      // A category is a library row, never a loggable target (N95).
      if (!isLoggableRow(exercise)) { return; }
      if (typeof exercise.name !== 'string' || exercise.name.trim() === '') { return; }
      var key = normalizeName(exercise.name);
      if (has(seen, key)) { return; }
      seen[key] = true;
      names.push(exercise.name.trim());
    });
    names.sort(compareExerciseNames);
    return names;
  }

  function renderDatalist() {
    var state = ui.state;
    var names = libraryNames(state.catalogFile);

    // The source-name input suggests the library's own spellings.
    var list = ui.elements.exerciseNames;
    list.textContent = '';
    names.forEach(function (name) {
      var option = document.createElement('option');
      option.value = name;
      list.appendChild(option);
    });

    // The mapping's target field is a searchable chooser: typing filters this list,
    // and "Create new exercise" is the explicit way to keep the source name.
    var choices = ui.elements.targetChoices;
    if (!choices) { return; }
    choices.textContent = '';
    var create = document.createElement('option');
    create.value = CREATE_LABEL;
    choices.appendChild(create);
    names.forEach(function (name) {
      var option = document.createElement('option');
      option.value = name;
      choices.appendChild(option);
    });
  }

  function collectNames(model) {
    var byName = {};
    var order = [];
    model.sessions.forEach(function (session) {
      session.blocks.forEach(function (block) {
        var normalized = normalizeName(block.name);
        if (!normalized) { return; }
        if (!has(byName, normalized)) {
          byName[normalized] = {
            normalized: normalized,
            displayName: String(block.name).trim(),
            unit: block.unit,
            blocks: 0,
            sets: 0
          };
          order.push(normalized);
        }
        byName[normalized].blocks += 1;
        byName[normalized].sets += block.sets.length;
      });
    });
    return order.map(function (normalized) { return byName[normalized]; });
  }

  function renderMapping() {
    var state = ui.state;
    var body = ui.elements.mappingBody;
    body.textContent = '';
    ui.mappingRows = {};
    if (!state.model || state.model.sessions.length === 0) {
      var emptyRow = document.createElement('tr');
      var emptyCell = h('td', 'mapping-empty', 'No exercises yet. Load rows to see the mapping.');
      emptyCell.colSpan = 5;
      emptyRow.appendChild(emptyCell);
      body.appendChild(emptyRow);
      return;
    }

    collectNames(state.model).forEach(function (entry) {
      var row = document.createElement('tr');

      row.appendChild(h('td', 'mapping-name', entry.displayName));
      row.appendChild(h('td', 'mapping-count',
        entry.blocks + ' block' + (entry.blocks === 1 ? '' : 's') + ', ' +
        entry.sets + ' set' + (entry.sets === 1 ? '' : 's')));

      var targetCell = document.createElement('td');
      var targetInput = document.createElement('input');
      targetInput.type = 'text';
      targetInput.className = 'mapping-target';
      targetInput.setAttribute('list', 'targetChoices');
      targetInput.setAttribute('autocomplete', 'off');
      targetInput.setAttribute('spellcheck', 'false');
      targetInput.setAttribute('aria-label', 'Search the library for ' + entry.displayName);
      fillTargetInput(targetInput, entry);
      targetCell.appendChild(targetInput);
      row.appendChild(targetCell);

      var unitCell = document.createElement('td');
      var unitSelect = document.createElement('select');
      unitSelect.setAttribute('aria-label', 'Default unit for ' + entry.displayName);
      ['kg', 'lb'].forEach(function (unit) {
        var option = document.createElement('option');
        option.value = unit;
        option.textContent = unit;
        unitSelect.appendChild(option);
      });
      unitSelect.value = has(state.model.nameUnits, entry.normalized)
        ? state.model.nameUnits[entry.normalized]
        : (entry.unit === 'lb' ? 'lb' : 'kg');
      unitCell.appendChild(unitSelect);
      row.appendChild(unitCell);

      var note = h('td', 'mapping-note', resolutionNoteForName(entry.normalized, entry.displayName));
      row.appendChild(note);

      targetInput.addEventListener('change', function () {
        var text = targetInput.value.trim();
        var catalog = state.model ? state.model.catalog : null;
        var next;
        if (text === '') {
          next = '';
        } else if (normalizeName(text) === normalizeName(CREATE_LABEL)) {
          next = 'create';
        } else if (catalog && has(catalog.byName, normalizeName(text))) {
          next = catalog.byName[normalizeName(text)].id;
        } else {
          // A typed name the library does not have is not silently turned into one:
          // put the previous choice back and say what the two ways out are.
          targetInput.value = fillTargetInput(targetInput, entry);
          announce('"' + text + '" is not in the library. Pick one from the list, or "' +
            CREATE_LABEL + '".');
          return;
        }
        state.mapping.targets[entry.normalized] = next;
        saveMapping();
        setText(note, resolutionNoteForName(entry.normalized, entry.displayName));
        refresh();
      });
      unitSelect.addEventListener('change', function () {
        applyMappingUnit(entry.normalized, unitSelect.value);
        setText(note, resolutionNoteForName(entry.normalized, entry.displayName));
      });

      ui.mappingRows[entry.normalized] = {
        displayName: entry.displayName,
        targetInput: targetInput,
        unitSelect: unitSelect,
        note: note
      };
      body.appendChild(row);
    });
  }

  /* Shows the current choice as a library name, and returns what it displayed. */
  function fillTargetInput(input, entry) {
    var state = ui.state;
    var catalog = state.model ? state.model.catalog : null;
    var target = has(state.mapping.targets, entry.normalized)
      ? state.mapping.targets[entry.normalized]
      : null;
    var desired = '';
    if (target === 'create') {
      desired = CREATE_LABEL;
    } else if (target && catalog && has(catalog.byId, target)) {
      desired = String(catalog.byId[target].name);
    } else if (catalog && has(catalog.byName, entry.normalized)) {
      desired = String(catalog.byName[entry.normalized].name);
    } else if (catalog && has(catalog.deletedByName, entry.normalized)) {
      desired = CREATE_LABEL;
    }
    input.value = desired;
    return desired;
  }

  function resolutionNoteForName(normalized, displayName) {
    var state = ui.state;
    if (!state.model) { return ''; }
    var display = displayName || (ui.mappingRows && ui.mappingRows[normalized]
      ? ui.mappingRows[normalized].displayName
      : normalized);
    var resolution = resolveExercise(display, state.model);
    var note;
    if (resolution.kind === 'library') {
      note = 'Library: ' + resolution.name;
      if (resolution.dto && resolution.dto.weightUnit) {
        note += ' (' + resolution.dto.weightUnit + ')';
      }
    } else if (resolution.kind === 'new') {
      note = 'New exercise (' + resolution.weightUnit + ')';
      if (resolution.deletedOnly) {
        note += '; the matching library row is deleted';
      } else if (!state.model.catalog || !state.model.catalog.loaded) {
        // Every name reads "new" only when no library loaded at all; say why, in the row it affects.
        note += ' — no exercise library loaded, so nothing can be linked. Load an app export above.';
      }
    } else {
      note = 'Unmapped: choose a library exercise or create a new one.';
    }
    var target = has(state.mapping.targets, normalized) ? state.mapping.targets[normalized] : null;
    if (target && target !== 'create' && state.model.catalog &&
        state.model.catalog.loaded && !has(state.model.catalog.byId, target)) {
      note += ' The saved mapping is not in this export.';
    }
    return note;
  }

  function applyMappingUnit(normalized, unit) {
    var state = ui.state;
    var value = unit === 'lb' ? 'lb' : 'kg';
    state.mapping.units[normalized] = value;
    saveMapping();
    if (!state.model) { refresh(); return; }
    state.model.nameUnits[normalized] = value;
    state.model.sessions.forEach(function (session, sessionIndex) {
      session.blocks.forEach(function (block, blockIndex) {
        if (normalizeName(block.name) !== normalized || block.manualUnit) { return; }
        block.unit = value;
        block.sets.forEach(function (set) {
          set.unit = value;
          var row = state.rows[set.sourceRow - 1];
          if (row) { row.unit = value; }
        });
        var domSession = state.dom && state.dom.sessions[sessionIndex];
        var domBlock = domSession && domSession.blocks[blockIndex];
        if (domBlock) {
          domBlock.unitSelect.value = value;
          domBlock.sets.forEach(function (domSet) { domSet.unitSelect.value = value; });
        }
      });
    });
    refresh();
  }

  function renderSessions() {
    var state = ui.state;
    var dateFormat = state.dateOrder === 'mdy' ? 'MM-DD-YYYY' : 'DD-MM-YYYY';
    var container = ui.elements.sessionsList;
    var previousOpen = [];
    if (state.dom && state.dom.sessions) {
      state.dom.sessions.forEach(function (entry) {
        previousOpen.push(!!entry.details.open);
      });
    }
    container.textContent = '';
    state.dom = { sessions: [] };

    if (!state.model) {
      container.appendChild(h('p', 'empty',
        'No rows loaded yet. Load JSON, CSV or TSV above, or press "Load the example".'));
      return;
    }

    state.model.sessions.forEach(function (session, sessionIndex) {
      var details = document.createElement('details');
      details.className = 'session';
      details.open = previousOpen.length > sessionIndex
        ? previousOpen[sessionIndex]
        : sessionIndex === 0;

      var summary = document.createElement('summary');
      var dateSpan = h('span', 'session-date', '');
      var metaSpan = h('span', 'session-meta', '');
      var badge = h('span', 'badge badge-ok', 'No problems');
      summary.appendChild(dateSpan);
      summary.appendChild(metaSpan);
      summary.appendChild(badge);
      details.appendChild(summary);

      var body = h('div', 'session-body');
      var header = h('div', 'session-header');

      var dateInput = document.createElement('input');
      dateInput.type = 'text';
      dateInput.className = 'session-date-input';
      dateInput.placeholder = dateFormat;
      dateInput.value = session.dateRaw || '';
      dateInput.setAttribute('autocomplete', 'off');
      dateInput.setAttribute('inputmode', 'numeric');
      dateInput.setAttribute('aria-label',
        'Date for the session starting at source row ' + session.sourceRow + ' (' + dateFormat + ')');
      dateInput.addEventListener('change', function () {
        updateSessionDate(sessionIndex, dateInput.value);
      });
      header.appendChild(labeledField('Date', dateInput));

      var timeInput = document.createElement('input');
      timeInput.type = 'text';
      timeInput.className = 'session-time-input';
      timeInput.placeholder = '12:00';
      timeInput.value = formatTime(session.hh, session.mm);
      timeInput.setAttribute('autocomplete', 'off');
      timeInput.setAttribute('aria-label',
        'Start time for the session at source row ' + session.sourceRow + ' (HH:MM)');
      timeInput.addEventListener('change', function () {
        updateSessionTime(sessionIndex, timeInput.value);
      });
      header.appendChild(labeledField('Time', timeInput));

      var durationInput = document.createElement('input');
      durationInput.type = 'number';
      durationInput.min = '0';
      durationInput.step = '5';
      durationInput.className = 'session-duration-input';
      durationInput.value = String(session.durationMinutes);
      durationInput.setAttribute('aria-label',
        'Duration in minutes for the session at source row ' + session.sourceRow);
      durationInput.addEventListener('input', function () {
        updateSessionDuration(sessionIndex, durationInput.value);
      });
      header.appendChild(labeledField('Duration (min)', durationInput));

      // The offset is the lifter's when the sheet states one, and the browser's
      // zone otherwise; either can be corrected here (N163).
      var offsetInput = document.createElement('input');
      offsetInput.type = 'number';
      offsetInput.step = '15';
      offsetInput.min = '-840';
      offsetInput.max = '840';
      offsetInput.className = 'session-offset-input';
      offsetInput.value = session.zoneOffsetMinutes === null ? '' : String(session.zoneOffsetMinutes);
      offsetInput.setAttribute('aria-label',
        'UTC offset in minutes for the session at source row ' + session.sourceRow +
        ' (empty to derive it from this browser)');
      offsetInput.addEventListener('change', function () {
        updateSessionOffset(sessionIndex, offsetInput.value);
      });
      header.appendChild(labeledField('UTC offset (min)', offsetInput));

      var offsetText = h('p', 'session-offset', '');
      header.appendChild(offsetText);
      body.appendChild(header);

      var blockEntries = [];
      session.blocks.forEach(function (block, blockIndex) {
        var blockEl = h('section', 'block');
        var head = h('div', 'block-head');

        var nameInput = document.createElement('input');
        nameInput.type = 'text';
        nameInput.className = 'exercise-name';
        nameInput.value = block.name;
        nameInput.placeholder = 'Exercise name';
        nameInput.setAttribute('list', 'exerciseNames');
        nameInput.setAttribute('autocomplete', 'off');
        nameInput.setAttribute('aria-label',
          'Exercise name, session ' + (sessionIndex + 1) + ', block ' + (blockIndex + 1) +
          ', source row ' + block.sourceRow);
        nameInput.addEventListener('input', function () {
          updateBlockName(sessionIndex, blockIndex, nameInput.value);
        });
        nameInput.addEventListener('change', function () {
          renderMapping();
        });
        head.appendChild(labeledField('Exercise', nameInput));

        var unitSelect = document.createElement('select');
        unitSelect.setAttribute('aria-label',
          'Unit for every set of ' + (block.name || 'this exercise'));
        ['kg', 'lb'].forEach(function (unit) {
          var option = document.createElement('option');
          option.value = unit;
          option.textContent = unit;
          unitSelect.appendChild(option);
        });
        unitSelect.value = block.unit;
        unitSelect.addEventListener('change', function () {
          updateBlockUnit(sessionIndex, blockIndex, unitSelect.value);
        });
        head.appendChild(labeledField('Unit for this block', unitSelect));

        var notes = h('div', 'block-notes');
        var note = h('span', 'block-note', '');
        var blockBadge = h('span', 'badge badge-ok', 'No problems');
        notes.appendChild(note);
        notes.appendChild(blockBadge);
        head.appendChild(notes);

        var bulk = h('div', 'block-bulk');
        var firstInput = document.createElement('input');
        firstInput.type = 'number';
        firstInput.min = '1';
        firstInput.step = '1';
        firstInput.className = 'first-warmup-input';
        firstInput.value = warmupCount(block) > 0 ? String(warmupCount(block)) : '';
        firstInput.placeholder = 'N';
        firstInput.setAttribute('aria-label',
          'Number of leading sets to mark as warm-up, ' + (block.name || 'this exercise'));
        var firstLabel = h('label', 'inline-field');
        firstLabel.appendChild(h('span', 'field-label', 'First'));
        firstLabel.appendChild(firstInput);
        firstLabel.appendChild(h('span', 'field-label', 'rows are warm-up'));
        bulk.appendChild(firstLabel);

        var applyFirst = makeButton('Mark first N');
        applyFirst.addEventListener('click', function () {
          var count = Number(firstInput.value);
          if (!isWholeNumber(count) || count < 1) {
            announce('Enter a whole number of rows to mark as warm-up.');
            return;
          }
          applyFirstWarmups(sessionIndex, blockIndex, count);
        });
        bulk.appendChild(applyFirst);

        var markWarm = makeButton('Mark selected warm-up');
        markWarm.addEventListener('click', function () {
          applySelectedWarmups(sessionIndex, blockIndex, 'WARMUP');
        });
        bulk.appendChild(markWarm);

        var markWork = makeButton('Mark selected working');
        markWork.addEventListener('click', function () {
          applySelectedWarmups(sessionIndex, blockIndex, 'NORMAL');
        });
        bulk.appendChild(markWork);
        bulk.appendChild(h('span', 'bulk-hint', 'Shift-click a checkbox to select a range.'));
        head.appendChild(bulk);
        blockEl.appendChild(head);

        var table = document.createElement('table');
        table.className = 'sets';
        table.appendChild(h('caption', 'visually-hidden',
          'Sets for ' + (block.name || 'the unnamed exercise')));
        var thead = document.createElement('thead');
        var headRow = document.createElement('tr');
        ['Select', '#', 'Reps', 'Weight and unit', 'RPE', 'Type', 'Problems'].forEach(function (label) {
          var th = document.createElement('th');
          th.scope = 'col';
          th.textContent = label;
          headRow.appendChild(th);
        });
        thead.appendChild(headRow);
        table.appendChild(thead);
        var tbody = document.createElement('tbody');

        var setEntries = [];
        block.sets.forEach(function (set, setIndex) {
          var tr = document.createElement('tr');

          var checkbox = document.createElement('input');
          checkbox.type = 'checkbox';
          checkbox.className = 'row-check';
          checkbox.setAttribute('aria-label',
            'Select set ' + set.index + ' of ' + (block.name || 'the unnamed exercise') +
            ' for bulk marking');
          var checkLabel = h('label', 'check-target');
          checkLabel.appendChild(checkbox);
          checkLabel.appendChild(h('span', 'visually-hidden', 'Select'));
          var checkCell = h('td', 'cell-check');
          checkCell.appendChild(checkLabel);
          tr.appendChild(checkCell);

          tr.appendChild(h('td', 'cell-index', String(set.index)));

          var repsInput = document.createElement('input');
          repsInput.type = 'number';
          repsInput.min = '1';
          repsInput.step = '1';
          repsInput.className = 'reps-input';
          repsInput.value = set.reps === null || set.reps === undefined ? '' : String(set.reps);
          repsInput.setAttribute('inputmode', 'numeric');
          repsInput.setAttribute('aria-label',
            'Reps, ' + (block.name || 'unnamed exercise') + ', set ' + set.index +
            ', source row ' + set.sourceRow);
          repsInput.addEventListener('input', function () {
            updateSetNumber(sessionIndex, blockIndex, setIndex, 'reps', repsInput.value);
          });
          var repsCell = h('td');
          repsCell.appendChild(repsInput);
          tr.appendChild(repsCell);

          var weightInput = document.createElement('input');
          weightInput.type = 'number';
          weightInput.min = '0';
          weightInput.step = '0.5';
          weightInput.className = 'weight-input';
          weightInput.value = set.weight === null || set.weight === undefined ? '' : String(set.weight);
          weightInput.setAttribute('inputmode', 'decimal');
          weightInput.setAttribute('aria-label',
            'Weight, ' + (block.name || 'unnamed exercise') + ', set ' + set.index +
            ', source row ' + set.sourceRow);
          weightInput.addEventListener('input', function () {
            updateSetNumber(sessionIndex, blockIndex, setIndex, 'weight', weightInput.value);
          });

          var rowUnitSelect = document.createElement('select');
          rowUnitSelect.className = 'row-unit';
          rowUnitSelect.setAttribute('aria-label',
            'Unit for this set, ' + (block.name || 'unnamed exercise') + ', set ' + set.index);
          ['kg', 'lb'].forEach(function (unit) {
            var option = document.createElement('option');
            option.value = unit;
            option.textContent = unit;
            rowUnitSelect.appendChild(option);
          });
          rowUnitSelect.value = set.unit;
          rowUnitSelect.addEventListener('change', function () {
            updateSetUnit(sessionIndex, blockIndex, setIndex, rowUnitSelect.value);
          });

          var gramsHint = h('span', 'grams-hint', '');
          var weightLine = h('div', 'weight-line');
          weightLine.appendChild(weightInput);
          weightLine.appendChild(rowUnitSelect);
          var weightCell = h('td', 'cell-weight');
          weightCell.appendChild(weightLine);
          weightCell.appendChild(gramsHint);
          tr.appendChild(weightCell);

          var rpeInput = document.createElement('input');
          rpeInput.type = 'number';
          rpeInput.min = '1';
          rpeInput.max = '10';
          rpeInput.step = '0.5';
          rpeInput.className = 'rpe-input';
          rpeInput.value = set.rpe === null || set.rpe === undefined ? '' : String(set.rpe);
          rpeInput.setAttribute('aria-label',
            'RPE, ' + (block.name || 'unnamed exercise') + ', set ' + set.index);
          rpeInput.addEventListener('input', function () {
            updateSetNumber(sessionIndex, blockIndex, setIndex, 'rpe', rpeInput.value);
          });
          var rpeCell = h('td');
          rpeCell.appendChild(rpeInput);
          tr.appendChild(rpeCell);

          var roleSelect = document.createElement('select');
          roleSelect.className = 'role-select';
          roleSelect.setAttribute('aria-label',
            'Type, ' + (block.name || 'unnamed exercise') + ', set ' + set.index);
          ROLE_NAMES.forEach(function (role) {
            var option = document.createElement('option');
            option.value = role;
            option.textContent = roleLabel(role);
            roleSelect.appendChild(option);
          });
          roleSelect.value = set.role;
          roleSelect.addEventListener('change', function () {
            updateSetRole(sessionIndex, blockIndex, setIndex, roleSelect.value);
          });
          var roleCell = h('td');
          roleCell.appendChild(roleSelect);
          tr.appendChild(roleCell);

          var problem = h('span', 'row-problem', '');
          var problemCell = h('td', 'cell-problem');
          problemCell.appendChild(problem);
          tr.appendChild(problemCell);

          tbody.appendChild(tr);
          setEntries.push({
            tr: tr,
            checkbox: checkbox,
            repsInput: repsInput,
            weightInput: weightInput,
            unitSelect: rowUnitSelect,
            rpeInput: rpeInput,
            roleSelect: roleSelect,
            gramsHint: gramsHint,
            problem: problem
          });
        });

        var lastSelected = null;
        setEntries.forEach(function (entry, entryIndex) {
          entry.checkbox.addEventListener('click', function (event) {
            if (event && event.shiftKey && lastSelected !== null) {
              var low = Math.min(lastSelected, entryIndex);
              var high = Math.max(lastSelected, entryIndex);
              for (var rangeIndex = low; rangeIndex <= high; rangeIndex += 1) {
                setEntries[rangeIndex].checkbox.checked = true;
              }
            }
            lastSelected = entryIndex;
          });
        });
        table.appendChild(tbody);
        var tableWrap = h('div', 'table-scroll');
        tableWrap.appendChild(table);
        blockEl.appendChild(tableWrap);
        body.appendChild(blockEl);
        blockEntries.push({
          root: blockEl,
          nameInput: nameInput,
          unitSelect: unitSelect,
          note: note,
          badge: blockBadge,
          firstInput: firstInput,
          sets: setEntries
        });
      });

      details.appendChild(body);
      container.appendChild(details);
      state.dom.sessions.push({
        details: details,
        dateInput: dateInput,
        timeInput: timeInput,
        durationInput: durationInput,
        offsetInput: offsetInput,
        offsetText: offsetText,
        dateSpan: dateSpan,
        metaSpan: metaSpan,
        badge: badge,
        blocks: blockEntries
      });
    });
  }

  function warmupCount(block) {
    var count = 0;
    block.sets.forEach(function (set) {
      if (set.role === 'WARMUP') { count += 1; }
    });
    return count;
  }

  function summaryDate(session) {
    if (session.date) { return formatDateDisplay(session.date); }
    if (session.dateRaw) { return 'Unreadable date: ' + session.dateRaw; }
    return 'No date';
  }

  function summaryMeta(session) {
    var exercises = session.blocks.length;
    var sets = 0;
    var warmups = 0;
    var workGrams = 0;
    var warmGrams = 0;
    session.blocks.forEach(function (block) {
      block.sets.forEach(function (set) {
        sets += 1;
        if (set.role === 'WARMUP') { warmups += 1; }
        var grams = unitToGrams(set.weight, set.unit);
        var volume = grams === null ? 0 : grams * (set.reps || 0);
        if (roleRecordsEffort(set.role)) {
          workGrams += volume;
        } else if (set.role === 'WARMUP') {
          warmGrams += volume;
        }
      });
    });
    return exercises + ' exercise' + (exercises === 1 ? '' : 's') + ' · ' +
      sets + ' set' + (sets === 1 ? '' : 's') + ' · ' +
      warmups + ' warm-up' + (warmups === 1 ? '' : 's') + ' · ' +
      'work ' + formatTonnage(workGrams) + ' · warm-up ' + formatTonnage(warmGrams);
  }

  function badgeText(blocking, warnings) {
    if (blocking === 0 && warnings === 0) { return 'No problems'; }
    var parts = [];
    if (blocking > 0) {
      parts.push(blocking + (blocking === 1 ? ' blocking problem' : ' blocking problems'));
    }
    if (warnings > 0) {
      parts.push(warnings + (warnings === 1 ? ' warning' : ' warnings'));
    }
    return parts.join(', ');
  }

  function bucket(index, sessionIndex, blockIndex, setIndex) {
    if (!index || !has(index.bySession, sessionIndex)) { return []; }
    var sessionEntry = index.bySession[sessionIndex];
    if (blockIndex === null || blockIndex === undefined) { return sessionEntry.list; }
    if (!has(sessionEntry.blocks, blockIndex)) { return []; }
    var blockEntry = sessionEntry.blocks[blockIndex];
    if (setIndex === null || setIndex === undefined) { return blockEntry.list; }
    return blockEntry.sets[setIndex] || [];
  }

  function refreshSessions() {
    var state = ui.state;
    if (!state.dom || !state.model) { return; }
    state.dom.sessions.forEach(function (domSession, sessionIndex) {
      var session = state.model.sessions[sessionIndex];
      var blocking = bucket(state.problemIndex, sessionIndex, null, null);
      var warnings = bucket(state.warningIndex, sessionIndex, null, null);
      setText(domSession.badge, badgeText(blocking.length, warnings.length));
      domSession.badge.className = 'badge ' +
        (blocking.length > 0 ? 'badge-blocking' : warnings.length > 0 ? 'badge-warning' : 'badge-ok');
      setText(domSession.dateSpan, summaryDate(session));
      setText(domSession.metaSpan, summaryMeta(session));
      setText(domSession.offsetText, session.zoneOffsetMinutes === null
        ? 'No zone offset: this session has no date yet.'
        : formatOffset(session.zoneOffsetMinutes) + ' (' + session.zoneOffsetMinutes +
          ' minutes east of UTC, ' +
          (session.manualOffset ? 'as stated by the source' : 'computed from this browser') + ')');
      if (domSession.offsetInput && document.activeElement !== domSession.offsetInput) {
        domSession.offsetInput.value = session.zoneOffsetMinutes === null
          ? '' : String(session.zoneOffsetMinutes);
      }
      domSession.details.hidden = state.onlyProblems && blocking.length + warnings.length === 0;

      domSession.blocks.forEach(function (domBlock, blockIndex) {
        var block = session.blocks[blockIndex];
        var blockBlocking = bucket(state.problemIndex, sessionIndex, blockIndex, null);
        var blockWarnings = bucket(state.warningIndex, sessionIndex, blockIndex, null);
        setText(domBlock.badge, badgeText(blockBlocking.length, blockWarnings.length));
        domBlock.badge.className = 'badge ' +
          (blockBlocking.length > 0 ? 'badge-blocking'
            : blockWarnings.length > 0 ? 'badge-warning' : 'badge-ok');
        setText(domBlock.note, resolutionNoteFor(block));

        domBlock.sets.forEach(function (domSet, setIndex) {
          var set = block.sets[setIndex];
          var setBlocking = bucket(state.problemIndex, sessionIndex, blockIndex, setIndex);
          var setWarnings = bucket(state.warningIndex, sessionIndex, blockIndex, setIndex);
          var messages = [];
          setBlocking.forEach(function (problem) { messages.push(problem.message); });
          setWarnings.forEach(function (problem) { messages.push(problem.message); });
          setText(domSet.problem, messages.join(' '));
          domSet.problem.className = 'row-problem ' +
            (setBlocking.length > 0 ? 'row-problem-blocking'
              : setWarnings.length > 0 ? 'row-problem-warning' : '');
          var grams = unitToGrams(set.weight, set.unit);
          setText(domSet.gramsHint, grams === null ? '' :
            formatWeight(grams, set.unit) + ' ' + set.unit + ' is ' + formatGrams(grams));
        });
      });
    });
  }

  function resolutionNoteFor(block) {
    var state = ui.state;
    if (!state.model || !normalizeName(block.name)) { return 'No exercise yet'; }
    var resolution = resolveExercise(block.name, state.model);
    if (resolution.kind === 'library') {
      var text = 'Library: ' + resolution.name;
      if (resolution.dto && resolution.dto.weightUnit) {
        text += ' (' + resolution.dto.weightUnit + ')';
      }
      return text;
    }
    if (resolution.kind === 'new') {
      return 'New exercise (' + resolution.weightUnit + ')' +
        (resolution.deletedOnly ? '; the matching library row is deleted' : '');
    }
    return 'Unmapped: choose a library exercise or create a new one.';
  }

  function indexProblems(list) {
    var bySession = {};
    var global = [];
    list.forEach(function (problem) {
      var match = /^sessions\[(\d+)\](?:[.]exercises\[(\d+)\](?:[.]sets\[(\d+)\])?)?/.exec(problem.path);
      if (!match) {
        global.push(problem);
        return;
      }
      var sessionIndex = Number(match[1]);
      var blockIndex = match[2] === undefined ? null : Number(match[2]);
      var setIndex = match[3] === undefined ? null : Number(match[3]);
      if (!has(bySession, sessionIndex)) {
        bySession[sessionIndex] = { list: [], blocks: {} };
      }
      var sessionEntry = bySession[sessionIndex];
      sessionEntry.list.push(problem);
      if (blockIndex !== null) {
        if (!has(sessionEntry.blocks, blockIndex)) {
          sessionEntry.blocks[blockIndex] = { list: [], sets: {} };
        }
        var blockEntry = sessionEntry.blocks[blockIndex];
        blockEntry.list.push(problem);
        if (setIndex !== null) {
          if (!has(blockEntry.sets, setIndex)) { blockEntry.sets[setIndex] = []; }
          blockEntry.sets[setIndex].push(problem);
        }
      }
    });
    return { bySession: bySession, global: global };
  }

  function refresh() {
    var state = ui.state;
    if (!state) { return; }
    var problems = state.inputProblems.slice();
    var warnings = state.inputWarnings.slice();
    if (state.model) {
      var result = revalidateModel(state.model);
      problems = problems.concat(result.problems);
      warnings = warnings.concat(result.warnings);
    }
    state.validation = { problems: problems, warnings: warnings };
    state.problemIndex = indexProblems(problems);
    state.warningIndex = indexProblems(warnings);

    refreshSessions();
    renderProblems();
    renderExport();
    refreshOrderNote();
    announceCounts();
  }

  function renderProblems() {
    var state = ui.state;
    var problems = state.validation.problems;
    var warnings = state.validation.warnings;
    setText(ui.elements.problemsCounts,
      problems.length + ' blocking, ' + warnings.length + ' warning' +
      (warnings.length === 1 ? '' : 's'));

    var list = ui.elements.problemList;
    list.textContent = '';

    if (problems.length === 0 && warnings.length === 0) {
      list.appendChild(h('li', 'problem-empty',
        state.model ? 'No problems.' : 'Load rows to see problems.'));
      return;
    }

    function addItem(problem, blocking) {
      var item = document.createElement('li');
      var button = document.createElement('button');
      button.type = 'button';
      button.className = 'problem ' + (blocking ? 'problem-blocking' : 'problem-warning');
      button.appendChild(h('span', 'problem-tag', blocking ? 'Blocks export' : 'Warning'));
      button.appendChild(h('span', 'problem-message', problem.message));
      button.appendChild(h('span', 'problem-path', problem.path));
      button.addEventListener('click', function () { focusProblem(problem.path); });
      item.appendChild(button);
      list.appendChild(item);
    }

    problems.forEach(function (problem) { addItem(problem, true); });
    warnings.forEach(function (problem) { addItem(problem, false); });
  }

  function announceCounts() {
    var state = ui.state;
    if (!state) { return; }
    var blocking = state.validation.problems.length;
    var warnings = state.validation.warnings.length;
    if (blocking === 0 && warnings === 0) {
      state.lastCounts = '';
      return;
    }
    var summary = blocking + ' blocking, ' + warnings + ' warning' +
      (warnings === 1 ? '' : 's') + '.';
    if (summary !== state.lastCounts) {
      state.lastCounts = summary;
      announce(summary);
    }
  }

  function parseModelPath(path) {
    var parts = [];
    var expression = /([A-Za-z]+)(?:\[(\d+)\])?/g;
    var match = expression.exec(path);
    while (match) {
      parts.push({
        key: match[1],
        index: match[2] === undefined ? null : Number(match[2])
      });
      match = expression.exec(path);
    }
    return parts;
  }

  function focusProblem(path) {
    var state = ui.state;
    var parts = parseModelPath(path);
    var source = ui.elements.pasteBox;

    if (parts.length === 0) {
      if (source) { source.focus(); }
      return;
    }

    if (parts[0].key === 'rows' || parts[0].key === 'columns' ||
        parts[0].key === 'document' || parts[0].key === 'root') {
      if (source) { source.focus(); source.scrollIntoView({ block: 'center' }); }
      announce('The problem is in the input text.');
      return;
    }

    if (parts[0].key !== 'sessions' || !state.dom ||
        !has(state.dom.sessions, parts[0].index)) {
      var mapping = byId('panel-source');
      if (mapping) { mapping.scrollIntoView({ block: 'start' }); }
      return;
    }

    var session = state.dom.sessions[parts[0].index];
    session.details.open = true;
    var target = session.dateInput;

    if (parts.length > 1 && parts[1].key === 'exercises') {
      if (parts[1].index === null || !has(session.blocks, parts[1].index)) {
        target = session.blocks.length > 0 ? session.blocks[0].nameInput : session.dateInput;
      } else {
        var block = session.blocks[parts[1].index];
        target = block.nameInput;
        if (parts.length > 2 && parts[2].key === 'sets') {
          if (parts[2].index === null || !has(block.sets, parts[2].index)) {
            target = block.sets.length > 0 ? block.sets[0].repsInput : block.nameInput;
          } else {
            var set = block.sets[parts[2].index];
            var field = parts.length > 3 ? parts[3].key : 'reps';
            if (field === 'weight' || field === 'weightGrams') { target = set.weightInput; }
            else if (field === 'rpe' || field === 'rpeHalves') { target = set.rpeInput; }
            else if (field === 'setType') { target = set.roleSelect; }
            else if (field === 'unit') { target = set.unitSelect; }
            else { target = set.repsInput; }
          }
        } else if (parts.length > 2 && (parts[2].key === 'unit' || parts[2].key === 'weightUnit')) {
          target = block.unitSelect;
        }
      }
    } else if (parts.length > 1 && parts[1].key === 'time') {
      target = session.timeInput;
    } else if (parts.length > 1 && parts[1].key === 'durationMinutes') {
      target = session.durationInput;
    }

    if (target && typeof target.focus === 'function') {
      target.focus();
      if (typeof target.scrollIntoView === 'function') {
        target.scrollIntoView({ block: 'center' });
      }
      announce('Moved to ' + path + '.');
    }
  }

  function refreshOrderNote() {
    var state = ui.state;
    var note = ui.elements.ambiguityNote;
    if (!state.model || state.model.dateStats.total === 0) {
      setText(note, 'No dates to check yet. Dates are read as DD-MM-YYYY until you change it.');
      return;
    }
    var stats = state.model.dateStats;
    var orderLabel = state.dateOrder === 'mdy' ? 'MM-DD-YYYY' : 'DD-MM-YYYY';
    var text;
    if (stats.ambiguous === 0) {
      text = 'No ambiguous dates; read as ' + orderLabel + '.';
    } else {
      text = formatInt(stats.ambiguous) + ' of ' + formatInt(stats.total) +
        ' dates are ambiguous; read as ' + orderLabel + '.';
    }
    text += ' DD-MM-YYYY: ' + stats.readings.dmy.future + ' in the future, ' +
      stats.readings.dmy.before1970 + ' before 1970, ' +
      stats.readings.dmy.unreadable + ' unreadable.';
    text += ' MM-DD-YYYY: ' + stats.readings.mdy.future + ' in the future, ' +
      stats.readings.mdy.before1970 + ' before 1970, ' +
      stats.readings.mdy.unreadable + ' unreadable.';
    setText(note, text);
  }

  /* ------------------------------------------------------------------ *
   * The progress panel (N165)
   * ------------------------------------------------------------------ */

  function svgElement(tag) {
    if (typeof document.createElementNS === 'function') {
      return document.createElementNS('http://www.w3.org/2000/svg', tag);
    }
    return document.createElement(tag);
  }

  function svgAttr(element, name, value) {
    element.setAttribute(name, String(value));
    return element;
  }

  function formatChartDate(millis) {
    var date = new Date(millis);
    var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
      'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    return date.getDate() + ' ' + months[date.getMonth()] + ' ' +
      String(date.getFullYear()).slice(2);
  }

  /* A round tick step: 1, 2 or 5 times a power of ten. */
  function niceStep(raw) {
    if (!(raw > 0)) { return 1; }
    var power = Math.pow(10, Math.floor(Math.log(raw) / Math.LN10));
    var normalised = raw / power;
    var step = normalised <= 1 ? 1 : normalised <= 2 ? 2 : normalised <= 5 ? 5 : 10;
    return step * power;
  }

  function formatKg(grams) {
    var value = grams / 1000;
    return value.toFixed(1).replace(/\.0$/, '');
  }

  /* The movements this export actually has history for, sorted, so the picker is not
   * a list of ninety lifts that would draw an empty chart. */
  function movementsWithHistory(backup) {
    var byId = {};
    (Array.isArray(backup.exercises) ? backup.exercises : []).forEach(function (exercise) {
      if (!isObject(exercise) || typeof exercise.id !== 'string') { return; }
      if (!isLoggableRow(exercise)) { return; }
      if (exercise.deletedAt !== null && exercise.deletedAt !== undefined) { return; }
      if (typeof exercise.name !== 'string' || exercise.name.trim() === '') { return; }
      byId[exercise.id] = exercise;
    });
    var exerciseOfLink = {};
    (Array.isArray(backup.sessionExercises) ? backup.sessionExercises : []).forEach(function (link) {
      if (isObject(link) && typeof link.id === 'string') { exerciseOfLink[link.id] = link.exerciseId; }
    });
    var setCount = {};
    (Array.isArray(backup.sets) ? backup.sets : []).forEach(function (set) {
      var id = exerciseOfLink[set && set.sessionExerciseId];
      if (id) { setCount[id] = (setCount[id] || 0) + 1; }
    });
    var list = Object.keys(byId)
      .map(function (id) { return { id: id, name: byId[id].name, sets: setCount[id] || 0 }; })
      .filter(function (row) { return row.sets > 0; });
    list.sort(function (a, b) { return compareExerciseNames(a.name, b.name); });
    return list;
  }

  function renderProgress() {
    var state = ui.state;
    var panel = ui.elements.progressPanel;
    if (!panel) { return; }
    var backup = state.catalogFile;
    var hasHistory = backup && !backup.seeded &&
      Array.isArray(backup.sessions) && backup.sessions.length > 0 &&
      Array.isArray(backup.exercises) && backup.exercises.length > 0;
    panel.hidden = !hasHistory;
    if (!hasHistory) { return; }

    var movements = movementsWithHistory(backup);
    var list = ui.elements.progressLifts;
    list.textContent = '';
    movements.forEach(function (movement) {
      var option = document.createElement('option');
      option.value = movement.name;
      list.appendChild(option);
    });

    var selected = null;
    var typed = ui.elements.progressLift.value.trim();
    if (typed !== '') {
      movements.forEach(function (movement) {
        if (normalizeName(movement.name) === normalizeName(typed)) { selected = movement; }
      });
    }
    if (!selected && state.progressExerciseId) {
      movements.forEach(function (movement) {
        if (movement.id === state.progressExerciseId) { selected = movement; }
      });
    }
    if (!selected && movements.length > 0) {
      // The most-trained movement is the useful default, not the alphabet's first.
      selected = movements.slice().sort(function (a, b) { return b.sets - a.sets; })[0];
    }

    var chart = ui.elements.progressChart;
    var summary = ui.elements.progressSummary;
    if (!selected) {
      state.progressExerciseId = null;
      ui.elements.progressLift.value = '';
      setText(summary, 'This export has no logged movements to chart.');
      chart.textContent = '';
      return;
    }
    state.progressExerciseId = selected.id;
    if (ui.elements.progressLift.value !== selected.name) {
      ui.elements.progressLift.value = selected.name;
    }

    var points = liftTrendPoints(backup, selected.id);
    var range = ui.elements.progressRange.value;
    var days = range === '365' ? 365 : range === '90' ? 90 : null;
    if (days !== null && points.length > 0) {
      var newest = points[points.length - 1].startedAt;
      points = points.filter(function (point) {
        return point.startedAt >= newest - days * 86400000;
      });
    }
    drawProgressChart(chart, points, selected);
    setText(summary, progressSummary(points, selected));
  }

  function progressSummary(points, movement) {
    if (points.length === 0) {
      return movement.name + ': no sessions in this range.';
    }
    var bestEstimate = null;
    var heaviest = null;
    points.forEach(function (point) {
      if (point.estimatedOneRepMaxGrams !== null &&
          (bestEstimate === null || point.estimatedOneRepMaxGrams > bestEstimate.grams)) {
        bestEstimate = { grams: point.estimatedOneRepMaxGrams, at: point.startedAt };
      }
      if (point.heaviestSetGrams !== null &&
          (heaviest === null || point.heaviestSetGrams > heaviest.grams)) {
        heaviest = { grams: point.heaviestSetGrams, at: point.startedAt };
      }
    });
    var parts = [movement.name, formatInt(points.length) + ' session' +
      (points.length === 1 ? '' : 's')];
    if (bestEstimate) {
      parts.push('best estimate ' + formatKg(bestEstimate.grams) + ' kg on ' +
        formatChartDate(bestEstimate.at));
    }
    if (heaviest) {
      parts.push('heaviest ' + formatKg(heaviest.grams) + ' kg on ' +
        formatChartDate(heaviest.at));
    }
    if (!bestEstimate) {
      parts.push('no set at 12 reps or fewer, so no estimate');
    }
    return parts.join(' · ');
  }

  function drawProgressChart(host, points, movement) {
    host.textContent = '';
    var usable = points.filter(function (point) {
      return point.heaviestSetGrams !== null || point.estimatedOneRepMaxGrams !== null;
    });
    if (usable.length === 0) {
      host.appendChild(h('p', 'empty', 'No working sets in this range.'));
      return;
    }

    var width = 900;
    var height = 340;
    var left = 64;
    var right = 18;
    var top = 16;
    var bottom = 42;
    var plotW = width - left - right;
    var plotH = height - top - bottom;

    var values = [];
    usable.forEach(function (point) {
      if (point.estimatedOneRepMaxGrams !== null) { values.push(point.estimatedOneRepMaxGrams / 1000); }
      if (point.heaviestSetGrams !== null) { values.push(point.heaviestSetGrams / 1000); }
    });
    var minKg = Math.min.apply(null, values);
    var maxKg = Math.max.apply(null, values);
    var span = Math.max(maxKg - minKg, 5);
    minKg = Math.max(0, minKg - span * 0.15);
    maxKg = maxKg + span * 0.15;
    var step = niceStep((maxKg - minKg) / 4);
    minKg = Math.floor(minKg / step) * step;
    maxKg = Math.ceil(maxKg / step) * step;

    var first = usable[0].startedAt;
    var last = usable[usable.length - 1].startedAt;
    var timeSpan = last - first;
    var xOf = function (millis) {
      if (timeSpan <= 0) { return left + plotW / 2; }
      return left + ((millis - first) / timeSpan) * plotW;
    };
    var yOf = function (kg) {
      return top + plotH - ((kg - minKg) / (maxKg - minKg)) * plotH;
    };

    var svg = svgElement('svg');
    svgAttr(svg, 'viewBox', '0 0 ' + width + ' ' + height);
    svgAttr(svg, 'preserveAspectRatio', 'xMidYMid meet');
    svgAttr(svg, 'role', 'img');
    svgAttr(svg, 'class', 'progress-svg');
    svg.setAttribute('aria-label', movement.name + ' progress: estimated one-rep max and heaviest set');

    // Horizontal grid and the kilogram labels.
    for (var kg = minKg; kg <= maxKg + 0.001; kg += step) {
      var y = yOf(kg);
      var line = svgElement('line');
      svgAttr(line, 'x1', left);
      svgAttr(line, 'x2', left + plotW);
      svgAttr(line, 'y1', y);
      svgAttr(line, 'y2', y);
      svgAttr(line, 'class', 'grid');
      svg.appendChild(line);
      var label = svgElement('text');
      svgAttr(label, 'x', left - 10);
      svgAttr(label, 'y', y + 4);
      svgAttr(label, 'text-anchor', 'end');
      svgAttr(label, 'class', 'axis');
      label.textContent = formatKg(kg * 1000) + ' kg';
      svg.appendChild(label);
    }

    // The date axis: first and last, and the middle when there is room.
    [first, last].forEach(function (millis, index) {
      var text = svgElement('text');
      svgAttr(text, 'x', xOf(millis));
      svgAttr(text, 'y', top + plotH + 24);
      svgAttr(text, 'text-anchor', index === 0 ? 'start' : 'end');
      svgAttr(text, 'class', 'axis');
      text.textContent = formatChartDate(millis);
      svg.appendChild(text);
    });
    if (timeSpan > 0) {
      var middle = svgElement('text');
      svgAttr(middle, 'x', left + plotW / 2);
      svgAttr(middle, 'y', top + plotH + 24);
      svgAttr(middle, 'text-anchor', 'middle');
      svgAttr(middle, 'class', 'axis');
      middle.textContent = formatChartDate(first + timeSpan / 2);
      svg.appendChild(middle);
    }

    // The estimated 1RM line, broken where a session has no estimate.
    var segments = [];
    var current = [];
    usable.forEach(function (point) {
      if (point.estimatedOneRepMaxGrams === null) {
        if (current.length > 0) { segments.push(current); current = []; }
        return;
      }
      current.push(point);
      var dot = svgElement('circle');
      svgAttr(dot, 'cx', xOf(point.startedAt));
      svgAttr(dot, 'cy', yOf(point.estimatedOneRepMaxGrams / 1000));
      svgAttr(dot, 'r', 2.5);
      svgAttr(dot, 'class', 'estimate');
      var title = svgElement('title');
      title.textContent = formatChartDate(point.startedAt) + ': estimate ' +
        formatKg(point.estimatedOneRepMaxGrams) + ' kg';
      dot.appendChild(title);
      svg.appendChild(dot);
    });
    if (current.length > 0) { segments.push(current); }
    segments.forEach(function (segment) {
      if (segment.length < 2) { return; }
      var linePath = svgElement('polyline');
      svgAttr(linePath, 'points', segment.map(function (point) {
        return xOf(point.startedAt) + ',' + yOf(point.estimatedOneRepMaxGrams / 1000);
      }).join(' '));
      svgAttr(linePath, 'class', 'estimate-line');
      svg.appendChild(linePath);
    });

    // The heaviest working set: the literal number, so a change of rep range is visible.
    usable.forEach(function (point) {
      if (point.heaviestSetGrams === null) { return; }
      var dot = svgElement('circle');
      svgAttr(dot, 'cx', xOf(point.startedAt));
      svgAttr(dot, 'cy', yOf(point.heaviestSetGrams / 1000));
      svgAttr(dot, 'r', 3.5);
      svgAttr(dot, 'class', 'heaviest');
      var title = svgElement('title');
      title.textContent = formatChartDate(point.startedAt) + ': heaviest ' +
        formatKg(point.heaviestSetGrams) + ' kg';
      dot.appendChild(title);
      svg.appendChild(dot);
    });

    host.appendChild(svg);

    var legend = h('p', 'progress-legend');
    legend.appendChild(h('span', 'legend-estimate', 'Estimated 1RM (Epley)'));
    legend.appendChild(h('span', 'legend-heaviest', 'Heaviest working set'));
    host.appendChild(legend);
  }

  function renderExport() {
    var state = ui.state;
    var refusal = ui.elements.exportRefusal;
    var download = ui.elements.downloadExport;
    var copy = ui.elements.copyExport;
    var preview = ui.elements.exportText;

    if (!state.model) {
      setText(ui.elements.exportStats, 'Load rows to begin.');
      refusal.hidden = true;
      setText(refusal, '');
      download.disabled = true;
      copy.disabled = true;
      if (document.activeElement !== preview) { preview.value = ''; }
      state.exportResult = null;
      return;
    }

    var stats = computeExportStats(state.model);
    var statsText = 'Will add ' + stats.addedSessions + ' session' +
      (stats.addedSessions === 1 ? '' : 's') + ' and ' + stats.addedSets + ' set' +
      (stats.addedSets === 1 ? '' : 's') + '; ' + stats.skippedSessions + ' session' +
      (stats.skippedSessions === 1 ? '' : 's') +
      ' already exist and will be skipped; ' + stats.newExercises + ' new exercise' +
      (stats.newExercises === 1 ? '' : 's') + '.';
    setText(ui.elements.exportStats, statsText);

    var problems = state.validation.problems;
    if (problems.length > 0) {
      var first = problems.slice(0, 3).map(function (problem) {
        return problem.path + ': ' + problem.message;
      });
      refusal.hidden = false;
      setText(refusal, 'Export blocked by ' + problems.length + ' problem' +
        (problems.length === 1 ? '' : 's') + '. ' + first.join(' '));
      download.disabled = true;
      copy.disabled = true;
      if (document.activeElement !== preview) { preview.value = ''; }
      state.exportResult = null;
      return;
    }

    var result = buildExport(state.model, { exportedAt: state.exportedAt });
    state.exportResult = result;
    if (!result.ok) {
      refusal.hidden = false;
      setText(refusal, 'Export refused: ' + result.problems.map(function (problem) {
        return problem.path + ': ' + problem.message;
      }).slice(0, 3).join(' '));
      download.disabled = true;
      copy.disabled = true;
      if (document.activeElement !== preview) { preview.value = ''; }
      return;
    }

    refusal.hidden = true;
    setText(refusal, '');
    download.disabled = false;
    copy.disabled = false;
    if (document.activeElement !== preview) { preview.value = result.json; }
  }

  function updateSessionDate(sessionIndex, raw) {
    var state = ui.state;
    var session = state.model.sessions[sessionIndex];
    var domSession = state.dom.sessions[sessionIndex];
    var text = String(raw === null || raw === undefined ? '' : raw).trim();

    if (text === '') {
      domSession.dateInput.value = session.dateRaw || '';
      announce('A session needs a date. Fix the source rows if this session should not exist.');
      return;
    }

    var parsed = parseDateValue(text, state.dateOrder);
    session.dateRaw = text;
    session.date = parsed.ok ? parsed : null;
    session.issues = session.issues.filter(function (issue) { return issue.field !== 'date'; });
    if (!parsed.ok) {
      session.issues.push({
        field: 'date',
        kind: parsed.error && parsed.error.indexOf('two-digit') !== -1 ? 'two-digit' : 'unreadable',
        raw: text,
        detail: parsed.error
      });
    }
    if (session.date) {
      session.startedAt = localEpochMillis(
        session.date.y, session.date.month, session.date.day, session.hh, session.mm
      );
      session.dateKey = dateKeyOf(session.date);
      if (!session.manualOffset) {
        session.zoneOffsetMinutes = zoneOffsetMinutesFor(session.startedAt);
      }
    } else {
      session.startedAt = null;
      session.dateKey = 'none';
      if (!session.manualOffset) { session.zoneOffsetMinutes = null; }
    }
    recomputeFinished(session);
    var row = state.rows[session.sourceRow - 1];
    if (row) { row.date = text; }
    if (!parsed.ok) {
      announce('That date could not be read. Use ' +
        (state.dateOrder === 'mdy' ? 'MM-DD-YYYY' : 'DD-MM-YYYY') +
        ' (or the same with / or .) or YYYY-MM-DD.');
    }
    refresh();
  }

  function updateSessionTime(sessionIndex, raw) {
    var state = ui.state;
    var session = state.model.sessions[sessionIndex];
    var domSession = state.dom.sessions[sessionIndex];
    var parsed = parseTimeValue(raw);
    if (!parsed.ok) {
      domSession.timeInput.value = formatTime(session.hh, session.mm);
      announce('Use a 24-hour time like 18:30.');
      return;
    }
    session.hh = parsed.hh;
    session.mm = parsed.mm;
    session.timeRaw = parsed.empty ? null : String(raw).trim();
    session.issues = session.issues.filter(function (issue) { return issue.field !== 'time'; });
    if (session.date) {
      session.startedAt = localEpochMillis(
        session.date.y, session.date.month, session.date.day, session.hh, session.mm
      );
      if (!session.manualOffset) {
        session.zoneOffsetMinutes = zoneOffsetMinutesFor(session.startedAt);
      }
    }
    recomputeFinished(session);
    var row = state.rows[session.sourceRow - 1];
    if (row) { row.time = session.timeRaw; }
    refresh();
  }

  function updateSessionDuration(sessionIndex, raw) {
    var state = ui.state;
    var session = state.model.sessions[sessionIndex];
    var domSession = state.dom.sessions[sessionIndex];
    var text = String(raw === null || raw === undefined ? '' : raw).trim();
    var value = text === '' ? NaN : Number(text);
    if (!isFinite(value)) {
      domSession.durationInput.value = String(session.durationMinutes);
      announce('Enter the duration in whole minutes.');
      return;
    }
    session.durationMinutes = Math.round(value);
    recomputeFinished(session);
    var row = state.rows[session.sourceRow - 1];
    if (row) { row.durationMinutes = session.durationMinutes; }
    refresh();
  }

  /* The stated offset lives on the source row, so it survives a rebuild (N163). */
  function updateSessionOffset(sessionIndex, raw) {
    var state = ui.state;
    var session = state.model && state.model.sessions[sessionIndex];
    if (!session) { return; }
    var row = state.rows[session.sourceRow - 1];
    if (!isObject(row)) { return; }
    var text = String(raw === null || raw === undefined ? '' : raw).trim();
    if (text === '') {
      if (has(row, 'zoneOffsetMinutes')) { delete row.zoneOffsetMinutes; }
      session.manualOffset = false;
      session.zoneOffsetMinutes = session.startedAt === null
        ? null : zoneOffsetMinutesFor(session.startedAt);
    } else {
      var value = Number(text);
      if (!isFinite(value) || Math.round(value) !== value || Math.abs(value) > 840) {
        announce('The offset must be whole minutes from UTC, within ±840.');
        refresh();
        return;
      }
      row.zoneOffsetMinutes = Math.round(value);
      session.manualOffset = true;
      session.zoneOffsetMinutes = Math.round(value);
    }
    refresh();
    announce('The session\'s UTC offset is ' + formatOffset(session.zoneOffsetMinutes) + '.');
  }

  function recomputeFinished(session) {
    if (session.startedAt === null || typeof session.durationMinutes !== 'number') {
      session.finishedAt = null;
      return;
    }
    session.finishedAt = session.startedAt + session.durationMinutes * 60000;
  }

  function updateBlockName(sessionIndex, blockIndex, raw) {
    var state = ui.state;
    var block = state.model.sessions[sessionIndex].blocks[blockIndex];
    var text = String(raw === null || raw === undefined ? '' : raw).trim();
    block.name = text;
    block.placeholder = text === '';
    block.issues = block.issues.filter(function (issue) { return issue.field !== 'name'; });
    block.sets.forEach(function (set) {
      var row = state.rows[set.sourceRow - 1];
      if (row) { row.exercise = text === '' ? null : text; }
    });
    refresh();
  }

  function updateBlockUnit(sessionIndex, blockIndex, unit) {
    var state = ui.state;
    var block = state.model.sessions[sessionIndex].blocks[blockIndex];
    var value = unit === 'lb' ? 'lb' : 'kg';
    block.unit = value;
    block.manualUnit = true;
    block.sets.forEach(function (set) {
      set.unit = value;
      var row = state.rows[set.sourceRow - 1];
      if (row) { row.unit = value; }
    });
    var domBlock = state.dom.sessions[sessionIndex].blocks[blockIndex];
    domBlock.sets.forEach(function (domSet) { domSet.unitSelect.value = value; });
    refresh();
  }

  function updateSetNumber(sessionIndex, blockIndex, setIndex, field, raw) {
    var state = ui.state;
    var set = state.model.sessions[sessionIndex].blocks[blockIndex].sets[setIndex];
    var row = state.rows[set.sourceRow - 1];
    var text = String(raw === null || raw === undefined ? '' : raw).trim();
    var value = text === '' ? null : Number(text);
    if (value !== null && !isFinite(value)) { value = null; }

    if (field === 'reps') {
      set.reps = value;
      if (row) { row.reps = value; }
      set.issues = set.issues.filter(function (issue) { return issue.field !== 'reps'; });
    } else if (field === 'weight') {
      set.weight = value;
      if (row) { row.weight = value; }
      set.issues = set.issues.filter(function (issue) { return issue.field !== 'weight'; });
    } else if (field === 'rpe') {
      set.rpe = value;
      if (row) { row.rpe = value; }
      set.issues = set.issues.filter(function (issue) { return issue.field !== 'rpe'; });
    }
    refresh();
  }

  function updateSetUnit(sessionIndex, blockIndex, setIndex, unit) {
    var state = ui.state;
    var block = state.model.sessions[sessionIndex].blocks[blockIndex];
    var set = block.sets[setIndex];
    block.manualUnit = true;
    set.unit = unit === 'lb' ? 'lb' : 'kg';
    set.issues = set.issues.filter(function (issue) { return issue.field !== 'unit'; });
    var row = state.rows[set.sourceRow - 1];
    if (row) { row.unit = set.unit; }
    refresh();
  }

  function updateSetRole(sessionIndex, blockIndex, setIndex, role) {
    var state = ui.state;
    var set = state.model.sessions[sessionIndex].blocks[blockIndex].sets[setIndex];
    assignRole(set, role);
    refresh();
  }

  function assignRole(set, role) {
    set.role = role;
    set.roleRaw = role;
    set.roleRecognised = true;
    set.roleRefused = REFUSED_ROLES.indexOf(role) !== -1;
    set.issues = set.issues.filter(function (issue) { return issue.field !== 'type'; });
    var row = ui.state.rows[set.sourceRow - 1];
    if (row) { row.type = role; }
  }

  function applyFirstWarmups(sessionIndex, blockIndex, count) {
    var state = ui.state;
    var block = state.model.sessions[sessionIndex].blocks[blockIndex];
    block.sets.forEach(function (set, index) {
      if (index < count) {
        assignRole(set, 'WARMUP');
      } else if (set.role === 'WARMUP') {
        assignRole(set, 'NORMAL');
      }
    });
    var domBlock = state.dom.sessions[sessionIndex].blocks[blockIndex];
    domBlock.sets.forEach(function (domSet, index) {
      if (index < count) { domSet.roleSelect.value = 'WARMUP'; }
      else if (domSet.roleSelect.value === 'WARMUP') { domSet.roleSelect.value = 'NORMAL'; }
    });
    announce('Marked the first ' + count + ' set' + (count === 1 ? '' : 's') + ' as warm-up.');
    refresh();
  }

  function applySelectedWarmups(sessionIndex, blockIndex, role) {
    var state = ui.state;
    var block = state.model.sessions[sessionIndex].blocks[blockIndex];
    var domBlock = state.dom.sessions[sessionIndex].blocks[blockIndex];
    var count = 0;
    domBlock.sets.forEach(function (domSet, index) {
      if (!domSet.checkbox.checked) { return; }
      assignRole(block.sets[index], role);
      domSet.roleSelect.value = role;
      count += 1;
    });
    if (count === 0) {
      announce('Select at least one set first.');
      return;
    }
    announce('Marked ' + count + ' selected set' + (count === 1 ? '' : 's') + ' as ' +
      roleLabel(role).toLowerCase() + '.');
    refresh();
  }

  function onOrderChange(event) {
    var state = ui.state;
    var order = event && event.target && event.target.value === 'mdy' ? 'mdy' : 'dmy';
    if (order === state.dateOrder) { return; }
    state.dateOrder = order;
    ui.elements.orderDmy.checked = order === 'dmy';
    ui.elements.orderMdy.checked = order === 'mdy';
    if (state.rows.length > 0) {
      rebuildModel();
      renderAll();
      announce('Dates are now read as ' + (order === 'mdy' ? 'MM-DD-YYYY' : 'DD-MM-YYYY') + '.');
    } else {
      refreshOrderNote();
    }
  }

  function copyText(text, done) {
    if (!text) { done(false); return; }

    function fallback() {
      var area = document.createElement('textarea');
      area.value = text;
      area.setAttribute('readonly', 'readonly');
      area.style.position = 'fixed';
      area.style.left = '-9999px';
      document.body.appendChild(area);
      area.select();
      var ok = false;
      try { ok = document.execCommand('copy'); } catch (error) { ok = false; }
      area.remove();
      done(ok);
    }

    if (navigator.clipboard && typeof navigator.clipboard.writeText === 'function') {
      navigator.clipboard.writeText(text).then(function () { done(true); }, function () { fallback(); });
    } else {
      fallback();
    }
  }

  function copied(ok, target) {
    if (ok) {
      announce('Copied to the clipboard.');
      return;
    }
    if (target && typeof target.focus === 'function') {
      target.focus();
      if (typeof target.select === 'function') {
        target.select();
      } else if (typeof document.createRange === 'function' && typeof window.getSelection === 'function') {
        var range = document.createRange();
        range.selectNodeContents(target);
        var selection = window.getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
      }
    }
    announce('Copy failed. The text is selected; copy it by hand.');
  }

  function downloadExport() {
    var state = ui.state;
    if (!state.exportResult || !state.exportResult.ok) { return; }
    var blob = new Blob([state.exportResult.json], { type: 'application/json' });
    var url = URL.createObjectURL(blob);
    var link = document.createElement('a');
    link.href = url;
    link.download = BACKUP_FILE_NAME;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.setTimeout(function () { URL.revokeObjectURL(url); }, 0);
    announce('Downloaded ' + BACKUP_FILE_NAME + '.');
  }

  return {
    SCHEMA_VERSION: SCHEMA_VERSION,
    DEFAULT_DURATION_MINUTES: DEFAULT_DURATION_MINUTES,
    STORAGE_KEY: STORAGE_KEY,
    BACKUP_FILE_NAME: BACKUP_FILE_NAME,
    MODEL_PROMPT: MODEL_PROMPT,
    normalizeName: normalizeName,
    fnv1a64Hex: fnv1a64Hex,
    makeId: makeId,
    makeExerciseId: makeExerciseId,
    makeSessionId: makeSessionId,
    makeSetId: makeSetId,
    makeSessionExerciseId: makeSessionExerciseId,
    parseDateValue: parseDateValue,
    parseTimeValue: parseTimeValue,
    localEpochMillis: localEpochMillis,
    zoneOffsetMinutesFor: zoneOffsetMinutesFor,
    zoneOffsetFromGetTimezoneOffset: zoneOffsetFromGetTimezoneOffset,
    isFutureDate: isFutureDate,
    daysInMonth: daysInMonth,
    formatDateDisplay: formatDateDisplay,
    formatOffset: formatOffset,
    parseUnit: parseUnit,
    unitToGrams: unitToGrams,
    gramsToUnit: gramsToUnit,
    formatWeight: formatWeight,
    formatGrams: formatGrams,
    formatTonnage: formatTonnage,
    weightUnitName: weightUnitName,
    normalizeRole: normalizeRole,
    roleRecordsEffort: roleRecordsEffort,
    roleIsWork: roleIsWork,
    MAX_ESTIMATED_REPS: MAX_ESTIMATED_REPS,
    estimatedOneRepMax: estimatedOneRepMax,
    liftTrendPoints: liftTrendPoints,
    roleLabel: roleLabel,
    parseDelimitedCells: parseDelimitedCells,
    parseCsvText: parseCsvText,
    parseInputText: parseInputText,
    parseBackupText: parseBackupText,
    buildModel: buildModel,
    revalidateModel: revalidateModel,
    resolveExercise: resolveExercise,
    computeExportStats: computeExportStats,
    buildExport: buildExport,
    validateExportFile: validateExportFile,
    serializeExport: serializeExport,
    init: init
  };
});
