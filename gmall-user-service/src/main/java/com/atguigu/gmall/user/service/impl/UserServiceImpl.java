package com.atguigu.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.atguigu.gmall.user.mapper.UserMeberMapper;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMeberMapper userMeberMapper;
    @Autowired
    UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;
    @Autowired
    RedisUtil redisUtil;

    @Override
    public List<UmsMember> getAllUser() {

        List<UmsMember> umsMemberList = userMeberMapper.selectAll();
        return umsMemberList;
    }

    @Override
    public List<UmsMemberReceiveAddress> getReceiveAddressMemberId(String memberId) {

        // 条件筛选
        Example e = new Example(UmsMemberReceiveAddress.class);
        e.createCriteria().andEqualTo("memberId", memberId);
        List<UmsMemberReceiveAddress> umsMemberReceiveAddressList = umsMemberReceiveAddressMapper.selectByExample(e);

//        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
//        umsMemberReceiveAddress.setMemberId(memberId);
//        List<UmsMemberReceiveAddress> umsMemberReceiveAddressList = umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);

        return umsMemberReceiveAddressList;
    }

    @Override
    public UmsMember login(UmsMember umsMember) {
        Jedis jedis = null;
        try {
//            String password = jedis.get("user:" + umsMember.getUsername() + ":password");
//            if (umsMember.getPassword().equals(password)){
//            }

            jedis = redisUtil.getJedis();
            if (jedis != null) {
                String umsMemberStr = jedis.get("user:" + umsMember.getUsername() + umsMember.getPassword() + ":info");
                if (StringUtils.isNotBlank(umsMemberStr)) {
                    // 验证密码正确 并获得了用户信息
                    UmsMember umsMemberJson = JSON.parseObject(umsMemberStr, UmsMember.class);
                    return umsMemberJson;
                }
            }

            // 验证密码失败
            UmsMember umsMemberDB = loginFromDB(umsMember);
            if (umsMemberDB != null) {
                String umsMeberDBJson = JSON.toJSONString(umsMemberDB);
                jedis.setex("user:" + umsMemberDB.getUsername() + umsMemberDB.getPassword() + ":info", 60 * 60 * 24, umsMeberDBJson);
            }
            return umsMemberDB;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }

        return null;
    }

    @Override
    public void addUserToken(String token, String id) {
        Jedis jedis = redisUtil.getJedis();
        jedis.setex("user:"+id+":token",60*60*2,token);
        jedis.close();
    }

    private UmsMember loginFromDB(UmsMember umsMember) {
        UmsMember umsMemberDB = userMeberMapper.select(umsMember).get(0);
        return umsMemberDB;
    }
}
