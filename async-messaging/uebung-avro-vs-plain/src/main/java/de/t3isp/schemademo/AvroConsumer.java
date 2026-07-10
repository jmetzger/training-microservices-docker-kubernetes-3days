package de.t3isp.schemademo;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

/**
 * Consumer mit Avro + Schema Registry. Das Schema wird pro Nachricht
 * automatisch anhand der Schema-ID aus der Registry aufgeloest -
 * neue, abwaertskompatible Felder tauchen einfach mit auf.
 */
public class AvroConsumer {

    public static void main(String[] args) {
        String bootstrapServers = Env.get("BOOTSTRAP_SERVERS", "kafka.kafka.svc.cluster.local:9092");
        String schemaRegistryUrl = Env.get("SCHEMA_REGISTRY_URL", "http://schema-registry:8081");
        String topic = Env.get("TOPIC", "orders-avro");
        String groupId = Env.get("GROUP_ID", "avro-consumer-" + System.currentTimeMillis());
        int idlePollsToExit = Env.getInt("IDLE_POLLS_TO_EXIT", 5);

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, false);

        System.out.println("[avro-consumer] bootstrap=" + bootstrapServers + " registry=" + schemaRegistryUrl
                + " topic=" + topic + " groupId=" + groupId);

        try (KafkaConsumer<String, GenericRecord> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            int emptyPolls = 0;
            int consumed = 0;
            while (emptyPolls < idlePollsToExit) {
                ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofSeconds(3));
                if (records.isEmpty()) {
                    emptyPolls++;
                    continue;
                }
                emptyPolls = 0;
                for (ConsumerRecord<String, GenericRecord> record : records) {
                    consumed++;
                    GenericRecord value = record.value();
                    Object customer = value.getSchema().getField("customer") != null ? value.get("customer") : "<Feld existiert in diesem Schema nicht>";
                    System.out.println("[avro-consumer] gelesen: id=" + value.get("id")
                            + " product=" + value.get("product")
                            + " quantity=" + value.get("quantity")
                            + " customer=" + customer);
                }
            }
            System.out.println("[avro-consumer] fertig, " + consumed + " Nachrichten gelesen, keine neuen mehr seit "
                    + (idlePollsToExit * 3) + "s -> beende");
        }
    }
}
