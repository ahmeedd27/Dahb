package org.example.store.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.example.store.model.Product;
import org.example.store.utils.AlertUtil;
import org.example.store.utils.CartItemDTO;
import org.example.store.utils.DB;
import org.example.store.utils.InvoicePrinter;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PurchaseViewController {

    @FXML
    private FlowPane productsPane;

    @FXML
    private TableView<CartItemDTO> cartTable;
    @FXML
    private TableColumn<CartItemDTO, String> nameCol;
    @FXML
    private TableColumn<CartItemDTO, Double> priceCol;
    @FXML
    private TableColumn<CartItemDTO, Integer> quantityCol;
    @FXML
    private TableColumn<CartItemDTO, Double> subtotalCol;
    @FXML
    private TableColumn<CartItemDTO, Void> actionsCol;

    @FXML
    private Label totalLabel;

    @FXML
    private Button checkoutButton;

    // زر الطباعة الجديد
    @FXML
    private Button printButton;

    private ObservableList<CartItemDTO> cartItems = FXCollections.observableArrayList();
    private List<Product> allProducts = new ArrayList<>();

    @FXML
    public void initialize() {
        // إعداد أعمدة الجدول
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        subtotalCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getSubtotal()).asObject());

        // عمود الكمية مع أزرار + و -
        quantityCol.setCellFactory(col -> new TableCell<CartItemDTO, Integer>() {
            private final Button minusBtn = new Button("-");
            private final Label quantityLabel = new Label();
            private final Button plusBtn = new Button("+");
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(5, minusBtn, quantityLabel, plusBtn);

            {
                box.setAlignment(Pos.CENTER);
                minusBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
                plusBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
                quantityLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-min-width: 30px; -fx-alignment: center;");

                minusBtn.setOnAction(e -> {
                    CartItemDTO item = getTableView().getItems().get(getIndex());
                    item.decrementQuantity();
                    updateTotal();
                    getTableView().refresh();
                });

                plusBtn.setOnAction(e -> {
                    CartItemDTO item = getTableView().getItems().get(getIndex());
                    item.incrementQuantity();
                    updateTotal();
                    getTableView().refresh();
                });
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    CartItemDTO cartItem = getTableView().getItems().get(getIndex());
                    quantityLabel.setText(String.valueOf(cartItem.getQuantity()));
                    setGraphic(box);
                }
            }
        });

        // عمود الإجراءات (حذف)
        actionsCol.setCellFactory(col -> new TableCell<CartItemDTO, Void>() {
            private final Button deleteBtn = new Button("🗑");

            {
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 16px;");
                deleteBtn.setOnAction(e -> {
                    CartItemDTO item = getTableView().getItems().get(getIndex());
                    cartItems.remove(item);
                    updateTotal();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });

        cartTable.setItems(cartItems);

        // تحميل المنتجات
        loadProducts();
        updateTotal();
    }

    private void loadProducts() {
        allProducts.clear();
        productsPane.getChildren().clear();

        String sql = "SELECT * FROM products WHERE active = 1 ORDER BY name";
        try (Connection conn = DB.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Product product = new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getString("image_path"),
                        rs.getBoolean("active")
                );
                allProducts.add(product);

                // إنشاء كارت المنتج
                VBox productCard = createProductCard(product);
                productsPane.getChildren().add(productCard);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("خطأ", "فشل في تحميل المنتجات");
        }
    }

    private VBox createProductCard(Product product) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 10; " +
                "-fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setPrefWidth(150);
        card.setPrefHeight(200);

        // الصورة
        ImageView imageView = new ImageView();
        imageView.setFitWidth(100);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);

        if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
            File imageFile = new File(product.getImagePath());
            if (imageFile.exists()) {
                imageView.setImage(new Image(imageFile.toURI().toString()));
            } else {
                // صورة افتراضية
                imageView.setStyle("-fx-background-color: #f0f0f0;");
            }
        } else {
            imageView.setStyle("-fx-background-color: #f0f0f0;");
        }

        // الاسم
        Label nameLabel = new Label(product.getName());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-alignment: center;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(130);

        // السعر
        Label priceLabel = new Label(String.format("%.2f جنيه", product.getPrice()));
        priceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4CAF50; -fx-font-weight: bold;");

        card.getChildren().addAll(imageView, nameLabel, priceLabel);

        // عند الضغط على الكارت
        card.setOnMouseClicked(e -> addToCart(product));
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-cursor: hand; -fx-border-color: #4CAF50; -fx-border-width: 2;"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-cursor: hand; -fx-border-color: #4CAF50; -fx-border-width: 2;", "")));

        return card;
    }

    private void addToCart(Product product) {
        // البحث إذا كان المنتج موجود في العربة
        CartItemDTO existingItem = cartItems.stream()
                .filter(item -> item.getProductId() == product.getId())
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // زيادة الكمية
            existingItem.incrementQuantity();
        } else {
            // إضافة منتج جديد
            CartItemDTO newItem = new CartItemDTO(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    product.getImagePath()
            );
            cartItems.add(newItem);
        }

        updateTotal();
        cartTable.refresh();
    }

    private void updateTotal() {
        double total = cartItems.stream()
                .mapToDouble(CartItemDTO::getSubtotal)
                .sum();
        totalLabel.setText(String.format("الإجمالي: %.2f جنيه", total));
    }

    @FXML
    public void handleCheckout() {
        if (cartItems.isEmpty()) {
            AlertUtil.showWarning("تنبيه", "العربة فارغة! أضف منتجات أولاً.");
            return;
        }

        try (Connection conn = DB.connect()) {
            conn.setAutoCommit(false);

            double total = cartItems.stream().mapToDouble(CartItemDTO::getSubtotal).sum();

            // إدراج الفاتورة
            String sqlSale = "INSERT INTO sales (sale_date, total) VALUES (?, ?)";
            PreparedStatement psSale = conn.prepareStatement(sqlSale, Statement.RETURN_GENERATED_KEYS);
            psSale.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            psSale.setDouble(2, total);
            psSale.executeUpdate();

            ResultSet keys = psSale.getGeneratedKeys();
            int saleId = -1;
            if (keys.next()) {
                saleId = keys.getInt(1);
            }

            // إدراج عناصر الفاتورة
            String sqlItem = "INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, total_amount) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement psItem = conn.prepareStatement(sqlItem);

            for (CartItemDTO item : cartItems) {
                psItem.setInt(1, saleId);
                psItem.setInt(2, item.getProductId());
                psItem.setInt(3, item.getQuantity());
                psItem.setDouble(4, item.getUnitPrice());
                psItem.setDouble(5, item.getSubtotal());
                psItem.addBatch();
            }

            psItem.executeBatch();
            conn.commit();

            AlertUtil.showSuccess("نجح", "✅ تمت عملية البيع بنجاح!\nرقم الفاتورة: " + saleId);

            // تفريغ العربة
            cartItems.clear();
            updateTotal();

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("خطأ", "فشل في إتمام عملية البيع");
        }
    }

    @FXML
    public void handlePrint() {
        if (cartItems.isEmpty()) {
            AlertUtil.showWarning("تنبيه", "العربة فارغة! أضف منتجات أولاً.");
            return;
        }

        // نقوم بفتح اتصال ونحفظ الفاتورة فعلياً في قاعدة البيانات ثم نطبع باستخدام الرقم الحقيقي
        try (Connection conn = DB.connect()) {
            conn.setAutoCommit(false);

            double total = cartItems.stream().mapToDouble(CartItemDTO::getSubtotal).sum();

            // إدراج الفاتورة في جدول sales والحصول على الـ generated key (الرقم الحقيقي)
            String sqlSale = "INSERT INTO sales (sale_date, total) VALUES (?, ?)";
            PreparedStatement psSale = conn.prepareStatement(sqlSale, Statement.RETURN_GENERATED_KEYS);
            psSale.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            psSale.setDouble(2, total);
            psSale.executeUpdate();

            ResultSet keys = psSale.getGeneratedKeys();
            int saleId = -1;
            if (keys.next()) {
                saleId = keys.getInt(1);
            } else {
                throw new SQLException("فشل الحصول على رقم الفاتورة من قاعدة البيانات");
            }

            // إدراج عناصر الفاتورة في sale_items
            String sqlItem = "INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, total_amount) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement psItem = conn.prepareStatement(sqlItem);

            for (CartItemDTO item : cartItems) {
                psItem.setInt(1, saleId);
                psItem.setInt(2, item.getProductId());
                psItem.setInt(3, item.getQuantity());
                psItem.setDouble(4, item.getUnitPrice());
                psItem.setDouble(5, item.getSubtotal());
                psItem.addBatch();
            }
            psItem.executeBatch();

            conn.commit();

            // الآن استدعاء الطباعة باستخدام رقم الفاتورة الحقيقي
            List<CartItemDTO> itemsToPrint = new ArrayList<>(cartItems); // نمرر نسخة
            InvoicePrinter.printCart(saleId, itemsToPrint);

            AlertUtil.showSuccess("طباعة", "تم حفظ الفاتورة وطباعتها بنجاح. رقم الفاتورة: " + saleId);

            // تفريغ العربة بعد الحفظ والطباعة (مثل checkout)
            cartItems.clear();
            updateTotal();

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("خطأ", "فشل في حفظ أو طباعة الفاتورة: " + e.getMessage());
        }
    }

    @FXML
    public void handleClearCart() {
        if (!cartItems.isEmpty()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("تأكيد");
            confirm.setHeaderText("هل أنت متأكد من تفريغ العربة؟");
            if (confirm.showAndWait().get() == ButtonType.OK) {
                cartItems.clear();
                updateTotal();
            }
        }
    }

    @FXML
    public void handleRefresh() {
        loadProducts();
    }
}
