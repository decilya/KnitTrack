package com.knittrac.app.presentation.common

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Модификатор клика с тактильным откликом.
 *
 * Единая точка haptic-отклика для UI-элементов с `Modifier.clickable`:
 * перед вызовом [onClick] выполняется короткая вибрация.
 *
 * Тип по умолчанию [HapticFeedbackType.LongPress] — «резкий тук», стандарт
 * для кнопок и карточек. Для мягких элементов (RadioButton, Switch)
 * рекомендуется [HapticFeedbackType.TextHandleMove].
 *
 * @param type Тип вибро-отклика.
 * @param enabled Если false — haptic не выполняется.
 * @param onClick Действие после haptic-отклика.
 */
@Composable
fun Modifier.hapticClickable(
    type: HapticFeedbackType = HapticFeedbackType.LongPress,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val haptic = LocalHapticFeedback.current
    return this.clickable(enabled = enabled) {
        haptic.performHapticFeedback(type)
        onClick()
    }
}
