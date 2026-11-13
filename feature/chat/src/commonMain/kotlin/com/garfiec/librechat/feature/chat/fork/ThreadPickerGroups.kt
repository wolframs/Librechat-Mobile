package com.garfiec.librechat.feature.chat.fork

import com.garfiec.librechat.feature.chat.util.PickerRow
import com.garfiec.librechat.fork.thread.usableGroupsFirst

/** Move whole endpoint groups, keeping spec rows, stars and search results as upstream emitted them. */
internal fun threadPickerGroups(rows: List<PickerRow>): List<List<PickerRow>> {
    val groups = mutableListOf<MutableList<PickerRow>>()
    rows.forEach { row ->
        if (groups.isEmpty() || row is PickerRow.Header) groups.add(mutableListOf())
        groups.last().add(row)
    }
    return usableGroupsFirst(groups) { (it.firstOrNull() as? PickerRow.Header)?.keyHint != null }
}
