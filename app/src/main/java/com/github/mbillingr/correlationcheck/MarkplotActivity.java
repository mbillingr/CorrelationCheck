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
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;


public class MarkplotActivity extends AppCompatActivity {

    private MarkplotView mpv = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout ll = new LinearLayout(this);
        mpv = new MarkplotView(this);
        Button button_done = new Button(this);

        button_done.setText(getResources().getString(R.string.done));

        button_done.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                done( );
            }
        });

        ll.setOrientation(LinearLayout.VERTICAL);
        ll.addView(button_done);
        ll.addView(mpv);

        setContentView(ll);
    }

    void done( ) {
        ImageProcessor.getInstance().setPerspectiveCorrection(mpv.getPerspectivePoints());
        Intent intent = new Intent(this, DataviewActivity.class);
        startActivity(intent);
    }
}


class MarkplotView extends SurfaceView implements SurfaceHolder.Callback {
    private Bitmap bitmap = null;
    private Marker[] markers;

    public MarkplotView(Context context) {
        super(context);

        getHolder().addCallback(this);
        setFocusable(true);

        markers = new Marker[4];
        markers[0] = new Marker(0, 0, 100);
        markers[1] = new Marker(0, 0, 100);
        markers[2] = new Marker(0, 0, 100);
        markers[3] = new Marker(0, 0, 100);
        markers[0].setRGB(255, 128, 128);
        markers[1].setRGB(128, 255, 128);
        markers[2].setRGB(128, 128, 255);
        markers[3].setRGB(255, 128, 255);
    }

    public List<Point> getPerspectivePoints() {

        List<Point> points = new ArrayList<>();
        for (Marker m : markers) {
            points.add(new Point(m.getY() / getHeight(), m.getX() / getWidth()));
        }
        return points;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.BLUE);

        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0, 0, null);
        }

        for (Marker marker : markers) {
            marker.draw(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getActionMasked();

        /*if (event.getAction() != MotionEvent.ACTION_MOVE) {
            Log.i("info", event.toString());
            Log.i("info", Integer.toString(action));
        }*/

        switch(action) {
            case MotionEvent.ACTION_DOWN: return onDown(event);
            case MotionEvent.ACTION_POINTER_DOWN: return onDown(event);
            case MotionEvent.ACTION_UP: return onUp(event);
            case MotionEvent.ACTION_POINTER_UP: return onUp(event);
            case MotionEvent.ACTION_MOVE: return onMove(event);
        }

        return false;
    }

    private boolean onDown(MotionEvent event) {
        boolean handeled = false;
        for (int e=0; e<event.getPointerCount(); e++) {
            for (Marker marker : markers) {
                if (marker.hasId()) {
                    continue;
                }
                float x = event.getX(e);
                float y = event.getY(e);
                if (marker.isInside(x, y)) {
                    marker.setId(event.getPointerId(e));
                    marker.set_move_origin(x, y);
                    handeled = true;
                    break;
                }
            }
        }
        return handeled;
    }

    private boolean onUp(MotionEvent event) {
        boolean handeled = false;
        for (int e=0; e<event.getPointerCount(); e++) {
            for (Marker marker : markers) {
                if (!marker.hasId()) {
                    continue;
                }
                if (marker.getId() == event.getPointerId(e)) {
                    marker.unsetId();
                    handeled = true;
                    break;
                }
            }
        }
        return handeled;
    }

    private boolean onMove(MotionEvent event) {
        boolean handeled = false;
        for (int e=0; e<event.getPointerCount(); e++) {
            for (Marker marker : markers) {
                if (!marker.hasId())
                    continue;
                if (marker.getId() == event.getPointerId(e)) {
                    marker.moveXY(event.getX(e), event.getY(e));
                    handeled = true;
                    break;
                }
            }
        }
        if (handeled)
            update();
        return handeled;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
        // TODO Auto-generated method stub
        update();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        ImageProcessor ip = ImageProcessor.getInstance();

        int w = getWidth();
        int h = getHeight();

        bitmap = ip.getRawBitmap(w, h);

        markers[0].setXY(50, 50);
        markers[1].setXY(w - 50, 50);
        markers[2].setXY(50, h - 50);
        markers[3].setXY(w - 50, h - 50);

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


class Marker {
    private float x;
    private float y;
    private float size;
    private Paint paint_transparent = new Paint();
    private Paint paint_opaque = new Paint();

    private int pointer_id;
    private boolean have_id;
    private float move_x, move_y;

    Marker(float x, float y, float size) {
        setXY(x, y);
        this.size = size;
        have_id = false;
        move_x = 0;
        move_y = 0;
    }

    float getX( ) {return x;}
    float getY( ) {return y;}

    public void setXY(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void moveXY(float x, float y) {
        this.x += x - move_x;
        this.y += y - move_y;
        set_move_origin(x, y);
    }

    public boolean isInside(float x, float y) {
        return x >= this.x - size / 2 &&
               y >= this.y - size / 2 &&
               x <= this.x + size / 2 &&
               y <= this.y + size / 2;
    }

    public void setRGB(int r, int g, int b) {
        paint_opaque.setColor(Color.rgb(r, g, b));
        paint_transparent.setColor(Color.argb(64, r, g, b));
    }

    public void draw(Canvas canvas) {
        canvas.drawRect(x - size / 2, y - size / 2, x + size / 2, y + size / 2, paint_transparent);
        canvas.drawLine(x - size * 0.55f, y, x + size * 0.55f, y, paint_opaque);
        canvas.drawLine(x, y - size * 0.55f, x, y + size * 0.55f, paint_opaque);
    }

    public boolean hasId() {return have_id;}

    public void setId(int id) {
        pointer_id = id;
        have_id = true;
    }

    public void unsetId() {have_id = false;}

    public int getId() {
        if (!have_id) {
            throw new RuntimeException("Marker does not have an ID.");
        }
        return pointer_id;
    }

    public void set_move_origin(float x, float y) {
        move_x = x;
        move_y = y;
    }
}