package org.example.store.model;

import javafx.beans.property.*;

import java.time.LocalDate;

public class Expense {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final DoubleProperty amount = new SimpleDoubleProperty();
    private final ObjectProperty<LocalDate> expenseDate = new SimpleObjectProperty<>();

    public Expense() {}

    public Expense(int id, String name, double amount, LocalDate expenseDate) {
        this.id.set(id);
        this.name.set(name);
        this.amount.set(amount);
        this.expenseDate.set(expenseDate);
    }

    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }
    public StringProperty nameProperty() { return name; }

    public double getAmount() { return amount.get(); }
    public void setAmount(double amount) { this.amount.set(amount); }
    public DoubleProperty amountProperty() { return amount; }

    public LocalDate getExpenseDate() { return expenseDate.get(); }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate.set(expenseDate); }
    public ObjectProperty<LocalDate> expenseDateProperty() { return expenseDate; }
}
