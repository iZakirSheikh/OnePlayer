package com.zs.audiofy.common.compose

import android.util.Log
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw

private const val TAG = "Transform"
private class TransformNode(var scaleX: Float, var scaleY: Float): Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() {
        // DrawScope.scale scales the canvas only for this draw pass
        scale(scaleX = scaleX, scaleY = scaleY) {
            Log.d(TAG, "draw: scaling")
            this@draw.drawContent()
        }
    }
}

private class TransformElement(
    private val scaleX: Float,
    private val scaleY: Float
): ModifierNodeElement<TransformNode>() {
    override fun create(): TransformNode = TransformNode(scaleX, scaleY)

    override fun update(node: TransformNode) {
        if (scaleX == node.scaleX && scaleY == node.scaleY)
            return
        node.scaleX = scaleX
        node.scaleY = scaleY
        node.invalidateDraw()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TransformElement

        if (scaleX != other.scaleX) return false
        if (scaleY != other.scaleY) return false

        return true
    }

    override fun hashCode(): Int {
        var result = scaleX.hashCode()
        result = 31 * result + scaleY.hashCode()
        return result
    }
}

private fun Modifier.transform(scaleX: Float, scaleY: Float): Modifier =
    this then TransformElement(scaleX = scaleX, scaleY = scaleY)

fun Modifier.scale(scaleX: Float, scaleY: Float) =
    if (scaleX != 1.0f || scaleY != 1.0f) {
        transform(scaleX = scaleX, scaleY = scaleY)
    } else {
        this
    }

fun Modifier.scale(scale: Float) =
    scale(scale, scale)