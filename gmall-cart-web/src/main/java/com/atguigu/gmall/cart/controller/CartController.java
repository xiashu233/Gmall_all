package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class CartController {

    @Reference
    SkuService skuService;
    @Reference
    CartService cartService;

    @LoginRequired(loginSuccess = false)
    @RequestMapping("addToCart")
    public String addToCart(String skuId, int quantity, HttpServletRequest request, HttpServletResponse response){

        // 购物车集合
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        // 调用商品服务获取商品信息
        PmsSkuInfo skuInfo = skuService.getSkuInfoBySkuId(skuId, "");
        // 将商品信息封装成购物车信息
        OmsCartItem omsCartItem = skuInfoToOmsCart(skuInfo, quantity);

        // 判断用户是否登录
        String memberId = "1";

        if (StringUtils.isBlank(memberId)){
            // 用户没登录

            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie",true);

            // 判断用户 购物车cookie 是否为空
            if (StringUtils.isBlank(cartListCookie)){
                omsCartItems.add(omsCartItem);
            }else{
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);

                int exist = if_cart_exist(omsCartItems,omsCartItem);

                if (exist != -1){
                    // 购物车之前有该商品记录 则更新购物车
                    omsCartItems.get(exist).setQuantity(omsCartItems.get(exist).getQuantity().add(omsCartItem.getQuantity()));
//                  cartItem.setPrice(cartItem.getPrice().add(omsCartItem.getPrice()));

                }else{
                    // 购物车之前没有该商品记录 添加商品到购物车
                    omsCartItems.add(omsCartItem);

                }
            }

            // 更新 Cookie
            CookieUtil.setCookie(request,response,"cartListCookie", JSON.toJSONString(omsCartItems),60*60*72,true);
        }else{
            // 用户登录
            OmsCartItem omsCartItemFromDb = cartService.ifCartExistByUser(memberId,skuId);

           if (omsCartItemFromDb == null){
               // 该用户没有添加过当前商品
               omsCartItem.setMemberId(memberId);
               omsCartItem.setQuantity(new BigDecimal(quantity));
               omsCartItem.setMemberNickname("储木杉");
               omsCartItem.setProductSkuId(skuId);
               cartService.addCart(omsCartItem);
           }else{
               // 用户添加过当前商品
               omsCartItemFromDb.setQuantity(omsCartItemFromDb.getQuantity().add(new BigDecimal(quantity)) );
               cartService.updateCart(omsCartItemFromDb);
           }

           // 同步到 redis 缓存
            cartService.flushCartCache(memberId);

        }
        return "redirect:/success.html";
    }

    @LoginRequired(loginSuccess = false)
    @GetMapping("cartList")
    public String cartList(HttpServletRequest request, HttpServletResponse response,Model model){

        // 购物车集合
        List<OmsCartItem> omsCartItems = new ArrayList<>();

        String memberId = "1";

        if (StringUtils.isBlank(memberId)){
            // 用户未登录 查询 cookie
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie",true);
            if (StringUtils.isNotBlank(cartListCookie)){
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
            }

        }else{
            // 用户登录状态
            omsCartItems = cartService.cartList(memberId);
        }

        BigDecimal allTotalPrice = new BigDecimal("0.0");
        for (OmsCartItem omsCartItem : omsCartItems) {
            omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));
            allTotalPrice = allTotalPrice.add(omsCartItem.getTotalPrice());
        }

        model.addAttribute("allTotalPrice",allTotalPrice);
        model.addAttribute("cartList",omsCartItems);
        
        return "cartList";
    }

    @LoginRequired(loginSuccess = false)
    @PostMapping("checkCart")
    public String checkCart(String isChecked,String skuId,Model model){

        String memberId = "1";
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setIsChecked(isChecked);
        omsCartItem.setMemberId(memberId);
        omsCartItem.setProductSkuId(skuId);
        // 调用服务 修改状态
        cartService.checkCart(omsCartItem);
        // 将最新的从缓存中
        List<OmsCartItem> omsCartItems = cartService.cartList(memberId);


        model.addAttribute("allTotalPrice",getAllTotalPrice(omsCartItems));
        model.addAttribute("cartList",omsCartItems);

        return "cartListInner";
    }

    // 结算功能 toTrade
    @LoginRequired(loginSuccess = true)
    @ResponseBody
    @GetMapping("toTrade")
    public String toTrade(){

        return "从购物车到结算页面";
    }

    private BigDecimal getAllTotalPrice(List<OmsCartItem> omsCartItems) {
        BigDecimal allTotalPrice = new BigDecimal("0.0");
        for (OmsCartItem omsCartItemd : omsCartItems) {
            if (omsCartItemd.getIsChecked().equals("1")){
                allTotalPrice = allTotalPrice.add(omsCartItemd.getTotalPrice());
            }
        }
        return allTotalPrice;
    }

//    private boolean if_cart_exist(List<OmsCartItem> omsCartItems, OmsCartItem omsCartItem) {
//        for (OmsCartItem cartItem : omsCartItems) {
//            if (cartItem.getProductId().equals(omsCartItem.getProductId())){
//                return true;
//            }
//        }
//        return false;
//    }

    // 对 if_cart_exist 进行改写 不仅能判断是否在其中还能确定其位置
    private int if_cart_exist(List<OmsCartItem> omsCartItems, OmsCartItem omsCartItem) {
        int index = -1;
        for (int node = 0; node < omsCartItems.size(); node++) {
            if (omsCartItems.get(node).getProductId().equals(omsCartItem.getProductId())){
                index = node;
            }
        }
        return index;
    }


    private OmsCartItem skuInfoToOmsCart(PmsSkuInfo skuInfo,int quantity){
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setCreateDate(new Date());
        omsCartItem.setDeleteStatus(0);
        omsCartItem.setModifyDate(new Date());
        omsCartItem.setPrice(skuInfo.getPrice());
        omsCartItem.setProductAttr("");
        omsCartItem.setProductBrand("");
        omsCartItem.setProductId(skuInfo.getProductId());
        omsCartItem.setProductCategoryId(skuInfo.getCatalog3Id());
        omsCartItem.setProductName(skuInfo.getSkuName());
        omsCartItem.setProductPic(skuInfo.getSkuDefaultImg());
        omsCartItem.setQuantity(new BigDecimal(quantity));
        omsCartItem.setProductSkuCode("111");
        return omsCartItem;
    }
}
