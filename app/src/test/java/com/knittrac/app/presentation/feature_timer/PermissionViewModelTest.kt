package com.knittrac.app.presentation.feature_timer

import com.knittrac.app.platform.notification.NotificationPermissionChecker
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты для [PermissionViewModel].
 *
 * Проверяют делегирование [PermissionViewModel.isGranted] в
 * [NotificationPermissionChecker] без искажений.
 */
class PermissionViewModelTest {

    private val checker: NotificationPermissionChecker = mockk()
    private val viewModel = PermissionViewModel(checker)

    @Test
    fun isGranted_returnsTrue_whenCheckerGrants() {
        every { checker.isGranted() } returns true

        assertTrue(viewModel.isGranted())
    }

    @Test
    fun isGranted_returnsFalse_whenCheckerDenies() {
        every { checker.isGranted() } returns false

        assertFalse(viewModel.isGranted())
    }
}
