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
    with open(path, 'rb') as f:
        data = [float(x) for x in f.readlines()]

    # Now run the feature extractor on the input data
    try:
        features = eng.FeatureExt(matlab.double(data))
    except:
        print path
        return None

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

def build_features_from_files (files):
    features_list = []
    for f in files:
        features = create_signature_from_signal_file(f)
        features_list.append((f, features))

    return features_list

POSITIVE = 1
NEGATIVE = 2
def verify_all_test ():
    TP = 0
    FP = 0
    TN = 0
    FN = 0
    for testing_f, testing_feature in all_list:
        result = 0
        for pst_f, pst_feature in pst_list:
            result = compare_signatures(testing_feature, pst_feature)
            if(result != 0): break
        mark = True
        if result != 0: # POSITIVE
            for pst_f in pst_files_list:
                if(testing_f == pst_f): #True Positive
                    mark = False
                    TP = TP + 1 
            if(mark):
                FP = FP + 1 #False Positive
            
        else: # NEGATIVE
            for ngt_f in ngt_files_list:
                if(testing_f == ngt_f): #True Negative
                    mark = False
                    TN = TN + 1
            if(mark):
                FN = FN + 1 #False Negative

    return TP, FP, TN, FN


    

def main():
    args = ap.parse_args()
    global pst_dir
    global ngt_dir
    pst_dir = args.pst_dir
    ngt_dir = args.ngt_dir
    fext = '*' + args.file_extension;

    print "POSITIVE data directory", pst_dir, "NEGATIVE data directory: ", ngt_dir
    # Start MATLAB interface
    print "starting Matlab engine..."
    global eng
    eng = matlab.engine.start_matlab()
    print "Done starting Matlab engine..."

    global pst_list
    global pst_files_list
    pst_files_list = glob.glob(pst_dir + fext) 
    pst_files_list.sort()
    pst_list = build_features_from_files(pst_files_list)
    
    global ngt_list
    global ngt_files_list
    ngt_files_list = glob.glob(ngt_dir + fext) 
    ngt_files_list.sort()
    ngt_list = build_features_from_files(ngt_files_list)

    global all_list
    all_files_list = ngt_files_list + pst_files_list
#    all_list = build_features_from_files(all_files_list)
    all_list = pst_list + ngt_list
    
    TP, FP, TN, FN = verify_all_test()
    print "TP:", TP, ";FP:", FP, ";TN:", TN, ";FN:", FN

    #ACC = (TP + TN) / (TP + FP + FN + TN)
    print "Accuracy: ", float(TP + TN) / float(TP + FP + FN + TN)


if __name__ == '__main__':
    main()
