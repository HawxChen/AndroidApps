#!/usr/bin/env python
import matlab
import matlab.engine
import os
from random import random
from hashlib import sha256
import pickle
import time
import glob
import argparse

def create_signature_from_signal_file(path):
    # Input file should contain samples
    #   - Captured at 160 Hz
    #   - 60s worth of data
    print path
    with open(path, 'rb') as f:
        data = [float(x) for x in f.readlines()]

    # Now run the feature extractor on the input data
    features = eng.FeatureExt(matlab.double(data))
    return features

def compare_signatures(sig1, sig2):
    # Compare two signatures
    # training_size = len(features) - 6
    training_size = len(sig1) / 12
    x = eng.Comparator(sig1, sig2, training_size)
    print 'Comparator returned', x
    return x

ap = argparse.ArgumentParser(description='calculate accuracy based on the two folders: POSITIVE data and NEGATIVE data')
ap.add_argument('--pst_dir', type=str, help='POSITIVE data directory', default="./POSITIVE/")
ap.add_argument('--ngt_dir', type=str, help='NEGATIVE data dir', default="./NEGATIVE/")
ap.add_argument('--file_extension', type=str, help='NEGATIVE data dir', default="txt")

#
# Main Function (runs everything)
#

def build_hashes_from_dir (path, fext):
    files = glob.glob(path + fext) 
    for f in files:
        features = create_signature_from_signal_file(f)


def main():

    args = ap.parse_args()
    global pst_dir
    global ngt_dir
    global fext
    pst_dir = args.pst_dir
    ngt_dir = args.ngt_dir
    fext = '*' + args.file_extension;

    print "POSITIVE data directory", pst_dir, "NEGATIVE data directory: ", ngt_dir
    # Start MATLAB interface
    print "starting Matlab engine..."
    global eng
    eng = matlab.engine.start_matlab()
    print "Done starting Matlab engine..."

    build_hashes_from_dir(pst_dir, fext)

    
    


if __name__ == '__main__':
    main()
