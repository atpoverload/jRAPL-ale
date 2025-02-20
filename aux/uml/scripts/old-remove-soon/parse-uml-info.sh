#!/bin/bash

outfile='parse-uml-info.output'

cat /dev/null > $outfile 

for jfile in $(ls ../src/*.java)
do
	echo "========================$jfile==========================" | tee -a $outfile
	python3 parse-uml-info.py $jfile | tee -a $outfile
	echo | tee -a $outfile
done
