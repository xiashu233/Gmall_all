package com.atguigu.gmall.service;

public interface OrderService {
    String genTradCode(String memberId);

    String checkTradCode(String memberId,String tradeCode);
}
