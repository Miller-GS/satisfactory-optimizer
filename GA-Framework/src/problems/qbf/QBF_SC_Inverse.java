package problems.qbf;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.HashSet;

import solutions.Solution;

public class QBF_SC_Inverse extends QBF_Inverse {
    protected HashSet<Integer>[] sets;
    protected double coefficientsMagnitude;

	/**
	 * Constructor for the QBF_SC_Inverse class.
	 * 
	 * @param filename
	 *            Name of the file for which the objective function parameters
	 *            should be read.
	 * @throws IOException
	 *             Necessary for I/O operations.
	 */
	public QBF_SC_Inverse(String filename) throws IOException {
		super(filename);
        coefficientsMagnitude = evaluateCoefficientsMagnitude();
	}

    /**
	 * Responsible for setting the QBF function parameters by reading the
	 * necessary input from an external file. this method reads the domain's
	 * dimension, matrix {@link #A}, and sets for the set-cover.
	 * 
	 * @param filename
	 *            Name of the file containing the input for setting the black
	 *            box function.
	 * @return The dimension of the domain.
	 * @throws IOException
	 *             Necessary for I/O operations.
	 */
	@SuppressWarnings("unchecked")
    protected Integer readInput(String filename) throws IOException {

		Reader fileInst = new BufferedReader(new FileReader(filename));
		StreamTokenizer stok = new StreamTokenizer(fileInst);

        // First line has the number of variables N
		stok.nextToken();
		Integer _size = (int) stok.nval;

        // There are also N sets that will be used for the set-cover restrictions
		sets = (HashSet<Integer>[]) new HashSet[_size];
        Integer[] setSizes = new Integer[_size];

        // The next line has the sizes of each set
        for (int i = 0; i < _size; i++) {
            sets[i] = new HashSet<Integer>();
            stok.nextToken();
            setSizes[i] = (int) stok.nval;
        }

        // And the next N lines each contain the elements of the sets
        for (int i = 0; i < _size; i++) {
            for (int j = 0; j < setSizes[i]; j++) {
                stok.nextToken();
                Integer elem = (int) stok.nval - 1; // Making it 0-index so we don't have to worry about it anywhere else
                sets[i].add(elem);
            }
        }

        // N x N is also the dimension of the matrix A
		A = new Double[_size][_size];
        // The next N lines are rows of the matrix A
        // We assume a superior triangular matrix
		for (int i = 0; i < _size; i++) {
			for (int j = i; j < _size; j++) {
				stok.nextToken();
				A[i][j] = stok.nval;
				if (j>i)
					A[j][i] = 0.0;
			}
		}

		return _size;
	}

    protected Double evaluateCoefficientsMagnitude() {
        Double sum = 0.0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                sum += Math.abs(A[i][j]);
            }
        }
        return sum;
    }

    /**
     * Returns the sum of the absolute values of the coefficients in matrix A.
     * This is used to determine a penalty for solutions that do not satisfy the
     * set-cover constraints. If this value is chosen as the penalty, then any
     * solution that does not satisfy the constraints will always have a worse
     * objective value than any solution that does satisfy them.
     * 
     * @return The sum of the absolute values of the coefficients in matrix A.
     */
    public Double getCoefficientsMagnitude() {
        return coefficientsMagnitude;
    }

    public Integer countUncoveredElements() {
        HashSet<Integer> covered = new HashSet<Integer>();
        for (int i = 0; i < size; i++) {
            if (variables[i] >= 1.0) { // If the variable is selected
                covered.addAll(sets[i]);
            }
        }
        return size - covered.size();
    }

    @Override
    public boolean isFeasible(Solution<Integer> sol) {
        setVariables(sol);
        return countUncoveredElements() == 0;
    }
}
