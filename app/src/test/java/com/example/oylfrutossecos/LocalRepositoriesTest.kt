package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.local.RemoteCheckoutCache
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.repository.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE)
class LocalRepositoriesTest:RepositorySqliteSupport() {
    private val registration=Registration("Cliente Nuevo","nuevo","nuevo@example.test","912345678","Calle de prueba 123","Cliente123","Cliente123")
    @Test fun authRegistrationLoginRoleFailuresAndLogoutPreserveOtherUsers()=runBlocking {
        val repo=AuthRepository(db);assertNull(repo.currentUser())
        repo.register(registration)
        val owner=repo.login(registration.alias,registration.password,UserRole.CLIENT)!!
        assertEquals(registration.name,owner.name);assertEquals(owner,repo.currentUser())
        assertNull(repo.login(registration.alias,"Wrong1234",UserRole.CLIENT))
        assertNull(repo.login(registration.alias,registration.password,UserRole.ADMIN))
        assertEquals(owner,repo.currentUser())
        repo.logout();assertNull(repo.currentUser())
        assertEquals(owner,repo.login(registration.alias,registration.password,UserRole.CLIENT))
    }
    @Test fun authValidatesBeforeWriteAndDuplicateAliasHasDomainException()=runBlocking {
        val repo=AuthRepository(db)
        try {repo.register(registration.copy(name="X"));fail("invalid register")} catch(_:IllegalArgumentException) {}
        assertNull(repo.login(registration.alias,registration.password,UserRole.CLIENT))
        repo.register(registration)
        try {repo.register(registration.copy(alias=" NUEVO "));fail("duplicate")} catch(_:AliasAlreadyUsedException) {}
        assertNotNull(repo.login(registration.alias,registration.password,UserRole.CLIENT))
    }
    @Test fun profilePersistsOnlyForActiveOwnerAndKeepsAuthIdentityUnchanged()=runBlocking {
        val repo=AuthRepository(db);repo.register(registration)
        val owner=repo.login(registration.alias,registration.password,UserRole.CLIENT)!!
        val other=user("otro")
        val draft=ProfileDraft("Nombre Editado","987654321","Dirección nueva 456")
        val saved=repo.updateOwnProfile(owner.id,draft)
        assertEquals(owner.copy(name=draft.name,phone=draft.phone,address=draft.address),saved)
        assertEquals(saved,AuthRepository(db).currentUser())
        try {repo.updateOwnProfile(other.id,draft);fail("another user's profile")} catch(_:IllegalStateException) {}
        repo.logout()
        try {repo.updateOwnProfile(owner.id,draft);fail("logged out")} catch(_:IllegalStateException) {}
        reopenSandbox()
        val reopened=AuthRepository(db)
        assertNull(reopened.currentUser())
        assertEquals(saved,reopened.login(registration.alias,registration.password,UserRole.CLIENT))
        reopenSandbox()
        assertEquals(saved,AuthRepository(db).currentUser())
    }
    @Test fun cartSnapshotQuantityValidationDeletionAndUserSeparationUseRealSqlite()=runBlocking {
        val owner=user();val other=user("otro");val repo=CartRepository(db)
        repo.setQuantity(owner.id,1,2);repo.setQuantity(other.id,1,1)
        val snapshot=repo.snapshot(owner.id)
        assertEquals(repo.getCart(owner.id),snapshot.items);assertNull(snapshot.pending)
        assertEquals(2,snapshot.items.single().quantity)
        try {repo.setQuantity(owner.id,1,999);fail("excess stock")} catch(_:IllegalArgumentException) {}
        assertEquals(snapshot.items,repo.snapshot(owner.id).items)
        repo.setQuantity(owner.id,1,0)
        assertTrue(repo.getCart(owner.id).isEmpty());assertEquals(1,repo.getCart(other.id).single().quantity)
    }
    @Test fun pendingCartSnapshotLocksOnlyTheOwnerAndTransactionEndsAfterError()=runBlocking {
        val owner=user();val other=user("otro");val repo=CartRepository(db)
        repo.setQuantity(owner.id,1,1)
        val pending=RemoteCheckoutCache(db).getOrCreate(owner.id,"pending-test","{}",DeliveryMethod.PICKUP,repo.getCart(owner.id))
        assertEquals(pending,repo.snapshot(owner.id).pending);assertNull(repo.snapshot(other.id).pending)
        try {repo.setQuantity(owner.id,1,0);fail("pending cart changed")} catch(_:IllegalStateException) {}
        assertFalse(db.readableDatabase.inTransaction())
        assertEquals(1,repo.getCart(owner.id).single().quantity)
    }
    @Test fun notificationsPersistAndReadOperationsCannotAffectAnotherOwner()=runBlocking {
        val owner=user();val other=user("otro");val repo=NotificationRepository(db)
        val a=repo.createTest(owner.id);val b=repo.createTest(other.id)
        assertEquals(NoticeKind.TEST,a.kind);assertFalse(a.isRead);assertEquals(owner.id,a.userId)
        assertTrue(repo.list(owner.id).any {it.id==a.id})
        assertFalse(repo.list(owner.id).any {it.id==b.id})
        repo.markRead(other.id,a.id);assertFalse(repo.list(owner.id).single {it.id==a.id}.isRead)
        repo.markRead(owner.id,a.id);assertTrue(repo.list(owner.id).single {it.id==a.id}.isRead)
        val c=repo.createTest(owner.id);repo.markAllRead(owner.id)
        assertTrue(repo.list(owner.id).single {it.id==c.id}.isRead)
        assertFalse(repo.list(other.id).single {it.id==b.id}.isRead)
    }
}
