package com.knittrac.app.platform.payments
import com.knittrac.app.core.common.Result
interface PaymentService {
    fun isReady(): Boolean
    suspend fun purchaseSubscription(productId: String): Result<Unit>
    fun checkSubscriptionStatus(): Boolean
}
