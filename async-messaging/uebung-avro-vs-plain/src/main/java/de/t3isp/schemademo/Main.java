package de.t3isp.schemademo;

public class Main {
    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "";
        switch (mode) {
            case "plain-producer" -> PlainProducer.main(args);
            case "plain-consumer" -> PlainConsumer.main(args);
            case "avro-producer"  -> AvroProducer.main(args);
            case "avro-consumer"  -> AvroConsumer.main(args);
            default -> {
                System.err.println("Unbekannter Modus: " + mode);
                System.err.println("Erwartet: plain-producer | plain-consumer | avro-producer | avro-consumer");
                System.exit(1);
            }
        }
    }
}
