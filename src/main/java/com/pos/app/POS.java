package com.pos.app;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class POS extends JFrame {
    private ProductDAO productDAO;
    private List<Product> inventory;
    private List<Product> cart;
    private DefaultTableModel inventoryTableModel;
    private DefaultTableModel cartTableModel;

    private JTable inventoryTable;
    private JTable cartTable;
    private JTextArea receiptTextArea;

    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
            setText((value == null) ? "Add to cart" : value.toString());
            return  this;
        }
    }
    //Button Editor class
    class ButtonEditor extends DefaultCellEditor{
        protected JButton button;
        private String label;
        private boolean isPushed;

        public ButtonEditor(JCheckBox checkBox){
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e ->{
               fireEditingStopped();
               addToCart(button);
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column){
            label = (value == null) ? "Add to Cart" : value.toString();
            button.setText(label);
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue(){
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing(){
            isPushed = false;
            return super.stopCellEditing();
        }

        @Override
        public void fireEditingStopped(){
            super.fireEditingStopped();
        }
    }

    public POS(){
        productDAO = new ProductDAO();
        inventory = new ArrayList<>(productDAO.loadData());
        cart = new ArrayList<>();
        inventoryTableModel = new DefaultTableModel();
        cartTableModel = new DefaultTableModel(){
            @Override
            public boolean isCellEditable(int row, int column){
                return false;
            }
        };
        setTitle("POS-WITH-DB");
        setSize(1100, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initializeUI();

        //Load the inventory
        loadInventoryData();

        setVisible(true);
    }

    public void initializeUI(){
        //Create a main panel
        JPanel inventoryPanel = new JPanel(new BorderLayout());
        JPanel cartPanel = new JPanel(new BorderLayout());
        JPanel receiptPanel = new JPanel(new BorderLayout());

        //Set title for every panel
        inventoryPanel.setBorder(BorderFactory.createTitledBorder("Inventory"));
        cartPanel.setBorder(BorderFactory.createTitledBorder("Cart"));
        receiptPanel.setBorder(BorderFactory.createTitledBorder("Receipt"));

        //Create an inventory data
        String[] inventoryHeaders = {"Name", "Price", "Stock", "Action"};
        inventoryTableModel.setColumnIdentifiers(inventoryHeaders);
        inventoryTable = new JTable(inventoryTableModel){
            @Override
            public boolean isCellEditable(int row, int column){
                return column == 3; ///Enable editing only for the Action column
            }
        };
        inventoryTable.getTableHeader().setReorderingAllowed(false);//Disable column reordering
        JScrollPane inventoryScrollPane = new JScrollPane(inventoryTable);

        //Set custom renderer and editor for action column
        inventoryTable.getColumn("Action").setCellRenderer(new ButtonRenderer());
        inventoryTable.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox()));

        //Create a cart table
        String[] cartHeader = {"Name", "Price", "Quantity"};
        cartTableModel.setColumnIdentifiers(cartHeader);
        cartTable = new JTable (cartTableModel){
            @Override
            public boolean isCellEditable(int row, int column){
                return false;
            }
        };
        cartTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane cartScrollPane = new JScrollPane(cartTable);

        //Create buttons without border
        JButton checkoutButton = new JButton("Checkout");
        JButton editItemButton = new JButton("Edit Cart");
        JButton deleteItemButton = new JButton("Delete Item");
        JButton addProductButton = new JButton("Add Product");
        JButton editProductButton = new JButton("Edit Product");
        JButton deleteProductButton = new JButton("Delete Product");

        //Create receipt text area
        receiptTextArea = new JTextArea();
        receiptTextArea.setEditable(false);
        JScrollPane receiptScrollPane = new JScrollPane(receiptTextArea);

        //Set the preferred size
        inventoryScrollPane.setPreferredSize(new Dimension(400,400));
        cartScrollPane.setPreferredSize(new Dimension(300, 400));
        receiptScrollPane.setPreferredSize(new Dimension(250, 400));

        //Add panel to main frame
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(inventoryPanel, BorderLayout.WEST);
        getContentPane().add(cartPanel, BorderLayout.CENTER);
        getContentPane().add(receiptPanel, BorderLayout.EAST);

        //All components panel
        inventoryPanel.add(inventoryScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(checkoutButton);
        buttonPanel.add(editItemButton);
        buttonPanel.add(deleteItemButton);

        //Create a panel for buttons Flowlayout (Horizontal Layout)
        JPanel inventoryButtonPanel = new JPanel();
        inventoryButtonPanel.add(checkoutButton);
        inventoryButtonPanel.add(editItemButton);
        inventoryButtonPanel.add(deleteItemButton);

        inventoryPanel.add(inventoryButtonPanel, BorderLayout.SOUTH);
        cartPanel.add(cartScrollPane, BorderLayout.CENTER);
        cartPanel.add(buttonPanel, BorderLayout.SOUTH);
        receiptPanel.add(receiptScrollPane, BorderLayout.CENTER);

        //Add an action listener
        deleteItemButton.addActionListener(e -> deleCartItem());
        editItemButton.addActionListener(e -> editCart());
        checkoutButton.addActionListener(e -> {
            try{
                checkout();
            }catch(SQLException ex){
                throw new RuntimeException(ex);
            }
        });
        addProductButton.addActionListener(e -> addProduct());
        editProductButton.addActionListener(e -> editProduct());
        deleteProductButton.addActionListener(e -> deleteProduct());

        //Load inventory
        loadInventoryData();

        if(inventory.isEmpty()){
            loadInventoryData();
        }
    }

    private void addProduct() {
        JTextField nameField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField stocksField = new JTextField();

        Object[] inputFields = {"Name: ", nameField,
                "Price: ", priceField,
                "Stocks: ", stocksField};

        int option  = JOptionPane.showConfirmDialog(this, inputFields, "Add Product", JOptionPane.OK_CANCEL_OPTION);
        if(option == JOptionPane.OK_OPTION){
            try{
                String name = nameField.getText();
                double price = Double.parseDouble(priceField.getText());
                int stock = Integer.parseInt(stocksField.getText());

                Product product = new Product(name, price, stock);
                if(productDAO.addProduct(product)){
                    inventory.add(product);
                    loadInventoryData();
                    updateInventoryTable();
                }else{
                    JOptionPane.showMessageDialog(this, "Failed to add product in the database.", "Error", JOptionPane.ERROR_MESSAGE);
                }
                JOptionPane.showMessageDialog(this, "Product successfully added!");
            }catch(NumberFormatException e){
                JOptionPane.showMessageDialog(this, "Invalid input. Please enter the valid value", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editProduct(){
        int selectedRow = inventoryTable.getSelectedRow();
        if(selectedRow == -1){
            JOptionPane.showMessageDialog(this, "Please select a product to edit.", "Selection Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Product selectedproduct = inventory.get(selectedRow);
        JTextField nameField = new JTextField(selectedproduct.getName());
        JTextField priceField = new JTextField(String.valueOf(selectedproduct.getPrice()));
        JTextField stocksField = new JTextField(String.valueOf(selectedproduct.getStock()));
        Object[] inputFields =  {"Name: " , nameField, "Price: ", priceField, "Stocks: ", stocksField};

        int option = JOptionPane.showConfirmDialog(this, inputFields, "Edit Product", JOptionPane.OK_CANCEL_OPTION);
        if(option == JOptionPane.OK_OPTION){
            try{
                String newName = nameField.getText();
                double newPrice = Double.parseDouble(priceField.getText());
                int newStocks = Integer.parseInt(stocksField.getText());

                selectedproduct.setName(newName);
                selectedproduct.setPrice(newPrice);
                selectedproduct.setStock(newStocks);

                if(productDAO.updateProduct(selectedproduct)){
                    updateInventoryTable();
                    loadInventoryData();
                    JOptionPane.showMessageDialog(this, "Product successfully updated.");
                }else{
                    JOptionPane.showMessageDialog(this, "Failed to update product.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }catch (NumberFormatException e){
                JOptionPane.showMessageDialog(this, "Invalid input. Please enter the valid values", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void updateInventoryTable(){

        inventoryTableModel.setRowCount(0);
        for (Product product : inventory){
            Object[] rowData = {product.getName(), product.getPrice(), product.getStock()};
            inventoryTableModel.addRow(rowData);
        }
    }

    public void loadInventoryData(){
    }
}

