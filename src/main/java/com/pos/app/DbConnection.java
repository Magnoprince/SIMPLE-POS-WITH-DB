package com.pos.app;


import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnection extends Component {
    private static final String URL = "jdbc:mysql://localhost:3306/pos_db";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "@prince";
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    public Connection connection;

    public void connectToDatabase(){
        try{
            Class.forName(DRIVER);
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        }catch (ClassNotFoundException | SQLException e){
            JOptionPane.showMessageDialog(this, "Database connection failed:" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }

    }
}
