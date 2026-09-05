# Keep native entry points from being removed or renamed by R8.
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Keep the explicit JNI wrapper and its native methods.
-keep class com.jusgif.upscayl.NativeUpscaler { *; }

# Keep Android component entry points used by the framework/reflection.
-keep class * extends android.app.Application { *; }
-keep class * extends android.app.Activity { *; }
-keep class * extends android.app.Service { *; }
