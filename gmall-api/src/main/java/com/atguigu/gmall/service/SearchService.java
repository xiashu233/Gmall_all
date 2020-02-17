package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PmsSearchSkuInfo;
import com.atguigu.gmall.bean.PmsSerachParam;

import java.io.IOException;
import java.util.List;

public interface SearchService {
    List<PmsSearchSkuInfo> list(PmsSerachParam pmsSerachParam) throws IOException;
}
