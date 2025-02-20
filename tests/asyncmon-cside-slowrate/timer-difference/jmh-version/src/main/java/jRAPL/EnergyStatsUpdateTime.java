/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
*/ 

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.time.Instant;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import java.util.ArrayList;

import java.io.FileWriter;
import java.io.IOException;

public class TimerTest {

	public native static long usecsCtimer()

	@State(Scope.Benchmark)
    public static class MyState {
		public ArrayList<Long> samples = new ArrayList<>(1024);

		int 

		@Setup(Level.Trial)
		public void setup() {

		}

		@TearDown(Level.Trial)
		public void tearDown() throws InterruptedException {

		}

		public void addSample(long usec) {
 			samples.add(usec);
 		}
	}
 
  
	@Benchmark
	@Fork(1)
	@Measurement(iterations = 1)
	@BenchmarkMode(Mode.Throughput)
	public void ctimer(MyState state) {
		state.samples.add(usecsCtimer());
	}

	@Benchmark
	@Fork(1)
	@Measurement(iterations = 1)
	@BenchmarkMode(Mode.Throughput)
	public void jtimer(MyState state) {
		Instant before = Instant.now();
		Thread.sleep(0);
		Instant after = Instant.now();
	}

}
/** This is a version where I did some of the data processing mid-collecting, but it was 
	more annoying to do in Java and took a bit away from the throughput to process while
	sampling, so the above benchmark just collects all the data and then it gets dumped to
	a file and parsed / plotted in an external Python script */

// 	@State(Scope.Benchmark)
// 	public static class MidProcessing {
// 		private SyncEnergyMonitor monitor;
// 		private static HashMap<Long,Long> histogramDram;
// 		private static HashMap<Long,Long> histogramCore;
// 		private static HashMap<Long,Long> histogramPkg;
// 		private static HashMap<Long,Long> histogramGpu;
// 
// 		private double dramLast = 0, gpuLast = 0, coreLast = 0, pkgLast = 0;
// 		private long dramLastTime = 0, gpuLastTime = 0, coreLastTime = 0, pkgLastTime = 0;
// 
// 		@Setup(Level.Trial)
// 		public void setup() {
// 			histogramDram = new HashMap();
//             histogramCore = new HashMap();
//             histogramPkg  = new HashMap();
// 			histogramGpu  = new HashMap();
// 			monitor = new SyncEnergyMonitor();
// 			monitor.activate();
// 		}
// 
// 		@TearDown(Level.Trial)
// 		public void tearDown() {
// 			monitor.deactivate();
// 			// try {
// 			// 	FileWriter writer = new FileWriter("dram-sample.json");
// 			// 	writer.write(histogramDram.toString());
// 			// 	writer.flush();
// 			// 	writer.close();
// 			// 	System.out.println("Successfully wrote to file.");
// 			// } catch (IOException ex) {
// 			// 	System.out.println("Error occurred dumping hashmap to file.");
// 			// 	ex.printStackTrace();
// 			// }
// 		}
// 
// 		public void addSample() {
// 			EnergyStats sample = monitor.getSample();
// 			long nowMicros = ChronoUnit.MICROS.between(Instant.EPOCH, sample.getTimestamp());
// 		
// 			if (sample.getDram() != dramLast) {
// 				long time = nowMicros - dramLastTime;
// 				histogramDram.put(
// 					time,
// 					(histogramDram.containsKey(time)) ? histogramDram.get(time)+1 : 1
// 				);
// 				dramLastTime = time;
// 				// System.out.println(time);
// 			}
// 			if (sample.getGpu() != gpuLast) {
// 				long time = nowMicros - gpuLastTime;
// 				histogramDram.put(
// 					time,
// 					(histogramGpu.containsKey(time)) ? histogramGpu.get(time)+1 : 1
// 				);
// 				gpuLastTime = time;
// 			}
// 			if (sample.getCore() != coreLast) {
// 				long time = nowMicros - coreLastTime;
// 				histogramCore.put(
// 					time,
// 					(histogramCore.containsKey(time)) ? histogramCore.get(time)+1 : 1
// 				);
// 				coreLastTime = time;
// 			}
// 			if (sample.getPackage() != pkgLast) {
// 				long time = nowMicros - pkgLastTime;
// 				histogramPkg.put(
// 					time,
// 					(histogramPkg.containsKey(time)) ? histogramPkg.get(time)+1 : 1
// 				);
// 				pkgLastTime = time;
// 			}
// 		}
// 
// 	}
// 
// 	@Benchmark
// 	@Fork(1)
// 	@Warmup(iterations = 1)
// 	@Measurement(iterations = 1)
// 	@BenchmarkMode(Mode.Throughput)
// 	public void midprocessAddSample(MidProcessing state) {
// 		state.addSample();
// 
