package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface UserService {
    List<UmsMember> getAllUser();

    List<UmsMemberReceiveAddress> getReceiveAddressMemberId(String memberId);

    UmsMember login(UmsMember umsMember);

    void addUserToken(String token, String id);

    UmsMember addOauthUser(UmsMember umsMember);

    UmsMember checkOauthUser(UmsMember umsCheckUser);

    UmsMemberReceiveAddress getReceiveAddressById(String receiveAddressId);
}
