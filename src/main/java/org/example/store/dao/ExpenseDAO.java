package org.example.store.dao;



import org.example.store.model.Expense;
import org.example.store.utils.DB;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExpenseDAO {

    public static List<Expense> getAllExpenses() {
        List<Expense> list = new ArrayList<>();
        String sql = "SELECT * FROM expenses ORDER BY expense_date DESC";
        try (Connection conn = DB.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Expense(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("amount"),
                        LocalDate.parse(rs.getString("expense_date"))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static boolean addExpense(Expense e) {
        String sql = "INSERT INTO expenses(name, amount, expense_date) VALUES(?, ?, ?)";
        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, e.getName());
            ps.setDouble(2, e.getAmount());
            ps.setString(3, LocalDate.now().toString()); // <-- التاريخ اليوم تلقائي
            return ps.executeUpdate() > 0;

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }


    public static boolean updateExpense(Expense e) {
        String sql = "UPDATE expenses SET name=?, amount=?, expense_date=? WHERE id=?";
        try (Connection conn =DB.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, e.getName());
            ps.setDouble(2, e.getAmount());
            ps.setString(3, e.getExpenseDate().toString());
            ps.setInt(4, e.getId());
            return ps.executeUpdate() > 0;

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public static boolean deleteExpense(int id) {
        String sql = "DELETE FROM expenses WHERE id=?";
        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }
    public static List<Expense> getExpensesSince(LocalDate fromDate) {
        List<Expense> list = new ArrayList<>();
        String sql = "SELECT * FROM expenses WHERE expense_date >= ? ORDER BY expense_date DESC";
        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fromDate.toString());
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                Expense e = new Expense(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("amount"),
                        LocalDate.parse(rs.getString("expense_date"))
                );
                list.add(e);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static double getTotalAmount() {
        String sql = "SELECT SUM(amount) as total FROM expenses";
        try (Connection conn = DB.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) return rs.getDouble("total");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0.0;
    }
}
