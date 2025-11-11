package org.example.store.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.store.dao.ExpenseDAO;
import org.example.store.model.Expense;
import org.example.store.utils.SceneRouter;


import java.time.LocalDate;

public class ExpensesController {
    @FXML
    private ComboBox<String> filterComboBox;
    @FXML
    private TextField nameField, amountField;
    @FXML
    private TableView<Expense> expenseTable;
    @FXML
    private TableColumn<Expense, Number> idColumn;
    @FXML
    private TableColumn<Expense, String> nameColumn;
    @FXML
    private TableColumn<Expense, Number> amountColumn;
    @FXML
    private TableColumn<Expense, LocalDate> dateColumn;
    @FXML
    private Label totalLabel;

    private ObservableList<Expense> expenseList;

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(data -> data.getValue().idProperty());
        nameColumn.setCellValueFactory(data -> data.getValue().nameProperty());
        amountColumn.setCellValueFactory(data -> data.getValue().amountProperty());
        dateColumn.setCellValueFactory(data -> data.getValue().expenseDateProperty());

        loadExpenses();

        expenseTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                nameField.setText(newSel.getName());
                amountField.setText(String.valueOf(newSel.getAmount()));
            }
        });
    }

    private void loadExpenses() {
        expenseList = FXCollections.observableArrayList(ExpenseDAO.getAllExpenses());
        expenseTable.setItems(expenseList);
        updateTotal();
    }

    private void updateTotal() {
        double sum = expenseTable.getItems().stream()
                .mapToDouble(Expense::getAmount)
                .sum();
        totalLabel.setText("المجموع: " + sum);
    }

    @FXML
    private void addExpense() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showAlert("ادخل اسم النثريه");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert("ادخل قيمة صحيحة");
            return;
        }

        // التاريخ يتم تلقائيًا
        Expense e = new Expense(0, name, amount, LocalDate.now());
        if (ExpenseDAO.addExpense(e)) {
            loadExpenses();
            clearFields();
        }
    }


    @FXML
    private void updateExpense() {
        Expense selected = expenseTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("اختر النثريه للتعديل");
            return;
        }

        selected.setName(nameField.getText().trim());
        try {
            selected.setAmount(Double.parseDouble(amountField.getText().trim()));
        } catch (NumberFormatException e) {
            showAlert("ادخل قيمة صحيحة");
            return;
        }

        if (ExpenseDAO.updateExpense(selected)) {
            loadExpenses();
            clearFields();
        }
    }


    @FXML
    private void deleteExpense() {
        Expense selected = expenseTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("اختر النثريه للحذف");
            return;
        }

        if (ExpenseDAO.deleteExpense(selected.getId())) {
            loadExpenses();
            clearFields();
        }
    }

    @FXML
    private void applyFilter() {
        LocalDate fromDate = null;
        String choice = filterComboBox.getValue();
        if (choice == null || choice.equals("الكل")) {
            loadExpenses(); // بدون فلترة
            return;
        }

        switch (choice) {
            case "اليوم":
                fromDate = LocalDate.now();
                break;
            case "آخر 7 أيام":
                fromDate = LocalDate.now().minusDays(7);
                break;
            case "آخر 30 يوم":
                fromDate = LocalDate.now().minusDays(30);
                break;
        }

        if (fromDate != null) {
            expenseList = FXCollections.observableArrayList(ExpenseDAO.getExpensesSince(fromDate));
            expenseTable.setItems(expenseList);
            updateTotal();
        }
    }

    private void clearFields() {
        nameField.clear();
        amountField.clear();
        expenseTable.getSelectionModel().clearSelection();
    }


    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void goHome() {
        SceneRouter.switchTo("/org/example/store/main-view.fxml");
    }
}
