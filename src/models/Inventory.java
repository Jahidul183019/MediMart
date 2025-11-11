package models;

import services.MedicineService;
import java.util.List;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Inventory {

    private static Inventory instance;
    private MedicineService medicineService;

    private Inventory() {
        this.medicineService = new MedicineService(); // Initialize the MedicineService
    }

    public static Inventory getInstance() {
        if (instance == null) {
            instance = new Inventory();
        }
        return instance;
    }

    // Fetch all medicines from the database
    public List<Medicine> getAllMedicines() {
        return medicineService.getAllMedicines();
    }

    // Add a new medicine to the database
    public boolean addMedicine(Medicine medicine) {
        return medicineService.addMedicine(medicine);
    }

    // Update an existing medicine
    public boolean updateMedicine(Medicine updatedMed) {
        // Pass individual properties of Medicine to updateMedicine method
        return medicineService.updateMedicine(
                updatedMed.getId(),
                updatedMed.getName(),
                updatedMed.getCategory(),
                updatedMed.getPrice(),
                updatedMed.getQuantity(),
                updatedMed.getExpiryDate()
        );
    }


    // Delete a medicine by ID
    public boolean deleteMedicine(int id) {
        return medicineService.deleteMedicine(id);
    }

    // Get a medicine by ID
    public Medicine getMedicineById(int id) {
        return medicineService.getMedicineById(id);
    }
}
