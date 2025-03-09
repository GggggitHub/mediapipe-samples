package com.google.mediapipe.examples.poselandmarker

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

/**
 * Created by zhangyingjie on 2025/3/9
 * @function
 */
// PoseDetector.kt
class PoseDetector {
    companion object {

        // 右手举起过头顶时，右手腕（RIGHT_WRIST，索引为 16）的 y 坐标会小于头顶（NOSE，索引为 0）的 y 坐标
        fun isRightHandRaisedAboveHead(results: PoseLandmarkerResult): Boolean {
            val landmarks = results.landmarks().getOrNull(0) ?: return false
            val rightWrist = landmarks.getOrNull(16) ?: return false
            val nose = landmarks.getOrNull(0) ?: return false
            return rightWrist.y() < nose.y()
        }

        //左手举起过头顶时，左手腕（LEFT_WRIST，索引为 15）的 y 坐标会小于头顶（NOSE，索引为 0）的 y 坐标
        fun isLeftHandRaisedAboveHead(results: PoseLandmarkerResult): Boolean {
            val landmarks = results.landmarks().getOrNull(0) ?: return false
            val leftWrist = landmarks.getOrNull(15) ?: return false
            val nose = landmarks.getOrNull(0) ?: return false
            return leftWrist.y() < nose.y()
        }


        private const val STANDING_THRESHOLD = 0.1

        //判断站着还是坐着可以通过比较髋关节（LEFT_HIP 和 RIGHT_HIP，索引分别为 23 和 24）与膝盖（LEFT_KNEE 和 RIGHT_KNEE，索引分别为 25 和 26）的垂直距离。站立时这个距离会比坐着时大。
        fun isStanding(results: PoseLandmarkerResult): Boolean {
            val landmarks = results.landmarks().getOrNull(0) ?: return false
            val leftHip = landmarks.getOrNull(23) ?: return false
            val leftKnee = landmarks.getOrNull(25) ?: return false
            val rightHip = landmarks.getOrNull(24) ?: return false
            val rightKnee = landmarks.getOrNull(26) ?: return false

            // 计算左右髋关节和膝盖的垂直距离
            val leftDistance = leftHip.y() - leftKnee.y()
            val rightDistance = rightHip.y() - rightKnee.y()

            // 设置一个阈值，当距离大于阈值时认为是站着
            return leftDistance > STANDING_THRESHOLD && rightDistance > STANDING_THRESHOLD
        }
    }
}