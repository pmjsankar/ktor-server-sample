package com.pmj

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class TokenService(database: Database) {

    object RefreshTokens : Table() {
        val id = integer("id").autoIncrement()
        val userId = reference("user_id", UserService.User.id)
        val token = varchar("token", length = 512)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(RefreshTokens)
        }
    }

    suspend fun storeToken(userId: Int, token: String): Int = dbQuery {
        RefreshTokens.insert {
            it[RefreshTokens.userId] = userId
            it[RefreshTokens.token] = token
        }[RefreshTokens.id]
    }

    suspend fun isValidToken(token: String): Boolean {
        return dbQuery {
            RefreshTokens.selectAll().where { RefreshTokens.token eq token }.count() > 0
        }
    }

    suspend fun deleteTokensForUser(userId: Int) {
        dbQuery {
            RefreshTokens.deleteWhere { RefreshTokens.userId eq userId }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}

