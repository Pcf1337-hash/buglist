package com.buglist.domain.model

/**
 * Sealed hierarchy for items rendered in the Dashboard crew list.
 *
 * The list can contain both [PersonItem] rows and [DividerItem] separator rows.
 * Items from both tables are merged and sorted by [sortIndex] in the ViewModel,
 * so the user's drag-to-reorder arrangement is fully preserved.
 *
 * [listKey] is used as the Compose LazyColumn item key — it must be unique across
 * the entire combined list (prefix "p_" for persons, "d_" for dividers).
 */
sealed class DashboardListItem {

    /** Manual sort position shared across persons and dividers. */
    abstract val sortIndex: Int

    /** Unique stable key for Compose `items(key = …)` — must not collide across types. */
    abstract val listKey: String

    /** A crew member row. */
    data class PersonItem(val data: PersonWithBalance) : DashboardListItem() {
        override val sortIndex: Int get() = data.person.sortIndex
        override val listKey: String get() = "p_${data.person.id}"
    }

    /** A decorative separator row with an optional label. */
    data class DividerItem(val data: Divider) : DashboardListItem() {
        override val sortIndex: Int get() = data.sortIndex
        override val listKey: String get() = "d_${data.id}"
    }
}
