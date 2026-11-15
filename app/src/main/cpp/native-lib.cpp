#include <jni.h>

// UI text only. No game access, no offsets, no memory reading.
static jstring str(JNIEnv *env, const char *s) { return env->NewStringUTF(s); }

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_fakemenu_FloatingService_nativeTitle(JNIEnv *env, jobject) {
    return str(env, "JELHZHAX");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_fakemenu_FloatingService_nativeHandle(JNIEnv *env, jobject) {
    return str(env, "@jelhzhax");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_fakemenu_FloatingService_nativeVersion(JNIEnv *env, jobject) {
    return str(env, "1.0.0");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_fakemenu_FloatingService_nativeFooter(JNIEnv *env, jobject) {
    return str(env, "UI demo only - not connected to any game");
}
