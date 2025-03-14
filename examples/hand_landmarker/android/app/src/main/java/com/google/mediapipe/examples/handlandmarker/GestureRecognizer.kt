package com.google.mediapipe.examples.handlandmarker

/**
 * Created by zhangyingjie on 2025/3/9
 * @function
 */



import android.content.Context
import android.icu.text.SimpleDateFormat
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import java.io.FileOutputStream
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

class GestureRecognizer(private val context: Context) {
    private val TAG = "GestureRecognizer"
    // 在类顶部添加以下属性
    private var outputStream: FileOutputStream? = null
    private val fileTimestamp by lazy {
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }
    private lateinit var classifier: GestureClassifier


    fun recognizeGestures(handLandmarks: List<NormalizedLandmark>): String {
        try {
            if (outputStream == null) {
                val fileName = "gesture_$fileTimestamp.txt"
                outputStream = context.openFileOutput(fileName, Context.MODE_APPEND)
            }

            //遍历 handLandmarks 列表，打印每个关键点的坐标
            val sb = buildString {
                append("[")
                handLandmarks.joinTo(this) { landmark ->
                    "%.6f,%.6f,%.6f".format(landmark.x(), landmark.y(), landmark.z())
                        .let { "[$it]" }
                }
                append("]\n")
            }
            outputStream?.write(sb.toByteArray())
            outputStream?.flush()
            // 将手部关键点数据转换为二维数组
            val handData = Array(1) { FloatArray(21 * 3) }.apply {
                handLandmarks.forEachIndexed { index, landmark ->
                    this[0][index * 3] = landmark.x().toFloat()
                    this[0][index * 3 + 1] = landmark.y().toFloat()
                    this[0][index * 3 + 2] = landmark.z().toFloat()
                }
            }


            // 示例代码，调用 TensorFlow Lite 进行推理
//            val handData = Array(1) { FloatArray(21 * 3) } // 需要传入 21×3 的手势数据
            if (!::classifier.isInitialized) {
                classifier = GestureClassifier(context)
            }
            val isFist = classifier.isFist(handData)
//            if (isFist) {
//                Log.e(TAG, "识别到拳头")
//            } else {
//                Log.e(TAG, "非拳头")
//            }



        } catch (e: Exception) {
            Log.e(TAG, "Save failed: ${e.message}")
        }

        return when {
            isVictory(handLandmarks) -> "剪刀"
            isFist(handLandmarks) -> "石头"
            isOpenHand(handLandmarks) -> "布"

            // 将次要手势移到主逻辑之后
            isOK(handLandmarks) -> "OK"
            isThumbUp(handLandmarks) -> "Yes"
            else -> "未识别"
        }.also { result ->
            showToast(result)
//            if (result != "未识别") showToast(result)
        }
    }

    // 在类中添加关闭方法
    fun release() {
        runCatching {
            outputStream?.close()
            outputStream = null
        }.onFailure {
            Log.e(TAG, "Stream close failed: ${it.message}")
        }
    }


    private fun isVictory(landmarks: List<NormalizedLandmark>): Boolean {
        val angles = calculateFingerAngles(landmarks)
        // 提高角度阈值，增加其他手指弯曲检查
        return angles[1] < 30f && angles[2] < 30f &&  // 食指和中指
                angles[3] > 120f && angles[4] > 120f  // 无名指和小指
    }

//    private fun isFist(landmarks: List<NormalizedLandmark>): Boolean {
//        val wrist = landmarks[0]
//        // 修改检查所有指尖（包括小指和中指）
//        val fingerTips = listOf(4, 8, 12, 16, 20).map { landmarks[it] }
//        // 收紧距离阈值，增加角度阈值
//        return fingerTips.all { distance(it, wrist) < 0.1f } &&
//                calculateFingerAngles(landmarks).all { it > 120f } &&
//                distance(landmarks[4], landmarks[8]) > 0.15f && // 增大拇指食指间距
//                // 新增手掌闭合检查（所有第二指节到手腕距离 < 0.15）
//                listOf(6, 10, 14, 18).all { distance(landmarks[it], wrist) < 0.15f }
//    }
    private fun isFist(landmarks: List<NormalizedLandmark>): Boolean {
        val wrist = landmarks[0]
        val fingerTips = listOf(4, 8, 12, 16, 20).map { landmarks[it] }

        // 放宽指尖到手腕的距离阈值（从0.1调整为0.2）
        val tipsClose = fingerTips.all { distance(it, wrist) < 0.2f }

        // 调整拇指食指间距阈值（从0.15调整为0.08）
        val thumbIndexSpacing = distance(landmarks[4], landmarks[8]) > 0.08f

        // 修改第二指节检查为相对距离比较
        val middleJoints = listOf(6, 10, 14, 18).map { landmarks[it] }
        val jointsClose = middleJoints.all { distance(it, wrist) < distance(landmarks[3], wrist) }

        return tipsClose &&
                calculateFingerAngles(landmarks).all { it > 100f } && // 角度阈值从120降为100
                thumbIndexSpacing &&
                jointsClose
    }
    private fun isOpenHand(landmarks: List<NormalizedLandmark>): Boolean {
        val wrist = landmarks[0]
        // 调整距离阈值，增加角度条件
        return landmarks.slice(4..8).all { distance(it, wrist) > 0.25f } &&
                calculateFingerAngles(landmarks).all { it < 30f }
    }

    private fun isOK(landmarks: List<NormalizedLandmark>): Boolean {
        // 拇指和食指接触，其他手指伸直
        return distance(landmarks[4], landmarks[8]) < 0.05f &&
                // 增加其他手指弯曲检查
                landmarks.slice(12..16).all { it.y() < landmarks[0].y() } &&
                calculateFingerAngles(landmarks).slice(2..4).all { it < 30f }
    }

    private fun isThumbUp(landmarks: List<NormalizedLandmark>): Boolean {
        // 拇指竖直向上，其他手指握拳
        return landmarks[4].y() < landmarks[3].y() &&
                landmarks.slice(8..16).all { it.y() > landmarks[0].y() }
    }

    private fun distance(a: NormalizedLandmark, b: NormalizedLandmark): Float {
        return Math.sqrt(
            (a.x() - b.x()).toDouble().pow(2) +
                    (a.y() - b.y()).toDouble().pow(2)
        ).toFloat()
    }

    private fun showToast(message: String) {
        Log.i(TAG, "showToast: $message");
//        android.os.Handler(Looper.getMainLooper()).post {
//            Toast.makeText(context, "识别到手势：$message", Toast.LENGTH_SHORT).show()
//        }
//        Toast.makeText(context, "识别到手势：$message", Toast.LENGTH_SHORT).show()
    }

    private fun calculateFingerAngles(landmarks: List<NormalizedLandmark>): FloatArray {
        // 实现手指角度计算逻辑（根据关节点的坐标计算）
        // 返回包含各手指弯曲角度的数组
        // 此处需要根据实际关键点索引实现具体算法
// 手指关节索引（根据MediaPipe手部关键点定义）
        val fingerIndices = arrayOf(
            intArrayOf(0, 1, 2, 3, 4),    // 拇指
            intArrayOf(0, 5, 6, 7, 8),    // 食指
            intArrayOf(0, 9, 10, 11, 12), // 中指
            intArrayOf(0, 13, 14, 15, 16),// 无名指
            intArrayOf(0, 17, 18, 19, 20) // 小指
        )

        return FloatArray(5).apply {
            fingerIndices.forEachIndexed { fingerIdx, joints ->
                // 计算三个关键点之间的角度（根部-中间关节-指尖）
                val root = landmarks[joints[1]]
                val mid = landmarks[joints[2]]
                val tip = landmarks[joints[3]]

                // 计算两个向量
                val vec1 = Point(mid.x() - root.x(), mid.y() - root.y())
                val vec2 = Point(tip.x() - mid.x(), tip.y() - mid.y())

                // 计算向量夹角
                val angle = Math.toDegrees(
                    Math.atan2(vec2.y.toDouble(), vec2.x.toDouble()) -
                            Math.atan2(vec1.y.toDouble(), vec1.x.toDouble())
                ).toFloat()

                this[fingerIdx] = abs(angle)
            }
        }
    }
    // 辅助类用于计算向量
    private data class Point(val x: Float, val y: Float)

}