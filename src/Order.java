import java.util.ArrayList;
import java.util.List;

public class Order {
    private List<Medicine> cart = new ArrayList<>();

    public void addToCart(Medicine m, int qty) {
        if (qty <= 0 || m.getQuantity() < qty) return;

        m.setQuantity(m.getQuantity() - qty);
        cart.add(new Medicine(m.getId(), m.getName(), m.getCategory(),
                m.getPrice(), qty, m.getExpiryDate()));
    }

    public double calculateTotal() {
        double total = 0;
        for (Medicine m : cart) total += m.getPrice() * m.getQuantity();
        return total;
    }

    public List<Medicine> getCartItems() { return cart; }
}
