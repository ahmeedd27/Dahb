package org.example.store.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.example.store.model.Product;
import org.example.store.utils.AlertUtil;
import org.example.store.utils.DB;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;

public class ProductViewController {
    public static String f() {
        return "AE";
    }

    private Connection conn;

    @FXML
    private TableView<Product> productTable;
    @FXML
    private TableColumn<Product, String> nameCol;
    @FXML
    private TableColumn<Product, Double> priceCol;
    @FXML
    private TableColumn<Product, String> imageCol;

    @FXML
    private TextField nameField;
    @FXML
    private TextField priceField;
    @FXML
    private Label imagePathLabel;
    @FXML
    private ImageView productImageView;

    @FXML
    private Button addButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button chooseImageButton;

    private ObservableList<Product> productList = FXCollections.observableArrayList();
    private String selectedImagePath = null;

    @FXML
    public void initialize() throws SQLException {
        conn = DB.connect();

        // إعداد الأعمدة
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        imageCol.setCellValueFactory(new PropertyValueFactory<>("imagePath"));

        // عمود الرقم التسلسلي
        TableColumn<Product, Number> serialCol = new TableColumn<>("الرقم");
        serialCol.setCellValueFactory(col ->
                new ReadOnlyObjectWrapper<>(productTable.getItems().indexOf(col.getValue()) + 1)
        );
        productTable.getColumns().add(0, serialCol);

        // عمود الصورة المصغرة
        TableColumn<Product, Void> thumbnailCol = new TableColumn<>("صورة");
        thumbnailCol.setCellFactory(col -> new TableCell<Product, Void>() {
            private final ImageView imageView = new ImageView();

            {
                imageView.setFitWidth(40);
                imageView.setFitHeight(40);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Product product = getTableView().getItems().get(getIndex());
                    if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
                        try {
                            File file = new File(product.getImagePath());
                            if (file.exists()) {
                                imageView.setImage(new Image(file.toURI().toString()));
                                setGraphic(imageView);
                            } else {
                                setGraphic(null);
                            }
                        } catch (Exception e) {
                            setGraphic(null);
                        }
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
        productTable.getColumns().add(1, thumbnailCol);

        // عند اختيار منتج
        productTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                nameField.setText(newSelection.getName());
                priceField.setText(String.valueOf(newSelection.getPrice()));
                selectedImagePath = newSelection.getImagePath();
                imagePathLabel.setText(selectedImagePath != null ? selectedImagePath : "لم يتم اختيار صورة");

                // عرض الصورة
                if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
                    File file = new File(selectedImagePath);
                    if (file.exists()) {
                        productImageView.setImage(new Image(file.toURI().toString()));
                    }
                } else {
                    productImageView.setImage(null);
                }
            }
        });

        loadProducts();
    }

    private void loadProducts() {
        productList.clear();
        String sql = "SELECT * FROM products WHERE active = 1 ORDER BY name";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                productList.add(new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getString("image_path"),
                        rs.getBoolean("active")
                ));
            }
            productTable.setItems(productList);
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("خطأ", "فشل في تحميل المنتجات");
        }
    }

    @FXML
    private void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("اختر صورة المنتج");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("صور", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(chooseImageButton.getScene().getWindow());
        if (selectedFile != null) {
            try {
                // إنشاء مجلد للصور إذا لم يكن موجوداً
                Path imagesDir = Paths.get("images");
                if (!Files.exists(imagesDir)) {
                    Files.createDirectories(imagesDir);
                }

                // نسخ الصورة لمجلد الصور
                String fileName = System.currentTimeMillis() + "_" + selectedFile.getName();
                Path destination = imagesDir.resolve(fileName);
                Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

                selectedImagePath = destination.toString();
                imagePathLabel.setText(selectedImagePath);
                productImageView.setImage(new Image(destination.toUri().toString()));
            } catch (Exception e) {
                e.printStackTrace();
                AlertUtil.showError("خطأ", "فشل في حفظ الصورة");
            }
        }
    }

    @FXML
    public void addProduct() {
        if (!validateInput()) return;

        String sql = "INSERT INTO products (name, price, image_path, active) VALUES (?, ?, ?, 1)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nameField.getText().trim());
            pstmt.setDouble(2, Double.parseDouble(priceField.getText().trim()));
            pstmt.setString(3, selectedImagePath);

            pstmt.executeUpdate();
            AlertUtil.showSuccess("نجح", "تمت إضافة المنتج بنجاح");
            loadProducts();
            clearFields();
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("خطأ", "فشل في إضافة المنتج");
        }
    }

    @FXML
    public void updateProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("تنبيه", "الرجاء اختيار منتج أولاً");
            return;
        }

        if (!validateInput()) return;

        String sql = "UPDATE products SET name=?, price=?, image_path=? WHERE id=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nameField.getText().trim());
            pstmt.setDouble(2, Double.parseDouble(priceField.getText().trim()));
            pstmt.setString(3, selectedImagePath);
            pstmt.setInt(4, selected.getId());

            pstmt.executeUpdate();
            AlertUtil.showSuccess("نجح", "تم تعديل المنتج بنجاح");
            loadProducts();
            clearFields();
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("خطأ", "فشل في تعديل المنتج");
        }
    }

    @FXML
    public void deleteProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("تنبيه", "الرجاء اختيار منتج أولاً");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("تأكيد الحذف");
        confirmAlert.setHeaderText("هل أنت متأكد من حذف هذا المنتج؟");
        confirmAlert.setContentText(selected.getName());

        if (confirmAlert.showAndWait().get() == ButtonType.OK) {
            // حذف ناعم - نخلي active = 0
            String sql = "UPDATE products SET active = 0 WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, selected.getId());
                pstmt.executeUpdate();
                AlertUtil.showSuccess("نجح", "تم حذف المنتج بنجاح");
                loadProducts();
                clearFields();
            } catch (SQLException e) {
                e.printStackTrace();
                AlertUtil.showError("خطأ", "فشل في حذف المنتج");
            }
        }
    }

    private boolean validateInput() {
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            AlertUtil.showWarning("خطأ", "الرجاء إدخال اسم المنتج");
            return false;
        }

        try {
            double price = Double.parseDouble(priceField.getText().trim());
            if (price <= 0) {
                AlertUtil.showWarning("خطأ", "السعر يجب أن يكون أكبر من صفر");
                return false;
            }
        } catch (NumberFormatException e) {
            AlertUtil.showWarning("خطأ", "الرجاء إدخال سعر صحيح");
            return false;
        }

        return true;
    }

    private void clearFields() {
        nameField.clear();
        priceField.clear();
        selectedImagePath = null;
        imagePathLabel.setText("لم يتم اختيار صورة");
        productImageView.setImage(null);
        productTable.getSelectionModel().clearSelection();
    }

    @FXML
    public void handleRefresh() {
        loadProducts();
    }
}