# experiment-android-client
Android Client SDK for Experiment

## Getting Started

See the [Documentation](https://amplitude.com/docs/sdks/experiment-sdks/experiment-android) for getting started.

## Custom HTTP client

Experiment uses a shared OkHttp client by default for variant and flag configuration requests. To
customize networking, supply a caller-owned `OkHttpClient` as the call factory when building the
configuration:

```kotlin
val httpClient = OkHttpClient.Builder()
    .dns(myDns)
    .build()
val config = ExperimentConfig.builder()
    .callFactory(httpClient)
    .build()
```

Pass `config` to `Experiment.initialize` or `Experiment.initializeWithAmplitudeAnalytics`. The
factory applies to both request types. Set it before the first initialization of an instance name
and API key; later initialization calls for that instance return the existing client. Experiment
does not change or close the supplied factory. Other Amplitude SDK network traffic is unaffected.
