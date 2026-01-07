package com.microservices.decoding.orderservice.service;

import com.microservices.decoding.orderservice.client.InventoryClient;
import com.microservices.decoding.orderservice.dto.OrderRequest;
import com.microservices.decoding.orderservice.dto.OrderLineItemsDto;
import com.microservices.decoding.orderservice.model.Order;
import com.microservices.decoding.orderservice.model.OrderLineItems;
import com.microservices.decoding.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient; // Inyección del cliente Feign

    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        // Mapeo manual (o usa MapStruct en el futuro)
        var orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToEntity)
                .toList();

        order.setOrderLineItemsList(orderLineItems);

        // 1. Llamada Síncrona a Inventario (Verificación)
        // Por simplicidad, verificamos solo el primer item o iteramos.
        // Vamos a verificar si TODOS los items tienen stock.
        boolean allProductsInStock = order.getOrderLineItemsList().stream()
                .allMatch(item -> inventoryClient.checkStock(item.getSkuCode()));

        if (allProductsInStock) {
            orderRepository.save(order);
            System.out.println("Pedido guardado exitosamente");
        } else {
            throw new IllegalArgumentException("El producto no está en stock, intente más tarde");
        }
    }

    private OrderLineItems mapToEntity(OrderLineItemsDto itemDto) {
        OrderLineItems item = new OrderLineItems();
        item.setPrice(itemDto.getPrice());
        item.setQuantity(itemDto.getQuantity());
        item.setSkuCode(itemDto.getSkuCode());
        return item;
    }
}