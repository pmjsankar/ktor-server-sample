package com.pmj

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.pmj.Constants.JWT_AUDIENCE
import com.pmj.Constants.JWT_ISSUER
import com.pmj.Constants.JWT_SECRET
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.websocket.*
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    configureSerialization()
    configureSecurity()
    val database = configureDatabases()
    configureRouting(database)

}

fun Application.configureSecurity() {

    val dotenv = dotenv()
    val jwtIssuer = dotenv[JWT_ISSUER]
    val jwtAudience = dotenv[JWT_AUDIENCE]
    val jwtSecret = dotenv[JWT_SECRET]

    install(Authentication) {
        jwt("auth-jwt") {
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }
    }
}
