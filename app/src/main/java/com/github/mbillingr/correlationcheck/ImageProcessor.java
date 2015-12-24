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


import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ImageProcessor {
    int work_width = 512;
    int work_height = 512;

    int raw_width, raw_height;

    private int debugcounter = 0;
    private boolean enable_debug = true;

    private static ImageProcessor mInstance = null;

    private File mImageFile = null;

    private Mat perspective_transform = null;
    private Mat working_image = null;

    File storageDir;

    public static ImageProcessor getInstance() {
        if (mInstance == null) {
            mInstance = new ImageProcessor();
        }
        return mInstance;
    }

    private ImageProcessor() {
        OpenCVLoader.initDebug();

        storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        mImageFile = new File(storageDir, "scatter.jpg");
    }

    public File getmImageFile( ) {
        return mImageFile;
    }

    Bitmap matToBitmap(Mat input) {
        if (input == null) {
            return Bitmap.createBitmap(0, 0, Bitmap.Config.ARGB_8888);
        }
        Mat tmp = new Mat();
        if (input.channels() == 1) {
            Imgproc.cvtColor(input, tmp, Imgproc.COLOR_GRAY2RGB);
        } else {
            Imgproc.cvtColor(input, tmp, Imgproc.COLOR_BGR2RGB);
        }
        Core.transpose(tmp, tmp);
        Core.flip(tmp, tmp, 1);

        Bitmap bm = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(tmp, bm);
        return bm;
    }

    public Bitmap getRawBitmap(int w, int h) {
        Log.i("info", "reading " + mImageFile.getAbsolutePath());
        Mat image = Imgcodecs.imread(mImageFile.getAbsolutePath());

        raw_width = image.width();
        raw_height = image.height();

        Imgproc.resize(image, image, new Size(h, w));
        return matToBitmap(image);
    }

    public Bitmap getImageBitmap( ) {
        return matToBitmap(working_image);
    }

    public Bitmap getImageBitmap(int w, int h) {
        Mat tmp = new Mat();
        Imgproc.resize(working_image, tmp, new Size(h, w));
        return matToBitmap(tmp);
    }

    public List<Point> extractPoints() {
        Mat gray = new Mat();//work_width, work_height, CvType.CV_8UC1);
        Mat binary = new Mat();

        Mat kernel = Mat.ones(3, 3, CvType.CV_8UC1);

        debugreset();

        Mat image = load_transformed();
        working_image = image.clone();
        debugsave(image, "source");

        Imgproc.cvtColor(image, gray, Imgproc.COLOR_RGB2GRAY);
        debugsave(gray, "grayscale");

        Imgproc.GaussianBlur(gray, gray, new Size(15, 15), 0);
        debugsave(gray, "blurred");

        //Imgproc.equalizeHist(gray, gray);
        //debugsave(gray, "equalized");

        Imgproc.adaptiveThreshold(gray, binary, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV, 129, 5);
        //Imgproc.threshold(gray, binary, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
        //Imgproc.threshold(gray, binary, 128, 255, Imgproc.THRESH_BINARY_INV);
        debugsave(binary, "binary");

        Imgproc.morphologyEx(binary, binary, Imgproc.MORPH_CLOSE, kernel);
        debugsave(binary, "closed");

        Imgproc.morphologyEx(binary, binary, Imgproc.MORPH_OPEN, kernel);
        debugsave(binary, "opened");

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_TREE,
                Imgproc.CHAIN_APPROX_SIMPLE); // is binary is now changed
        Imgproc.drawContours(image, contours, -1, new Scalar(0, 0, 255), 3);
        debugsave(image, "contours");

        List<PointAndArea> points = new ArrayList<>();

        for (MatOfPoint cnt: contours) {
            MatOfPoint2f c2f = new MatOfPoint2f();
            c2f.fromArray(cnt.toArray());
            RotatedRect rr = Imgproc.minAreaRect(c2f);

            double area = Imgproc.contourArea(cnt);

            if (rr.size.width / rr.size.height < 3 &&
                    rr.size.height / rr.size.width < 3 &&
                    rr.size.width < 64 && rr.size.height < 64 &&
                    area > 9 &&
                    area < 10000) {
                points.add(new PointAndArea((int) area, rr.center));
            }
        }

        List<Point> final_points = new ArrayList<>();

        Collections.sort(points);
        Collections.reverse(points);
        int prev = -1;
        for (PointAndArea p: points) {
            Log.i("area", Integer.toString(p.area));
            if (prev == -1 || p.area >= prev / 2) {
                prev = p.area;
                Imgproc.circle(image, p.point, 10, new Scalar(0, 255, 0), 5);
                final_points.add(new Point(1 - p.point.y / work_height, 1 - p.point.x / work_width));
            }
        }
        debugsave(image, "circles");

        return final_points;
    }

    private void debugreset() {
        debugcounter = 0;
    }

    private void debugsave(Mat image, String name) {
        if (!enable_debug)
            return;
        debugcounter++;
        File file = new File(storageDir, String.format("scatter_%02d_%s.jpg", debugcounter, name));
        Imgcodecs.imwrite(file.getAbsolutePath(), image);
    }

    private Mat load_transformed( ) {
        Mat image = Imgcodecs.imread(mImageFile.getAbsolutePath());
        Imgproc.warpPerspective(image, image, perspective_transform,
                new Size(work_width, work_height), Imgproc.INTER_CUBIC);
        return image;
    }

    void setPerspectiveCorrection(float ax, float ay, float bx, float by,
                                  float cx, float cy, float dx, float dy) {

        List<Point> pts_in = new ArrayList<>();
        pts_in.add(new Point(ax, ay));
        pts_in.add(new Point(bx, by));
        pts_in.add(new Point(cx, cy));
        pts_in.add(new Point(dx, dy));

        setPerspectiveCorrection(pts_in);
    }

    void setPerspectiveCorrection(List<Point> refpoints) {
        List<Point> pts_in = new ArrayList<>();
        for (Point p : refpoints) {
            pts_in.add(new Point(p.x * raw_width, (1-p.y) * raw_height));
        }
        Mat mat_src = Converters.vector_Point2f_to_Mat(pts_in);

        List<Point> pts_out = new ArrayList<>();
        pts_out.add(new Point(0, work_height));
        pts_out.add(new Point(0, 0));
        pts_out.add(new Point(work_width, work_height));
        pts_out.add(new Point(work_width, 0));
        Mat mat_dst = Converters.vector_Point2f_to_Mat(pts_out);

        perspective_transform = Imgproc.getPerspectiveTransform(mat_src, mat_dst);
    }
}

class PointAndArea implements Comparable<PointAndArea> {

    public int area;
    public Point point;

    PointAndArea(int area, Point point) {
        this.area = area;
        this.point = point;
    }

    @Override
    public int compareTo(PointAndArea other) {
        return this.area - other.area;
    }
}