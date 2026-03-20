package com.buglist.domain.model

/**
 * Domain model representing a person tracked in the debt ledger.
 *
 * This is a pure domain object — it has no Room annotations.
 * Mapping to/from [com.buglist.data.local.entity.PersonEntity] is handled
 * in the data layer.
 *
 * @param id          Auto-generated primary key (0 = new, not yet persisted).
 * @param name        Display name shown in the UI. Non-empty, max 100 chars.
 * @param phone       Optional phone number for quick-dial / contact lookup.
 * @param notes       Free-text notes about this person.
 * @param avatarColor ARGB color int for the initials avatar background.
 * @param createdAt   Unix timestamp (ms) when this record was created.
 */
data class Person(
    val id: Long = 0,
    val name: String,
    val phone: String? = null,
    val notes: String? = null,
    val avatarColor: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * Manual sort position. Lower values appear higher in the list.
     * Default [Int.MAX_VALUE] so new persons appear at the bottom before the user
     * has manually reordered. After the first drag-to-reorder, all items get
     * sequential values (0, 1, 2, …). Items with equal sortIndex fall back to
     * name ordering (see PersonDao.getAllPersonsWithBalance ORDER BY clause).
     */
    val sortIndex: Int = Int.MAX_VALUE,
    /**
     * Absolute path to a custom avatar image stored in the app's internal files directory.
     * `null` = no custom photo → fall back to initials circle with [avatarColor].
     * Only settable via the EditPersonSheet (AddPersonSheet does not expose this field).
     */
    val avatarImagePath: String? = null
)
