package com.example.ui.components

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
import com.example.ui.theme.MetroColors
import com.example.ui.theme.MetroDimensions
import com.example.ui.theme.MetroTypography

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
                .background(Color(0xF0000000))
                .clickable { onDismiss() }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}, // Prevent clicks from passing through
                horizontalArrangement = Arrangement.spacedBy(MetroDimensions.jumpGridGap, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(MetroDimensions.jumpGridGap),
                maxItemsInEachRow = 4
            ) {
                for (ch in characters) {
                    val isActive = activeLetters.contains(ch)
                    val bgColor = if (isActive) accentColor else MetroColors.JumpInactiveBackground
                    val textColor = if (isActive) Color.White else MetroColors.JumpInactiveText

                    Box(
                        modifier = Modifier
                            .testTag("jump_cell_$ch")
                            .size(MetroDimensions.jumpCellSize)
                            .background(bgColor, RectangleShape)
                            .clickable(enabled = isActive) {
                                onLetterSelected(ch)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ch.toString(),
                            style = MetroTypography.jumpLetter,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}
