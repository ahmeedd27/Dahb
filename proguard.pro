# ========== INPUT & OUTPUT ==========
-injars  target/Store-1.0-SNAPSHOT-jar-with-dependencies.jar
-outjars target/Store-obfuscated.jar

# ========== LIBRARIES ==========
# مكتبات الـ JDK 8 (rt.jar + jce.jar)
-libraryjars "C:/Program Files (x86)/Java/jdk1.8.0_202/jre/lib/rt.jar"
-libraryjars "C:/Program Files (x86)/Java/jdk1.8.0_202/jre/lib/jce.jar"

# مكتباتك الخارجية (من الـ Maven)
-libraryjars "C:/Users/computop/.m2/repository/org/xerial/sqlite-jdbc/3.36.0.3/sqlite-jdbc-3.36.0.3.jar"
-libraryjars "C:/Users/computop/.m2/repository/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar"
-libraryjars "C:/Users/computop/.m2/repository/org/json/json/20230227/json-20230227.jar"
-libraryjars "C:/Users/computop/.m2/repository/com/itextpdf/itextpdf/5.5.13.3/itextpdf-5.5.13.3.jar"
-libraryjars "C:/Users/computop/.m2/repository/com/ibm/icu/icu4j/72.1/icu4j-72.1.jar"
-libraryjars "C:/Users/computop/.m2/repository/com/google/zxing/core/3.4.1/core-3.4.1.jar"
-libraryjars "C:/Users/computop/.m2/repository/com/google/zxing/javase/3.4.1/javase-3.4.1.jar"
-libraryjars "C:/Users/computop/.m2/repository/org/controlsfx/controlsfx/8.40.14/controlsfx-8.40.14.jar"
-libraryjars "C:/Users/computop/.m2/repository/org/kordamp/ikonli/ikonli-javafx/2.4.0/ikonli-javafx-2.4.0.jar"
-libraryjars "C:/Users/computop/.m2/repository/org/kordamp/bootstrapfx/bootstrapfx-core/0.4.0/bootstrapfx-core-0.4.0.jar"
-libraryjars "C:/Users/computop/.m2/repository/eu/hansolo/tilesfx/1.5.8/tilesfx-1.5.8.jar"

# ========== GENERAL SETTINGS ==========
-dontoptimize
-ignorewarnings
-dontwarn javafx.**
-dontwarn com.sun.javafx.**
-dontwarn org.sqlite.**

# ========== ENTRY POINT ==========
-keep public class org.example.store.HelloApplication {
    public static void main(java.lang.String[]);
}

# ========== JAVA FX ==========
-keep class * extends javafx.application.Application { *; }
-keep class javafx.** { *; }
-keepclassmembers class * {
    @javafx.fxml.FXML *;
}

# ========== SQLITE ==========
-keep class org.sqlite.** { *; }
-keepnames class org.sqlite.** { *; }
-keep class org.sqlite.core.** { *; }

# ========== CONTROLSFX ==========
-keep class org.controlsfx.** { *; }
-dontwarn org.controlsfx.**

# ========== PROJECT CLASSES ==========
-keep class org.example.store.** { *; }

# ========== ANNOTATIONS ==========
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature,SourceFile,LineNumberTable,StackMapTable
-keep interface * { *; }
