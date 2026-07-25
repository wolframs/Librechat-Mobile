package com.garfiec.librechat.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.feature.chat.resources.Res
import com.garfiec.librechat.feature.chat.resources.cache_ttl_armed_1h
import com.garfiec.librechat.feature.chat.resources.cache_ttl_armed_5m
import com.garfiec.librechat.feature.chat.resources.cache_ttl_expired
import com.garfiec.librechat.feature.chat.resources.cache_ttl_idle
import com.garfiec.librechat.feature.chat.resources.cache_ttl_semantics
import com.garfiec.librechat.feature.chat.viewmodel.CacheTtl
import com.garfiec.librechat.feature.chat.viewmodel.CacheTtlAnchor
import com.garfiec.librechat.feature.chat.viewmodel.cacheTtlRemainingMillis
import com.garfiec.librechat.feature.chat.viewmodel.formatCacheTtlRemaining
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

/**
 * Fixed-width vertical Anthropic prompt-cache control for the upper-left thread gutter.
 * The horizontal pill is rotated as a unit so its label remains compact and the hit target
 * occupies a predictable 28 × 104 dp rail regardless of countdown text.
 */
@Composable
fun CacheTtlRail(
    anchor: CacheTtlAnchor?,
    armed: CacheTtl?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fallbackAnchorTime = remember(anchor?.messageId) { Clock.System.now().toEpochMilliseconds() }
    val anchorTime = anchor?.timestampMillis ?: fallbackAnchorTime
    var nowMillis by remember(anchor?.messageId) {
        mutableLongStateOf(Clock.System.now().toEpochMilliseconds())
    }
    LaunchedEffect(anchor?.messageId) {
        while (true) {
            delay(1_000)
            nowMillis = Clock.System.now().toEpochMilliseconds()
        }
    }

    val remaining = anchor?.let {
        cacheTtlRemainingMillis(anchorTime, it.ttl, nowMillis)
    }
    val isExpired = remaining != null && remaining <= 0L
    val label = when (armed) {
        CacheTtl.ONE_HOUR -> stringResource(Res.string.cache_ttl_armed_1h)
        CacheTtl.FIVE_MINUTES -> stringResource(Res.string.cache_ttl_armed_5m)
        null -> when {
            anchor == null -> stringResource(Res.string.cache_ttl_idle)
            isExpired -> stringResource(Res.string.cache_ttl_expired)
            else -> formatCacheTtlRemaining(remaining ?: 0L)
        }
    }

    val containerColor = when (armed) {
        CacheTtl.ONE_HOUR -> Color(0x33F59E0B)
        CacheTtl.FIVE_MINUTES -> Color(0x332A9DF4)
        null -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (armed) {
        CacheTtl.ONE_HOUR -> Color(0xFFF59E0B)
        CacheTtl.FIVE_MINUTES -> Color(0xFF38A8F8)
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val semanticsLabel = stringResource(Res.string.cache_ttl_semantics, label)

    Box(
        modifier = modifier
            .width(28.dp)
            .height(104.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                // The parent is intentionally only 28 dp wide. requiredWidth prevents those
                // constraints from squeezing the pill before the graphics-layer rotation.
                .requiredWidth(104.dp)
                .rotate(-90f)
                .semantics {
                    contentDescription = semanticsLabel
                    role = Role.Button
                },
            shape = RoundedCornerShape(50),
            color = containerColor,
            contentColor = contentColor,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (armed == null && anchor != null && !isExpired) {
                                Color(0xFF10B981)
                            } else {
                                contentColor
                            },
                        ),
                )
                Text(
                    text = label,
                    modifier = Modifier.padding(start = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}
