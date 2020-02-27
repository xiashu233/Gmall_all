package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OmsCartItem;

import java.util.List;

public interface CartService {
    List<OmsCartItem> getCartsByUser(String memberId);

    void updateCart(OmsCartItem omsCartItemFromDb);

    void addCart(OmsCartItem omsCartItemFromDb);

    OmsCartItem ifCartExistByUser(String memberId, String skuId);

    void flushCartCache(String memberId);

    List<OmsCartItem> cartList(String memberId);

    void checkCart(OmsCartItem omsCartItem);
}
