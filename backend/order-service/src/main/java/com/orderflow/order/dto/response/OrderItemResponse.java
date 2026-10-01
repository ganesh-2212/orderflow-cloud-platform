package com.orderflow.order.dto.response;

import com.orderflow.order.entity.OrderItem;

public class OrderItemResponse {
    private String productId;
    private Integer quantity;

    public OrderItemResponse(OrderItem item) {
        this.productId = item.getProductId();
        this.quantity = item.getQuantity();
    }

    public String getProductId() { return productId; }
    public Integer getQuantity() { return quantity; }
}
