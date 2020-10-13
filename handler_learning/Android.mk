LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, app/src)
LOCAL_RESOURCE_DIR += $(addprefix $(LOCAL_PATH)/, app/src/main/res)
#LOCAL_ASSET_DIR += $(addprefix $(LOCAL_PATH)/, app/src/main/assets)
LOCAL_MANIFEST_FILE := app/src/main/AndroidManifest.xml

LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_USE_AAPT2 := true
LOCAL_PRIVILEGED_MODULE := true
LOCAL_PRIVATE_PLATFORM_APIS := true 
LOCAL_PROGUARD_ENABLED := disabled
LOCAL_DEX_PREOPT := false
LOCAL_AAPT_FLAGS += \
    --auto-add-overlay \


LOCAL_STATIC_ANDROID_LIBRARIES := \
	 $(ANDROID_SUPPORT_DESIGN_TARGETS) \
         android-support-v13 \
         android-support-v4 \
         android-support-v7-appcompat \
	 android-support-design

LOCAL_PACKAGE_NAME := HandlerThreadTest

include $(BUILD_PACKAGE)
 
include $(call all-makefiles-under,$(LOCAL_PATH))
