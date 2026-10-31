package com.knittrac.app

import com.knittrac.app.domain.service.AddPatternOperationService
import com.knittrac.app.domain.service.AddPatternOperationUseCase
import com.knittrac.app.domain.service.MarkNextRowUseCase
import com.knittrac.app.domain.service.MarkRowUseCase
import com.knittrac.app.domain.service.ProgressService
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

/**
 * Smoke-тест Hilt-графа (7g-A).
 *
 * Гарантирует, что граф собирается в runtime: если хотя бы один
 * inject-таргет или его транзитивные зависимости отсутствуют в графе,
 * hiltRule.inject() бросает в @Before, тест падает.
 *
 * Регрессия: коммит 1b0c411 — PatternDao не был провайден в DatabaseModule,
 * но Hilt ленив, и сборка оставалась зелёной. Тест закрывает P1 #2.
 *
 * MarkRowUseCase и AddPatternOperationUseCase (dead code до 7f-B)
 * инжектятся намеренно как reachability-target — D5 в HANDOVER §8.4.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class HiltGraphTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var progressService: ProgressService

    @Inject
    lateinit var addPatternOperationService: AddPatternOperationService

    @Inject
    lateinit var markNextRowUseCase: MarkNextRowUseCase

    @Inject
    lateinit var markRowUseCase: MarkRowUseCase

    @Inject
    lateinit var addPatternOperationUseCase: AddPatternOperationUseCase

    /** Триггерит inject() — без него Hilt-компонент не построит граф. */
    @Before
    fun setUp() {
        hiltRule.inject()
    }

    /**
     * Все 5 ключевых зависимостей сконструированы Hilt без ошибок.
     * Если @Before прошёл — поля non-null; assertions фиксируют намерение.
     */
    @Test
    fun hiltGraph_constructsAllCoreServices() {
        assertNotNull(progressService)
        assertNotNull(addPatternOperationService)
        assertNotNull(markNextRowUseCase)
        assertNotNull(markRowUseCase)
        assertNotNull(addPatternOperationUseCase)
    }
}
