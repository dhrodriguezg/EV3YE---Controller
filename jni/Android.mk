LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
OPENCV_INSTALL_MODULES:=on
OPENCV_CAMERA_MODULES:=off
include ${NDKROOT}/../OpenCV-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := jniOCV
LOCAL_SRC_FILES := nonfree_init.cpp sift.cpp surf.cpp findFeatures.cpp matchFeatures.cpp
LOCAL_LDLIBS +=  -llog -ldl

include $(BUILD_SHARED_LIBRARY)
