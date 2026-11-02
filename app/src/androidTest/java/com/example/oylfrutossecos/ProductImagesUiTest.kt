package com.example.oylfrutossecos

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.oylfrutossecos.data.model.Product
import com.example.oylfrutossecos.ui.components.ProductImage
import com.example.oylfrutossecos.ui.theme.OYLFrutosSecosTheme
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ProductImagesUiTest {
    @get:Rule val compose = createComposeRule()
    private val product = Product(99, "Almendras", "Frutos secos", "Bolsa", 3490, 250, 4, "nut")

    @Test fun packagedPhotosAndCreditsAreGoneWhileAllIllustrationsRender() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val removed = listOf("oyl_photo_almond", "oyl_photo_walnut", "oyl_photo_peanut", "oyl_photo_cashew",
            "oyl_photo_raisin", "oyl_photo_cranberry", "oyl_photo_chia", "oyl_photo_pumpkin", "oyl_welcome_background")
        removed.forEach { assertEquals(it, 0, context.resources.getIdentifier(it, "drawable", context.packageName)) }
        try {
            context.assets.open("PHOTO_CREDITS.md").use { fail("Credits still packaged") }
        } catch (_: FileNotFoundException) { }
        compose.setContent { OYLFrutosSecosTheme {
            Column { listOf("nut", "fruit", "seed").forEachIndexed { index, key ->
                ProductImage(product.copy(id = index.toLong(), imageKey = key), Modifier.size(80.dp))
            } }
        } }
        for (id in 0..2) compose.onNodeWithTag("product-placeholder-$id").assertIsDisplayed()
    }

    @Test fun privatePhotoHasPriorityAndItsBytesArePreserved() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val isolated = File(base.cacheDir, "private-photo-ui/${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(base) { override fun getFilesDir(): File = isolated }
        val file = File(isolated, "product_photos/own.jpg").apply { parentFile!!.mkdirs() }
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(210, 30, 100)) }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        bitmap.recycle()
        val bytes = file.readBytes()
        val photographed = product.copy(imageKey = "photo:own.jpg")
        compose.setContent { CompositionLocalProvider(LocalContext provides context) {
            OYLFrutosSecosTheme { ProductImage(photographed, Modifier.size(80.dp).testTag("photo-slot")) }
        } }
        compose.onNodeWithTag("product-photo-99").assertIsDisplayed()
        compose.onNodeWithTag("product-placeholder-99").assertDoesNotExist()
        val image = compose.onNodeWithTag("photo-slot").captureToImage().asAndroidBitmap()
        val pixel = image.getPixel(image.width / 2, image.height / 2)
        assertTrue(Color.red(pixel) > 190 && Color.green(pixel) < 50 && Color.blue(pixel) in 80..120)
        assertArrayEquals(bytes, file.readBytes())
        assertEquals("photo:own.jpg", photographed.imageKey)
    }

    @Test fun unavailablePhotoFallsBackWithoutChangingItsReference() {
        val key = "photo:missing-${UUID.randomUUID()}.jpg"
        val photographed = product.copy(imageKey = key)
        compose.setContent { OYLFrutosSecosTheme { ProductImage(photographed, Modifier.size(80.dp)) } }
        compose.onNodeWithTag("product-placeholder-99").assertIsDisplayed()
        compose.onNodeWithTag("product-photo-99").assertDoesNotExist()
        assertEquals(key, photographed.imageKey)
    }
}
