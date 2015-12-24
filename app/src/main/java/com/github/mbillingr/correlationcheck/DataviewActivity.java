/*
Copyright (c) 2015-2016 Martin Billinger

This file is part of the "Correlation Check" App.

The "Correlation Check" App is free software: you can redistribute it and/or modifyit under the
terms of the GNU General Public License as published by the Free Software Foundation, either
version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.  If not,
see <http://www.gnu.org/licenses/>.
*/


package com.github.mbillingr.correlationcheck;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

public class DataviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i("info", "DataViewActivity.onCreate()");

        Button button_done = new Button(this);
        button_done.setText(getResources().getString(R.string.done));

        ScatterPlotView scatter = new ScatterPlotView(this);

        ImageProcessor imp = ImageProcessor.getInstance();

        List<Point> points = imp.extractPoints();

        Statistics.BootstrapResult bs = Statistics.bootstrap(points, 1000);

        List<Integer> colors = new ArrayList<>();
        for (double rd: bs.r_diff) {
            float[] hsv = {240.0f, 0.8f, 0.8f};

            float r = (float)Math.abs(rd);
            r = Math.min(r, 0.2f) / 0.2f;

            hsv[0] = 120.0f * (1.0f - r);  // 0=red, 120=green, 240=blue
            colors.add(Color.HSVToColor(hsv));

            Log.i("info", String.format("HSV = %f, %f, %f", hsv[0], hsv[1], hsv[2]));
        }

        scatter.setBackground(imp.getImageBitmap());
        scatter.setData(points);
        scatter.setColors(colors);

        double[] ci = Statistics.confidenceInterval(0.05, bs.r_boot);

        TextView correlation = new TextView(this);
        String r = String.format("r = %f   (CI: %f .. %f)", Statistics.computeCorrelation(points), ci[0], ci[1]);
        correlation.setText(r);
        Log.i("info", r);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.addView(button_done);
        ll.addView(correlation);
        ll.addView(scatter);

        setContentView(ll);
    }
}

class DataView extends SurfaceView implements SurfaceHolder.Callback {
    private Bitmap bitmap = null;

    public DataView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);
        Log.i("info", "DataView()");
    }

    public void setBitmap(Bitmap bm) {
        bitmap = bm;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.BLUE);

        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0, 0, null);
        }

        Paint paint = new Paint();
        paint.setColor(Color.rgb(255, 128, 64));

        canvas.drawRect(10, 10, 100, 200, paint);

        Log.i("info", "exiting DataView.onDraw()");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
        update();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        update();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
    }

    private void update() {
        Canvas canvas = null;
        try {
            canvas = getHolder().lockCanvas(null);
            synchronized (getHolder()) {
                onDraw(canvas);
            }
        }
        finally {
            if (canvas != null) {
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

}