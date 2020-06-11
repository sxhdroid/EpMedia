LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE :=  libijkffmpeg
LOCAL_SRC_FILES := prebuilt/libijkffmpeg.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm
LOCAL_MODULE := ffmpegCmd


LOCAL_SRC_FILES := Jni_FFmpegCmd.c\
                   cmdutils.c \
                   ffmpeg.c \
                   ffmpeg_opt.c \
                   ffmpeg_filter.c \
                   ffmpeg_thread.c

LOCAL_C_INCLUDES := $(LOCAL_PATH)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include/
LOCAL_C_INCLUDES += /Users/apple/Desktop/sxhdroid/ijkplayer/extra/ffmpeg

LOCAL_LDLIBS := -llog -ljnigraphics -lz -landroid -lm -pthread -lOpenSLES
LOCAL_SHARED_LIBRARIES := libijkffmpeg

include $(BUILD_SHARED_LIBRARY)