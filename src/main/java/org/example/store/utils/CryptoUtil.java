package org.example.store.utils;

import org.example.store.controller.LoginController;
import org.example.store.controller.ProductViewController;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;


public class CryptoUtil {
    public static String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance(ProductViewController.f() + LoginController.s());
        SecretKeySpec keySpec = new SecretKeySpec(l(), ProductViewController.f() + LoginController.s());
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static byte[] l() {
        int[] ints = {65, 97, 83, 115, 68, 100, 70, 102, 49, 50, 51, 52, 52, 51, 50, 49};
        byte[] f = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            f[i] = (byte) ints[i];
        }
        return f;
    }

    public static String decrypt(String encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance(ProductViewController.f() + LoginController.s());
        SecretKeySpec keySpec = new SecretKeySpec(l(), ProductViewController.f() + LoginController.s());
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedBytes);
    }
}
