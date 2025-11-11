package org.example.store.utils;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupUtil {
    public static void createManualBackup() {
        String manualDir = "./manual_backup/";
        String zipFileName = manualDir + "manual-backup.zip";

        try {
            Files.createDirectories(Paths.get(manualDir));

            try (FileOutputStream fos = new FileOutputStream(zipFileName);
                 ZipOutputStream zipOut = new ZipOutputStream(fos);
                 FileInputStream fis = new FileInputStream("./store.db")) {

                ZipEntry zipEntry = new ZipEntry("store.db");
                zipOut.putNextEntry(zipEntry);

                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
            }

            System.out.println("✅ Manual backup created at: " + zipFileName);

        } catch (IOException e) {
            System.out.println("❌ Failed to create manual backup");
            e.printStackTrace();
        }
    }


}
