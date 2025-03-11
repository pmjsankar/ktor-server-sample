package com.pmj

import com.pmj.Constants.POSTGRES_PASSWORD
import com.pmj.Constants.POSTGRES_URL
import com.pmj.Constants.POSTGRES_USER
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import java.sql.Connection

/**
 * Makes a connection to a Postgres database.
 *
 * In order to connect to your running Postgres process,
 * please specify the following parameters in your configuration file:
 * - postgres.url -- Url of your running database process.
 * - postgres.user -- Username for database connection
 * - postgres.password -- Password for database connection
 *
 * If you don't have a database process running yet, you may need to [download]((https://www.postgresql.org/download/))
 * and install Postgres and follow the instructions [here](https://postgresapp.com/).
 * Then, you would be able to edit your url,  which is usually "jdbc:postgresql://host:port/database", as well as
 * user and password values.
 * @return [Database] that represent connection to the database. Please, don't forget to close this connection when
 * your application shuts down by calling [Connection.close]
 * */
fun Application.configureDatabases(): Database {

    val dotenv = dotenv()
    val url = dotenv[POSTGRES_URL]
    val user = dotenv[POSTGRES_USER]
    val password = dotenv[POSTGRES_PASSWORD]

    return Database.connect(
        url = url,
        user = user,
        driver = "org.postgresql.Driver",
        password = password,
    )
}
