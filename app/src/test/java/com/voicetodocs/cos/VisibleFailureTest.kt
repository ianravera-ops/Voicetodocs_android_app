package com.voicetodocs.cos

import com.voicetodocs.cos.data.CosException
import com.voicetodocs.cos.data.VisibleFailure
import com.voicetodocs.cos.ui.home.HomeViewModel
import com.voicetodocs.cos.ui.record.RecordViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VisibleFailureTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setMain() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun reset() {
        Dispatchers.resetMain()
    }

    @Test
    fun geminiAndDriveErrorsStayUserVisible() {
        val gemini = VisibleFailure.of(CosException("Gemini could not finish: 503"))
        val drive = VisibleFailure.of(CosException("Google returned an error: 401"))
        assertEquals("Gemini could not finish: 503", gemini.message)
        assertEquals("Google returned an error: 401", drive.message)
        assertTrue(gemini.message.isNotBlank())
        assertTrue(drive.message.isNotBlank())
    }

    @Test
    fun recordProcessShowsErrorAndAllowsRetry() = runTest {
        val vm = RecordViewModel()
        vm.process({ "Listening with Gemini…" }) {
            throw CosException("Gemini could not finish: 500 boom")
        }
        assertEquals("Gemini could not finish: 500 boom", vm.state.value.error)
        assertFalse(vm.state.value.done)
        assertFalse(vm.state.value.busy)

        vm.process({ step -> if (step.name == "DONE") "Saved. You can open the note." else step.name }) { onStep ->
            onStep(com.voicetodocs.cos.data.pipeline.MemoStep.DONE)
        }
        assertNull(vm.state.value.error)
        assertTrue(vm.state.value.done)
        assertEquals("Saved. You can open the note.", vm.state.value.status)
    }

    @Test
    fun inboxLoadShowsErrorAndAllowsRetry() = runTest {
        val vm = HomeViewModel()
        vm.load { throw CosException("Google returned an error: 403") }
        assertEquals("Google returned an error: 403", vm.state.value.error)
        assertFalse(vm.state.value.loading)

        vm.load {
            vm.showMail(emptyList())
        }
        assertNull(vm.state.value.error)
        assertFalse(vm.state.value.loading)
    }
}
