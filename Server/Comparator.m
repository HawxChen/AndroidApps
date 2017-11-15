function out = Comparator(signal_Alpha, signature, training_size)

labels = ones(2*training_size,1);
labels(training_size + 1:2*training_size) = 0;

signature_data = zeros(training_size,6);
signal_data = zeros(training_size,6);
for i=1:training_size
    offset = (i-1) * 6;
    signature_data(i,:) = signature(offset + 1: offset + 1 + 5);
    signal_data(i,:) = signal_Alpha(offset + 1: offset + 1 + 5);
end

NBC_input = [signature_data;signal_data];

O1 = fitNaiveBayes(NBC_input,labels);
C1 = O1.predict(NBC_input);
cMat = confusionmat(labels,C1)

if (cMat(1,1) > cMat(1,2)) && (cMat(2,2) > cMat(2,1))
    out = 0;
else
    out = 1;
end

