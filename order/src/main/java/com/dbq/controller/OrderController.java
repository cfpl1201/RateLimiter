package com.dbq.controller;

import com.dbq.common.model.Account;
import com.dbq.common.result.PojoResult;
import com.dbq.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author dbq
 * @create 2018-10-19 11:58
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/{id}/getOrderAndAccount")
    public PojoResult<Account> getOrderAndAccount(@PathVariable Integer id) {
        return new PojoResult<>(orderService.getBankAccount(id));
    }

    /**
     * 模拟下单操作，验证分布式事务
     * @param id
     * @return
     */
    @GetMapping("/{id}/add")
    public void add(@PathVariable Integer id) {
        orderService.add(id);
    }
}
