package com.atguigu.gmall.user.mapper;

import com.atguigu.gmall.bean.UmsMember;
import tk.mybatis.mapper.common.Mapper;

// 继承通用 Mapper 实现单表的增删改查（省去写一些 mapper.xml 的语句了）
public interface UserMapper extends Mapper<UmsMember> {

}
