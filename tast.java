import java.util.*;
import java.time.LocalDate;

// Interface for shippable items
interface Shippable {
    String getName();
    double getWeight();
}

class Product implements Shippable {
    String name;
    double price;
    int quantity;
    boolean expires;
    double weight;
    LocalDate expirationDate;
    boolean requiresShipping;

    Product(String name, double price, int quantity, boolean expires, double weight, boolean requiresShipping) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.expires = expires;
        this.weight = weight;
        this.requiresShipping = requiresShipping;
        // Set expiration date for expirable products (example: 7 days from now)
        if (expires) {
            this.expirationDate = LocalDate.now().plusDays(7);
        }
    }

    // Constructor for expired products (for testing)
    Product(String name, double price, int quantity, boolean expires, double weight, boolean requiresShipping, LocalDate expirationDate) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.expires = expires;
        this.weight = weight;
        this.requiresShipping = requiresShipping;
        this.expirationDate = expirationDate;
    }

    boolean isExpired() {
        return expires && expirationDate != null && LocalDate.now().isAfter(expirationDate);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getWeight() {
        return weight;
    }
}

class Customer {
    double balance;

    Customer(double balance) {
        this.balance = balance;
    }
}

class Cart {
    Map<Product, Integer> items = new HashMap<>();

    void add(Product product, int quantity) {
        if (product.isExpired()) {
            System.out.println("Error: Product " + product.name + " has expired!");
            return;
        }
        
        if (product.quantity < quantity) {
            System.out.println("Error: Insufficient stock for " + product.name + 
                             ". Available: " + product.quantity + ", Requested: " + quantity);
            return;
        }
        
        items.put(product, items.getOrDefault(product, 0) + quantity);
        product.quantity -= quantity;
        System.out.println("Added " + quantity + "x " + product.name + " to cart");
    }

    boolean isEmpty() {
        return items.isEmpty();
    }
}

class ShippingService {
    void ship(List<Shippable> items, Map<Product, Integer> quantities) {
        if (items.isEmpty()) {
            System.out.println("No items to ship");
            return;
        }
        
        System.out.println("** Shipment notice **");
        double totalWeight = 0;
        
        for (Shippable item : items) {
            Product product = (Product) item;
            int qty = quantities.get(product);
            double itemWeight = item.getWeight() * qty;
            totalWeight += itemWeight;
            System.out.println(qty + "x " + item.getName() + "\t" + (int)itemWeight + "g");
        }
        
        System.out.println("Total package weight " + String.format("%.1f", totalWeight / 1000) + "kg");
        System.out.println();
    }
}

class Checkout {
    static void checkout(Customer customer, Cart cart) {
        // Check if cart is empty
        if (cart.isEmpty()) {
            System.out.println("Error: Cart is empty!");
            return;
        }

        // Check for expired products
        for (Product product : cart.items.keySet()) {
            if (product.isExpired()) {
                System.out.println("Error: Product " + product.name + " has expired!");
                return;
            }
        }

        double subtotal = calculateSubtotal(cart);
        double shipping = calculateShipping(cart);
        double amount = subtotal + shipping;

        // Check if customer has sufficient balance
        if (customer.balance < amount) {
            System.out.println("Error: Customer's balance is insufficient. Required: " + 
                             amount + ", Available: " + customer.balance);
            return;
        }

        // Process shipping first
        List<Shippable> toShip = new ArrayList<>();
        Map<Product, Integer> shippableQuantities = new HashMap<>();
        
        for (Map.Entry<Product, Integer> entry : cart.items.entrySet()) {
            Product product = entry.getKey();
            if (product.requiresShipping && product.weight > 0) {
                toShip.add(product);
                shippableQuantities.put(product, entry.getValue());
            }
        }
        
        new ShippingService().ship(toShip, shippableQuantities);

        // Print receipt
        printReceipt(cart, subtotal, shipping, amount, customer.balance);
        
        // Update customer balance
        customer.balance -= amount;
        System.out.println("Customer current balance after payment: " + customer.balance);
        
        // Clear cart
        cart.items.clear();
        System.out.println("Checkout completed successfully!");
    }

    static double calculateSubtotal(Cart cart) {
        return cart.items.entrySet().stream()
                .mapToDouble(entry -> entry.getKey().price * entry.getValue())
                .sum();
    }

    static double calculateShipping(Cart cart) {
        double totalWeight = 0;
        for (Map.Entry<Product, Integer> entry : cart.items.entrySet()) {
            Product product = entry.getKey();
            if (product.requiresShipping && product.weight > 0) {
                totalWeight += product.weight * entry.getValue();
            }
        }
        return totalWeight > 0 ? 30.0 : 0.0;
    }

    static void printReceipt(Cart cart, double subtotal, double shipping, double amount, double initialBalance) {
        System.out.println("** Checkout receipt **");
        cart.items.forEach((product, qty) -> 
            System.out.println(qty + "x " + product.name + "\t" + (int)(product.price * qty)));
        System.out.println("-----------------------");
        System.out.println("Subtotal\t" + (int)subtotal);
        System.out.println("Shipping\t" + (int)shipping);
        System.out.println("Amount\t\t" + (int)amount);
        System.out.println();
    }
}

public class Main {
    public static void main(String[] args) {
        System.out.println("=== E-COMMERCE SYSTEM TEST ===\n");
        
        // Test Case 1: Normal checkout
        System.out.println("TEST 1: Normal Checkout");
        Customer customer = new Customer(400.0);
        Cart cart = new Cart();

        Product cheese = new Product("Cheese", 100.0, 10, true, 400.0, true);
        Product biscuits = new Product("Biscuits", 150.0, 5, true, 300.0, true);
        Product tv = new Product("TV", 200.0, 3, false, 700.0, true);
        Product scratchCard = new Product("ScratchCard", 50.0, 10, false, 0.0, false);

        cart.add(cheese, 2);
        cart.add(biscuits, 1);
        cart.add(tv, 1); // Changed from 3 to 1 to avoid insufficient stock
        cart.add(scratchCard, 1);

        Checkout.checkout(customer, cart);
        
        // Test Case 2: Empty cart
        System.out.println("\nTEST 2: Empty Cart");
        Cart emptyCart = new Cart();
        Checkout.checkout(customer, emptyCart);
        
        // Test Case 3: Insufficient balance
        System.out.println("\nTEST 3: Insufficient Balance");
        Customer poorCustomer = new Customer(50.0);
        Cart expensiveCart = new Cart();
        expensiveCart.add(tv, 1);
        Checkout.checkout(poorCustomer, expensiveCart);
        
        // Test Case 4: Insufficient stock
        System.out.println("\nTEST 4: Insufficient Stock");
        Cart cart2 = new Cart();
        cart2.add(tv, 5); // Only 2 left in stock
        
        // Test Case 5: Expired product
        System.out.println("\nTEST 5: Expired Product");
        Product expiredCheese = new Product("Expired Cheese", 100.0, 5, true, 400.0, true, LocalDate.now().minusDays(1));
        Cart cart3 = new Cart();
        cart3.add(expiredCheese, 1);
        
        // Test Case 6: Digital product only (no shipping)
        System.out.println("\nTEST 6: Digital Product Only");
        Customer digitalCustomer = new Customer(100.0);
        Cart digitalCart = new Cart();
        digitalCart.add(scratchCard, 2);
        Checkout.checkout(digitalCustomer, digitalCart);
    }
}