package com.cashticket.controller;

import com.cashticket.dto.BidOrderRequest;
import com.cashticket.dto.BidOrderResponse;
import com.cashticket.entity.User;
import com.cashticket.service.BidOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


// controller/BidPaymentController.java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auction")
public class BidPaymentController {

    private final BidOrderService bidOrderService;

    @PostMapping("/orders")
    public BidOrderResponse createOrder(@RequestBody BidOrderRequest req,
                                        User user) {
        return bidOrderService.createOrder(req, user.getId());
    }
}
