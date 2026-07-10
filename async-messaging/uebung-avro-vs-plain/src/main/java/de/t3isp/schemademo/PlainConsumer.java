package de.t3isp.schemademo;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Consumer ohne Schema-Kontrolle: erwartet hart das Feld "quantity".
 * Es gibt keine Registry, die vor Producern warnt, die dieses Feld umbenennen.
 */
public class PlainConsumer {

    private static final Pattern QUANTITY_FIELD = Pattern.compile("\"quantity\"\\s*:\\s*(\\d+)");

    public static void main(String[] args) {
        String bootstrapServers = Env.get("BOOTSTRAP_SERVERS", "kafka.kafka.svc.cluster.local:9092");
        String topic = Env.get("TOPIC", "orders-plain");
        String groupId = Env.get("GROUP_ID", "plain-consumer-" + System.currentTimeMillis());
        int idlePollsToExit = Env.getInt("IDLE_POLLS_TO_EXIT", 5);

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        System.out.println("[plain-consumer] bootstrap=" + bootstrapServers + " topic=" + topic + " groupId=" + groupId);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            int emptyPolls = 0;
            int consumed = 0;
            while (emptyPolls < idlePollsToExit) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(3));
                if (records.isEmpty()) {
                    emptyPolls++;
                    continue;
                }
                emptyPolls = 0;
                for (ConsumerRecord<String, String> record : records) {
                    consumed++;
                    Matcher matcher = QUANTITY_FIELD.matcher(record.value());
                    if (matcher.find()) {
                        System.out.println("[plain-consumer] gelesen: " + record.value()
                                + " -> quantity=" + matcher.group(1));
                    } else {
                        System.out.println("[plain-consumer] gelesen: " + record.value()
                                + " -> quantity=FEHLT! Feld nicht gefunden, Consumer erwartet \"quantity\", Producer hat es umbenannt.");
                    }
                }
            }
            System.out.println("[plain-consumer] fertig, " + consumed + " Nachrichten gelesen, keine neuen mehr seit "
                    + (idlePollsToExit * 3) + "s -> beende");
        }
    }
}
