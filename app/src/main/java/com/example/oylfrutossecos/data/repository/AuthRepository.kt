package com.example.oylfrutossecos.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.model.Registration
import com.example.oylfrutossecos.data.model.User
import com.example.oylfrutossecos.data.model.UserRole
import com.example.oylfrutossecos.domain.AuthValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AliasAlreadyUsedException : Exception()

class AuthRepository(private val database: OylDatabase) {
    suspend fun currentUser(): User? = withContext(Dispatchers.IO) {
        database.readSession()
    }

    suspend fun register(form: Registration) = withContext(Dispatchers.IO) {
        require(AuthValidator.registration(form).isEmpty())
        try {
            database.registerClient(form)
        } catch (error: SQLiteConstraintException) {
            throw AliasAlreadyUsedException()
        }
    }

    suspend fun login(alias: String, password: String, role: UserRole): User? =
        withContext(Dispatchers.IO) {
            val user = database.authenticate(alias, password, role)
            if (user != null) database.saveSession(user.id)
            user
        }

    suspend fun logout() = withContext(Dispatchers.IO) {
        database.clearSession()
    }
}
