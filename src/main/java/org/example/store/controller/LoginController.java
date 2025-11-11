package org.example.store.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.example.store.model.Role;
import org.example.store.utils.Session;
import org.example.store.model.User;
import org.example.store.utils.DB;
import org.example.store.utils.SceneRouter;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    @FXML
    public void initialize() {
        // السماح بالضغط على Enter في أي من الحقلين
        passwordField.setOnAction(this::handleLogin);
        usernameField.setOnAction(this::handleLogin);

        // افراغ رسالة الخطأ في البداية
        errorLabel.setText("");
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        final String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        final String password = passwordField.getText() == null ? "" : passwordField.getText();

        // امسح رسالة قديمة
        setErrorMessage("");

        if (username.isEmpty() || password.isEmpty()) {
            setErrorMessage("يرجى إدخال اسم المستخدم وكلمة المرور.");
            return;
        }

        // تنفيذ محاولة تسجيل الدخول
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT password, username, role FROM user WHERE username = ?")) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("password");

                if (hashedPassword != null && BCrypt.checkpw(password, hashedPassword)) {
                    // تسجيل الدخول ناجح
                    User user = new User();
                    user.setUsername(rs.getString("username"));

                    // محاولة قراءة الدور بأمان
                    String roleStr = rs.getString("role");
                    try {
                        if (roleStr != null) {
                            user.setRole(Role.valueOf(roleStr.toUpperCase()));
                        }
                    } catch (IllegalArgumentException iae) {
                        // دور غير معروف في قاعدة البيانات -> نضع دور افتراضي (أو يمكنك رفض الدخول)
                        System.err.println("Unknown role for user '" + username + "': " + roleStr);
                    }

                    Session.setUser(user);

                    // الانتقال للشاشة الرئيسية - أي خطأ هنا يعرض رسالة مهندلة أدناه
                    try {
                        SceneRouter.switchTo("/org/example/store/unified-tabs.fxml");
                    } catch (Exception e) {
                        // لو حدث خطأ أثناء الانتقال للمشهد
                        System.err.println("Scene switch error: " + e.getMessage());
                        setErrorMessage("حدث خطأ أثناء التحويل للصفحة الرئيسية. حاول لاحقًا.");
                    }

                } else {
                    // باسورد غير صحيح
                    setErrorMessage("كلمة المرور غير صحيحة.");
                }
            } else {
                // مستخدم غير موجود
                setErrorMessage("اسم المستخدم غير موجود.");
            }

        } catch (SQLException e) {
            // أخطاء قاعدة البيانات -> رسالة مهندلة للمستخدم + لوج للمطور
            System.err.println("Database error: " + e.getMessage());
            setErrorMessage("تعذر الاتصال بقاعدة البيانات حالياً. حاول لاحقًا.");
        } catch (Exception e) {
            // أي خطأ غير متوقع
            System.err.println("Unexpected error: " + e.getMessage());
            setErrorMessage("حدث خطأ غير متوقع. حاول مرة أخرى.");
        }
    }

    /**
     * تحديث رسالة الخطأ بأمان على JavaFX Application Thread
     */
    private void setErrorMessage(String message) {
        if (Platform.isFxApplicationThread()) {
            errorLabel.setText(message);
        } else {
            Platform.runLater(() -> errorLabel.setText(message));
        }
    }

    public static String s() {
        return "S";
    }
}
