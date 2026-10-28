package com.example.oylfrutossecos

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.ui.screens.OrdersScreen
import com.example.oylfrutossecos.ui.viewmodel.OrderUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AdminOrdersUiTest {
    @get:Rule val compose = createComposeRule()
    private fun order(remote: String? = "server-1") = OrderSummary(1, 2, "Cliente", 0,
        OrderStatus.PENDING, DeliveryMethod.PICKUP, "", "", 3490,
        listOf(OrderLine(1, "Almendras", 1, 3490)), remote)

    @Test fun remoteOrderOffersAllTransitionsAndDisplaysRefreshedState() {
        var state by mutableStateOf(OrderUiState(orders = listOf(order()), loading = false, remoteFresh = true))
        compose.setContent {
            MaterialTheme { OrdersScreen(state, true, {}, {}, { selected ->
                state = state.copy(orders = listOf(selected.copy(status = selected.status.next()!!)))
            }) }
        }
        for (status in listOf(OrderStatus.CONFIRMED, OrderStatus.PREPARED, OrderStatus.DELIVERED)) {
            compose.onNodeWithText("Marcar como ${status.displayName}").performScrollTo().performClick()
            compose.onNodeWithText("Estado: ${status.displayName}").assertExists()
        }
        compose.onAllNodesWithText("Marcar como", substring = true).assertCountEquals(0)
    }

    @Test fun offlineRestCannotAdvance() {
        compose.setContent {
            MaterialTheme { OrdersScreen(OrderUiState(orders = listOf(order()), loading = false), true, {}, {}, {}) }
        }
        compose.onNodeWithText("Marcar como Confirmado").performScrollTo().assertIsNotEnabled()
    }

    @Test fun localOrderKeepsAdvanceButtonWithoutAdminRestKey() {
        var selected: OrderSummary? = null
        compose.setContent {
            MaterialTheme { OrdersScreen(OrderUiState(orders = listOf(order(null)), loading = false), true,
                {}, {}, { selected = it }) }
        }
        compose.onNodeWithText("Marcar como Confirmado").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertNull(selected!!.remoteOrderId) }
    }

    @Test fun keyIsPassedOnlyOnSubmitAndInputIsCleared() {
        var submitted = ""
        compose.setContent {
            MaterialTheme { OrdersScreen(OrderUiState(loading = false), true, {}, {}, {}, { submitted = it }) }
        }
        compose.onNodeWithText("Clave administrativa REST").performTextInput("ui-test-only")
        compose.runOnIdle { assertEquals("", submitted) }
        compose.onNodeWithText("Usar clave y consultar").performClick()
        compose.runOnIdle { assertEquals("ui-test-only", submitted) }
        compose.onNodeWithText("Clave administrativa REST").assert(
            SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText,
                androidx.compose.ui.text.AnnotatedString("")))
    }
}
