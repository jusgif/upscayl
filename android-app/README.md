# Upscayl Android

Minimal AMOLED-friendly Android 12+ image upscaler based on the Upscayl/Real-ESRGAN approach.

## Stack
- Kotlin + Jetpack Compose
- Android API 31+
- Native C++ via JNI
- NCNN with Vulkan acceleration when available
- Real-ESRGAN x4plus model downloaded on first use
- 256px tiled inference with 10px replicated padding
- MediaStore export to `Pictures/Upscayl`

## Build

1. Install Android Studio with SDK 35, NDK 29 and CMake 3.22.1.
2. Fetch the prebuilt NCNN Vulkan package:

```bash
bash ./scripts/fetch-ncnn.sh
```

3. Open `android-app` in Android Studio and build the `app` module.

The repository CI fetches NCNN automatically and runs unit tests plus a debug APK build.

## Runtime

The first upscale downloads the `realesrgan-x4plus` NCNN model (~33 MB). The model is then kept in app-private storage and subsequent runs work without another download.

4× is native model output. 2× is produced from the 4× model result by a high-quality Android resize, matching Upscayl's scale-emulation approach for models that only provide x4 output.

## Licensing

The upstream Upscayl backend is AGPL-3.0. Real-ESRGAN/NCNN components retain their respective upstream licenses; see the repository license files.
