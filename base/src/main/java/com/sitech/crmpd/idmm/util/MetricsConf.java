package com.sitech.crmpd.idmm.util;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Strings;
import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Created by guanyf on 7/5/2017.
 */
@Configuration
@EnableMetrics
public class MetricsConf extends MetricsConfigurerAdapter {
    @Value("${metrics_interval_secs:10}")
    private int metrics_interval_secs;

    @Value("${metrics_out_path:}")
    private String outdir;

    @Override
    public void configureReporters(MetricRegistry metricRegistry) {
        if(!Strings.isNullOrEmpty(outdir)) {
//            ConsoleReporter
//                    .forRegistry(metricRegistry)
//                    .build()
//                    .start(metrics_interval_secs, TimeUnit.SECONDS);
            final CsvReporter reporter = CsvReporter.forRegistry(metricRegistry)
                    //.formatFor(Locale.US)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .build(new File(outdir));
            reporter.start(metrics_interval_secs, TimeUnit.SECONDS);
        }
    }
}
