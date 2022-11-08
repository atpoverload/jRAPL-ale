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
#include "arch_spec.h"
#include "msr.h"

JNIEXPORT jstring JNICALL
Java_jrapl_Rapl_readEnergyStats(JNIEnv *env, jclass jcls) {
	char energy_str[512];
	energy_stat_t energy_stat_per_socket[getSocketNum()];
	EnergyStatCheck(energy_stat_per_socket);
	energy_stat_csv_string(energy_stat_per_socket, energy_str);
	return (*env)->NewStringUTF(env, energy_str);
}

JNIEXPORT jint JNICALL
Java_jrapl_Rapl_sockets(JNIEnv * env, jclass jcls) {
  return getSocketNum();
}

JNIEXPORT jstring JNICALL
Java_jrapl_Rapl_componentIndices(JNIEnv* env, jclass jcls) {
	char* order;
	switch(get_power_domains_supported(get_micro_architecture())) {
		case DRAM_GPU_CORE_PKG:
			order = "dram,gpu,core,pkg";
			break;
		case DRAM_CORE_PKG:
			order = "dram,core,pkg";
			break;
		case GPU_CORE_PKG:
			order = "gpu,core,pkg";
			break;
		default:
			order = "undefined";
	}
	return (*env)->NewStringUTF(env, order);
}
