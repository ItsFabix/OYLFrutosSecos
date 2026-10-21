package com.example.oylfrutossecos

import android.app.Activity
import android.graphics.Bitmap
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.domain.*
import com.example.oylfrutossecos.ui.components.ProductPhotoStore
import com.example.oylfrutossecos.ui.screens.EditProductScreen
import com.example.oylfrutossecos.ui.screens.StockAdjustmentScreen
import com.example.oylfrutossecos.ui.viewmodel.*
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Forms are rendered without MainActivity, real users, or an application database. */
class AdminFormsUiTest {
    @get:Rule val compose = createComposeRule()
    private val product = Product(1, "Almendras", "Frutos secos", "Bolsa natural", 3490, 250, 5, "nut")
    private val hasError = SemanticsMatcher.keyIsDefined(SemanticsProperties.Error)

    @Test fun productErrorsAppearAtFieldsAndEditingNameKeepsPriceError() {
        val vm = ProductFormViewModel(null, true, SavedStateHandle()) { _, _ -> fail("Invalid product saved") }
        compose.setContent { MaterialTheme { EditProductScreen(null, vm, {}) } }
        compose.onNodeWithText("Guardar producto").performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasText("Nombre")).assert(hasError)
            .performScrollTo().performTextInput("Almendras")
        compose.onNode(hasSetTextAction() and hasText("Nombre")).assert(hasError.not())
        compose.onNode(hasSetTextAction() and hasText("Precio en pesos")).assert(hasError)
        compose.onNodeWithText("Ingresa el precio.").performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            assertFalse(vm.state.value.errors.containsKey(ProductField.NAME))
            assertTrue(vm.state.value.errors.containsKey(ProductField.DESCRIPTION))
        }
    }

    @Test fun validProductSaveDisablesButtonAndOnlyRunsOnce() {
        var calls = 0
        val vm = ProductFormViewModel(product, false, SavedStateHandle()) { _, _ -> calls++ }
        compose.setContent { MaterialTheme { EditProductScreen(product, vm, {}) } }
        compose.onNodeWithText("Guardar producto").performScrollTo().performClick()
        compose.onNodeWithText("Guardando…").assertIsNotEnabled()
        compose.runOnIdle { vm.save(); assertEquals(1, calls) }
    }

    @Test fun inventoryErrorsAreIndependentAndValidFieldsAllowSaving() {
        var calls = 0
        val vm = StockAdjustmentViewModel(5, SavedStateHandle(mapOf("type" to "INVALID"))) { type, amount, note, _ ->
            calls++
            assertEquals(MovementType.ENTRY, type)
            assertEquals(2, amount)
            assertEquals("Reposición", note)
        }
        compose.setContent { MaterialTheme { StockAdjustmentScreen(product, vm, {}) } }
        compose.onNodeWithText("Guardar movimiento").performScrollTo().performClick()
        compose.onNodeWithText("Selecciona Entrada o Salida.").performScrollTo().assertIsDisplayed()
        compose.onNode(hasSetTextAction() and hasText("Cantidad de bolsas")).assert(hasError)
            .performScrollTo().performTextInput("2")
        compose.onNode(hasSetTextAction() and hasText("Motivo")).assert(hasError)
        compose.onNodeWithText("Entrada").performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasText("Motivo")).performScrollTo().performTextInput("Reposición")
        compose.onNodeWithText("Guardar movimiento").performScrollTo().performClick()
        compose.onNodeWithText("Guardando…").assertIsNotEnabled()
        compose.runOnIdle { vm.save(); assertEquals(1, calls) }
    }

    @Test fun inventoryShowsInsufficientStockBesideQuantityAndPreservesValidNote() {
        val vm = StockAdjustmentViewModel(5, SavedStateHandle()) { _, _, _, _ -> fail("Insufficient stock saved") }
        compose.setContent { MaterialTheme { StockAdjustmentScreen(product, vm, {}) } }
        compose.onNodeWithText("Salida").performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasText("Cantidad de bolsas")).performScrollTo().performTextInput("6")
        compose.onNode(hasSetTextAction() and hasText("Motivo")).performScrollTo().performTextInput("Merma")
        compose.onNodeWithText("Guardar movimiento").performScrollTo().performClick()
        compose.onNodeWithText("Stock insuficiente: solo quedan 5 bolsas.").performScrollTo().assertIsDisplayed()
        compose.onNode(hasSetTextAction() and hasText("Cantidad de bolsas")).assert(hasError)
        compose.onNode(hasSetTextAction() and hasText("Motivo")).assert(hasError.not())
    }

    @Test fun cancelledCameraThroughFileProviderLeavesExistingPhotoBytesUntouched() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // A unique test-owned file. No existing user photo is read, overwritten, or deleted.
        val (name, uri) = ProductPhotoStore.create(context)
        assertEquals("content", uri.scheme)
        val file = File(context.filesDir, "product_photos/$name")
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        val before = file.readBytes()
        val photographed = product.copy(imageKey = "photo:$name")
        val vm = ProductFormViewModel(photographed, false, SavedStateHandle()) { _, _ -> fail("Unexpected save") }
        var launches = 0
        val registry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>,
                input: I, options: ActivityOptionsCompat?) {
                launches++
                dispatchResult(requestCode, Activity.RESULT_CANCELED, null)
            }
        }
        val owner = object : ActivityResultRegistryOwner { override val activityResultRegistry = registry }
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                MaterialTheme { EditProductScreen(photographed, vm, {}) }
            }
        }
        compose.onNodeWithText("Tomar fotografía").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, launches)
            assertEquals(photographed.imageKey, vm.state.value.imageKey)
            assertNull(vm.state.value.pendingPhoto)
            assertNull(vm.state.value.selectedPhoto)
            assertTrue(file.isFile)
            assertArrayEquals(before, file.readBytes())
        }
    }
}
