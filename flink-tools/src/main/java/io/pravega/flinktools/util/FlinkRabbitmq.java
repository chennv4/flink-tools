package io.pravega.flinktools.util;

/**
 * Created by rootcss on 16/12/16.
 */

import com.rabbitmq.client.AMQP;
import org.apache.flink.streaming.connectors.rabbitmq.RMQSource;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;
import org.apache.flink.streaming.util.serialization.DeserializationSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

public class FlinkRabbitmq extends RMQSource {

    public static String exchangeName          = "demo";
    public static String queueName             = "demo-q";
    public static String rabbitmqHostname      = "10.243.37.7";
    public static String rabbitmqVirtualHost   = "/";
    public static String rabbitmqUsername      = "admin";
    public static String rabbitmqPassword      = "password";
    public static Integer rabbitmqPort         = 5672;
    public static boolean durableQueue         = false;

    public static Logger logger = LoggerFactory.getLogger(FlinkRabbitmq.class);

    public FlinkRabbitmq(RMQConnectionConfig rmqConnectionConfig, String queueName, DeserializationSchema deserializationSchema) {
        super(rmqConnectionConfig, queueName, deserializationSchema);
    }

    @Override
    protected void setupQueue() throws IOException {
        AMQP.Queue.DeclareOk result = channel.queueDeclare(queueName, true, durableQueue, false, null);
        channel.queueBind(result.getQueue(), exchangeName, "*");
    }
}
