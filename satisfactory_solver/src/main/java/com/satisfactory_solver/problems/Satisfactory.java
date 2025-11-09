package com.satisfactory_solver.problems;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.satisfactory_solver.decoder.DecodedSolution;
import com.satisfactory_solver.decoder.Decoder;
import com.satisfactory_solver.decoder.Solution;
import com.satisfactory_solver.instance.Instance;
import com.satisfactory_solver.instance.InstanceJsonReader;

/**
 * A quadractic binary function (QBF) is a function that can be expressed as the
 * sum of quadractic terms: f(x) = \sum{i,j}{a_{ij}*x_i*x_j}. In matricial form
 * a QBF can be expressed as f(x) = x'.A.x 
 * The problem of minimizing a QBF is NP-hard [1], even when no constraints
 * are considered.
 * 
 * [1] Kochenberger, et al. The unconstrained binary quadratic programming
 * problem: a survey. J Comb Optim (2014) 28:58–81. DOI
 * 10.1007/s10878-014-9734-0.
 * 
 * @author ccavellucci, fusberti
 *
 */
public class Satisfactory implements Evaluator<Integer> {

	/**
	 * Dimension of the domain.
	 */
	public final Integer size;

	/**
	 * The array of numbers representing the domain.
	 */
	public final List<Double> variables;

	protected Instance instance;
    protected Decoder decoder;

	/**
	 * The constructor for QuadracticBinaryFunction class. The filename of the
	 * input for setting matrix of coefficients A of the QBF. The dimension of
	 * the array of variables x is returned from the {@link #readInput} method.
	 * 
	 * @param filename
	 *            Name of the file containing the input for setting the QBF.
	 * @throws IOException
	 *             Necessary for I/O operations.
	 */
	public Satisfactory(String filename) throws IOException {
		size = readInput(filename);
		variables = allocateVariables();
	}

    public Decoder getDecoder() {
        return this.decoder;
    }

	/**
	 * Evaluates the value of a solution by transforming it into a vector. This
	 * is required to perform the matrix multiplication which defines a QBF.
	 * 
	 * @param sol
	 *            the solution which will be evaluated.
	 */
	public void setVariables(Solution<Integer> sol) {

		resetVariables();
		if (!sol.isEmpty()) {
			for (Integer elem : sol) {
				variables.set(elem, 1.0);
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see problems.Evaluator#getDomainSize()
	 */
	@Override
	public Integer getDomainSize() {
		return size;
	}

	/**
	 * {@inheritDoc} In the case of a QBF, the evaluation correspond to
	 * computing a matrix multiplication x'.A.x. A better way to evaluate this
	 * function when at most two variables are modified is given by methods
	 * {@link #evaluateInsertionQBF(int)}, {@link #evaluateRemovalQBF(int)} and
	 * {@link #evaluateExchangeQBF(int,int)}.
	 * 
	 * @return The evaluation of the QBF.
	 */
	@Override
	public Double evaluate(Solution<Integer> sol) {

		setVariables(sol);
        DecodedSolution decoded = decoder.decode(variables);
		return sol.cost = Double.valueOf(decoded.getNumberOfUsedMachines());
	}

    public DecodedSolution decode(Solution<Integer> sol) {
        setVariables(sol);
        DecodedSolution decoded = decoder.decode(variables);
        return decoded;
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see problems.Evaluator#evaluateInsertionCost(java.lang.Object,
	 * solutions.Solution)
	 */
	@Override
	public Double evaluateInsertionCost(Integer elem, Solution<Integer> sol) {
        setVariables(sol);
        variables.set(elem, 1.0);

		DecodedSolution decoded = decoder.decode(variables);
		return sol.cost = Double.valueOf(decoded.getNumberOfUsedMachines());
	}

	@Override
	public Double evaluateRemovalCost(Integer elem, Solution<Integer> sol) {

		setVariables(sol);
        variables.set(elem, 0.0);   
        DecodedSolution decoded = decoder.decode(variables);
        return sol.cost = Double.valueOf(decoded.getNumberOfUsedMachines());
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see problems.Evaluator#evaluateExchangeCost(java.lang.Object,
	 * java.lang.Object, solutions.Solution)
	 */
	@Override
	public Double evaluateExchangeCost(Integer elemIn, Integer elemOut, Solution<Integer> sol) {

		setVariables(sol);
        variables.set(elemIn, 1.0);
        variables.set(elemOut, 0.0);    
        DecodedSolution decoded = decoder.decode(variables);
        return sol.cost = Double.valueOf(decoded.getNumberOfUsedMachines());
	}

	/**
	 * Responsible for setting the QBF function parameters by reading the
	 * necessary input from an external file. this method reads the domain's
	 * dimension and matrix {@link #A}.
	 * 
	 * @param filename
	 *            Name of the file containing the input for setting the black
	 *            box function.
	 * @return The dimension of the domain.
	 * @throws IOException
	 *             Necessary for I/O operations.
	 */
	protected Integer readInput(String filename) throws IOException {
        InstanceJsonReader reader = new InstanceJsonReader();
        try {
            String jsonContent = Files.readString(Paths.get(filename), StandardCharsets.UTF_8);
            this.instance = reader.readInstanceFromJson(jsonContent);
            this.decoder = new Decoder(this.instance);
            return decoder.getChromosomeLength();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON file: " + filename, e);
        }
	}

	/**
	 * Reserving the required memory for storing the values of the domain
	 * variables.
	 * 
	 * @return a pointer to the array of domain variables.
	 */
	protected List<Double> allocateVariables() {
		List<Double> _variables = new ArrayList<>(Collections.nCopies(size, 0.0));
		return _variables;
	}

	/**
	 * Reset the domain variables to their default values.
	 */
	public void resetVariables() {
		Collections.fill(variables, 0.0);
	}


    @Override
    public boolean isFeasible(Solution<Integer> sol) {
        setVariables(sol);
        DecodedSolution decoded = decoder.decode(variables);
        return decoded.getUnsatisfiedDemandSum() == 0.0; 
    }
}
