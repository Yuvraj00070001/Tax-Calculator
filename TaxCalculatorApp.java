import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;

// Database Manager Class
class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_NAME = "TaxCalculationDB";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // Change if you have a password

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found!");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL + DB_NAME, USER, PASSWORD);
    }

    public static Connection getRootConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void initializeDatabase() {
        try (Connection conn = getRootConnection()) {
            // Create database if not exists
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            System.out.println("Database created/verified");

            // Use the database
            stmt.executeUpdate("USE " + DB_NAME);

            // Create Users table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS Users (" +
                    "Username VARCHAR(50) PRIMARY KEY, " +
                    "Password VARCHAR(50) NOT NULL)");

            // Create TaxCalculations table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS TaxCalculations (" +
                    "ID INT AUTO_INCREMENT PRIMARY KEY, " +
                    "Username VARCHAR(50), " +
                    "Income DOUBLE, " +
                    "Deductions DOUBLE, " +
                    "TaxAmount DOUBLE, " +
                    "Status VARCHAR(20), " +
                    "CalculationDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (Username) REFERENCES Users(Username))");

            // Add default admin user if not exists
            ResultSet rs = stmt.executeQuery("SELECT * FROM Users WHERE Username='admin'");
            if (!rs.next()) {
                stmt.executeUpdate("INSERT INTO Users (Username, Password) VALUES ('admin', 'admin123')");
                System.out.println("Default admin user created");
            }
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }

    public static void saveTaxRecord(double income, double deductions, double tax, String username) {
        String query = "INSERT INTO TaxCalculations (Username, Income, Deductions, TaxAmount, Status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setDouble(2, income);
            stmt.setDouble(3, deductions);
            stmt.setDouble(4, tax);
            stmt.setString(5, "Pending");
            stmt.executeUpdate();
            System.out.println("Tax record saved successfully.");
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    public static boolean validateUser(String username, String password) {
        // Hardcoded credentials for testing
        return "admin".equals(username) && "admin123".equals(password);
    }
}

// Tax Calculator Logic
class TaxCalculator {
    public static double calculateTax(double income, double deductions) {
        double taxableIncome = income - deductions;
        if (taxableIncome <= 250000) return 0;
        if (taxableIncome <= 500000) return (taxableIncome - 250000) * 0.05;
        if (taxableIncome <= 1000000) return 12500 + (taxableIncome - 500000) * 0.2;
        return 112500 + (taxableIncome - 1000000) * 0.3;
    }
}

// Login GUI
class LoginFrame extends JFrame {
    public LoginFrame() {
        setTitle("User Login");
        setSize(350, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel userLabel = new JLabel("Username:");
        JTextField userText = new JTextField(15);
        JLabel passLabel = new JLabel("Password:");
        JPasswordField passText = new JPasswordField(15);
        JButton loginButton = new JButton("Login");
        JLabel messageLabel = new JLabel("");

        loginButton.addActionListener(e -> {
            String username = userText.getText();
            String password = new String(passText.getPassword());
            if (DatabaseManager.validateUser(username, password)) {
                new TaxCalculatorGUI(username);
                dispose();
            } else {
                messageLabel.setText("Invalid Credentials!");
            }
        });

        gbc.gridx = 0; gbc.gridy = 0; add(userLabel, gbc);
        gbc.gridx = 1; add(userText, gbc);
        gbc.gridx = 0; gbc.gridy = 1; add(passLabel, gbc);
        gbc.gridx = 1; add(passText, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; add(loginButton, gbc);
        gbc.gridy = 3; add(messageLabel, gbc);
    }
}

// Tax Calculator GUI
class TaxCalculatorGUI extends JFrame {
    public TaxCalculatorGUI(String username) {
        setTitle("Tax Calculator - " + username);
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel incomeLabel = new JLabel("Income:");
        JTextField incomeText = new JTextField(10);
        JLabel deductionLabel = new JLabel("Deductions:");
        JTextField deductionText = new JTextField(10);
        JButton calculateButton = new JButton("Calculate Tax");
        JLabel resultLabel = new JLabel("Tax: ");

        calculateButton.addActionListener(e -> {
            try {
                double income = Double.parseDouble(incomeText.getText());
                double deductions = Double.parseDouble(deductionText.getText());
                double tax = TaxCalculator.calculateTax(income, deductions);
                resultLabel.setText("Tax: " + tax);
                DatabaseManager.saveTaxRecord(income, deductions, tax, username);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Enter valid numbers!");
            }
        });

        gbc.gridx = 0; gbc.gridy = 0; add(incomeLabel, gbc);
        gbc.gridx = 1; add(incomeText, gbc);
        gbc.gridx = 0; gbc.gridy = 1; add(deductionLabel, gbc);
        gbc.gridx = 1; add(deductionText, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; add(calculateButton, gbc);
        gbc.gridy = 3; add(resultLabel, gbc);

        setVisible(true);
    }
}

// Main Class
public class TaxCalculatorApp {
    public static void main(String[] args) {
        // Initialize database and tables
        DatabaseManager.initializeDatabase();
        
        // Start the application
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}