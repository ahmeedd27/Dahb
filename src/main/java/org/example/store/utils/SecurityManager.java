package org.example.store.utils;

import org.example.store.model.User;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SecurityManager {

    private static final Path LICENSE_PATH = Paths.get(System.getProperty("user.home"), ".store_license", "license.dat");

    public static boolean isActivated() {
        try {
            if (!Files.exists(LICENSE_PATH)) return false;

            String encrypted = new String(Files.readAllBytes(LICENSE_PATH));
            String savedKey = CryptoUtil.decrypt(encrypted);

            String uuid = HardwareUtils.getMachineUUID();
            String expected = uuid + PurchaseProductDTO.getS();

            return savedKey.equals(expected);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void saveActivationKey(String rawKey) {
        try {
            String encrypted = CryptoUtil.encrypt(rawKey);
            Files.createDirectories(LICENSE_PATH.getParent());
            Files.write(LICENSE_PATH, encrypted.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String generateActivationKey(String uuid) {
        return uuid + PurchaseProductDTO.getS();
    }
    public static boolean userExists() {
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM user");
             ResultSet rs = stmt.executeQuery()) {

            return rs.next() && rs.getInt(1) > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}