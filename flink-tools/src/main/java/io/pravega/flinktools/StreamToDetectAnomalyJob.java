package io.pravega.flinktools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.pravega.client.stream.StreamCut;
import io.pravega.connectors.flink.FlinkPravegaReader;
import io.pravega.flinktools.util.Alert;
import io.pravega.flinktools.util.SlackNotifier;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Display the contents of a Pravega stream as UTF8 strings in the Task Manager stderr.
 */
public class StreamToDetectAnomalyJob extends AbstractJob {
    final private static Logger log = LoggerFactory.getLogger(StreamToDetectAnomalyJob.class);
    private static SlackNotifier notifier = new SlackNotifier(
            "Chenna",
            "#nautilus-lab-alerts",
            "",
            true
    );

    /**
     * The entry point for Flink applications.
     *
     * @param args Command line arguments
     */
    public static void main(String... args) throws Exception {
        AppConfiguration config = new AppConfiguration(args);
        log.info("config: {}", config);
        StreamToConsoleJob job = new StreamToConsoleJob(config);
        job.run();
    }

    public StreamToDetectAnomalyJob(AppConfiguration appConfiguration) {
        super(appConfiguration);
    }

    public void run() {
        try {
            final String jobName = getConfig().getJobName(StreamToConsoleJob.class.getName());
            final AppConfiguration.StreamConfig inputStreamConfig = getConfig().getStreamConfig("input");
            log.info("input stream: {}", inputStreamConfig);
            createStream(inputStreamConfig);
            final StreamCut startStreamCut = resolveStartStreamCut(inputStreamConfig);
            final StreamCut endStreamCut = resolveEndStreamCut(inputStreamConfig);
            final StreamExecutionEnvironment env = initializeFlinkStreaming();
            final FlinkPravegaReader<String> flinkPravegaReader = FlinkPravegaReader.<String>builder()
                    .withPravegaConfig(inputStreamConfig.getPravegaConfig())
                    .forStream(inputStreamConfig.getStream(), startStreamCut, endStreamCut)
                    .withDeserializationSchema(new SimpleStringSchema())
                    .build();
            final DataStream<String> lines = env.addSource(flinkPravegaReader);
            lines.printToErr();

            DataStream<Alert> alerts = lines.flatMap(new AnomalyDetector());
            alerts.printToErr();

            log.info("Executing {} job", jobName);
            env.execute(jobName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A simple rule-based anomaly detector.
     * It considers a temperature of >100 deg to be an anomaly and outputs an Alert event.
     */
    public static class AnomalyDetector implements FlatMapFunction<String, Alert> {
        @Override
        public void flatMap(String message, Collector<Alert> out) throws JsonProcessingException {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(message);
            String status = node.get("status").asText();
            String payment = node.get("payment").asText();

            if(payment.equalsIgnoreCase("FAILED"))
            {
                Alert alert = new Alert();
                alert.AlertDescription = "Payment Failed";
                alert.poNumber = node.get("poNumber").asText();
                alert.status = node.get("status").asText();
                alert.date = node.get("date").asText();
                notifier.notify(alert.toString());
                out.collect(alert);
            }

            if(status.equalsIgnoreCase("CANCELLED"))
            {
                Alert alert = new Alert();
                alert.AlertDescription = "Payment Failed";
                alert.poNumber = node.get("poNumber").asText();
                alert.status = node.get("status").asText();
                alert.date = node.get("date").asText();
                notifier.notify(alert.toString());
                out.collect(alert);
            }

        }
    }
}
