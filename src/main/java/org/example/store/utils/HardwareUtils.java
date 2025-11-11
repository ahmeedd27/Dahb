package org.example.store.utils;

import java.io.File;
import java.util.UUID;

public class HardwareUtils {
    public static String getMachineUUID() {
        try {
            String os = System.getProperty("os.name");
            String user = System.getProperty("user.name");
            String home = System.getProperty("user.home");
            String processor = System.getenv("PROCESSOR_IDENTIFIER");
            String disk = new File(System.getProperty("user.home")).getTotalSpace() + "";

            String raw = os + user + home + processor + disk;
            return UUID.nameUUIDFromBytes(raw.getBytes()).toString().toUpperCase();
        } catch (Exception e) {
            e.printStackTrace();
            return "UNKNOWN-UUID";
        }
    }
}