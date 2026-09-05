#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <algorithm>
#include <string>
#include <vector>
#include "net.h"

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "Upscayl", __VA_ARGS__)

static bool getBitmapInfo(JNIEnv* env, jobject bitmap, AndroidBitmapInfo& info) {
    return AndroidBitmap_getInfo(env, bitmap, &info) == ANDROID_BITMAP_RESULT_SUCCESS && info.format == ANDROID_BITMAP_FORMAT_RGBA_8888;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_jusgif_upscayl_NativeUpscaler_upscale(JNIEnv* env, jobject, jobject bitmap, jstring modelDir, jint tileSize, jboolean useVulkan) {
    AndroidBitmapInfo info{};
    if (!getBitmapInfo(env, bitmap, info)) { LOGE("Unsupported bitmap format"); return nullptr; }
    const char* dir = env->GetStringUTFChars(modelDir, nullptr);
    std::string base(dir ? dir : "");
    env->ReleaseStringUTFChars(modelDir, dir);
    const std::string param = base + "/realesrgan-x4plus.param";
    const std::string bin = base + "/realesrgan-x4plus.bin";

    ncnn::Net net;
    net.opt.use_vulkan_compute = useVulkan && ncnn::get_gpu_count() > 0;
    net.opt.use_fp16_packed = true;
    net.opt.use_fp16_storage = true;
    net.opt.use_fp16_arithmetic = false;
    if (net.opt.use_vulkan_compute) net.set_vulkan_device(0);
    if (net.load_param(param.c_str()) != 0 || net.load_model(bin.c_str()) != 0) { LOGE("Failed to load model"); return nullptr; }

    constexpr int scale = 4;
    const int tile = std::max(32, (int)tileSize);
    constexpr int pad = 10;
    const int sw = (int)info.width, sh = (int)info.height;
    const int dw = sw * scale, dh = sh * scale;

    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    jmethodID createBitmap = env->GetStaticMethodID(bitmapClass, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jclass configClass = env->FindClass("android/graphics/Bitmap$Config");
    jfieldID argb8888Field = env->GetStaticFieldID(configClass, "ARGB_8888", "Landroid/graphics/Bitmap$Config;");
    jobject argb8888 = env->GetStaticObjectField(configClass, argb8888Field);
    jobject outBitmap = env->CallStaticObjectMethod(bitmapClass, createBitmap, dw, dh, argb8888);
    if (!outBitmap) return nullptr;

    AndroidBitmapInfo outInfo{};
    if (!getBitmapInfo(env, outBitmap, outInfo)) return nullptr;
    void* outPixels = nullptr;
    if (AndroidBitmap_lockPixels(env, outBitmap, &outPixels) != ANDROID_BITMAP_RESULT_SUCCESS) return nullptr;
    auto* out = static_cast<unsigned char*>(outPixels);
    const size_t outStride = outInfo.stride;
    const float norm[3] = {255.f, 255.f, 255.f};

    for (int y = 0; y < sh; y += tile) {
        for (int x = 0; x < sw; x += tile) {
            const int tw = std::min(tile, sw - x), th = std::min(tile, sh - y);
            ncnn::Mat roi = ncnn::Mat::from_android_bitmap_roi(env, bitmap, ncnn::Mat::PIXEL_RGBA2RGB, x, y, tw, th);
            if (roi.empty()) { AndroidBitmap_unlockPixels(env, outBitmap); return nullptr; }
            ncnn::Mat in;
            ncnn::copy_make_border(roi, in, pad, pad, pad, pad, ncnn::BORDER_REPLICATE, 0.f);
            in.substract_mean_normalize(nullptr, norm);

            ncnn::Extractor ex = net.create_extractor();
            ex.set_light_mode(true);
            ex.set_num_threads(2);
            if (ex.input("data", in) != 0) { AndroidBitmap_unlockPixels(env, outBitmap); return nullptr; }
            ncnn::Mat result;
            if (ex.extract("output", result) != 0 || result.empty()) { AndroidBitmap_unlockPixels(env, outBitmap); return nullptr; }
            result.substract_mean_normalize(nullptr, norm);

            std::vector<unsigned char> rgba((size_t)result.w * result.h * 4);
            result.to_pixels(rgba.data(), ncnn::Mat::PIXEL_RGB2RGBA);
            const int crop = pad * scale;
            const int copyW = tw * scale, copyH = th * scale;
            for (int yy = 0; yy < copyH; ++yy) {
                const unsigned char* src = rgba.data() + ((size_t)(yy + crop) * result.w + crop) * 4;
                unsigned char* dst = out + (size_t)(y * scale + yy) * outStride + (size_t)(x * scale) * 4;
                std::copy(src, src + (size_t)copyW * 4, dst);
            }
        }
    }
    AndroidBitmap_unlockPixels(env, outBitmap);
    return outBitmap;
}
