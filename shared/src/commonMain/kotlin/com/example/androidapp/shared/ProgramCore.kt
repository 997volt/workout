package com.example.androidapp.shared

/**
 * The program document version this build reads and writes (ROADMAP N170).
 *
 * Kept here rather than in either host: the page refuses what it cannot write and the app refuses
 * what it cannot read, and a version that lived twice would be the first thing to disagree.
 *
 * **6 is B276's failure set** — the reason a ceiling exists (N185). A document may now name a role a
 * v5 build has no constant for, and that build decodes with `ignoreUnknownKeys`, so the set would
 * arrive as an ordinary one rather than being refused: a wrong meaning, not a dropped extra. The
 * floor stays at 4, because a v5 document still means exactly what it says.
 */
const val PROGRAM_FORMAT_VERSION: Int = 6
