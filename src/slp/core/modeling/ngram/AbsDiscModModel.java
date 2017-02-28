package slp.core.modeling.ngram;

import java.util.List;

import slp.core.counting.Counter;
import slp.core.util.Pair;

public class AbsDiscModModel extends NGramModel {

	public AbsDiscModModel(Counter counter) {
		super(counter);
	}
	
	@Override
	public Pair<Double, Double> modelWithConfidence(List<Integer> in) {
		int[] counts = this.counter.getShortCounts(in);
		int count = counts[0];
		int contextCount = counts[1];
		if (contextCount == 0) return Pair.of(0.0, 0.0);
		
		// Parameters for discount weight
		int n1 = this.counter.getNCount(in.size(), 1);
		int n2 = this.counter.getNCount(in.size(), 2);
		int n3 = this.counter.getNCount(in.size(), 3);
		int n4 = this.counter.getNCount(in.size(), 4);
		double Y = (double) n1 / ((double) n1 + 2*n2);
		double[] Ds = new double[] {
			Y,
			2 - 3*Y*n3/n2,
			3 - 4*Y*n4/n3
		};
		// Smooth out extreme (possibly non-finite) discount factors (in case of few observations)
		for (int i = 0; i < Ds.length; i++) {
			if (Double.isNaN(Ds[i]) || Ds[i] < 0.25*(i + 1) || Ds[i] > i + 1) Ds[i] = 0.6 * (i + 1);
		}
		int[] Ns = this.counter.getDistinctCounts(3, in.subList(0, in.size() - 1));
		
		// Probability calculation
		double discount = count > 0 ? Ds[Math.min(count, Ds.length) - 1] : 0.0;
		double MLE = Math.max(0.0, count - discount) / contextCount;
		double lambda = 1 - (Ds[0] * Ns[0] + Ds[1] * Ns[1] + Ds[2] * Ns[2]) / contextCount;
		
		// Must pre-divide MLE by lambda to match contract
		return Pair.of(MLE/lambda, lambda);
	}
}