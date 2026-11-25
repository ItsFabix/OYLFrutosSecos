package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.remote.ApiInventoryMovement
import com.example.oylfrutossecos.data.repository.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE)
class InventoryRepositoryTest:RepositorySqliteSupport() {
    @Test fun movementsMapRemoteIdsDirectionsAbsoluteQuantitiesAndOptionalFilter()=runBlocking {
        val repo=InventoryRepository(db,api) {key};val p=repo.products().single()
        api.inventory=listOf(ApiInventoryMovement(1,1,p.name,2,3,5,"Ingreso",100),
            ApiInventoryMovement(2,999,"Otro",-3,8,5,"Salida",200))
        val all=repo.movements()
        assertEquals(p.id,all[0].productId);assertEquals(MovementType.ENTRY,all[0].type)
        assertEquals(2,all[0].quantity);assertEquals(MovementType.EXIT,all[1].type)
        assertEquals(3,all[1].quantity);assertEquals(999L,all[1].productId)
        assertEquals(listOf(all[0]),repo.movements(p.id));assertTrue(repo.movements(100000).isEmpty())
        assertEquals(key,api.lastKey)
    }
    @Test fun adjustmentsUseRemoteIdentityAndTrimNoteAndConfirmedWriteSurvivesRefreshFailure()=runBlocking {
        val repo=InventoryRepository(db,api) {key};val p=repo.products().single()
        assertEquals(p.id,repo.adjust(1,p.id,MovementType.ENTRY,2,"  Reposición  "))
        assertEquals(2,api.stockChanges.last().second.delta);assertEquals("Reposición",api.stockChanges.last().second.note)
        api.productsFailure=java.io.IOException("refresh only")
        assertEquals(p.id,repo.adjust(1,p.id,MovementType.EXIT,1,"Venta"))
        assertEquals(-1,api.stockChanges.last().second.delta);assertEquals(1L,api.stockChanges.last().first)
    }
    @Test fun missingCredentialsInvalidFieldsAndUnmappedProductsDoNotSendMovements()=runBlocking {
        val repo=InventoryRepository(db,api) {key};val p=repo.products().single()
        try {InventoryRepository(db,api).movements();fail("key required")} catch(_:IllegalStateException) {}
        try {InventoryRepository(db,api).adjust(1,p.id,MovementType.ENTRY,1,"Nota");fail("key required")} catch(_:IllegalStateException) {}
        for((quantity,note) in listOf(0 to "Nota",10001 to "Nota",1 to "X",1 to "a".repeat(201))) {
            try {repo.adjust(1,p.id,MovementType.ENTRY,quantity,note);fail("invalid movement")} catch(_:IllegalArgumentException) {}
        }
        val local=db.saveProduct(null,LogicFixtures.draft.copy(name="Local"))
        for(id in listOf(local,99999L)) {
            try {repo.adjust(1,id,MovementType.ENTRY,1,"Nota");fail("unmapped")} catch(_:IllegalStateException) {}
        }
        assertTrue(api.stockChanges.isEmpty())
    }
    @Test fun rejectedWriteAndCancelledRefreshPropagateWithoutRetry()=runBlocking {
        val repo=InventoryRepository(db,api) {key};val p=repo.products().single()
        api.writeFailure=LogicFixtures.http(403)
        try {repo.adjust(1,p.id,MovementType.ENTRY,1,"Nota");fail("forbidden")} catch(e:retrofit2.HttpException) {assertEquals(403,e.code())}
        assertTrue(api.stockChanges.isEmpty())
        api.writeFailure=null;api.productsFailure=CancellationException()
        try {repo.adjust(1,p.id,MovementType.ENTRY,1,"Nota");fail("cancelled refresh")} catch(_:CancellationException) {}
        assertEquals(1,api.stockChanges.size)
    }
}
