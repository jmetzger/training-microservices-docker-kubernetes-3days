package de.t3isp.schemademo;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Producer mit Avro + Schema Registry. Die Registry prueft bei jedem
 * Producer-Start (Registrierung des Schemas), ob die neue Version mit
 * der Kompatibilitaetsregel des Subjects vereinbar ist.
 */
public class AvroProducer {

    public static void main(String[] args) throws IOException {
        String bootstrapServers = Env.get("BOOTSTRAP_SERVERS", "kafka.kafka.svc.cluster.local:9092");
        String schemaRegistryUrl = Env.get("SCHEMA_REGISTRY_URL", "http://schema-registry:8081");
        String topic = Env.get("TOPIC", "orders-avro");
        String schemaVersion = Env.get("SCHEMA_VERSION", "v1");
        int messageCount = Env.getInt("MESSAGE_COUNT", 5);

        Schema schema = loadSchema(schemaVersion);

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);

        System.out.println("[avro-producer] bootstrap=" + bootstrapServers + " registry=" + schemaRegistryUrl
                + " topic=" + topic + " schemaVersion=" + schemaVersion);
        System.out.println("[avro-producer] Schema:\n" + schema.toString(true));

        try (KafkaProducer<String, GenericRecord> producer = new KafkaProducer<>(props)) {
            for (int i = 1; i <= messageCount; i++) {
                String key = "o-" + i;
                GenericRecord record = buildRecord(schema, schemaVersion, key, i);
                producer.send(new ProducerRecord<>(topic, key, record), (metadata, exception) -> {
                    if (exception != null) {
                        System.err.println("[avro-producer] Fehler beim Senden (Registry hat vermutlich abgelehnt): "
                                + exception.getMessage());
                    } else {
                        System.out.println("[avro-producer] gesendet: " + record
                                + " -> partition=" + metadata.partition() + " offset=" + metadata.offset());
                    }
                });
            }
            producer.flush();
            System.out.println("[avro-producer] fertig, " + messageCount + " Nachrichten gesendet");
        } catch (Exception e) {
            System.err.println("[avro-producer] ABGEBROCHEN: " + e.getMessage());
            System.exit(1);
        }
    }

    private static Schema loadSchema(String schemaVersion) throws IOException {
        String resource = "/order-" + schemaVersion + ".avsc";
        try (InputStream in = AvroProducer.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("Schema-Resource nicht gefunden: " + resource);
            }
            return new Schema.Parser().parse(in);
        }
    }

    private static GenericRecord buildRecord(Schema schema, String schemaVersion, String id, int quantity) {
        GenericData.Record record = new GenericData.Record(schema);
        record.put("id", id);
        record.put("product", "Schraubenzieher");
        if ("v3-incompatible".equals(schemaVersion)) {
            record.put("quantity", String.valueOf(quantity));
        } else {
            record.put("quantity", quantity);
        }
        if (schema.getField("customer") != null) {
            record.put("customer", "Kunde-" + id);
        }
        return record;
    }
}
