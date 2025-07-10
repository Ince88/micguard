package com.polyhistor.micguard.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager(
    val context: Context,
    private val onPurchaseSuccess: (String) -> Unit,
    private val onPurchaseError: (String) -> Unit
) : PurchasesUpdatedListener, BillingClientStateListener {

    companion object {
        private const val TAG = "BillingManager"
        
        // Product IDs for in-app purchases
        const val PRODUCT_SMALL = "support_small"
        const val PRODUCT_MEDIUM = "support_medium" 
        const val PRODUCT_LARGE = "support_large"
    }

    private var billingClient: BillingClient? = null
    
    private val _productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetails: StateFlow<List<ProductDetails>> = _productDetails.asStateFlow()
    
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        initializeBillingClient()
    }

    private fun initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        
        connectToBillingService()
    }

    private fun connectToBillingService() {
        billingClient?.startConnection(this)
    }



    override fun onBillingServiceDisconnected() {
        Log.d(TAG, "Billing service disconnected")
        _isReady.value = false
    }

    private fun loadProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_SMALL)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_MEDIUM)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_LARGE)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Product details loaded: ${productDetailsList.size} products")
                _productDetails.value = productDetailsList
            } else {
                Log.e(TAG, "Failed to load product details: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productId: String) {
        val productDetails = _productDetails.value.find { it.productId == productId }
        
        if (productDetails == null) {
            Log.e(TAG, "Product details not found for: $productId")
            onPurchaseError("Product not available")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
        
        if (billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Failed to launch billing flow: ${billingResult?.debugMessage}")
            onPurchaseError("Failed to start purchase")
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled purchase")
                onPurchaseError("Purchase canceled")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Item already owned")
                onPurchaseError("Item already owned")
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
                onPurchaseError("Purchase failed: ${billingResult.debugMessage}")
            }
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "Billing client connected successfully")
            _isReady.value = true
            loadProductDetails()
            // Query for any pending purchases
            queryPendingPurchases()
        } else {
            Log.e(TAG, "Failed to connect billing client: ${billingResult.debugMessage}")
            _isReady.value = false
        }
    }

    private fun queryPendingPurchases() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }
            
            // Get the product name for the thank you message
            val productName = when (purchase.products.firstOrNull()) {
                PRODUCT_SMALL -> "Coffee"
                PRODUCT_MEDIUM -> "Lunch" 
                PRODUCT_LARGE -> "Privacy Hero status"
                else -> "Support"
            }
            
            onPurchaseSuccess("Thank you for the $productName tip! 🎉")
            
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "Purchase is pending")
            onPurchaseError("Purchase is pending approval")
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged successfully")
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
            }
        }
    }

    fun getProductPrice(productId: String): String {
        return _productDetails.value.find { it.productId == productId }
            ?.oneTimePurchaseOfferDetails?.formattedPrice ?: "N/A"
    }

    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
    }
} 