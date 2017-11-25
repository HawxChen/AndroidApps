package com.group25.activityclassification;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

class Classifier {

    private String modelDir;
    private String modelName;
    private String modelPath;
    public  String mErrorMessage;
    public  float  mCrossValidationAccuracy;

    public Classifier() {
        // Path to model in the filesystem
        modelDir = Environment.getExternalStorageDirectory().getPath() + File.separator + "Cse535_Group25";
        modelName = "model";
        modelPath = modelDir + File.separator + modelName;
    }

    public Boolean isModelAvailable() {
        return (new File(modelPath)).exists();
    }

    public void destroyModel() {
        File f = new File(modelPath);
        if (f.exists()) {
            f.delete();
        }
    }

    public ActivityType classifyActivity(UserActivity activity) {
        try {
            // Write activity in libsvm format to a temp file
            File inputFile = new File(modelDir, "input");
            FileOutputStream fout = new FileOutputStream(inputFile);
            PrintWriter p = new PrintWriter(fout);
            p.write(activity.getSvmFormat());
            p.close();

            // Run svm_predict on the model + input file
            File outputFile = new File(modelDir, "output");
            String[] argv = new String[3];
            argv[0] = inputFile.getPath();
            argv[1] = modelPath;
            argv[2] = outputFile.getPath();

            // Read output file to get activity type
            svm_predict.main(argv);
            BufferedReader br = new BufferedReader(new FileReader(outputFile));
            String line = br.readLine();

            if (line == null) {
                return ActivityType.ACTIVITY_UNKNOWN;
            }

            int activityId = (int)Float.parseFloat(line);

            br.close();

            switch (activityId) {
                case -1: return ActivityType.ACTIVITY_WALKING;
                case  0: return ActivityType.ACTIVITY_RUNNING;
                case  1: return ActivityType.ACTIVITY_JUMPING;
                default: return ActivityType.ACTIVITY_UNKNOWN;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return ActivityType.ACTIVITY_UNKNOWN;
        }
    }

    public int train(ArrayList<UserActivity> activities, float cost, float gamma) {

        Log.d("CLASSIFIER", String.format("Starting training (cost=%f, gamma=%f)", cost, gamma));
        mErrorMessage = "";
        mCrossValidationAccuracy = 0;

        try {
            // Write activity in libsvm format to a temp file
            File inputFile = new File(modelDir, "input");
            FileOutputStream fout = new FileOutputStream(inputFile);
            PrintWriter p = new PrintWriter(fout);
            for (int i = 0; i < activities.size(); i++) {
                p.write(activities.get(i).getSvmFormat());
            }
            p.close();

            // Run svm_train on the training data (svm_train -c 2.0 -g 0.125 -v 4 input model)
            String[] argv = new String[8];
            argv[0] = "-c"; argv[1] = String.format("%f", cost);
            argv[2] = "-g"; argv[3] = String.format("%f", gamma);
            argv[4] = "-v"; argv[5] = "4";
            argv[6] = inputFile.getPath();
            argv[7] = modelPath;

            svm_train trainer = new svm_train();
            int result = trainer.run(argv);
            mErrorMessage = trainer.error_msg;
            mCrossValidationAccuracy = trainer.cross_validation_result;
            return result;

        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }
    }
}
