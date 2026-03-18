package com.buglist.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.buglist.data.local.entity.DebtEntryEntity
import com.buglist.data.local.entity.PaymentEntity

/**
 * Room relation combining a [DebtEntryEntity] with all its [PaymentEntity] records.
 *
 * Used in @Transaction queries in [com.buglist.data.local.dao.DebtEntryDao]
 * and [com.buglist.data.local.dao.PaymentDao].
 */
data class DebtEntryWithPaymentsRelation(
    @Embedded val debtEntry: DebtEntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "debtEntryId"
    )
    val payments: List<PaymentEntity>
)
