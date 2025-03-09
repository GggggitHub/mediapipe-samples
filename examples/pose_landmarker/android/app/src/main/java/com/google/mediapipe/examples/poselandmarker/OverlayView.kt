/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.mediapipe.examples.poselandmarker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.max
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {
    private val TAG = "OverlayView"

    private var results: PoseLandmarkerResult? = null
    private var pointPaint = Paint()
    private var linePaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    private val numberPaint = Paint() // 新增 Paint 对象

    init {
        initPaints()
    }

    fun clear() {
        results = null
        pointPaint.reset()
        linePaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        linePaint.color =
            ContextCompat.getColor(context!!, R.color.mp_color_primary)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL

        // 初始化 numberPaint
        numberPaint.color = Color.WHITE
        numberPaint.textSize = 24f // 增大字体大小
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.let { poseLandmarkerResult ->

            // 判断“野马分鬃”招式是否标准
            if (PoseEvaluator.isWildHorsePartingManeStandard(poseLandmarkerResult)) {
                Log.d(TAG, "野马分鬃招式标准")
            } else {
                Log.d(TAG, "野马分鬃招式不标准")
            }

            // 判断“白鹤亮翅”招式是否标准
            if (PoseEvaluator.isWhiteCraneSpreadingWingsStandard(poseLandmarkerResult)) {
                Log.d(TAG, "白鹤亮翅招式标准")
            } else {
                Log.d(TAG, "白鹤亮翅招式不标准")
            }


//            // 使用 PoseDetector 类进行姿势判断
//            if (PoseDetector.isRightHandRaisedAboveHead(poseLandmarkerResult)) {
//                Log.d(TAG, "右手举起过头顶")
//            }
//            if (PoseDetector.isLeftHandRaisedAboveHead(poseLandmarkerResult)) {
//                Log.d(TAG, "左手举起过头顶")
//            }
//            if (PoseDetector.isStanding(poseLandmarkerResult)) {
//                Log.d(TAG, "站着")
//            } else {
//                Log.d(TAG, "坐着")
//            }

            // 绘制关键点和连线
            for(landmark in poseLandmarkerResult.landmarks()) {
                for ((index, normalizedLandmark) in landmark.withIndex()) {
                    val x = normalizedLandmark.x() * imageWidth * scaleFactor
                    val y = normalizedLandmark.y() * imageHeight * scaleFactor
                    canvas.drawPoint(
                        x,
                        y,
                        pointPaint
                    )

                    // 调整数字绘制位置，在点的上方一定距离
                    val textY = y - numberPaint.textSize
                    // 绘制数字
                    canvas.drawText(
                        index.toString(),
                        x,
                        textY,
                        numberPaint
                    )
                }

                // 绘制骨骼连线
                PoseLandmarker.POSE_LANDMARKS.forEach {
                    canvas.drawLine(
                        poseLandmarkerResult.landmarks().get(0).get(it!!.start()).x() * imageWidth * scaleFactor,
                        poseLandmarkerResult.landmarks().get(0).get(it.start()).y() * imageHeight * scaleFactor,
                        poseLandmarkerResult.landmarks().get(0).get(it.end()).x() * imageWidth * scaleFactor,
                        poseLandmarkerResult.landmarks().get(0).get(it.end()).y() * imageHeight * scaleFactor,
                        linePaint)
                }
            }
        }
    }

    fun setResults(
        poseLandmarkerResults: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = poseLandmarkerResults

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                // PreviewView is in FILL_START mode. So we need to scale up the
                // landmarks to match with the size that the captured images will be
                // displayed.
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 12F
    }
}