package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.SimpleFormatter;

@Controller
public class OrderController {

    @Reference
    CartService cartService;
    @Reference
    UserService userService;
    @Reference
    OrderService orderService;
    @Reference
    SkuService skuService;

    // 结算功能 toTrade
    @LoginRequired(loginSuccess = true)
    @GetMapping("toTrade")
    public String toTrade(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap){
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        // 将购物车集合转化为页面需要的订单列表
        List<OmsCartItem> omsCartItems = cartService.cartList(memberId);
        List<OmsOrderItem> omsOrderItems = new ArrayList<>();

        cartsToOrders(omsOrderItems, omsCartItems);

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
    public ModelAndView submitOrder(String receiveAddressId, BigDecimal totalAmount, String tradeCode, HttpServletRequest request, HttpServletResponse response, HttpSession session, ModelMap modelMap){
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");
        // 检查交易码
        String success =  orderService.checkTradCode(memberId,tradeCode);
        if (success.equals("success")){
            List<OmsOrderItem> omsOrderItems = new ArrayList<>();
            OmsOrder omsOrder = new OmsOrder();
            // 自动确认收货时间
            omsOrder.setAutoConfirmDay(15);
            omsOrder.setCreateTime(new Date());
            omsOrder.setDiscountAmount(null);
            omsOrder.setMemberId(memberId);
            omsOrder.setMemberUsername(nickname);
            omsOrder.setModifyTime(new Date());
            omsOrder.setNote("订单备注：xxxxxx");

            // 设置外部订单号，用来与外部系统交互，防止重复
            String outTradeNo = "gmall";
            // 添加一个时间戳
            outTradeNo += System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("YYMMDDHHmmss");
            String date = sdf.format(new Date());
            outTradeNo += date;

            omsOrder.setOrderSn(outTradeNo);
            omsOrder.setPayAmount(totalAmount);
            omsOrder.setOrderType(1);

            UmsMemberReceiveAddress address = userService.getReceiveAddressById(receiveAddressId);
            omsOrder.setReceiverCity(address.getCity());
            omsOrder.setReceiverDetailAddress(address.getDetailAddress());
            omsOrder.setReceiverPhone(address.getPhoneNumber());
            omsOrder.setReceiverName(address.getName());
            omsOrder.setReceiverProvince(address.getProvince());
            omsOrder.setReceiverPostCode(address.getPostCode());
            omsOrder.setReceiverRegion(address.getRegion());

            Calendar calendar = Calendar.getInstance();
            calendar.add(calendar.DATE,1);
            Date today = calendar.getTime();
            omsOrder.setReceiveTime(today);
            omsOrder.setSourceType(0);
            omsOrder.setStatus(1);
            omsOrder.setTotalAmount(totalAmount);
            // 提交订单具体操作
            // 根据用户 id 获取要购买的商品列表（购物车）和价格

            // 检验价格，库存和相关信息是否合法
            List<OmsCartItem> omsCartItems = cartService.cartList(memberId);

            for (OmsCartItem omsCartItem : omsCartItems) {
                if (omsCartItem.getIsChecked().equals("1")){
                    // 获得订单详情列表
                    OmsOrderItem omsOrderItem = new OmsOrderItem();

                    // 检验库存，远程调用库存系统
                    boolean result =  skuService.checkPrice(omsCartItem.getProductSkuId(),omsCartItem.getPrice());
                    if (result == false){
                        ModelAndView mv = new ModelAndView("tradeFail");
                        return mv;
                    }
                    omsOrderItem.setProductId(omsCartItem.getProductId());
                    omsOrderItem.setProductName(omsCartItem.getProductName());
                    omsOrderItem.setProductPic(omsCartItem.getProductPic());
                    omsOrderItem.setProductSkuId(omsCartItem.getProductSkuId());
                    omsOrderItem.setProductSkuCode(omsCartItem.getProductSkuCode());
                    omsOrderItem.setProductPrice(omsCartItem.getPrice()+ "");
                    omsOrderItem.setProductCategoryId(omsCartItem.getProductCategoryId());

                    omsOrderItem.setOrderSn(outTradeNo);

                    omsOrderItem.setRealAmount(omsCartItem.getTotalPrice()+"");
                    omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                    // 设置仓库对应的商品编号
                    omsOrderItem.setProductSn("");
                    omsOrderItems.add(omsOrderItem);


                }

            }

            omsOrder.setOmsOrderItems(omsOrderItems);

            // 将订单和订单详情写入数据库
            orderService.saveOrder(omsOrder);
            // 删除购物车中相关信息

            // 重定向到支付系统
            ModelAndView mv = new ModelAndView("redirect:http://payment.gmall.com:8087/index");
            mv.addObject("outTradeNo",outTradeNo);
            mv.addObject("totalAmount",totalAmount);
            return mv;
        }else{
            ModelAndView mv = new ModelAndView("tradeFail");
            return mv;
        }

    }

    private void cartsToOrders(List<OmsOrderItem> omsOrderItems, List<OmsCartItem> omsCartItems) {
        for (OmsCartItem omsCartItem : omsCartItems) {
            if (omsCartItem.getIsChecked().equals("1")){
                // 获得订单详情列表
                OmsOrderItem omsOrderItem = new OmsOrderItem();
                omsOrderItem.setProductName(omsCartItem.getProductName());
                omsOrderItem.setProductPic(omsCartItem.getProductPic());
                omsOrderItem.setProductSkuId(omsCartItem.getProductSkuId());
                omsOrderItem.setProductSkuCode(omsCartItem.getProductSkuCode());
                omsOrderItem.setProductPrice(omsCartItem.getPrice()+ "");
                omsOrderItems.add(omsOrderItem);


            }

        }
    }

}
