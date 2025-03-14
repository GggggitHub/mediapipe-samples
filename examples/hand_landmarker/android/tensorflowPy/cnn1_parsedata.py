import os
os.environ["CUDA_VISIBLE_DEVICES"] = "-1"  # 禁用 GPU
os.environ["TF_METAL_ENABLE"] = "0"  # 关闭 Metal 插件


import json
import numpy as np
import ast


# 文件路径

# file_test_path = "/Users/macpro/Downloads/optimistic/mediapipe-samples/examples/hand_landmarker/android/gesture_20250314_192241_testdata.txt"
file_test_path = "/Users/macpro/Downloads/optimistic/mediapipe-samples/examples/hand_landmarker/android/gesture_20250314_193106_testdata2.txt"
# file_quantou_path = "/Users/macpro/Downloads/optimistic/mediapipe-samples/examples/hand_landmarker/android/gesture_20250314_184124_shitou.txt"
file_quantou_path = "/Users/macpro/Downloads/optimistic/mediapipe-samples/examples/hand_landmarker/android/gesture_20250314_194228_shitou2.txt"
# file_jiandao_path = "/Users/macpro/Downloads/optimistic/mediapipe-samples/examples/hand_landmarker/android/gesture_20250314_184355_jiandao.txt"
file_jiandao_path = "/Users/macpro/Downloads/optimistic/mediapipe-samples/examples/hand_landmarker/android/gesture_20250314_194110_jiandao2.txt"
# file_bu_path = "/Users/macpro/Downloads/optimistic/mediapipe-samples/examples/hand_landmarker/android/gesture_20250314_185756_bu.txt"
file_bu_path = "/Users/macpro/Downloads/optimistic/mediapipe-samples/examples/hand_landmarker/android/gesture_20250314_193925_bu2.txt"
# 解析手势数据，每一行是一个手势样本
def load_gesture_data(file_path):
    data = []
    with open(file_path, "r") as f:
        for line in f:
            try:
                parsed_line = ast.literal_eval(line.strip())  # 解析字符串为 Python 列表
                data.append(parsed_line)
            except Exception as e:
                print(f"解析错误: {e}, 数据: {line}")
    return np.array(data)  # 转换为 NumPy 数组

# 读取数据
gesture_data_fist = load_gesture_data(file_quantou_path)
gesture_data_scissors = load_gesture_data(file_jiandao_path)
gesture_data_paper = load_gesture_data(file_bu_path)

# **检查数据形状**
print("拳头数据形状:", gesture_data_fist.shape)  # 应该是 (N, 21, 3)
print("剪刀数据形状:", gesture_data_scissors.shape)  # 应该是 (M, 21, 3)
print("布数据形状:", gesture_data_paper.shape)  # 应该是 (K, 21, 3)


import os
os.environ["CUDA_VISIBLE_DEVICES"] = "-1"  # 禁用 GPU
os.environ["TF_METAL_ENABLE"] = "0"  # 关闭 Metal 插件
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "2"  # 只显示重要的 TensorFlow 日志
os.environ["TF_FORCE_GPU_ALLOW_GROWTH"] = "true"  # 避免 GPU 计算图优化错误

import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers


# **合并数据**
gesture_data = np.concatenate((gesture_data_fist, gesture_data_scissors, gesture_data_paper), axis=0)
labels = np.concatenate((
    np.zeros((gesture_data_fist.shape[0],)),  # 拳头标签为 0
    np.ones((gesture_data_scissors.shape[0],)),  # 剪刀标签为 1
    np.full((gesture_data_paper.shape[0],), 2)  # 布标签为 2
), axis=0)

# **创建 CNN 训练模型** 
# 0: 拳头，1: 剪刀，2: 布
# 方法 1：改用 Conv2D 代替 Conv1D
# **修改模型，确保适配 Conv2D**
model = keras.Sequential([
    layers.Reshape((21, 3, 1), input_shape=(21, 3)),  # **增加通道维度**
    layers.Conv2D(32, (3, 1), activation="relu"),  # **(3,1) 适配 `width=3` 的数据，确保 width=3 时仍然有效，防止 width=0 。
    layers.MaxPooling2D((2, 1)),  # **确保 `width` 不会缩到 0**
    layers.Conv2D(64, (3, 1), activation="relu"),
    layers.GlobalAveragePooling2D(),
    layers.Dense(64, activation="relu"),
    # layers.Dense(1, activation="sigmoid")  # 0: 非拳头，1: 拳头
    layers.Dense(3, activation="softmax")  # 3 个输出节点，分别对应拳头、剪刀、布

])

# 编译模型
# model.compile(optimizer="adam", loss="binary_crossentropy", metrics=["accuracy"])# binary_crossentropy: 适用于二分类问题，即输出只有两个类别（例如，0 和 1）。
model.compile(optimizer="adam", loss="sparse_categorical_crossentropy", metrics=["accuracy"]) # sparse_categorical_crossentropy: 适用于多分类问题，即输出有多个类别（例如，0, 1, 2, ...）。


# **✔ 训练时正确引用数据**
# y_train = np.ones((gesture_data.shape[0],))  # 全部标记为拳头 (1)


# **训练模型**
model.fit(gesture_data, labels, epochs=10, batch_size=32)


# **保存模型**
model.save("fist_model.h5")
print("模型训练完成，已保存为 fist_model.h5")



# **加载测试数据**
test_data = load_gesture_data(file_test_path)

# **进行预测**
predictions = model.predict(test_data)


# **打印输出结果**
gesture_names = ["拳头", "剪刀", "布"]
for i, prediction in enumerate(predictions):
    max_prob = np.max(prediction)
    predicted_label = np.argmax(prediction)
    if max_prob < 0.5:
        # print(f"样本 {i+1}: 未知类型----",f"样本 {i+1}: {gesture_names[predicted_label]}")
        a=1
    else:
        predicted_label = np.argmax(prediction)
        print(f"样本 {i+1}: {gesture_names[predicted_label]}",max_prob)

# # **打印输出结果**
# gesture_names = ["拳头", "剪刀", "布"]
# for i, prediction in enumerate(predictions):
#     predicted_label = np.argmax(prediction)
#     print(f"样本 {i+1}: {gesture_names[predicted_label]}")