Accuracy Calculation
==============

This is the evaluation BraiNet's algorithm to justify different brainwaves.

How to run it
------------
*  #python2.7 ./accuracy.py --help
*  #python2.7 ./accuracy.py


How it works
------------
First of all, we feed all files under POSITIVE, NEGATIE folders to BraiNet's algorithm in order to produce the unique Hash ID for each file. Each file presents one type of brainwave. Second, we compare all Hash IDs with all IDs generated from the files of the POSITIVE folder through Comparator function in order to lable all brainwaves as the positivie or negative flag. Finally, we divide these labes from Positive/Negative flags into True Positive, False Positive, True Negative and False Negative through the files in the POSITIVE or NEGATIVE folders.

