package com.pos.app;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class POS extends JFrame {
    private final ProductDAO productDAO;
    private List<Product> inventory;
    private final List<Product> cart;
    private final DefaultTableModel inventoryTableModel;
    private final DefaultTableModel cartTableModel;

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
        inventory = new ArrayList<>(productDAO.loadInventoryData());
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
        loadInventory();

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
        inventoryButtonPanel.add(addProductButton);
        inventoryButtonPanel.add(editProductButton);
        inventoryButtonPanel.add(deleteProductButton);

        inventoryPanel.add(inventoryButtonPanel, BorderLayout.SOUTH);
        cartPanel.add(cartScrollPane, BorderLayout.CENTER);
        cartPanel.add(buttonPanel, BorderLayout.SOUTH);
        receiptPanel.add(receiptScrollPane, BorderLayout.CENTER);

        //Add an action listener
        deleteItemButton.addActionListener(e -> deleteCartItem());
        editItemButton.addActionListener(e -> editCart());
        checkoutButton.addActionListener(e -> {
            try {
                checkout();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });

        addProductButton.addActionListener(e -> addProduct());
        editProductButton.addActionListener(e -> editProduct());
        deleteProductButton.addActionListener(e -> deleteProduct());

        //Load inventory
        loadInventory();

        if(inventory.isEmpty()){
            loadInventory();
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
                    loadInventory();
                    updateInventoryTable();
                }else{
                    JOptionPane.showMessageDialog(this, "Failed to add product in the database.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
                JOptionPane.showMessageDialog(this, "Product successfully added!");
            }catch(NumberFormatException e){
                JOptionPane.showMessageDialog(this, "Invalid input. Please enter the valid value",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editProduct(){
        int selectedRow = inventoryTable.getSelectedRow();
        if(selectedRow == -1){
            JOptionPane.showMessageDialog(this, "Please select a product to edit.",
                    "Selection Error", JOptionPane.ERROR_MESSAGE);
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
                    loadInventory();
                    JOptionPane.showMessageDialog(this, "Product successfully updated.");
                }else{
                    JOptionPane.showMessageDialog(this, "Failed to update product.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid input. Please enter the valid values",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteProduct(){
        int selectedRow = inventoryTable.getSelectedRow();
        if(selectedRow == -1){
            JOptionPane.showMessageDialog(this, "Please select a product to delete.", "Selection Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        int option = JOptionPane.showConfirmDialog(this, "Are you sure, you want to delete the selected product?", "Delete Product",
                JOptionPane.YES_NO_OPTION);
        if(option == JOptionPane.YES_OPTION){
            //Get the product from the selected row
            Product selectedProduct = inventory.get(selectedRow);
            if(productDAO.deleteProduct(selectedProduct)){
                inventory.remove(selectedRow);
                updateInventoryTable();
                JOptionPane.showMessageDialog(this, "Product successfully deleted.");
            }else{
                JOptionPane.showMessageDialog(this, "Failed to delete the product.", "Deleting Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteCartItem(){
        int selectedRow = cartTable.getSelectedRow();
        if(selectedRow == -1){
            JOptionPane.showMessageDialog(this, "Please select a product to delete", "Selected Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        //Get the name of product to delete
        String nameToDelete = (String) cartTableModel.getValueAt(selectedRow, 0);

        //Find the corresponding product in the cart list and remove it.
        Product productToRemove = null;
        for(Product cartProduct : cart){
            if(cartProduct.getName().equals(nameToDelete)){
                productToRemove = cartProduct;
                break;
            }
        }
        if(productToRemove != null){
            cart.remove(productToRemove);
            cartTableModel.removeRow(selectedRow);
            JOptionPane.showMessageDialog(this, "Product deleted from the cart successfully",
                    "Delete Item", JOptionPane.INFORMATION_MESSAGE);
        }else {
            JOptionPane.showMessageDialog(this, "Product not found in the cart", "Deleted Item.",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editCart(){
        int selectedRow = cartTable.getSelectedRow();
        if(selectedRow == -1){
            JOptionPane.showMessageDialog(this, "Please select product to edit.", "Selection Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        //Get the current value from the cart table
        String name = (String) cartTableModel.getValueAt(selectedRow, 0);
        double price = (double) cartTableModel.getValueAt(selectedRow, 1);
        int currentQuantity = (int) cartTableModel.getValueAt(selectedRow, 2);

        //Prompt user for quantity
        String newQuantityStr = JOptionPane.showInputDialog(this, "Enter the quantity: ", currentQuantity);
        if(newQuantityStr == null || newQuantityStr.isEmpty()){
            return;
        }
        try{
            int newQuantity = Integer.parseInt(newQuantityStr);
            if(newQuantity <= 0){
                JOptionPane.showMessageDialog(this, "Quantity must be greater than zero.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return; //User cancelled or entering the empty input
            }
            //Update cart list with new quantity
            Product productToUpdate = null;
            for(Product cartProduct : cart){
                if(cartProduct.getName().equals(name)){
                    productToUpdate = cartProduct;
                    break;
                }
            }
            if(productToUpdate != null){
                productToUpdate.setStock(newQuantity);
            }
            //Update cart table with new quantity
            cartTableModel.setValueAt(newQuantity, selectedRow, 2);
        }catch (NumberFormatException e){
            JOptionPane.showMessageDialog(this, "Invalid input of quantity. Please enter a number: ",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addToCart(JButton button){
        int selectedRow = inventoryTable.getSelectedRow();
        if(selectedRow == -1){
            JOptionPane.showMessageDialog(this, "Please select a product to add to cart.", "Selection Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        //Get the product details from selected row
        String name = (String) inventoryTableModel.getValueAt(selectedRow, 0);
        double price = (double) inventoryTableModel.getValueAt(selectedRow, 1);
        int stocks = (int) inventoryTableModel.getValueAt(selectedRow, 2);

        //Prompt user for quantity
        String quantityStr = JOptionPane.showInputDialog(this, "Enter the quantity: ");
        if(quantityStr ==  null ||  quantityStr.isEmpty()){
            return; //User cancelled or entered an empty input
        }
        try{
            int quantity = Integer.parseInt(quantityStr);
            if(quantity <= 0){
                JOptionPane.showMessageDialog(this, "Quantity must be greater than zero.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(quantity > stocks){
                JOptionPane.showMessageDialog(this, "Insufficient stock available",
                        "Stock Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Product inventoryProduct = inventory.get(selectedRow);
            inventoryProduct.setStock(inventoryProduct.getStock() -  quantity);

            //Check if the product is already in the cart
            boolean productAlreadyExist = false;
            for(Product cartProduct : cart){
                if(cartProduct.getName().equals(name)){
                    cartProduct.setStock(cartProduct.getStock() + quantity);
                    productAlreadyExist = true;
                    break;
                }
            }
            //Add product into the cart
            if(!productAlreadyExist){
                cart.add(new Product(name, price, quantity));
            }
            //Refresh the UI
            updateCartTable();
            updateInventoryTable();
            JOptionPane.showMessageDialog(this, "Product successfully added to cart");
        }catch (NumberFormatException e){
            JOptionPane.showMessageDialog(this, "Invalid input of quantity. Please enter a number", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void checkout() throws SQLException{
        if(cart.isEmpty()){
            JOptionPane.showMessageDialog(this, "Cart is empty. Add products before checkout.",
                    "Checkout error.", JOptionPane.ERROR_MESSAGE);
            return;
        }
        //Calculate total and generate receipt
        double total = 0;
        StringBuilder receiptBuilder = new StringBuilder();
        DecimalFormat df = new DecimalFormat("#.##");

        receiptBuilder.append("Receipt:\n\n");
        receiptBuilder.append(String.format("%-20s %-10s %-10s\n", "Name", "Price", "Quantity"));
        System.out.println("---------------------------------------------------------\n");

        //Calculate total and inventory after confirming purchase
        for(Product cartProduct : cart){
            double lineTotal = cartProduct.getPrice() * cartProduct.getStock();
            receiptBuilder.append(String.format("%-20s %-10s %-10s\n",
                    cartProduct.getName(),
                    df.format(cartProduct.getPrice()),
                    cartProduct.getStock(),
                    df.format(lineTotal)));
            total += lineTotal;

            for(Product inventoryProduct : inventory){
                if(inventoryProduct.getName().equals(cartProduct.getName())){
                    inventoryProduct.setStock(inventoryProduct.getStock() - cartProduct.getStock());
                    productDAO.updateProduct(inventoryProduct);
                }
            }
        }
        receiptBuilder.append("-------------------------------------------------\n");
        receiptBuilder.append(String.format("Total: $%s\n", df.format(total)));

        //Display the receipt to text area
        receiptTextArea.setText(receiptBuilder.toString());
        JOptionPane.showMessageDialog(this, "Checkout successfully completed.",
                "Checkout", JOptionPane.INFORMATION_MESSAGE);

        //Update inventory table
        cart.clear();
        updateCartTable();
        loadInventory();
    }

    private void updateCartTable(){
        cartTableModel.setRowCount(0); //Clear the existing cart table

        //Populate cart data into table
        for(Product product : cart){
            Object[] rowData = {product.getName(), product.getPrice(), product.getStock()};
            cartTableModel.addRow(rowData);
        }

    }
    public void updateInventoryTable(){
        inventoryTableModel.setRowCount(0);
        for (Product product : inventory){
            Object[] rowData = {product.getName(), product.getPrice(), product.getStock()};
            inventoryTableModel.addRow(rowData);
        }
    }

    private void loadInventory(){
        inventory.clear(); //Clear the existing data
        try{
            //Fetch the inventory from the database using productDAO
            inventory = productDAO.loadInventoryData();

            //Update the inventory table with the latest data
            updateInventoryTable();
        }catch (Exception e){
            JOptionPane.showMessageDialog(this, "Error loading inventory data.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] arg){
        SwingUtilities.invokeLater(POS::new);
    }
}

