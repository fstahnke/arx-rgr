/*
 * Source code of the experiments for the entropy metric
 *      
 * Copyright (C) 2015 Fabian Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deidentifier.arx.benchmark;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkAlgorithm;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkPrivacyModel;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkUtilityMeasure;

import de.linearbits.objectselector.Selector;
import de.linearbits.subframe.analyzer.Analyzer;
import de.linearbits.subframe.graph.Field;
import de.linearbits.subframe.graph.Labels;
import de.linearbits.subframe.graph.Plot;
import de.linearbits.subframe.graph.PlotLinesClustered;
import de.linearbits.subframe.graph.Point2D;
import de.linearbits.subframe.graph.Point3D;
import de.linearbits.subframe.graph.Series2D;
import de.linearbits.subframe.graph.Series3D;
import de.linearbits.subframe.io.CSVFile;
import de.linearbits.subframe.render.GnuPlotParams;
import de.linearbits.subframe.render.GnuPlotParams.KeyPos;
import de.linearbits.subframe.render.LaTeX;
import de.linearbits.subframe.render.PlotGroup;

/**
 * Example benchmark
 * 
 * @author Fabian Prasser
 */
public class BenchmarkAnalysis2 {

    /**
     * Main
     * 
     * @param args
     * @throws IOException
     * @throws ParseException
     */
    public static void main(String[] args) throws IOException, ParseException {

        List<PlotGroup> groups = new ArrayList<PlotGroup>();
        BenchmarkSetup setup = new BenchmarkSetup("benchmarkConfig/generalizationDegreeRGR.xml");
        CSVFile file = new CSVFile(new File(setup.getPlotFile()));

        // Repeat for each data set
        for (BenchmarkAlgorithm algorithm : setup.getAlgorithms()) {
            for (BenchmarkDataset data : setup.getDatasets()) {
                for (BenchmarkPrivacyModel model : setup.getPrivacyModels()) {
                    for (BenchmarkUtilityMeasure measure : setup.getUtilityMeasures()) {
                        for (double suppression : setup.getSuppressionLimits()) {
                            groups.add(analyze(file, data, measure, model, algorithm, suppression));
                        }
                    }
                }
            }
        }

        LaTeX.plot(groups, "results/experiment2");

    }

    /**
     * Performs the analysis
     * 
     * @param file
     * @param suppression
     * @param algorithm
     * @param model
     * @param measure
     * @param data
     * @return
     * @throws ParseException
     */
    private static PlotGroup analyze(CSVFile file,
                                     BenchmarkDataset data,
                                     BenchmarkUtilityMeasure measure,
                                     BenchmarkPrivacyModel model,
                                     BenchmarkAlgorithm algorithm,
                                     double suppression) throws ParseException {

        // Selects according rows
        Selector<String[]> selector = file.getSelectorBuilder()
                                          .field("Dataset")
                                          .equals(data.toString())
                                          .and()
                                          .field("UtilityMeasure")
                                          .equals(measure.toString())
                                          .and()
                                          .field("PrivacyModel")
                                          .equals(model.toString())
                                          .and()
                                          .field("Algorithm")
                                          .equals(algorithm.toString())
                                          .and()
                                          .field("Suppression")
                                          .equals(String.valueOf(suppression))
                                          .build();

        // Read generalization degrees into 2D series
        Series2D[] degreeSeries = new Series2D[9];
        for (int i = 0; i < 9; i++) {
            degreeSeries[i] = new Series2D(file,
                                           selector,
                                           new Field("Time", Analyzer.VALUE),
                                           new Field("Generalization degree " + (i+1), Analyzer.VALUE));
        }

        // Dirty hack for creating a 3D series from two 2D series'
        Series3D series = new Series3D(file, selector, new Field("Dataset"), // Cluster
                                       new Field("UtilityMeasure"), // Type
                                       new Field("PrivacyModel")); // Value
        series.getData().clear();
        
        // Read generalization degrees into 2D series
        for (int i = 0; i < 9; i++) {
            for (Point2D point : degreeSeries[i].getData()) {
                series.getData().add(new Point3D(point.x, "Generalization degree " + (i+1), point.y));
            }
        }

        // Plot
        List<Plot<?>> plots = new ArrayList<Plot<?>>();
        plots.add(new PlotLinesClustered(data.toString() + "/" + measure.toString() + "/" +
                                                 model.toString() + "/" +
                                                 String.valueOf(suppression),
                                         new Labels("Time [ms]",
                                                    "Utility [%] / Suppression [%] / Generalization Degree [%]"),
                                         series));

        GnuPlotParams params = new GnuPlotParams();
        params.colorize = true;
        params.rotateXTicks = 0;
        params.keypos = KeyPos.TOP_LEFT;
        params.size = 1.0d;
        params.ratio = 0.5d;
        return new PlotGroup("Development of utility and ratio of suppressed tuples over time. ",
                             plots,
                             params,
                             1.0d);
    }
}
