#!/usr/bin/env python3

import os
import sys
import json
import statistics
import pandas as pd
from collections import Counter
from statistics import StatisticsError

'''-----------------------------------------------------------------------------'''

def filter_zero_columns(dataframe): #delete columns that are 0.0 down the line. on jolteon, this happens for core
    for column in dataframe:
        if sum(dataframe[column]) == 0.0:
            del dataframe[column]

def diff_list(l, wraparound = 0):
	diffs = []
	for i in range(1,len(l)):
		diff = l[i] - l[i-1]
		if diff < 0:
			print(".> caught negative diff:",diff,"l[i]:",l[i],"l[i-1]:",l[i-1],"wraparound:",wraparound)
			diff += wraparound#262143.99993896484#wraparound
			print("...> fixed to:",diff)
		if wraparound != 0 and diff > 100: # TODO: better way to determine too big. for now we know that its never gonna be anywhere near above 100 unless it was a bug
			print('...> enormous diff found:',diff)
			print('     replacing it with',diffs[-1])
			diff = diffs[-1]
		diffs.append(diff)
	return diffs
    # return [ float(float(l[i]) - float(l[i-1])) for i in range(1,len(l))]

def memory_data(benchmark, iteration, type):
    filename = '_'.join([benchmark, iteration, type]) + ".memory.json"
    with open(filename) as f: memdata = json.loads(f.read())
    samples = memdata['samples']
    del memdata['samples']
    ##.#.## -- Do not delete these! We will probably end up not includling these metrics, but we might!! Do not delete them unless they are confirmed useless!
    ##.#.##memdata['max'] = min(samples)
    ##.#.##memdata['min'] = max(samples)
    ##.#.##memdata['median'] = statistics.median(samples)
    memdata['avg'] = statistics.mean(samples)
    memdata['stdev'] = statistics.stdev(samples)
    del memdata['timestamps'] # this is useful for generating an individual time-series plot of the memory of an iteration. but not for here.
    return memdata

def some_stats(datalist):
    return {
        'numSamples': len(datalist),
        'avg': statistics.mean(datalist),
        'stdev': statistics.stdev(datalist)
    }

# takes a list of lists and returns a list where each index is the sum of the items at the sublist at that index
def parallel_list_sum(lists):
    assert( [len(l) for l in lists] == sorted(len(l) for l in lists) ) # all lists are the same length
    return [ sum([ lists[i][j] for i in range(len(lists)) ]) for j in range(len(lists[0])) ]

def make_path_absolute(path):
	return path if ( path.startswith('/') or path.startswith('~') ) else os.path.join(os.getcwd(), path)

'''-----------------------------------------------------------------------------'''

if len(sys.argv) != 3:
	print("usage: python3 "+sys.argv[0]+" <raw data directory> <directory to dump to>")
	exit(1)

data_dir = sys.argv[1]
result_dir = make_path_absolute(sys.argv[2])
os.chdir(data_dir)

datafiles = os.listdir()
datafilenames = list(set([ name.split('.')[0] for name in datafiles ])) #remove file extension

for filename in sorted([ f for f in datafilenames if not f.endswith("nojrapl")]): # potentially find a better way to gracefully deal with memory data files
    filename_parts = filename.split('_')
    if len(filename_parts) != 3: continue
    print("<=< started working on '"+filename+"'")
    benchmark = filename_parts[0]
    iteration = filename_parts[1]
    monitor_type = filename_parts[2]

    result = {}

    with open(filename+'.metadata.json') as fh:
        metadata = json.loads(fh.read())
        metadata['benchmark'] = benchmark # add these next 3 items to the metadata
        metadata['iteration'] = iteration
        metadata['monitor_type'] = monitor_type
        result['metadata'] = metadata

    result['memory'] = dict()
    result['memory']['jraplon']  = memory_data(result['metadata']['benchmark'], result['metadata']['iteration'], result['metadata']['monitor_type'])
    result['memory']['jraploff'] = memory_data(result['metadata']['benchmark'], result['metadata']['iteration'], 'nojrapl')

    energydata = pd.read_csv(filename+'.csv')
    filter_zero_columns(energydata)

    result['time-energy'] = dict()

    timestamps = energydata['timestamp']#.to_list()
    del energydata['timestamp']

    time = diff_list(timestamps.to_list())
    result['time-energy']['sample-period'] = some_stats(time)

    result['time-energy']['energy-per-sample'] = dict()
    result['time-energy']['power-per-sample']  = dict()

    power_domains = list(energydata.keys())
    for power_domain in power_domains:
        energy = diff_list (
            energydata[power_domain].to_list(),
            wraparound = metadata['energyWrapAround']
        )
        result['time-energy']['energy-per-sample'][power_domain] = some_stats(energy)

        assert len(energy) == len(time)
        time_seconds = [ t/1000000 for t in time ]
        power = [
            energy[i] / time_seconds[i]
            for i in range(len(energy))
        ]
        result['time-energy']['power-per-sample'][power_domain] = some_stats(power)

    condensed_power_domains = list(set([ p.split('_')[0] for p in power_domains ])) # name without the _socketN suffix
    for condensed_power_domain in condensed_power_domains:
        pds = [ p for p in power_domains if p.startswith(condensed_power_domain)]
        energy = parallel_list_sum([
            diff_list (
                energydata[power_domain].to_list(),
                wraparound=metadata['energyWrapAround']
            )
            for power_domain in pds
        ])
        result['time-energy']['energy-per-sample'][condensed_power_domain] = some_stats(energy)

        assert len(energy) == len(time)
        time_seconds = [ t/1000000 for t in time ]
        power = [
            energy[i] / time_seconds[i]
            for i in range(len(energy))
        ]
        result['time-energy']['power-per-sample'][condensed_power_domain]  = some_stats(power)

    with open (
        os.path.join (
            result_dir,
            filename+'.stats.json'
        ), 'w'
    ) as fh: fh.write(json.dumps(result))

    result.clear()
    print(">=> done processing, json result written to "+filename+".stats.json'")

