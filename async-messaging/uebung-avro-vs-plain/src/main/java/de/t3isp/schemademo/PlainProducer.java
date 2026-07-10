package de.t3isp.schemademo;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

/**
 * Producer ohne jegliche Schema-Kontrolle: schreibt rohe JSON-Strings.
 * Niemand prueft, ob das Feld-Layout zum Consumer passt.
 */
public class PlainProducer {

    public static void main(String[] args) {
        String bootstrapServers = Env.get("BOOTSTRAP_SERVERS", "kafka.kafka.svc.cluster.local:9092");
        String topic = Env.get("TOPIC", "orders-plain");
        String schemaVersion = Env.get("SCHEMA_VERSION", "v1");
        int messageCount = Env.getInt("MESSAGE_COUNT", 5);

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        System.out.println("[plain-producer] bootstrap=" + bootstrapServers + " topic=" + topic + " schemaVersion=" + schemaVersion);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            for (int i = 1; i <= messageCount; i++) {
                String key = "o-" + i;
                String json = buildJson(schemaVersion, key, i);
                producer.send(new ProducerRecord<>(topic, key, json), (metadata, exception) -> {
                    if (exception != null) {
                        System.err.println("[plain-producer] Fehler beim Senden: " + exception.getMessage());
                    } else {
                        System.out.println("[plain-producer] gesendet: " + json
                                + " -> partition=" + metadata.partition() + " offset=" + metadata.offset());
                    }
                });
            }
            producer.flush();
        }
        System.out.println("[plain-producer] fertig, " + messageCount + " Nachrichten gesendet");
    }

    private static String buildJson(String schemaVersion, String id, int quantity) {
        return switch (schemaVersion) {
            case "v1" -> """
                    {"id":"%s","product":"Schraubenzieher","quantity":%d}""".formatted(id, quantity);
            // Breaking Change: Feld "quantity" wurde ohne Ankuendigung in "qty" umbenannt.
            // Ohne Registry gibt es niemanden, der das verhindert.
            case "v2-breaking" -> """
                    {"id":"%s","product":"Schraubenzieher","qty":%d}""".formatted(id, quantity);
            default -> throw new IllegalArgumentException("Unbekannte SCHEMA_VERSION: " + schemaVersion);
        };
    }
}
