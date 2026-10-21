package com.example.oylfrutossecos

import androidx.lifecycle.SavedStateHandle
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.domain.*
import com.example.oylfrutossecos.ui.viewmodel.*
import org.junit.Assert.*
import org.junit.Test

class AdminFormStateTest {
    private val product = Product(1, "Almendras", "Frutos secos", "Bolsa natural", 3490, 250, 5, "photo:existing.jpg")

    @Test fun correctingProductFieldKeepsAllUnrelatedErrors() {
        val vm = ProductFormViewModel(null, true, SavedStateHandle()) { _, _ -> fail("Invalid form saved") }
        vm.save()
        val before = vm.state.value.errors
        assertTrue(before.containsKey(ProductField.NAME))
        vm.change(ProductField.NAME, "Almendras")
        assertEquals(before - ProductField.NAME, vm.state.value.errors)
        assertFalse(vm.state.value.saving)
    }

    @Test fun productSaveLocksSynchronouslyAgainstRapidTapsAndInputChanges() {
        var calls = 0
        var finish: ((String?) -> Unit)? = null
        val vm = ProductFormViewModel(product, false, SavedStateHandle()) { draft, callback ->
            calls++; finish = callback
            assertNull(draft.imageKey) // null preserves the existing photo in SQLite.
            assertEquals(5, draft.stock)
        }
        vm.save(); vm.save()
        vm.change(ProductField.NAME, "Changed while saving")
        assertEquals(1, calls)
        assertTrue(vm.state.value.saving)
        assertEquals(product.name, vm.state.value.input.name)
        finish!!(null)
        vm.save()
        assertTrue(vm.state.value.completed)
        assertFalse(vm.state.value.saving)
        assertEquals(1, calls)
    }

    @Test fun productFailurePreservesValuesAndAllowsAnExplicitRetry() {
        var calls = 0
        val vm = ProductFormViewModel(product, false, SavedStateHandle()) { _, callback ->
            calls++; callback("Error SQLite")
        }
        vm.save()
        assertEquals("Error SQLite", vm.state.value.error)
        assertFalse(vm.state.value.locked)
        vm.save()
        assertEquals(2, calls)
    }

    @Test fun existingStockCannotBeEditedInProductForm() {
        val vm = ProductFormViewModel(product, false, SavedStateHandle()) { _, _ -> }
        vm.change(ProductField.STOCK, "999")
        assertEquals("5", vm.state.value.input.initialStock)
    }

    @Test fun cancellingCameraNeverRequestsDeletionOfExistingPhoto() {
        val vm = ProductFormViewModel(product, false, SavedStateHandle()) { _, _ -> }
        assertTrue(vm.beginPhotoCapture("temporary.jpg"))
        assertEquals(listOf("temporary.jpg"), vm.finishPhotoCapture(false))
        assertEquals(product.imageKey, vm.state.value.imageKey)
        assertFalse(vm.beginPhotoCapture("existing.jpg"))
        assertTrue(vm.discardPhoto().isEmpty())
    }

    @Test fun cancellingSecondCaptureKeepsPreviouslyAcceptedCandidate() {
        val vm = ProductFormViewModel(product, false, SavedStateHandle()) { _, _ -> }
        vm.beginPhotoCapture("candidate.jpg")
        assertTrue(vm.finishPhotoCapture(true).isEmpty())
        vm.beginPhotoCapture("cancelled.jpg")
        assertEquals(listOf("cancelled.jpg"), vm.finishPhotoCapture(false))
        assertEquals("photo:candidate.jpg", vm.state.value.imageKey)
        assertEquals(listOf("candidate.jpg"), vm.discardPhoto())
        assertEquals(product.imageKey, vm.state.value.imageKey)
    }

    @Test fun savedValuesAndPendingCameraSurviveFormRecreation() {
        val saved = SavedStateHandle()
        val original = ProductFormViewModel(product, false, saved) { _, _ -> }
        original.change(ProductField.NAME, "Nombre editado")
        original.beginPhotoCapture("temporary.jpg")
        val recreated = ProductFormViewModel(product, false, saved) { _, _ -> }
        assertEquals("Nombre editado", recreated.state.value.input.name)
        assertEquals(listOf("temporary.jpg"), recreated.finishPhotoCapture(false))
        assertEquals(product.imageKey, recreated.state.value.imageKey)
    }

    @Test fun cameraCannotLaunchDuringSaveAndSavedPhotoCannotBeDiscarded() {
        var finish: ((String?) -> Unit)? = null
        val vm = ProductFormViewModel(product, false, SavedStateHandle()) { _, callback -> finish = callback }
        vm.beginPhotoCapture("accepted.jpg"); vm.finishPhotoCapture(true)
        vm.save()
        assertFalse(vm.beginPhotoCapture("another.jpg"))
        assertTrue(vm.discardPhoto().isEmpty())
        finish!!(null)
        assertTrue(vm.discardPhoto().isEmpty())
    }

    @Test fun correctingInventoryFieldKeepsOtherErrorsIncludingType() {
        val vm = StockAdjustmentViewModel(5, SavedStateHandle()) { _, _, _, _ -> fail("Invalid form saved") }
        vm.change(InventoryField.TYPE, "INVALID")
        vm.save()
        assertEquals(InventoryField.entries.toSet(), vm.state.value.errors.keys)
        vm.change(InventoryField.QUANTITY, "2")
        assertEquals(setOf(InventoryField.TYPE, InventoryField.NOTE), vm.state.value.errors.keys)
        vm.change(InventoryField.TYPE, "ENTRY")
        assertEquals(setOf(InventoryField.NOTE), vm.state.value.errors.keys)
    }

    @Test fun validInventorySaveCannotRunTwiceAndPreservesExistingCalculation() {
        var calls = 0
        var finish: ((String?) -> Unit)? = null
        val vm = StockAdjustmentViewModel(5, SavedStateHandle()) { type, quantity, note, callback ->
            calls++; finish = callback
            assertEquals(MovementType.EXIT, type)
            assertEquals(2, quantity)
            assertEquals("Merma", note)
        }
        vm.change(InventoryField.TYPE, "EXIT")
        vm.change(InventoryField.QUANTITY, "2")
        vm.change(InventoryField.NOTE, " Merma ")
        assertEquals(3, vm.state.value.projectedStock)
        vm.save(); vm.save()
        vm.change(InventoryField.QUANTITY, "3")
        assertEquals("2", vm.state.value.input.quantity)
        assertEquals(1, calls)
        assertTrue(vm.state.value.saving)
        finish!!(null)
        vm.save()
        assertEquals(1, calls)
        assertTrue(vm.state.value.completed)
    }

    @Test fun inventoryValidationUsesLatestStockBeforeSaving() {
        val vm = StockAdjustmentViewModel(5, SavedStateHandle()) { _, _, _, _ -> fail("Oversale") }
        vm.change(InventoryField.TYPE, "EXIT")
        vm.change(InventoryField.QUANTITY, "5")
        vm.change(InventoryField.NOTE, "Merma")
        vm.updateStock(3)
        vm.save()
        assertEquals(setOf(InventoryField.QUANTITY), vm.state.value.errors.keys)
        assertFalse(vm.state.value.saving)
    }

    @Test fun inventoryFailureUnlocksFormAndKeepsValuesForRetry() {
        val saved = SavedStateHandle()
        val vm = StockAdjustmentViewModel(5, saved) { _, _, _, done -> done("Error local") }
        vm.change(InventoryField.QUANTITY, "2")
        vm.change(InventoryField.NOTE, "Reposición")
        vm.save()
        assertFalse(vm.state.value.locked)
        assertEquals("Error local", vm.state.value.error)
        val restored = StockAdjustmentViewModel(5, saved) { _, _, _, _ -> }
        assertEquals(vm.state.value.input, restored.state.value.input)
        assertFalse(restored.state.value.saving)
    }
}
