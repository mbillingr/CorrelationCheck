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


import android.util.Log;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.lang.Math;
import java.util.Random;


public class Statistics {

    static double[] confidenceInterval(double alpha, double[] bootstat) {
        Arrays.sort(bootstat);
        int k = (int)Math.floor(alpha * bootstat.length);

        double[] result = new double[2];
        result[0] = bootstat[k];
        result[1] = bootstat[bootstat.length - k - 1];
        return result;
    }

    static class BootstrapResult {
        public double[] r_boot;
        public double[] r_diff;
    }

    static BootstrapResult bootstrap(List<Point> points, int n_reps) {
        return bootstrap(points, n_reps, points.size());
    }

    static BootstrapResult bootstrap(List<Point> points, int n_reps, int n_boot) {
        Random rnd = new Random();

        int n = points.size();

        double[] r_included = new double[n];
        double[] r_excluded = new double[n];

        int[] n_included = new int[n];
        int[] n_excluded = new int[n];

        for (int i=0; i<n; i++) {
            r_included[i] = 0;
            r_excluded[i] = 0;
            n_included[i] = 0;
            n_excluded[i] = 0;
        }

        double[] r_boot = new double[n_reps];
        for (int rep=0; rep<n_reps; rep++) {
            List<Point> resample = new ArrayList<>();
            List<Integer> indices = new ArrayList<>();
            for (int i=0; i<n_boot; i++) {
                int k = rnd.nextInt(n);
                resample.add(points.get(k));
                indices.add(k);
            }

            double r = computeCorrelation(resample);
            r_boot[rep] = r;

            for (int i=0; i<n; i++) {
                if (indices.contains(i)) {
                    r_included[i] += r;
                    n_included[i] += 1;
                } else {
                    r_excluded[i] += r;
                    n_excluded[i] += 1;
                }
            }
        }

        double[] r_diff = new double[n];
        for (int i=0; i<n; i++) {
            Log.i("info", String.format("%f / %d - %f / %d", r_included[i], n_included[i], r_included[i], n_excluded[i]));
            r_diff[i] = r_included[i] / n_included[i] - r_excluded[i] / n_excluded[i];
        }

        BootstrapResult result = new BootstrapResult();
        result.r_boot = r_boot;
        result.r_diff = r_diff;
        return result;
    }

    static double computeCorrelation(List<Point> points) {
        double n = points.size();
        Point sums = sum(points);
        double sumxy = prodsum(points);
        Point sqs = squaresum(points);

        double stdx = Math.sqrt(n * sqs.x - sums.x * sums.x);
        double stdy = Math.sqrt(n * sqs.y - sums.y * sums.y);
        double cov = n * sumxy - sums.x * sums.y;
        return cov / (stdx * stdy);
    }

    static Point sum(List<Point> points) {
        Point sum = new Point(0, 0);
        for (Point p: points) {
            sum.x += p.x;
            sum.y += p.y;
        }
        return sum;
    }

    static Point squaresum(List<Point> points) {
        Point sum = new Point(0, 0);
        for (Point p: points) {
            sum.x += p.x * p.x;
            sum.y += p.y * p.y;
        }
        return sum;
    }

    static double prodsum(List<Point> points) {
        double sum = 0;
        for (Point p: points) {
            sum += p.x * p.y;
        }
        return sum;
    }
}
