package com.atguigu.gmall.passport.cotroller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PassPortController {

    @GetMapping("index")
    public String index(){
        return "index";
    }
}
