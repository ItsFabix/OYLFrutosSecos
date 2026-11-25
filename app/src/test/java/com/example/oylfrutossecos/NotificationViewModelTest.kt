package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.repository.NotificationRepository
import com.example.oylfrutossecos.ui.viewmodel.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest : LogicViewModelTest() {
    private val repo=mockk<NotificationRepository>()
    private val user=LogicFixtures.client.id
    private val notice=LogicFixtures.notice()
    private fun vm()=create(NotificationViewModel.Factory(repo,user),NotificationViewModel::class.java)
    @Test fun readsOnlyOwnersNoticesAndReadActionsRefreshUnreadCount() = runTest {
        coEvery { repo.list(user) } returns listOf(notice,LogicFixtures.notice(2,read=true))
        coEvery { repo.markRead(user,1) } returns Unit
        coEvery { repo.markAllRead(user) } returns Unit
        val vm=vm();assertTrue(vm.state.value.loading);advanceUntilIdle()
        assertEquals(1,vm.state.value.unreadCount);assertEquals(2,vm.state.value.items.size)
        coEvery { repo.list(user) } returns listOf(notice.copy(readAt=200))
        vm.read(1);advanceUntilIdle();assertEquals(0,vm.state.value.unreadCount)
        vm.readAll();assertTrue(vm.state.value.busy);vm.readAll();advanceUntilIdle()
        assertFalse(vm.state.value.busy);assertNull(vm.state.value.error)
        coVerify(exactly=1) { repo.markAllRead(user) }
        coVerify { repo.markRead(user,1) }
    }
    @Test fun loadReadAndReadAllFailuresHaveSpecificRetryableMessages() = runTest {
        coEvery { repo.list(user) } throws java.io.IOException()
        val vm=vm();advanceUntilIdle();assertEquals("No se pudieron cargar los avisos.",vm.state.value.error)
        coEvery { repo.markRead(user,1) } throws IllegalStateException()
        vm.read(1);advanceUntilIdle();assertTrue(vm.state.value.error!!.contains("marcar"))
        coEvery { repo.markAllRead(user) } throws java.io.IOException()
        vm.readAll();advanceUntilIdle();assertFalse(vm.state.value.busy)
        assertEquals("No se pudieron actualizar los avisos.",vm.state.value.error)
        coEvery { repo.list(user) } returns emptyList()
        vm.load();advanceUntilIdle();assertNull(vm.state.value.error);assertTrue(vm.state.value.items.isEmpty())
    }
    @Test fun announcementFiltersKindAndTimestampAndDoesNotInventMissingNotice() = runTest {
        coEvery { repo.list(user) } returns listOf(LogicFixtures.notice(1,NoticeKind.TEST,300),
            LogicFixtures.notice(2,NoticeKind.ORDER_CREATED,99),LogicFixtures.notice(3,NoticeKind.ORDER_CREATED,100))
        val vm=vm();advanceUntilIdle();val found=mutableListOf<Long>()
        vm.announceIfNew(NoticeKind.ORDER_CREATED,100) {found+=it.id};advanceUntilIdle()
        assertEquals(listOf(3L),found)
        vm.announceIfNew(NoticeKind.STOCK_LOW,0) {fail("missing notice")};advanceUntilIdle()
        coEvery { repo.list(user) } throws java.io.IOException()
        vm.announceIfNew(NoticeKind.TEST,0) {fail("failed read")};advanceUntilIdle()
        assertNotNull(vm.state.value.error)
    }
    @Test fun createTestDistinguishesAndroidPermissionAndCallbackFailureFromStorageFailure() = runTest {
        coEvery { repo.list(user) } returns listOf(notice)
        coEvery { repo.createTest(user) } returns notice
        val vm=vm();advanceUntilIdle()
        vm.createTest {assertEquals(notice,it);true};advanceUntilIdle()
        assertEquals("Aviso de comprobación enviado.",vm.state.value.message)
        vm.createTest {false};advanceUntilIdle();assertTrue(vm.state.value.message!!.contains("permite las notificaciones"))
        vm.createTest {throw SecurityException()};advanceUntilIdle()
        assertTrue(vm.state.value.message!!.contains("Aviso guardado"));assertNull(vm.state.value.error)
        coEvery { repo.createTest(user) } throws java.io.IOException()
        vm.createTest {throw AssertionError("storage failed")};advanceUntilIdle()
        assertFalse(vm.state.value.busy);assertNull(vm.state.value.message);assertNotNull(vm.state.value.error)
    }
    @Test fun busyBlocksAnotherTestAndCancellationNeverShowsAnOrdinaryError() = runTest {
        coEvery { repo.list(user) } returns emptyList()
        val gate=CompletableDeferred<AppNotice>()
        coEvery { repo.createTest(user) } coAnswers {gate.await()}
        val vm=vm();advanceUntilIdle();var callbacks=0
        vm.createTest {callbacks++;true};vm.createTest {throw AssertionError("duplicate")};runCurrent()
        assertTrue(vm.state.value.busy);gate.complete(notice);advanceUntilIdle()
        assertEquals(1,callbacks);coVerify(exactly=1) {repo.createTest(user)}
        coEvery { repo.markRead(user,1) } throws CancellationException()
        vm.read(1);advanceUntilIdle();assertNull(vm.state.value.error)
        coEvery { repo.list(user) } throws CancellationException()
        vm.load();vm.announceIfNew(NoticeKind.TEST,0) {fail("cancelled")};advanceUntilIdle()
        assertNull(vm.state.value.error)
    }
}
