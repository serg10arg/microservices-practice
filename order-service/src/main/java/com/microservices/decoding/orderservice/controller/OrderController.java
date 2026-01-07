package com.microservices.decoding.orderservice.controller;

import com.microservices.decoding.orderservice.dto.OrderRequest;
import com.microservices.decoding.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // Devuelve 201 si todo sale bien
    public String placeOrder(@RequestBody OrderRequest orderRequest) {
        orderService.placeOrder(orderRequest);
        return "Pedido realizado con éxito";
    }
}