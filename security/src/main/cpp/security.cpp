#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_xixi_security_NativeLib_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_xixi_security_NativeLib_baseUrl(JNIEnv *env, jobject thiz) {
    std::string baseUrl = "http://www.ttawa.com";
    return env->NewStringUTF(baseUrl.c_str());
}