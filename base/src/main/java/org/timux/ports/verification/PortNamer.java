package org.timux.ports.verification;

class PortNamer {

    public static String toOutPortName(String messageType) {
        String suffix = messageType.substring(messageType.lastIndexOf('.') + 1)
                + (messageType.endsWith("Exception") ? "Event" : "");

        return Character.toLowerCase(suffix.charAt(0)) + suffix.substring(1);
    }

    public static String toInPortName(String messageType) {
        String suffix = messageType.substring(messageType.lastIndexOf('.') + 1);

        if (messageType.endsWith("Event")) {
            return "on" + suffix.substring(0, suffix.length() - "Event".length());
        } else {
            return "on" + suffix;
        }
    }
}
