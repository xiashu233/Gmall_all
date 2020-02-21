package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.SpuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
//@CrossOrigin
public class ItemController {

    @Reference
    SkuService skuService;
    @Reference
    SpuService spuService;

    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId, Model model, HttpServletRequest request){
        // 获取请求 ip
        String remoteAddr = request.getRemoteAddr();
        request.getHeader(""); // nginx 负载均衡时用此方法获得请求地址
//        System.out.println(remoteAddr);
        // sku对应的信息
        PmsSkuInfo pmsSkuInfo = skuService.getSkuInfoBySkuId(skuId,remoteAddr);
        model.addAttribute("skuInfo",pmsSkuInfo);

        // 销售属性信息
        List<PmsProductSaleAttr> pmsProductSaleAttrs = spuService.spuSaleAttrListCheckBySku(pmsSkuInfo.getProductId(),pmsSkuInfo.getId());
        model.addAttribute("spuSaleAttrListCheckBySku",pmsProductSaleAttrs);

        // 查询当前 sku 的 spu 的其他 sku 的集合
        List<PmsSkuInfo> pmsSkuInfos = spuService.getSkuSaleAttrValeBySpu(pmsSkuInfo.getProductId());
        Map<String,String> skuSaleAttrHash = new HashMap<>();
        for (PmsSkuInfo skuInfo : pmsSkuInfos) {
            String k = "";
            String v = skuInfo.getId();

            List<PmsSkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
            for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
                k += pmsSkuSaleAttrValue.getSaleAttrValueId() + "|";
            }
            skuSaleAttrHash.put(k,v);
        }
        String skuSaleAttrHashJsonStr = JSON.toJSONString(skuSaleAttrHash);
        model.addAttribute("skuSaleAttrHashJsonStr",skuSaleAttrHashJsonStr);

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
