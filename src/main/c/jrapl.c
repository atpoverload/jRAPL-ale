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

#include "JNIFunctionDeclarations.h"

static uint64_t num_sockets;
static int power_domains_supported; // this variable is not necessary to store in this file scope, or is it?

JNIEXPORT void JNICALL
Java_jrapl_JRapl_initialize(JNIEnv *env, jclass jcls) {
	num_sockets = getSocketNum();
	power_domains_supported = get_power_domains_supported(get_micro_architecture()); // this variable is not necessary to store in this file scope, or is it?
	ProfileInit();
}

JNIEXPORT void JNICALL
Java_jrapl_JRapl_deallocate(JNIEnv * env, jclass jcls) {
	ProfileDealloc();
}

JNIEXPORT jstring JNICALL
Java_jrapl_JRapl_readEnergyStats(JNIEnv *env, jclass jcls) {
	char energy_str[512];
	energy_stat_t energy_stat_per_socket[num_sockets];
	EnergyStatCheck(energy_stat_per_socket);
	energy_stat_csv_string(energy_stat_per_socket, energy_str);
	return (*env)->NewStringUTF(env, energy_str);
}
