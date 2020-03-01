package com.atguigu.gmall.passport;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.util.HttpclientUtil;
import org.apache.http.client.utils.HttpClientUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPassportWebApplicationTests {

    @Test
    public void contextLoads() {




    }

    public String getCode(){
        // 第三方授权地址 https://api.weibo.com/oauth2/authorize?client_id=4140827957&response_type=code&redirect_uri=http://passport.gmall.com:8085/vlogin
        // 1. 利用地址来让用户登录 返回 code 授权码
        String code1 = HttpclientUtil.doGet("https://api.weibo.com/oauth2/authorize?client_id=4140827957&response_type=code&redirect_uri=http://passport.gmall.com:8085/vlogin");
        System.out.println(code1);
        // 2. 授权码回调给本地服务
        String code2 = HttpclientUtil.doGet("http://passport.gmall.com:8085/vlogin?code=e48bb99a44b89925ba46cd786fb36935");
        System.out.println(code2);
        return code2;
    }

    public String getAccess_token(){
        // 3. 通过得到的 授权码 去交换 access_token
        //
        Map<String,String> map = new HashMap<>();
        map.put("client_id","4140827957");
        map.put("client_secret","004e4ce4de694a4246055884b4054a83");
        map.put("grant_type","authorization_code");
        map.put("redirect_uri","http://passport.gmall.com:8085/vlogin");
        map.put("code","e48bb99a44b89925ba46cd786fb36935");
        String access_tokenJson = HttpclientUtil.doPost("https://api.weibo.com/oauth2/access_token?client_id=4140827957&client_secret=004e4ce4de694a4246055884b4054a83&grant_type=authorization_code&redirect_uri=http://passport.gmall.com:8085/vlogin&code=e48bb99a44b89925ba46cd786fb36935", map);
        Map<String, String> resultMap = JSON.parseObject(access_tokenJson, Map.class);
        return resultMap.get("access_token");
    }

    public Map<String,String> getUserInfo(){
        // 4. 用 access_token 交换用户信息
        String user_Json = HttpclientUtil.doGet("https://api.weibo.com/2/users/show.json?access_token=2.00SJ1pCGhVUOWE7ae192c5e0NAI6pC&uid=5538581848");
        Map<String,String> userInfo = JSON.parseObject(user_Json,Map.class);
        return userInfo;
    }



}
