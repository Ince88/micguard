package com.polyhistor.micguard

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polyhistor.micguard.billing.BillingManager
import com.polyhistor.micguard.ui.theme.MicGuardTheme

class SupportActivity : ComponentActivity() {
    
    private lateinit var billingManager: BillingManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize billing manager
        billingManager = BillingManager(
            context = this,
            onPurchaseSuccess = { message ->
                showToast(message)
            },
            onPurchaseError = { error ->
                showToast(error)
            }
        )
        
        setContent {
            MicGuardTheme {
                SupportScreen(
                    billingManager = billingManager,
                    onBackClick = { finish() }
                )
            }
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        billingManager.destroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    billingManager: BillingManager,
    onBackClick: () -> Unit
) {
    val isReady by billingManager.isReady.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.support_developer_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        SupportContent(
            modifier = Modifier.padding(paddingValues),
            billingManager = billingManager,
            isReady = isReady
        )
    }
}

@Composable
fun SupportContent(
    modifier: Modifier = Modifier,
    billingManager: BillingManager,
    isReady: Boolean
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.support_developer_subtitle),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(R.string.support_description),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )
            }
        }
        
        // Tip options
        if (isReady) {
            TipOption(
                icon = "☕",
                title = stringResource(R.string.coffee_tip),
                description = "Small tip to keep me caffeinated",
                price = billingManager.getProductPrice(BillingManager.PRODUCT_SMALL),
                gradient = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF8B4513),
                        Color(0xFFD2691E)
                    )
                ),
                onClick = {
                    billingManager.launchBillingFlow(
                        activity = billingManager.context as Activity,
                        productId = BillingManager.PRODUCT_SMALL
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TipOption(
                icon = "🍕",
                title = stringResource(R.string.lunch_tip),
                description = "Medium tip for a developer lunch",
                price = billingManager.getProductPrice(BillingManager.PRODUCT_MEDIUM),
                gradient = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFF6B35),
                        Color(0xFFF7931E)
                    )
                ),
                onClick = {
                    billingManager.launchBillingFlow(
                        activity = billingManager.context as Activity,
                        productId = BillingManager.PRODUCT_MEDIUM
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TipOption(
                icon = "🛡️",
                title = stringResource(R.string.hero_tip),
                description = "Large tip to become a Privacy Hero",
                price = billingManager.getProductPrice(BillingManager.PRODUCT_LARGE),
                gradient = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF6A4C93),
                        Color(0xFF9B59B6)
                    )
                ),
                onClick = {
                    billingManager.launchBillingFlow(
                        activity = billingManager.context as Activity,
                        productId = BillingManager.PRODUCT_LARGE
                    )
                }
            )
        } else {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading tip options...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun TipOption(
    icon: String,
    title: String,
    description: String,
    price: String,
    gradient: Brush,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Text(
                    text = icon,
                    fontSize = 32.sp,
                    modifier = Modifier.padding(end = 16.dp)
                )
                
                // Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                
                // Price
                Text(
                    text = price,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

 