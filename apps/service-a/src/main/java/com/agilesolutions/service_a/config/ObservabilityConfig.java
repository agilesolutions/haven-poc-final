package com.agilesolutions.service_a.config;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry Configuration for Service A
 * 
 * Configures distributed tracing, metrics collection, and logs export
 * to LGTM stack (Loki, Grafana, Tempo, Prometheus).
 */
@Configuration
public class ObservabilityConfig {

    @Value("${management.otlp.tracing.endpoint:http://localhost:4317}")
    private String otlpEndpoint;

    @Value("${spring.application.name:service-a}")
    private String applicationName;

    @Value("${spring.application.version:1.0.0}")
    private String applicationVersion;

    @Value("${management.tracing.sampling.probability:1.0}")
    private double samplingProbability;

    /**
     * Creates a Resource identifying this service
     */
    @Bean
    public Resource otelResource() {
        return Resource.getDefault()
                .merge(Resource.builder()
                        .put("service.name", applicationName)
                        .put("service.version", applicationVersion)
                        .put("environment", System.getProperty("spring.profiles.active", "dev"))
                        .build());
    }

    /**
     * Configures TracerProvider for distributed tracing
     */
    @Bean
    public SdkTracerProvider tracerProvider(Resource resource) {
        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(otlpEndpoint)
                .build();

        return SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .setSampler(Sampler.traceIdRatioBased(samplingProbability))
                .build();
    }

    /**
     * Configures MeterProvider for metrics collection
     */
    @Bean
    public SdkMeterProvider meterProvider(Resource resource) {
        OtlpGrpcMetricExporter metricExporter = OtlpGrpcMetricExporter.builder()
                .setEndpoint(otlpEndpoint)
                .build();

        return SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(PeriodicMetricReader.builder(metricExporter)
                        .setIntervalMillis(60000) // Export metrics every 60 seconds
                        .build())
                .build();
    }

    /**
     * Update GlobalOpenTelemetry with configured providers
     * This ensures Spring Boot auto-configuration picks up our configuration
     */
    @Bean
    public TracerProvider globalTracerProvider(SdkTracerProvider sdkTracerProvider) {
        return sdkTracerProvider;
    }

    @Bean
    public MeterProvider globalMeterProvider(SdkMeterProvider sdkMeterProvider) {
        return sdkMeterProvider;
    }
}

