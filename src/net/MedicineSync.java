// src/net/MedicineSync.java
package net;
public class MedicineSync {
    private static final MedicineSyncServer INSTANCE = new MedicineSyncServer();
    public static MedicineSyncServer getInstance() { return INSTANCE; }
}
