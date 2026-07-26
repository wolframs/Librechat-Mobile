package com.garfiec.librechat.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class IconButtonContentDescriptionRuleTest {

    private fun lint(code: String) = IconButtonContentDescriptionRule(Config.empty).lint(code)

    @Test
    fun `reports null description inside IconButton`() {
        val findings = lint(
            """
            fun Example() {
                IconButton(onClick = {}) {
                    Icon(imageVector = Any(), contentDescription = null)
                }
            }
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `accepts localized action description`() {
        val findings = lint(
            """
            fun Example(label: String) {
                IconButton(onClick = {}) {
                    Icon(imageVector = Any(), contentDescription = label)
                }
            }
            """.trimIndent(),
        )

        assertTrue(findings.isEmpty())
    }

    @Test
    fun `ignores decorative icon outside IconButton`() {
        val findings = lint(
            """
            fun Example() {
                Row {
                    Icon(imageVector = Any(), contentDescription = null)
                    Text("Visible label")
                }
            }
            """.trimIndent(),
        )

        assertTrue(findings.isEmpty())
    }
}
