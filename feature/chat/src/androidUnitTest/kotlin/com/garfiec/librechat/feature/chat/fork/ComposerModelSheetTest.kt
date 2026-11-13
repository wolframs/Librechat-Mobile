package com.garfiec.librechat.feature.chat.fork

import com.garfiec.librechat.core.model.ModelRef
import com.garfiec.librechat.feature.chat.util.CompareQuickPick
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposerModelSheetTest {
    private fun pick(model: String, starred: Boolean = false) = CompareQuickPick("endpoint", model, model, "Provider", starred)

    @Test fun chipDropsVendorButKeepsShortName() {
        assertEquals("haiku-5.5", shortModelName("anthropic/haiku-5.5"))
        assertEquals("haiku-5.5", shortModelName("haiku-5.5"))
    }

    @Test fun claudeNamesAndSpecLabelsAreShared() {
        assertEquals("Haiku 5.5", shortModelName("claude-haiku-5.5"))
        assertEquals("Opus 4.8", shortModelName("anthropic/claude-opus-4-8"))
        assertEquals("My spec", shortModelName("claude-haiku-5.5", "My spec"))
        assertEquals("deepseek-v4.1-flash", shortModelName("vendor/deepseek-v4.1-flash"))
    }

    @Test fun currentModelIsFirstWhenUnstarredAndAlwaysFits() {
        val current = pick("current")
        val pins = (1..8).map { pick("m$it", true) }
        assertEquals(listOf(current) + pins.take(6), modelSheetPicks(pins, emptyList(), current))
        assertEquals(listOf(current), modelSheetPicks(emptyList(), emptyList(), current))
        assertEquals(pins.take(5) + pins.last(), modelSheetPicks(pins, emptyList(), pins.last()))
    }

    @Test fun starredModelsWinInPinOrderAndStopAtSix() {
        val pins = (1..8).map { pick("m$it", true) }
        assertEquals(
            pins.take(6),
            modelSheetPicks(listOf(pick("other")) + pins, listOf(ModelRef("endpoint", "other"))),
        )
    }

    @Test fun unstarredFallbackFollowsUsageAndDropsUnavailableModels() {
        val candidates = listOf(pick("a"), pick("b"))
        val usage = listOf(ModelRef("endpoint", "gone"), ModelRef("endpoint", "b"), ModelRef("endpoint", "a"))
        assertEquals(listOf(pick("b"), pick("a")), modelSheetPicks(candidates, usage))
        assertEquals(emptyList(), modelSheetPicks(candidates, emptyList()))
    }
}
