package com.garfiec.librechat.core.ui.components

import com.garfiec.librechat.core.model.Banner
import kotlin.test.Test
import kotlin.test.assertEquals

class BannerDisplayPolicyTest {

    @Test
    fun `dismissed ordinary banner is hidden but persistable banner cannot be hidden`() {
        val ordinary = Banner(bannerId = "ordinary", persistable = false)
        val mandatory = Banner(bannerId = "mandatory", persistable = true)

        val visible = visibleServerBanners(
            banners = listOf(ordinary, mandatory),
            dismissedIds = setOf("ordinary", "mandatory"),
        )

        assertEquals(listOf(mandatory), visible)
    }

    @Test
    fun `banner without an identity is not rendered as dismissible state`() {
        assertEquals(
            emptyList(),
            visibleServerBanners(listOf(Banner(message = "anonymous")), emptySet()),
        )
    }
}
