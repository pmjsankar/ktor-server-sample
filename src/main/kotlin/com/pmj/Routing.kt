package com.pmj

import com.pmj.Constants.USER_ID
import com.pmj.util.JWTUtil
import com.pmj.util.save
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import java.io.File

fun Application.configureRouting(database: Database) {

    val userService = UserService(database)
    val tokenService = TokenService(database)

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }

    routing {
        staticResources("/resources", "static")
        staticResources("/upload-profile", "static")
        staticFiles("/uploads", File("static/uploads"))

        get("/") {
            call.respondText("Hello, Welcome to Rest World!")
        }

        post("/user") {
            val user = call.receive<ExposedUser>()
            val id = userService.create(user)
            call.respond(HttpStatusCode.Created, id)
        }

        post("/login") {
            val user = call.receive<ExposedUser>()
            val userId = userService.read(user)

            if (userId != null) {
                val accessToken = JWTUtil.generateAccessToken(userId = userId)
                val refreshToken = JWTUtil.generateRefreshToken(userId = userId)
                tokenService.storeToken(userId = userId, token = refreshToken)
                call.respond(mapOf("accessToken" to accessToken, "refreshToken" to refreshToken))
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
            }
        }

        post("/refresh") {
            val refreshToken = call.request.headers["Authorization"]?.removePrefix("Bearer ")
            if (refreshToken.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing refresh token")
                return@post
            }

            if (!tokenService.isValidToken(refreshToken)) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid or expired refresh token")
                return@post
            }

            val userId = JWTUtil.verifyRefreshToken(refreshToken) ?: run {
                call.respond(HttpStatusCode.Unauthorized, "Invalid refresh token")
                return@post
            }

            val accessToken = JWTUtil.generateAccessToken(userId)
            call.respond(HttpStatusCode.OK, mapOf("accessToken" to accessToken))
        }

        authenticate("auth-jwt") {
            get("/protected") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim(USER_ID).asInt()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                    return@get
                }
                val username = userService.getUserById(userId)
                if (username.isNullOrBlank()) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    return@get
                }
                call.respond(HttpStatusCode.OK, "Hello, $username! This is a protected route.")
            }

            post("/logout") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim(USER_ID).asInt()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                    return@post
                }

                tokenService.deleteTokensForUser(userId)
                call.respond(HttpStatusCode.OK, "Logged out successfully")
            }
        }

        post("upload-profile-image") {
//            val principal = call.principal<JWTPrincipal>()
//            val userId = principal!!.payload.getClaim(USER_ID).asInt()

//            if (userId == null) {
//                call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
//                return@post
//            }

            val multipart = call.receiveMultipart()
            var fileName: String? = null
            var fileSaved = false

            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    if (!part.name.isNullOrBlank() && !part.originalFileName.isNullOrBlank()) {
                        fileName = part.save("static/uploads/")
                        fileSaved = true
                    }
                }
                part.dispose()
            }

            if (fileSaved) {
                val filePath = "/uploads/$fileName"
                userService.updateProfilePicture(id = 1, filePath = filePath)
                call.respond(HttpStatusCode.OK, "File uploaded successfully: $filePath")
            } else {
                call.respond(HttpStatusCode.BadRequest, "No file uploaded")
            }
        }
    }
}
