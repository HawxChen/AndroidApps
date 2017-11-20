package com.example.hawx.a01_healthmonitor;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_print_interface;
import libsvm.svm_problem;

class svm_train {
    private svm_parameter param;
    private svm_problem prob;
    private svm_model model;
    private String input_file_name;
    private String model_file_name;
    private String error_msg;
    private int cross_validation;
    private int nr_fold;
    private static svm_print_interface svm_print_null = new svm_print_interface() {
        public void print(String var1) {
        }
    };

    svm_train() {
    }

    private static void exit_with_help() {
        System.out.print("Usage: svm_train [options] training_set_file [model_file]\noptions:\n-s svm_type : set type of SVM (default 0)\n\t0 -- C-SVC\t\t(multi-class classification)\n\t1 -- nu-SVC\t\t(multi-class classification)\n\t2 -- one-class SVM\n\t3 -- epsilon-SVR\t(regression)\n\t4 -- nu-SVR\t\t(regression)\n-t kernel_type : set type of kernel function (default 2)\n\t0 -- linear: u\'*v\n\t1 -- polynomial: (gamma*u\'*v + coef0)^degree\n\t2 -- radial basis function: exp(-gamma*|u-v|^2)\n\t3 -- sigmoid: tanh(gamma*u\'*v + coef0)\n\t4 -- precomputed kernel (kernel values in training_set_file)\n-d degree : set degree in kernel function (default 3)\n-g gamma : set gamma in kernel function (default 1/num_features)\n-r coef0 : set coef0 in kernel function (default 0)\n-c cost : set the parameter C of C-SVC, epsilon-SVR, and nu-SVR (default 1)\n-n nu : set the parameter nu of nu-SVC, one-class SVM, and nu-SVR (default 0.5)\n-p epsilon : set the epsilon in loss function of epsilon-SVR (default 0.1)\n-m cachesize : set cache memory size in MB (default 100)\n-e epsilon : set tolerance of termination criterion (default 0.001)\n-h shrinking : whether to use the shrinking heuristics, 0 or 1 (default 1)\n-b probability_estimates : whether to train a SVC or SVR model for probability estimates, 0 or 1 (default 0)\n-wi weight : set the parameter C of class i to weight*C, for C-SVC (default 1)\n-v n : n-fold cross validation mode\n-q : quiet mode (no outputs)\n");
        System.exit(1);
    }

    private void do_cross_validation() {
        int var2 = 0;
        double var3 = 0.0D;
        double var5 = 0.0D;
        double var7 = 0.0D;
        double var9 = 0.0D;
        double var11 = 0.0D;
        double var13 = 0.0D;
        double[] var15 = new double[this.prob.l];
        svm.svm_cross_validation(this.prob, this.param, this.nr_fold, var15);
        int var1;
        if(this.param.svm_type != 3 && this.param.svm_type != 4) {
            for(var1 = 0; var1 < this.prob.l; ++var1) {
                if(var15[var1] == this.prob.y[var1]) {
                    ++var2;
                }
            }

            System.out.print("Cross Validation Accuracy = " + 100.0D * (double)var2 / (double)this.prob.l + "%\n");
        } else {
            for(var1 = 0; var1 < this.prob.l; ++var1) {
                double var16 = this.prob.y[var1];
                double var18 = var15[var1];
                var3 += (var18 - var16) * (var18 - var16);
                var5 += var18;
                var7 += var16;
                var9 += var18 * var18;
                var11 += var16 * var16;
                var13 += var18 * var16;
            }

            System.out.print("Cross Validation Mean squared error = " + var3 / (double)this.prob.l + "\n");
            System.out.print("Cross Validation Squared correlation coefficient = " + ((double)this.prob.l * var13 - var5 * var7) * ((double)this.prob.l * var13 - var5 * var7) / (((double)this.prob.l * var9 - var5 * var5) * ((double)this.prob.l * var11 - var7 * var7)) + "\n");
        }

    }

    private void run(String[] var1) throws IOException {
        this.parse_command_line(var1);
        this.read_problem();
        this.error_msg = svm.svm_check_parameter(this.prob, this.param);
        if(this.error_msg != null) {
            System.err.print("ERROR: " + this.error_msg + "\n");
            System.exit(1);
        }

        if(this.cross_validation != 0) {
            this.do_cross_validation();
        } else {
            this.model = svm.svm_train(this.prob, this.param);
            svm.svm_save_model(this.model_file_name, this.model);
        }

    }

    public static void main(String[] var0) throws IOException {
        svm_train var1 = new svm_train();
        var1.run(var0);
    }

    private static double atof(String var0) {
        double var1 = Double.valueOf(var0).doubleValue();
        if(Double.isNaN(var1) || Double.isInfinite(var1)) {
            System.err.print("NaN or Infinity in input\n");
            System.exit(1);
        }

        return var1;
    }

    private static int atoi(String var0) {
        return Integer.parseInt(var0);
    }

    private void parse_command_line(String[] var1) {
        svm_print_interface var3 = null;
        this.param = new svm_parameter();
        this.param.svm_type = 0;
        this.param.kernel_type = 2;
        this.param.degree = 3;
        this.param.gamma = 0.0D;
        this.param.coef0 = 0.0D;
        this.param.nu = 0.5D;
        this.param.cache_size = 100.0D;
        this.param.C = 1.0D;
        this.param.eps = 0.001D;
        this.param.p = 0.1D;
        this.param.shrinking = 1;
        this.param.probability = 0;
        this.param.nr_weight = 0;
        this.param.weight_label = new int[0];
        this.param.weight = new double[0];
        this.cross_validation = 0;

        int var2;
        for(var2 = 0; var2 < var1.length && var1[var2].charAt(0) == 45; ++var2) {
            ++var2;
            if(var2 >= var1.length) {
                exit_with_help();
            }

            switch(var1[var2 - 1].charAt(1)) {
                case 'b':
                    this.param.probability = atoi(var1[var2]);
                    break;
                case 'c':
                    this.param.C = atof(var1[var2]);
                    break;
                case 'd':
                    this.param.degree = atoi(var1[var2]);
                    break;
                case 'e':
                    this.param.eps = atof(var1[var2]);
                    break;
                case 'f':
                case 'i':
                case 'j':
                case 'k':
                case 'l':
                case 'o':
                case 'u':
                default:
                    System.err.print("Unknown option: " + var1[var2 - 1] + "\n");
                    exit_with_help();
                    break;
                case 'g':
                    this.param.gamma = atof(var1[var2]);
                    break;
                case 'h':
                    this.param.shrinking = atoi(var1[var2]);
                    break;
                case 'm':
                    this.param.cache_size = atof(var1[var2]);
                    break;
                case 'n':
                    this.param.nu = atof(var1[var2]);
                    break;
                case 'p':
                    this.param.p = atof(var1[var2]);
                    break;
                case 'q':
                    var3 = svm_print_null;
                    --var2;
                    break;
                case 'r':
                    this.param.coef0 = atof(var1[var2]);
                    break;
                case 's':
                    this.param.svm_type = atoi(var1[var2]);
                    break;
                case 't':
                    this.param.kernel_type = atoi(var1[var2]);
                    break;
                case 'v':
                    this.cross_validation = 1;
                    this.nr_fold = atoi(var1[var2]);
                    if(this.nr_fold < 2) {
                        System.err.print("n-fold cross validation: n must >= 2\n");
                        exit_with_help();
                    }
                    break;
                case 'w':
                    ++this.param.nr_weight;
                    int[] var4 = this.param.weight_label;
                    this.param.weight_label = new int[this.param.nr_weight];
                    System.arraycopy(var4, 0, this.param.weight_label, 0, this.param.nr_weight - 1);
                    double[] var5 = this.param.weight;
                    this.param.weight = new double[this.param.nr_weight];
                    System.arraycopy(var5, 0, this.param.weight, 0, this.param.nr_weight - 1);
                    this.param.weight_label[this.param.nr_weight - 1] = atoi(var1[var2 - 1].substring(2));
                    this.param.weight[this.param.nr_weight - 1] = atof(var1[var2]);
            }
        }

        svm.svm_set_print_string_function(var3);
        if(var2 >= var1.length) {
            exit_with_help();
        }

        this.input_file_name = var1[var2];
        if(var2 < var1.length - 1) {
            this.model_file_name = var1[var2 + 1];
        } else {
            int var6 = var1[var2].lastIndexOf(47);
            ++var6;
            this.model_file_name = var1[var2].substring(var6) + ".model";
        }

    }

    private void read_problem() throws IOException {
        BufferedReader var1 = new BufferedReader(new FileReader(this.input_file_name));
        Vector var2 = new Vector();
        Vector var3 = new Vector();
        int var4 = 0;

        while(true) {
            String var5 = var1.readLine();
            if(var5 == null) {
                this.prob = new svm_problem();
                this.prob.l = var2.size();
                this.prob.x = new svm_node[this.prob.l][];

                int var10;
                for(var10 = 0; var10 < this.prob.l; ++var10) {
                    this.prob.x[var10] = (svm_node[])var3.elementAt(var10);
                }

                this.prob.y = new double[this.prob.l];

                for(var10 = 0; var10 < this.prob.l; ++var10) {
                    this.prob.y[var10] = ((Double)var2.elementAt(var10)).doubleValue();
                }

                if(this.param.gamma == 0.0D && var4 > 0) {
                    this.param.gamma = 1.0D / (double)var4;
                }

                if(this.param.kernel_type == 4) {
                    for(var10 = 0; var10 < this.prob.l; ++var10) {
                        if(this.prob.x[var10][0].index != 0) {
                            System.err.print("Wrong kernel matrix: first column must be 0:sample_serial_number\n");
                            System.exit(1);
                        }

                        if((int)this.prob.x[var10][0].value <= 0 || (int)this.prob.x[var10][0].value > var4) {
                            System.err.print("Wrong input format: sample_serial_number out of range\n");
                            System.exit(1);
                        }
                    }
                }

                var1.close();
                return;
            }

            StringTokenizer var6 = new StringTokenizer(var5, " \t\n\r\f:");
            var2.addElement(Double.valueOf(atof(var6.nextToken())));
            int var7 = var6.countTokens() / 2;
            svm_node[] var8 = new svm_node[var7];

            for(int var9 = 0; var9 < var7; ++var9) {
                var8[var9] = new svm_node();
                var8[var9].index = atoi(var6.nextToken());
                var8[var9].value = atof(var6.nextToken());
            }

            if(var7 > 0) {
                var4 = Math.max(var4, var8[var7 - 1].index);
            }

            var3.addElement(var8);
        }
    }
}
