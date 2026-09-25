package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Interactive drag handles for text selection (Start Handle and End Handle).
 * Renders floating, touch-friendly handles with generous 48dp touch targets,
 * prominent high-contrast knobs, and tactile drag behavior allowing users to freely expand,
 * contract, or reposition selection across characters, words, and lines.
 */
enum class DraggingHandle { START, END }

@Composable
fun SelectionDragHandles(
    text: String,
    selection: TextRange,
    layoutResult: TextLayoutResult?,
    hideHeadingSymbols: Boolean,
    highlightColor: Color,
    onSelectionChange: (TextRange) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
    cachedMapping: SelectionHighlightTransformation.CachedOffsetMap? = null
) {
    if (layoutResult == null || selection.collapsed || text.isEmpty()) return

    val density = LocalDensity.current
    val layoutTextLen = layoutResult.layoutInput.text.length
    if (layoutTextLen == 0) return

    val currentSelection by rememberUpdatedState(selection)
    val onSelectionChangeUpdated by rememberUpdatedState(onSelectionChange)
    val currentText by rememberUpdatedState(text)
    val currentCachedMapping by rememberUpdatedState(cachedMapping)
    val currentHideHeadingSymbols by rememberUpdatedState(hideHeadingSymbols)
    val currentLayoutResult by rememberUpdatedState(layoutResult)

    var activeDraggingHandle by remember { mutableStateOf<DraggingHandle?>(null) }

    val transStart = remember(selection.min, cachedMapping, hideHeadingSymbols) {
        if (hideHeadingSymbols && cachedMapping != null) {
            SelectionHighlightTransformation.originalToTransformed(selection.min, cachedMapping)
                .coerceIn(0, layoutTextLen)
        } else {
            SelectionHighlightTransformation.originalToTransformed(text, selection.min, hideHeadingSymbols)
                .coerceIn(0, layoutTextLen)
        }
    }
    val transEnd = remember(selection.max, cachedMapping, hideHeadingSymbols) {
        if (hideHeadingSymbols && cachedMapping != null) {
            SelectionHighlightTransformation.originalToTransformed(selection.max, cachedMapping)
                .coerceIn(0, layoutTextLen)
        } else {
            SelectionHighlightTransformation.originalToTransformed(text, selection.max, hideHeadingSymbols)
                .coerceIn(0, layoutTextLen)
        }
    }

    val startCursorRect = remember(transStart, layoutResult) {
        try {
            layoutResult.getCursorRect(transStart)
        } catch (e: Exception) {
            null
        }
    }

    val endCursorRect = remember(transEnd, layoutResult) {
        try {
            layoutResult.getCursorRect(transEnd)
        } catch (e: Exception) {
            null
        }
    }

    val currentStartCursorRect by rememberUpdatedState(startCursorRect)
    val currentEndCursorRect by rememberUpdatedState(endCursorRect)

    if (startCursorRect == null || endCursorRect == null) return

    val touchTargetRadiusPx = with(density) { 24.dp.toPx() }
    val knobRadiusPx = with(density) { 12.dp.toPx() }
    val hitRadiusPx = with(density) { 52.dp.toPx() }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val sRect = currentStartCursorRect
                    val eRect = currentEndCursorRect
                    if (sRect == null || eRect == null) return@awaitEachGesture

                    val startCenter = Offset(sRect.left, sRect.bottom + knobRadiusPx)
                    val endCenter = Offset(eRect.right, eRect.bottom + knobRadiusPx)

                    val distStart = (down.position - startCenter).getDistance()
                    val distEnd = (down.position - endCenter).getDistance()

                    val hitStart = distStart <= hitRadiusPx ||
                        (down.position.x in (sRect.left - hitRadiusPx)..(sRect.left + hitRadiusPx) &&
                         down.position.y in (sRect.top - 8.dp.toPx())..(sRect.bottom + hitRadiusPx))
                    val hitEnd = distEnd <= hitRadiusPx ||
                        (down.position.x in (eRect.right - hitRadiusPx)..(eRect.right + hitRadiusPx) &&
                         down.position.y in (eRect.top - 8.dp.toPx())..(eRect.bottom + hitRadiusPx))

                    var draggingHandle: DraggingHandle = when {
                        hitStart && (!hitEnd || distStart <= distEnd) -> DraggingHandle.START
                        hitEnd -> DraggingHandle.END
                        else -> null
                    } ?: return@awaitEachGesture

                    // Touch hit a handle! Consume DOWN immediately at Initial pass to pre-empt underlying text field selection detector
                    down.consume()
                    activeDraggingHandle = draggingHandle

                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val dragChange = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!dragChange.pressed) {
                            dragChange.consume()
                            break
                        }

                        dragChange.consume()
                        val currentLayout = currentLayoutResult ?: continue

                        // Sample slightly above finger position to align with text line center
                        val targetY = (dragChange.position.y - knobRadiusPx).coerceAtLeast(0f)
                        val evalPos = Offset(dragChange.position.x, targetY)

                        val transOffset = currentLayout.getOffsetForPosition(evalPos)
                        val map = currentCachedMapping
                        val origOffset = if (currentHideHeadingSymbols && map != null) {
                            SelectionHighlightTransformation.transformedToOriginal(transOffset, map)
                        } else {
                            SelectionHighlightTransformation.transformedToOriginal(currentText, transOffset, currentHideHeadingSymbols)
                        }

                        val textLength = currentText.length
                        val clampedOffset = origOffset.coerceIn(0, textLength)
                        val curMin = currentSelection.min
                        val curMax = currentSelection.max

                        val newRange = when (draggingHandle) {
                            DraggingHandle.START -> {
                                if (clampedOffset <= curMax) {
                                    TextRange(clampedOffset, curMax)
                                } else {
                                    draggingHandle = DraggingHandle.END
                                    activeDraggingHandle = DraggingHandle.END
                                    TextRange(curMax, clampedOffset)
                                }
                            }
                            DraggingHandle.END -> {
                                if (clampedOffset >= curMin) {
                                    TextRange(curMin, clampedOffset)
                                } else {
                                    draggingHandle = DraggingHandle.START
                                    activeDraggingHandle = DraggingHandle.START
                                    TextRange(clampedOffset, curMin)
                                }
                            }
                        }

                        if (newRange != currentSelection) {
                            onSelectionChangeUpdated(newRange)
                        }
                    }
                    activeDraggingHandle = null
                }
            }
    ) {
        // Draw connecting stems to text baseline
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Line at start cursor
            drawLine(
                color = highlightColor,
                start = Offset(startCursorRect.left, startCursorRect.top),
                end = Offset(startCursorRect.left, startCursorRect.bottom),
                strokeWidth = 3.dp.toPx()
            )
            // Line at end cursor
            drawLine(
                color = highlightColor,
                start = Offset(endCursorRect.right, endCursorRect.top),
                end = Offset(endCursorRect.right, endCursorRect.bottom),
                strokeWidth = 3.dp.toPx()
            )
        }

        // --- START HANDLE ---
        val startHandleX = (startCursorRect.left - touchTargetRadiusPx).roundToInt()
        val startHandleY = (startCursorRect.bottom - 4.dp.value * density.density).roundToInt()

        Box(
            modifier = Modifier
                .offset { IntOffset(startHandleX, startHandleY) }
                .size(48.dp)
                .testTag("selection_start_handle"),
            contentAlignment = Alignment.TopCenter
        ) {
            // Visual Knob for Start Handle
            val isStartActive = activeDraggingHandle == DraggingHandle.START
            Box(
                modifier = Modifier
                    .size(if (isStartActive) 28.dp else 24.dp)
                    .shadow(elevation = if (isStartActive) 10.dp else 6.dp, shape = CircleShape)
                    .background(highlightColor, CircleShape)
                    .border(if (isStartActive) 3.dp else 2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isStartActive) 10.dp else 8.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }

        // --- END HANDLE ---
        val endHandleX = (endCursorRect.right - touchTargetRadiusPx).roundToInt()
        val endHandleY = (endCursorRect.bottom - 4.dp.value * density.density).roundToInt()

        Box(
            modifier = Modifier
                .offset { IntOffset(endHandleX, endHandleY) }
                .size(48.dp)
                .testTag("selection_end_handle"),
            contentAlignment = Alignment.TopCenter
        ) {
            // Visual Knob for End Handle
            val isEndActive = activeDraggingHandle == DraggingHandle.END
            Box(
                modifier = Modifier
                    .size(if (isEndActive) 28.dp else 24.dp)
                    .shadow(elevation = if (isEndActive) 10.dp else 6.dp, shape = CircleShape)
                    .background(highlightColor, CircleShape)
                    .border(if (isEndActive) 3.dp else 2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isEndActive) 10.dp else 8.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }
    }
}
