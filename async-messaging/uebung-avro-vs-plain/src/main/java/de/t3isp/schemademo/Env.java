package de.t3isp.schemademo;

class Env {
    static String get(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    static int getInt(String name, int defaultValue) {
        return Integer.parseInt(get(name, String.valueOf(defaultValue)));
    }
}
