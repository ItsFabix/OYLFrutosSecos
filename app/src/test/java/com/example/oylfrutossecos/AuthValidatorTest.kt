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

    @Test fun nameBoundsRemainTwoToEightyCharacters() {
        for (length in listOf(1, 81)) assertTrue(AuthValidator.registration(valid.copy(name = "a".repeat(length))).containsKey("name"))
        for (length in listOf(2, 80)) assertFalse(AuthValidator.registration(valid.copy(name = "a".repeat(length))).containsKey("name"))
    }

    @Test fun aliasKeepsLengthAndCharacterRestrictions() {
        for (alias in listOf("ab", "a".repeat(21), "con espacio", "álias", "cliente!", "admin"))
            assertTrue(alias, AuthValidator.registration(valid.copy(alias = alias)).containsKey("alias"))
        for (alias in listOf("abc", "a".repeat(20), "cliente_1"))
            assertFalse(alias, AuthValidator.registration(valid.copy(alias = alias)).containsKey("alias"))
    }

    @Test fun emailMustHaveValidShapeAndAtMostOneHundredTwentyCharacters() {
        for (email in listOf("a@b", "a b@example.cl", "a".repeat(109) + "@example.com"))
            assertTrue(AuthValidator.registration(valid.copy(email = email)).containsKey("email"))
        assertFalse(AuthValidator.registration(valid.copy(email = "a".repeat(108) + "@example.com")).containsKey("email"))
    }

    @Test fun chileanMobilePhoneRulesRemainUnchanged() {
        for (phone in listOf("912345678", "+56912345678", "+56 9 1234-5678"))
            assertFalse(phone, AuthValidator.registration(valid.copy(phone = phone)).containsKey("phone"))
        for (phone in listOf("123", "812345678", "91234567", "9123456789", "+55912345678"))
            assertTrue(phone, AuthValidator.registration(valid.copy(phone = phone)).containsKey("phone"))
    }

    @Test fun addressBoundsRemainFiveToOneHundredSixtyCharacters() {
        for (length in listOf(4, 161)) assertTrue(AuthValidator.registration(valid.copy(address = "a".repeat(length))).containsKey("address"))
        for (length in listOf(5, 160)) assertFalse(AuthValidator.registration(valid.copy(address = "a".repeat(length))).containsKey("address"))
    }

    @Test fun passwordStillRequiresEightToOneHundredTwentyEightCharactersLettersAndDigits() {
        for (password in listOf("Abc1234", "a".repeat(128) + "1", "abcdefgh", "12345678"))
            assertTrue(AuthValidator.registration(valid.copy(password = password, confirmation = password)).containsKey("password"))
        for (password in listOf("Abc12345", "A1".repeat(64)))
            assertFalse(AuthValidator.registration(valid.copy(password = password, confirmation = password)).containsKey("password"))
    }

    @Test fun correctingConfirmationDoesNotMakeAnInvalidPasswordValid() {
        val errors = AuthValidator.registration(valid.copy(password = "short", confirmation = "short"))
        assertTrue(errors.containsKey("password"))
        assertFalse(errors.containsKey("confirmation"))
    }

    @Test fun validLoginRetainsItsExistingNonBlankRules() {
        assertTrue(AuthValidator.login(valid.alias, valid.password).isEmpty())
        assertEquals(setOf("alias"), AuthValidator.login("", valid.password).keys)
        assertEquals(setOf("password"), AuthValidator.login(valid.alias, "").keys)
    }
}
