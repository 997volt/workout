package com.example.androidapp

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The source manifest's privacy promises (B275).
 *
 * The APK-level permission check is in `tools/check-apk-permissions.py`; this reads the file a reviewer edits,
 * so a permission or an exported component cannot be added without a test saying what it means.
 */
class ManifestInvariantsTest {

    private val android = "http://schemas.android.com/apk/res/android"

    private fun manifest(): Element {
        val file = File(repoRoot, "app/src/main/AndroidManifest.xml")
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val document = factory.newDocumentBuilder().parse(file)
        return document.documentElement
    }

    @Test
    fun theApplicationOptsOutOfBackup_andNamesBothRuleFiles() {
        val application = manifest().getElementsByTagName("application").item(0) as Element
        assertEquals("false", application.getAttributeNS(android, "allowBackup"))
        assertEquals("@xml/data_extraction_rules", application.getAttributeNS(android, "dataExtractionRules"))
        assertEquals("@xml/backup_rules", application.getAttributeNS(android, "fullBackupContent"))
    }

    @Test
    fun theSourceDeclaresNoPermissions() {
        assertEquals(
            "the source manifest is the place a permission would be added",
            0,
            manifest().getElementsByTagName("uses-permission").length,
        )
    }

    @Test
    fun onlyTheLauncher_isExported() {
        val manifest = manifest()
        val launcher = manifest.launcher()
        assertEquals("true", launcher.getAttributeNS(android, "exported"))
        listOf("activity", "service", "receiver", "provider").forEach { tag ->
            val nodes = manifest.getElementsByTagName(tag)
            for (index in 0 until nodes.length) {
                val component = nodes.item(index) as Element
                if (component === launcher) continue
                assertEquals(
                    "$tag ${component.getAttributeNS(android, "name")} must not be exported",
                    "false",
                    component.getAttributeNS(android, "exported"),
                )
            }
        }
    }

    /** The activity carrying MAIN/LAUNCHER, not whichever activity is written first (N298). */
    private fun Element.launcher(): Element {
        val activities = getElementsByTagName("activity")
        for (index in 0 until activities.length) {
            val activity = activities.item(index) as Element
            val filters = activity.getElementsByTagName("intent-filter")
            for (filterIndex in 0 until filters.length) {
                val filter = filters.item(filterIndex) as Element
                val actions = filter.getElementsByTagName("action")
                val categories = filter.getElementsByTagName("category")
                val isMain = (0 until actions.length).any {
                    (actions.item(it) as Element).getAttributeNS(android, "name") ==
                        "android.intent.action.MAIN"
                }
                val isLauncher = (0 until categories.length).any {
                    (categories.item(it) as Element).getAttributeNS(android, "name") ==
                        "android.intent.category.LAUNCHER"
                }
                if (isMain && isLauncher) return activity
            }
        }
        error("the source manifest has no MAIN/LAUNCHER activity")
    }
}
