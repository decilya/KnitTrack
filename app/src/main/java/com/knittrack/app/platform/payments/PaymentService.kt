package com.knittrack.app.platform.payments
import com.knittrack.app.core.common.Result
interface PaymentService {
    fun isReady(): Boolean
    suspend fun purchaseSubscription(productId: String): Result<Unit>
    fun checkSubscriptionStatus(): Boolean
}
