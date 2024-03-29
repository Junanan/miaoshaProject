package com.june.service;

import com.june.error.BuinessException;
import com.june.service.model.UserModel;

public interface UserService {
    //通过用户ID获取用户对象的方法
    UserModel userGetById(Integer id);

    //通过缓存获取对象
    UserModel getUserByIdInCache(Integer id);

    void register(UserModel userModel) throws BuinessException;

    /*
   telphone:用户注册手机
   password:用户加密后的密码
    */
    UserModel validateLogin(String telphone,String encrptPassword) throws BuinessException;

}
