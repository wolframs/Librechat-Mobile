package com.garfiec.librechat.fork.brand

import androidx.compose.runtime.Composable
import com.garfiec.librechat.fork.resources.Res
import com.garfiec.librechat.fork.resources.brand_based_on
import com.garfiec.librechat.fork.resources.brand_based_on_detail
import org.jetbrains.compose.resources.stringResource

/** The About page's attribution row: the name of the app this one is built on, and its licence. */
@Composable
fun basedOnTitle(): String = stringResource(Res.string.brand_based_on, Brand.UPSTREAM_NAME)

@Composable
fun basedOnSummary(): String = stringResource(Res.string.brand_based_on_detail)
