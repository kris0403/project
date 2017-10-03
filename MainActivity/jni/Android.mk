LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := HDCode2
LOCAL_SRC_FILES := HDCode2.cpp locator.cpp pixel_reader.cpp reed_solomon_code.cpp decoder.cpp demodulator.cpp
LOCAL_LDLIBS    := -ljnigraphics -llog

include $(BUILD_SHARED_LIBRARY)
