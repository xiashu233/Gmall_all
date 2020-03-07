package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OmsOrder;

import java.math.BigDecimal;

public interface OrderService {
    String genTradCode(String memberId);

    String checkTradCode(String memberId,String tradeCode);

    void saveOrder(OmsOrder omsOrder);
}
