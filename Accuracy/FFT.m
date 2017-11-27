function y =FFT(signal)
    Fs = 512;                    % Sampling frequency
    T = 1/Fs;                     % Sample time
    L = size(signal,1);           % Length of signal
    t = (0:L-1)*T;                % Time vector
    NFFT = 2^nextpow2(L);         % Next power of 2 from length of y
    f = Fs/2*linspace(0,1,NFFT/2+1);
    Y = fft(signal,NFFT)/L;
    %y = 2*abs(Y(1:NFFT/2+1));
    y = 2*abs(Y);
end
