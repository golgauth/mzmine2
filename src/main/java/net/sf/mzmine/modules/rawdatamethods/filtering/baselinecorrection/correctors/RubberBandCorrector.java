/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection.correctors;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection.BaselineCorrector;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.R.RSessionWrapper;
import net.sf.mzmine.util.R.RSessionWrapperException;
import net.sf.mzmine.util.R.Rcaller.RCallerResultType;

/**
 * @description Rubber Band baseline corrector. Estimates a trend based on
 *              Rubber Band algorithm (which determines a convex envelope for
 *              the spectra - underneath side). Uses "spc.rubberband" feature
 *              from "hyperSpec" R-package
 *              (http://cran.r-project.org/web/packages
 *              /hyperSpec/vignettes/baseline.pdf).
 * 
 */
public class RubberBandCorrector extends BaselineCorrector {

    @Override
    public String[] getRequiredRPackages() {
        return new String[] { /* "rJava", "Rserve", */"hyperSpec" };
    }

    @Override
    public double[] computeBaseline(final RSessionWrapper rSession,
            final RawDataFile origDataFile, double[] chromatogram,
            ParameterSet parameters) throws RSessionWrapperException {

        // Rubber Band parameters.
        double noise = parameters.getParameter(
                RubberBandCorrectorParameters.NOISE).getValue();
        boolean autoNoise = parameters.getParameter(
                RubberBandCorrectorParameters.AUTO_NOISE).getValue();
        double df = parameters.getParameter(RubberBandCorrectorParameters.DF)
                .getValue();
        boolean spline = parameters.getParameter(
                RubberBandCorrectorParameters.SPLINE).getValue();
        double bend = parameters.getParameter(
                RubberBandCorrectorParameters.BEND_FACTOR).getValue();

        final double[] baseline;

        // Set chromatogram.
        rSession.assign("chromatogram", chromatogram);
        // Transform chromatogram.
        rSession.eval("mat <- matrix(chromatogram, nrow=1)");
        rSession.eval("spc <- new (\"hyperSpec\", spc = mat, wavelength = as.numeric(seq("
                + 1 + ", " + chromatogram.length + ")))");
        // Auto noise ?
        rSession.eval("noise <- " + ((autoNoise) ? "min(mat)" : "" + noise));
        // Bend
        rSession.eval("bend <- " + bend
                + " * wl.eval(spc, function(x) x^2, normalize.wl=normalize01)");
        // Calculate baseline.
        rSession.eval("baseline <- spc.rubberband(spc + bend, noise = noise, df = "
                + df + ", spline=" + (spline ? "T" : "F") + ") - bend");
        rSession.eval("baseline <- orderwl(baseline)[[1]]");
        baseline = ((double[][]) rSession.collect("baseline", RCallerResultType.DOUBLE_MATRIX))[0];
		// Done: Refresh R code stack
		rSession.clearCode();
        
        return baseline;
    }
//    public class RubberBandCorrector extends BaselineCorrector {
//
//        @Override
//        public String[] getRequiredRPackages() {
//            return new String[] { /* "rJava", "Rserve", */"hyperSpec" };
//        }
//
//        @Override
//        public double[] computeBaseline(final RSessionWrapper rSession,
//                final RawDataFile origDataFile, double[] chromatogram,
//                ParameterSet parameters) throws RSessionWrapperException {
//
//            // Rubber Band parameters.
//            double noise = parameters.getParameter(
//                    RubberBandCorrectorParameters.NOISE).getValue();
//            boolean autoNoise = parameters.getParameter(
//                    RubberBandCorrectorParameters.AUTO_NOISE).getValue();
//            double df = parameters.getParameter(RubberBandCorrectorParameters.DF)
//                    .getValue();
//            boolean spline = parameters.getParameter(
//                    RubberBandCorrectorParameters.SPLINE).getValue();
//            double bend = parameters.getParameter(
//                    RubberBandCorrectorParameters.BEND_FACTOR).getValue();
//
//            final double[] baseline;
//
//            // Set chromatogram.
//            rSession.assign("chromatogram", chromatogram);
//            // Transform chromatogram.
////          # na.approx on the entire matrix does not work
////          na.approx(m)
////          # Error in approx(x[!na], y[!na], xout, ...) : 
////          #   need at least two non-NA values to interpolate
//    //
////          # find columns with at least two non-NA values
////          idx <- colSums(!is.na(m)) > 1
////          idx
////          # [1] FALSE FALSE  TRUE  TRUE  TRUE
//    //
////          # interpolate 'TRUE columns' only
////          m[ , idx] <- na.approx(m[ , idx])
//            // That is: make sure mat is OK with NA values in every column
//            rSession.eval("mat <- matrix(chromatogram, nrow=1)");
//            rSession.eval("idx <- colSums(!is.na(mat)) > 1");
////            rSession.eval("paste(idx, collapse=\" \")");
////            rSession.eval("stop(idx)");
//           rSession.eval("mat <- mat[ , idx]");
//            rSession.eval("spc <- new (\"hyperSpec\", spc = mat, wavelength = as.numeric(seq("
//                    + 1 + ", " + chromatogram.length + ")))");
////            rSession.eval("spc <- new (\"hyperSpec\", spc = mat, wavelength = as.numeric(seq("
////                    + 1 + ", nrow(mat))))");
//            // Auto noise ?
//            rSession.eval("noise <- " + ((autoNoise) ? "min(mat)" : "" + noise));
//            // Bend
//            rSession.eval("bend <- " + bend
//                    + " * wl.eval(spc, function(x) x^2, normalize.wl=normalize01)");
//            // Calculate baseline.
////            rSession.eval("baseline <- spc.rubberband(spc + bend, noise = noise, df = " + df + ", spline=" + (spline ? "T" : "F") + ") - bend");
//            rSession.eval("if (!is.null(mat)) { baseline <- spc.rubberband(spc + bend, noise = noise, df = " + df + ", spline=" + (spline ? "T" : "F") + ") - bend; } else { baseline <- matrix(rep(min(chromatogram), length(chromatogram)), nrow=1); }");
//            rSession.eval("baseline <- orderwl(baseline)[[1]]");
//            baseline = ((double[][]) rSession.collect("baseline"))[0];
//
//            return baseline;
//        }

    @Override
    public @Nonnull
    String getName() {
        return "RubberBand baseline corrector";
    }

    @Override
    public @Nonnull
    Class<? extends ParameterSet> getParameterSetClass() {
        return RubberBandCorrectorParameters.class;
    }

}
