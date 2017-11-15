#!/usr/bin/env python

print 'Starting MATLAB interface (~10 seconds)...'
import matlab
import matlab.engine
eng = matlab.engine.start_matlab()

# Load intial data
with open('S001R01_22.txt', 'rb') as f:
    data = [float(x) for x in f.readlines()]

def main():
    features = eng.FeatureExt(matlab.double(data))
    print 'Feature extraction successful...'
    print features

    print 'Length = ', len(features)

    print 'Trying comparator'
    signature = features
    signal_alpha = features
    # training_size = len(features) - 6
    training_size = len(features) / 6
    x = eng.Comparator(signature, signal_alpha, training_size)
    print 'Comparator returned', x

if __name__ == '__main__':
    main()