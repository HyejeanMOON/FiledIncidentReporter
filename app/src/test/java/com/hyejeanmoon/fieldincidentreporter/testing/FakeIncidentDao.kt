package com.hyejeanmoon.fieldincidentreporter.testing

import androidx.paging.PagingSource
import com.hyejeanmoon.fieldincidentreporter.data.local.IncidentDao
import com.hyejeanmoon.fieldincidentreporter.data.local.IncidentEntity

/** Keeps rows in memory with the SQL ordering of [IncidentDao]; the real [syncPage] runs on top of it. */
internal class FakeIncidentDao(rows: List<IncidentEntity> = emptyList()) : IncidentDao() {
    private val rows = rows.associateBy(IncidentEntity::id).toMutableMap()
    val upserted = mutableListOf<IncidentEntity>()
    val deleted = mutableListOf<String>()

    /** Rows in list order, newest first. */
    val list: List<IncidentEntity>
        get() = rows.values.sortedWith(compareByDescending<IncidentEntity> { it.createdAt }.thenByDescending { it.id })

    override fun pagingSource(): PagingSource<Int, IncidentEntity> = error("Not used by these tests")

    override suspend fun upsert(incidents: List<IncidentEntity>) {
        upserted += incidents
        incidents.forEach { rows[it.id] = it }
    }

    override suspend fun delete(ids: List<String>) {
        deleted += ids
        ids.forEach(rows::remove)
    }

    override suspend fun range(firstCreatedAt: Long?, firstId: String?, lastCreatedAt: Long, lastId: String) =
        list.filter { row ->
            (firstCreatedAt == null || row.compareTo(firstCreatedAt, firstId!!) <= 0) && row.compareTo(lastCreatedAt, lastId) >= 0
        }

    override suspend fun hasRowsFrom(createdAt: Long, id: String) = rows.values.any { it.compareTo(createdAt, id) <= 0 }

    override suspend fun deleteAfter(createdAt: Long, id: String) {
        rows.values.filter { it.compareTo(createdAt, id) < 0 }.forEach { delete(listOf(it.id)) }
    }

    override suspend fun deleteAll() = delete(rows.keys.toList())

    /** Positive when this row is newer than the given position, so it comes earlier in the list. */
    private fun IncidentEntity.compareTo(createdAt: Long, id: String): Int =
        if (this.createdAt != createdAt) this.createdAt.compareTo(createdAt) else this.id.compareTo(id)
}
