package com.pmj

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ExposedUser(val username: String, val password: String, val profilePicture: String)

class UserService(database: Database) {
    object User : Table() {
        val id = integer("id").autoIncrement()
        val username = varchar("username", length = 15)
        val password = varchar("password", length = 200)
        val profilePicture = varchar("profile_picture", length = 255).nullable()

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(User)
        }

    }

    suspend fun create(user: ExposedUser): Int = dbQuery {
        User.insert {
            it[username] = user.username
            it[password] = user.password
            it[profilePicture] = user.profilePicture
        }[User.id]
    }

    suspend fun read(requestUser: ExposedUser): Int? {
        return dbQuery {
            User.select(User.id)
                .where { (User.username eq requestUser.username) and (User.password eq requestUser.password) }
                .singleOrNull()
                ?.get(User.id)
        }
    }

    suspend fun getUserById(userId: Int): String? {
        return dbQuery {
            User.select(User.username)
                .where { User.id eq userId }
                .singleOrNull()
                ?.get(User.username)
        }
    }

    suspend fun updateProfilePicture(id: Int, filePath: String) {
        dbQuery {
            User.update({ User.id eq id }) {
                it[profilePicture] = filePath
            }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}

