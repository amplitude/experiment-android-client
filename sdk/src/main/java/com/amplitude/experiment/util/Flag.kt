package com.amplitude.experiment.util

import com.amplitude.experiment.evaluation.EvaluationFlag

internal fun EvaluationFlag?.isLocalEvaluationMode() : Boolean{
    return this?.metadata?.get("evaluationMode") == "local"
}

internal fun EvaluationFlag?.isRemoteEvaluationMode() : Boolean{
    return this?.metadata?.get("evaluationMode") == "remote"
}
