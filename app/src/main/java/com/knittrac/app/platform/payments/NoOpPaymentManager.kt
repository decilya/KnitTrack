package com.knittrac.app.platform.payments
import com.knittrac.app.core.common.Result
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class NoOpPaymentManager @Inject constructor() : PaymentManager {
    override fun isReady(): Boolean = true
    override suspend fun purchaseSubscription(productId: String): Result<Unit> = Result.Success(Unit)
    override fun checkSubscriptionStatus(): Boolean = false
}
