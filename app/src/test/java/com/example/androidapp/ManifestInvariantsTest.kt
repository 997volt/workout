package com.example.androidapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        val launcher = manifest().getElementsByTagName("activity").item(0) as Element
        assertEquals("true", launcher.getAttributeNS(android, "exported"))
        listOf("activity", "service", "receiver", "provider").forEach { tag ->
            val nodes = manifest().getElementsByTagName(tag)
            for (index in 0 until nodes.length) {
                val component = nodes.item(index) as Element
                val name = component.getAttributeNS(android, "name")
                if (name.endsWith(".MainActivity")) {
                    assertTrue(
                        "the launcher must remain exported",
                        component.getAttributeNS(android, "exported") == "true",
                    )
                } else {
                    assertEquals(
                        "$tag $name must not be exported",
                        "false",
                        component.getAttributeNS(android, "exported"),
                    )
                }
            }
        }
    }
}
