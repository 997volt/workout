package com.example.androidapp.domain.model

import java.time.LocalDate
import java.time.ZoneId

/**
 * A screen's local day together with the zone it was read in (ROADMAP N286/B391).
 *
 * The pair travels as one value because a resume can change the zone without changing the date. A flow of
 * dates alone would conflate that case and leave the screen drawing yesterday's zone.
 */
data class ScreenDay(val date: LocalDate, val zone: ZoneId)
