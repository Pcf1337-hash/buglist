package com.buglist.presentation.settlement

import com.buglist.domain.model.DebtEntry
import com.buglist.domain.model.DebtEntryWithPayments
import com.buglist.domain.model.Payment
import com.buglist.domain.model.Result
import com.buglist.domain.model.SettlementResult
import com.buglist.domain.repository.DebtRepository
import com.buglist.domain.usecase.AddDebtUseCase
import com.buglist.domain.usecase.SettleDebtsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettlementViewModelTest {
    private val repository = mockk<DebtRepository>()
    private val settle = mockk<SettleDebtsUseCase>()
    private val addDebt = mockk<AddDebtUseCase>()

    @Before fun mainDispatcher() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun resetDispatcher() { Dispatchers.resetMain() }
    private fun result(amount: Double) = SettlementResult(emptyList(), null, amount, 0.0)
    private fun debts(remaining: Double) = listOf(DebtEntryWithPayments.from(
        DebtEntry(id = 1, personId = 1, amount = 100.0, isOwedToMe = true, date = 1),
        if (remaining < 100) listOf(Payment(id = 1, debtEntryId = 1, amount = 100 - remaining)) else emptyList()
    ))

    @Test fun reopeningClearsOldSuccessBeforeLoadingAndDoesNotRepeatCompletion() = runTest {
        val vm = SettlementViewModel(settle, addDebt, repository)
        val events = mutableListOf<SettlementResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.completedSettlements.collect { events += it }
        }
        coEvery { repository.getOpenDebtsForPerson(1, true) } returns debts(100.0)
        coEvery { settle(1, 20.0, true) } returns Result.Success(result(20.0))
        vm.loadOpenDebts(1, true)
        runCurrent()
        vm.settleDebts(1, 20.0, true)
        runCurrent()
        assertTrue(vm.uiState.value is SettlementUiState.Success)
        assertEquals(1, events.size)

        val loading = CompletableDeferred<List<DebtEntryWithPayments>>()
        coEvery { repository.getOpenDebtsForPerson(1, true) } coAnswers { loading.await() }
        vm.loadOpenDebts(1, true)
        assertEquals(SettlementUiState.Idle(0.0, 0), vm.uiState.value)
        runCurrent()
        vm.updatePreview(0.0)
        assertEquals(1, events.size)
        loading.complete(debts(80.0))
        runCurrent()
        assertEquals(SettlementUiState.Idle(80.0, 1), vm.uiState.value)
        assertEquals(1, events.size)
    }

    @Test fun fastRepeatedConfirmBooksOnceAndInputCannotOverwriteProcessing() = runTest {
        val vm = SettlementViewModel(settle, addDebt, repository)
        coEvery { repository.getOpenDebtsForPerson(1, true) } returns debts(100.0)
        val booking = CompletableDeferred<Result<SettlementResult>>()
        coEvery { settle(1, 20.0, true) } coAnswers { booking.await() }
        vm.loadOpenDebts(1, true)
        runCurrent()
        vm.settleDebts(1, 20.0, true)
        vm.settleDebts(1, 20.0, true)
        vm.settleDebtsWithCounterDebt(1, 120.0, true, "EUR")
        vm.updatePreview(0.0)
        vm.loadOpenDebts(1, true)
        assertTrue(vm.uiState.value is SettlementUiState.Processing)
        runCurrent()
        coVerify(exactly = 1) { settle(any(), any(), any()) }
        coVerify(exactly = 0) { addDebt(any()) }
        booking.complete(Result.Success(result(20.0)))
        runCurrent()
        assertTrue(vm.uiState.value is SettlementUiState.Success)
    }

    @Test fun consecutivePaymentsEachEmitOnceAndOpenWithoutAnotherSuccess() = runTest {
        val vm = SettlementViewModel(settle, addDebt, repository)
        val events = mutableListOf<SettlementResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.completedSettlements.collect { events += it }
        }
        var remaining = 100.0
        coEvery { repository.getOpenDebtsForPerson(1, true) } coAnswers { debts(remaining) }
        coEvery { settle(1, any(), true) } coAnswers {
            val amount = secondArg<Double>()
            remaining -= amount
            Result.Success(result(amount))
        }
        listOf(20.0, 50.0).forEach { amount ->
            vm.loadOpenDebts(1, true)
            runCurrent()
            assertTrue(vm.uiState.value is SettlementUiState.Idle)
            vm.settleDebts(1, amount, true)
            runCurrent()
        }
        vm.loadOpenDebts(1, true)
        runCurrent()
        assertEquals(SettlementUiState.Idle(30.0, 1), vm.uiState.value)
        assertEquals(listOf(20.0, 50.0), events.map { it.totalSettled })
        coVerify(exactly = 2) { settle(any(), any(), any()) }
    }

    @Test fun failureAndZeroBookingDoNotEmitSuccessAndSuccessIsNotReplayed() = runTest {
        val vm = SettlementViewModel(settle, addDebt, repository)
        val events = mutableListOf<SettlementResult>()
        coEvery { repository.getOpenDebtsForPerson(1, true) } returns debts(100.0)
        coEvery { settle(1, 20.0, true) } returnsMany listOf(
            Result.Error("Nicht gebucht"), Result.Success(result(0.0)), Result.Success(result(20.0))
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.completedSettlements.collect { events += it }
        }
        repeat(2) {
            vm.loadOpenDebts(1, true)
            runCurrent()
            vm.settleDebts(1, 20.0, true)
            runCurrent()
            assertTrue(vm.uiState.value is SettlementUiState.Error)
            assertTrue(events.isEmpty())
        }
        vm.loadOpenDebts(1, true)
        runCurrent()
        vm.settleDebts(1, 20.0, true)
        runCurrent()
        assertEquals(1, events.size)
        val lateEvents = mutableListOf<SettlementResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.completedSettlements.collect { lateEvents += it }
        }
        runCurrent()
        assertTrue(lateEvents.isEmpty())
    }
}
