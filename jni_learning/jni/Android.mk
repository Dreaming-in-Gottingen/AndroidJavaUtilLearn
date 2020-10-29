LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := hellojni.c

LOCAL_SHARED_LIBRARIES := libbinder   \
                          libutils    \
                          liblog      \

LOCAL_CFLAGS := -Wno-unused-variable -Wno-unused-parameter
LOCAL_MODULE := libhellojni

include $(BUILD_SHARED_LIBRARY)
