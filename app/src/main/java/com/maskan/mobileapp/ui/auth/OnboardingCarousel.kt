package com.maskan.mobileapp.ui.auth

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maskan.mobileapp.data.model.Currencies
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

/*
 * Swipeable intro slides on the role-selection screen (mirrors iOS
 * OnboardingCarousel.swift). Every slide is a stack of layers (glow, hero
 * card, floating chips, title, subtitle) translated by a different multiple
 * of the page's swipe progress, so while dragging they drift apart at
 * different speeds (parallax).
 */

enum class OnboardingHero { PROPERTIES, RENT, TENANT, REQUESTS, INSIGHTS }

data class OnboardingChip(val icon: ImageVector, val text: String)

data class OnboardingSlide(
    val hero: OnboardingHero,
    val title: String,
    val subtitle: String,
    val chips: List<OnboardingChip>,
)

val onboardingSlides = listOf(
    OnboardingSlide(
        OnboardingHero.PROPERTIES,
        "All your properties,\none place",
        "Buildings, flats, shops and villas — organised the way you own them.",
        listOf(OnboardingChip(Icons.Filled.Apartment, "12 units"), OnboardingChip(Icons.Filled.Verified, "92% occupied")),
    ),
    OnboardingSlide(
        OnboardingHero.RENT,
        "Rent & bills,\non autopilot",
        "Monthly rent is generated for you. Add utility bills in seconds.",
        listOf(OnboardingChip(Icons.Filled.CalendarMonth, "Due on the 1st"), OnboardingChip(Icons.Filled.Bolt, "Electricity")),
    ),
    OnboardingSlide(
        OnboardingHero.TENANT,
        "Tenants stay\nin the loop",
        "Tenants see what's due and tell you the moment they've paid.",
        listOf(OnboardingChip(Icons.Filled.NotificationsActive, "Payment reported"), OnboardingChip(Icons.Filled.Person, "Tenant app")),
    ),
    OnboardingSlide(
        OnboardingHero.REQUESTS,
        "Repairs without\nthe phone calls",
        "Maintenance requests with status from initiated to completed.",
        listOf(OnboardingChip(Icons.Filled.Handyman, "Plumbing"), OnboardingChip(Icons.Filled.Schedule, "In progress")),
    ),
    OnboardingSlide(
        OnboardingHero.INSIGHTS,
        "Know where every\npayment goes",
        "Collected vs. expenses across your whole portfolio, month by month.",
        listOf(OnboardingChip(Icons.Filled.TrendingUp, "+18% this month"), OnboardingChip(Icons.Filled.History, "History")),
    ),
)

@Composable
fun OnboardingCarousel(pagerState: PagerState, slides: List<OnboardingSlide>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // "Remove animations" in accessibility settings sets the animator scale to 0.
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    HorizontalPager(state = pagerState, modifier = modifier) { page ->
        // 0 when the page is centred, -1 one page to the left, +1 to the right.
        val progress = (page - pagerState.currentPage) - pagerState.currentPageOffsetFraction
        OnboardingSlideView(slides[page], progress, reduceMotion)
    }
}

/** Stretching-capsule page indicator. */
@Composable
fun OnboardingPageIndicator(count: Int, selection: Int, modifier: Modifier = Modifier) {
    val colors = MaskanTheme.colors
    Row(
        modifier = modifier.semantics { contentDescription = "Page ${selection + 1} of $count" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(count) { index ->
            val selected = index == selection
            val width by animateDpAsState(
                targetValue = if (selected) 24.dp else 8.dp,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                label = "indicatorWidth",
            )
            Box(
                modifier = Modifier
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .then(if (selected) Modifier.background(colors.primaryGradient) else Modifier.background(colors.border)),
            )
        }
    }
}

// MARK: - Slide

@Composable
private fun OnboardingSlideView(slide: OnboardingSlide, progress: Float, reduceMotion: Boolean) {
    val colors = MaskanTheme.colors
    val distance = abs(progress).coerceAtMost(1f)
    val bob = if (reduceMotion) 0f else {
        val transition = rememberInfiniteTransition(label = "chipBob")
        val value by transition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "chipBobValue",
        )
        value
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        // Shrinks the 320dp illustration on short screens so the title and
        // subtitle below always get their full height.
        val heroScale = ((maxHeight.value - 190f) / 320f).coerceIn(0.7f, 1f)
        // Extra horizontal shift for a layer on top of the page's own movement.
        // Positive speeds outrun the swipe, negative ones lag behind it.
        fun shift(speed: Float) = if (reduceMotion) 0f else progress * widthPx * speed

        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp * heroScale)
                    .graphicsLayer { scaleX = heroScale; scaleY = heroScale },
                contentAlignment = Alignment.Center,
            ) {
                // Slowest layer: soft glow that almost stays put. A radial
                // gradient rather than Modifier.blur, which clips to the
                // layer's bounds and renders as a visible square.
                Box(
                    modifier = Modifier
                        .size(380.dp)
                        .graphicsLayer { translationX = shift(-0.7f) }
                        .background(
                            Brush.radialGradient(listOf(colors.gradientEnd.copy(alpha = 0.30f), colors.gradientStart.copy(alpha = 0.10f), Color.Transparent)),
                            CircleShape,
                        ),
                )

                OnboardingHeroCard(
                    hero = slide.hero,
                    modifier = Modifier
                        .width(250.dp)
                        .graphicsLayer {
                            translationX = shift(0.15f)
                            rotationZ = if (reduceMotion) 0f else progress * -10f
                            val scale = 1f - distance * 0.18f
                            scaleX = scale
                            scaleY = scale
                        },
                )

                slide.chips.getOrNull(0)?.let { chip ->
                    OnboardingChipView(
                        chip,
                        Modifier.graphicsLayer {
                            translationX = (-88).dp.toPx() + shift(0.9f)
                            translationY = (-128).dp.toPx() + bob * 6.dp.toPx()
                        },
                    )
                }
                slide.chips.getOrNull(1)?.let { chip ->
                    OnboardingChipView(
                        chip,
                        Modifier.graphicsLayer {
                            translationX = 92.dp.toPx() + shift(-0.45f)
                            translationY = 122.dp.toPx() - bob * 5.dp.toPx()
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .graphicsLayer { alpha = (1f - distance * 1.3f).coerceIn(0f, 1f) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = slide.title,
                    style = MaskanType.screenTitle.copy(fontSize = 26.sp, lineHeight = 32.sp),
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { translationX = shift(0.35f) },
                )
                Text(
                    text = slide.subtitle,
                    style = MaskanType.body,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp).graphicsLayer { translationX = shift(0.7f) },
                )
            }

            Spacer(modifier = Modifier.weight(0.6f))
        }
    }
}

@Composable
private fun OnboardingChipView(chip: OnboardingChip, modifier: Modifier = Modifier) {
    val colors = MaskanTheme.colors
    Row(
        modifier = modifier
            .shadow(12.dp, CircleShape, ambientColor = colors.shadowColor, spotColor = colors.shadowColor)
            .background(colors.surface, CircleShape)
            .border(BorderStroke(1.dp, colors.border), CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(chip.icon, contentDescription = null, tint = colors.gold, modifier = Modifier.size(14.dp))
        Text(chip.text, style = MaskanType.fieldLabel, color = colors.textPrimary, maxLines = 1)
    }
}

// MARK: - Hero cards

private val deviceCurrencyCode: String
    get() = runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull()
        ?.takeIf { Currencies.byCode(it) != null } ?: "USD"

/**
 * A small mock of the real screen each slide talks about, drawn with
 * shapes and icons so it follows light/dark mode.
 */
@Composable
private fun OnboardingHeroCard(hero: OnboardingHero, modifier: Modifier = Modifier) {
    MaskanCard(modifier = modifier, cornerRadius = 24.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (hero) {
                OnboardingHero.PROPERTIES -> PropertiesHero()
                OnboardingHero.RENT -> RentHero()
                OnboardingHero.TENANT -> TenantHero()
                OnboardingHero.REQUESTS -> RequestsHero()
                OnboardingHero.INSIGHTS -> InsightsHero()
            }
        }
    }
}

@Composable
private fun IconTile(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier.size(36.dp).background(tint.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun HeroRow(icon: ImageVector, title: String, detail: String, modifier: Modifier = Modifier, tint: Color = MaskanTheme.colors.gradientStart) {
    val colors = MaskanTheme.colors
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        IconTile(icon, tint)
        Column {
            Text(title, style = MaskanType.secondary.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary, maxLines = 1)
            Text(detail, style = MaskanType.caption.copy(fontWeight = FontWeight.Normal), color = colors.textSecondary, maxLines = 1)
        }
    }
}

@Composable
private fun Pill(text: String, color: Color) {
    Text(
        text = text,
        style = MaskanType.badge,
        color = color,
        modifier = Modifier.background(color.copy(alpha = 0.14f), CircleShape).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun HeroTitle(text: String) {
    Text(text, style = MaskanType.cardTitle, color = MaskanTheme.colors.textPrimary)
}

@Composable
private fun ColumnScope.PropertiesHero() {
    val colors = MaskanTheme.colors
    HeroTitle("Properties")
    HeroRow(Icons.Filled.Apartment, "Sunrise Apartments", "8 flats · 7 occupied")
    HeroRow(Icons.Filled.House, "Garden Villa", "Villa · Occupied", tint = colors.success)
    HeroRow(Icons.Filled.Storefront, "Corner Shop", "Shop · Vacant", tint = colors.gold)
}

@Composable
private fun ColumnScope.RentHero() {
    val colors = MaskanTheme.colors
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconTile(Icons.Filled.House, colors.gradientStart)
        Spacer(modifier = Modifier.weight(1f))
        Pill("Pending", colors.warning)
    }
    Column {
        Text("Rent · November", style = MaskanType.caption.copy(fontWeight = FontWeight.Normal), color = colors.textSecondary)
        Text("$deviceCurrencyCode 6,500", style = MaskanType.statValue.copy(fontSize = 26.sp), color = colors.textPrimary)
    }
    HorizontalDivider(color = colors.border)
    HeroRow(Icons.Filled.Bolt, "Electricity", "Paid by tenant", tint = colors.gold)
}

@Composable
private fun ColumnScope.TenantHero() {
    val colors = MaskanTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.primaryGradient, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Text("Hello, Sara", style = MaskanType.caption, color = Color.White.copy(alpha = 0.8f))
        Text("$deviceCurrencyCode 6,500", style = MaskanType.statValue, color = Color.White, modifier = Modifier.padding(top = 4.dp))
        Row(
            modifier = Modifier
                .padding(top = 10.dp)
                .background(Color.White, CircleShape)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = colors.indigoDeep, modifier = Modifier.size(14.dp))
            Text("I've Paid Rent", style = MaskanType.fieldLabel, color = colors.indigoDeep)
        }
    }
    HeroRow(Icons.Filled.Description, "Water bill", "Due 5 Nov")
}

@Composable
private fun ColumnScope.RequestsHero() {
    val colors = MaskanTheme.colors
    HeroTitle("Requests")
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HeroRow(Icons.Filled.WaterDrop, "Leaking tap", "Initiated 2 Nov", modifier = Modifier.weight(1f))
        Pill("In progress", colors.warning)
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HeroRow(Icons.Filled.Bolt, "Power socket", "Completed 28 Oct", modifier = Modifier.weight(1f), tint = colors.success)
        Pill("Done", colors.success)
    }
}

@Composable
private fun ColumnScope.InsightsHero() {
    val colors = MaskanTheme.colors
    val values = listOf(0.35f, 0.5f, 0.42f, 0.7f, 0.62f, 0.9f)
    val months = listOf("J", "J", "A", "S", "O", "N")
    Text("Collected vs. Expense", style = MaskanType.bodyMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { index, value ->
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(Modifier.size(width = 9.dp, height = (90 * value).dp).background(colors.gold, RoundedCornerShape(3.dp)))
                    Box(Modifier.size(width = 9.dp, height = (90 * value * 0.45f).dp).background(colors.gradientStart, RoundedCornerShape(3.dp)))
                }
                Text(months[index], style = MaskanType.badge.copy(fontWeight = FontWeight.Normal), color = colors.textTertiary, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
