package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.local.PasswordHasher
import org.junit.Assert.*
import org.junit.Test

class PasswordHasherTest {
    @Test fun verifiesCorrectPasswordAndRejectsIncorrectPassword() {
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash("Cliente123", salt)
        assertTrue(PasswordHasher.verify("Cliente123", salt, hash))
        assertFalse(PasswordHasher.verify("cliente123", salt, hash))
    }

    @Test fun differentSaltsProduceDifferentHashes() {
        val first = PasswordHasher.hash("Cliente123", ByteArray(16) { 1 })
        val second = PasswordHasher.hash("Cliente123", ByteArray(16) { 2 })
        assertFalse(first.contentEquals(second))
    }
}
