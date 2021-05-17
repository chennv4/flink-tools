/*
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 */
package io.pravega.flinktools;

import com.google.common.base.Strings;
import io.pravega.connectors.flink.FlinkPravegaWriter;
import io.pravega.connectors.flink.PravegaWriterMode;
import io.pravega.flinktools.util.EventNumberIterator;
import io.pravega.flinktools.util.JsonSerializationSchema;
import io.pravega.flinktools.util.SampleEvent;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.rabbitmq.RMQSource;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static io.pravega.flinktools.util.FlinkRabbitmq.*;

/**
 * This job simulates writing events from multiple sensors to Pravega.
 * Events are encoded as JSON.
 */
public class RabbitMQToPravegaJob extends AbstractJob {
    final private static Logger log = LoggerFactory.getLogger(RabbitMQToPravegaJob.class);

    /**
     * The entry point for Flink applications.
     *
     * @param args Command line arguments
     */
    public static void main(String... args) throws Exception {
        AppConfiguration config = new AppConfiguration(args);
        log.info("config: {}", config);
        RabbitMQToPravegaJob job = new RabbitMQToPravegaJob(config);
        job.run();
    }

    public RabbitMQToPravegaJob(AppConfiguration appConfiguration) {
        super(appConfiguration);
    }

    public void run() {
        try {
            final String jobName = getConfig().getJobName(RabbitMQToPravegaJob.class.getName());

            final double eventsPerSec = getConfig().getParams().getDouble("eventsPerSec", 1.0);
            log.info("eventsPerSec: {}", eventsPerSec);
            final int numSensors = getConfig().getParams().getInt("numSensors", 1);
            log.info("numSensors: {}", numSensors);
            final int dataSizeBytes = getConfig().getParams().getInt("dataSizeBytes", 10);
            log.info("dataSizeBytes: {}", dataSizeBytes);
            final AppConfiguration.StreamConfig outputStreamConfig = getConfig().getStreamConfig("output");
            log.info("output stream: {}", outputStreamConfig);
            createStream(outputStreamConfig);

            final StreamExecutionEnvironment env = initializeFlinkStreaming();

            RMQConnectionConfig connectionConfig = new RMQConnectionConfig.Builder()
                    .setHost(rabbitmqHostname).setPort(rabbitmqPort).setUserName(rabbitmqUsername)
                    .setPassword(rabbitmqPassword).setVirtualHost(rabbitmqVirtualHost).build();

            DataStream<String> dataStream = env.addSource(new RMQSource<String>(connectionConfig,
                    queueName,
                    new org.apache.flink.api.common.serialization.SimpleStringSchema()));

            DataStream<SampleEvent> pairs = dataStream.flatMap(new TextLengthCalculator())
                    .setParallelism(6);
                    //.keyBy(0)
                    //.setParallelism();


            // Write to Pravega as JSON.
            FlinkPravegaWriter<SampleEvent> sink = FlinkPravegaWriter.<SampleEvent>builder()
                    .withPravegaConfig(outputStreamConfig.getPravegaConfig())
                    .forStream(outputStreamConfig.getStream())
                    .withSerializationSchema(new JsonSerializationSchema<>())
                    .withEventRouter(frame -> String.format("%d", frame.sensorId))
                    .withWriterMode(PravegaWriterMode.ATLEAST_ONCE)
                    .build();

            pairs.addSink(sink)
                    .uid("pravega-writer")
                    .name("pravega-writer");

            log.info("Executing {} job", jobName);
            env.execute(jobName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static final class TextLengthCalculator implements FlatMapFunction<String, SampleEvent> {

        @Override
        public void flatMap(String value, Collector<SampleEvent> out) {
            Random ran = new Random();
            int x = ran.nextInt(6) + 5;
            long eventNumber = ran.nextLong();
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());

            SampleEvent event = new SampleEvent(x, eventNumber, timestamp, value);

            out.collect(event);
        }

        public static <T> T getRandomElement(T[] arr){
            return arr[ThreadLocalRandom.current().nextInt(arr.length)];
        }

    }
}
