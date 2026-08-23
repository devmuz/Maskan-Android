package com.maskan.mobileapp.ui.landlord.settings

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.repository.SubscriptionProducts
import com.maskan.mobileapp.ui.components.GradientButton
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType

/**
 * Product list + purchase, backed by Play Billing (08-landlord-settings.md
 * notes the purchase-flow UI itself is an implementation detail to redesign
 * around Play Billing — the cross-platform contract is what gets written to
 * Firestore after a successful purchase, handled by
 * `LandlordRepository.grantProEntitlement`).
 */
@Composable
fun PaywallScreen(viewModel: LandlordViewModel, onBack: () -> Unit) {
    val colors = MaskanTheme.colors
    val activity = LocalContext.current as Activity
    val products by viewModel.purchaseRepository.products.collectAsStateWithLifecycle()
    var selectedProductId by remember { mutableStateOf(SubscriptionProducts.YEARLY) }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary) }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = MaskanDimens.screenHPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(16.dp, CircleShape, ambientColor = colors.gold, spotColor = colors.gold)
                    .background(colors.accentGradient, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(34.dp))
            }
            Text(
                text = "Unlock Maskan Pro",
                style = MaskanType.sectionTitle,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = "Unlimited properties, co-owners, and every feature across your whole portfolio.",
                style = MaskanType.body,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )

            Column(modifier = Modifier.fillMaxWidth().padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(MaskanDimens.itemSpacing)) {
                if (products.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.gradientStart)
                    }
                } else {
                    products[SubscriptionProducts.YEARLY]?.let { product ->
                        PlanCard(
                            title = "Yearly",
                            price = product.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "—",
                            badge = "Best value",
                            selected = selectedProductId == SubscriptionProducts.YEARLY,
                            onClick = { selectedProductId = SubscriptionProducts.YEARLY },
                        )
                    }
                    products[SubscriptionProducts.MONTHLY]?.let { product ->
                        PlanCard(
                            title = "Monthly",
                            price = product.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "—",
                            badge = null,
                            selected = selectedProductId == SubscriptionProducts.MONTHLY,
                            onClick = { selectedProductId = SubscriptionProducts.MONTHLY },
                        )
                    }
                }
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))

            GradientButton(
                text = "Continue",
                enabled = products.containsKey(selectedProductId),
                onClick = {
                    products[selectedProductId]?.let { viewModel.purchaseRepository.launchPurchase(activity, it) }
                },
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun PlanCard(title: String, price: String, badge: String?, selected: Boolean, onClick: () -> Unit) {
    val colors = MaskanTheme.colors
    MaskanCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        cornerRadius = MaskanDimens.cornerRadius,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = title, style = MaskanType.cardTitle, color = colors.textPrimary)
                    badge?.let {
                        Text(
                            text = it,
                            style = MaskanType.badge,
                            color = colors.onAccent,
                            modifier = Modifier.background(colors.accentGradient, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                Text(text = price, style = MaskanType.bodyMedium, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(if (selected) colors.gradientStart else Color.Transparent, CircleShape)
                    .then(if (!selected) Modifier.background(colors.fieldBackground, CircleShape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}
