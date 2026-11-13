package com.garfiec.librechat.core.network.client

import com.garfiec.librechat.core.common.identity.AccountId
import com.garfiec.librechat.core.common.identity.AccountState.Resolved
import com.garfiec.librechat.core.common.identity.InMemoryActiveAccountProvider
import com.garfiec.librechat.core.common.identity.deriveAccountId
import com.garfiec.librechat.core.common.identity.deriveServerId
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Test
import kotlin.test.assertFailsWith

/**
 * Snapshot semantics of [SwitchGate.captureSnapshot]: the bearer is keyed to the snapshot's account
 * (immune to another account's sign-in staging), and a [PendingRequestIdentity] in the coroutine
 * context short-circuits to the pending identity without touching the live providers or the gate.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SwitchGateTest {

    private val accountA = deriveAccountId(deriveServerId("https://a.example.com"), "user-a").value

    private class FakeTokenManager(
        private val liveToken: String?,
        keyed: Map<String, String> = emptyMap(),
        /** What a proactive renewal rewrites the account's keyed slot to, or null to renew nothing. */
        private val renewsTo: String? = null,
    ) : TokenManager {
        private val keyed = keyed.toMutableMap()

        /** How many times [ensureFreshAccessToken] was reached. The gate is opt-in, so this is the assertion. */
        var renewals = 0
            private set

        /** The `(accountId, baseUrl, bearer)` each renewal was handed — the pair-coherence assertion. */
        val renewalArgs = mutableListOf<Triple<String?, String, String?>>()

        override val isAuthenticated: Boolean get() = liveToken != null
        override suspend fun getAccessToken(): String? = liveToken
        override suspend fun setTokens(accessToken: String, refreshToken: String) = Unit
        override suspend fun refreshAccessToken(usedAccessToken: String?): RefreshResult = RefreshResult.HardExpired
        override suspend fun clearTokens() = Unit
        override suspend fun getAccessTokenFor(accountId: String): String? = keyed[accountId]

        override suspend fun ensureFreshAccessToken(
            accountId: String?,
            baseUrl: String,
            currentAccessToken: String?,
        ): String? {
            renewals++
            renewalArgs += Triple(accountId, baseUrl, currentAccessToken)
            if (renewsTo != null && accountId != null) keyed[accountId] = renewsTo
            return renewsTo ?: currentAccessToken
        }

        override suspend fun getStagedAccessToken(): String? = null
        override suspend fun clearStagedTokens() = Unit
        override suspend fun selectAccount(accountId: String) = Unit
        override suspend fun removeAccount(accountId: String) = Unit
        override suspend fun refreshAccessTokenFor(
            accountId: String,
            baseUrl: String,
            usedAccessToken: String?,
        ): RefreshResult = RefreshResult.HardExpired
        override suspend fun onAccountResolved(accountId: String) = Unit
        override suspend fun onAccountCleared() = Unit
        override fun emitSessionExpired(expiredAccountId: String?, reason: SessionEndReason) = Unit
        override val sessionExpiredFlow: SharedFlow<SessionEndReason> = MutableSharedFlow()
    }

    private class FakeServerUrlProvider(private val baseUrl: String) : ServerUrlProvider {
        override fun getBaseUrl(): String = baseUrl
    }

    @Test
    fun `a sign in transition waits for the cold start seed before clearing its binding`() = runTest {
        val seeded = CompletableDeferred<Unit>()
        val gate = SwitchGate(
            activeAccountProvider = InMemoryActiveAccountProvider(),
            serverUrlProvider = FakeServerUrlProvider("https://a.example.com"),
            tokenManager = FakeTokenManager(null),
            accountReadyGate = object : AccountReadyGate {
                override suspend fun awaitReady() = seeded.await()
            },
        )
        var detached = false
        val transition = launch { gate.withSwitch { detached = true } }
        yield()
        assertThat(detached).isFalse()

        seeded.complete(Unit)
        transition.join()

        assertThat(detached).isTrue()
    }

    @Test
    fun `a bound account refuses a different deployment before reading or renewing credentials`() = runTest {
        val tokens = object : TokenManager by FakeTokenManager("a-token") {
            override suspend fun getAccessTokenFor(accountId: String): String? =
                error("credentials must not be read for a mismatched server")
        }
        for (url in listOf(
            "https://b.example.com", "http://a.example.com", "https://a.example.com:8443",
            "https://a.example.com/other", "", "file:///tmp/server",
        )) {
            val gate = SwitchGate(
                activeAccountProvider = InMemoryActiveAccountProvider(Resolved(AccountId(accountA))),
                serverUrlProvider = FakeServerUrlProvider(url),
                tokenManager = tokens,
                accountReadyGate = null,
            )
            val failure = assertFailsWith<IllegalStateException> { gate.captureSnapshot(renewIfStale = true) }
            assertThat(failure).hasMessageThat().isEqualTo("Active account does not belong to the configured server")
        }
    }

    @Test
    fun `equivalent server URLs keep the bound account authenticated`() = runTest {
        val gate = SwitchGate(
            activeAccountProvider = InMemoryActiveAccountProvider(Resolved(AccountId(accountA))),
            serverUrlProvider = FakeServerUrlProvider("https://A.EXAMPLE.COM:443/"),
            tokenManager = FakeTokenManager("a-token", mapOf(accountA to "a-token")),
            accountReadyGate = null,
        )

        assertThat(gate.captureSnapshot().bearer).isEqualTo("a-token")
    }

    @Test
    fun `snapshot bearer is keyed to the snapshot account, not the live cache`() = runTest {
        // Sign-in staging in progress: the live cache holds the STAGED account's token, but the
        // resolved account A must snapshot its own keyed token.
        val gate = SwitchGate(
            activeAccountProvider = InMemoryActiveAccountProvider(Resolved(AccountId(accountA))),
            serverUrlProvider = FakeServerUrlProvider("https://a.example.com"),
            tokenManager = FakeTokenManager(liveToken = "staged-b", keyed = mapOf(accountA to "a-keyed")),
            accountReadyGate = null,
        )

        val snapshot = gate.captureSnapshot()

        assertThat(snapshot.accountId).isEqualTo(accountA)
        assertThat(snapshot.bearer).isEqualTo("a-keyed")
        assertThat(snapshot.isPending).isFalse()
    }

    @Test
    fun `snapshot bearer is null for a resolved account with an empty keyed slot`() = runTest {
        // Must not fall through to the live cache (which may hold another account's staged token).
        val gate = SwitchGate(
            activeAccountProvider = InMemoryActiveAccountProvider(Resolved(AccountId(accountA))),
            serverUrlProvider = FakeServerUrlProvider("https://a.example.com"),
            tokenManager = FakeTokenManager(liveToken = "staged-b", keyed = emptyMap()),
            accountReadyGate = null,
        )

        assertThat(gate.captureSnapshot().bearer).isNull()
    }

    @Test
    fun `snapshot falls back to the live token when no account is resolved`() = runTest {
        // Legacy pre-migration / logged-out-with-bare-tokens path.
        val gate = SwitchGate(
            activeAccountProvider = InMemoryActiveAccountProvider(Resolved(null)),
            serverUrlProvider = FakeServerUrlProvider("https://a.example.com"),
            tokenManager = FakeTokenManager(liveToken = "bare-legacy"),
            accountReadyGate = null,
        )

        assertThat(gate.captureSnapshot().bearer).isEqualTo("bare-legacy")
    }

    @Test
    fun `pending identity short-circuits without reading the live providers`() = runTest {
        // Live providers that would fail the test if consulted.
        val gate = SwitchGate(
            activeAccountProvider = InMemoryActiveAccountProvider(Resolved(AccountId(accountA))),
            serverUrlProvider = object : ServerUrlProvider {
                override fun getBaseUrl(): String = error("live URL must not be read for a pending request")
            },
            tokenManager = object : TokenManager by FakeTokenManager(null) {
                override suspend fun getAccessToken(): String? = error("live token must not be read")
                override suspend fun getAccessTokenFor(accountId: String): String? = error("keyed token must not be read")
            },
            accountReadyGate = object : AccountReadyGate {
                override suspend fun awaitReady() = error("ready gate must not be awaited for a pending request")
            },
        )

        val snapshot = withContext(PendingRequestIdentity("https://b.example.com") { "staged-b" }) {
            gate.captureSnapshot()
        }

        assertThat(snapshot.baseUrl).isEqualTo("https://b.example.com")
        assertThat(snapshot.accountId).isNull()
        assertThat(snapshot.bearer).isEqualTo("staged-b")
        assertThat(snapshot.isPending).isTrue()
    }

    @Test
    fun `pending identity bypasses a closed switch gate`() = runTest {
        val gate = SwitchGate(
            activeAccountProvider = InMemoryActiveAccountProvider(Resolved(AccountId(accountA))),
            serverUrlProvider = FakeServerUrlProvider("https://a.example.com"),
            tokenManager = FakeTokenManager(liveToken = "a-token", keyed = mapOf(accountA to "a-token")),
            accountReadyGate = null,
        )
        val flipStarted = CompletableDeferred<Unit>()
        val releaseFlip = CompletableDeferred<Unit>()
        val switch = launch {
            gate.withSwitch {
                flipStarted.complete(Unit)
                releaseFlip.await()
            }
        }
        flipStarted.await()

        // A pending capture completes while the gate is closed; a live capture would park.
        val pending = async {
            withContext(PendingRequestIdentity("https://b.example.com") { null }) {
                gate.captureSnapshot()
            }
        }
        yield()
        assertThat(pending.isCompleted).isTrue()
        assertThat(pending.await().baseUrl).isEqualTo("https://b.example.com")

        releaseFlip.complete(Unit)
        switch.join()
    }

    /**
     * Proactive renewal is opt-in per capture. The default must stay off: logout and the post-login
     * profile fetch snapshot through here too, and each decides for itself whether to renew — logout
     * renews only once it knows the account holds a refresh token to revoke with.
     */
    @Test
    fun `the default capture never renews`() = runTest {
        val tokens = FakeTokenManager(liveToken = "a", keyed = mapOf(accountA to "a-keyed"))
        val gate = SwitchGate(
            activeAccountProvider = InMemoryActiveAccountProvider(Resolved(AccountId(accountA))),
            serverUrlProvider = FakeServerUrlProvider("https://a.example.com"),
            tokenManager = tokens,
            accountReadyGate = null,
        )

        gate.captureSnapshot()

        assertThat(tokens.renewals).isEqualTo(0)
    }

    @Test
    fun `an opted-in capture renews and snapshots the renewed bearer`() = runTest {
        val tokens = FakeTokenManager(
            liveToken = "a",
            keyed = mapOf(accountA to "a-stale"),
            renewsTo = "a-renewed",
        )
        val gate = SwitchGate(
            activeAccountProvider = InMemoryActiveAccountProvider(Resolved(AccountId(accountA))),
            serverUrlProvider = FakeServerUrlProvider("https://a.example.com"),
            tokenManager = tokens,
            accountReadyGate = null,
        )

        val snapshot = gate.captureSnapshot(renewIfStale = true)

        assertThat(tokens.renewals).isEqualTo(1)
        // The renewal runs before the triple is read, so the request carries the new bearer rather
        // than sending the stale one and recovering from its 401.
        assertThat(snapshot.bearer).isEqualTo("a-renewed")
    }

    @Test
    fun `a pending identity never renews even when opted in`() = runTest {
        // An add-account probe has no keyed account to refresh, and renewing the *active* account's
        // token on its behalf would be the live session's traffic done under another flow's identity.
        val tokens = FakeTokenManager(liveToken = "a", keyed = mapOf(accountA to "a-keyed"))
        val gate = SwitchGate(
            activeAccountProvider = InMemoryActiveAccountProvider(Resolved(AccountId(accountA))),
            serverUrlProvider = FakeServerUrlProvider("https://a.example.com"),
            tokenManager = tokens,
            accountReadyGate = null,
        )

        withContext(PendingRequestIdentity("https://b.example.com") { "staged-b" }) {
            gate.captureSnapshot(renewIfStale = true)
        }

        assertThat(tokens.renewals).isEqualTo(0)
    }

    /**
     * The renewal must be handed the snapshot's own values, never a fresh read of the live providers.
     *
     * A switch publishes the new server URL *before* the new account id, so two independent reads can
     * straddle it and produce (old account, new URL). Refreshing against that pair POSTs one account's
     * refresh token to another deployment — the exact tearing this class exists to prevent — and the
     * rejection then drops that account's slot. Pinned here with a provider that changes value on
     * every read: whatever the renewal is given must equal what the snapshot froze.
     */
    @Test
    fun `renewal is handed the snapshot's own account, url and bearer`() = runTest {
        // Stands in for a switch landing between two reads: every call returns something new, so any
        // value the renewal did not take from the snapshot is immediately visible as a mismatch.
        class ShiftingUrlProvider : ServerUrlProvider {
            var reads = 0
                private set

            override fun getBaseUrl(): String = "https://server-${reads++}.example.com"
        }

        val shiftingAccount = deriveAccountId(deriveServerId("https://server-1.example.com"), "user-a").value
        val baseline = ShiftingUrlProvider()
        SwitchGate(
            activeAccountProvider = InMemoryActiveAccountProvider(Resolved(AccountId(shiftingAccount))),
            serverUrlProvider = baseline,
            tokenManager = FakeTokenManager(liveToken = "a", keyed = mapOf(shiftingAccount to "a-stale")),
            accountReadyGate = null,
        ).captureSnapshot()

        val shifting = ShiftingUrlProvider()
        val tokens = FakeTokenManager(liveToken = "a", keyed = mapOf(shiftingAccount to "a-stale"))
        val gate = SwitchGate(
            activeAccountProvider = InMemoryActiveAccountProvider(Resolved(AccountId(shiftingAccount))),
            serverUrlProvider = shifting,
            tokenManager = tokens,
            accountReadyGate = null,
        )

        val snapshot = gate.captureSnapshot(renewIfStale = true)

        assertThat(tokens.renewalArgs).hasSize(1)
        val (account, baseUrl, bearer) = tokens.renewalArgs.single()
        assertThat(account).isEqualTo(snapshot.accountId)
        assertThat(baseUrl).isEqualTo(snapshot.baseUrl)
        assertThat(bearer).isEqualTo("a-stale")
        // Renewing costs no extra provider read: it reuses the snapshot rather than re-reading.
        assertThat(shifting.reads).isEqualTo(baseline.reads)
    }

    /**
     * The renewal must happen outside [SwitchGate]'s lock — it is the same mutex a switch takes, and a
     * refresh POST suspended inside it would stall every account switch behind a network round trip.
     */
    @Test
    fun `a renewal in progress does not block an account switch`() = runTest {
        val releaseRenewal = CompletableDeferred<Unit>()
        val renewalStarted = CompletableDeferred<Unit>()
        val gate = SwitchGate(
            activeAccountProvider = InMemoryActiveAccountProvider(Resolved(AccountId(accountA))),
            serverUrlProvider = FakeServerUrlProvider("https://a.example.com"),
            tokenManager = object : TokenManager by FakeTokenManager("a", mapOf(accountA to "a-keyed")) {
                override suspend fun ensureFreshAccessToken(
                    accountId: String?,
                    baseUrl: String,
                    currentAccessToken: String?,
                ): String? {
                    renewalStarted.complete(Unit)
                    releaseRenewal.await()
                    return currentAccessToken
                }
            },
            accountReadyGate = null,
        )

        val capture = async { gate.captureSnapshot(renewIfStale = true) }
        renewalStarted.await()

        var flipped = false
        val switch = launch { gate.withSwitch { flipped = true } }
        yield()

        assertThat(flipped).isTrue()
        assertThat(capture.isCompleted).isFalse()

        releaseRenewal.complete(Unit)
        switch.join()
        capture.await()
    }
}
