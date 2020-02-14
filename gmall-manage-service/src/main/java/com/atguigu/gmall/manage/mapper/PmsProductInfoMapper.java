package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.PmsProductInfo;
import com.atguigu.gmall.bean.PmsSkuInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface PmsProductInfoMapper extends Mapper<PmsProductInfo> {
    List<PmsSkuInfo> selectSkuSaleAttrValeBySpu(String productId);
}
