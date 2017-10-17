//
//
// Class: CSE535, Fall 2017
// Assignment 1
// Group 25
//
// This is the graph view object. It is responsible for drawing the line graph
// in the main activity. The code was originally provided by the instructor
// for the assignment. It was modified by the group to fit the drawing requirements.
//
//

package com.example.hawx.a01_healthmonitor;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.util.Log;
import android.view.View;

/**
 * GraphView creates a scaled line or bar graph with x and y axis labels.
 * @author Arno den Hond
 *
 */
public class GraphView extends View {

    public static boolean BAR = false;
    public static boolean LINE = true;

    private Paint paint;
    private float[] values;
    private String[] horlabels;
    private String[] verlabels;
    private String title;
    private boolean type;

    public GraphView(Context context, float[] values, String title, String[] horlabels, String[] verlabels, boolean type) {
        super(context);
        if (values == null)
            this.values = new float[0];
        else
            this.values = values;
        if (title == null)
            this.title = "";
        else
            this.title = title;
        if (horlabels == null)
            this.horlabels = new String[0];
        else
            this.horlabels = horlabels;
        if (verlabels == null)
            this.verlabels = new String[0];
        else
            this.verlabels = verlabels;
        this.type = type;
        paint = new Paint();
    }

    public void setValues(float[] newValues)
    {
        this.values = newValues;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float border = 60;
        float horstart = border * 2;
        float height = getHeight();
        float width = getWidth() - 1;
        float max = 10;//getMax();
        float min = -10;//getMin();
        float diff = max - min;
        float graphheight = height - (2 * border);
        float graphwidth = width - (2 * border);

        // Set background draw color
        canvas.drawColor(Color.BLACK);
        paint.setTextSize(40);

        paint.setTextAlign(Align.LEFT);
        int vers = verlabels.length - 1;
        for (int i = 0; i < verlabels.length; i++) {
            paint.setColor(Color.DKGRAY);
            float y = ((graphheight / vers) * i) + border;
            canvas.drawLine(horstart, y, width, y, paint);
            paint.setColor(Color.WHITE);
            canvas.drawText(verlabels[i], 0, y, paint);
        }
        int hors = horlabels.length - 1;
        for (int i = 0; i < horlabels.length; i++) {
            paint.setColor(Color.GRAY);
            float x = ((graphwidth / hors) * i) + horstart;
            canvas.drawLine(x, height - border, x, border, paint);
            paint.setTextAlign(Align.CENTER);
            if (i==horlabels.length-1)
                paint.setTextAlign(Align.RIGHT);
            if (i==0)
                paint.setTextAlign(Align.LEFT);
            paint.setColor(Color.WHITE);
            canvas.drawText(horlabels[i], x, height - 4, paint);
        }

        paint.setTextAlign(Align.CENTER);
        canvas.drawText(title, (graphwidth / 2) + horstart, border - 4, paint);

        int stride = 3; // Data stride (number of components in the data, e.g. X, Y, Z)
        int colors[] = { Color.RED, Color.BLUE, Color.GREEN }; // Colors to use when graphing lines

        // Loop through, graphing the different lines
        for (int line_index = 0; line_index < stride; line_index++) {
            int datalength = values.length / stride;
            float colwidth = (width - (2 * border)) / 10;//datalength;
            float halfcol = colwidth / 2;
            float lasth = 0;
            paint.setColor(colors[line_index % stride]);
            paint.setStrokeWidth(8.0f);

            // Draw the line segments for this line
            for (int i = 0; i < datalength; i++) {
                float val = values[line_index + i * stride] - min;
                float rat = val / diff;
                float h = graphheight * rat;

                //Log.d("Graph", String.format("Graphing line %d, point %d, value %f", j, i, val));

                if (i > 0) { // Need two points to draw a line, start with the second point
                    canvas.drawLine(
                            ((i - 1) * colwidth) + (horstart + 1),
                            (border - lasth) + graphheight,
                            (i * colwidth) + (horstart + 1),
                            (border - h) + graphheight, paint);
                }
                lasth = h;
            }
        }
    }

    private float getMax() {
        float largest = Integer.MIN_VALUE;
        for (int i = 0; i < values.length; i++)
            if (values[i] > largest)
                largest = values[i];

        //largest = 3000;
        return largest;
    }

    private float getMin() {
        float smallest = Integer.MAX_VALUE;
        for (int i = 0; i < values.length; i++)
            if (values[i] < smallest)
                smallest = values[i];

        //smallest = 0;
        return smallest;
    }

}