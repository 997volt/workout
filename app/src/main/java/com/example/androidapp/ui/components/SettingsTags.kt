package com.example.androidapp.ui.components

/**
 * Tags for Settings, the tab bar and the app-wide actions they own: export and import, the backup
 * folder, diagnostics and housekeeping.
 */
internal interface SettingsTags {
    /**
     * Export and import (ROADMAP B1, N43). Tagged generically because the point is *which screen*
     * offers them: a test asserts presence in Settings and absence in the library, using the same
     * two tags. They moved here from the home overflow (N43), which is where the app's data is
     * what the screen is about.
     */
    val DATA_EXPORT: String get() = "data_export"

    /** An encrypted export (N294). */
    val DATA_EXPORT_ENCRYPTED: String get() = "data_export_encrypted"

    /** The password prompt and its controls, shared by an encrypted export and import (N294). */
    val PASSWORD_DIALOG: String get() = "password_dialog"
    val PASSWORD_FIELD: String get() = "password_field"
    val PASSWORD_CONFIRM_FIELD: String get() = "password_confirm_field"
    val PASSWORD_CONFIRM: String get() = "password_confirm"
    val PASSWORD_CANCEL: String get() = "password_cancel"

    val DATA_IMPORT: String get() = "data_import"

    /** The bottom bar's five roots (ROADMAP N34). */
    val TAB_BAR: String get() = "tab_bar"

    val TAB_WORKOUTS: String get() = "tab_workouts"

    val TAB_HISTORY: String get() = "tab_history"

    val TAB_STATISTICS: String get() = "tab_statistics"

    val TAB_LIBRARY: String get() = "tab_library"

    val TAB_SETTINGS: String get() = "tab_settings"

    val SETTINGS_SCREEN: String get() = "settings_screen"

    val SETTINGS_REST_CURRENT: String get() = "settings_rest_current"

    val SETTINGS_REST_CUE: String get() = "settings_rest_cue"

    val SETTINGS_KEEP_SCREEN_ON: String get() = "settings_keep_screen_on"

    /** The rest-timer switch (ROADMAP N44), and the workout screen's static prescription it turns on. */
    val SETTINGS_REST_TIMER: String get() = "settings_rest_timer"

    /** One chip of the progression mode, by enum name (P3.17). */
    fun settingsProgressionMode(mode: String) = "settings_progression_$mode"

    /** One cadence chip for the recovery-week suggestion (ROADMAP N301). */
    fun settingsDeloadSuggestion(weeks: Int) = "settings_deload_suggestion_$weeks"

    /** The two unit chips on Settings (ROADMAP N64). */
    val SETTINGS_WEIGHT_UNIT_KG: String get() = "setting_weight_unit_kg"

    val SETTINGS_WEIGHT_UNIT_LB: String get() = "setting_weight_unit_lb"

    /** The row that opens the typed delete-everything confirmation (ROADMAP N43). */
    val SETTINGS_CLEAR_DATA: String get() = "settings_clear_data"

    /** The Diagnostics screen (B324, N244): its way in, the two records and the two actions. */
    val SETTINGS_DIAGNOSTICS: String get() = "settings_diagnostics"

    val DIAGNOSTICS_SCREEN: String get() = "diagnostics_screen"

    val DIAGNOSTICS_CRASH: String get() = "diagnostics_crash"

    val DIAGNOSTICS_REPAIR: String get() = "diagnostics_repair"

    val DIAGNOSTICS_COPY: String get() = "diagnostics_copy"

    val DIAGNOSTICS_RETRY: String get() = "diagnostics_retry"

    /** The backup preview's question (ROADMAP N246). */
    val IMPORT_PREVIEW_DIALOG: String get() = "import_preview_dialog"

    val IMPORT_PREVIEW_CONFIRM: String get() = "import_preview_confirm"

    val IMPORT_PREVIEW_CANCEL: String get() = "import_preview_cancel"

    val IMPORT_PREVIEW_PROBLEMS: String get() = "import_preview_problems"

    /** The one-time notice a failed housekeeping pass raises (B324). */
    val HOUSEKEEPING_ALERT: String get() = "housekeeping_alert"

    val HOUSEKEEPING_RETRY: String get() = "housekeeping_retry"

    val HOUSEKEEPING_LATER: String get() = "housekeeping_later"

    val HOUSEKEEPING_DETAILS: String get() = "housekeeping_details"

    /** The backup that writes itself (ROADMAP N218): the switch, one chip per period, the folder and the date. */
    val SETTINGS_BACKUP_SWITCH: String get() = "settings_backup_switch"

    val SETTINGS_BACKUP_NOW: String get() = "settings_backup_now"

    val SETTINGS_BACKUP_FOLDER: String get() = "settings_backup_folder"

    val SETTINGS_BACKUP_LAST: String get() = "settings_backup_last"

    fun settingsBackupSchedule(name: String) = "settings_backup_$name"

    fun settingRest(seconds: Int) = "setting_rest_$seconds"
}
