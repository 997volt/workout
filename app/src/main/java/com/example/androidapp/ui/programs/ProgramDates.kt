package com.example.androidapp.ui.programs

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * A day as a program screen writes it, in the reader's own locale (ROADMAP N188).
 *
 * One function for the three places a program's dates are shown — the edit confirmation, the
 * editor's start row, and the deload week — because a second copy is a formatter that can disagree
 * with the first about a locale or a style, the drift the weekday labels had before F8.
 */
internal fun formatProgramDay(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
