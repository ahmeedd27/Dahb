package org.example.store.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.store.model.Role;
import org.example.store.utils.Session;
import org.example.store.model.User;
import org.example.store.utils.AlertUtil;
import org.example.store.utils.DB;
import org.example.store.utils.SceneRouter;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

public class UserManagementController {

    @FXML
    private TableView<User> userTable;
    @FXML
    private TableColumn<User, String> usernameCol;
    @FXML
    private TableColumn<User, String> roleCol;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ComboBox<String> roleCombo;
    @FXML
    private Label statusLabel;

    private final ObservableList<User> userList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        roleCombo.getItems().addAll("ADMIN", "MANAGER", "CASHIER");
        usernameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getUsername()));
        roleCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getRole().toString()));
        loadUsers();
    }

    private void loadUsers() {
        userList.clear();
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM user")) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setRole(Role.valueOf(rs.getString("role")));
                userList.add(u);
            }
            userTable.setItems(userList);

        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("❌ فشل في تحميل المستخدمين");
        }
    }

    @FXML
    public void handleAddUser() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String role = roleCombo.getValue();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            statusLabel.setText("🟡 أدخل جميع البيانات");
            return;
        }

        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO user (username, password, role) VALUES (?, ?, ?)")) {

            String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
            stmt.setString(1, username);
            stmt.setString(2, hashed);
            stmt.setString(3, role);

            stmt.executeUpdate();
            statusLabel.setText("✅ تم الإضافة");
            loadUsers();
            clearFields();

        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("❌ اسم المستخدم موجود بالفعل");
        }
    }

    @FXML
    public void handleUpdateUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("🟡 اختر مستخدمًا للتعديل");
            return;
        }

        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String role = roleCombo.getValue();

        if (username.isEmpty() || role == null) {
            statusLabel.setText("🟡 أدخل اسم المستخدم والدور");
            return;
        }

        try (Connection conn = DB.connect()) {
            String sql = password.isEmpty() ?
                    "UPDATE user SET username = ?, role = ? WHERE id = ?" :
                    "UPDATE user SET username = ?, password = ?, role = ? WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                if (password.isEmpty()) {
                    stmt.setString(2, role);
                    stmt.setInt(3, selected.getId());
                } else {
                    String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
                    stmt.setString(2, hashed);
                    stmt.setString(3, role);
                    stmt.setInt(4, selected.getId());
                }

                stmt.executeUpdate();
                statusLabel.setText("✅ تم التحديث");
                loadUsers();
                clearFields();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("❌ خطأ في التحديث");
        }
    }

    @FXML
    public void handleDeleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("🟡 اختر مستخدمًا للحذف");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("تأكيد الحذف");
        alert.setHeaderText("هل أنت متأكد من حذف المستخدم؟");
        alert.setContentText("اسم المستخدم: " + selected.getUsername());

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM user WHERE id = ?")) {

                stmt.setInt(1, selected.getId());
                stmt.executeUpdate();
                statusLabel.setText("✅ تم الحذف");
                loadUsers();
                clearFields();

            } catch (SQLException e) {
                e.printStackTrace();
                statusLabel.setText("❌ خطأ في الحذف");
            }
        }
    }

    @FXML
    public void handleSearchUser() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            loadUsers();
            return;
        }

        userList.clear();
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM user WHERE username LIKE ?")) {

            stmt.setString(1, "%" + username + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setRole(Role.valueOf(rs.getString("role")));
                userList.add(u);
            }
            userTable.setItems(userList);

        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("❌ فشل في البحث");
        }
    }

    private void clearFields() {
        usernameField.clear();
        passwordField.clear();
        roleCombo.getSelectionModel().clearSelection();
    }

    public void goToMainView() {
        if (Session.getRole() != null) {
            SceneRouter.switchTo("/org/example/store/main-view.fxml");
        }

    }

    public void goToLogin() {
        SceneRouter.switchTo("/org/example/store/login.fxml");
    }
}
