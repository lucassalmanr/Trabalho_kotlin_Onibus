package br.com.mobilibus.tracker.data.repository

import br.com.mobilibus.tracker.data.local.SessionStore
import br.com.mobilibus.tracker.data.local.UserDao
import br.com.mobilibus.tracker.data.local.UserEntity
import br.com.mobilibus.tracker.domain.repository.AuthRepository
import java.security.MessageDigest

class RoomAuthRepository(
    private val users: UserDao,
    private val session: SessionStore
) : AuthRepository {
    override suspend fun register(email: String, password: String): Result<Unit> = runCatching {
        require(EMAIL.matches(email)) { "Informe um e-mail válido." }
        require(password.length >= 6) { "A senha deve ter pelo menos 6 caracteres." }
        require(users.findByEmail(email.trim().lowercase()) == null) { "Este e-mail já está cadastrado." }
        val id = users.insert(UserEntity(email = email.trim().lowercase(), passwordHash = password.sha256()))
        session.saveUser(id)
    }

    override suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val user = users.findByEmail(email.trim().lowercase())
        if (user == null || user.passwordHash != password.sha256()) {
            throw IllegalArgumentException("E-mail ou senha incorretos.")
        }
        session.saveUser(user.id)
    }

    override suspend fun logout() {
        session.clear()
    }

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray())
        .joinToString("") { "%02x".format(it) }

    private companion object {
        val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
