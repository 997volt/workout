package com.example.androidapp.platform

import androidx.core.content.FileProvider

/**
 * The release provider for the staged block CSV (ROADMAP N331).
 *
 * A subclass only so it has a different `ContentProvider` name from the debug-only provider the
 * instrumented document test declares (N281): two `<provider>` elements with the same class but
 * different authorities conflict in the manifest merger, and the debug test's provider must stay.
 */
class ReportFileProvider : FileProvider()
