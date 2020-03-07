package com.atguigu.gmall.payment.controller;

import com.atguigu.gmall.annotations.LoginRequired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

@Controller
public class paymentController {

    @RequestMapping("index")
    @LoginRequired(loginSuccess = true)
    public String index(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap modelMap){
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        modelMap.put("orderId",outTradeNo);
        modelMap.put("totalAmount",totalAmount);
        modelMap.put("nickName",nickname);


        return "index";
    }
}
