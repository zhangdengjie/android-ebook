#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_xixi_security_Monitor_baseUrl(JNIEnv *env, jobject thiz) {
    std::string baseUrl = "http://www.ttawa.com";
    return env->NewStringUTF(baseUrl.c_str());
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_xixi_security_Monitor_mqttHost(JNIEnv *env, jobject thiz) {
    std::string baseUrl = "111.229.197.250";
    return env->NewStringUTF(baseUrl.c_str());
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_xixi_security_Monitor_mqttPort(JNIEnv *env, jobject thiz) {
    return 1883;
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_xixi_security_Monitor_mqttUsername(JNIEnv *env, jobject thiz) {
    std::string baseUrl = "emqx_client";
    return env->NewStringUTF(baseUrl.c_str());
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_xixi_security_Monitor_mqttPassword(JNIEnv *env, jobject thiz) {
    std::string baseUrl = "20220607!";
    return env->NewStringUTF(baseUrl.c_str());
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_xixi_security_Monitor_getUserId(JNIEnv *env, jobject thiz) {
    std::string userId = "emqx";
    return env->NewStringUTF(userId.c_str());
}