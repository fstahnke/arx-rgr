/*
 * Benchmark of risk-based anonymization in ARX 3.0.0 Copyright 2015 - Fabian
 * Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.deidentifier.arx.benchmark;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkAlgorithm;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkPrivacyModel;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkUtilityMeasure;
import org.deidentifier.arx.clustering.TassaAlgorithm;
import org.deidentifier.arx.exceptions.RollbackRequiredException;
import org.deidentifier.arx.recursive.BenchmarkAlgorithmRGR;
import org.deidentifier.arx.utility.AggregateFunction;
import org.deidentifier.arx.utility.DataConverter;
import org.deidentifier.arx.utility.UtilityMeasureDiscernibility;
import org.deidentifier.arx.utility.UtilityMeasureLoss;

import de.linearbits.subframe.Benchmark;
import de.linearbits.subframe.analyzer.ValueBuffer;

/**
 * BenchmarkExperiment analysing utility and runtime (with multiple runs).
 * 
 * @author Fabian Stahnke
 */
public class BenchmarkExperimentUtilityAndRuntime {

    /** The benchmark instance */
    private final Benchmark BENCHMARK              = new Benchmark(new String[] {
            "Dataset",
            "UtilityMeasure",
            "PrivacyModel",
            "Algorithm",
            "SuppressionLimit",
            "gsFactor",
            "gsFactorStepSize",
            "K",
            "Records",
            "QIs"                                 });
    
    
    /** UTILITY */
    private final int       UTILITY                = BENCHMARK.addMeasure("Utility");
    /** RUNTIME */
    private final int       RUNTIME                = BENCHMARK.addMeasure("Runtime");
    /** NUMBER OF SUPPRESSED TUPLES */
    private final int       SUPPRESSED             = BENCHMARK.addMeasure("Suppressed");
    /** RATIO OF SUPPRESSED TUPLES */
    private final int       SUPPRESSED_RATIO       = BENCHMARK.addMeasure("SuppressedRatio");
    /** GENERALIZATION VARIANCE */
    private final int       VARIANCE               = BENCHMARK.addMeasure("Variance");
    /** GENERALIZATION VARIANCE WITHOUT SUPPRESSED TUPLES */
    private final int       VARIANCE_NOTSUPPRESSED = BENCHMARK.addMeasure("VarianceWithoutSuppressed");
    /** Number of runs for each benchmark setting */
    private int             numberOfRuns;
    /** Number of warmup runs */
    private int             numberOfWarmups;
    /** The setup of this experiment */
    private BenchmarkSetup setup;
    /** The metadata of this experiment */
    private BenchmarkMetadataUtility metadata;
    
    
    public BenchmarkExperimentUtilityAndRuntime(String benchmarkConfig) throws IOException {
        // Init
        BENCHMARK.addAnalyzer(UTILITY, new ValueBuffer());
        BENCHMARK.addAnalyzer(RUNTIME, new ValueBuffer());
        BENCHMARK.addAnalyzer(SUPPRESSED, new ValueBuffer());
        BENCHMARK.addAnalyzer(SUPPRESSED_RATIO, new ValueBuffer());
        BENCHMARK.addAnalyzer(VARIANCE, new ValueBuffer());
        BENCHMARK.addAnalyzer(VARIANCE_NOTSUPPRESSED, new ValueBuffer());
        
        setup = new BenchmarkSetup(benchmarkConfig);
        metadata = new BenchmarkMetadataUtility(setup);
        
    }

    /**
     * Main entry point
     * 
     * @param args
     * @throws IOException
     * @throws RollbackRequiredException
     */
    public void execute() throws IOException, RollbackRequiredException {


        File resultFile = new File(setup.getOutputFile());
        resultFile.getParentFile().mkdirs();
        numberOfRuns = setup.getNumberOfRuns();
        numberOfWarmups = setup.getNumberOfWarmups();

        // Repeat for each data set
        for (BenchmarkPrivacyModel model : setup.getPrivacyModels()) {
            for (BenchmarkUtilityMeasure measure : setup.getUtilityMeasures()) {
                for (BenchmarkAlgorithm algorithm : setup.getAlgorithms()) {
                    for (BenchmarkDataset dataset : setup.getDatasets()) {
                        for (double suppressionLimit : setup.getSuppressionLimits()) {
                            for (double gsStepSize : setup.getGsStepSizes()) {
                                for (double gsFactor : setup.getGsFactors()) {

                                    // Tassa doesn't support suppression limits
                                    if (algorithm == BenchmarkAlgorithm.TASSA) {
                                        suppressionLimit = 0.0;
                                    }

                                    System.out.println("Performing run: " + dataset.name() + " / " +
                                                       measure + " / " + model + " / " + algorithm +
                                                       " / suppLimit: " + suppressionLimit +
                                                       " / gsFactor: " + gsFactor +
                                                       " / gsStepSize: " + gsStepSize + " / QIs: " +
                                                       dataset.getNumQIs() + " / Records: " +
                                                       dataset.getNumRecords());

                                    performExperiment(metadata,
                                                      dataset,
                                                      measure,
                                                      model,
                                                      algorithm,
                                                      suppressionLimit,
                                                      gsFactor,
                                                      gsStepSize);
                                    // Write after each experiment
                                    BENCHMARK.getResults().write(resultFile);
                                    // Break gsFactor loop for Tassa
                                    if (algorithm == BenchmarkAlgorithm.TASSA) {
                                        break;
                                    }
                                }
                                // Break gsStepSize loop for Tassa
                                if (algorithm == BenchmarkAlgorithm.TASSA) {
                                    break;
                                }
                            }
                            // Break suppression limit loop for Tassa
                            if (algorithm == BenchmarkAlgorithm.TASSA) {
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Perform experiments
     * 
     * @param metadata
     * @param dataset
     * @param measure
     * @param model
     * @param algorithm
     * @param suppressionLimit
     * @param gsFactor
     * @throws IOException
     * @throws RollbackRequiredException
     */
    private void performExperiment(final BenchmarkMetadataUtility metadata,
                                          final BenchmarkDataset dataset,
                                          final BenchmarkUtilityMeasure measure,
                                          final BenchmarkPrivacyModel model,
                                          final BenchmarkAlgorithm algorithm,
                                          final double suppressionLimit,
                                          final double gsFactor,
                                          final double gsStepSize) throws IOException,
                                                                  RollbackRequiredException {

        Data data = BenchmarkSetup.getData(dataset, model);
        ARXConfiguration config = BenchmarkSetup.getConfiguration(dataset,
                                                                  measure,
                                                                  model,
                                                                  suppressionLimit,
                                                                  gsFactor);

        final Map<String, String[][]> hierarchies = new DataConverter().toMap(data.getDefinition());
        final String[] header = new DataConverter().getHeader(data.getHandle());

        // Calculate max generalization levels
        final int maxGeneralizationLevels[] = new int[header.length];
        for (int i = 0; i < maxGeneralizationLevels.length; i++) {
            maxGeneralizationLevels[i] = data.getDefinition()
                                             .getHierarchy(data.getHandle().getAttributeName(i))[0].length - 1;
        }

        if (algorithm == BenchmarkAlgorithm.TASSA ||
            algorithm == BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING ||
            algorithm == BenchmarkAlgorithm.FLASH) {
            IBenchmarkObserver observer = new IBenchmarkObserver() {

                private boolean  isWarmup       = false;
                private int      run            = 0;
                private double[] utilityResults = new double[numberOfRuns];
                private double[] runtimes       = new double[numberOfRuns];

                @Override
                public void notify(long timestamp, String[][] output, int[] transformation) {
                    // Empty by design. We only want final results.
                }

                @Override
                public void notifyFinished(long timestamp, String[][] output) {

                    if (!isWarmup) {
                        // Obtain utility
                        double utility = 0d;
                        if (measure == BenchmarkUtilityMeasure.LOSS) {
                            utility = new UtilityMeasureLoss<Double>(header,
                                                                     hierarchies,
                                                                     AggregateFunction.GEOMETRIC_MEAN).evaluate(output)
                                                                                                      .getUtility();
                        } else if (measure == BenchmarkUtilityMeasure.DISCERNIBILITY) {
                            utility = new UtilityMeasureDiscernibility().evaluate(output)
                                                                        .getUtility();
                        }

                        // Normalize
                        utility -= metadata.getLowerBound(dataset, measure);
                        utility /= (metadata.getUpperBound(dataset, measure) - metadata.getLowerBound(dataset,
                                                                                                      measure));

                        // Save intermediary results
                        utilityResults[run] = utility;
                        runtimes[run] = timestamp;

                        // Calculate suppressed tuples
                        int suppressedTuples = 0;
                        for (int i = 0; i < output.length; i++) {
                            suppressedTuples += isSuppressed(output[i]) ? 1 : 0;
                        }
                        double suppressedRatio = (double) suppressedTuples / output.length;

                        // Write
                        if (run == numberOfRuns - 1) {

                            double utilityMean = calculateArithmeticMean(utilityResults);
                            double runtime = calculateArithmeticMean(runtimes);
                            double variance = getVariance(output, header, hierarchies, false);
                            double varianceNotSuppressed = getVariance(output,
                                                                       header,
                                                                       hierarchies,
                                                                       true);

                            BENCHMARK.addRun(dataset,
                                             measure,
                                             model,
                                             algorithm,
                                             suppressionLimit,
                                             gsFactor,
                                             gsStepSize,
                                             model.getStrength(),
                                             output.length,
                                             output[0].length);
                            BENCHMARK.addValue(UTILITY, utilityMean);
                            BENCHMARK.addValue(RUNTIME, runtime);
                            BENCHMARK.addValue(SUPPRESSED, suppressedTuples);
                            BENCHMARK.addValue(SUPPRESSED_RATIO, suppressedRatio);
                            BENCHMARK.addValue(VARIANCE, variance);
                            BENCHMARK.addValue(VARIANCE_NOTSUPPRESSED, varianceNotSuppressed);
                        }

                        run++;
                        // Run complete
                        if (numberOfRuns > 1 && (run % numberOfWarmups == 0 || run == numberOfRuns)) {
                            System.out.print(run + " ");
                        }
                    }
                }

                @Override
                public void setWarmup(boolean isWarmup) {
                    this.isWarmup = isWarmup;
                }
            };

            org.deidentifier.arx.benchmark.BenchmarkAlgorithm algorithmImplementation = null;
            if (algorithm == BenchmarkAlgorithm.TASSA) {
                algorithmImplementation = new TassaAlgorithm(observer, data, config);
            } else if (algorithm == BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING) {
                algorithmImplementation = new BenchmarkAlgorithmRGR(observer,
                                                                    data,
                                                                    config,
                                                                    gsStepSize);
            } else if (algorithm == BenchmarkAlgorithm.FLASH) {
                algorithmImplementation = new BenchmarkAlgorithmFlash(observer, data, config);
            }

            if (numberOfWarmups > 0) {
                System.out.print("Warmup... ");
                observer.setWarmup(true);
                for (int i = 0; i < numberOfWarmups; i++) {
                    algorithmImplementation.execute();
                }
                observer.setWarmup(false);
                System.out.println("done!");

                System.out.print("Iteration: ");
            }
            for (int i = 0; i < numberOfRuns; i++) {
                algorithmImplementation.execute();
            }
            System.out.println(">> done!");

        } else {
            throw new UnsupportedOperationException("Unimplemented Algorithm: " + algorithm);
        }
    }

    private static double getVariance(String[][] output,
                                      String[] header,
                                      Map<String, String[][]> hierarchies,
                                      boolean ignoreSuppressed) {

        final int numberOfRecords = output.length;
        final int numberOfAttributes = output[0].length;
        final int[] maxGeneralizationLevels = new int[numberOfAttributes];

        // Create maps with the generalization level for each string
        ArrayList<Map<String, Integer>> stringToLevelMaps = new ArrayList<Map<String, Integer>>();
        for (int columnIndex = 0; columnIndex < numberOfAttributes; columnIndex++) {
            String attribute = header[columnIndex];
            Map<String, Integer> map = new HashMap<String, Integer>();
            stringToLevelMaps.add(map);
            for (String[] row : hierarchies.get(attribute)) {
                maxGeneralizationLevels[columnIndex] = row.length;
                for (int level = row.length - 1; level >= 0; level--) {
                    if (map.containsKey(row[level])) {
                        int lvl = Math.max(map.get(row[level]), level);
                        map.put(row[level], lvl);
                    } else {
                        map.put(row[level], level);
                    }
                }
            }
        }

        // Compute average generalization degree per attribute
        double[] averageDegrees = new double[numberOfAttributes];
        Arrays.fill(averageDegrees, 0.0);
        int numberOfTuplesConsidered = 0;
        for (int rowIndex = 0; rowIndex < numberOfRecords; rowIndex++) {
            String[] row = output[rowIndex];
            if (!ignoreSuppressed || !isSuppressed(row)) {
                for (int columnIndex = 0; columnIndex < numberOfAttributes; columnIndex++) {
                    averageDegrees[columnIndex] += (double) stringToLevelMaps.get(columnIndex)
                                                                             .get(row[columnIndex]) /
                                                   maxGeneralizationLevels[columnIndex];
                }
                numberOfTuplesConsidered++;
            }
        }
        
        if (numberOfTuplesConsidered > 0) {
            for (int i = 0; i < averageDegrees.length; i++) {
                averageDegrees[i] /= numberOfTuplesConsidered;
            }

            // Compute variances
            double[] variances = new double[numberOfAttributes];
            Arrays.fill(variances, 0.0);
            for (int rowIndex = 0; rowIndex < numberOfRecords; rowIndex++) {
                String[] row = output[rowIndex];
                if (!ignoreSuppressed || !isSuppressed(row)) {
                    for (int columnIndex = 0; columnIndex < numberOfAttributes; columnIndex++) {
                        double degree = (double) stringToLevelMaps.get(columnIndex)
                                                                  .get(row[columnIndex]) /
                                        maxGeneralizationLevels[columnIndex];
                        variances[columnIndex] += Math.pow(degree - averageDegrees[columnIndex], 2);
                    }
                }
            }
            // Normalize
            for (int i = 0; i < variances.length; i++) {
                variances[i] /= numberOfTuplesConsidered;
            }
            return calculateArithmeticMean(variances);
        } else {
            return 0;
        }

    }

    /**
     * Get the arithmetic mean for a set of values.
     * 
     * @param values
     * @return The arithmetic mean.
     */
    private static double calculateArithmeticMean(double[] values) {
        if (values.length == 1) { return values[0]; }
        double arithmeticMean = 0d;
        for (double value : values) {
            arithmeticMean += value;
        }
        arithmeticMean /= values.length;
        return arithmeticMean;
    }

    /**
     * Is this row suppressed?
     * 
     * @param row
     * @return
     */
    private static boolean isSuppressed(String[] row) {
        for (String s : row) {
            if (!s.equals("*")) { return false; }
        }
        return true;
    }
}
