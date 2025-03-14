import tensorflow as tf

# **加载已训练的 Keras 模型**
model = tf.keras.models.load_model("fist_model.h5")

# **直接从 Keras 模型转换**
converter = tf.lite.TFLiteConverter.from_keras_model(model)

# **优化模型大小**
converter.optimizations = [tf.lite.Optimize.DEFAULT]

# **生成 TFLite 模型**
tflite_model = converter.convert()

# **保存文件**
with open("fist_model.tflite", "wb") as f:
    f.write(tflite_model)

print("模型转换完成，已保存为 fist_model.tflite")