package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    CartService cartService;
    @Reference
    UserService userService;
    @Reference
    OrderService orderService;

    // 结算功能 toTrade
    @LoginRequired(loginSuccess = true)
    @GetMapping("toTrade")
    public String toTrade(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap){
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        // 将购物车集合转化为页面需要的订单列表
        List<OmsCartItem> omsCartItems = cartService.cartList(memberId);
        List<OmsOrderItem> omsOrderItems = new ArrayList<>();

        for (OmsCartItem omsCartItem : omsCartItems) {
            if (omsCartItem.getIsChecked().equals("1")){
                OmsOrderItem omsOrderItem = new OmsOrderItem();
                omsOrderItem.setProductName(omsCartItem.getProductName());
                omsOrderItem.setProductPic(omsCartItem.getProductPic());
                omsOrderItem.setProductSkuId(omsCartItem.getProductSkuId());
                omsOrderItem.setProductSkuCode(omsCartItem.getProductSkuCode());
                omsOrderItem.setProductPrice(omsCartItem.getPrice()+ "");
                omsOrderItems.add(omsOrderItem);
            }
        }

        // 计算总金额
        BigDecimal allTotalPrice = cartService.getAllTotalPrice(omsCartItems);
        modelMap.put("totalAmount",allTotalPrice);
        modelMap.put("orderDetailList",omsOrderItems);

        //
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = userService.getReceiveAddressMemberId(memberId);
        modelMap.put("userAddressList",umsMemberReceiveAddresses);

        // 生成交易码，为了在提交订单时做交易码的 校验，来看用户是不是反复提交同一个订单
        String tradCode =  orderService.genTradCode(memberId);
        modelMap.put("tradeCode",tradCode);

        return "trade";
    }

    @LoginRequired(loginSuccess = true)
    @PostMapping("submitOrder")
    public String submitOrder(String receiveAddressId,BigDecimal totalAmount,String tradeCode,HttpServletRequest request, HttpServletResponse response, HttpSession session, ModelMap modelMap){
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");
        // 检查交易码
        String success =  orderService.checkTradCode(memberId,tradeCode);
        if (success.equals("success")){
            // 提交订单具体操作
            // 根据用户 id 获取要购买的商品列表（购物车）和价格

            // 将订单和定点详情写入数据库

            // 删除购物车中相关信息

            // 重定向到支付系统

        }

        return "";
    }

}
