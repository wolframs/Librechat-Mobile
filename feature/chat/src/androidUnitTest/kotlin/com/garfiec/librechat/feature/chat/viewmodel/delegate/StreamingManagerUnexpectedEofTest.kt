package com.garfiec.librechat.feature.chat.viewmodel.delegate

import com.garfiec.librechat.core.common.identity.ActiveAccountProvider
import com.garfiec.librechat.core.common.network.ConnectivityObserver
import com.garfiec.librechat.core.data.repository.ChatRepository
import com.garfiec.librechat.core.model.Message
import com.garfiec.librechat.core.model.StreamEvent
import com.garfiec.librechat.feature.chat.viewmodel.ChatStateHandle
import com.garfiec.librechat.feature.chat.viewmodel.ChatUiState
import com.garfiec.librechat.feature.chat.viewmodel.ConversationMetaState
import com.garfiec.librechat.feature.chat.viewmodel.MessagesState
import com.garfiec.librechat.feature.chat.viewmodel.StreamingHandle
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Pins the caller-independent stream contract for a flow that returns without Final or Error.
 * Both new sends and edit/regenerate/continue enter through [StreamingManagerDelegate.launchStream],
 * so neither path can accidentally omit the EOF teardown.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StreamingManagerUnexpectedEofTest {

    private val chatRepository = mockk<ChatRepository>(relaxed = true)
    private val comparisonDelegate = mockk<ComparisonModeDelegate>(relaxed = true)
    private val completionDelegate = mockk<SendCompletionDelegate>(relaxed = true)
    private val queueDelegate = mockk<MessageQueueDelegate>(relaxed = true)
    private val reloadConversation = mockk<(String) -> Unit>(relaxed = true)

    private fun delegateWith(
        scope: TestScope,
    ): Pair<StreamingManagerDelegate, MutableStateFlow<ChatUiState>> {
        val state = MutableStateFlow(
            ChatUiState(
                conversation = ConversationMetaState(conversationId = "conv-1"),
                content = MessagesState(isStreaming = true),
            ),
        )
        val root = ChatStateHandle(state, scope)
        val connectivity = mockk<ConnectivityObserver>(relaxed = true)
        every { connectivity.isConnected } returns flowOf(true)
        return StreamingManagerDelegate(
            handle = StreamingHandle(root),
            chatRepository = chatRepository,
            activeAccountProvider = mockk<ActiveAccountProvider>(relaxed = true),
            connectivityObserver = connectivity,
            comparisonDelegate = comparisonDelegate,
            subagentTraceDelegate = mockk(relaxed = true),
            officePreviewDelegate = mockk(relaxed = true),
            completionDelegate = completionDelegate,
            queueDelegate = queueDelegate,
            treeDelegate = mockk(relaxed = true),
            emitUserKeyError = {},
            reloadConversation = reloadConversation,
            restoreUnsentInput = { _, _ -> },
            pendingActionDelegate = mockk(relaxed = true),
            steeringDelegate = mockk(relaxed = true),
            isNewConversation = { false },
            isHandedOffNewChat = { false },
        ) to state
    }

    @Test
    fun `clean EOF recovers a normal send and preserves its partial`() =
        runTest(StandardTestDispatcher()) {
            val (delegate, state) = delegateWith(this)
            delegate.prepareForStreaming(isEdit = false)

            delegate.launchStream(flowOf(StreamEvent.ContentDelta(chunk = "partial")))
            advanceUntilIdle()

            assertThat(state.value.isStreaming).isFalse()
            assertThat(state.value.streamingContent).isEqualTo("partial")
            assertThat(state.value.error).contains("Connection closed")
            verify(exactly = 1) { reloadConversation("conv-1") }
            verify(exactly = 1) { comparisonDelegate.endStreaming() }
            verify(exactly = 1) { queueDelegate.pause() }
        }

    @Test
    fun `clean EOF applies the same teardown to edit regenerate and continue streams`() =
        runTest(StandardTestDispatcher()) {
            val (delegate, state) = delegateWith(this)
            delegate.prepareForStreaming(isEdit = true)
            assertThat(delegate.isEditOrRegenerate).isTrue()

            delegate.launchStream(flowOf())
            advanceUntilIdle()

            assertThat(state.value.isStreaming).isFalse()
            assertThat(state.value.error).contains("Connection closed")
            verify(exactly = 1) { reloadConversation("conv-1") }
            verify(exactly = 1) { comparisonDelegate.endStreaming() }
            verify(exactly = 1) { queueDelegate.pause() }
        }

    @Test
    fun `terminal Final followed by flow completion cannot finalize twice`() =
        runTest(StandardTestDispatcher()) {
            val (delegate, state) = delegateWith(this)
            delegate.prepareForStreaming(isEdit = false)
            val response = Message(
                messageId = "a1",
                conversationId = "conv-1",
                parentMessageId = "u1",
                text = "done",
            )

            delegate.launchStream(flowOf(StreamEvent.Final(responseMessage = response)))
            advanceUntilIdle()

            assertThat(state.value.isStreaming).isFalse()
            assertThat(state.value.error).isNull()
            verify(exactly = 1) {
                completionDelegate.onFinal(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }
            verify(exactly = 1) { queueDelegate.drainNext(any()) }
            verify(exactly = 0) { queueDelegate.pause() }
            verify(exactly = 0) { reloadConversation(any()) }
        }
}
