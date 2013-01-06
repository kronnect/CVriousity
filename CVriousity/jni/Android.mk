LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include /home/ramcor/OpenCV-android/sdk/native/jni/OpenCV.mk


LOCAL_MODULE    := mixed_sample
LOCAL_SRC_FILES := jni_part.cpp
LOCAL_LDLIBS +=  -llog -ldl

include $(BUILD_SHARED_LIBRARY)

DUMMY := $(shell /home/ramcor/android-sdk/platform-tools/adb uninstall org.cvriousity.testapp)


