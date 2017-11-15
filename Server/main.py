#!/usr/bin/env python

eng = None

def init_matlab():
    print 'Starting MATLAB interface (~10 seconds)...'
    import matlab.engine
    eng = matlab.engine.start_matlab()

x = 5

def main():
    print x

if __name__ == '__main__':
    main()