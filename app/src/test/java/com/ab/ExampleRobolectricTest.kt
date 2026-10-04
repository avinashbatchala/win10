package com.ab

import android.app.Application
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.ab.ui.viewmodel.LauncherViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Win10 Start", appName)
    }

    @Test
    fun `root back gesture does not finish launcher activity`() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            // Perform back press at root
            activity.onBackPressedDispatcher.onBackPressed()
            // Activity must remain alive and NOT finish
            assertFalse("Launcher activity must not finish on root back gesture", activity.isFinishing)
            assertFalse("Launcher activity must not be destroyed", activity.isDestroyed)
        }
    }

    @Test
    fun `handleInternalBack consumes in priority order`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val vm = LauncherViewModel(application)

        // At root with no modals/edit/search: should return false (consumed by root idle handler)
        assertFalse(vm.handleInternalBack())

        // 1. Settings open -> should consume
        vm.openSettings()
        assertTrue(vm.isSettingsOpen.value)
        assertTrue(vm.handleInternalBack())
        assertFalse(vm.isSettingsOpen.value)

        // 2. Edit mode active -> should consume
        vm.onTileLongClicked("test-tile-id")
        assertTrue(vm.isEditMode.value)
        assertTrue(vm.handleInternalBack())
        assertFalse(vm.isEditMode.value)

        // 3. Search query / active -> should consume
        vm.setSearchQuery("camera")
        assertTrue(vm.searchQuery.value.isNotEmpty())
        assertTrue(vm.handleInternalBack())
        assertTrue(vm.searchQuery.value.isEmpty())

        // 4. Jump list open -> should consume
        vm.openJumpList()
        assertTrue(vm.isJumpListOpen.value)
        assertTrue(vm.handleInternalBack())
        assertFalse(vm.isJumpListOpen.value)

        // Back at root again: returns false
        assertFalse(vm.handleInternalBack())
    }
}
