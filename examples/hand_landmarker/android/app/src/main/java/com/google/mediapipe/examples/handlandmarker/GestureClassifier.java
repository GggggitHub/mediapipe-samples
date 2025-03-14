package com.google.mediapipe.examples.handlandmarker;

/**
 * Created by zhangyingjie on 3/12/25
 *
 * @function
 */
import org.tensorflow.lite.Interpreter;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;

public class GestureClassifier {
    private static final String TAG = "GestureClassifier";
    private Interpreter interpreter;

    public GestureClassifier(Context context) throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        options.setUseXNNPACK(false);  // 禁用 XNNPack 加速器

        interpreter = new Interpreter(loadModelFile(context), options);

    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd("fist_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public boolean isFist(float[][] input) {
//        float[][] output = new float[1][1];
        float[][] output = new float[1][3];// 输出形状为 (1, 3)
        interpreter.run(input, output);

        // 解析结果
        int gesture = argmax(output[0]);
        String gestureName = getGestureName(gesture);
//        System.out.println("识别结果: " + gestureName);
        Log.e(TAG, "识别结果: " + gestureName);

        return output[0][0] > 0.5;  // 返回是否是拳头
    }

    private int argmax(float[] array) {
        int maxIndex = -1;
        float maxValue = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private String getGestureName(int gesture) {
        switch (gesture) {
            case 0:
                return "拳头";
            case 1:
                return "剪刀";
            case 2:
                return "布";
            default:
                return "未知手势";
        }
    }
}
