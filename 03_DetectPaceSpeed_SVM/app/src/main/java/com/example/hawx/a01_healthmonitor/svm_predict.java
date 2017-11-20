package com.example.hawx.a01_healthmonitor;

/**
 * Created by Hawx on 20/11/2017.
 */

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_print_interface;

class svm_predict {
    private static svm_print_interface svm_print_null = new svm_print_interface() {
        public void print(String var1) {
        }
    };
    private static svm_print_interface svm_print_stdout = new svm_print_interface() {
        public void print(String var1) {
            System.out.print(var1);
        }
    };
    private static svm_print_interface svm_print_string;

    svm_predict() {
    }

    static void info(String var0) {
        svm_print_string.print(var0);
    }

    private static double atof(String var0) {
        return Double.valueOf(var0).doubleValue();
    }

    private static int atoi(String var0) {
        return Integer.parseInt(var0);
    }

    private static void predict(BufferedReader var0, DataOutputStream var1, svm_model var2, int var3) throws IOException {
        int var4 = 0;
        int var5 = 0;
        double var6 = 0.0D;
        double var8 = 0.0D;
        double var10 = 0.0D;
        double var12 = 0.0D;
        double var14 = 0.0D;
        double var16 = 0.0D;
        int var18 = svm.svm_get_svm_type(var2);
        int var19 = svm.svm_get_nr_class(var2);
        double[] var20 = null;
        if(var3 == 1) {
            if(var18 != 3 && var18 != 4) {
                int[] var21 = new int[var19];
                svm.svm_get_labels(var2, var21);
                var20 = new double[var19];
                var1.writeBytes("labels");

                for(int var22 = 0; var22 < var19; ++var22) {
                    var1.writeBytes(" " + var21[var22]);
                }

                var1.writeBytes("\n");
            } else {
                info("Prob. model for test data: target value = predicted value + z,\nz: Laplace distribution e^(-|z|/sigma)/(2sigma),sigma=" + svm.svm_get_svr_probability(var2) + "\n");
            }
        }

        while(true) {
            String var30 = var0.readLine();
            if(var30 == null) {
                if(var18 != 3 && var18 != 4) {
                    info("Accuracy = " + (double)var4 / (double)var5 * 100.0D + "% (" + var4 + "/" + var5 + ") (classification)\n");
                } else {
                    info("Mean squared error = " + var6 / (double)var5 + " (regression)\n");
                    info("Squared correlation coefficient = " + ((double)var5 * var16 - var8 * var10) * ((double)var5 * var16 - var8 * var10) / (((double)var5 * var12 - var8 * var8) * ((double)var5 * var14 - var10 * var10)) + " (regression)\n");
                }

                return;
            }

            StringTokenizer var31 = new StringTokenizer(var30, " \t\n\r\f:");
            double var23 = atof(var31.nextToken());
            int var25 = var31.countTokens() / 2;
            svm_node[] var26 = new svm_node[var25];

            for(int var27 = 0; var27 < var25; ++var27) {
                var26[var27] = new svm_node();
                var26[var27].index = atoi(var31.nextToken());
                var26[var27].value = atof(var31.nextToken());
            }

            double var32;
            if(var3 != 1 || var18 != 0 && var18 != 1) {
                var32 = svm.svm_predict(var2, var26);
                var1.writeBytes(var32 + "\n");
            } else {
                var32 = svm.svm_predict_probability(var2, var26, var20);
                var1.writeBytes(var32 + " ");

                for(int var29 = 0; var29 < var19; ++var29) {
                    var1.writeBytes(var20[var29] + " ");
                }

                var1.writeBytes("\n");
            }

            if(var32 == var23) {
                ++var4;
            }

            var6 += (var32 - var23) * (var32 - var23);
            var8 += var32;
            var10 += var23;
            var12 += var32 * var32;
            var14 += var23 * var23;
            var16 += var32 * var23;
            ++var5;
        }
    }

    private static void exit_with_help() {
        System.err.print("usage: svm_predict [options] test_file model_file output_file\noptions:\n-b probability_estimates: whether to predict probability estimates, 0 or 1 (default 0); one-class SVM not supported yet\n-q : quiet mode (no outputs)\n");
        System.exit(1);
    }

    public static void main(String[] var0) throws IOException {
        int var2 = 0;
        svm_print_string = svm_print_stdout;

        int var1;
        for(var1 = 0; var1 < var0.length && var0[var1].charAt(0) == 45; ++var1) {
            ++var1;
            switch(var0[var1 - 1].charAt(1)) {
                case 'b':
                    var2 = atoi(var0[var1]);
                    break;
                case 'q':
                    svm_print_string = svm_print_null;
                    --var1;
                    break;
                default:
                    System.err.print("Unknown option: " + var0[var1 - 1] + "\n");
                    exit_with_help();
            }
        }

        if(var1 >= var0.length - 2) {
            exit_with_help();
        }

        try {
            BufferedReader var3 = new BufferedReader(new FileReader(var0[var1]));
            DataOutputStream var4 = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(var0[var1 + 2])));
            svm_model var5 = svm.svm_load_model(var0[var1 + 1]);
            if(var5 == null) {
                System.err.print("can\'t open model file " + var0[var1 + 1] + "\n");
                System.exit(1);
            }

            if(var2 == 1) {
                if(svm.svm_check_probability_model(var5) == 0) {
                    System.err.print("Model does not support probabiliy estimates\n");
                    System.exit(1);
                }
            } else if(svm.svm_check_probability_model(var5) != 0) {
                info("Model supports probability estimates, but disabled in prediction.\n");
            }

            predict(var3, var4, var5, var2);
            var3.close();
            var4.close();
        } catch (FileNotFoundException var6) {
            exit_with_help();
        } catch (ArrayIndexOutOfBoundsException var7) {
            exit_with_help();
        }

    }

    static {
        svm_print_string = svm_print_stdout;
    }
}