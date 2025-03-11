package com.pmj.util

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.pmj.Constants.ACCESS_EXPIRY
import com.pmj.Constants.JWT_AUDIENCE
import com.pmj.Constants.JWT_ISSUER
import com.pmj.Constants.JWT_REFRESH_SECRET
import com.pmj.Constants.JWT_SECRET
import com.pmj.Constants.REFRESH_EXPIRY
import com.pmj.Constants.USER_ID
import io.github.cdimascio.dotenv.dotenv
import java.util.*

object JWTUtil {

    fun generateAccessToken(userId: Int): String {

        val dotenv = dotenv()

        val issuer = dotenv[JWT_ISSUER]
        val audience = dotenv[JWT_AUDIENCE]
        val accessSecret = dotenv[JWT_SECRET]
        val accessExpiry = dotenv[ACCESS_EXPIRY]?.toLong() ?: 900000L

        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim(USER_ID, userId)
            .withExpiresAt(Date(System.currentTimeMillis() + accessExpiry))
            .sign(Algorithm.HMAC256(accessSecret))
    }

    fun generateRefreshToken(userId: Int): String {
        val dotenv = dotenv()
        val issuer = dotenv[JWT_ISSUER]
        val refreshSecret = dotenv[JWT_REFRESH_SECRET]
        val refreshExpiry = dotenv[REFRESH_EXPIRY]?.toLong() ?: 604800000L

        return JWT.create()
            .withIssuer(issuer)
            .withClaim(USER_ID, userId)
            .withExpiresAt(Date(System.currentTimeMillis() + refreshExpiry))
            .sign(Algorithm.HMAC256(refreshSecret))
    }

    fun verifyRefreshToken(token: String): Int? {

        val dotenv = dotenv()

        val issuer = dotenv[JWT_ISSUER]
        val refreshSecret = dotenv[JWT_REFRESH_SECRET]

        return try {
            val verifier = JWT.require(Algorithm.HMAC256(refreshSecret))
                .withIssuer(issuer)
                .build()
            val decodedJWT = verifier.verify(token)
            decodedJWT.getClaim(USER_ID).asInt()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}