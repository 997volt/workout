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

  /* True unless the row carries a soft delete (B179).
   *
   * The app keeps a deleted row under its id — an export carries it, and an import can restore it —
   * so every read that answers "what is in this file" has to ask this first, exactly as the app's
   * own queries filter `deletedAt IS NULL`. */
  function isLive(row) {
    if (!isObject(row)) { return false; }
    return row.deletedAt === null || row.deletedAt === undefined;
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
   * app's own `setType != WARMUP` (N17).
   *
   * **The same rows the app's trend query reads** (B179): a session the lifter deleted or
   * never finished, an exercise the session holds as deleted, and a deleted set are all
   * absent from `TrendsDao`, so counting them here would draw a point the phone never
   * shows — a deleted 300 kg typo would become the movement's best session. */
  function liftTrendPoints(backup, exerciseId) {
    if (!isObject(backup) || !Array.isArray(backup.sessions)) { return []; }
    var sessionsById = {};
    backup.sessions.forEach(function (session) {
      if (!isObject(session) || typeof session.id !== 'string') { return; }
      if (!isLive(session)) { return; }
      if (typeof session.startedAt !== 'number' || typeof session.finishedAt !== 'number') { return; }
      sessionsById[session.id] = session;
    });
    var linkSession = {};
    var bySession = {};
    (Array.isArray(backup.sessionExercises) ? backup.sessionExercises : []).forEach(function (link) {
      if (!isObject(link) || link.exerciseId !== exerciseId) { return; }
      if (typeof link.id !== 'string' || typeof link.sessionId !== 'string') { return; }
      if (!isLive(link)) { return; }
      var session = sessionsById[link.sessionId];
      if (!session) { return; }
      linkSession[link.id] = link.sessionId;
      if (!has(bySession, link.sessionId)) {
        bySession[link.sessionId] = { startedAt: session.startedAt, sets: [] };
      }
    });
    (Array.isArray(backup.sets) ? backup.sets : []).forEach(function (set) {
      if (!isObject(set) || !has(linkSession, set.sessionExerciseId)) { return; }
      if (!isLive(set)) { return; }
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
      // A program table is a different shape from a history one; catch it before the row parser.
      if (looksLikeProgramTable(parseDelimitedCells(raw, detectDelimiter(raw)))) {
        return {
          kind: 'program',
          format: 'csv',
          rows: [],
          source: options.fileName || 'program-sheet',
          warnings: [],
          problems: [],
          program: parseProgramSheet(raw, options)
        };
      }
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

    if (Array.isArray(doc.diary)) {
      return {
        kind: 'waistline',
        format: 'json',
        rows: [],
        source: documentSource(doc, options),
        warnings: warnings,
        problems: problems,
        waistline: parseWaistlineDoc(doc)
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

  function computeExportStats(model, measurements) {
    measurements = Array.isArray(measurements) ? measurements : [];
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

    var existingMeasurementIds = {};
    var existingMeasurementDays = {};
    if (catalog.file) {
      (Array.isArray(catalog.file.measurements) ? catalog.file.measurements : [])
        .forEach(function (measurement) {
          if (!isObject(measurement)) { return; }
          if (typeof measurement.id === 'string') { existingMeasurementIds[measurement.id] = true; }
          if (typeof measurement.measuredAt !== 'number') { return; }
          if (!isLive(measurement)) { return; }
          existingMeasurementDays[localDayKey(measurement.measuredAt) + '|' + measurement.kind] = true;
        });
    }
    var skippedMeasurements = 0;
    var dayOverlaps = 0;
    measurements.forEach(function (measurement) {
      if (has(existingMeasurementIds, measurement.id)) {
        skippedMeasurements += 1;
        return;
      }
      // The app keeps one entry of each kind per day, so its own row wins over this one even when the
      // ids differ (B178) — the import adds no second live row for that day.
      if (typeof measurement.measuredAt === 'number' &&
          has(existingMeasurementDays, localDayKey(measurement.measuredAt) + '|' + measurement.kind)) {
        dayOverlaps += 1;
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
      unmatchedNames: unmatchedNames,
      measurements: measurements.length,
      addedMeasurements: measurements.length - skippedMeasurements - dayOverlaps,
      skippedMeasurements: skippedMeasurements,
      dayOverlaps: dayOverlaps
    };
  }

  function buildExport(model, options) {
    options = options || {};
    var validation = revalidateModel(model);
    var measurements = Array.isArray(options.measurements) ? options.measurements : [];
    var stats = computeExportStats(model, measurements);
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

    /* Key order is fixed: schemaVersion, exportedAt, then the collections. Measurements come last,
     * and only when the body data exists, so a history-only export is byte-for-byte what it was. */
    var file = {
      schemaVersion: SCHEMA_VERSION,
      exportedAt: exportedAt,
      exercises: exercises,
      sessions: sessions,
      sessionExercises: sessionExercises,
      sets: sets
    };
    if (measurements.length > 0) {
      file.measurements = measurements;
    }

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
    if (has(file, 'measurements') && !Array.isArray(file.measurements)) {
      add(problems, 'measurements', 'The measurements collection must be a list.');
    }

    var globalIds = {};
    var exercises = Array.isArray(file.exercises) ? file.exercises : [];
    var sessions = Array.isArray(file.sessions) ? file.sessions : [];
    var sessionExercises = Array.isArray(file.sessionExercises) ? file.sessionExercises : [];
    var sets = Array.isArray(file.sets) ? file.sets : [];

    if (Array.isArray(file.measurements)) {
      file.measurements.forEach(function (measurement, index) {
        var path = 'measurements[' + index + ']';
        if (!isObject(measurement)) {
          add(problems, path, 'A measurement must be an object.');
          return;
        }
        checkId(measurement, path);
        if (measurement.kind !== 'DAILY' && measurement.kind !== 'TAPE') {
          add(problems, path + '.kind', 'kind must be DAILY or TAPE.');
        }
        if (typeof measurement.measuredAt !== 'number' || !isFinite(measurement.measuredAt)) {
          add(problems, path + '.measuredAt', 'measuredAt must be epoch millis.');
        }
        ['weightGrams', 'bodyFatTenths', 'muscleTenths'].forEach(function (key) {
          var value = measurement[key];
          if (value !== null && value !== undefined &&
              (typeof value !== 'number' || !isFinite(value))) {
            add(problems, path + '.' + key, key + ' must be a number or null.');
          }
        });
        if (typeof measurement.weightGrams === 'number' && measurement.weightGrams <= 0) {
          add(problems, path + '.weightGrams', 'A weight must be positive.');
        }
      });
    }

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

  /**
   * The tool tabs (no roadmap id: the page's tools are independent, so one is on screen at a time).
   *
   * Import, Programs, Progress and Body each hold their own panels; a tool whose panel has no data
   * yet says so inside its tab rather than being hidden, so a reader can find it before loading
   * anything. The selection is mirrored in the URL hash (`#programs`), keyboard arrows move between
   * tabs, and an old section anchor (`#panel-editor`) still lands on the tab that owns it.
   */
  function initToolTabs() {
    var tabs = Array.prototype.slice.call(document.querySelectorAll('.tool-tab'));
    if (tabs.length === 0) { return; }
    var byTool = {};
    tabs.forEach(function (tab) { byTool[tab.getAttribute('data-tool')] = tab; });
    var aliases = {
      'panel-source': 'import', 'panel-sessions': 'import', 'panel-problems': 'import',
      'panel-export': 'import', 'panel-editor': 'programs', 'panel-program': 'programs',
      'panel-progress': 'progress', 'panel-body': 'body'
    };

    function select(tool, focus, scroll) {
      var target = byTool[tool] ? tool : 'import';
      tabs.forEach(function (tab) {
        var on = tab.getAttribute('data-tool') === target;
        tab.setAttribute('aria-selected', on ? 'true' : 'false');
        tab.tabIndex = on ? 0 : -1;
        var panel = byId(tab.getAttribute('aria-controls'));
        if (panel) {
          panel.hidden = !on;
          // Switching from a long tab leaves the reader scrolled past the new one, which reads
          // as a dead button. Show the top of the chosen tool instead.
          if (on && scroll && panel.scrollIntoView) {
            panel.scrollIntoView({ block: 'start' });
          }
        }
      });
      if (focus && byTool[target]) { byTool[target].focus(); }
      if (typeof history !== 'undefined' && history.replaceState) {
        // Some browsers refuse replaceState on a file:// origin; the tab still works.
        try {
          history.replaceState(null, '', '#' + target);
        } catch (ignored) {
          // no hash mirror, no harm
        }
      }
    }

    function fromHash() {
      var hash = String((typeof location !== 'undefined' && location.hash) || '').replace(/^#/, '');
      if (aliases[hash]) { return aliases[hash]; }
      return byTool[hash] ? hash : 'import';
    }

    tabs.forEach(function (tab) {
      tab.addEventListener('click', function () { select(tab.getAttribute('data-tool'), false, true); });
      tab.addEventListener('keydown', function (event) {
        var delta = event.key === 'ArrowRight' ? 1 : event.key === 'ArrowLeft' ? -1 : 0;
        if (delta === 0) { return; }
        event.preventDefault();
        var index = tabs.indexOf(tab);
        var next = tabs[(index + delta + tabs.length) % tabs.length];
        select(next.getAttribute('data-tool'), true, true);
      });
    });
    if (typeof window !== 'undefined') {
      window.addEventListener('hashchange', function () { select(fromHash()); });
    }
    ui.selectTool = select;
    select(fromHash(), false, false);
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
      /* The progress panel's chosen movement, and the list the picker offers (N165). */
      progressExerciseId: null,
      progressMovements: [],
      /* The rows' own charting file, rebuilt when the rows change (N173's progress fallback). */
      progressRowsFile: null,
      /* Waistline daily body data and the unit its numbers were entered in (N166). */
      waistline: null,
      waistlineUnit: 'kg',
      /* A program table being authored, and the document built from it (N167). */
      programSheet: null,
      /* The program editor's opened snapshot and working copy, or null (N172). */
      programEditor: null,
      programResult: null,
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
      progressFilter: byId('progressFilter'),
      progressRange: byId('progressRange'),
      progressSummary: byId('progressSummary'),
      progressChart: byId('progressChart'),
      progressList: byId('progressList'),
      programPanel: byId('panel-program'),
      programSummary: byId('programSummary'),
      programPreview: byId('programPreview'),
      programProblems: byId('programProblems'),
      downloadProgram: byId('downloadProgram'),
      programEditorHost: byId('programEditor'),
      editorStatus: byId('editorStatus'),
      editorPreview: byId('editorPreview'),
      editorUnavailable: byId('editorUnavailable'),
      newProgram: byId('newProgram'),
      programDropZone: byId('programDropZone'),
      programFileInput: byId('programFileInput'),
      bodyDropZone: byId('bodyDropZone'),
      bodyFileInput: byId('bodyFileInput'),
      bodyStatus: byId('bodyStatus'),
      previewProgramEdit: byId('previewProgramEdit'),
      downloadProgramEdit: byId('downloadProgramEdit'),
      saveProgramDraft: byId('saveProgramDraft'),
      bodyPanel: byId('panel-body'),
      bodySummary: byId('bodySummary'),
      bodyPreview: byId('bodyPreview'),
      waistlineUnit: byId('waistlineUnit'),
      clearBody: byId('clearBody'),
      exportHistory: byId('exportHistory'),
      exportHistorySummary: byId('exportHistorySummary'),
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
    elements.downloadProgram.addEventListener('click', downloadProgram);
    editorInit(elements);
    initToolTabs();
    elements.waistlineUnit.addEventListener('change', function () {
      state.waistlineUnit = elements.waistlineUnit.value === 'lb' ? 'lb' : 'kg';
      renderBodyPanel();
      refresh();
    });
    elements.clearBody.addEventListener('click', function () {
      state.waistline = null;
      renderBodyPanel();
      refresh();
      announce('Body data removed.');
    });
    elements.progressFilter.addEventListener('input', function () {
      renderProgressList(state.progressMovements || []);
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

    wireFileDrop(elements.bodyDropZone, elements.bodyFileInput, function (files) {
      readFile(files[0], 'body');
    });

    renderAll();
  }

  function readFile(file, purpose) {
    var reader = new FileReader();
    reader.onload = function () {
      var text = typeof reader.result === 'string' ? reader.result : '';
      if (purpose === 'export') {
        loadExportText(text, file.name);
      } else if (purpose === 'program') {
        // A program table is not JSON and opens the sheet; everything else is a document to open.
        var trimmed = text.trim();
        if (trimmed.charAt(0) === '{' || trimmed.charAt(0) === '[') {
          editorOpenText(text, file.name);
        } else {
          loadText(text, file.name);
        }
      } else if (purpose === 'body') {
        loadBodyText(text, file.name);
      } else {
        loadText(text, file.name);
      }
    };
    reader.onerror = function () { announce('That file could not be read.'); };
    reader.readAsText(file, 'UTF-8');
  }

  /**
   * Wires a tab's own drop zone and file input (Programs and Body, the tabs beside N172's editor).
   *
   * The Import source keeps its own multi-file reader: these two take one file each, because an edit
   * is one program and a Waistline backup is one body record.
   */
  function wireFileDrop(zone, input, onFiles) {
    if (!zone || !input) { return; }
    ['dragenter', 'dragover'].forEach(function (type) {
      zone.addEventListener(type, function (event) {
        event.preventDefault();
        zone.classList.add('is-over');
      });
    });
    ['dragleave', 'drop'].forEach(function (type) {
      zone.addEventListener(type, function (event) {
        event.preventDefault();
        zone.classList.remove('is-over');
      });
    });
    zone.addEventListener('drop', function (event) {
      var transfer = event.dataTransfer;
      if (transfer && transfer.files && transfer.files.length > 0) {
        onFiles(transfer.files);
      }
    });
    input.addEventListener('change', function () {
      if (input.files && input.files.length > 0) {
        onFiles(input.files);
      }
      input.value = '';
    });
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
    var sessions = Array.isArray(file.sessions) ? file.sessions.length : 0;
    if (sessions > 0) {
      text += ' It also carries ' + sessions + ' sessions for the Progress chart below.';
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

    if (parsed.kind === 'program') {
      state.programSheet = parsed.program;
      ui.elements.sourceStatus.textContent = 'That is a program sheet: ' +
        parsed.program.days.length + ' day' + (parsed.program.days.length === 1 ? '' : 's') +
        ' and ' + parsed.program.rows + ' exercise row' +
        (parsed.program.rows === 1 ? '' : 's') + '. It is a program document, not a history list.';
      announce('That is a program sheet. It will be written as a program document.');
      renderProgramPanel();
      // A program sheet is written in Programs; show the tab rather than leaving it behind Import.
      if (ui.selectTool) { ui.selectTool('programs', false, true); }
      refresh();
      return;
    }

    if (parsed.kind === 'waistline') {
      setWaistline(parsed.waistline, fileName);
      ui.elements.sourceStatus.textContent = 'That is a Waistline backup: ' +
        parsed.waistline.days.length + ' day' +
        (parsed.waistline.days.length === 1 ? '' : 's') +
        ' of daily body data. It is not a row list, so it was kept as body data.';
      announce('That is a Waistline backup. It was kept as body data, not rows.');
      return;
    }

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
    // A CSV dropped on Programs is read here but belongs on Import, which is where its rows are shown and
    // where this message lives — so go there, as a JSON rows list already does (B192's rule).
    if (ui.selectTool) { ui.selectTool('import', false, false); }
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
    state.progressRowsFile = null;
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
    renderProgramPanel();
    renderBodyPanel();
    renderDatalist();
    renderSessions();
    renderMapping();
    renderCombine();
    refresh();
    renderProgress();
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
      // Clicking a filled field selects its text, so the first keystroke replaces it rather
      // than appending to a name that is no longer wanted.
      targetInput.addEventListener('focus', function () { targetInput.select(); });
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
      var backup = state.catalogFile;
      var fromExport = backup && !backup.seeded &&
        Array.isArray(backup.sessions) && backup.sessions.length > 0;
      container.appendChild(h('p', 'empty', fromExport
        ? 'The app export is loaded: its ' + backup.sessions.length + ' sessions and ' +
          (Array.isArray(backup.exercises) ? backup.exercises.length : 0) +
          ' exercises feed the Progress chart below. This panel reviews the rows you want to import, ' +
          'so load them above to see them here.'
        : 'No rows loaded yet. Load JSON, CSV or TSV above, or press "Load the example".'));
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
      details.appendChild(body);

      var entry = {
        details: details,
        body: body,
        built: false,
        dateSpan: dateSpan,
        metaSpan: metaSpan,
        badge: badge,
        blocks: []
      };
      state.dom.sessions.push(entry);

      var build = function () {
        if (entry.built) { return; }
        buildSessionBody(entry, session, sessionIndex, dateFormat);
        entry.built = true;
      };
      entry.build = build;
      details.addEventListener('toggle', function () {
        if (details.open) {
          build();
          refreshSessions();
        }
      });
      if (details.open) { build(); }
      container.appendChild(details);
    });
  }

  /* Builds one session's controls the first time it is opened.
   *
   * The panel used to build every set's inputs at load: a 3,750-row import made about 127,000 DOM nodes
   * and froze the tab. A closed <details> needs nothing inside it, so the body is built on first open and
   * kept; the summary line always exists, because that is what the badge, the filter and the count read. */
  function buildSessionBody(entry, session, sessionIndex, dateFormat) {
    var body = entry.body;
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

      entry.dateInput = dateInput;
      entry.timeInput = timeInput;
      entry.durationInput = durationInput;
      entry.offsetInput = offsetInput;
      entry.offsetText = offsetText;
      entry.blocks = blockEntries;
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
    if (!session.built && session.build) {
      session.build();
      refreshSessions();
    }
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
   * a list of ninety lifts that would draw an empty chart.
   *
   * The rows are counted the way `liftTrendPoints` reads them (B179): a movement whose only
   * sets were deleted, or which only appears in a deleted or unfinished session, has no
   * history to chart, and offering it would answer with an empty plot. */
  function movementsWithHistory(backup) {
    var byId = {};
    (Array.isArray(backup.exercises) ? backup.exercises : []).forEach(function (exercise) {
      if (!isObject(exercise) || typeof exercise.id !== 'string') { return; }
      if (!isLoggableRow(exercise)) { return; }
      if (!isLive(exercise)) { return; }
      if (typeof exercise.name !== 'string' || exercise.name.trim() === '') { return; }
      byId[exercise.id] = exercise;
    });
    var finishedSessions = {};
    (Array.isArray(backup.sessions) ? backup.sessions : []).forEach(function (session) {
      if (!isObject(session) || typeof session.id !== 'string') { return; }
      if (!isLive(session)) { return; }
      if (typeof session.finishedAt !== 'number') { return; }
      finishedSessions[session.id] = true;
    });
    var exerciseOfLink = {};
    (Array.isArray(backup.sessionExercises) ? backup.sessionExercises : []).forEach(function (link) {
      if (!isObject(link) || typeof link.id !== 'string') { return; }
      if (!isLive(link)) { return; }
      if (!has(finishedSessions, link.sessionId)) { return; }
      exerciseOfLink[link.id] = link.exerciseId;
    });
    var setCount = {};
    (Array.isArray(backup.sets) ? backup.sets : []).forEach(function (set) {
      if (!isObject(set) || !isLive(set)) { return; }
      var id = exerciseOfLink[set.sessionExerciseId];
      if (id) { setCount[id] = (setCount[id] || 0) + 1; }
    });
    var list = Object.keys(byId)
      .map(function (id) { return { id: id, name: byId[id].name, sets: setCount[id] || 0 }; })
      .filter(function (row) { return row.sets > 0; });
    list.sort(function (a, b) { return compareExerciseNames(a.name, b.name); });
    return list;
  }

  /* A read-only reading of the loaded export: what is in it, newest first (N165).
   *
   * This is deliberately not the Sessions panel: that one is the editable row model, which drops the fields
   * a backup carries (readiness, template, superset, assistance, ratings). Nothing here is writable. */
  function exportSnapshot(backup) {
    var snapshot = {
      sessions: [], live: 0, deleted: 0, exercises: 0, sets: 0, first: null, last: null
    };
    if (!isObject(backup) || !Array.isArray(backup.sessions)) { return snapshot; }
    var exercisesById = {};
    (Array.isArray(backup.exercises) ? backup.exercises : []).forEach(function (exercise) {
      if (isObject(exercise) && typeof exercise.id === 'string') { exercisesById[exercise.id] = exercise; }
    });
    var links = {};
    (Array.isArray(backup.sessionExercises) ? backup.sessionExercises : []).forEach(function (link) {
      if (!isObject(link) || typeof link.id !== 'string') { return; }
      // A removed exercise's row is hidden like the sets that hung off it; showing its sets
      // under a live session would read as work that is still there (B179).
      if (!isLive(link)) { return; }
      links[link.id] = link;
    });
    var setsByLink = {};
    (Array.isArray(backup.sets) ? backup.sets : []).forEach(function (set) {
      if (!isObject(set) || !has(links, set.sessionExerciseId)) { return; }
      if (set.deletedAt !== null && set.deletedAt !== undefined) { return; }
      if (!has(setsByLink, set.sessionExerciseId)) { setsByLink[set.sessionExerciseId] = []; }
      setsByLink[set.sessionExerciseId].push(set);
    });
    var blocksBySession = {};
    Object.keys(links).forEach(function (id) {
      var link = links[id];
      var sets = setsByLink[id];
      if (!sets || sets.length === 0) { return; }
      if (!has(blocksBySession, link.sessionId)) { blocksBySession[link.sessionId] = []; }
      var exercise = exercisesById[link.exerciseId];
      blocksBySession[link.sessionId].push({
        name: exercise ? String(exercise.name) : String(link.exerciseId),
        position: typeof link.position === 'number' ? link.position : 0,
        sets: sets
      });
    });
    var live = [];
    backup.sessions.forEach(function (session) {
      if (!isObject(session) || typeof session.startedAt !== 'number') { return; }
      if (session.deletedAt !== null && session.deletedAt !== undefined) {
        snapshot.deleted += 1;
        return;
      }
      snapshot.live += 1;
      var blocks = (blocksBySession[session.id] || []).slice()
        .sort(function (a, b) { return a.position - b.position; });
      var tonnage = 0;
      blocks.forEach(function (block) {
        block.sets.forEach(function (set) {
          if (!roleIsWork(set.setType || 'NORMAL')) { return; }
          if (typeof set.weightGrams === 'number' && typeof set.reps === 'number') {
            tonnage += set.weightGrams * set.reps;
          }
        });
      });
      live.push({
        id: session.id,
        startedAt: session.startedAt,
        activity: session.activity || null,
        notes: session.notes || null,
        blocks: blocks,
        tonnage: tonnage
      });
    });
    live.sort(function (a, b) { return b.startedAt - a.startedAt; });
    snapshot.sessions = live;
    snapshot.exercises = Object.keys(exercisesById).length;
    snapshot.sets = (Array.isArray(backup.sets) ? backup.sets : []).length;
    if (live.length > 0) {
      snapshot.first = live[live.length - 1].startedAt;
      snapshot.last = live[0].startedAt;
    }
    return snapshot;
  }

  /* ------------------------------------------------------------------ *
   * Waistline body data (N166)
   * ------------------------------------------------------------------ */

  /* Waistline keeps a day's stats in `diary[].stats`, keyed by the names it shows ("weight",
   * "body fat"), and the file's own `settings` says which unit the weights were entered in.
   * Anything else (tape sites, custom stats) has no home in a DAILY row and is reported. */

  function waistlineExportedUnit(doc) {
    var settings = isObject(doc.settings) ? doc.settings : null;
    if (!settings) { return 'kg'; }
    var candidates = [settings.bodyStatsUnits, settings.units];
    for (var index = 0; index < candidates.length; index += 1) {
      var units = candidates[index];
      if (!isObject(units)) { continue; }
      var weight = units.weight;
      if (typeof weight !== 'string') { continue; }
      var name = normalizeName(weight);
      if (name === 'lb' || name === 'lbs' || name === 'pound' || name === 'pounds') { return 'lb'; }
      if (name === 'kg' || name === 'kgs' || name === 'kilogram' || name === 'kilograms') { return 'kg'; }
    }
    return 'kg';
  }

  /* A calendar day as local noon: noon is far enough from either midnight that a zone
   * change cannot move the weigh-in onto another day (N166). */
  function noonOfDate(dateText) {
    var match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(dateText || '').trim());
    if (!match) { return null; }
    var year = Number(match[1]);
    var month = Number(match[2]) - 1;
    var day = Number(match[3]);
    var date = new Date(year, month, day, 12, 0, 0, 0);
    if (date.getFullYear() !== year || date.getMonth() !== month || date.getDate() !== day) {
      return null;
    }
    return date.getTime();
  }

  /* A calendar day in the reader's own zone, as a comparison key (B178).
   *
   * The app's identity for a daily record is its local day and kind, not its id: two rows on one day are
   * the same day even when one was entered by hand and the other written by this page. The preview uses
   * the reader's zone, which is the phone's in the ordinary case of both being in one place; the app
   * itself always decides, and its own row wins. */
  function localDayKey(millis) {
    var date = new Date(millis);
    return date.getFullYear() + '-' + pad2(date.getMonth() + 1) + '-' + pad2(date.getDate());
  }

  function parseWaistlineDoc(doc) {
    var result = { days: [], ignored: [], problems: [], unit: 'kg', skippedDays: 0 };
    if (!isObject(doc) || !Array.isArray(doc.diary)) { return result; }
    result.unit = waistlineExportedUnit(doc);
    var ignored = {};
    doc.diary.forEach(function (entry, index) {
      if (!isObject(entry)) { return; }
      var dateText = typeof entry.date === 'string' ? entry.date.trim() : '';
      if (noonOfDate(dateText) === null) {
        result.problems.push({
          path: 'diary[' + index + '].date',
          message: 'The date "' + dateText + '" is not a real YYYY-MM-DD day.'
        });
        return;
      }
      if (!isObject(entry.stats)) { return; }
      var day = { date: dateText, weight: null, bodyFat: null, muscle: null };
      Object.keys(entry.stats).forEach(function (key) {
        var value = entry.stats[key];
        if (value === null || value === undefined || value === '') { return; }
        var name = normalizeName(key);
        var field = name === 'weight' || name === 'body weight' || name === 'bodyweight' ? 'weight'
          : name === 'body fat' || name === 'bodyfat' ? 'bodyFat'
            : name === 'muscle' || name === 'muscle mass' ? 'muscle'
              : null;
        if (field === null) {
          ignored[key] = true;
          return;
        }
        if (typeof value !== 'number' || !isFinite(value)) {
          result.problems.push({
            path: 'diary[' + index + '].stats.' + key,
            message: 'The value of "' + key + '" is not a number.'
          });
          return;
        }
        day[field] = value;
      });
      if (day.weight !== null || day.bodyFat !== null || day.muscle !== null) {
        result.days.push(day);
      } else if (Object.keys(entry.stats).length > 0) {
        // A day Waistline recorded only in stats this collection cannot hold, e.g. a tape day.
        result.skippedDays += 1;
      }
    });
    result.days.sort(function (a, b) { return a.date < b.date ? -1 : a.date > b.date ? 1 : 0; });
    result.ignored = Object.keys(ignored);
    return result;
  }

  function waistlineRows(waistline, unit) {
    var days = waistline && Array.isArray(waistline.days) ? waistline.days : [];
    return days.map(function (day) {
      return {
        date: day.date,
        weightGrams: day.weight === null ? null
          : Math.round(day.weight * (unit === 'lb' ? 453.59237 : 1000)),
        bodyFatTenths: day.bodyFat === null ? null : Math.round(day.bodyFat * 10),
        muscleTenths: day.muscle === null ? null : Math.round(day.muscle * 10)
      };
    });
  }

  /* The id is the day and the kind, the app's own identity for a measurement (N113), so a
   * second import of the same file adds nothing and restores a row the app hid. */
  function makeMeasurementId(dateText, kind) {
    return 'mes_' + fnv1a64Hex('waistline|' + dateText + '|' + kind);
  }

  function waistlineMeasurements(waistline, unit, exportedAt) {
    var time = typeof exportedAt === 'number' && isFinite(exportedAt) ? exportedAt : Date.now();
    return waistlineRows(waistline, unit).map(function (row) {
      return {
        id: makeMeasurementId(row.date, 'DAILY'),
        measuredAt: noonOfDate(row.date),
        kind: 'DAILY',
        weightGrams: row.weightGrams,
        bodyFatTenths: row.bodyFatTenths,
        muscleTenths: row.muscleTenths,
        neckMm: null,
        chestMm: null,
        waistMm: null,
        hipsMm: null,
        upperArmMm: null,
        thighMm: null,
        calfMm: null,
        createdAt: time,
        updatedAt: time,
        deletedAt: null
      };
    });
  }

  /* ------------------------------------------------------------------ *
   * Program sheets (N167)
   * ------------------------------------------------------------------ */

  var PROGRAM_WEEKDAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
  var PROGRAM_COLUMNS = {
    program: ['program'],
    day: ['day'],
    weekday: ['weekday', 'day of week', 'dow'],
    exercise: ['exercise', 'lift', 'movement'],
    sets: ['sets', 'set'],
    reps: ['reps', 'rep', 'reps range', 'rep range'],
    load: ['load', 'weight', 'target'],
    rest: ['rest', 'rest seconds', 'rest (s)'],
    rpe: ['rpe', 'effort'],
    note: ['note', 'notes', 'cue'],
    unit: ['unit', 'units'],
    // A block's shape, said once for the whole table rather than per row (N186).
    kind: ['kind', 'program kind', 'program type'],
    start: ['start', 'starts', 'start date', 'starts on']
  };

  function programFieldOf(header) {
    var name = normalizeName(header);
    var fields = Object.keys(PROGRAM_COLUMNS);
    for (var index = 0; index < fields.length; index += 1) {
      if (PROGRAM_COLUMNS[fields[index]].indexOf(name) !== -1) { return fields[index]; }
    }
    return null;
  }

  /* A program table, not a history one: it names days and set counts, and no date. */
  function looksLikeProgramTable(cells) {
    if (!Array.isArray(cells) || cells.length < 2 || !Array.isArray(cells[0])) { return false; }
    var fields = {};
    var dated = false;
    cells[0].forEach(function (header) {
      var name = normalizeName(header);
      if (name === 'date') { dated = true; }
      var field = programFieldOf(header);
      if (field) { fields[field] = true; }
    });
    return !dated && has(fields, 'day') && has(fields, 'exercise') && has(fields, 'sets');
  }

  function parseProgramWeekday(text) {
    var name = normalizeName(text).replace(/[^a-z0-9]/g, '');
    if (name === '') { return { ok: true, value: null }; }
    if (/^[1-7]$/.test(name)) { return { ok: true, value: PROGRAM_WEEKDAYS[Number(name) - 1] }; }
    if (name.length < 3) { return { ok: false }; }
    for (var index = 0; index < PROGRAM_WEEKDAYS.length; index += 1) {
      var full = PROGRAM_WEEKDAYS[index].toLowerCase();
      if (full.indexOf(name) === 0) { return { ok: true, value: PROGRAM_WEEKDAYS[index] }; }
    }
    return { ok: false };
  }

  function parseRepRange(text) {
    var value = String(text === null || text === undefined ? '' : text).trim().replace(/[\u2013\u2014]/g, '-');
    var match = /^(\d{1,3})\s*(?:-\s*(\d{1,3}))?$/.exec(value);
    if (!match) { return null; }
    var min = Number(match[1]);
    var max = match[2] === undefined ? min : Number(match[2]);
    if (min < 1 || max < min || max > 100) { return null; }
    return { min: min, max: max };
  }

  function parseProgramSheet(text, options) {
    options = options || {};
    var cells = parseDelimitedCells(text, detectDelimiter(text));
    while (cells.length > 0 && cells[cells.length - 1].every(function (cell) {
      return String(cell).trim() === '';
    })) {
      cells.pop();
    }
    var sheet = {
      name: 'My program',
      unit: 'kg',
      days: [],
      problems: [],
      warnings: [],
      rows: 0
    };
    if (cells.length < 2) {
      sheet.problems.push({ message: 'The table needs a header row and at least one exercise row.' });
      return sheet;
    }

    var headerFields = [];
    cells[0].forEach(function (header) {
      headerFields.push(programFieldOf(header));
    });
    ["kind", "start"].forEach(function (field) {
      if (headerFields.indexOf(field) === -1) { return; }
      sheet[field] = [];
    });
    ['day', 'exercise', 'sets'].forEach(function (field) {
      if (headerFields.indexOf(field) === -1) {
        sheet.problems.push({
          message: 'The table has no "' + (field === 'sets' ? 'Sets' : field === 'day' ? 'Day' : 'Exercise') +
            '" column. A program sheet needs Program (optional), Day, Weekday (optional), Exercise, Sets, ' +
            'Reps, Load, Rest, RPE and Note.'
        });
      }
    });
    if (sheet.problems.length > 0) { return sheet; }

    function collectProgramField(row, field) {
      if (!has(sheet, field)) { return; }
      var value = valueOf(row, field);
      if (value !== '') { sheet[field].push(value); }
    }

    function valueOf(row, field) {
      var index = headerFields.indexOf(field);
      if (index === -1) { return ''; }
      return String(index < row.length ? row[index] : '').trim();
    }

    var byDay = {};
    var dayOrder = [];
    for (var rowIndex = 1; rowIndex < cells.length; rowIndex += 1) {
      var row = cells[rowIndex];
      if (row.every(function (cell) { return String(cell).trim() === ''; })) { continue; }
      sheet.rows += 1;

      var program = valueOf(row, 'program');
      if (program !== '' && sheet.name === 'My program') { sheet.name = program; }
      var unit = normalizeName(valueOf(row, 'unit'));
      if (unit === 'lb' || unit === 'lbs') { sheet.unit = 'lb'; }

      var dayName = valueOf(row, 'day');
      if (dayName === '') {
        sheet.problems.push({ message: 'Row ' + (rowIndex + 1) + ' has no Day.' });
        continue;
      }
      if (!has(byDay, dayName)) {
        byDay[dayName] = { name: dayName, weekdays: [], exercises: [] };
        dayOrder.push(dayName);
      }
      var day = byDay[dayName];

      var weekday = valueOf(row, 'weekday');
      if (weekday !== '') {
        var parsedWeekday = parseProgramWeekday(weekday);
        if (!parsedWeekday.ok) {
          sheet.problems.push({ message: 'Row ' + (rowIndex + 1) + ': "' + weekday + '" is not a weekday.' });
        } else if (parsedWeekday.value !== null && day.weekdays.indexOf(parsedWeekday.value) === -1) {
          day.weekdays.push(parsedWeekday.value);
        }
      }

      collectProgramField(row, 'kind');
      collectProgramField(row, 'start');
      var exerciseName = valueOf(row, 'exercise');
      if (exerciseName === '') {
        sheet.problems.push({ message: 'Row ' + (rowIndex + 1) + ' has no Exercise.' });
        continue;
      }

      var setsText = valueOf(row, 'sets');
      var sets = Number(setsText);
      if (!/^\d+$/.test(setsText) || sets < 1 || sets > 20) {
        sheet.problems.push({
          message: 'Row ' + (rowIndex + 1) + ': Sets must be a whole number from 1 to 20.'
        });
        continue;
      }

      var reps = parseRepRange(valueOf(row, 'reps'));
      if (reps === null) {
        sheet.problems.push({
          message: 'Row ' + (rowIndex + 1) + ': Reps must be "5" or a range like "6-8".'
        });
        continue;
      }

      var loadText = valueOf(row, 'load');
      var load = null;
      if (loadText !== '') {
        load = Number(loadText.replace(',', '.'));
        if (!isFinite(load) || load <= 0) {
          sheet.problems.push({ message: 'Row ' + (rowIndex + 1) + ': Load must be a positive number.' });
          continue;
        }
      }
      var rowUnit = normalizeName(valueOf(row, 'unit'));
      var unitForRow = rowUnit === 'lb' || rowUnit === 'lbs' ? 'lb' : (rowUnit === 'kg' ? 'kg' : sheet.unit);

      var restText = valueOf(row, 'rest');
      var rest = null;
      if (restText !== '') {
        rest = Number(restText);
        if (!/^\d+$/.test(restText) || rest < 0 || rest > 3600) {
          sheet.problems.push({ message: 'Row ' + (rowIndex + 1) + ': Rest must be seconds, 0 to 3600.' });
          continue;
        }
      }

      var rpeText = valueOf(row, 'rpe');
      var rpeHalves = null;
      if (rpeText !== '') {
        var rpe = Number(rpeText.replace(',', '.'));
        if (!isFinite(rpe) || rpe < 1 || rpe > 10) {
          sheet.problems.push({ message: 'Row ' + (rowIndex + 1) + ': RPE must be from 1 to 10.' });
          continue;
        }
        rpeHalves = Math.round(rpe * 2);
      }

      day.exercises.push({
        name: exerciseName,
        row: rowIndex + 1,
        sets: sets,
        repsMin: reps.min,
        repsMax: reps.max,
        weightGrams: load === null ? null : Math.round(load * (unitForRow === 'lb' ? 453.59237 : 1000)),
        restSeconds: rest,
        rpeHalves: rpeHalves,
        note: valueOf(row, 'note') === '' ? null : valueOf(row, 'note')
      });
    }

    dayOrder.forEach(function (name) { sheet.days.push(byDay[name]); });
    if (sheet.rows === 0) {
      sheet.problems.push({ message: 'The table has headers but no exercise rows.' });
    }
    resolveProgramShape(sheet);
    return sheet;
  }

  /**
   * The table's program-level cells as one value each (ROADMAP N186).
   *
   * A program kind is a fact about the whole program, so a table that says two different things is
   * refused rather than resolved by whichever row came last. A start is a day, and is read the same
   * way: one date, or none — which leaves the app's creation-date fallback.
   */
  function resolveProgramShape(sheet) {
    if (has(sheet, 'kind')) {
      var kinds = sheet.kind.map(parseProgramKind);
      if (kinds.indexOf(null) !== -1) {
        sheet.problems.push({
          message: 'Kind must be "weekly" or "block" (or "one block").'
        });
        sheet.kind = 'WEEKLY';
      } else {
        var distinct = kinds.filter(function (value, index) { return kinds.indexOf(value) === index; });
        if (distinct.length > 1) {
          sheet.problems.push({ message: 'The table names two program kinds; use one.' });
        }
        sheet.kind = distinct[0];
      }
    }
    // A Start without Kind is two contradictory statements, and a weekly program's start is a field
    // nothing reads: refuse it rather than write it and hide it (B205).
    if (has(sheet, 'start') && sheet.kind !== 'SEQUENCE') {
      sheet.problems.push({ message: 'A Start needs Kind: block.' });
    }
    if (has(sheet, 'start')) {
      var days = sheet.start.map(parseProgramDay);
      if (days.indexOf(null) !== -1) {
        sheet.problems.push({ message: 'Start must be a date, like 2026-08-04.' });
        sheet.start = null;
      } else {
        var dates = days.filter(function (value, index) { return days.indexOf(value) === index; });
        if (dates.length > 1) {
          sheet.problems.push({ message: 'The table names two start dates; use one.' });
        }
        sheet.start = dates[0];
      }
    }
  }

  /** "Every week" or "one block" as a program kind, or null for a value the table cannot mean. */
  function parseProgramKind(text) {
    var name = normalizeName(text).replace(/[^a-z0-9]/g, '');
    if (name === 'weekly' || name === 'week' || name === 'everyweek' || name === 'repeating') {
      return 'WEEKLY';
    }
    if (name === 'block' || name === 'oneblock' || name === 'sequence' || name === 'finite') {
      return 'SEQUENCE';
    }
    return null;
  }

  /** An ISO date as an epoch day, or null when the cell is not one. */
  function parseProgramDay(text) {
    var match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(text).trim());
    if (!match) { return null; }
    var year = Number(match[1]);
    var month = Number(match[2]);
    var dayOfMonth = Number(match[3]);
    var ms = Date.UTC(year, month - 1, dayOfMonth);
    if (isNaN(ms)) { return null; }
    // `Date.UTC` normalises instead of refusing, so a well-shaped wrong day became a start its author
    // never typed: 2026-02-31 turned into 3 March, 2026-13-01 into January 2027, 0000-01-01 into 1900
    // (B204). The day is accepted only when it survives the round trip.
    var check = new Date(ms);
    if (check.getUTCFullYear() !== year || check.getUTCMonth() !== month - 1 ||
        check.getUTCDate() !== dayOfMonth) {
      return null;
    }
    return Math.round(ms / 86400000);
  }

  function programExerciseIndex(rows) {
    var byName = {};
    (Array.isArray(rows) ? rows : []).forEach(function (row) {
      if (!isObject(row) || typeof row.id !== 'string' || typeof row.name !== 'string') { return; }
      if (!isLoggableRow(row)) { return; }
      if (row.deletedAt !== null && row.deletedAt !== undefined) { return; }
      var key = normalizeName(row.name);
      if (key !== '' && !has(byName, key)) { byName[key] = row; }
    });
    return byName;
  }

  function programExerciseDto(row, time) {
    return {
      id: row.id,
      name: String(row.name),
      primaryMuscle: row.primaryMuscle || null,
      secondaryMuscles: Array.isArray(row.secondaryMuscles) ? row.secondaryMuscles : [],
      equipment: row.equipment || null,
      movementPattern: row.movementPattern || null,
      createdAt: typeof row.createdAt === 'number' ? row.createdAt : time,
      updatedAt: typeof row.updatedAt === 'number' ? row.updatedAt : time,
      deletedAt: null,
      rowKind: row.rowKind || 'MOVEMENT',
      parentId: row.parentId || null
    };
  }

  function buildProgramDocument(sheet, options) {
    options = options || {};
    var time = typeof options.exportedAt === 'number' && isFinite(options.exportedAt)
      ? options.exportedAt : Date.now();
    var index = programExerciseIndex(options.exercises);
    var problems = (sheet.problems || []).slice();

    var programName = String(sheet.name || 'My program').trim() || 'My program';
    var programId = 'pgm_' + fnv1a64Hex('program|' + programName);
    var templates = [];
    var slots = [];
    var templateExercises = [];
    var templateSets = [];
    var exercisesById = {};
    var newExercises = [];
    var templateByDay = {};
    var setCount = 0;
    var exerciseCount = 0;

    (sheet.days || []).forEach(function (day) {
      var templateId = 'tpl_' + fnv1a64Hex('template|' + programName + '|' + day.name);
      templateByDay[day.name] = templateId;
      templates.push({
        id: templateId,
        name: day.name,
        versionOf: null,
        createdAt: time,
        updatedAt: time,
        deletedAt: null
      });
      var weekdays = (day.weekdays || []).slice();
      if (weekdays.length === 0) { weekdays.push(null); }
      weekdays.forEach(function (weekday) {
        slots.push({
          id: 'slot_' + fnv1a64Hex('slot|' + programName + '|' + day.name + '|' + (weekday || 'none')),
          programId: programId,
          templateId: templateId,
          position: slots.length,
          weekday: weekday,
          createdAt: time,
          updatedAt: time,
          deletedAt: null
        });
      });

      day.exercises.forEach(function (exercise) {
        var normalized = normalizeName(exercise.name);
        var found = has(index, normalized) ? index[normalized] : null;
        var exerciseId;
        if (found) {
          exerciseId = found.id;
          exercisesById[exerciseId] = programExerciseDto(found, time);
        } else {
          exerciseId = 'ex_' + fnv1a64Hex('program-exercise|' + normalized);
          if (!has(exercisesById, exerciseId)) {
            exercisesById[exerciseId] = {
              id: exerciseId,
              name: String(exercise.name).trim(),
              primaryMuscle: null,
              secondaryMuscles: [],
              equipment: null,
              movementPattern: null,
              createdAt: time,
              updatedAt: time,
              deletedAt: null,
              rowKind: 'MOVEMENT',
              parentId: null
            };
            newExercises.push(String(exercise.name).trim());
          }
        }
        exerciseCount += 1;
        var templateExerciseId = 'tex_' + fnv1a64Hex(
          'template-exercise|' + programName + '|' + day.name + '|' + exercise.row
        );
        templateExercises.push({
          id: templateExerciseId,
          templateId: templateId,
          exerciseId: exerciseId,
          position: templateExercises.filter(function (row) {
            return row.templateId === templateId;
          }).length,
          restSeconds: exercise.restSeconds,
          techniqueNote: exercise.note,
          targetRpeHalves: exercise.rpeHalves,
          supersetGroup: null,
          createdAt: time,
          updatedAt: time,
          deletedAt: null
        });
        for (var setIndex = 1; setIndex <= exercise.sets; setIndex += 1) {
          setCount += 1;
          templateSets.push({
            id: 'tset_' + fnv1a64Hex(
              'template-set|' + programName + '|' + day.name + '|' + exercise.row + '|' + setIndex
            ),
            templateExerciseId: templateExerciseId,
            setIndex: setIndex,
            role: 'NORMAL',
            targetWeightGrams: exercise.weightGrams,
            targetAssistanceGrams: null,
            targetRepsMin: exercise.repsMin,
            targetRepsMax: exercise.repsMax,
            targetRepsCurrent: null,
            dropValueGrams: null,
            note: null,
            createdAt: time,
            updatedAt: time,
            deletedAt: null
          });
        }
      });
    });

    if (templates.length === 0 && problems.length === 0) {
      problems.push({ message: 'The sheet has no days to write.' });
    }

    var document = {
      formatVersion: programFormatVersion(),
      exportedAt: time,
      program: {
        id: programId,
        name: programName,
        isActive: false,
        position: 0,
        // Absent means weekly, which is what every table written before this column is (N186).
        // Type-checked rather than defaulted: a table that failed to parse still builds a preview,
        // and an array left in this field would be a kind no reader can name.
        kind: sheet.kind === 'SEQUENCE' ? 'SEQUENCE' : 'WEEKLY',
        startDate: programStartOf(sheet),
        createdAt: time,
        updatedAt: time,
        deletedAt: null
      },
      slots: slots,
      templates: templates,
      templateExercises: templateExercises,
      templateSets: templateSets,
      exercises: Object.keys(exercisesById).map(function (id) { return exercisesById[id]; })
    };

    return {
      ok: problems.length === 0,
      document: problems.length === 0 ? document : null,
      problems: problems,
      warnings: sheet.warnings || [],
      stats: {
        days: templates.length,
        exercises: exerciseCount,
        sets: setCount,
        newExercises: newExercises
      }
    };
  }

  /* The lifts that have history, as a list beside the chart. A list rather than a dropdown: the
   * chart is the point, and a visible list makes changing lift one click instead of a search. */
  function renderProgressList(movements) {
    var state = ui.state;
    var host = ui.elements.progressList;
    if (!host) { return; }
    host.textContent = '';
    var filter = normalizeName(ui.elements.progressFilter.value || '');
    var shown = 0;
    movements.forEach(function (movement) {
      if (filter !== '' && normalizeName(movement.name).indexOf(filter) === -1) { return; }
      shown += 1;
      var button = document.createElement('button');
      button.type = 'button';
      button.className = 'progress-lift';
      button.setAttribute('aria-pressed', movement.id === state.progressExerciseId ? 'true' : 'false');
      button.appendChild(h('span', 'progress-lift-name', movement.name));
      button.appendChild(h('span', 'progress-lift-count', movement.sets + ' sets'));
      button.addEventListener('click', function () {
        state.progressExerciseId = movement.id;
        renderProgress();
      });
      host.appendChild(button);
    });
    if (shown === 0) {
      host.appendChild(h('p', 'empty', 'No lift matches that filter.'));
    }
  }

  /**
   * What the progress chart reads (N165, extended).
   *
   * The chart was built for an app export, and read only that: a lifter who pasted a history file
   * saw every session in Import and an empty chart, because the rows were not the export. When no
   * export with history is loaded, the rows being imported are the same `BackupFile` shape — the
   * export the page is about to write — so they are charted instead, and the panel says which it is.
   */
  function progressSource() {
    var state = ui.state;
    var catalog = state.catalogFile;
    if (catalog && !catalog.seeded &&
        Array.isArray(catalog.sessions) && catalog.sessions.length > 0 &&
        Array.isArray(catalog.exercises) && catalog.exercises.length > 0) {
      return { backup: catalog, kind: 'export', blocked: false };
    }
    var built = state.exportResult;
    if (built && built.ok && built.file &&
        Array.isArray(built.file.sessions) && built.file.sessions.length > 0) {
      return { backup: built.file, kind: 'rows', blocked: false };
    }
    var rowsFile = progressRowsFile();
    if (rowsFile) {
      var blocked = !!(state.validation && state.validation.problems.length > 0) ||
        !!(built && !built.ok);
      return { backup: rowsFile, kind: 'rows', blocked: blocked };
    }
    return { backup: null, kind: null, blocked: false };
  }

  /**
   * The rows as a charting file, independent of the import's mapping gate.
   *
   * The export refuses an exercise name the loaded library does not have until the lifter maps it,
   * which is right for an import and wrong for a chart: the rows are already the record, and the
   * chart is a view of them. Building with no catalog turns every name into the exercise it already
   * is, so the panel has something to draw while the mapping is still being answered in Import.
   */
  function progressRowsFile() {
    var state = ui.state;
    if (!state.rows || state.rows.length === 0) { return null; }
    if (state.progressRowsFile) { return state.progressRowsFile; }
    var model = buildModel(state.rows, {
      seed: state.seed,
      sheet: state.sheet,
      source: state.source,
      sources: state.sources.map(function (item) {
        return { seed: item.seed, sheet: item.sheet, name: item.name };
      }),
      combineDates: state.combineDates,
      dateOrder: state.dateOrder,
      catalog: null,
      nameUnits: state.mapping.units,
      nameTargets: state.mapping.targets
    });
    var result = buildExport(model, { exportedAt: state.exportedAt });
    state.progressRowsFile = result.ok ? result.file : null;
    return state.progressRowsFile;
  }

  function renderProgress() {
    var state = ui.state;
    var panel = ui.elements.progressPanel;
    if (!panel) { return; }
    var source = progressSource();
    var backup = source.backup;
    var hasHistory = !!backup &&
      Array.isArray(backup.sessions) && backup.sessions.length > 0 &&
      Array.isArray(backup.exercises) && backup.exercises.length > 0;
    panel.hidden = !hasHistory;
    if (!hasHistory) { return; }

    setText(byId('progressSource'), source.kind === 'export'
      ? 'Charting the app export you loaded.'
      : source.blocked
        ? 'Charting the rows you are importing. The export is still blocked by the problems in Import; ' +
          'this chart does not need them mapped.'
        : 'Charting the rows you are importing.');

    var movements = movementsWithHistory(backup);
    state.progressMovements = movements;
    var heading = byId('export-history-heading');
    if (heading) {
      setText(heading, source.kind === 'export' ? 'In your export' : 'In these rows');
    }
    renderExportHistory(exportSnapshot(backup), source.kind);

    var selected = null;
    if (state.progressExerciseId) {
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
      renderProgressList(movements);
      setText(summary, source.kind === 'export'
        ? 'This export has no logged movements to chart.'
        : 'These rows have no logged movements to chart.');
      chart.textContent = '';
      return;
    }
    state.progressExerciseId = selected.id;
    renderProgressList(movements);

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
      // Two reasons the estimate can be missing, and naming the wrong one is a lie about the lift: a
      // bodyweight movement has no added weight to estimate from, which is not a set above twelve reps (N169).
      parts.push(heaviest
        ? 'no set at 12 reps or fewer, so no estimate'
        : 'no added weight, so no estimate');
    }
    return parts.join(' · ');
  }

  function formatChartDateLong(millis) {
    var date = new Date(millis);
    var months = ['January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'];
    return date.getDate() + ' ' + months[date.getMonth()] + ' ' + date.getFullYear();
  }

  /* What the export holds, newest first, with nothing to edit (N165). */
  function renderExportHistory(snapshot, kind) {
    var host = ui.elements.exportHistory;
    var summary = ui.elements.exportHistorySummary;
    if (!host || !summary) { return; }
    host.textContent = '';
    if (snapshot.live === 0) {
      setText(summary, kind === 'rows' ? 'No sessions in these rows.' : 'No sessions in this export.');
      return;
    }
    var text = formatInt(snapshot.live) + ' session' + (snapshot.live === 1 ? '' : 's') +
      ' from ' + formatChartDateLong(snapshot.first) + ' to ' +
      formatChartDateLong(snapshot.last) + ' · ' + formatInt(snapshot.exercises) +
      ' exercises · ' + formatInt(snapshot.sets) + ' set rows';
    if (snapshot.deleted > 0) {
      text += ' · ' + formatInt(snapshot.deleted) + ' deleted, hidden';
    }
    setText(summary, text + (kind === 'rows'
      ? '. These are the rows you are importing, not the app\'s record.'
      : '. Read-only: this is what the app stores, not a draft.'));

    snapshot.sessions.forEach(function (session) {
      var details = document.createElement('details');
      details.className = 'export-session';
      var head = document.createElement('summary');
      head.appendChild(h('span', 'export-date', formatChartDateLong(session.startedAt)));
      var meta = session.activity
        ? 'Activity: ' + String(session.activity).toLowerCase().replace(/_/g, ' ')
        : session.blocks.length + ' exercise' + (session.blocks.length === 1 ? '' : 's') +
          ' · ' + session.blocks.reduce(function (n, block) { return n + block.sets.length; }, 0) +
          ' sets · ' + formatKg(session.tonnage) + ' kg';
      head.appendChild(h('span', 'export-meta', meta));
      details.appendChild(head);
      var body = h('div', 'export-session-body');
      details.appendChild(body);
      details.addEventListener('toggle', function () {
        if (!details.open || body.childNodes.length > 0) { return; }
        buildExportSessionBody(body, session);
      });
      host.appendChild(details);
    });
  }

  function buildExportSessionBody(host, session) {
    if (session.notes) {
      host.appendChild(h('p', 'export-notes', session.notes));
    }
    if (session.blocks.length === 0) {
      host.appendChild(h('p', 'hint', session.activity
        ? 'An activity, not a workout: it has no sets.'
        : 'No sets.'));
      return;
    }
    var table = document.createElement('table');
    table.className = 'export-sets';
    var thead = document.createElement('thead');
    var headRow = document.createElement('tr');
    ['Exercise', 'Sets'].forEach(function (label) {
      var th = document.createElement('th');
      th.scope = 'col';
      th.textContent = label;
      headRow.appendChild(th);
    });
    thead.appendChild(headRow);
    table.appendChild(thead);
    var tbody = document.createElement('tbody');
    session.blocks.forEach(function (block) {
      var tr = document.createElement('tr');
      var name = document.createElement('td');
      name.textContent = block.name;
      tr.appendChild(name);
      var sets = document.createElement('td');
      block.sets.forEach(function (set, index) {
        if (index > 0) { sets.appendChild(h('span', 'set-sep', ' · ')); }
        sets.appendChild(h('span', 'export-set', exportSetText(set)));
      });
      tr.appendChild(sets);
      tbody.appendChild(tr);
    });
    table.appendChild(tbody);
    host.appendChild(table);
  }

  function exportSetText(set) {
    var role = set.setType || 'NORMAL';
    var prefix = role === 'WARMUP' ? 'W '
      : role === 'TOP_SET' ? 'T '
        : role === 'FAILURE' ? 'F '
          : role === 'DROP' ? 'D '
            : role === 'CLUSTER' ? 'C ' : '';
    var reps = typeof set.reps === 'number' ? set.reps : '?';
    var weight;
    if (typeof set.weightGrams === 'number' && set.weightGrams === 0 &&
        typeof set.assistanceGrams === 'number' && set.assistanceGrams > 0) {
      weight = 'assisted ' + formatKg(set.assistanceGrams) + ' kg';
    } else if (typeof set.weightGrams === 'number') {
      weight = formatKg(set.weightGrams) + ' kg';
    } else {
      weight = '—';
    }
    var text = prefix + reps + ' × ' + weight;
    if (typeof set.rpeHalves === 'number') { text += ' @' + (set.rpeHalves / 2); }
    return text;
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

  function programWeekdayLabel(weekday) {
    if (!weekday) { return 'any day'; }
    var lower = weekday.toLowerCase();
    return lower.charAt(0).toUpperCase() + lower.slice(1);
  }

  /* The program sheet's preview: the document as the phone will read it (N167). */
  function renderProgramPanel() {
    var state = ui.state;
    var panel = ui.elements.programPanel;
    if (!panel) { return; }
    var sheet = state.programSheet;
    panel.hidden = !sheet;
    if (!sheet) { return; }

    // The lifter's own library first, then the shipped one: a movement they renamed or created wins,
    // and a seeded slug is still found when their export was pruned.
    var exercises = [];
    if (state.catalogFile && Array.isArray(state.catalogFile.exercises)) {
      exercises = exercises.concat(state.catalogFile.exercises);
    }
    exercises = exercises.concat(window.SEEDED_EXERCISES || []);
    // The document's format version comes from the shared core (N183), so a page without it cannot
    // write a program table at all — say so rather than failing inside a render.
    if (!programCore()) {
      panel.textContent = '';
      panel.appendChild(h('p', 'refusal',
        'The program table cannot become a document: the shared core did not load. ' +
        'Run ./gradlew :shared:syncSharedJs and reload the page.'));
      return;
    }
    var built = buildProgramDocument(sheet, {
      exportedAt: state.exportedAt,
      exercises: exercises
    });
    state.programResult = built;
    var stats = built.stats;

    var isSequence = sheet.kind === 'SEQUENCE';
    var summary = programPanelSummary(sheet, stats);
    if (stats.newExercises.length > 0) {
      summary += ' Created as new movements: ' + stats.newExercises.join(', ') + '.';
    }
    setText(ui.elements.programSummary, summary);

    var host = ui.elements.programPreview;
    host.textContent = '';
    (sheet.days || []).forEach(function (day, index) {
      var block = h('section', 'program-day');
      var label = isSequence
        ? 'Day ' + (index + 1) + ' of ' + (sheet.days || []).length + ' · ' + day.name
        : day.name;
      var heading = h('h3', 'program-day-name', label);
      if (day.weekdays.length > 0) {
        heading.appendChild(h('span', 'hint',
          (isSequence ? ' · due ' : ' · ') + day.weekdays.map(programWeekdayLabel).join(', ')));
      } else {
        heading.appendChild(h('span', 'hint', ' · any day'));
      }
      block.appendChild(heading);

      var table = document.createElement('table');
      table.className = 'program-table';
      var thead = document.createElement('thead');
      var headRow = document.createElement('tr');
      ['Exercise', 'Sets', 'Reps', 'Load', 'Rest', 'RPE'].forEach(function (label) {
        var th = document.createElement('th');
        th.scope = 'col';
        th.textContent = label;
        headRow.appendChild(th);
      });
      thead.appendChild(headRow);
      table.appendChild(thead);
      var tbody = document.createElement('tbody');
      day.exercises.forEach(function (exercise) {
        var tr = document.createElement('tr');
        tr.appendChild(h('td', '', exercise.name));
        tr.appendChild(h('td', '', String(exercise.sets)));
        tr.appendChild(h('td', '', exercise.repsMin === exercise.repsMax
          ? String(exercise.repsMin)
          : exercise.repsMin + '–' + exercise.repsMax));
        tr.appendChild(h('td', '', exercise.weightGrams === null
          ? '—' : formatKg(exercise.weightGrams) + ' kg'));
        tr.appendChild(h('td', '', exercise.restSeconds === null
          ? '—' : exercise.restSeconds + ' s'));
        tr.appendChild(h('td', '', exercise.rpeHalves === null
          ? '—' : String(exercise.rpeHalves / 2)));
        tbody.appendChild(tr);
      });
      table.appendChild(tbody);
      block.appendChild(table);
      host.appendChild(block);
    });

    var problems = built.problems.map(function (problem) { return problem.message; });
    var notes = [];
    if (built.ok) {
      var clash = programClashNote(built.document, state.catalogFile);
      if (clash) { notes.push(clash); }
    }
    setText(ui.elements.programProblems, problems.length > 0
      ? 'Cannot write this sheet: ' + problems.join(' ')
      : notes.join(' '));
    ui.elements.downloadProgram.disabled = !built.ok;
  }

  /**
   * Offers [text] as a download named [name] — the page's one blob-and-anchor (N184).
   *
   * Three of these were hand-rolled, one per file the page writes, and a fix to one of them was a fix
   * to a third of the page. The object URL is revoked on the next turn of the clock rather than
   * immediately: a browser reads it while the click is still being handled.
   */
  function downloadText(text, name) {
    var blob = new Blob([text], { type: 'application/json' });
    var url = URL.createObjectURL(blob);
    var link = document.createElement('a');
    link.href = url;
    link.download = name;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.setTimeout(function () { URL.revokeObjectURL(url); }, 0);
  }

  function downloadProgram() {
    var state = ui.state;
    if (!state.programResult || !state.programResult.ok) { return; }
    downloadText(JSON.stringify(state.programResult.document, null, 2) + '\n', 'program.json');
    announce('Downloaded program.json.');
  }

  /* What the loaded export already holds under this document's program id, or null (B180).
   *
   * The id is the identity, not the name: the page derives it from the program's name, so loading a sheet
   * twice is a no-op and the app never merges a changed day into a program it already has. A *different*
   * program that happens to share the name is the one case that adds a second program. Saying which case
   * applies is the point — the note used to say "a second one" for all of them, and for a re-load that was
   * the opposite of what the merge does. */
  function programClashNote(document, catalogFile) {
    if (!isObject(document) || !isObject(document.program) || !isObject(catalogFile) ||
        !Array.isArray(catalogFile.programs)) {
      return null;
    }
    var id = document.program.id;
    var held = catalogFile.programs.filter(function (program) {
      return isObject(program) && program.id === id;
    });
    var hidden = held.some(function (program) {
      return program.deletedAt !== null && program.deletedAt !== undefined;
    });
    if (hidden) {
      return 'The loaded export holds this program as deleted; loading the file brings it back, without ' +
        're-following it.';
    }
    if (held.length > 0) {
      return 'The loaded export already holds this program; loading the file again adds nothing, and a ' +
        'changed sheet is changed in the app.';
    }
    var named = catalogFile.programs.filter(function (program) {
      return isObject(program) && program.id !== id &&
        (program.deletedAt === null || program.deletedAt === undefined) &&
        normalizeName(program.name) === normalizeName(document.program.name);
    });
    if (named.length > 0) {
      return 'The loaded export holds a different program named "' + document.program.name +
        '"; the import will add a second one, because it never renames or overwrites.';
    }
    return null;
  }

  /* The body-data panel: what Waistline contributed, before it is written (N166). */
  /**
   * A Waistline backup dropped on the Body tab.
   *
   * It goes through the same parser and `setWaistline` as the Import drop, but it does not touch the
   * history rows: a body file on the Body tab is body data, and anything else is refused rather than
   * imported by a control that promised one thing.
   */
  function loadBodyText(text, fileName) {
    var parsed = parseInputText(text, { fileName: fileName || '' });
    if (parsed.kind !== 'waistline') {
      setText(ui.elements.bodyStatus, 'That is not a Waistline backup.');
      announce('That is not a Waistline backup.');
      return;
    }
    setWaistline(parsed.waistline, fileName);
    var label = fileName || 'the backup';
    setText(ui.elements.bodyStatus, 'Loaded the daily body data from ' + label + '.');
    announce('Loaded the daily body data from ' + label + '.');
  }

  function setWaistline(waistline, fileName) {
    var state = ui.state;
    state.waistline = waistline;
    state.waistlineUnit = waistline && waistline.unit === 'lb' ? 'lb' : 'kg';
    state.waistlineFile = fileName || 'Waistline backup';
    if (ui.elements.waistlineUnit) {
      ui.elements.waistlineUnit.value = state.waistlineUnit;
    }
    renderBodyPanel();
    refresh();
  }

  function formatTenths(tenths) {
    return (tenths / 10).toFixed(1);
  }

  function renderBodyPanel() {
    var state = ui.state;
    var panel = ui.elements.bodyPanel;
    if (!panel) { return; }
    var waistline = state.waistline;
    var rows = waistlineRows(waistline, state.waistlineUnit);
    panel.hidden = rows.length === 0;
    if (rows.length === 0) { return; }

    var first = noonOfDate(rows[0].date);
    var last = noonOfDate(rows[rows.length - 1].date);
    var text = formatInt(rows.length) + ' day' + (rows.length === 1 ? '' : 's') +
      ' from ' + formatChartDateLong(first) + ' to ' + formatChartDateLong(last) +
      ' · weights in ' + state.waistlineUnit + '.';
    if (waistline.ignored.length > 0) {
      text += ' Waistline also holds ' + waistline.ignored.map(function (name) {
        return '"' + name + '"';
      }).join(', ') + '; the app stores only weight, body fat and muscle in a daily record, ' +
      'so those are left behind.';
    }
    if (waistline.skippedDays > 0) {
      text += ' ' + formatInt(waistline.skippedDays) + ' day' +
        (waistline.skippedDays === 1 ? '' : 's') +
        ' had only stats the app cannot store and were skipped.';
    }
    setText(ui.elements.bodySummary, text);

    var host = ui.elements.bodyPreview;
    host.textContent = '';

    var table = document.createElement('table');
    table.className = 'body-table';
    var thead = document.createElement('thead');
    var headRow = document.createElement('tr');
    ['Day', 'Weight', 'Body fat', 'Muscle'].forEach(function (label) {
      var th = document.createElement('th');
      th.scope = 'col';
      th.textContent = label;
      headRow.appendChild(th);
    });
    thead.appendChild(headRow);
    table.appendChild(thead);
    var tbody = document.createElement('tbody');
    rows.forEach(function (row) {
      var tr = document.createElement('tr');
      tr.appendChild(h('td', 'body-date', formatChartDateLong(noonOfDate(row.date))));
      tr.appendChild(h('td', '', row.weightGrams === null
        ? '—' : formatKg(row.weightGrams) + ' kg'));
      tr.appendChild(h('td', '', row.bodyFatTenths === null
        ? '—' : formatTenths(row.bodyFatTenths) + ' %'));
      tr.appendChild(h('td', '', row.muscleTenths === null
        ? '—' : formatTenths(row.muscleTenths) + ' %'));
      tbody.appendChild(tr);
    });
    table.appendChild(tbody);
    host.appendChild(table);

    if (waistline.problems.length > 0) {
      var list = document.createElement('ul');
      list.className = 'body-problems';
      waistline.problems.slice(0, 8).forEach(function (problem) {
        list.appendChild(h('li', '', problem.message));
      });
      host.appendChild(list);
    }
  }

  function renderExport() {
    var state = ui.state;
    var refusal = ui.elements.exportRefusal;
    var download = ui.elements.downloadExport;
    var copy = ui.elements.copyExport;
    var preview = ui.elements.exportText;

    var measurements = state.waistline
      ? waistlineMeasurements(state.waistline, state.waistlineUnit, state.exportedAt)
      : [];

    if (!state.model && measurements.length === 0) {
      setText(ui.elements.exportStats, 'Load rows or a Waistline backup to begin.');
      refusal.hidden = true;
      setText(refusal, '');
      download.disabled = true;
      copy.disabled = true;
      if (document.activeElement !== preview) { preview.value = ''; }
      state.exportResult = null;
      return;
    }

    // Body data alone still exports: an empty model is the same file with no sessions.
    var model = state.model || buildModel([], {
      seed: state.seed,
      sheet: state.sheet,
      source: state.source,
      sources: [],
      combineDates: state.combineDates,
      dateOrder: state.dateOrder,
      catalog: state.catalogFile,
      nameUnits: state.mapping.units,
      nameTargets: state.mapping.targets
    });

    var stats = computeExportStats(model, measurements);
    var statsText = 'Will add ' + stats.addedSessions + ' session' +
      (stats.addedSessions === 1 ? '' : 's') + ' and ' + stats.addedSets + ' set' +
      (stats.addedSets === 1 ? '' : 's') + '; ' + stats.skippedSessions + ' session' +
      (stats.skippedSessions === 1 ? '' : 's') +
      ' already exist and will be skipped; ' + stats.newExercises + ' new exercise' +
      (stats.newExercises === 1 ? '' : 's') + '.';
    if (stats.measurements > 0) {
      statsText += ' Body data: ' + stats.addedMeasurements + ' measurement' +
        (stats.addedMeasurements === 1 ? '' : 's') + ' will be added; ' +
        stats.skippedMeasurements + ' already exist and will be skipped.';
      if (stats.dayOverlaps > 0) {
        statsText += ' ' + stats.dayOverlaps + ' more fall on a day the app already records; the app keeps ' +
          'its own entry for that day.';
      }
    }
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

    var result = buildExport(model, {
      exportedAt: state.exportedAt,
      measurements: measurements
    });
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
    downloadText(state.exportResult.json, BACKUP_FILE_NAME);
    announce('Downloaded ' + BACKUP_FILE_NAME + '.');
  }

  /* ---------------------------------------------------------------------------------------------
   * The program editor (ROADMAP N172)
   *
   * Open a program the app exported, change it, and download an edit the app applies. The rules are
   * not re-implemented here: the edit is planned by the shared core (`docs/shared/`, N170), and the
   * preview is that plan's own change list — so the page cannot promise what the app would not do.
   *
   * The working copy is the document, not a table: every field the app can hold travels, including
   * the ones this UI does not show, because a round trip that drops one is a silent edit. New rows
   * mint their own ids and timestamps; existing rows keep theirs, which is what the app's staleness
   * check reads.
   * ------------------------------------------------------------------------------------------- */

  var EDITOR_WEEKDAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
  var EDITOR_ROLES = ['NORMAL', 'WARMUP', 'TOP_SET', 'DROP', 'CLUSTER'];
  var EDITOR_DRAFT_KIND = 'workout-program-draft';
  var editorIdCounter = 0;

  /** The browser's globals, or the test harness's (this file's pure half runs under node too). */
  function editorGlobal() {
    if (typeof window !== 'undefined') { return window; }
    if (typeof globalThis !== 'undefined') { return globalThis; }
    return {};
  }

  /** The shared core's JS door, or null when docs/shared was not built (N170, N172). */
  function programCore() {
    var mod = editorGlobal()['workout:shared'];
    var ns = mod && mod.com && mod.com.example && mod.com.example.androidapp;
    return ns && ns.shared ? ns.shared : null;
  }

  /**
   * The document format the page writes, read from the core rather than declared here (N183).
   *
   * The core exports it for exactly this reason: a version in two places is the first thing to
   * disagree, and the page would then stamp a new program with the version it remembered while the
   * app refuses it. A page whose core did not load cannot write a document at all, and says so
   * rather than guessing a number.
   */
  function programFormatVersion() {
    var core = programCore();
    if (!core) {
      throw new Error('the shared core is not loaded; run ./gradlew :shared:syncSharedJs');
    }
    return core.programFormatVersion();
  }

  /**
   * A blank program, as the document the app adds (the same shape the sheet writes, N172).
   *
   * It has no `edit` marker: a new program is a proposal to add, and the app's insert-only merge is
   * what puts it in the list. The id is minted here and carried by every download and draft, so
   * re-importing one file is a no-op rather than a second program.
   */
  function newProgramDocument(name, id, time) {
    var now = typeof time === 'number' ? time : Date.now();
    return {
      formatVersion: programFormatVersion(),
      exportedAt: now,
      program: {
        id: typeof id === 'string' && id !== '' ? id : editorNewId('pgm'),
        name: typeof name === 'string' && name.trim() !== '' ? name : 'My program',
        isActive: false,
        position: 0,
        createdAt: now,
        updatedAt: now,
        deletedAt: null
      },
      slots: [],
      templates: [],
      templateExercises: [],
      templateSets: [],
      exercises: []
    };
  }

  function editorNewId(prefix) {
    editorIdCounter += 1;
    return prefix + '_' + Date.now().toString(36) + '_' + editorIdCounter.toString(36) + '_' +
      Math.random().toString(36).slice(2, 8);
  }

  function editorDeepCopy(value) {
    return JSON.parse(JSON.stringify(value));
  }

  function editorRung(role) {
    return role === 'DROP' || role === 'CLUSTER';
  }

  function editorNextPosition(rows) {
    var max = -1;
    (rows || []).forEach(function (row) {
      if (row && typeof row.position === 'number' && row.position > max) { max = row.position; }
    });
    return max + 1;
  }

  function editorNextSetIndex(rows) {
    var max = -1;
    (rows || []).forEach(function (row) {
      if (row && typeof row.setIndex === 'number' && row.setIndex > max) { max = row.setIndex; }
    });
    return max + 1;
  }

  function editorFind(rows, id) {
    var found = null;
    (rows || []).forEach(function (row) { if (row && row.id === id) { found = row; } });
    return found;
  }

  function editorKg(grams) {
    var kg = Math.round(grams) / 1000;
    return String(kg);
  }

  /** The app's plan writes assistance as `-20` in the load field; this is that field's text. */
  function editorLoadText(weightGrams, assistanceGrams) {
    if (typeof assistanceGrams === 'number' && assistanceGrams > 0) {
      return '-' + editorKg(assistanceGrams);
    }
    if (typeof weightGrams !== 'number') { return ''; }
    return editorKg(weightGrams);
  }

  /** `-20` is assistance, `20` is weight, blank is no load; anything else is null. */
  function editorParseLoad(text) {
    var trimmed = String(text === null || text === undefined ? '' : text).trim();
    if (trimmed === '') { return { weightGrams: null, assistanceGrams: null }; }
    var assisted = trimmed.charAt(0) === '-';
    var number = Number((assisted ? trimmed.slice(1) : trimmed).replace(',', '.'));
    if (!isFinite(number) || number < 0) { return null; }
    var grams = Math.round(number * 1000);
    return assisted
      ? { weightGrams: 0, assistanceGrams: grams }
      : { weightGrams: grams, assistanceGrams: null };
  }

  function editorParseInt(text) {
    var trimmed = String(text === null || text === undefined ? '' : text).trim();
    if (trimmed === '') { return null; }
    if (!/^\d+$/.test(trimmed)) { return null; }
    return Number(trimmed);
  }

  /** A stored epoch day as the date input's value, which is always `yyyy-mm-dd` in UTC. */
  function editorDayText(epochDay) {
    if (typeof epochDay !== 'number' || !isFinite(epochDay)) { return ''; }
    var day = new Date(epochDay * 86400000);
    // A millis-valued or huge start would make toISOString throw out of the file reader and leave the
    // editor half built, with no message (B204): a value this page cannot render is no value.
    if (isNaN(day.getTime())) { return ''; }
    return day.toISOString().slice(0, 10);
  }

  /** A date input's value as an epoch day, or null for a cleared field. */
  function editorParseDay(text) {
    if (!text) { return null; }
    var match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(text).trim());
    if (!match) { return null; }
    var ms = Date.UTC(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
    if (isNaN(ms)) { return null; }
    var day = new Date(ms);
    // The same round trip the table's parser keeps (B204): a date input only emits real days, but the
    // function is reachable with anything, and a rolled-over day is a day nobody chose.
    if (day.getUTCFullYear() !== Number(match[1]) || day.getUTCMonth() !== Number(match[2]) - 1 ||
        day.getUTCDate() !== Number(match[3])) {
      return null;
    }
    return Math.round(ms / 86400000);
  }

  /** An epoch day as the sentence a preview says, in the page's own month names. */
  function editorDayLabel(epochDay) {
    var day = new Date(Number(epochDay) * 86400000);
    if (isNaN(day.getTime())) { return String(epochDay); }
    return day.getUTCDate() + ' ' + MONTH_NAMES[day.getUTCMonth()] + ' ' + day.getUTCFullYear();
  }

  function editorParseHalves(text) {
    var trimmed = String(text === null || text === undefined ? '' : text).trim();
    if (trimmed === '') { return null; }
    var rpe = Number(trimmed.replace(',', '.'));
    if (!isFinite(rpe) || rpe < 1 || rpe > 10) { return null; }
    return Math.round(rpe * 2);
  }

  /**
   * One program's document out of a backup (N172).
   *
   * The app's own export is the supported path, but a lifter who has an export and no program file
   * should not have to make one on the phone first: the backup already carries every row. The
   * returned document is the same shape `ProgramDocumentCodec` writes, with the exercises the plan
   * names and their ancestors, and no history.
   */
  function programDocumentFromBackup(backup, programId) {
    if (!isObject(backup) || !Array.isArray(backup.programs)) { return null; }
    var program = editorFind(backup.programs, programId);
    if (!program || program.deletedAt) { return null; }

    var slots = (backup.programSlots || []).filter(function (slot) {
      return slot && slot.programId === programId && !slot.deletedAt;
    });
    var templateIds = {};
    slots.forEach(function (slot) { templateIds[slot.templateId] = true; });

    var templates = (backup.templates || []).filter(function (template) {
      return template && templateIds[template.id] && !template.deletedAt;
    });
    var exerciseRowIds = {};
    var templateExercises = (backup.templateExercises || []).filter(function (planned) {
      if (!planned || planned.deletedAt || !templateIds[planned.templateId]) { return false; }
      exerciseRowIds[planned.id] = true;
      return true;
    });
    var templateSets = (backup.templateSets || []).filter(function (set) {
      return set && !set.deletedAt && exerciseRowIds[set.templateExerciseId];
    });

    var byId = {};
    (backup.exercises || []).forEach(function (row) { if (row && row.id) { byId[row.id] = row; } });
    var carried = {};
    templateExercises.forEach(function (planned) {
      var row = byId[planned.exerciseId];
      if (!row) { return; }
      carried[row.id] = row;
      var guard = 0;
      var parent = byId[row.parentId];
      while (parent && guard < 32) {
        carried[parent.id] = parent;
        parent = byId[parent.parentId];
        guard += 1;
      }
    });

    return {
      formatVersion: programFormatVersion(),
      exportedAt: typeof backup.exportedAt === 'number' ? backup.exportedAt : Date.now(),
      program: editorDeepCopy(program),
      slots: editorDeepCopy(slots),
      templates: editorDeepCopy(templates),
      templateExercises: editorDeepCopy(templateExercises),
      templateSets: editorDeepCopy(templateSets),
      exercises: Object.keys(carried).map(function (id) { return editorDeepCopy(carried[id]); })
    };
  }

  /** Resolves a typed name to an id, creating or carrying a movement when it is new (N47). */
  /**
   * The lifter's own movements, when the page holds them: the export that was loaded (B187).
   *
   * A program document carries only the movements its plan names, so on its own it cannot answer
   * "do I already have this movement?" — the loaded export can.
   */
  function editorLibraryExercises() {
    var file = ui.state && ui.state.catalogFile;
    return file && Array.isArray(file.exercises) ? file.exercises : [];
  }

  /**
   * A name resolves to a movement the page can see, or mints one (B187).
   *
   * The document comes first, then the lifter's library, then the app's seed library: any of the
   * three is that movement rather than a second row with the same name. [libraryRows] is only for
   * callers that hold the library themselves — the page's own tests — and defaults to the loaded
   * export.
   */
  function editorResolveExercise(working, name, libraryRows) {
    var trimmed = String(name === null || name === undefined ? '' : name).trim();
    var normalized = normalizeName(trimmed);
    if (normalized === '') { return null; }
    function match(rows) {
      return (rows || []).filter(function (row) {
        return row && isLoggableRow(row) && !row.deletedAt && normalizeName(row.name) === normalized;
      })[0];
    }
    var known = match(working.exercises);
    if (known) { return known.id; }

    var library = typeof libraryRows === 'undefined' ? editorLibraryExercises() : libraryRows;
    var owned = match(library);
    if (owned) {
      working.exercises.push(editorDeepCopy(owned));
      return owned.id;
    }

    var seeded = match(editorGlobal().SEEDED_EXERCISES);
    if (seeded) {
      working.exercises.push(programExerciseDto(seeded, Date.now()));
      return seeded.id;
    }

    var now = Date.now();
    var id = editorNewId('ex');
    working.exercises.push({
      id: id,
      name: trimmed,
      primaryMuscle: null,
      secondaryMuscles: [],
      equipment: null,
      movementPattern: null,
      createdAt: now,
      updatedAt: now,
      deletedAt: null,
      restSeconds: null,
      techniqueNote: null,
      weightUnit: null,
      stepGrams: null,
      parentId: null,
      rowKind: 'MOVEMENT'
    });
    return id;
  }

  /**
   * Keeps the movements the document still needs: the ones its plan names, and their ancestors (B187).
   *
   * A name typed into a field mints a movement when nothing matches it, so a typo corrected afterwards
   * would leave the minted row in the document — carried into the edit and created in the app's
   * library, where the page has no way to remove it. Ancestors stay because a movement's family is
   * part of its definition and is not itself named by a plan (N103).
   */
  function editorPruneExercises(working) {
    var needed = {};
    (working.templateExercises || []).forEach(function (planned) {
      if (planned && planned.exerciseId) { needed[planned.exerciseId] = true; }
    });
    var byId = {};
    (working.exercises || []).forEach(function (row) {
      if (row && row.id) { byId[row.id] = row; }
    });
    var pending = Object.keys(needed);
    while (pending.length > 0) {
      var row = byId[pending.pop()];
      if (row && row.parentId && !needed[row.parentId]) {
        needed[row.parentId] = true;
        pending.push(row.parentId);
      }
    }
    working.exercises = (working.exercises || []).filter(function (row) {
      return row && row.id && needed[row.id];
    });
  }

  function editorAddDay(working) {
    var now = Date.now();
    var template = {
      id: editorNewId('tpl'),
      name: 'New day',
      versionOf: null,
      createdAt: now,
      updatedAt: now,
      deletedAt: null
    };
    working.templates.push(template);
    working.slots.push({
      id: editorNewId('slot'),
      programId: working.program.id,
      templateId: template.id,
      position: editorNextPosition(working.slots),
      weekday: null,
      createdAt: now,
      updatedAt: now,
      deletedAt: null
    });
  }

  function editorAddExercise(working, templateId, exerciseId) {
    var now = Date.now();
    var inTemplate = (working.templateExercises || []).filter(function (row) {
      return row && row.templateId === templateId;
    });
    working.templateExercises.push({
      id: editorNewId('tex'),
      templateId: templateId,
      exerciseId: exerciseId,
      position: editorNextPosition(inTemplate),
      restSeconds: null,
      techniqueNote: null,
      targetRpeHalves: null,
      supersetGroup: null,
      createdAt: now,
      updatedAt: now,
      deletedAt: null
    });
  }

  function editorAddSet(working, templateExerciseId) {
    var rows = (working.templateSets || []).filter(function (row) {
      return row && row.templateExerciseId === templateExerciseId;
    }).sort(function (a, b) { return a.setIndex - b.setIndex; });
    var last = rows.length > 0 ? rows[rows.length - 1] : null;
    var carriesTargets = last && !editorRung(last.role);
    var now = Date.now();
    working.templateSets.push({
      id: editorNewId('tset'),
      templateExerciseId: templateExerciseId,
      setIndex: editorNextSetIndex(rows),
      role: 'NORMAL',
      targetWeightGrams: carriesTargets ? last.targetWeightGrams : null,
      targetAssistanceGrams: carriesTargets ? last.targetAssistanceGrams : null,
      targetRepsMin: carriesTargets ? last.targetRepsMin : null,
      targetRepsMax: carriesTargets ? last.targetRepsMax : null,
      targetRepsCurrent: null,
      dropValueGrams: null,
      note: null,
      createdAt: now,
      updatedAt: now,
      deletedAt: null
    });
  }

  function editorRemoveDay(working, slot) {
    working.slots = working.slots.filter(function (row) { return row !== slot; });
    var stillUsed = working.slots.some(function (row) { return row.templateId === slot.templateId; });
    if (stillUsed) { return; }
    // A workout no other day uses leaves the document with its plan; the app keeps the row in its
    // library, which is the app's own removal rule and the plan's WORKOUT_KEPT warning.
    var exercises = (working.templateExercises || []).filter(function (row) {
      return row && row.templateId === slot.templateId;
    });
    var exerciseIds = {};
    exercises.forEach(function (row) { exerciseIds[row.id] = true; });
    working.templateExercises = working.templateExercises.filter(function (row) {
      return !exerciseIds[row.id];
    });
    working.templateSets = working.templateSets.filter(function (row) {
      return !exerciseIds[row.templateExerciseId];
    });
    working.templates = working.templates.filter(function (row) { return row.id !== slot.templateId; });
  }

  function editorRemoveExercise(working, planned) {
    working.templateSets = working.templateSets.filter(function (row) {
      return row.templateExerciseId !== planned.id;
    });
    working.templateExercises = working.templateExercises.filter(function (row) {
      return row !== planned;
    });
  }

  function editorMove(rows, row, delta) {
    var ordered = rows.slice().sort(function (a, b) { return a.position - b.position; });
    var index = ordered.indexOf(row);
    var swap = ordered[index + delta];
    if (!swap) { return; }
    var at = row.position;
    row.position = swap.position;
    swap.position = at;
  }

  function editorSharedProgramNames(working, catalogFile) {
    if (!isObject(catalogFile) || !Array.isArray(catalogFile.programSlots) || !Array.isArray(catalogFile.programs)) {
      return [];
    }
    var templateIds = {};
    (working.templates || []).forEach(function (row) { templateIds[row.id] = true; });
    var names = {};
    catalogFile.programSlots.forEach(function (slot) {
      if (!slot || slot.deletedAt || slot.programId === working.program.id || !templateIds[slot.templateId]) {
        return;
      }
      var program = editorFind(catalogFile.programs, slot.programId);
      if (program && !program.deletedAt) { names[program.name] = true; }
    });
    return Object.keys(names).sort();
  }

  /* --- the DOM ------------------------------------------------------------------------------- */

  function editorMessage(text) {
    setText(ui.elements.editorStatus, text);
  }

  /**
   * A button in the editor (B188).
   *
   * [key] is how a re-render finds this control again, so focus can return to it; [ariaLabel] is for
   * the controls whose visible text is a glyph or repeats on every row.
   */
  function editorButton(label, onClick, key, ariaLabel) {
    var button = h('button', 'secondary', label);
    button.type = 'button';
    if (key) { button.setAttribute('data-editor-key', key); }
    if (ariaLabel) { button.setAttribute('aria-label', ariaLabel); }
    button.addEventListener('click', onClick);
    return button;
  }

  function editorTextField(label, value, onInput, options) {
    options = options || {};
    var wrap = h('label', 'editor-field');
    wrap.appendChild(h('span', 'editor-field-label', label));
    var input = document.createElement('input');
    input.type = options.type || 'text';
    if (options.step) { input.step = options.step; }
    if (options.list) { input.setAttribute('list', options.list); }
    if (options.placeholder) { input.placeholder = options.placeholder; }
    input.value = value === null || value === undefined ? '' : String(value);
    input.addEventListener(options.event || 'input', function () { onInput(input.value); });
    wrap.appendChild(input);
    return wrap;
  }

  function editorSelect(label, values, value, onChange, key) {
    var wrap = h('label', 'editor-field');
    wrap.appendChild(h('span', 'editor-field-label', label));
    var select = document.createElement('select');
    if (key) { select.setAttribute('data-editor-key', key); }
    values.forEach(function (option) {
      var node = document.createElement('option');
      node.value = option.value;
      node.textContent = option.label;
      select.appendChild(node);
    });
    select.value = value === null || value === undefined ? '' : String(value);
    select.addEventListener('change', function () { onChange(select.value); });
    wrap.appendChild(select);
    return wrap;
  }

  function editorRenderExerciseNames(host, working) {
    var list = document.createElement('datalist');
    list.id = 'editorExerciseNames';
    var seen = {};
    (working.exercises || []).forEach(function (row) {
      if (!row || !isLoggableRow(row) || seen[normalizeName(row.name)]) { return; }
      seen[normalizeName(row.name)] = true;
      var option = document.createElement('option');
      option.value = row.name;
      list.appendChild(option);
    });
    function offer(rows) {
      (rows || []).forEach(function (row) {
        if (!row || !isLoggableRow(row) || row.deletedAt || seen[normalizeName(row.name)]) { return; }
        seen[normalizeName(row.name)] = true;
        var option = document.createElement('option');
        option.value = row.name;
        list.appendChild(option);
      });
    }
    offer(editorLibraryExercises());
    offer(editorGlobal().SEEDED_EXERCISES);
    host.appendChild(list);
  }

  /**
   * The control that had focus, and the re-render that gives it back (B188).
   *
   * The editor has no diffing: a re-render rebuilds every node, so the focused button is destroyed
   * and focus falls to the body. The key the action buttons carry is what finds the same control
   * afterwards — and when the action removed it (a deleted row, the last set), the region's heading
   * is where a keyboard user resumes.
   */
  function editorFocusedKey() {
    var active = document.activeElement;
    return active && active.getAttribute ? active.getAttribute('data-editor-key') : null;
  }

  function editorRestoreFocus(key) {
    if (!key) { return; }
    var found = ui.elements.programEditorHost.querySelector('[data-editor-key="' + key + '"]');
    if (found && typeof found.focus === 'function') {
      found.focus();
      return;
    }
    var heading = document.getElementById('editor-heading');
    if (heading && typeof heading.focus === 'function') {
      heading.setAttribute('tabindex', '-1');
      heading.focus();
    }
  }

  function editorRender() {
    var focusKey = editorFocusedKey();
    editorRenderBody();
    editorRestoreFocus(focusKey);
  }

  function editorRenderBody() {
    var ed = ui.state.programEditor;
    var host = ui.elements.programEditorHost;
    if (!host) { return; }
    host.textContent = '';
    var has = ed && ed.working;
    var isNew = has && ed.mode === 'new';
    ui.elements.previewProgramEdit.disabled = !has;
    ui.elements.downloadProgramEdit.disabled = true;
    ui.elements.saveProgramDraft.disabled = !has;
    setText(ui.elements.previewProgramEdit, isNew ? 'Check the program' : 'Preview changes');
    setText(ui.elements.downloadProgramEdit, isNew ? 'Download program' : 'Download edit');
    if (!has) {
      editorMessage('Open a program the app exported, start a new one, or open a draft.');
      return;
    }
    var working = ed.working;
    setText(
      ui.elements.editorStatus,
      isNew
        ? 'Building "' + (working.program.name || 'a program') + '" from scratch. The app adds it when ' +
          'you load the document.'
        : 'Editing "' + (working.program.name || 'a program') + '" from ' + (ed.fileName || 'a file') +
          '. Nothing is written until the app applies the edit.'
    );

    host.appendChild(
      editorTextField('Program name', working.program.name, function (value) {
        working.program.name = value;
      })
    );
    // A program's shape is part of the document (N186): a week that repeats, or a block done once.
    // The start is the anchor a block's first due date is read from, and a weekly program has no
    // use for one, so the field is only offered for a block.
    host.appendChild(editorSelect('Program kind', [
      { value: 'WEEKLY', label: 'Every week' },
      { value: 'SEQUENCE', label: 'One block' }
    ], working.program.kind || 'WEEKLY', function (value) {
      editorSetKind(working, value);
      editorRender();
    }, 'root:kind'));
    if ((working.program.kind || 'WEEKLY') === 'SEQUENCE') {
      host.appendChild(editorTextField('Starts on', editorDayText(working.program.startDate), function (value) {
        working.program.startDate = editorParseDay(value);
      }, { type: 'date' }));
    }
    editorRenderExerciseNames(host, working);

    var days = (working.slots || []).slice().sort(function (a, b) { return a.position - b.position; });
    days.forEach(function (slot) { host.appendChild(editorDayNode(working, slot)); });

    var addDay = h('div', 'editor-add');
    addDay.appendChild(editorButton('Add a day', function () {
      editorAddDay(working);
      editorRender();
    }, 'add-day'));
    host.appendChild(addDay);
  }

  /**
   * A program's kind, and the start that only a block has (B205).
   *
   * Switching a block back to "every week" kept its start date, hid the field that could have cleared
   * it, and reported no start change — so the app kept a date no screen could show.
   */
  function editorSetKind(working, kind) {
    working.program.kind = kind;
    if (kind !== 'SEQUENCE') { working.program.startDate = null; }
  }

  function editorDayNode(working, slot) {
    var template = editorFind(working.templates, slot.templateId);
    var isSequence = (working.program.kind || 'WEEKLY') === 'SEQUENCE';
    var orderedSlots = working.slots.slice().sort(function (a, b) { return a.position - b.position; });
    var section = h('section', 'editor-day');
    var head = h('div', 'editor-day-head');
    var title = template ? template.name : 'A workout this file does not carry';
    // A block's day is its place in the order, and its weekday is a placement rather than a
    // recurrence (N174): "Day 4 of 9" and a control labelled Due say what the day means here (B203).
    if (isSequence) {
      title = 'Day ' + (orderedSlots.indexOf(slot) + 1) + ' of ' + orderedSlots.length + ' · ' + title;
    }
    head.appendChild(h('h3', '', title));

    var weekdayOptions = [{ value: '', label: isSequence ? 'Any day' : 'Any day' }].concat(
      EDITOR_WEEKDAYS.map(function (day) { return { value: day, label: programWeekdayLabel(day) }; })
    );
    head.appendChild(editorSelect(isSequence ? 'Due' : 'Day', weekdayOptions, slot.weekday || '', function (value) {
      slot.weekday = value === '' ? null : value;
    }));

    var dayName = template ? 'day "' + template.name + '"' : 'this day';
    head.appendChild(editorButton(
      '\u2191',
      function () {
        editorMove(orderedSlots, slot, -1);
        editorRender();
      },
      'move-day-up:' + slot.id,
      'Move ' + dayName + ' up'
    ));
    head.appendChild(editorButton(
      '\u2193',
      function () {
        editorMove(orderedSlots, slot, 1);
        editorRender();
      },
      'move-day-down:' + slot.id,
      'Move ' + dayName + ' down'
    ));
    head.appendChild(editorButton(
      'Remove day',
      function () {
        editorRemoveDay(working, slot);
        editorRender();
      },
      'remove-day:' + slot.id,
      'Remove ' + dayName
    ));
    section.appendChild(head);

    if (template) {
      section.appendChild(
        editorTextField('Workout name', template.name, function (value) {
          template.name = value;
        })
      );
      var planned = (working.templateExercises || []).filter(function (row) {
        return row && row.templateId === template.id;
      }).sort(function (a, b) { return a.position - b.position; });
      planned.forEach(function (row) {
        section.appendChild(editorExerciseNode(working, template, row));
      });
      var add = h('div', 'editor-add');
      var nameInput = document.createElement('input');
      nameInput.type = 'text';
      nameInput.setAttribute('list', 'editorExerciseNames');
      nameInput.setAttribute('aria-label', 'Movement to add to ' + (template ? template.name : 'this day'));
      nameInput.placeholder = 'Add an exercise';
      add.appendChild(nameInput);
      add.appendChild(editorButton('Add exercise', function () {
        var exerciseId = editorResolveExercise(working, nameInput.value);
        if (!exerciseId) { editorMessage('Type a movement to add.'); return; }
        editorAddExercise(working, template.id, exerciseId);
        editorRender();
      }));
      section.appendChild(add);
    }

    return section;
  }

  function editorExerciseNode(working, template, planned) {
    var exercise = editorFind(working.exercises, planned.exerciseId);
    var node = h('div', 'editor-exercise');
    var head = h('div', 'editor-exercise-head');
    var nameField = editorTextField(
      'Exercise',
      exercise ? exercise.name : planned.exerciseId,
      function (value) {
        var exerciseId = editorResolveExercise(working, value);
        if (exerciseId) { planned.exerciseId = exerciseId; }
      },
      // On change, not on every keystroke: a half-typed name is not a new movement.
      { list: 'editorExerciseNames', event: 'change' }
    );
    head.appendChild(nameField);
    head.appendChild(editorTextField('Rest (s)', planned.restSeconds, function (value) {
      planned.restSeconds = editorParseInt(value);
    }, { type: 'number', step: '1' }));
    head.appendChild(editorTextField('RPE', typeof planned.targetRpeHalves === 'number' ? planned.targetRpeHalves / 2 : '', function (value) {
      planned.targetRpeHalves = editorParseHalves(value);
    }, { type: 'number', step: '0.5' }));
    head.appendChild(editorTextField('Superset', planned.supersetGroup, function (value) {
      planned.supersetGroup = editorParseInt(value);
    }, { type: 'number', step: '1' }));
    var exerciseName = exercise ? exercise.name : planned.exerciseId;
    head.appendChild(editorButton(
      '\u2191',
      function () {
        editorMove(
          working.templateExercises.filter(function (row) { return row.templateId === template.id; }),
          planned,
          -1
        );
        editorRender();
      },
      'move-exercise-up:' + planned.id,
      'Move ' + exerciseName + ' up'
    ));
    head.appendChild(editorButton(
      '\u2193',
      function () {
        editorMove(
          working.templateExercises.filter(function (row) { return row.templateId === template.id; }),
          planned,
          1
        );
        editorRender();
      },
      'move-exercise-down:' + planned.id,
      'Move ' + exerciseName + ' down'
    ));
    head.appendChild(editorButton(
      'Remove',
      function () {
        editorRemoveExercise(working, planned);
        editorRender();
      },
      'remove-exercise:' + planned.id,
      'Remove ' + exerciseName
    ));
    node.appendChild(head);

    var note = editorTextField('Cue', planned.techniqueNote || '', function (value) {
      planned.techniqueNote = value.trim() === '' ? null : value;
    });
    note.className = 'editor-field-wide';
    node.appendChild(note);

    var sets = (working.templateSets || []).filter(function (row) {
      return row && row.templateExerciseId === planned.id;
    }).sort(function (a, b) { return a.setIndex - b.setIndex; });
    sets.forEach(function (set) { node.appendChild(editorSetNode(working, set)); });

    var add = h('div', 'editor-add');
    add.appendChild(editorButton(
      'Add set',
      function () {
        editorAddSet(working, planned.id);
        editorRender();
      },
      'add-set:' + planned.id,
      'Add a set to ' + exerciseName
    ));
    node.appendChild(add);
    return node;
  }

  /**
   * A set's fields follow its role (B190).
   *
   * A rung takes its load and reps from the anchor above, so a number on the row is a value nothing
   * reads — N79's rule, and the shared validation refuses it. A drop is the only role that names a
   * value to take off, so a cluster and a working set carry none. The change clears what the new role
   * cannot carry and remembers what it cleared, because switching to a rung and changing your mind
   * must not cost the load and reps the row already had.
   */
  function editorApplyRole(set) {
    if (editorRung(set.role)) {
      if (!set.editorCarried) {
        // Non-enumerable: it is the form's memory, not part of the document the app receives.
        Object.defineProperty(set, 'editorCarried', {
          value: {
            targetWeightGrams: set.targetWeightGrams,
            targetAssistanceGrams: set.targetAssistanceGrams,
            targetRepsMin: set.targetRepsMin,
            targetRepsMax: set.targetRepsMax,
            targetRepsCurrent: set.targetRepsCurrent
          },
          writable: true,
          configurable: true,
          enumerable: false
        });
      }
      set.targetWeightGrams = null;
      set.targetAssistanceGrams = null;
      set.targetRepsMin = null;
      set.targetRepsMax = null;
      set.targetRepsCurrent = null;
    } else if (set.editorCarried) {
      var carried = set.editorCarried;
      set.targetWeightGrams = carried.targetWeightGrams;
      set.targetAssistanceGrams = carried.targetAssistanceGrams;
      set.targetRepsMin = carried.targetRepsMin;
      set.targetRepsMax = carried.targetRepsMax;
      set.targetRepsCurrent = carried.targetRepsCurrent;
      delete set.editorCarried;
    }
    if (set.role !== 'DROP') { set.dropValueGrams = null; }
  }

  function editorSetNode(working, set) {
    var row = h('div', 'editor-set');
    row.appendChild(editorSelect('Role', EDITOR_ROLES.map(function (role) {
      return { value: role, label: roleLabel(role) };
    }), set.role, function (value) {
      set.role = value;
      editorApplyRole(set);
      editorRender();
    }, 'role:' + set.id));

    row.appendChild(editorTextField('Set', set.setIndex, function (value) {
      var parsed = editorParseInt(value);
      if (parsed !== null) { set.setIndex = parsed; }
    }, { type: 'number', step: '1' }));

    if (!editorRung(set.role)) {
      row.appendChild(editorTextField('Reps from', set.targetRepsMin, function (value) {
        set.targetRepsMin = editorParseInt(value);
      }, { type: 'number', step: '1' }));
      row.appendChild(editorTextField('Reps to', set.targetRepsMax, function (value) {
        set.targetRepsMax = editorParseInt(value);
      }, { type: 'number', step: '1' }));
      row.appendChild(editorTextField('At', set.targetRepsCurrent, function (value) {
        set.targetRepsCurrent = editorParseInt(value);
      }, { type: 'number', step: '1' }));
      row.appendChild(editorTextField('Load (kg, -assist)', editorLoadText(set.targetWeightGrams, set.targetAssistanceGrams), function (value) {
        var load = editorParseLoad(value);
        if (load) {
          set.targetWeightGrams = load.weightGrams;
          set.targetAssistanceGrams = load.assistanceGrams;
        }
      }));
    } else {
      row.appendChild(h('span', 'hint', 'Load and reps come from the set above.'));
    }

    if (set.role === 'DROP') {
      row.appendChild(editorTextField('Drop (kg)', typeof set.dropValueGrams === 'number' ? editorKg(set.dropValueGrams) : '', function (value) {
        var load = editorParseLoad(value);
        set.dropValueGrams = load && load.weightGrams !== null ? load.weightGrams : null;
      }));
    }

    row.appendChild(editorTextField('Note', set.note || '', function (value) {
      set.note = value.trim() === '' ? null : value;
    }));
    row.appendChild(editorButton(
      'Remove',
      function () {
        working.templateSets = working.templateSets.filter(function (candidate) { return candidate !== set; });
        editorRender();
      },
      'remove-set:' + set.id,
      'Remove set ' + (typeof set.setIndex === 'number' ? set.setIndex + 1 : '')
    ));
    return row;
  }

  function editorProblemText(problem) {
    var messages = {
      STALE_BASE: 'This program changed on the phone since this export. Export it again and redo the edit.',
      PROGRAM_MISSING: 'The app does not have this program.',
      PROGRAM_MISMATCH: 'This edit names a different program from the one it was made from.',
      NO_EDIT_MARKER: 'This file does not say which export it came from.',
      INVALID_SET_INDEX: 'Two sets in one exercise share an index.',
      RUNG_WITHOUT_ANCHOR: 'A drop or cluster set needs a set above it.',
      RUNG_UNDER_WARMUP: 'A warm-up cannot be the set a drop or cluster run comes off.',
      RUNG_WITHOUT_VALUE: 'The first drop of a run needs its drop value.',
      RUN_MIXES_KINDS: 'A run of drops or clusters cannot change kind part-way.',
      RUNG_CARRIES_TARGETS: 'A drop or cluster set takes its load and reps from the set above it.',
      INVALID_REPS: 'A rep target is not usable.',
      INVALID_LOAD: 'A load is not usable.',
      INVALID_RPE: 'An effort target must be between 1 and 10.',
      INVALID_REST: 'Rest must be 0 to 3600 seconds.',
      INVALID_POSITION: 'A row has no place in its order.',
      DANGLING_SLOT_TEMPLATE: 'A day names a workout this file does not carry.',
      DANGLING_TEMPLATE_EXERCISE: 'An exercise names a workout this file does not carry.',
      DANGLING_SET: 'A set names an exercise this file does not carry.',
      DANGLING_EXERCISE: 'A plan names a movement this file does not carry.',
      DUPLICATE_ID: 'Two rows share an id.',
      EMPTY_NAME: 'A name is empty.',
      MALFORMED: 'The file is not shaped like a program.',
      NEWER_VERSION: 'The file is newer than this page.',
      OLDER_VERSION: 'The file is older than this page.',
      MISSING_VERSION: 'The file names no version.'
    };
    var message = messages[problem.code] || ('The app refused the edit (' + problem.code + ').');
    return problem.path ? message + ' (' + problem.path + ')' : message;
  }

  function editorChangeText(change) {
    var labels = {
      PROGRAM_RENAMED: 'Rename the program',
      PROGRAM_KIND: 'Change the program kind',
      PROGRAM_START: 'Change when the block starts',
      SLOT_ADDED: 'Add a day',
      SLOT_UPDATED: 'Change a day',
      SLOT_REMOVED: 'Remove a day',
      TEMPLATE_ADDED: 'Add a workout',
      TEMPLATE_UPDATED: 'Change a workout',
      TEMPLATE_EXERCISE_ADDED: 'Add an exercise',
      TEMPLATE_EXERCISE_UPDATED: 'Change an exercise',
      TEMPLATE_EXERCISE_REMOVED: 'Remove an exercise',
      SET_ADDED: 'Add a set',
      SET_UPDATED: 'Change a set',
      SET_REMOVED: 'Remove a set',
      EXERCISE_ADDED: 'Add a movement'
    };
    // A block's shape reads as what it becomes rather than which field moved (N186): the detail is
    // the new value, and a cleared start is its own sentence rather than a missing one.
    if (change.kind === 'PROGRAM_KIND') {
      return change.detail === 'SEQUENCE' ? 'Make the program a block' : 'Make the program weekly';
    }
    if (change.kind === 'PROGRAM_START') {
      return change.detail
        ? 'Start the block on ' + editorDayLabel(change.detail)
        : 'Start the block the day it was created';
    }
    var label = labels[change.kind] || change.kind;
    return change.name ? label + ': ' + change.name : label;
  }

  /**
   * Leaves the editor with nothing open (B184).
   *
   * Every path that refuses a file goes through here, because the previous program's working copy,
   * its preview and the enabled Download are state, not screen: a failure that only rewrote the status
   * line left the old edit one click from being downloaded under the new file's name.
   */
  function editorClear(message) {
    ui.state.programEditor = null;
    ui.elements.editorPreview.textContent = '';
    editorRender();
    if (message) { editorMessage(message); }
  }

  function editorOpenText(text, fileName) {
    var parsed;
    try {
      parsed = JSON.parse(text);
    } catch (ignored) {
      editorClear('That does not look like JSON.');
      return;
    }
    if (isObject(parsed) && parsed.kind === EDITOR_DRAFT_KIND) {
      editorLoadDraft(parsed, fileName);
      return;
    }
    if (isObject(parsed) && typeof parsed.formatVersion === 'number' && isObject(parsed.program)) {
      if (isObject(parsed.edit)) {
        editorClear('That is an edit, not a program. Open the program\'s export, or a draft.');
        return;
      }
      editorLoadDocument(parsed, fileName);
      return;
    }
    if (isObject(parsed) && Array.isArray(parsed.programs)) {
      editorOfferBackup(parsed, fileName);
      return;
    }
    // A rows list is not a program document, and the answer is where it belongs rather than what it
    // is not (B192). Anything else keeps a Programs-side message, now pointing at the right tab.
    if (Array.isArray(parsed) || (isObject(parsed) && Array.isArray(parsed.rows))) {
      editorClear('');
      loadText(text, fileName);
      if (ui.selectTool) { ui.selectTool('import', false, true); }
      return;
    }
    editorClear('That is not a program document or a backup. A history or rows file belongs on Import.');
  }

  /**
   * What an opened document becomes: the snapshot it was opened from, the copy being edited, and the
   * marker the app reads (N171).
   *
   * Separate from [editorLoadDocument] because this is the whole of the state and a test can call it
   * with no DOM (N181). The marker names the export it was opened from, so an edit of a program this
   * device changed after the export is refused rather than merged.
   */
  function editorOpenSnapshot(document, source) {
    var original = editorDeepCopy(document);
    var working = editorDeepCopy(document);
    working.edit = { baseExportedAt: original.exportedAt };
    // The working copy is written at the format this build reads (B198): opening a v4 export and
    // turning it into a block would otherwise download a v4 document carrying a kind, which an old
    // build accepts (its floor is 4) and reads as a weekly program — the hazard N185 closed.
    working.formatVersion = programFormatVersion();
    return {
      original: original,
      working: working,
      fileName: '',
      // A program taken from a backup keeps it: the other programs that share its workouts are a fact
      // only the backup holds (B191). A plain export carries one program and nothing to compare.
      source: isObject(source) ? source : null,
      mode: 'edit',
      preview: null
    };
  }

  function editorLoadDocument(document, fileName, source) {
    var opened = editorOpenSnapshot(document, source);
    opened.fileName = fileName || '';
    ui.state.programEditor = opened;
    ui.elements.editorPreview.textContent = '';
    editorRender();
  }

  /**
   * What the core would do with the working copy — the answer the preview shows and the download
   * re-plans, so the two cannot disagree (B185).
   *
   * Returns the plan, or `{ error }` when the core could not be asked; a test calls this without a
   * DOM (N181).
   */
  function editorPlanEdit(core, ed) {
    if (!core || !ed || !ed.working) { return { error: 'no program is open' }; }
    // What is planned is what is written, so a movement nothing names leaves before the plan (B187).
    editorPruneExercises(ed.working);
    if (ed.mode === 'new') { return { error: 'a new program is checked, not planned' }; }
    try {
      return JSON.parse(core.planJson(JSON.stringify(ed.original), JSON.stringify(ed.working)));
    } catch (problem) {
      return { error: String(problem) };
    }
  }

  /** The file a download writes: the working copy as it stands, and the name it gets (B185). */
  /**
   * The start a table's document carries: only a block's, and only a real day (B205).
   *
   * A weekly program's start is a field nothing reads, and a table that names one without a kind is
   * refused by the resolver; this is the belt that keeps such a value out of the document anyway.
   */
  function programStartOf(sheet) {
    return sheet.kind === 'SEQUENCE' && typeof sheet.start === 'number' ? sheet.start : null;
  }

  /**
   * What the program table's preview says about the table (B203).
   *
   * Its kind and, for a block, its start, beside the counts: a table authored as a block used to
   * preview identically to a weekly program, so a start a year off was invisible until the app
   * refused the file or the block was already overdue.
   */
  function programPanelSummary(sheet, stats) {
    var isSequence = sheet.kind === 'SEQUENCE';
    return 'Program "' + (sheet.name || 'My program') + '" · ' + formatInt(stats.days) +
      ' day' + (stats.days === 1 ? '' : 's') + ' · ' + formatInt(stats.exercises) +
      ' exercise row' + (stats.exercises === 1 ? '' : 's') + ' · ' + formatInt(stats.sets) +
      ' set' + (stats.sets === 1 ? '' : 's') + ' · loads in ' + sheet.unit + ' · ' +
      (isSequence ? 'one block' : 'every week') +
      (isSequence && typeof sheet.start === 'number'
        ? ', starting ' + editorDayLabel(sheet.start)
        : '') + '.';
  }

  /**
   * A plan's changes as one line: one count and one sentence per kind (B202).
   *
   * The sentence comes from a representative change rather than from its kind alone, because the
   * kind and start sentences read the change's `detail`: passing only the kind made an edit that
   * turns a program into a block say "make the program weekly" above the itemized list that said the
   * opposite.
   */
  function editorSummary(changes) {
    var counts = {};
    var representative = {};
    (changes || []).forEach(function (change) {
      counts[change.kind] = (counts[change.kind] || 0) + 1;
      if (!representative[change.kind]) { representative[change.kind] = change; }
    });
    return Object.keys(counts).map(function (kind) {
      return counts[kind] + ' \u00d7 ' + editorChangeText(representative[kind]).toLowerCase();
    });
  }

  function editorDownloadPayload(ed) {
    return {
      text: JSON.stringify(ed.working, null, 2) + '\n',
      fileName: ed.mode === 'new' ? 'program.json' : 'program-edit.json'
    };
  }

  /** A blank program in the editor: no snapshot, no edit marker, validated and downloaded (N172). */
  function editorStartNew() {
    ui.state.programEditor = {
      original: null,
      working: newProgramDocument('My program'),
      fileName: 'a new program',
      source: null,
      mode: 'new',
      preview: null
    };
    ui.elements.editorPreview.textContent = '';
    editorRender();
  }

  function editorLoadDraft(draft, fileName) {
    if (!isObject(draft.working)) {
      editorClear('That draft is missing its program.');
      return;
    }
    var mode = draft.mode === 'new' ? 'new' : 'edit';
    if (mode === 'edit' && !isObject(draft.original)) {
      editorClear('That draft is missing the export it was made from.');
      return;
    }
    var working = editorDeepCopy(draft.working);
    // A draft saved by an older build is upgraded on the way in, for the same reason an opened
    // document is (B198).
    working.formatVersion = programFormatVersion();
    if (mode === 'edit') {
      working.edit = { baseExportedAt: draft.original.exportedAt };
    } else {
      delete working.edit;
    }
    ui.state.programEditor = {
      original: mode === 'edit' ? editorDeepCopy(draft.original) : null,
      working: working,
      fileName: fileName || '',
      source: isObject(draft.source) ? draft.source : null,
      mode: mode,
      preview: null
    };
    ui.elements.editorPreview.textContent = '';
    editorRender();
  }

  function editorOfferBackup(backup, fileName) {
    var programs = (backup.programs || []).filter(function (row) { return row && !row.deletedAt; });
    if (programs.length === 0) {
      editorClear('That backup holds no programs.');
      return;
    }
    // Nothing is open until a program is chosen: the question must not sit above the previous
    // program's form with its Download still enabled (B184).
    editorClear('');
    var host = ui.elements.programEditorHost;
    host.appendChild(h('p', 'hint', 'That is a backup. Choose the program to edit:'));
    var select = document.createElement('select');
    programs.forEach(function (program) {
      var option = document.createElement('option');
      option.value = program.id;
      option.textContent = program.name;
      select.appendChild(option);
    });
    host.appendChild(select);
    host.appendChild(editorButton('Edit this program', function () {
      var document = programDocumentFromBackup(backup, select.value);
      if (!document) {
        editorClear('That program cannot be read from this backup.');
        return;
      }
      editorLoadDocument(document, fileName, backup);
    }));
    editorMessage('A program taken from a backup carries the time the backup was made; the app refuses the edit if the program changed after that.');
  }

  /**
   * The new-program half of the check button (N172): validate, not plan.
   *
   * There is no export to plan an edit against, so the shared core's `validate` is the whole
   * question. The app's insert-only merge adds the document; the page only promises what it holds.
   */
  function editorCheckNew(core, ed) {
    var checked;
    try {
      checked = JSON.parse(core.validateJson(JSON.stringify(ed.working)));
    } catch (problem) {
      ed.preview = null;
      ui.elements.downloadProgramEdit.disabled = true;
      editorMessage('The shared core could not check this program: ' + problem);
      return;
    }
    var problems = checked.problems || [];
    ed.preview = { ok: problems.length === 0, problems: problems, changes: [], warnings: [] };
    var host = ui.elements.editorPreview;
    host.textContent = '';
    ui.elements.downloadProgramEdit.disabled = problems.length > 0;
    if (problems.length > 0) {
      host.appendChild(h('p', 'refusal', 'The app would refuse this program:'));
      var list = document.createElement('ul');
      problems.forEach(function (problem) {
        list.appendChild(h('li', '', editorProblemText(problem)));
      });
      host.appendChild(list);
      editorMessage('The program is not ready; fix the problems above.');
      return;
    }
    var days = (ed.working.slots || []).length;
    var workouts = (ed.working.templates || []).length;
    var exercises = (ed.working.templateExercises || []).length;
    var sets = (ed.working.templateSets || []).length;
    host.appendChild(h('p', '', 'The program is ready: ' + days + ' day' + (days === 1 ? '' : 's') +
      ', ' + workouts + ' workout' + (workouts === 1 ? '' : 's') + ', ' + exercises + ' exercise' +
      (exercises === 1 ? '' : 's') + ', ' + sets + ' set' + (sets === 1 ? '' : 's') + '.'));
    host.appendChild(h('p', 'hint', 'Programs → Load adds it as a new program; it is not an edit of anything.'));
    editorMessage('Ready: download the program and load it in Programs → Load.');
  }

  function editorPreview() {
    var core = programCore();
    var ed = ui.state.programEditor;
    if (!core || !ed || !ed.working) { return; }
    // What is planned is what is written, so a movement nothing names leaves before the plan (B187).
    editorPruneExercises(ed.working);
    if (ed.mode === 'new') {
      editorCheckNew(core, ed);
      return;
    }
    var planned = editorPlanEdit(core, ed);
    if (planned.error) {
      ed.preview = null;
      ui.elements.downloadProgramEdit.disabled = true;
      editorMessage('The shared core could not plan this edit: ' + planned.error);
      return;
    }
    ed.preview = planned;
    ui.elements.downloadProgramEdit.disabled = !planned.ok;
    var host = ui.elements.editorPreview;
    host.textContent = '';
    if (!planned.ok) {
      host.appendChild(h('p', 'refusal', 'The app would refuse this edit:'));
      var problems = document.createElement('ul');
      (planned.problems || []).forEach(function (problem) {
        problems.appendChild(h('li', '', editorProblemText(problem)));
      });
      host.appendChild(problems);
      editorMessage('The edit is not ready; fix the problems above.');
      return;
    }

    var summary = editorSummary(planned.changes);
    host.appendChild(h('p', '', summary.length > 0 ? 'The app would: ' + summary.join(' \u00b7 ') + '.' : 'The app would change nothing.'));

    var list = document.createElement('ul');
    (planned.changes || []).forEach(function (change) {
      list.appendChild(h('li', '', editorChangeText(change)));
    });
    if ((planned.changes || []).length > 0) { host.appendChild(list); }

    (planned.warnings || []).forEach(function (warning) {
      if (warning.code === 'NEW_MOVEMENT') {
        host.appendChild(h('p', 'hint', 'The app will create the movement "' + (warning.name || warning.id) + '".'));
      } else if (warning.code === 'WORKOUT_KEPT') {
        host.appendChild(h('p', 'hint', 'The workout "' + (warning.name || warning.id) + '" stays in the app\'s library.'));
      }
    });
    // The document's own source first: a program opened from a backup knows what shared its workouts
    // even when no export was ever loaded (B191). The export is the fallback.
    var shared = editorSharedProgramNames(ed.working, ed.source || ui.state.catalogFile);
    if (shared.length > 0) {
      host.appendChild(h('p', 'hint', 'Also used by ' + shared.join(', ') + '; this edit changes them too.'));
    }
    editorMessage('Ready: download the edit and load it in Programs \u2192 Load.');
  }

  function editorDownload() {
    var ed = ui.state.programEditor;
    if (!ed || !ed.working || !ed.preview || !ed.preview.ok) { return; }
    // The download is the promise, and typing does not re-render, so the document is planned here and
    // now rather than trusted from the last Preview (B185). A field that stopped being valid turns
    // into the refusal in the preview panel instead of a file the app will refuse.
    editorPreview();
    if (!ed.preview || !ed.preview.ok) { return; }
    var payload = editorDownloadPayload(ed);
    downloadText(payload.text, payload.fileName);
    announce(ed.mode === 'new'
      ? 'Downloaded program.json. Load it in Programs, then Load, to add it.'
      : 'Downloaded program-edit.json. Load it in Programs, then Load.');
  }

  function editorSaveDraft() {
    var ed = ui.state.programEditor;
    if (!ed || !ed.working) { return; }
    editorPruneExercises(ed.working);
    var draft = {
      kind: EDITOR_DRAFT_KIND,
      version: 1,
      mode: ed.mode,
      original: ed.original,
      source: ed.source || null,
      working: ed.working
    };
    downloadText(JSON.stringify(draft, null, 2) + '\n', 'program-draft.json');
    announce('Downloaded program-draft.json. Nothing is stored in the page.');
  }

  function editorInit(elements) {
    var core = programCore();
    if (!core) {
      if (elements.editorUnavailable) { elements.editorUnavailable.hidden = false; }
      if (elements.programFileInput) { elements.programFileInput.disabled = true; }
      if (elements.newProgram) { elements.newProgram.disabled = true; }
      return;
    }
    elements.newProgram.addEventListener('click', editorStartNew);
    wireFileDrop(elements.programDropZone, elements.programFileInput, function (files) {
      readFile(files[0], 'program');
    });
    elements.previewProgramEdit.addEventListener('click', editorPreview);
    elements.downloadProgramEdit.addEventListener('click', editorDownload);
    elements.saveProgramDraft.addEventListener('click', editorSaveDraft);
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
    movementsWithHistory: movementsWithHistory,
    exportSnapshot: exportSnapshot,
    parseWaistlineDoc: parseWaistlineDoc,
    parseProgramSheet: parseProgramSheet,
    programDocumentFromBackup: programDocumentFromBackup,
    newProgramDocument: newProgramDocument,
    editorOpenSnapshot: editorOpenSnapshot,
    editorPlanEdit: editorPlanEdit,
    editorDownloadPayload: editorDownloadPayload,
    editorLoadText: editorLoadText,
    editorParseLoad: editorParseLoad,
    editorResolveExercise: editorResolveExercise,
    editorAddDay: editorAddDay,
    editorAddExercise: editorAddExercise,
    editorAddSet: editorAddSet,
    editorRemoveDay: editorRemoveDay,
    editorRemoveExercise: editorRemoveExercise,
    editorPruneExercises: editorPruneExercises,
    editorApplyRole: editorApplyRole,
    editorChangeText: editorChangeText,
    editorSummary: editorSummary,
    programPanelSummary: programPanelSummary,
    programStartOf: programStartOf,
    parseProgramDay: parseProgramDay,
    editorParseDay: editorParseDay,
    editorDayText: editorDayText,
    editorSetKind: editorSetKind,
    editorSharedProgramNames: editorSharedProgramNames,
    editorMove: editorMove,
    editorParseInt: editorParseInt,
    editorParseHalves: editorParseHalves,
    buildProgramDocument: buildProgramDocument,
    programClashNote: programClashNote,
    looksLikeProgramTable: looksLikeProgramTable,
    waistlineRows: waistlineRows,
    waistlineMeasurements: waistlineMeasurements,
    noonOfDate: noonOfDate,
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
