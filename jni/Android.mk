LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := RsTest
LOCAL_SRC_FILES := RsTest.cpp

include $(BUILD_SHARED_LIBRARY)
