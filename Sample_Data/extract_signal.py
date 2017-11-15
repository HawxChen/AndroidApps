#!/usr/bin/env python
"""
Extract a signal from an EDF+ file
"""
import pyedflib
# import numpy as np
import argparse

ap = argparse.ArgumentParser(description='Extract a signal from an EDF+ file')
ap.add_argument('file', type=str, help='input filename')
ap.add_argument('signal', type=int, help='number of signal to extract (>=1)')
args = ap.parse_args()

f = pyedflib.EdfReader(args.file)

n = f.signals_in_file
print 'Found %d signals in the file' % n

if args.signal < 1 or args.signal > f.signals_in_file:
	print 'Error: signal out of range'
	exit(1)

signal_labels = f.getSignalLabels()
print 'Extracting signal #%d (%s)' % (args.signal, signal_labels[args.signal-1])

# sigbufs = np.zeros((n, f.getNSamples()[0]))
# for i in np.arange(n):
#     sigbufs[i, :] = f.readSignal(i)
#     print sigbufs[i, :]

for s in f.readSignal(args.signal-1):
	print s
