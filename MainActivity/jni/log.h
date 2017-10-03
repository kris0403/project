/*
 * log.h
 *
 *  Created on: 2014年7月30日
 *      Author: 安然
 */

#ifndef LOG_H_
#define LOG_H_

#include <android/log.h>

#define logi(...) __android_log_print(ANDROID_LOG_INFO,##__VA_ARGS__)
#define logw(...) __android_log_print(ANDROID_LOG_WARNING,##__VA_ARGS__)

#endif /* LOG_H_ */
