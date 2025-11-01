import java.util.ArrayList;
import java.util.List;

public class Inventory {
    private List<Medicine> medicines;

    public Inventory() {
        medicines = new ArrayList<>();

        medicines.add(new Medicine(1, "Paracetamol", "Painkiller", 5.0, 50, "2025-12-31"));
        medicines.add(new Medicine(2, "Aspirin", "Painkiller", 3.0, 50, "2025-11-30"));
        medicines.add(new Medicine(3, "Ibuprofen", "Painkiller", 4.5, 50, "2026-01-15"));
    }

    public List<Medicine> getAllMedicines() {
        return medicines;
    }

    public Medicine getMedicineById(int id) {
        for (Medicine med : medicines) {
            if (med.getId() == id) {
                return med;
            }
        }
        return null;
    }

    // ✅ ADD THIS METHOD
    public void addMedicine(Medicine medicine) {
        medicines.add(medicine);
    }
}
