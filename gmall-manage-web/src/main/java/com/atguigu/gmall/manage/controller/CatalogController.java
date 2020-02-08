package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsBaseCatalog1;
import com.atguigu.gmall.bean.PmsBaseCatalog2;
import com.atguigu.gmall.bean.PmsBaseCatalog3;
import com.atguigu.gmall.service.CatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
// 解决跨域问题
@CrossOrigin
public class CatalogController {

    @Reference
    CatalogService catalogService;


    @ResponseBody
    @RequestMapping("/getCatalog1")
    public List<PmsBaseCatalog1> getCatalog1(){
        List<PmsBaseCatalog1> catalog1s = catalogService.getCatalog1();
        return catalog1s;

    }

    @ResponseBody
    @RequestMapping("/getCatalog2")
    public List<PmsBaseCatalog2> getCatalog2(String catalog1Id){
        List<PmsBaseCatalog2> catalog2s = catalogService.getCatalog2(catalog1Id);
        return catalog2s;
    }

    @ResponseBody
    @RequestMapping("/getCatalog3")
    public List<PmsBaseCatalog3> getCatalog3(String catalog2Id){
        List<PmsBaseCatalog3> catalog3s = catalogService.getCatalog3(catalog2Id);
        return catalog3s;
    }






}
