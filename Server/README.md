Install MATLAB
--------------
You'll need to install MATLAB before running the server. MATLAB is used to
run the instructor provided files for feature extraction and comparator.
Fortunately, as an ASU student, you can download and install MATLAB for free.
Just go to: https://myapps.asu.edu/app/matlab-2017b-student-home-use-licensing.
Running the installer is easy, but installation itself takes quite a while.

Then install the matlab engine for Python:

	https://www.mathworks.com/help/matlab/matlab_external/install-the-matlab-engine-for-python.html

Setting up your virtual environment
-----------------------------------
As the server portion is written in Python, a virtual environment is encouraged
to neatly manage the server dependencies without disrupting the system. Follow
the steps below to create and install a virtual environment.

Install the virtualenv tool which is used to create the virtual environments:

	C:\Python27\Scripts\pip.exe install virtualenv

Create and activate environment:

	mkdir env
	C:\Python27\Scripts\virtualenv.exe --system-site-packages env
	env\Scripts\activate

Now the command prompt will have "(env)" before the usual prompt.

Install requirements
--------------------
There are a few Python packages that are used in the server. Dependencies can
be installed using pip.

	pip install -r 