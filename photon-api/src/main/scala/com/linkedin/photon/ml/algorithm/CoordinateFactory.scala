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
package com.linkedin.photon.ml.algorithm

import com.linkedin.photon.ml.data.{DataSet, FixedEffectDataSet, RandomEffectDataSetInProjectedSpace}
import com.linkedin.photon.ml.function.ObjectiveFunctionHelper.ObjectiveFunctionFactory
import com.linkedin.photon.ml.function.{DistributedObjectiveFunction, ObjectiveFunction, SingleNodeObjectiveFunction}
import com.linkedin.photon.ml.model.Coefficients
import com.linkedin.photon.ml.normalization.{NormalizationContextBroadcast, NormalizationContextWrapper}
import com.linkedin.photon.ml.optimization.DistributedOptimizationProblem
import com.linkedin.photon.ml.optimization.game.{CoordinateOptimizationConfiguration, FixedEffectOptimizationConfiguration, RandomEffectOptimizationConfiguration, RandomEffectOptimizationProblem}
import com.linkedin.photon.ml.sampling.DownSampler
import com.linkedin.photon.ml.sampling.DownSamplerHelper.DownSamplerFactory
import com.linkedin.photon.ml.supervised.model.GeneralizedLinearModel
import com.linkedin.photon.ml.util.PhotonBroadcast

/**
 * Factory to build [[Coordinate]] derived objects. Given generic input shared between coordinates, determine the type
 * of [[Coordinate]] to build, and do so.
 */
object CoordinateFactory {

  /**
   * Creates a [[Coordinate]] of the appropriate type, given the input [[DataSet]],
   * [[CoordinateOptimizationConfiguration]], and [[ObjectiveFunction]].
   *
   * @tparam D Some type of [[DataSet]]
   * @param dataSet The input data to use for training
   * @param coordinateOptConfig The optimization settings for training
   * @param lossFunctionConstructor A constructor for the loss function used for training
   * @param glmConstructor A constructor for the type of [[GeneralizedLinearModel]] being trained
   * @param downSamplerFactory A factory function for the [[DownSampler]] (if down-sampling is enabled)
   * @param normalizationContextWrapper A wrapper for the [[com.linkedin.photon.ml.normalization.NormalizationContext]]
   * @param trackState Should the internal optimization states be recorded?
   * @param computeVariance Should the trained coefficient variances be computed in addition to the means?
   * @return A [[Coordinate]] for the [[DataSet]] of type [[D]]
   */
  def build[D <: DataSet[D]](
      dataSet: D,
      coordinateOptConfig: CoordinateOptimizationConfiguration,
      lossFunctionConstructor: ObjectiveFunctionFactory,
      glmConstructor: (Coefficients) => GeneralizedLinearModel,
      downSamplerFactory: DownSamplerFactory,
      normalizationContextWrapper: NormalizationContextWrapper,
      trackState: Boolean,
      computeVariance: Boolean): Coordinate[D] = {

    val lossFunction: ObjectiveFunction = lossFunctionConstructor(coordinateOptConfig)

    (dataSet, coordinateOptConfig, lossFunction, normalizationContextWrapper) match {
      case (
        fEDataSet: FixedEffectDataSet,
        fEOptConfig: FixedEffectOptimizationConfiguration,
        distributedLossFunction: DistributedObjectiveFunction,
        normalizationContextBroadcast: NormalizationContextBroadcast) =>

        val downSamplerOpt = if (DownSampler.isValidDownSamplingRate(fEOptConfig.downSamplingRate)) {
          Some(downSamplerFactory(fEOptConfig.downSamplingRate))
        } else {
          None
        }

        new FixedEffectCoordinate(
          fEDataSet,
          DistributedOptimizationProblem(
            fEOptConfig,
            distributedLossFunction,
            downSamplerOpt,
            glmConstructor,
            PhotonBroadcast(normalizationContextBroadcast.context),
            trackState,
            computeVariance)).asInstanceOf[Coordinate[D]]

      case (
        rEDataSet: RandomEffectDataSetInProjectedSpace,
        rEOptConfig: RandomEffectOptimizationConfiguration,
        singleNodeLossFunction: SingleNodeObjectiveFunction,
        _) =>

        new RandomEffectCoordinateInProjectedSpace(
          rEDataSet,
          RandomEffectOptimizationProblem(
            rEDataSet,
            rEOptConfig,
            singleNodeLossFunction,
            glmConstructor,
            normalizationContextWrapper,
            trackState,
            computeVariance)).asInstanceOf[Coordinate[D]]

      case _ =>
        throw new UnsupportedOperationException(
          s"""Cannot build coordinate for the following input class combination:
          |  ${dataSet.getClass.getName}
          |  ${coordinateOptConfig.getClass.getName}
          |  ${lossFunction.getClass.getName}
          |  ${normalizationContextWrapper.getClass.getName}""".stripMargin)
    }
  }
}