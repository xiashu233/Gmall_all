package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PmsSkuInfo;

public interface SkuService {
    String saveSkuInfo(PmsSkuInfo pmsSkuInfo);

    PmsSkuInfo getSkuInfoBySkuId(String skuId,String ip);
}
