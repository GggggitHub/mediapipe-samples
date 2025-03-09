package com.google.mediapipe.examples.poselandmarker

/**
 * Created by zhangyingjie on 2025/3/9
 * @function
 */
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.abs
import kotlin.math.atan2

// 定义一个 PoseEvaluator 类来封装太极拳姿势评估的方法
class PoseEvaluator {
    companion object {
        // 定义角度误差阈值，用于判断角度是否符合要求
        private const val ANGLE_THRESHOLD = 15.0

        // 计算两点之间的角度
        private fun calculateAngle(x1: Float, y1: Float, x2: Float, y2: Float): Double {
            return Math.toDegrees(atan2((y2 - y1).toDouble(), (x2 - x1).toDouble()))
        }

        // 判断某个招式是否标准，这里以“野马分鬃”为例
        fun isWildHorsePartingManeStandard(results: PoseLandmarkerResult): Boolean {
            val landmarks = results.landmarks().getOrNull(0) ?: return false
            // 假设“野马分鬃”中，左手和左肩膀、左肘部形成的角度有一定要求
            val leftShoulder = landmarks.getOrNull(11) ?: return false
            val leftElbow = landmarks.getOrNull(13) ?: return false
            val leftWrist = landmarks.getOrNull(15) ?: return false

            // 计算左肩膀、左肘部、左手腕形成的角度
            val angle = calculateAngle(
                leftShoulder.x(),
                leftShoulder.y(),
                leftElbow.x(),
                leftElbow.y()
            ) - calculateAngle(
                leftElbow.x(),
                leftElbow.y(),
                leftWrist.x(),
                leftWrist.y()
            )

            // 假设标准角度是 120 度，判断实际角度是否在误差范围内
            return abs(angle - 120) <= ANGLE_THRESHOLD
        }

        // 判断“白鹤亮翅”招式是否标准
        fun isWhiteCraneSpreadingWingsStandard(results: PoseLandmarkerResult): Boolean {
            val landmarks = results.landmarks().getOrNull(0) ?: return false
            // 假设“白鹤亮翅”中，右手和右肩膀、右肘部形成的角度有一定要求
            val rightShoulder = landmarks.getOrNull(12) ?: return false
            val rightElbow = landmarks.getOrNull(14) ?: return false
            val rightWrist = landmarks.getOrNull(16) ?: return false

            // 计算右肩膀、右肘部、右手腕形成的角度
            val angle = calculateAngle(
                rightShoulder.x(),
                rightShoulder.y(),
                rightElbow.x(),
                rightElbow.y()
            ) - calculateAngle(
                rightElbow.x(),
                rightElbow.y(),
                rightWrist.x(),
                rightWrist.y()
            )

            // 假设标准角度是 90 度，判断实际角度是否在误差范围内
            return abs(angle - 90) <= ANGLE_THRESHOLD
        }
    }
}