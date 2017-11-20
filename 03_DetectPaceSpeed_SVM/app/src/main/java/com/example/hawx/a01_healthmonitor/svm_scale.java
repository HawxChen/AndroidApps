package com.example.hawx.a01_healthmonitor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Formatter;
import java.util.StringTokenizer;

class svm_scale {
    private String line = null;
    private double lower = -1.0D;
    private double upper = 1.0D;
    private double y_lower;
    private double y_upper;
    private boolean y_scaling = false;
    private double[] feature_max;
    private double[] feature_min;
    private double y_max = -1.7976931348623157E308D;
    private double y_min = 1.7976931348623157E308D;
    private int max_index;
    private long num_nonzeros = 0L;
    private long new_num_nonzeros = 0L;

    svm_scale() {
    }

    private static void exit_with_help() {
        System.out.print("Usage: svm-scale [options] data_filename\noptions:\n-l lower : x scaling lower limit (default -1)\n-u upper : x scaling upper limit (default +1)\n-y y_lower y_upper : y scaling limits (default: no y scaling)\n-s save_filename : save scaling parameters to save_filename\n-r restore_filename : restore scaling parameters from restore_filename\n");
        System.exit(1);
    }

    private BufferedReader rewind(BufferedReader var1, String var2) throws IOException {
        var1.close();
        return new BufferedReader(new FileReader(var2));
    }

    private void output_target(double var1) {
        if(this.y_scaling) {
            if(var1 == this.y_min) {
                var1 = this.y_lower;
            } else if(var1 == this.y_max) {
                var1 = this.y_upper;
            } else {
                var1 = this.y_lower + (this.y_upper - this.y_lower) * (var1 - this.y_min) / (this.y_max - this.y_min);
            }
        }

        System.out.print(var1 + " ");
    }

    private void output(int var1, double var2) {
        if(this.feature_max[var1] != this.feature_min[var1]) {
            if(var2 == this.feature_min[var1]) {
                var2 = this.lower;
            } else if(var2 == this.feature_max[var1]) {
                var2 = this.upper;
            } else {
                var2 = this.lower + (this.upper - this.lower) * (var2 - this.feature_min[var1]) / (this.feature_max[var1] - this.feature_min[var1]);
            }

            if(var2 != 0.0D) {
                System.out.print(var1 + ":" + var2 + " ");
                ++this.new_num_nonzeros;
            }

        }
    }

    private String readline(BufferedReader var1) throws IOException {
        this.line = var1.readLine();
        return this.line;
    }

    private void run(String[] var1) throws IOException {
        BufferedReader var4 = null;
        BufferedReader var5 = null;
        String var6 = null;
        String var7 = null;
        String var8 = null;

        int var2;
        for(var2 = 0; var2 < var1.length && var1[var2].charAt(0) == 45; ++var2) {
            ++var2;
            switch(var1[var2 - 1].charAt(1)) {
                case 'l':
                    this.lower = Double.parseDouble(var1[var2]);
                    break;
                case 'm':
                case 'n':
                case 'o':
                case 'p':
                case 'q':
                case 't':
                case 'v':
                case 'w':
                case 'x':
                default:
                    System.err.println("unknown option");
                    exit_with_help();
                    break;
                case 'r':
                    var7 = var1[var2];
                    break;
                case 's':
                    var6 = var1[var2];
                    break;
                case 'u':
                    this.upper = Double.parseDouble(var1[var2]);
                    break;
                case 'y':
                    this.y_lower = Double.parseDouble(var1[var2]);
                    ++var2;
                    this.y_upper = Double.parseDouble(var1[var2]);
                    this.y_scaling = true;
            }
        }

        if(this.upper <= this.lower || this.y_scaling && this.y_upper <= this.y_lower) {
            System.err.println("inconsistent lower/upper specification");
            System.exit(1);
        }

        if(var7 != null && var6 != null) {
            System.err.println("cannot use -r and -s simultaneously");
            System.exit(1);
        }

        if(var1.length != var2 + 1) {
            exit_with_help();
        }

        var8 = var1[var2];

        try {
            var4 = new BufferedReader(new FileReader(var8));
        } catch (Exception var21) {
            System.err.println("can\'t open file " + var8);
            System.exit(1);
        }

        this.max_index = 0;
        int var9;
        if(var7 != null) {
            try {
                var5 = new BufferedReader(new FileReader(var7));
            } catch (Exception var20) {
                System.err.println("can\'t open file " + var7);
                System.exit(1);
            }

            if(var5.read() == 121) {
                var5.readLine();
                var5.readLine();
                var5.readLine();
            }

            var5.readLine();
            var5.readLine();

            for(String var11 = null; (var11 = var5.readLine()) != null; this.max_index = Math.max(this.max_index, var9)) {
                StringTokenizer var12 = new StringTokenizer(var11);
                var9 = Integer.parseInt(var12.nextToken());
            }

            var5 = this.rewind(var5, var7);
        }

        int var3;
        while(this.readline(var4) != null) {
            StringTokenizer var22 = new StringTokenizer(this.line, " \t\n\r\f:");
            var22.nextToken();

            while(var22.hasMoreTokens()) {
                var3 = Integer.parseInt(var22.nextToken());
                this.max_index = Math.max(this.max_index, var3);
                var22.nextToken();
                ++this.num_nonzeros;
            }
        }

        try {
            this.feature_max = new double[this.max_index + 1];
            this.feature_min = new double[this.max_index + 1];
        } catch (OutOfMemoryError var19) {
            System.err.println("can\'t allocate enough memory");
            System.exit(1);
        }

        for(var2 = 0; var2 <= this.max_index; ++var2) {
            this.feature_max[var2] = -1.7976931348623157E308D;
            this.feature_min[var2] = 1.7976931348623157E308D;
        }

        var4 = this.rewind(var4, var8);

        double var10;
        StringTokenizer var14;
        double var26;
        while(this.readline(var4) != null) {
            var9 = 1;
            var14 = new StringTokenizer(this.line, " \t\n\r\f:");
            var10 = Double.parseDouble(var14.nextToken());
            this.y_max = Math.max(this.y_max, var10);

            for(this.y_min = Math.min(this.y_min, var10); var14.hasMoreTokens(); var9 = var3 + 1) {
                var3 = Integer.parseInt(var14.nextToken());
                var26 = Double.parseDouble(var14.nextToken());

                for(var2 = var9; var2 < var3; ++var2) {
                    this.feature_max[var2] = Math.max(this.feature_max[var2], 0.0D);
                    this.feature_min[var2] = Math.min(this.feature_min[var2], 0.0D);
                }

                this.feature_max[var3] = Math.max(this.feature_max[var3], var26);
                this.feature_min[var3] = Math.min(this.feature_min[var3], var26);
            }

            for(var2 = var9; var2 <= this.max_index; ++var2) {
                this.feature_max[var2] = Math.max(this.feature_max[var2], 0.0D);
                this.feature_min[var2] = Math.min(this.feature_min[var2], 0.0D);
            }
        }

        var4 = this.rewind(var4, var8);
        if(var7 != null) {
            var5.mark(2);
            StringTokenizer var15;
            if(var5.read() == 121) {
                var5.readLine();
                var15 = new StringTokenizer(var5.readLine());
                this.y_lower = Double.parseDouble(var15.nextToken());
                this.y_upper = Double.parseDouble(var15.nextToken());
                var15 = new StringTokenizer(var5.readLine());
                this.y_min = Double.parseDouble(var15.nextToken());
                this.y_max = Double.parseDouble(var15.nextToken());
                this.y_scaling = true;
            } else {
                var5.reset();
            }

            if(var5.read() == 120) {
                var5.readLine();
                var15 = new StringTokenizer(var5.readLine());
                this.lower = Double.parseDouble(var15.nextToken());
                this.upper = Double.parseDouble(var15.nextToken());
                String var16 = null;

                while((var16 = var5.readLine()) != null) {
                    StringTokenizer var17 = new StringTokenizer(var16);
                    var9 = Integer.parseInt(var17.nextToken());
                    double var25 = Double.parseDouble(var17.nextToken());
                    double var13 = Double.parseDouble(var17.nextToken());
                    if(var9 <= this.max_index) {
                        this.feature_min[var9] = var25;
                        this.feature_max[var9] = var13;
                    }
                }
            }

            var5.close();
        }

        if(var6 != null) {
            Formatter var24 = new Formatter(new StringBuilder());
            BufferedWriter var23 = null;

            try {
                var23 = new BufferedWriter(new FileWriter(var6));
            } catch (IOException var18) {
                System.err.println("can\'t open file " + var6);
                System.exit(1);
            }

            if(this.y_scaling) {
                var24.format("y\n", new Object[0]);
                var24.format("%.16g %.16g\n", new Object[]{Double.valueOf(this.y_lower), Double.valueOf(this.y_upper)});
                var24.format("%.16g %.16g\n", new Object[]{Double.valueOf(this.y_min), Double.valueOf(this.y_max)});
            }

            var24.format("x\n", new Object[0]);
            var24.format("%.16g %.16g\n", new Object[]{Double.valueOf(this.lower), Double.valueOf(this.upper)});

            for(var2 = 1; var2 <= this.max_index; ++var2) {
                if(this.feature_min[var2] != this.feature_max[var2]) {
                    var24.format("%d %.16g %.16g\n", new Object[]{Integer.valueOf(var2), Double.valueOf(this.feature_min[var2]), Double.valueOf(this.feature_max[var2])});
                }
            }

            var23.write(var24.toString());
            var23.close();
        }

        while(this.readline(var4) != null) {
            var9 = 1;
            var14 = new StringTokenizer(this.line, " \t\n\r\f:");
            var10 = Double.parseDouble(var14.nextToken());
            this.output_target(var10);

            while(var14.hasMoreElements()) {
                var3 = Integer.parseInt(var14.nextToken());
                var26 = Double.parseDouble(var14.nextToken());

                for(var2 = var9; var2 < var3; ++var2) {
                    this.output(var2, 0.0D);
                }

                this.output(var3, var26);
                var9 = var3 + 1;
            }

            for(var2 = var9; var2 <= this.max_index; ++var2) {
                this.output(var2, 0.0D);
            }

            System.out.print("\n");
        }

        if(this.new_num_nonzeros > this.num_nonzeros) {
            System.err.print("WARNING: original #nonzeros " + this.num_nonzeros + "\n         new      #nonzeros " + this.new_num_nonzeros + "\nUse -l 0 if many original feature values are zeros\n");
        }

        var4.close();
    }

    public static void main(String[] var0) throws IOException {
        svm_scale var1 = new svm_scale();
        var1.run(var0);
    }
}
