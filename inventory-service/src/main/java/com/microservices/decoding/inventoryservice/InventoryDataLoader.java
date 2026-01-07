package com.microservices.decoding.inventoryservice;

import com.microservices.decoding.inventoryservice.model.Inventory;
import com.microservices.decoding.inventoryservice.repository.InventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class InventoryDataLoader implements CommandLineRunner {

    private final InventoryRepository inventoryRepository;

    public InventoryDataLoader(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        inventoryRepository.save(new Inventory(null, "iphone_15", 100));
        inventoryRepository.save(new Inventory(null, "samsung_s24", 0)); // Sin stock

        System.out.println("--- Datos de prueba de Inventario cargados ---");
    }
}