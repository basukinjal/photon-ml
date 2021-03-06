/*
 * Copyright 2017 LinkedIn Corp. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.linkedin.photon.ml.diagnostics.independence

import com.linkedin.photon.ml.diagnostics.reporting.LogicalReport

/**
 * Analysis of independence of error and prediction.
 *
 * @param errorSample Sample of errors. This should have same length as [[predictionSample]]
 * @param predictionSample Sample of predictions. This should have the same length as [[errorSample]]
 * @param kendallTau Kendall &tau; independence test report
 */
case class PredictionErrorIndependenceReport(val errorSample: Array[Double],
                                             val predictionSample: Array[Double],
                                             val kendallTau: KendallTauReport) extends LogicalReport

