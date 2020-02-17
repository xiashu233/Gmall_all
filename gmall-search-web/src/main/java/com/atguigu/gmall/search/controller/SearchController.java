package com.atguigu.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsSearchSkuInfo;
import com.atguigu.gmall.bean.PmsSerachParam;
import com.atguigu.gmall.service.SearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.util.List;


@Controller
public class SearchController {

    @Reference
    SearchService searchService;

    @RequestMapping("index")
    public String index(){
        return "index";
    }

    @RequestMapping("list.html")
    public String list(PmsSerachParam pmsSerachParam, Model model) throws IOException {
        //调用搜索服务 返回搜索结果
        List<PmsSearchSkuInfo> pmsSearchSkuInfos = searchService.list(pmsSerachParam);
        model.addAttribute("skuLsInfoList",pmsSearchSkuInfos);
        model.addAttribute("keyword",pmsSerachParam.getKeyword());


        return "list";
    }
}
