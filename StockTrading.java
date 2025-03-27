import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

// Main class to run the application
public class StockTrading {
    public static void main(String[] args) {
        StockMarket stockMarket = new StockMarket();
        UserManager userManager = new UserManager();
        TransactionLogger transactionLogger = new TransactionLogger();
        
        // Initialize with some sample stocks
        stockMarket.addStock(new Stock("TTM", "TATAMOTORS", 425.27));
        stockMarket.addStock(new Stock("YSB", "YESBANK", 185.92));
        stockMarket.addStock(new Stock("JSW", "JSWSTEEL", 175.33));
        stockMarket.addStock(new Stock("ASP", "ASIANPAINT", 187.63));
        stockMarket.addStock(new Stock("GAB", "GABRIEL", 243.64));
        
        Console console = new Console(userManager, stockMarket, transactionLogger);
        console.start();
    }
}

// User class to store user information
class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private String password;
    private BankAccount bankAccount;
    private Map<String, Integer> portfolio; // Map of stock symbol to quantity

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.bankAccount = new BankAccount(10000.0); // Initial balance of ₹10,000
        this.portfolio = new HashMap<>();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public Map<String, Integer> getPortfolio() {
        return portfolio;
    }

    public void addToPortfolio(String symbol, int quantity) {
        portfolio.put(symbol, portfolio.getOrDefault(symbol, 0) + quantity);
        // Remove stock from portfolio if quantity becomes zero
        if (portfolio.get(symbol) <= 0) {
            portfolio.remove(symbol);
        }
    }
}

// Bank Account class to manage user's balance
class BankAccount implements Serializable {
    private static final long serialVersionUID = 1L;
    private double balance;

    public BankAccount(double initialBalance) {
        this.balance = initialBalance;
    }

    public double getBalance() {
        return balance;
    }

    public void deposit(double amount) {
        balance += amount;
    }

    public boolean withdraw(double amount) {
        if (amount <= balance) {
            balance -= amount;
            return true;
        }
        return false;
    }
}

// Stock class to represent a stock in the market
class Stock implements Serializable {
    private static final long serialVersionUID = 1L;
    private String symbol;
    private String name;
    private double price;

    public Stock(String symbol, String name, double price) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return String.format("%-5s | %-25s | ₹%.2f", symbol, name, price);
    }
}

// Stock Market class to manage available stocks
class StockMarket {
    private Map<String, Stock> stocks;

    public StockMarket() {
        this.stocks = new HashMap<>();
    }

    public void addStock(Stock stock) {
        stocks.put(stock.getSymbol(), stock);
    }

    public Stock getStock(String symbol) {
        return stocks.get(symbol);
    }

    public List<Stock> getAllStocks() {
        return new ArrayList<>(stocks.values());
    }
}

// Transaction class to log buy/sell operations
class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private String stockSymbol;
    private int quantity;
    private double pricePerShare;
    private double totalAmount;
    private TransactionType type;
    private LocalDateTime timestamp;

    public enum TransactionType {
        BUY, SELL
    }

    public Transaction(String username, String stockSymbol, int quantity, 
                      double pricePerShare, TransactionType type) {
        this.username = username;
        this.stockSymbol = stockSymbol;
        this.quantity = quantity;
        this.pricePerShare = pricePerShare;
        this.totalAmount = quantity * pricePerShare;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }
    
    // Add getter for username
    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format("%-20s | %-5s | %-4s | %-3d | ₹%-8.2f | ₹%-10.2f", 
            timestamp.format(formatter), stockSymbol, type, quantity, pricePerShare, totalAmount);
    }
}

// Transaction Logger to manage transaction history
class TransactionLogger {
    private Map<String, List<Transaction>> transactions;

    public TransactionLogger() {
        this.transactions = new HashMap<>();
    }

    public void logTransaction(Transaction transaction) {
        String username = transaction.getUsername(); // Using getter method now
        if (!transactions.containsKey(username)) {
            transactions.put(username, new ArrayList<>());
        }
        transactions.get(username).add(transaction);
    }

    public List<Transaction> getUserTransactions(String username) {
        return transactions.getOrDefault(username, new ArrayList<>());
    }
}

// User Manager class to handle user authentication and registration
class UserManager {
    private Map<String, User> users;
    private final String DATA_FILE = "users.dat";

    public UserManager() {
        this.users = new HashMap<>();
        loadUsers();
    }

    public boolean registerUser(String username, String password) {
        if (users.containsKey(username)) {
            return false;
        }
        
        User newUser = new User(username, password);
        users.put(username, newUser);
        saveUsers();
        return true;
    }

    public User authenticateUser(String username, String password) {
        User user = users.get(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    public User getUser(String username) {
        return users.get(username);
    }

    @SuppressWarnings("unchecked")
    private void loadUsers() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
            users = (Map<String, User>) ois.readObject();
        } catch (FileNotFoundException e) {
            // File doesn't exist yet, start with empty users map
            users = new HashMap<>();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error loading user data: " + e.getMessage());
            users = new HashMap<>();
        }
    }

    private void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            System.out.println("Error saving user data: " + e.getMessage());
        }
    }
}

// Console class to handle user interaction
class Console {
    private Scanner scanner;
    private UserManager userManager;
    private StockMarket stockMarket;
    private TransactionLogger transactionLogger;
    private User currentUser;

    public Console(UserManager userManager, StockMarket stockMarket, TransactionLogger transactionLogger) {
        this.scanner = new Scanner(System.in);
        this.userManager = userManager;
        this.stockMarket = stockMarket;
        this.transactionLogger = transactionLogger;
    }

    public void start() {
        boolean running = true;
        
        while (running) {
            if (currentUser == null) {
                displayAuthMenu();
            } else {
                displayMainMenu();
            }
        }
    }

    private void displayAuthMenu() {
        System.out.println("\n===== STOCK TRADING APPLICATION =====");
        System.out.println("1. Login");
        System.out.println("2. Register");
        System.out.println("3. Exit");
        System.out.print("Select an option: ");
        
        int choice = getIntInput();
        
        switch (choice) {
            case 1:
                login();
                break;
            case 2:
                register();
                break;
            case 3:
                System.out.println("Thank you for using the Stock Trading App. Goodbye!");
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option. Please try again.");
        }
    }

    private void register() {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        
        if (userManager.registerUser(username, password)) {
            System.out.println("Registration successful! You can now login.");
        } else {
            System.out.println("Username already exists. Please choose a different username.");
        }
    }

    private void login() {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        
        User user = userManager.authenticateUser(username, password);
        if (user != null) {
            currentUser = user;
            System.out.println("Login successful! Welcome, " + username + "!");
        } else {
            System.out.println("Invalid username or password. Please try again.");
        }
    }

    private void displayMainMenu() {
        System.out.println("\n===== MAIN MENU =====");
        System.out.println("Logged in as: " + currentUser.getUsername());
        System.out.println("Account Balance: ₹" + String.format("%.2f", currentUser.getBankAccount().getBalance()));
        System.out.println("1. View Available Stocks");
        System.out.println("2. View My Portfolio");
        System.out.println("3. Buy Stocks");
        System.out.println("4. Sell Stocks");
        System.out.println("5. View Transaction History");
        System.out.println("6. Logout");
        System.out.print("Select an option: ");
        
        int choice = getIntInput();
        
        switch (choice) {
            case 1:
                viewAvailableStocks();
                break;
            case 2:
                viewPortfolio();
                break;
            case 3:
                buyStocks();
                break;
            case 4:
                sellStocks();
                break;
            case 5:
                viewTransactionHistory();
                break;
            case 6:
                logout();
                break;
            default:
                System.out.println("Invalid option. Please try again.");
        }
    }

    private void viewAvailableStocks() {
        List<Stock> stocks = stockMarket.getAllStocks();
        
        System.out.println("\n===== AVAILABLE STOCKS =====");
        System.out.println("SYMBOL | NAME                      | PRICE");
        System.out.println("------------------------------------------------");
        
        for (Stock stock : stocks) {
            System.out.println(stock);
        }
        
        pressEnterToContinue();
    }

    private void viewPortfolio() {
        Map<String, Integer> portfolio = currentUser.getPortfolio();
        
        if (portfolio.isEmpty()) {
            System.out.println("\nYou don't own any stocks yet.");
            pressEnterToContinue();
            return;
        }
        
        System.out.println("\n===== YOUR PORTFOLIO =====");
        System.out.println("SYMBOL | NAME                      | QUANTITY | PRICE     | TOTAL VALUE");
        System.out.println("----------------------------------------------------------------------");
        
        double totalPortfolioValue = 0;
        
        for (Map.Entry<String, Integer> entry : portfolio.entrySet()) {
            String symbol = entry.getKey();
            int quantity = entry.getValue();
            Stock stock = stockMarket.getStock(symbol);
            
            if (stock != null) {
                double totalValue = quantity * stock.getPrice();
                totalPortfolioValue += totalValue;
                
                System.out.printf("%-5s | %-25s | %-8d | ₹%-8.2f | ₹%.2f%n", 
                    symbol, stock.getName(), quantity, stock.getPrice(), totalValue);
            }
        }
        
        System.out.println("----------------------------------------------------------------------");
        System.out.printf("TOTAL PORTFOLIO VALUE: ₹%.2f%n", totalPortfolioValue);
        
        pressEnterToContinue();
    }

    private void buyStocks() {
        viewAvailableStocks();
        
        System.out.print("Enter stock symbol to buy (or 'cancel' to go back): ");
        String symbol = scanner.nextLine().toUpperCase();
        
        if (symbol.equalsIgnoreCase("cancel")) {
            return;
        }
        
        Stock stock = stockMarket.getStock(symbol);
        
        if (stock == null) {
            System.out.println("Invalid stock symbol. Please try again.");
            pressEnterToContinue();
            return;
        }
        
        System.out.print("Enter quantity to buy: ");
        int quantity = getIntInput();
        
        if (quantity <= 0) {
            System.out.println("Quantity must be greater than zero.");
            pressEnterToContinue();
            return;
        }
        
        double totalCost = quantity * stock.getPrice();
        
        if (currentUser.getBankAccount().getBalance() >= totalCost) {
            // Process the transaction
            currentUser.getBankAccount().withdraw(totalCost);
            currentUser.addToPortfolio(symbol, quantity);
            
            // Log the transaction
            Transaction transaction = new Transaction(
                currentUser.getUsername(), symbol, quantity, stock.getPrice(), Transaction.TransactionType.BUY);
            transactionLogger.logTransaction(transaction);
            
            System.out.printf("Successfully purchased %d shares of %s for ₹%.2f%n", 
                quantity, stock.getName(), totalCost);
        } else {
            System.out.println("Insufficient funds. Transaction cancelled.");
        }
        
        pressEnterToContinue();
    }

    private void sellStocks() {
        Map<String, Integer> portfolio = currentUser.getPortfolio();
        
        if (portfolio.isEmpty()) {
            System.out.println("\nYou don't own any stocks to sell.");
            pressEnterToContinue();
            return;
        }
        
        viewPortfolio();
        
        System.out.print("Enter stock symbol to sell (or 'cancel' to go back): ");
        String symbol = scanner.nextLine().toUpperCase();
        
        if (symbol.equalsIgnoreCase("cancel")) {
            return;
        }
        
        if (!portfolio.containsKey(symbol)) {
            System.out.println("You don't own any shares of this stock.");
            pressEnterToContinue();
            return;
        }
        
        int ownedQuantity = portfolio.get(symbol);
        Stock stock = stockMarket.getStock(symbol);
        
        System.out.printf("You own %d shares of %s. Current price: ₹%.2f%n", 
            ownedQuantity, stock.getName(), stock.getPrice());
        System.out.print("Enter quantity to sell: ");
        int quantity = getIntInput();
        
        if (quantity <= 0) {
            System.out.println("Quantity must be greater than zero.");
            pressEnterToContinue();
            return;
        }
        
        if (quantity > ownedQuantity) {
            System.out.println("You don't own that many shares.");
            pressEnterToContinue();
            return;
        }
        
        double totalValue = quantity * stock.getPrice();
        
        // Process the transaction
        currentUser.getBankAccount().deposit(totalValue);
        currentUser.addToPortfolio(symbol, -quantity);
        
        // Log the transaction
        Transaction transaction = new Transaction(
            currentUser.getUsername(), symbol, quantity, stock.getPrice(), Transaction.TransactionType.SELL);
        transactionLogger.logTransaction(transaction);
        
        System.out.printf("Successfully sold %d shares of %s for ₹%.2f%n", 
            quantity, stock.getName(), totalValue);
        
        pressEnterToContinue();
    }

    private void viewTransactionHistory() {
        List<Transaction> transactions = transactionLogger.getUserTransactions(currentUser.getUsername());
        
        if (transactions.isEmpty()) {
            System.out.println("\nNo transaction history available.");
            pressEnterToContinue();
            return;
        }
        
        System.out.println("\n===== TRANSACTION HISTORY =====");
        System.out.println("TIMESTAMP            | SYMBOL | TYPE | QTY | PRICE    | TOTAL      ");
        System.out.println("--------------------------------------------------------------------");
        
        for (Transaction transaction : transactions) {
            System.out.println(transaction);
        }
        
        pressEnterToContinue();
    }

    private void logout() {
        currentUser = null;
        System.out.println("Logged out successfully.");
    }

    private int getIntInput() {
        try {
            String input = scanner.nextLine();
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void pressEnterToContinue() {
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }
}