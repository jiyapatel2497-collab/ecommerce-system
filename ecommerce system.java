import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// ==========================================
// 1. CUSTOM EXCEPTIONS (UNIT 3)
// ==========================================
class OutOfStockException extends Exception {
    public OutOfStockException(String message) {
        super(message);
    }
}

class ProductNotFoundException extends Exception {
    public ProductNotFoundException(String message) {
        super(message);
    }
}

// ==========================================
// 2. OOP MODELS & INHERITANCE (UNIT 2)
// ==========================================
abstract class Product {
    private final String productId;
    private final String name;
    private final double unitPrice;
    private int stockQuantity;

    public Product(String productId, String name, double unitPrice, int stockQuantity) {
        this.productId = productId;
        this.name = name;
        this.unitPrice = unitPrice;
        this.stockQuantity = stockQuantity;
    }

    public String getProductId() { return productId; }
    public String getName() { return name; }
    public double getUnitPrice() { return unitPrice; }
    public synchronized int getStockQuantity() { return stockQuantity; }

    public synchronized void reduceStock(int quantity) throws OutOfStockException {
        if (quantity > stockQuantity) {
            throw new OutOfStockException("Stock insufficient for " + name + ". Available: " + stockQuantity);
        }
        this.stockQuantity -= quantity;
    }

    public abstract double calculateTotalCost(int quantity);
    public abstract String getCategory();
}

class PhysicalProduct extends Product {
    private final double weightKg;

    public PhysicalProduct(String productId, String name, double unitPrice, int stockQuantity, double weightKg) {
        super(productId, name, unitPrice, stockQuantity);
        this.weightKg = weightKg;
    }

    @Override
    public double calculateTotalCost(int quantity) {
        double shippingFee = weightKg * 2.50; // $2.50 per kg
        return (getUnitPrice() * quantity) + shippingFee;
    }

    @Override
    public String getCategory() { return "PHYSICAL"; }
}

class DigitalProduct extends Product {
    public DigitalProduct(String productId, String name, double unitPrice, int stockQuantity) {
        super(productId, name, unitPrice, stockQuantity);
    }

    @Override
    public double calculateTotalCost(int quantity) {
        return getUnitPrice() * quantity;
    }

    @Override
    public String getCategory() { return "DIGITAL"; }
}

// ==========================================
// 3. ORDER CLASS
// ==========================================
class Order {
    private final String orderId;
    private final String customerName;
    private final Product product;
    private final int quantity;
    private final double totalPrice;
    private final String timestamp;

    public Order(String orderId, String customerName, Product product, int quantity) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.product = product;
        this.quantity = quantity;
        this.totalPrice = product.calculateTotalCost(quantity);
        this.timestamp = LocalDateTime.now().toString();
    }

    public String getOrderId() { return orderId; }
    public String getCustomerName() { return customerName; }
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public double getTotalPrice() { return totalPrice; }

    public String getInvoiceDetails() {
        return String.format("[%s] Order: %s | Customer: %s | Product: %s (x%d) | Total: $%.2f",
                timestamp, orderId, customerName, product.getName(), quantity, totalPrice);
    }
}

// ==========================================
// 4. INVENTORY SERVICE (UNITS 3, 4, 5)
// ==========================================
class InventoryService {
    private final Map<String, Product> inventory = new ConcurrentHashMap<>();
    private final List<Order> fulfilledOrders = new ArrayList<>();
    private Connection dbConnection;

    public InventoryService() {
        initDatabase();
    }

    private void initDatabase() {
        try {
            String url = "jdbc:sqlite:ecommerce.db";
            dbConnection = DriverManager.getConnection(url);
            try (Statement stmt = dbConnection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS inventory (product_id TEXT PRIMARY KEY, name TEXT, price REAL, stock INTEGER, category TEXT)");
                stmt.execute("CREATE TABLE IF NOT EXISTS orders (order_id TEXT PRIMARY KEY, customer_name TEXT, product_id TEXT, quantity INTEGER, total_price REAL)");
            }
        } catch (SQLException e) {
            System.out.println("[JDBC Note] SQLite driver not found. Running seamlessly in in-memory mode.");
        }
    }

    public void addProduct(Product product) {
        inventory.put(product.getProductId(), product);
        saveProductToDB(product);
    }

    public Product getProduct(String productId) throws ProductNotFoundException {
        Product p = inventory.get(productId);
        if (p == null) {
            throw new ProductNotFoundException("Product with ID '" + productId + "' was not found.");
        }
        return p;
    }

    public synchronized Order processOrder(String orderId, String customerName, String productId, int quantity)
            throws ProductNotFoundException, OutOfStockException {
        
        Product product = getProduct(productId);
        product.reduceStock(quantity);

        Order order = new Order(orderId, customerName, product, quantity);
        fulfilledOrders.add(order);

        updateDBStock(product);
        saveOrderToDB(order);

        System.out.println("[ORDER SUCCESS] " + order.getInvoiceDetails());
        return order;
    }

    public void exportInvoicesToFile(String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("=== E-COMMERCE ORDER INVOICES REPORT ===\n");
            synchronized (fulfilledOrders) {
                for (Order o : fulfilledOrders) {
                    writer.write(o.getInvoiceDetails());
                    writer.newLine();
                }
            }
            System.out.println("Successfully exported order invoices to: " + filePath);
        } catch (IOException e) {
            System.err.println("File I/O Error: " + e.getMessage());
        }
    }

    private void saveProductToDB(Product p) {
        if (dbConnection == null) return;
        String sql = "INSERT OR REPLACE INTO inventory VALUES(?,?,?,?,?)";
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, p.getProductId());
            pstmt.setString(2, p.getName());
            pstmt.setDouble(3, p.getUnitPrice());
            pstmt.setInt(4, p.getStockQuantity());
            pstmt.setString(5, p.getCategory());
            pstmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    private void updateDBStock(Product p) {
        if (dbConnection == null) return;
        String sql = "UPDATE inventory SET stock = ? WHERE product_id = ?";
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, p.getStockQuantity());
            pstmt.setString(2, p.getProductId());
            pstmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    private void saveOrderToDB(Order o) {
        if (dbConnection == null) return;
        String sql = "INSERT INTO orders VALUES(?,?,?,?,?)";
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, o.getOrderId());
            pstmt.setString(2, o.getCustomerName());
            pstmt.setString(3, o.getProduct().getProductId());
            pstmt.setInt(4, o.getQuantity());
            pstmt.setDouble(5, o.getTotalPrice());
            pstmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public Map<String, Product> getInventory() { return inventory; }
}

// ==========================================
// 5. MAIN CLI APPLICATION (UNITS 1 & 3)
// ==========================================
public class ECommerceOrderApp {
    private static final InventoryService service = new InventoryService();

    public static void main(String[] args) {
        seedInitialData();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n--- CONCURRENT E-COMMERCE INVENTORY SYSTEM ---");
            System.out.println("1. View Available Product Inventory");
            System.out.println("2. Place Single Customer Order");
            System.out.println("3. Run Multithreaded Flash Sale Simulation");
            System.out.println("4. Export Invoices to File");
            System.out.println("5. Exit");
            System.out.print("Select an option (1-5): ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    displayInventory();
                    break;
                case "2":
                    handleSingleOrder(scanner);
                    break;
                case "3":
                    runFlashSaleSimulation();
                    break;
                case "4":
                    service.exportInvoicesToFile("invoices_report.txt");
                    break;
                case "5":
                    System.out.println("Exiting Application. Thank you!");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid input. Please enter a choice between 1 and 5.");
            }
        }
    }

    private static void seedInitialData() {
        service.addProduct(new PhysicalProduct("P101", "Gaming Laptop", 1200.00, 5, 2.5));
        service.addProduct(new PhysicalProduct("P102", "Wireless Mouse", 25.50, 20, 0.2));
        service.addProduct(new DigitalProduct("P103", "Java Programming E-Book", 19.99, 100));
    }

    private static void displayInventory() {
        System.out.println("\n=== CURRENT INVENTORY ===");
        for (Product p : service.getInventory().values()) {
            System.out.printf("ID: %-5s | Category: %-8s | Name: %-22s | Price: $%-7.2f | Stock: %d\n",
                    p.getProductId(), p.getCategory(), p.getName(), p.getUnitPrice(), p.getStockQuantity());
        }
    }

    private static void handleSingleOrder(Scanner scanner) {
        try {
            System.out.print("Enter Customer Name: ");
            String name = scanner.nextLine().trim();
            System.out.print("Enter Product ID: ");
            String pId = scanner.nextLine().trim();
            System.out.print("Enter Quantity: ");
            int qty = Integer.parseInt(scanner.nextLine().trim());

            String orderId = "ORD-" + (System.currentTimeMillis() % 10000);
            service.processOrder(orderId, name, pId, qty);
        } catch (NumberFormatException e) {
            System.out.println("Error: Quantity must be a valid integer.");
        } catch (ProductNotFoundException | OutOfStockException e) {
            System.out.println("Order Rejected: " + e.getMessage());
        }
    }

    private static void runFlashSaleSimulation() {
        System.out.println("\n--- Launching 8 Concurrent Threads Attempting to Buy 'Gaming Laptop' (Stock: 5) ---");
        ExecutorService executor = Executors.newFixedThreadPool(4);

        for (int i = 1; i <= 8; i++) {
            final int buyerId = i;
            executor.submit(() -> {
                try {
                    String orderId = "FLASH-" + buyerId;
                    service.processOrder(orderId, "Buyer_" + buyerId, "P101", 1);
                } catch (Exception e) {
                    System.err.println("[BUYER " + buyerId + " FAILED]: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        try {
            if (executor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println("--- Flash Sale Simulation Completed Safely ---");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
