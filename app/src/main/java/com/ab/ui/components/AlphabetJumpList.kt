package com.ab.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ab.ui.theme.LocalMetroDarkTheme
import com.ab.ui.theme.MetroColors
import com.ab.ui.theme.MetroDimensions
import com.ab.ui.theme.MetroTypography

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlphabetJumpList(
    isOpen: Boolean,
    activeLetters: Set<Char>,
    accentColor: Color,
    onLetterSelected: (Char) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    BackHandler(enabled = isOpen) {
        onDismiss()
    }

    val isDark = LocalMetroDarkTheme.current
    val overlayBg = if (isDark) Color(0xF0000000) else Color(0xF0FFFFFF)
    val inactiveBg = if (isDark) MetroColors.JumpInactiveDark else MetroColors.JumpInactiveLight
    val inactiveText = if (isDark) MetroColors.JumpInactiveTextDark else MetroColors.JumpInactiveTextLight

    val characters = listOf('#') + ('A'..'Z').toList()

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.95f)
    ) {
        Box(
            modifier = modifier
                .testTag("jump_list_overlay")
                .fillMaxSize()
                .background(overlayBg)
                .clickable { onDismiss() }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {},
                horizontalArrangement = Arrangement.spacedBy(MetroDimensions.jumpGridGap),
                verticalArrangement = Arrangement.spacedBy(MetroDimensions.jumpGridGap),
                maxItemsInEachRow = 4
            ) {
                for (char in characters) {
                    val isActive = activeLetters.contains(char)

                    Box(
                        modifier = Modifier
                            .testTag("jump_tile_$char")
                            .size(MetroDimensions.jumpCellSize)
                            .background(
                                color = if (isActive) accentColor else inactiveBg,
                                shape = RectangleShape
                            )
                            .clickable(enabled = isActive) {
                                onLetterSelected(char)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char.toString(),
                            style = MetroTypography.jumpLetter,
                            color = if (isActive) Color.White else inactiveText
                        )
                    }
                }
            }
        }
    }
}
