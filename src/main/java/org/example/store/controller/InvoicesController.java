package org.example.store.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.store.utils.DB;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class InvoicesController {

    @FXML
    private ComboBox<String> rangeCombo;
    @FXML
    private DatePicker datePicker;
    @FXML
    private TableView<SaleRow> saleTable;
    @FXML
    private TableColumn<SaleRow, Integer> saleIdCol;
    @FXML
    private TableColumn<SaleRow, String> saleDateCol;
    @FXML
    private TableColumn<SaleRow, Double> saleTotalCol;
    @FXML
    private Label pageTotalLabel;

    private ObservableList<SaleRow> sales = FXCollections.observableArrayList();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"); // عرض: يوم-شهر-سنة ساعة:دقيقة

    @FXML
    public void initialize() {
        // نستخدم PropertyValueFactory للبيانات، ثم نجعل الخلايا متوسّطة
        saleIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        saleDateCol.setCellValueFactory(new PropertyValueFactory<>("dateTime"));
        saleTotalCol.setCellValueFactory(new PropertyValueFactory<>("total"));

        // center alignment للخلايا
        saleIdCol.setCellFactory(col -> centeredIntegerCell());
        saleDateCol.setCellFactory(col -> centeredStringCell());
        saleTotalCol.setCellFactory(col -> centeredDoubleCell());

        saleTable.setItems(sales);

        // دبل كليك لفتح تفاصيل الفاتورة
        saleTable.setRowFactory(tv -> {
            TableRow<SaleRow> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    SaleRow rowData = row.getItem();
                    showSaleDetails(rowData.getId());
                }
            });
            return row;
        });

        loadAllSales();
    }

    private TableCell<SaleRow, Integer> centeredIntegerCell() {
        return new TableCell<SaleRow, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(item));
                }
                setAlignment(Pos.CENTER);
            }
        };
    }

    private TableCell<SaleRow, String> centeredStringCell() {
        return new TableCell<SaleRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty ? null : item));
                setAlignment(Pos.CENTER);
            }
        };
    }

    private TableCell<SaleRow, Double> centeredDoubleCell() {
        return new TableCell<SaleRow, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                }
                setAlignment(Pos.CENTER);
            }
        };
    }

    // --- تحميل كل الفواتير بترتيب رقم الفاتورة تصاعدي (id ASC) ---
    private void loadAllSales() {
        sales.clear();
        String sql = "SELECT id, sale_date, total FROM sales ORDER BY id ASC"; // <<-- هنا الترتيب حسب id
        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                Timestamp ts = rs.getTimestamp("sale_date");
                double total = rs.getDouble("total");
                String dt = "";
                if (ts != null) {
                    LocalDateTime ldt = ts.toLocalDateTime();
                    dt = ldt.format(dtf); // صيغة "dd-MM-yyyy HH:mm" (بدون ثواني)
                }
                sales.add(new SaleRow(id, dt, total));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("خطأ", "فشل في تحميل الفواتير: " + ex.getMessage());
        }
        recalcPageTotal();
    }

    private void recalcPageTotal() {
        double sum = sales.stream().mapToDouble(SaleRow::getTotal).sum();
        pageTotalLabel.setText(String.format("%.2f", sum));
    }

    @FXML
    private void applyRangeFilter() {
        String choice = rangeCombo.getValue();
        if (choice == null) {
            showAlert("تنبيه", "اختر المدى أولاً");
            return;
        }

        LocalDateTime from = null;
        LocalDateTime to = LocalDateTime.now().plusSeconds(1);

        switch (choice) {
            case "اليوم":
                from = LocalDate.now().atStartOfDay();
                break;
            case "الأسبوع":
                from = LocalDate.now().minusDays(6).atStartOfDay();
                break;
            case "الشهر":
                from = LocalDate.now().minusDays(29).atStartOfDay();
                break;
            case "الكل":
                from = null;
                break;
        }

        loadSalesBetween(from, to);
        datePicker.setValue(null);
    }

    @FXML
    private void applyDateFilter() {
        LocalDate date = datePicker.getValue();
        if (date == null) {
            showAlert("تنبيه", "اختر تاريخًا أولاً");
            return;
        }
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();
        loadSalesBetween(from, to);
        rangeCombo.setValue(null);
    }

    @FXML
    private void applyRefresh() {
        rangeCombo.setValue(null);
        datePicker.setValue(null);
        loadAllSales();
    }

    private void loadSalesBetween(LocalDateTime from, LocalDateTime to) {
        sales.clear();
        String sql;
        if (from == null) {
            sql = "SELECT id, sale_date, total FROM sales ORDER BY id ASC";
        } else {
            sql = "SELECT id, sale_date, total FROM sales WHERE sale_date >= ? AND sale_date < ? ORDER BY id ASC";
        }

        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (from != null) {
                ps.setTimestamp(1, Timestamp.valueOf(from));
                ps.setTimestamp(2, Timestamp.valueOf(to));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    Timestamp ts = rs.getTimestamp("sale_date");
                    double total = rs.getDouble("total");
                    String dt = "";
                    if (ts != null) {
                        dt = ts.toLocalDateTime().format(dtf);
                    }
                    sales.add(new SaleRow(id, dt, total));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("خطأ", "فشل في تحميل الفواتير: " + ex.getMessage());
        }
        recalcPageTotal();
    }

    private void showSaleDetails(int saleId) {
        TableView<SaleItemRow> itemsTable = new TableView<>();
        TableColumn<SaleItemRow, String> nameCol = new TableColumn<>("اسم المنتج");
        TableColumn<SaleItemRow, Integer> qtyCol = new TableColumn<>("الكمية");
        TableColumn<SaleItemRow, Double> priceCol = new TableColumn<>("سعر الوحدة");
        TableColumn<SaleItemRow, Double> totalCol = new TableColumn<>("المجموع");

        nameCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("productName"));
        qtyCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantity"));
        priceCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("unitPrice"));
        totalCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("totalAmount"));

        // محاذاة الخلايا (مركزي) + اجعل الجدول يستخدم اتجاه الواجهة من اليمين لليسار
        nameCol.setStyle("-fx-alignment: CENTER;");
        qtyCol.setStyle("-fx-alignment: CENTER;");
        priceCol.setStyle("-fx-alignment: CENTER;");
        totalCol.setStyle("-fx-alignment: CENTER;");
        itemsTable.getColumns().addAll(nameCol, qtyCol, priceCol, totalCol);
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        itemsTable.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        ObservableList<SaleItemRow> items = FXCollections.observableArrayList();
        String sql = "SELECT si.quantity, si.unit_price, si.total_amount, p.name " +
                "FROM sale_items si JOIN products p ON si.product_id = p.id " +
                "WHERE si.sale_id = ?";

        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String pname = rs.getString("name");
                    int q = rs.getInt("quantity");
                    double up = rs.getDouble("unit_price");
                    double ta = rs.getDouble("total_amount");
                    items.add(new SaleItemRow(pname, q, up, ta));
                }
            }

            // جلب إجمالي الفاتورة من جدول sales (أفضل من جمع العناصر لأن الحقل متاح)
            double saleTotal = 0.0;
            try (PreparedStatement psTotal = conn.prepareStatement("SELECT total FROM sales WHERE id = ?")) {
                psTotal.setInt(1, saleId);
                try (ResultSet rs2 = psTotal.executeQuery()) {
                    if (rs2.next()) saleTotal = rs2.getDouble("total");
                }
            }

            itemsTable.setItems(items);

            Label title = new Label("تفاصيل الفاتورة: " + saleId);
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            Label totalLabel = new Label(String.format("إجمالي الفاتورة: %.2f", saleTotal));
            totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            // نجعل اللابل على اليمين داخل الـ VBox
            totalLabel.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            totalLabel.setMaxWidth(Double.MAX_VALUE);

            VBox root = new VBox(10, title, itemsTable, totalLabel);
            root.setPadding(new javafx.geometry.Insets(10));
            // اجعل النافذة تتصرف من اليمين لليسار
            root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

            Stage dlg = new Stage();
            dlg.initModality(Modality.APPLICATION_MODAL);
            dlg.setTitle("تفاصيل الفاتورة " + saleId);
            dlg.setScene(new Scene(root, 600, 400));
            dlg.showAndWait();

        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("خطأ", "فشل في تحميل تفاصيل الفاتورة: " + ex.getMessage());
        }
    }

    @FXML
    private void openStatistics() {
        LocalDate date = datePicker.getValue();
        if (date == null) date = LocalDate.now();

        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();

        String sql = "SELECT p.name, SUM(si.quantity) AS qty " +
                "FROM sale_items si " +
                "JOIN products p ON si.product_id = p.id " +
                "JOIN sales s ON si.sale_id = s.id " +
                "WHERE s.sale_date >= ? AND s.sale_date < ? " +
                "GROUP BY p.id, p.name " +
                "ORDER BY qty DESC";

        ObservableList<StatRow> stats = FXCollections.observableArrayList();
        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    int qty = rs.getInt("qty");
                    stats.add(new StatRow(name, qty));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("خطأ", "فشل في حساب الإحصائيات: " + ex.getMessage());
            return;
        }

        TableView<StatRow> table = new TableView<>(stats);
        TableColumn<StatRow, String> prodCol = new TableColumn<>("الصنف");
        TableColumn<StatRow, Integer> cntCol = new TableColumn<>("الكمية المباعة");

        prodCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("productName"));
        cntCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("count"));

        // محاذاة الخلايا ومظهر RTL
        prodCol.setStyle("-fx-alignment: CENTER;");
        cntCol.setStyle("-fx-alignment: CENTER;");
        table.getColumns().addAll(prodCol, cntCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("إحصائيات المبيعات — " + date.toString());

        Label title = new Label("الإجمالي حسب الصنف للتاريخ: " + date.toString());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        VBox root = new VBox(10, title, table);
        root.setPadding(new javafx.geometry.Insets(10));
        root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        dlg.setScene(new Scene(root, 500, 400));
        dlg.showAndWait();
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // DTO classes
    public static class SaleRow {
        private final int id;
        private final String dateTime;
        private final double total;

        public SaleRow(int id, String dateTime, double total) {
            this.id = id;
            this.dateTime = dateTime;
            this.total = total;
        }

        public int getId() {
            return id;
        }

        public String getDateTime() {
            return dateTime;
        }

        public double getTotal() {
            return total;
        }
    }

    public static class SaleItemRow {
        private final String productName;
        private final int quantity;
        private final double unitPrice;
        private final double totalAmount;

        public SaleItemRow(String productName, int quantity, double unitPrice, double totalAmount) {
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalAmount = totalAmount;
        }

        public String getProductName() {
            return productName;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getUnitPrice() {
            return unitPrice;
        }

        public double getTotalAmount() {
            return totalAmount;
        }
    }

    public static class StatRow {
        private final String productName;
        private final int count;

        public StatRow(String productName, int count) {
            this.productName = productName;
            this.count = count;
        }

        public String getProductName() {
            return productName;
        }

        public int getCount() {
            return count;
        }
    }
}
