package com.knittrack.app.platform.payments
import com.knittrack.app.core.common.Result
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class NoOpPaymentService @Inject constructor() : PaymentService {
    override fun isReady(): Boolean = true
    override suspend fun purchaseSubscription(productId: String): Result<Unit> = Result.Success(Unit)
    override fun checkSubscriptionStatus(): Boolean = false
}
