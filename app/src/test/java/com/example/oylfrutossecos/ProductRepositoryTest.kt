package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.repository.ProductRepository
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE)
class ProductRepositoryTest:RepositorySqliteSupport() {
    @Test fun remoteCatalogIsAuthoritativeAndOfflineCacheExcludesLocalProducts()=runBlocking {
        val localId=db.saveProduct(null,LogicFixtures.draft.copy(name="Solo local"))
        val repo=ProductRepository(db,api)
        val products=repo.getProducts()
        assertEquals(1,products.size);assertEquals(1L,products.single().remoteId)
        assertFalse(products.any {it.id==localId});assertEquals(products,repo.cachedProducts())
        api.productsFailure=java.io.IOException("offline")
        try {repo.getProducts();fail("network error hidden")} catch(_:java.io.IOException) {}
        assertEquals(products,repo.cachedProducts())
    }
    @Test fun createPrivatePhotoStaysLocalAndEditKeepsServerStock()=runBlocking {
        val repo=ProductRepository(db,api) {key}
        repo.getProducts()
        val draft=LogicFixtures.draft.copy(imageKey="photo:unit-private.jpg")
        val id=repo.saveProduct(null,draft)
        assertNull(api.productInputs.single().imageKey);assertEquals(key,api.lastKey)
        assertEquals("photo:unit-private.jpg",db.readProducts().single {it.id==id}.imageKey)
        assertEquals(1000L,db.readProducts().single {it.id==id}.remoteId)
        repo.saveProduct(id,draft.copy(name="Producto editado",stock=999,imageKey=null))
        val saved=db.readProducts().single {it.id==id}
        assertEquals("Producto editado",saved.name);assertEquals(draft.stock,saved.stock)
        assertEquals("photo:unit-private.jpg",saved.imageKey)
    }
    @Test fun ownIllustrationsAreSentAndInvalidOrUnauthorizedProductsNeverWrite()=runBlocking {
        val repo=ProductRepository(db,api) {key}
        val id=repo.saveProduct(null,LogicFixtures.draft.copy(imageKey="seed"))
        assertEquals("seed",api.productInputs.single().imageKey);assertTrue(id>0)
        val inputs=api.productInputs.size
        for(draft in listOf(LogicFixtures.draft.copy(priceClp=0),LogicFixtures.draft.copy(name="X"))) {
            try {repo.saveProduct(null,draft);fail("invalid saved")} catch(_:IllegalArgumentException) {}
        }
        try {ProductRepository(db,api).saveProduct(null,LogicFixtures.draft);fail("missing key")} catch(_:IllegalStateException) {}
        val local=db.saveProduct(null,LogicFixtures.draft.copy(name="Local sin vínculo"))
        try {repo.saveProduct(local,LogicFixtures.draft);fail("local collision")} catch(_:IllegalStateException) {}
        try {repo.saveProduct(99999,LogicFixtures.draft);fail("missing local")} catch(_:IllegalStateException) {}
        assertEquals(inputs,api.productInputs.size)
    }
    @Test fun httpAndCancellationDoNotInventSuccessfulLocalProducts()=runBlocking {
        val repo=ProductRepository(db,api) {key};repo.getProducts();val before=repo.cachedProducts()
        for(failure in listOf(LogicFixtures.http(403),CancellationException())) {
            api.writeFailure=failure
            try {repo.saveProduct(null,LogicFixtures.draft);fail("write failed")} catch(actual:Exception) {assertEquals(failure.javaClass,actual.javaClass)}
            assertEquals(before,repo.cachedProducts())
        }
    }
}
