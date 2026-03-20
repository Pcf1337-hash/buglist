package com.buglist.data.repository

import com.buglist.data.local.dao.PersonDao
import com.buglist.data.local.entity.PersonEntity
import com.buglist.data.local.relation.PersonWithNetBalance
import com.buglist.domain.model.Person
import com.buglist.domain.model.PersonWithBalance
import com.buglist.domain.repository.PersonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [PersonRepository] backed by Room + SQLCipher.
 *
 * All mapping between domain models and entities is contained in this class.
 * The domain layer never sees Room annotations.
 */
@Singleton
class PersonRepositoryImpl @Inject constructor(
    private val personDao: PersonDao
) : PersonRepository {

    override fun getAllPersonsWithBalance(): Flow<List<PersonWithBalance>> =
        personDao.getAllPersonsWithBalance().map { list ->
            list.map { it.toDomain() }
        }

    override fun getPersonById(id: Long): Flow<Person?> =
        personDao.getPersonById(id).map { it?.toDomain() }

    override fun getPersonCount(): Flow<Int> =
        personDao.getPersonCount()

    override suspend fun addPerson(person: Person): Long =
        personDao.insert(person.toEntity())

    override suspend fun updatePerson(person: Person) =
        personDao.update(person.toEntity())

    override suspend fun deletePerson(person: Person) =
        personDao.delete(person.toEntity())

    override suspend fun deletePersonById(personId: Long) =
        personDao.deleteById(personId)

    override suspend fun updatePersonSortIndices(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            personDao.updateSortIndex(id, index)
        }
    }
}

// --- Mapping extensions ---

internal fun PersonEntity.toDomain(): Person = Person(
    id = id,
    name = name,
    phone = phone,
    notes = notes,
    avatarColor = avatarColor,
    createdAt = createdAt,
    sortIndex = sortIndex
)

internal fun Person.toEntity(): PersonEntity = PersonEntity(
    id = id,
    name = name,
    phone = phone,
    notes = notes,
    avatarColor = avatarColor,
    createdAt = createdAt,
    sortIndex = sortIndex
)

internal fun PersonWithNetBalance.toDomain(): PersonWithBalance = PersonWithBalance(
    person = person.toDomain(),
    netBalance = netBalance,
    openCount = openCount
)
