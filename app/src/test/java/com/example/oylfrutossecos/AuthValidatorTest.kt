package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.model.Registration
import com.example.oylfrutossecos.domain.AuthValidator
import org.junit.Assert.*
import org.junit.Test

class AuthValidatorTest {
    private val valid = Registration("Cliente Demo", "cliente1", "cliente1@example.com",
        "+56 9 0000 0000", "Calle de Prueba 123", "Cliente123", "Cliente123")

    @Test fun validRegistrationHasNoErrors() {
        assertTrue(AuthValidator.registration(valid).isEmpty())
    }

    @Test fun blankRegistrationReportsEveryField() {
        assertEquals(setOf("name", "alias", "email", "phone", "address", "password", "confirmation"),
            AuthValidator.registration(Registration()).keys)
    }

    @Test fun adminAliasIsReservedEvenWithSpacesAndUppercase() {
        assertTrue(AuthValidator.registration(valid.copy(alias = " ADMIN ")).containsKey("alias"))
    }

    @Test fun differentConfirmationIsRejected() {
        assertTrue(AuthValidator.registration(valid.copy(confirmation = "Otra1234")).containsKey("confirmation"))
    }

    @Test fun malformedContactIsRejected() {
        val errors = AuthValidator.registration(valid.copy(email = "sin-correo", phone = "123"))
        assertTrue(errors.containsKey("email"))
        assertTrue(errors.containsKey("phone"))
    }

    @Test fun loginRejectsEmptyFields() {
        assertEquals(setOf("alias", "password"), AuthValidator.login(" ", "").keys)
    }

    @Test fun aliasNormalizationIsConsistent() {
        assertEquals("cliente1", AuthValidator.normalizeAlias(" Cliente1 "))
    }
}
