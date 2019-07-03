package distance.elastic;

import java.util.Random;
import datasets.TSDataset;

/**
 * Some classes in this package may contain borrowed code from the timeseriesweka project (Bagnall, 2017), 
 * we might have modified (bug fixes, and improvements for efficiency) the original classes.
 * 
 */

public class WDTW {
	
	private static final double WEIGHT_MAX = 1;
	private double g; // "empirical constant that controls the curvature
			  // (slope) of the function
	private double[] weightVector; // initilised on first distance call
	
	public WDTW() {
		
	}
	
	public void setG(double g,int length){
		if(this.g!=g || this.weightVector==null){
			this.g= g;
			this.initWeights(length);
		}
		
	}	
	
//	public synchronized double distance(double[] first, double[] second, double bsf, double g) {
//		this.setG(g, first.length);
//
//		double[][] distances = MemorySpaceProvider.getInstance(first.length).getDoubleMatrix();
////		double[][] distances = MemoryManager.getInstance().getDoubleMatrix(0);
//
//		// create empty array
//		if (distances == null || distances.length < first.length || distances[0].length < second.length) {
//			distances = new double[first.length][second.length];
//		}
//
//		// first value
//		distances[0][0] = this.weightVector[0] * (first[0] - second[0]) * (first[0] - second[0]);
//
//		// top row
//		for (int i = 1; i < second.length; i++) {
//			distances[0][i] = distances[0][i - 1] + this.weightVector[i] * (first[0] - second[i]) * (first[0] - second[i]); // edited
//																	// by
//																	// Jay
//		}
//
//		// first column
//		for (int i = 1; i < first.length; i++) {
//			distances[i][0] = distances[i - 1][0] + this.weightVector[i] * (first[i] - second[0]) * (first[i] - second[0]); // edited
//																	// by
//																	// Jay
//		}
//
//		// warp rest
//		double minDistance;
//		for (int i = 1; i < first.length; i++) {
//
//			for (int j = 1; j < second.length; j++) {
//				// calculate distances
//				minDistance = DistanceTools.Min3(distances[i][j - 1], distances[i - 1][j], distances[i - 1][j - 1]);
//				distances[i][j] = minDistance + this.weightVector[Math.abs(i - j)] * (first[i] - second[j]) * (first[i] - second[j]); // edited
//																		      // by
//																		      // Jay
//
//				//
//				// if(minDistance > cutOffValue &&
//				// this.isEarlyAbandon){
//				// this.distances[i][j] = Double.MAX_VALUE;
//				// }else{
//				// this.distances[i][j] =
//				// minDistance+this.weightVector[Math.abs(i-j)]
//				// *(first[i]-second[j])*(first[i]-second[j]);
//				// //edited by Jay
//				// overflow = false;
//				// }
//			}
//
//		}
//
//		double res = distances[first.length - 1][second.length - 1];
//		MemorySpaceProvider.getInstance().returnDoubleMatrix(distances);
//		return res;
//	}
	
	//fast WDTW implemented by Geoff Webb
	public synchronized double distance(double[] first, double[] second, double bsf, double g) {
		this.setG(g, Math.max(first.length,second.length));

		double[] prevRow = new double[second.length];
		double[] curRow = new double[second.length];
		double second0 = second[0];
		double thisDiff;
		double prevVal = 0.0;
		
		// put the first row into prevRow to save swapping before moving to the second row
		
		{	double first0 = first[0];
		
			// first value
			thisDiff = first0 - second0;
			prevVal = prevRow[0] = this.weightVector[0] * thisDiff * thisDiff;
	
			// top row
			for (int j = 1; j < second.length; j++) {
				thisDiff = first0 - second[j];
				prevVal = prevRow[j] = prevVal + this.weightVector[j] * thisDiff * thisDiff;
			}
		}
		
		double minDistance;
		double firsti = first[1];
		
		// second row is a special case because path can't go through prevRow[j]
		thisDiff = firsti - second0;
		prevVal = curRow[0] = prevRow[0] + this.weightVector[1] * thisDiff * thisDiff;
		
		for (int j = 1; j < second.length; j++) {
			// calculate distances
			minDistance = Math.min(prevVal, prevRow[j - 1]);
			thisDiff = firsti - second[j];
			prevVal = curRow[j] = minDistance + this.weightVector[j-1] * thisDiff * thisDiff;
		}

		// warp rest
		for (int i = 2; i < first.length; i++) {
			// make the old current row into the current previous row and set current row to use the old prev row
			double [] tmp = curRow;
			curRow = prevRow;
			prevRow = tmp;
			firsti = first[i];
			
			thisDiff = firsti - second0;
			prevVal = curRow[0] = prevRow[0] + this.weightVector[i] * thisDiff * thisDiff;
			
			for (int j = 1; j < second.length; j++) {
				// calculate distances
				minDistance = min(prevVal, prevRow[j], prevRow[j - 1]);
				thisDiff = firsti - second[j];
				prevVal = curRow[j] = minDistance + this.weightVector[Math.abs(i - j)] * thisDiff * thisDiff;
			}

		}

		double res = prevVal;
		return res;
	}
	
	public static final double min(double A, double B, double C) {
		if (A < B) {
			if (A < C) {
				// A < B and A < C
				return A;
			} else {
				// C < A < B
				return C;
			}
		} else {
			if (B < C) {
				// B < A and B < C
				return B;
			} else {
				// C < B < A
				return C;
			}
		}
	}
	
	private void initWeights(int seriesLength) {
		this.weightVector = new double[seriesLength];
		double halfLength = (double) seriesLength / 2;

		for (int i = 0; i < seriesLength; i++) {
			weightVector[i] = WEIGHT_MAX / (1 + Math.exp(-g * (i - halfLength)));
		}
	}
	
	public double get_random_g(TSDataset d, Random r) {
		return r.nextDouble();
	}
	
}
