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
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;


public class ScatterPlotView extends SurfaceView implements SurfaceHolder.Callback {
    private Bitmap background_image = null;
    private Paint background_paint = null;
    private int background_color;
    private Paint grid_paint;
    private Paint marker_paint = null;
    private Rect drawing_rect;

    private List<Paint> marker_paints;

    private List<Point> points;
    private List<Integer> colors = null;

    public ScatterPlotView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);
        drawing_rect = new Rect();
        background_color = getResources().getColor(R.color.plot_background);

        marker_paint = new Paint();
        marker_paint.setColor(Color.rgb(0, 192, 0));

        grid_paint = new Paint();
        grid_paint.setColor(getResources().getColor(R.color.plot_grid));

        Paint p;
        marker_paints = new ArrayList<>();
        p = new Paint();
        p.setColor(getResources().getColor(R.color.plot_color1));
        marker_paints.add(p);
        p = new Paint();
        p.setColor(getResources().getColor(R.color.plot_color2));
        marker_paints.add(p);
        p = new Paint();
        p.setColor(getResources().getColor(R.color.plot_color3));
        marker_paints.add(p);

    }

    public void setData(List<Point> p) {
        points = p;
    }

    public void setColors(List<Integer> c) {
        colors = c;
    }

    public void setBackground(Bitmap bm, double alpha) {
        background_image = bm;
        background_paint = new Paint();
        background_paint.setColor(Color.rgb(255, 255, 255));
        background_paint.setAlpha((int) (255 * alpha));
    }

    public void setBackground(Bitmap bm) {
        setBackground(bm, 0.5);
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

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        canvas.drawColor(background_color);

        if (background_image != null) {
            canvas.drawBitmap(background_image, null, drawing_rect, background_paint);
        }

        int w = getWidth();
        int h = getHeight();

        for (float i=0.2f; i<1; i+=0.2) {
            canvas.drawLine(0, i*h, w, i*h, grid_paint);
            canvas.drawLine(i*w, 0, i*w, h, grid_paint);
        }

        if (colors == null) {
            for (Point p: points) {
                canvas.drawCircle((float)p.x * w, (1.0f - (float)p.y) * h, 10, marker_paints.get(0));
            }
        } else {
            Paint paint = new Paint();
            for (int i=0; i<points.size(); i++) {
                Point p = points.get(i);
                paint.setColor(colors.get(i));
                canvas.drawCircle((float)p.x * w, (1.0f - (float)p.y) * h, 10, paint);
            }
        }
    }

    private void update() {
        this.getDrawingRect(drawing_rect);
        Canvas canvas = null;
        try {
            canvas = getHolder().lockCanvas(null);
            synchronized (getHolder()) {
                draw(canvas);
            }
        }
        finally {
            if (canvas != null) {
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }
}
