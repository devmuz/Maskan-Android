package com.maskan.mobileapp.ui.landlord.settings

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.billingclient.api.ProductDetails
import com.maskan.mobileapp.data.repository.SubscriptionProducts
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType

private data class FeatureRow(val icon: ImageVector, val color: Color, val title: String, val free: String, val pro: String)

private val featureRows = listOf(
    FeatureRow(Icons.Filled.Apartment, Color(0xFF4A389E), "Properties", "1 building", "Unlimited"),
    FeatureRow(Icons.Filled.Group, Color(0xFF30B0C7), "Co-owners", "1 per building", "Up to 7"),
    FeatureRow(Icons.Filled.Person, Color(0xFF1C944F), "Tenants", "Unlimited", "Unlimited"),
    FeatureRow(Icons.Filled.Description, Color(0xFFFF9500), "Bills & Payments", "✓", "✓"),
    FeatureRow(Icons.Filled.Notifications, Color(0xFFFF3B30), "Notifications", "✓", "✓"),
)

private val crownGold = Color(0xFFF2C24F)
private val columnWidth = 88.dp

private fun ProductDetails.basePhase() =
    subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()

/**
 * Pro paywall — same layout as iOS's `PaywallView`: gradient hero, Free vs
 * Pro feature table, Annual/Monthly plan picker, Subscribe Now. No Restore
 * Purchases button: Play purchases are re-checked automatically at every
 * landlord session start (LandlordViewModel). Backed by Play Billing; the cross-platform contract is what
 * `LandlordRepository.grantProEntitlement` writes to Firestore after a
 * purchase (08-landlord-settings.md). Closes itself once the landlord is Pro.
 */
@Composable
fun PaywallScreen(viewModel: LandlordViewModel, onBack: () -> Unit) {
    val colors = MaskanTheme.colors
    val activity = LocalContext.current as Activity
    val products by viewModel.purchaseRepository.products.collectAsStateWithLifecycle()
    val landlord by viewModel.landlord.collectAsStateWithLifecycle()
    var selectedProductId by remember { mutableStateOf(SubscriptionProducts.YEARLY) }

    LaunchedEffect(landlord?.isPro) {
        if (landlord?.isPro == true) onBack()
    }

    val monthly = products[SubscriptionProducts.MONTHLY]
    val yearly = products[SubscriptionProducts.YEARLY]

    // Computed from live Play prices so it never goes stale after a price change.
    val yearlySavingsPercent: Int? = run {
        val monthlyMicros = monthly?.basePhase()?.priceAmountMicros ?: return@run null
        val yearlyMicros = yearly?.basePhase()?.priceAmountMicros ?: return@run null
        val fullYear = monthlyMicros * 12
        if (monthlyMicros <= 0 || fullYear <= yearlyMicros) null
        else ((fullYear - yearlyMicros) * 100 / fullYear).toInt()
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.Cancel, contentDescription = "Close", tint = colors.textSecondary.copy(alpha = 0.7f), modifier = Modifier.size(26.dp))
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 40.dp)) {
            HeroSection()
            Spacer(modifier = Modifier.height(28.dp))
            FeaturesSection()
            Spacer(modifier = Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("CHOOSE YOUR PLAN", Modifier.padding(horizontal = 24.dp))
                if (products.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.gradientStart)
                    }
                } else {
                    PlanButton(
                        title = "Annual",
                        price = yearly?.basePhase()?.formattedPrice ?: "—",
                        badge = "Best Value",
                        detail = yearlySavingsPercent?.let { "Billed yearly  ·  save ~$it%" } ?: "Billed yearly",
                        selected = selectedProductId == SubscriptionProducts.YEARLY,
                        onClick = { selectedProductId = SubscriptionProducts.YEARLY },
                    )
                    PlanButton(
                        title = "Monthly",
                        price = monthly?.basePhase()?.formattedPrice ?: "—",
                        badge = null,
                        detail = "Billed monthly  ·  cancel anytime",
                        selected = selectedProductId == SubscriptionProducts.MONTHLY,
                        onClick = { selectedProductId = SubscriptionProducts.MONTHLY },
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val selectedProduct = products[selectedProductId]
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .height(MaskanDimens.buttonHeight)
                    .shadow(12.dp, RoundedCornerShape(MaskanDimens.cornerRadius), spotColor = Color(0xFF4D2680))
                    .clip(RoundedCornerShape(MaskanDimens.cornerRadius))
                    .background(colors.primaryGradient)
                    .clickable(enabled = selectedProduct != null) {
                        selectedProduct?.let { viewModel.purchaseRepository.launchPurchase(activity, it) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Subscribe Now",
                    style = MaskanType.bodyMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = if (selectedProduct != null) 1f else 0.6f),
                )
            }

            Text(
                text = "Subscription auto-renews. Cancel anytime in\nGoogle Play › Payments & subscriptions.",
                style = MaskanType.caption.copy(fontSize = 11.sp),
                color = colors.textSecondary.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun HeroSection() {
    val colors = MaskanTheme.colors
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaskanDimens.cornerRadius))
            .background(colors.primaryGradient)
            .padding(top = 20.dp, bottom = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.padding(vertical = 12.dp).size(75.dp).background(Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = crownGold, modifier = Modifier.size(40.dp))
        }
        Text(
            text = "Maskan Pro",
            style = MaskanType.screenTitle.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold),
            color = Color.White,
        )
        Text(
            text = "Manage unlimited properties and tenants\nwith no restrictions.",
            style = MaskanType.caption.copy(fontSize = 12.sp),
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun FeaturesSection() {
    val colors = MaskanTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("FEATURES", Modifier.weight(1f))
            Text(
                text = "Free",
                style = MaskanType.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(columnWidth),
            )
            Text(
                text = "Pro",
                style = MaskanType.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                color = colors.gradientStart,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(columnWidth),
            )
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(MaskanDimens.cornerRadius))
                .background(colors.fieldBackground),
        ) {
            featureRows.forEachIndexed { index, row ->
                FeatureRowView(row)
                if (index < featureRows.lastIndex) {
                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(start = 58.dp))
                }
            }
        }
    }
}

@Composable
private fun FeatureRowView(row: FeatureRow) {
    val colors = MaskanTheme.colors
    val proHighlighted = row.pro == "Unlimited" || row.pro == "Up to 7"
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(30.dp).background(row.color, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
            Icon(row.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Text(text = row.title, style = MaskanType.body.copy(fontSize = 14.sp), color = colors.textPrimary, modifier = Modifier.weight(1f))
        Text(
            text = row.free,
            style = MaskanType.caption.copy(fontSize = 12.sp),
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(columnWidth),
        )
        Text(
            text = row.pro,
            style = MaskanType.caption.copy(fontSize = 12.sp, fontWeight = if (proHighlighted) FontWeight.Bold else FontWeight.Normal),
            color = if (proHighlighted) colors.gradientStart else colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(columnWidth),
        )
    }
}

@Composable
private fun PlanButton(title: String, price: String, badge: String?, detail: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaskanTheme.colors
    val shape = RoundedCornerShape(MaskanDimens.cornerRadius)
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.fieldBackground)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.gradientStart else colors.border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(22.dp).border(2.dp, if (selected) colors.gradientStart else colors.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(modifier = Modifier.size(12.dp).background(colors.gradientStart, CircleShape))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = title, style = MaskanType.bodyMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                badge?.let {
                    Text(
                        text = it,
                        style = MaskanType.badge.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = colors.onAccent,
                        modifier = Modifier.background(colors.accentGradient, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            Text(text = detail, style = MaskanType.caption.copy(fontSize = 12.sp), color = colors.textSecondary)
        }
        Text(
            text = price,
            style = MaskanType.cardTitle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            color = if (selected) colors.gradientStart else colors.textPrimary,
        )
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaskanType.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
        color = MaskanTheme.colors.textSecondary,
        modifier = modifier,
    )
}
