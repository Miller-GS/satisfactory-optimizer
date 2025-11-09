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
 * The goal of this class is to represent a possible configuration of a factory
 * in the Satisfactory game. The ultimate goal is to minimize the number of machines.
 */
public class Satisfactory implements Evaluator<Double> {

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
	 * The constructor for Satisfactory class. The filename of the
	 * input for setting the recipes, available input and desired output. The dimension of
	 * the array of variables x is returned from the {@link #readInput} method.
	 * 
	 * @param filename
	 *            Name of the file containing the input for setting the Satisfactory problem.
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
	 * Evaluates the value of a solution by transforming it into a vector.
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
	public Double evaluate(Solution<Double> sol) {

        DecodedSolution decoded = decoder.decode(sol);
		return sol.cost = Double.valueOf(decoded.getNumberOfUsedMachines());
	}

    public DecodedSolution decode(Solution<Double> sol) {
        DecodedSolution decoded = decoder.decode(sol);
        return decoded;
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
    public boolean isFeasible(Solution<Double> sol) {
        DecodedSolution decoded = decoder.decode(sol);
        return decoded.getUnsatisfiedDemandSum() == 0.0;
    }
}
