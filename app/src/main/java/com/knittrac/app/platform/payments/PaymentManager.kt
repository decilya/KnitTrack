package com.knittrac.app.platform.payments
import com.knittrac.app.core.common.Result
interface PaymentManager {
    fun isReady(): Boolean
    suspend fun purchaseSubscription(productId: String): Result<Unit>
    fun checkSubscriptionStatus(): Boolean
}
