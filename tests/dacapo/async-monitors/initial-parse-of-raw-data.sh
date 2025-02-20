#!/bin/bash

function echo_eval() {
    echo "))) $@" && eval "$@"
}

set -e

sudo -v

samplingrates='1 2 4 8'
systems='SystemA SystemB'

for system in $systems; do
    for r in $samplingrates; do
	(
		echo_eval \
			./scripts/analysis/analysis-periteration.py \
			./results/$system/samplingrate_$r/raw \
			./results/$system/samplingrate_$r/intermediate-results
		
		printf "Subject: parsing status.\ndone with $system $samplingrate_$r\n" \
			>> status.txt
	)&
    done
done
