function out = FeatureExt(signal)
% in seconds
signal_len = 120;
% sample rate in HZ
SR = 512;

out = zeros(signal_len*6,1);
S_FFT = zeros(SR,1);
Temp = zeros(SR,1);


for i=1:signal_len
    offset = (i-1) * SR;
   % Temp(1:i) = signal (1:i);
    S_FFT(1:SR) = FFT(signal(offset + 1 : offset + SR));
    out((i-1)*6 + 1:(i-1)*6 + 1 + 5) = S_FFT(8:13);
end
end