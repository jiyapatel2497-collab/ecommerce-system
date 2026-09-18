================================================================================
           CONCURRENT E-COMMERCE INVENTORY & ORDER PROCESSING SYSTEM
================================================================================
Course Title : Programming in Java (CSE2006)
Source File  : ECommerceOrderApp.java
Documentation: Comprehensive Technical Architecture & Deployment Guide
================================================================================

1. EXECUTIVE SUMMARY
--------------------
This system is an enterprise-grade Command Line Interface (CLI) application 
designed to handle high-concurrency order placement and inventory tracking 
during flash sale events. In modern e-commerce systems, high traffic causes 
race conditions where multiple threads access and mutate shared inventory state 
simultaneously, leading to stock overselling and data inconsistency. 

This project solves those challenges using Java thread synchronization, thread-safe 
collection wrappers, custom domain exception barriers, character stream file 
exports, and parameterized JDBC database persistence with automatic fallback.


2. SYSTEM ARCHITECTURE & DATA FLOW
----------------------------------
Below is the execution flow of customer order processing under thread contention:

  +-----------------------------------------------------------------------+
  |                        CLIENT INPUT / THREAD POOL                     |
  |   +------------------+  +------------------+  +------------------+   |
  |   | Buyer Thread 1   |  | Buyer Thread 2   |  | Buyer Thread N   |   |
  |   +--------+---------+  +--------+---------+  +--------+---------+   |
  +------------|---------------------|---------------------|--------------+
               |                     |                     |
               +---------------------+---------------------+
                                     |
                                     v
  +-----------------------------------------------------------------------+
  |                     INVENTORY SERVICE (CRITICAL SECTION)              |
  |                                                                       |
  |  - synchronized processOrder() / reduceStock()                        |
  |  - Validates stock sufficiency                                        |
  |  - Throws OutOfStockException if stock < requested                     |
  +----------------------------------+------------------------------------+
                                     |
                                     +-------------------+
                                     |                   |
                                     v                   v
  +----------------------------------+---+   +-----------+----------------+
  |    IN-MEMORY STATE (COLLECTIONS)     |   |     PERSISTENCE ENGINE         |
  |                                      |   |                            |
  | - ConcurrentHashMap<String, Product> |   | - JDBC SQLite Persistence  |
  | - ArrayList<Order> fulfilledOrders   |   | - BufferedWriter File I/O  |
  +--------------------------------------+   +----------------------------+


3. CLASS DIAGRAM & COMPONENT BREAKDOWN
-------------------------------------
1. Product (Abstract Superclass)
   - Fields: productId (String), name (String), unitPrice (double), stockQuantity (int)
   - Synchronized Methods: getStockQuantity(), reduceStock(int quantity)
   - Abstract Methods: calculateTotalCost(int quantity), getCategory()

2. PhysicalProduct (Concrete Subclass)
   - Fields: weightKg (double)
   - Behavior: Extends Product and calculates shipping fee = base price + (weightKg * $2.50)

3. DigitalProduct (Concrete Subclass)
   - Behavior: Extends Product with zero shipping fee (cost = base price * quantity)

4. Order (Domain Object)
   - Fields: orderId, customerName, product, quantity, totalPrice, timestamp
   - Behavior: Builds timestamped formatted string representations for invoices

5. OutOfStockException & ProductNotFoundException (Custom Exceptions)
   - Checked exceptions extending java.lang.Exception for explicit operational control

6. InventoryService (Application Engine)
   - Data Structures: ConcurrentHashMap<String, Product>, List<Order>
   - Integrations: SQLite JDBC persistence layer with driver fallback mode

7. ECommerceOrderApp (Main Driver)
   - Features: Interactive CLI menu loop and ExecutorService fixed thread pool engine


4. COMPREHENSIVE CSE2006 SYLLABUS MATRIX
----------------------------------------
+-----------------------+----------------------------------+--------------------------------------+
| SYLLABUS MODULE       | JAVA FEATURE / CONCEPT           | SOURCE CODE LOCATION                 |
+-----------------------+----------------------------------+--------------------------------------+
| Unit 1: Basics & Flow | Scanner Input Reading            | ECommerceOrderApp.main()             |
|                       | Switch-Case Control Routing      | ECommerceOrderApp.main()             |
|                       | Formatted Console Output         | displayInventory()                   |
+-----------------------+----------------------------------+--------------------------------------+
| Unit 2: OOP Principles| Abstract Classes & Methods       | abstract class Product               |
|                       | Single / Hierarchical Inheritance| PhysicalProduct, DigitalProduct      |
|                       | Runtime Polymorphism             | calculateTotalCost() override        |
|                       | Data Encapsulation               | Private fields with getters          |
+-----------------------+----------------------------------+--------------------------------------+
| Unit 3: Threads & Ex  | Custom Checked Exceptions        | OutOfStockException                  |
|                       | Thread Synchronization Locks     | synchronized void reduceStock()      |
|                       | ExecutorService Thread Pools     | runFlashSaleSimulation()             |
+-----------------------+----------------------------------+--------------------------------------+
| Unit 4: Collections   | Concurrent Map Collections       | ConcurrentHashMap<String, Product>   |
|                       | Sequence Lists                   | List<Order> fulfilledOrders          |
|                       | Character Stream File Writer     | exportInvoicesToFile()               |
+-----------------------+----------------------------------+--------------------------------------+
| Unit 5: JDBC Persistence| DriverManager Connections      | initDatabase()                       |
|                       | DDL Schema Creation              | CREATE TABLE IF NOT EXISTS           |
|                       | Parameterized PreparedStatement  | saveOrderToDB(), updateDBStock()     |
+-----------------------+----------------------------------+--------------------------------------+


5. COMPILATION & OS-SPECIFIC EXECUTION GUIDE
---------------------------------------------
Prerequisites: JDK 11+ configured in PATH.

A. Standard Execution (In-Memory Fallback Mode):
   1. Navigate to repository root:
      cd path/to/ecommerce-inventory-system

   2. Compile Java source code:
      javac ECommerceOrderApp.java

   3. Execute application:
      java ECommerceOrderApp

B. Execution with SQLite JDBC Persistence Mode:
   1. Download `sqlite-jdbc-3.45.1.0.jar` into project directory.
   2. Execute command based on Operating System:

      - macOS / Linux Terminal:
        java -cp .:sqlite-jdbc-3.45.1.0.jar ECommerceOrderApp

      - Windows Command Prompt (cmd):
        java -cp .;sqlite-jdbc-3.45.1.0.jar ECommerceOrderApp

      - Windows PowerShell:
        java -cp ".;sqlite-jdbc-3.45.1.0.jar" ECommerceOrderApp


6. GIT REPOSITORY CREATION & SETUP COMMANDS
-------------------------------------------
Commands to build and push this project to GitHub from scratch:

   git init
   git config user.name "Your Name"
   git config user.email "your.email@example.com"
   git add ECommerceOrderApp.java README.txt
   git commit -m "Initial commit: Complete CSE2006 concurrent e-commerce project"
   git branch -M main
   git remote add origin https://github.com/atharvatyagi08it-collab/ecommerce-inventory-system.git
   git push -u origin main


7. SAMPLE TERMINAL EXECUTION LOGS
---------------------------------
Main Menu Screen:
   --- CONCURRENT E-COMMERCE INVENTORY SYSTEM ---
   1. View Available Product Inventory
   2. Place Single Customer Order
   3. Run Multithreaded Flash Sale Simulation
   4. Export Invoices to File
   5. Exit
   Select an option (1-5): 3

Option 3 Output (Flash Sale Simulation):
   --- Launching 8 Concurrent Threads Attempting to Buy 'Gaming Laptop' (Stock: 5) ---
   [ORDER SUCCESS]  Order: FLASH-1 | Customer: Buyer_1 | Product: Gaming Laptop (x1) | Total: $1206.25
   [ORDER SUCCESS]  Order: FLASH-2 | Customer: Buyer_2 | Product: Gaming Laptop (x1) | Total: $1206.25
   [ORDER SUCCESS] Order: FLASH-3 | Customer: Buyer_3 | Product: Gaming Laptop (x1) | Total: $1206.25
   [ORDER SUCCESS]  Order: FLASH-4 | Customer: Buyer_4 | Product: Gaming Laptop (x1) | Total: $1206.25
   [ORDER SUCCESS]  Order: FLASH-5 | Customer: Buyer_5 | Product: Gaming Laptop (x1) | Total: $1206.25
   [BUYER 6 FAILED]: Stock insufficient for Gaming Laptop. Available: 0
   [BUYER 7 FAILED]: Stock insufficient for Gaming Laptop. Available: 0
   [BUYER 8 FAILED]: Stock insufficient for Gaming Laptop. Available: 0
   --- Flash Sale Simulation Completed Safely ---


8. DATABASE SCHEMA & OUTPUT FILE FORMATS
---------------------------------------
Database Schema (SQLite: ecommerce.db):
  - Table: inventory
    (product_id TEXT PRIMARY KEY, name TEXT, price REAL, stock INTEGER, category TEXT)
  - Table: orders
    (order_id TEXT PRIMARY KEY, customer_name TEXT, product_id TEXT, quantity INTEGER, total_price REAL)

Exported Report Format (invoices_report.txt):
  === E-COMMERCE ORDER INVOICES REPORT ===
  Order: ORD-1021 | Customer: John Doe | Product: Gaming Laptop (x1) | Total: $1206.25


9. TROUBLESHOOTING & FAQ
------------------------
Q1: Error "Could not find or load main class ECommerceOrderApp.java"
A1: Omit the `.java` extension when executing `java ECommerceOrderApp`. Run `javac ECommerceOrderApp.java` first.

Q2: Error "javac is not recognized as an internal or external command"
A2: JDK is not added to system environment variables. Reinstall JDK or set PATH to JDK bin folder.

Q3: JDBC Note printed in console: "[JDBC Note] SQLite driver not found..."
A3: This is expected behavior when running without external drivers. The application runs seamlessly using in-memory structures.
================================================================================

## Author
**Name:** _JIYA KIRITKUMAR PATEL_________________________  
**Registration Number:** _____25BAI10520________________  
**Course:** Programming in Java  
**Institution:** VIT Bhopal University  
**Academic Year:** 2026–2027
