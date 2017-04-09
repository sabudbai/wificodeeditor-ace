LOCAL_PATH := $(call my-dir)




# main
include $(CLEAR_VARS)
LOCAL_MODULE    := 8cc
# List of source files *.cpp files
LOCAL_SRC_FILES := wificompile.c buffer.c cpp.c debug.c dict.c encoding.c error.c file.c gen.c lex.c main.c map.c  parse.c path.c set.c utiltest.c vector.c
# Using android logging library
LOCAL_LDLIBS := -llog
# Define pre-processor macro for log levels
LOCAL_CFLAGS := -DLOG_LEVEL=LOG_VERBOSE

CFLAGS += -DBUILD_DIR='"$(shell pwd)"'
CFLAGS=-Wall -Wno-strict-aliasing -std=gnu11 -g -I. -O0

include $(BUILD_SHARED_LIBRARY)