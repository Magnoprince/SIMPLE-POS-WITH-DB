package com.pos.app;

import javax.swing.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO extends DbConnection{
    List<Product> products = new ArrayList<>();

    public boolean addProduct(Product product){
        String query = "INSERT INTO inventory(product_name, price, stock) VALUES (?, ?, ?)";
        try{
            connectToDatabase();
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, product.getName());
            preparedStatement.setDouble(2, product.getPrice());
            preparedStatement.setInt(3, product.getStock());
            preparedStatement.executeUpdate();
            JOptionPane.showMessageDialog(this, "Product successfully added!", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        }catch (SQLException e){
            JOptionPane.showMessageDialog(this, "Error adding product!", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    public void  updateProduct(Product product){
        String query = "UPDATE inventory SET  price = ?, stock = ? WHERE product_name";
        try{
            connectToDatabase();
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, product.getName());
            preparedStatement.setDouble(2, product.getPrice());
            preparedStatement.setInt(3, product.getStock());
            preparedStatement.executeUpdate();
        }catch (SQLException e){
            JOptionPane.showMessageDialog(this, "Error updating product!", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void deleteProduct(Product product){
        String query =  "DELETE FROM inventory WHERE product_name = ?";
        try {
            connectToDatabase();
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, product.getName());
            preparedStatement.executeUpdate();
        }catch (SQLException e){
            JOptionPane.showMessageDialog(this, "Error deleting product!", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public List<Product> loadData() {
        String query = "SELECT * FROM inventory";
        try {
            connectToDatabase();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                String product = resultSet.getString("product_name");
                double price = resultSet.getDouble("price");
                int stock = resultSet.getInt("stock");
                products.add(new Product(product, price, stock));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading data!", "Error",
                    JOptionPane.ERROR_MESSAGE);

        }

        return products;

    }
}
