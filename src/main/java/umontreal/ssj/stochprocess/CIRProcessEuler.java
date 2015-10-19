/*
 * Class:        CIRProcessEuler
 * Description:  
 * Environment:  Java
 * Software:     SSJ 
 * Copyright (C) 2001  Pierre L'Ecuyer and Universite de Montreal
 * Organization: DIRO, Universite de Montreal
 * @author       
 * @since

 * SSJ is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation, either version 3 of the License, or
 * any later version.

 * SSJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * A copy of the GNU General Public License is available at
   <a href="http://www.gnu.org/licenses">GPL licence site</a>.
 */
package umontreal.ssj.stochprocess;
import umontreal.ssj.rng.*;
import umontreal.ssj.probdist.*;
import umontreal.ssj.randvar.*;

/**
 * This class represents a *CIR* process as in  @ref CIRProcess, but the
 * process is generated using the simple Euler scheme
 * @anchor REF_stochprocess_CIRProcessEuler_eq_cir_seq_euler
 * @f[
 *   X(t_j) - X(t_{j-1}) = \alpha(b - X(t_{j-1}))(t_j - t_{j-1}) + \sigma\sqrt{(t_j - t_{j-1})X(t_{j-1})}  Z_j \tag{cir-seq-euler}
 * @f]
 * where @f$Z_j \sim N(0,1)@f$. This is a good approximation only for small
 * time intervals @f$t_j - t_{j-1}@f$.
 *
 * <div class="SSJ-bigskip"></div><div class="SSJ-bigskip"></div>
 */
public class CIRProcessEuler extends StochasticProcess {
    protected NormalGen    gen;
    protected double       alpha,
                           beta,
                           sigma;
    // Precomputed values
    protected double[]     alphadt,
                           sigmasqrdt;

   /**
    * Constructs a new `CIRProcessEuler` with parameters @f$\alpha=@f$
    * `alpha`, @f$b@f$, @f$\sigma=@f$ `sigma` and initial value @f$X(t_0)
    * =@f$ `x0`. The normal variates @f$Z_j@f$ will be generated by
    * inversion using the stream `stream`.
    */
   public CIRProcessEuler (double x0, double alpha, double b, double sigma,
                           RandomStream stream) {
      this (x0, alpha, b, sigma, new NormalGen (stream, new NormalDist()));
   }

   /**
    * The normal variate generator `gen` is specified directly instead of
    * specifying the stream. `gen` can use another method than inversion.
    */
   public CIRProcessEuler (double x0, double alpha, double b, double sigma,
                           NormalGen gen) {
      this.alpha = alpha;
      this.beta  = b;
      this.sigma = sigma;
      this.x0    = x0;
      this.gen   = gen;
   }


   public double nextObservation() {
      double xOld = path[observationIndex];
      double x;
      x = xOld + (beta - xOld) * alphadt[observationIndex]
           + sigmasqrdt[observationIndex] * Math.sqrt(xOld) * gen.nextDouble();
      observationIndex++;
      if (x >= 0.0)
         path[observationIndex] = x;
      else
         path[observationIndex] = 0.;
      return x;
   }

/**
 * Generates and returns the next observation at time @f$t_{j+1} =@f$
 * `nextTime`, using the previous observation time @f$t_j@f$ defined earlier
 * (either by this method or by <tt>setObservationTimes</tt>), as well as the
 * value of the previous observation @f$X(t_j)@f$. *Warning*: This method
 * will reset the observations time @f$t_{j+1}@f$ for this process to
 * `nextTime`. The user must make sure that the @f$t_{j+1}@f$ supplied is
 * @f$\geq t_j@f$.
 */
public double nextObservation (double nextTime) {
      double previousTime = t[observationIndex];
      double xOld = path[observationIndex];
      observationIndex++;
      t[observationIndex] = nextTime;
      double dt = nextTime - previousTime;
      double x = xOld + alpha * (beta - xOld) * dt
           + sigma * Math.sqrt (dt*xOld) * gen.nextDouble();
      if (x >= 0.0)
         path[observationIndex] = x;
      else
         path[observationIndex] = 0.;
      return x;
   }

   /**
    * Generates an observation of the process in `dt` time units, assuming
    * that the process has value @f$x@f$ at the current time. Uses the
    * process parameters specified in the constructor. Note that this
    * method does not affect the sample path of the process stored
    * internally (if any).
    */
   public double nextObservation (double x, double dt) {
      x = x + alpha * (beta - x) * dt +
          sigma * Math.sqrt (dt*x) * gen.nextDouble();
      if (x >= 0.0)
         return x;
      return 0.0;
   }
public double[] generatePath() {
      double x;
      double xOld = x0;
      for (int j = 0; j < d; j++) {
          x = xOld + (beta - xOld) * alphadt[j]
              + sigmasqrdt[j] * Math.sqrt(xOld) * gen.nextDouble();
          if (x < 0.0)
              x = 0.0;
          path[j + 1] = x;
          xOld = x;
      }
      observationIndex = d;
      return path;
   }

/**
 * Generates a sample path of the process at all observation times, which are
 * provided in array `t`. Note that `t[0]` should be the observation time of
 * `x0`, the initial value of the process, and `t[]` should have at least
 * @f$d+1@f$ elements (see the `setObservationTimes` method).
 */
public double[] generatePath (RandomStream stream) {
        gen.setStream (stream);
        return generatePath();
   }

   /**
    * Resets the parameters @f$X(t_0) =@f$ `x0`, @f$\alpha=@f$ `alpha`,
    * @f$b =@f$ `b` and @f$\sigma=@f$ `sigma` of the process. *Warning*:
    * This method will recompute some quantities stored internally, which
    * may be slow if called too frequently.
    */
   public void setParams (double x0, double alpha, double b, double sigma) {
      this.alpha = alpha;
      this.beta  = b;
      this.sigma = sigma;
      this.x0    = x0;
      if (observationTimesSet) init(); // Otherwise not needed.
   }

   /**
    * Resets the random stream of the normal generator to `stream`.
    */
   public void setStream (RandomStream stream) {
      gen.setStream (stream);
   }

   /**
    * Returns the random stream of the normal generator.
    */
   public RandomStream getStream() {
      return gen.getStream ();
   }

   /**
    * Returns the value of @f$\alpha@f$.
    */
   public double getAlpha() { return alpha; }

   /**
    * Returns the value of @f$b@f$.
    */
   public double getB() { return beta; }

   /**
    * Returns the value of @f$\sigma@f$.
    */
   public double getSigma() { return sigma; }

   /**
    * Returns the normal random variate generator used. The `RandomStream`
    * used for that generator can be changed via
    * `getGen().setStream(stream)`, for example.
    */
   public NormalGen getGen() { return gen; }


    // This is called by setObservationTimes to precompute constants
    // in order to speed up the path generation.
    protected void init() {
       super.init();
       alphadt = new double[d];
       sigmasqrdt = new double[d];
       double dt;
       for (int j = 0; j < d; j++) {
           dt = t[j+1] - t[j];
           alphadt[j]      = alpha * (dt);
           sigmasqrdt[j]   = sigma * Math.sqrt (dt);
       }
    }

}