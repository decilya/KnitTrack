package com.knittrac.app.core.base
import io.mockk.MockKAnnotations
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
abstract class BaseTest {
    @get:org.junit.Rule val coroutineRule = CoroutineTestRule()
    @Before fun setUp() { MockKAnnotations.init(this, relaxUnitFun = true) }
    @After fun tearDown() { unmockkAll() }
}
