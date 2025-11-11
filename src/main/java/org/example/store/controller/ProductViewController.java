package org.example.store.controller;

import javafx.application.Platform;
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
    public void initialize() {
        // placeholder لو الجدول فاضي
        productTable.setPlaceholder(new Label("جارٍ تحميل المنتجات..."));

        // محاولة الاتصال بقاعدة البيانات
        try {
            conn = DB.connect();
        } catch (Exception ex) {
            ex.printStackTrace();
            AlertUtil.showError("خطأ", "فشل الاتصال بقاعدة البيانات: " + ex.getMessage());
            productTable.setPlaceholder(new Label("خطأ في الاتصال بقاعدة البيانات"));
            return;
        }

        // إعداد أعمدة الربط (اسم/سعر/مسار صورة) — تأكد إنها مرتبطة بالـ model
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        imageCol.setCellValueFactory(new PropertyValueFactory<>("imagePath"));

        // إزالة أية أعمدة مساعدة قد تكون أضيفت سابقاً لتفادي التكرار (بناء المشهد أكثر من مرة)
        productTable.getColumns().removeIf(c -> "الرقم".equals(c.getText()) || "صورة".equals(c.getText()));

        // عمود الرقم التسلسلي (الرقم)
        TableColumn<Product, Number> serialCol = new TableColumn<>("الرقم");
        serialCol.setCellValueFactory(col ->
                new ReadOnlyObjectWrapper<>(productTable.getItems().indexOf(col.getValue()) + 1)
        );
        serialCol.setPrefWidth(60);
        productTable.getColumns().add(0, serialCol);

        // عمود الصورة المصغرة — نسخة آمنة تستخدم getTableRow().getItem()
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
                    return;
                }

                // نحصل على المنتج من الـ row بدلاً من الاعتماد على getIndex()
                Product product = null;
                TableRow<?> row = getTableRow();
                if (row != null) {
                    Object rowItem = row.getItem();
                    if (rowItem instanceof Product) {
                        product = (Product) rowItem;
                    }
                }

                if (product == null) {
                    setGraphic(null);
                    return;
                }

                String imgPath = product.getImagePath();
                if (imgPath != null && !imgPath.trim().isEmpty()) {
                    try {
                        File file = new File(imgPath);
                        if (file.exists()) {
                            imageView.setImage(new Image(file.toURI().toString()));
                            setGraphic(imageView);
                        } else {
                            // ملف الصورة غير موجود — لا نعرض مصغر لكن نعرض صف المنتج
                            setGraphic(null);
                        }
                    } catch (Exception e) {
                        setGraphic(null);
                    }
                } else {
                    // لا توجد صورة — لا نعرض مصغر لكن نعرض الصف عادي
                    setGraphic(null);
                }
            }
        });
        thumbnailCol.setPrefWidth(60);
        productTable.getColumns().add(1, thumbnailCol);

        // عند اختيار صف نملأ الحقول
        productTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                nameField.setText(newSelection.getName());
                priceField.setText(String.valueOf(newSelection.getPrice()));
                selectedImagePath = newSelection.getImagePath();
                imagePathLabel.setText(selectedImagePath != null ? selectedImagePath : "لم يتم اختيار صورة");

                if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
                    File file = new File(selectedImagePath);
                    if (file.exists()) {
                        productImageView.setImage(new Image(file.toURI().toString()));
                    } else {
                        productImageView.setImage(null);
                    }
                } else {
                    productImageView.setImage(null);
                }
            } else {
                // تم إلغاء الاختيار — نفضي الحقول
                clearFields();
            }
        });

        // ضبط الـ items مبكراً لتفادي مشاكل الـ index أثناء حساب الرقم التسلسلي
        productTable.setItems(productList);

        // أخيراً نحمل المنتجات
        loadProducts();
    }

    private void loadProducts() {
        productList.clear();

        if (conn == null) {
            try {
                conn = DB.connect();
            } catch (Exception e) {
                e.printStackTrace();
                AlertUtil.showError("خطأ", "لا يمكن الاتصال بقاعدة البيانات: " + e.getMessage());
                productTable.setPlaceholder(new Label("خطأ في الاتصال بقاعدة البيانات"));
                return;
            }
        }

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

            // عرض الـ items (ضمن الـ UI thread)
            Platform.runLater(() -> {
                productTable.setItems(productList);
                productTable.refresh();
            });

            if (productList.isEmpty()) {
                productTable.setPlaceholder(new Label("لا توجد منتجات مفعّلة لعرضها."));
            } else {
                productTable.setPlaceholder(new Label("لا توجد منتجات"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("خطأ", "فشل في تحميل المنتجات: " + e.getMessage());
            productTable.setPlaceholder(new Label("فشل في تحميل المنتجات"));
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
                Path imagesDir = Paths.get("images");
                if (!Files.exists(imagesDir)) {
                    Files.createDirectories(imagesDir);
                }

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
