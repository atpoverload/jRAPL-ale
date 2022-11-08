#include <stdio.h>
#include <jni.h>
#include <stdlib.h>
#include <fcntl.h>
#include <unistd.h>
#include <math.h>
#include <stdint.h>
#include <string.h>
#include <inttypes.h>
#include <sys/types.h>

#include "energy_check_utils.h"

JNIEXPORT void JNICALL
Java_jrapl_NativeLibrary_initializeNative(JNIEnv *env, jclass jcls) {
	ProfileInit();
}

JNIEXPORT void JNICALL
Java_jrapl_NativeLibrary_deallocateNative(JNIEnv * env, jclass jcls) {
	ProfileDealloc();
}
