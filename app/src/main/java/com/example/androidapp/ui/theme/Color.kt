package com.example.androidapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The palette.
 *
 * One fixed dark scheme rather than Material You's wallpaper colours: the reference this is
 * drawn from — M.E. Me, a patient-held health record — is dark wherever it runs, and separating
 * surfaces by *tone* instead of by elevation is what makes a deep-black screen read as calm
 * rather than as a light theme switched off. A dynamic scheme would hand the app's identity to
 * whatever the launcher's wallpaper happens to be, so it is gone rather than merely defaulted.
 *
 * Colours are grouped by the job they do, not by the screen that first wanted one.
 */

// --- Surfaces: four steps of near-black. Depth is tone; nothing here casts a shadow. ---

/** The page. Also the window background, so the first frame is not a white flash. */
val Ink = Color(0xFF0A0A0F)

/** A card sitting on the page. */
val InkRaised = Color(0xFF14141B)

/** A menu, a sheet, or a card sitting on a card. */
val InkHigh = Color(0xFF1D1D26)

/** The 1dp line where two surfaces of the same tone meet. */
val Hairline = Color(0xFF2A2A36)

// --- Text ---

/** Primary text. Not quite pure white, which glares against a black page. */
val Chalk = Color(0xFFF4F4F7)

/** Supporting text, labels, anything that must be readable but not compete. */
val ChalkMuted = Color(0xFF9A9AA8)

// --- Accents ---
//
// The reference uses colour as *category*: a pill, a tooth and a clipboard each get their own
// hue so a list can be recognised before it is read. These are therefore named by colour and
// handed out per row, not bound to a semantic role.

/** The app's own colour: the primary action and the selected state. */
val Indigo = Color(0xFF6C5CE7)

/** Pressed/held indigo, for a container behind the accent rather than the accent itself. */
val IndigoDeep = Color(0xFF473A9E)

val Teal = Color(0xFF2FA8A0)
val Coral = Color(0xFFE8735A)
val Amber = Color(0xFFD09A4E)
val Sky = Color(0xFF5B7CDE)

// --- Roles a component reads rather than a category it hands out ---

/**
 * The filled slot that is *not* the primary action (ROADMAP N49).
 *
 * Deliberately not [Teal], which stays the category accent a row's tile wears. This is the same
 * hue taken down to a surface: it is what `secondaryContainer` was reaching for, and Teal itself
 * could not be it — white on Teal measures 2.9:1, so every control that filled with the category
 * colour carried a label under the contrast body text needs. The pair is asserted by
 * `PaletteContrastTest` rather than eyeballed, the way `TileAccent.onColor` already is (B55).
 */
val TealDeep = Color(0xFF1E6F6A)

/**
 * The link colour: a text action inside a screen (ROADMAP N49).
 *
 * Not [Indigo]. Primary is the app's own colour and it *fills* things — the selected tab, a chip that
 * is on, and home's *Resume* — and Indigo measures 4.07:1 against the page and 3.77:1 on a raised
 * card, both under the 4.5:1 body-size text needs. Redefining primary under a filled surface would
 * have dragged every one of them down with the links, so the links get a role of their own and
 * [Indigo] keeps filling.
 */
val IndigoLink = Color(0xFFB0A4F0)

/** Destructive only. Deliberately redder than [Coral], which is a category and not a warning. */
val Crimson = Color(0xFFF2555A)

/** Behind a dialog or a menu. Black at most of its opacity: a scrim on a black page has to be
 * dark enough to read as a veil rather than as a slightly different black. */
val Scrim = Color(0xCC000000)
