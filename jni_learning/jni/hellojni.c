#define LOG_NDEBUG 0
#define LOG_TAG "HelloJni"

#include <string.h>
#include <jni.h>

#include <utils/Log.h>

JNIEXPORT jstring JNICALL
Java_com_example_MainActivity_stringFromJNI(JNIEnv* env, jobject thiz)
{
    ALOGD("(%s, %d) stringFromJNI!", __func__, __LINE__);
    return (*env)->NewStringUTF(env, "<HelloWorld> from JNI!");
}

JNIEXPORT jint JNICALL
Java_com_example_MainActivity_addFromJNI(JNIEnv* env, jobject thiz, jint a, jint b)
{
    ALOGD("(%s, %d) addFromJNI(%d, %d)!", __func__, __LINE__, a, b);
    return (a+b);
}

JNIEXPORT jint JNICALL
Java_com_example_MainActivity_subFromJNI(JNIEnv* env, jobject thiz, jint a, jint b)
{
    ALOGD("(%s, %d) subFromJNI(%d, %d)!", __func__, __LINE__, a, b);
    return (a-b);
}
