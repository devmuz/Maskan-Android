package com.maskan.mobileapp.ui.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.maskan.mobileapp.R
import com.maskan.mobileapp.ui.components.GradientButton
import com.maskan.mobileapp.ui.nav.Routes
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType

@Composable
fun RoleSelectScreen(navController: NavHostController) {
    val colors = MaskanTheme.colors
    val slides = onboardingSlides
    val pagerState = rememberPagerState(pageCount = { slides.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Static brand header; only the carousel below swipes.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 36.dp)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(55.dp)
                    .shadow(10.dp, RoundedCornerShape(15.dp), ambientColor = colors.shadowColor, spotColor = colors.shadowColor)
                    .clip(RoundedCornerShape(15.dp)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Maskan",
                    style = MaskanType.screenTitle.copy(fontSize = 24.sp, lineHeight = 28.sp),
                    color = colors.textPrimary,
                )
                Text(text = "Your Rental Property Manager", style = MaskanType.secondary, color = colors.textSecondary)
            }
        }

        OnboardingCarousel(pagerState = pagerState, slides = slides, modifier = Modifier.weight(1f))

        OnboardingPageIndicator(
            count = slides.size,
            selection = pagerState.currentPage,
            modifier = Modifier.padding(vertical = 20.dp),
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            // Landlord is the primary choice (primaryGradient, not the gold
            // accentGradient — these are navigation choices, not a form's one
            // CTA, 03-auth.md); Tenant is the outlined secondary.
            GradientButton(
                text = "I am a Landlord",
                onClick = { navController.navigate(Routes.LANDLORD_LOGIN) },
                brush = colors.primaryGradient,
                contentColor = Color.White,
                leadingIcon = { Icon(Icons.Filled.Apartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(RoleButtonIconSize)) },
                height = RoleButtonHeight,
                textStyle = RoleButtonTextStyle,
            )
            SecondaryRoleButton(
                text = "I am a Tenant",
                onClick = { navController.navigate(Routes.TENANT_LOGIN) },
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun SecondaryRoleButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaskanTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 380f),
        label = "secondaryButtonScale",
    )
    val shape = RoundedCornerShape(MaskanDimens.cornerRadius)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RoleButtonHeight)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(colors.surface)
            .border(BorderStroke(1.5.dp, colors.gradientStart.copy(alpha = 0.35f)), shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Person, contentDescription = null, tint = colors.gradientStart, modifier = Modifier.size(RoleButtonIconSize))
        Text(text = text, style = RoleButtonTextStyle, color = colors.gradientStart)
    }
}

// iOS uses 56pt / 17pt on a ~402pt-wide iPhone; scaled to this ~384dp-wide
// layout so the role buttons take up the same share of the screen.
private val RoleButtonHeight = 54.dp
private val RoleButtonIconSize = 18.dp
private val RoleButtonTextStyle = MaskanType.buttonLabel.copy(fontSize = 16.sp, lineHeight = 21.sp)
