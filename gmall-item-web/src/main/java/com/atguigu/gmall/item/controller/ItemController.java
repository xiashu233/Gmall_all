package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.SkuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
//@CrossOrigin
public class ItemController {

    @Reference
    SkuService skuService;

    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId,Model model){
        PmsSkuInfo pmsSkuInfo = skuService.getSkuInfoBySkuId(skuId);
        model.addAttribute("skuInfo",pmsSkuInfo);
        return "item";
    }

    @RequestMapping("index")
    public String index(Model model){

        List<Integer> ints = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ints.add(i);
        }

        model.addAttribute("welcome","welcome to thymeleaf page!!!");
        model.addAttribute("ints",ints);
        model.addAttribute("check",true);
        model.addAttribute("hello","原来是你喔");
        return "index";
    }
}
