package com.microservices.decoding.orderservice.service;

import com.microservices.decoding.orderservice.client.InventoryClient;
import com.microservices.decoding.orderservice.dto.OrderRequest;
import com.microservices.decoding.orderservice.dto.OrderLineItemsDto;
import com.microservices.decoding.orderservice.event.OrderPlacedEvent;
import com.microservices.decoding.orderservice.model.Order;
import com.microservices.decoding.orderservice.model.OrderLineItems;
import com.microservices.decoding.orderservice.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient; // Inyección del cliente Feign
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackPlaceOrder")
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

            kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
            System.out.println("Pedido guardado y notificacion enviada para: " + order.getOrderNumber());
        } else {
            throw new IllegalArgumentException("El producto no está en stock, intente más tarde");
        }
    }

    /**
     * MÉTODO FALLBACK
     * Se ejecuta cuando el InventoryService está caído o el circuito está abierto.
     * IMPORTANTE: Debe tener la misma firma (argumentos) que el método original,
     * más un argumento extra para la Excepción.
     */
    public void fallbackPlaceOrder(OrderRequest orderRequest, Throwable runtimeException) {
        // Opción A: Guardar el pedido en un estado "PENDIENTE_VERIFICACION"
        // Opción B: Rechazar amablemente.

        System.out.println("ATENCIÓN: Inventario no disponible. Ejecutando Fallback logic.");

        // Para esta práctica, lanzaremos una excepción personalizada evitando el timeout
        throw new RuntimeException("El servicio de inventario no está disponible en este momento. Intente más tarde. (Fallback activado)");
    }

    private OrderLineItems mapToEntity(OrderLineItemsDto itemDto) {
        OrderLineItems item = new OrderLineItems();
        item.setPrice(itemDto.getPrice());
        item.setQuantity(itemDto.getQuantity());
        item.setSkuCode(itemDto.getSkuCode());
        return item;
    }
}