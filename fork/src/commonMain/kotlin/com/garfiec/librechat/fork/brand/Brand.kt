package com.garfiec.librechat.fork.brand

/**
 * What the fork calls itself and where it comes from. The app id is `dev.wsiener.mercuric`
 * (`app/build.gradle.kts`); the Kotlin package stays upstream's, so its changes keep merging in.
 * A core module cannot read this: it repeats the name as a literal on a hook line.
 */
object Brand {
    const val NAME = "Mercuric"
    const val SOURCE_URL = "https://github.com/wolframs/Librechat-Mobile"

    /** The app this one is a modified version of (Apache License 2.0). */
    const val UPSTREAM_NAME = "Switchboard"
    const val UPSTREAM_URL = "https://github.com/garfiec/Librechat-Mobile"

    /** The fork's copy of the policy; its text is still upstream's. */
    const val PRIVACY_POLICY_URL = "$SOURCE_URL/blob/develop/docs/PRIVACY.md"

    /** Upstream's report screen mails upstream's maintainer, who does not support modified builds. */
    const val REPORTS_TO_MAINTAINER = false
}
