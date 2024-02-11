/* This file is part of the jgpml Project.
 * http://github.com/renzodenardi/jgpml
 *
 * Copyright (c) 2011 Renzo De Nardi and Hugo Gravato-Marques
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package jgpml.covariancefunctions;

import Jama.Matrix;
import org.apache.commons.math3.util.FastMath;

import java.util.Arrays;

import static jgpml.covariancefunctions.MatrixOperations.exp;


/**
 * Squared Exponential covariance function with Automatic Relevance Detemination (ARD) distance measure. The covariance
 * function is parameterized as:
 * <p>
 * k(x^p,x^q) = sf2 * exp(-(x^p - x^q)'*inv(P)*(x^p - x^q)/2)
 * <p>
 * where the P matrix is diagonal with ARD parameters ell_1^2,...,ell_D^2, where D is the dimension of the input space
 * and sf2 is the signal variance. The hyperparameters are:
 * <p>
 * [ log(ell_1) log(ell_2) . log(ell_D) log(sqrt(sf2))]
 *
 * @author josephramsey
 * @version $Id: $Id
 */
public class CovSEard implements CovarianceFunction {

    private final int D;
    private final int numParameters;
    private Matrix K;

    /**
     * Creates a new <code>CovSEard CovarianceFunction</code>
     *
     * @param inputDimension muber of dimension of the input
     */
    public CovSEard(int inputDimension) {
        this.D = inputDimension;
        this.numParameters = this.D + 1;
    }

    private static Matrix squareDist(Matrix a) {
        return CovSEard.squareDist(a, a);
    }

    private static Matrix squareDist(Matrix a, Matrix b) {
        Matrix C = new Matrix(a.getColumnDimension(), b.getColumnDimension());
        int m = a.getColumnDimension();
        int n = b.getColumnDimension();
        int d = a.getRowDimension();

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                double z = 0.0;
                for (int k = 0; k < d; k++) {
                    double t = a.get(k, i) - b.get(k, j);
                    z += t * t;
                }
                C.set(i, j, z);
            }
        }

        return C;
    }

    /**
     * Returns the number of hyperparameters of <code>CovSEard</code>
     *
     * @return number of hyperparameters
     */
    public int numParameters() {
        return this.numParameters;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Compute covariance matrix of a dataset X
     */
    public Matrix compute(Matrix loghyper, Matrix X) {

        if (X.getColumnDimension() != this.D)
            throw new IllegalArgumentException("The number of dimensions specified on the covariance function " + this.D + " must agree with the size of the input vector" + X.getColumnDimension());
        if (loghyper.getColumnDimension() != 1 || loghyper.getRowDimension() != this.numParameters)
            throw new IllegalArgumentException("Wrong number of hyperparameters, " + loghyper.getRowDimension() + " instead of " + this.numParameters);

        Matrix ell = exp(loghyper.getMatrix(0, this.D - 1, 0, 0));                         // characteristic length scales
        double sf2 = FastMath.exp(2 * loghyper.get(this.D, 0));                              // signal variance

        Matrix diag = new Matrix(this.D, this.D);
        for (int i = 0; i < this.D; i++)
            diag.set(i, i, 1 / ell.get(i, 0));

        this.K = exp(CovSEard.squareDist(diag.times(X.transpose())).times(-0.5)).times(sf2);   // SE covariance

        return this.K;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Compute compute test set covariances
     */
    public Matrix[] compute(Matrix loghyper, Matrix X, Matrix Xstar) {

        if (X.getColumnDimension() != this.D)
            throw new IllegalArgumentException("The number of dimensions specified on the covariance function " + this.D + " must agree with the size of the input vector" + X.getColumnDimension());
        if (loghyper.getColumnDimension() != 1 || loghyper.getRowDimension() != this.numParameters)
            throw new IllegalArgumentException("Wrong number of hyperparameters, " + loghyper.getRowDimension() + " instead of " + this.numParameters);

        Matrix ell = exp(loghyper.getMatrix(0, this.D - 1, 0, 0));                         // characteristic length scales
        double sf2 = FastMath.exp(2 * loghyper.get(this.D, 0));                              // signal variance

        double[] a = new double[Xstar.getRowDimension()];
        Arrays.fill(a, sf2);
        Matrix A = new Matrix(a, Xstar.getRowDimension());

        Matrix diag = new Matrix(this.D, this.D);
        for (int i = 0; i < this.D; i++)
            diag.set(i, i, 1 / ell.get(i, 0));

        Matrix B = exp(CovSEard.squareDist(diag.times(X.transpose()), diag.times(Xstar.transpose())).times(-0.5)).times(sf2);

        return new Matrix[]{A, B};
    }

    /**
     * {@inheritDoc}
     * <p>
     * Coompute the derivatives of this <code>CovarianceFunction</code> with respect to the hyperparameter with index
     * <code>idx</code>
     */
    public Matrix computeDerivatives(Matrix loghyper, Matrix X, int index) {

        if (X.getColumnDimension() != this.D)
            throw new IllegalArgumentException("The number of dimensions specified on the covariance function " + this.D + " must agree with the size of the input vector" + X.getColumnDimension());
        if (loghyper.getColumnDimension() != 1 || loghyper.getRowDimension() != this.numParameters)
            throw new IllegalArgumentException("Wrong number of hyperparameters, " + loghyper.getRowDimension() + " instead of " + this.numParameters);
        if (index > numParameters() - 1)
            throw new IllegalArgumentException("Wrong hyperparameters index " + index + " it should be smaller or equal to " + (numParameters() - 1));

        Matrix A = null;

        Matrix ell = exp(loghyper.getMatrix(0, this.D - 1, 0, 0));                         // characteristic length scales
        double sf2 = FastMath.exp(2 * loghyper.get(this.D, 0));                              // signal variance
        // noise variance

        if (this.K.getRowDimension() != X.getRowDimension() || this.K.getColumnDimension() != X.getRowDimension()) {
            Matrix diag = new Matrix(this.D, this.D);
            for (int i = 0; i < this.D; i++)
                diag.set(i, i, 1 / ell.get(i, 0));

            this.K = exp(CovSEard.squareDist(diag.times(X.transpose())).times(-0.5)).times(sf2);   // SE covariance
        }

        if (index < this.D) {   //length scale parameters
            Matrix col = CovSEard.squareDist(X.getMatrix(0, X.getRowDimension() - 1, index, index).transpose().times(1 / ell.get(index, 0)));

            A = this.K.arrayTimes(col);
        } else {    // magnitude parameter
            A = this.K.times(2);
            this.K = null;
        }

        return A;
    }

}
