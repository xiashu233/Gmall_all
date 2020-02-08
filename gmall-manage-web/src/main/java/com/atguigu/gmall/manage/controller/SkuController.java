package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.SkuService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@CrossOrigin
public class SkuController {

    @Reference
    SkuService skuService;

    @ResponseBody
    @RequestMapping("/saveSkuInfo")
    public String saveSkuInfo(@RequestBody PmsSkuInfo pmsSkuInfo){
        pmsSkuInfo.setProductId(pmsSkuInfo.getSpuId());

        // 处理用户没有选择默认图片时 将默认图片设置成第一张图片
        String skuDefaultImg = pmsSkuInfo.getSkuDefaultImg();
        if (StringUtils.isBlank(skuDefaultImg)){
            pmsSkuInfo.setSkuDefaultImg(pmsSkuInfo.getSkuImageList().get(0).getImgUrl());
        }

        String success = skuService.saveSkuInfo(pmsSkuInfo);
        return success;
    }
}
