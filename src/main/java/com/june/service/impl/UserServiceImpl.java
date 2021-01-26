package com.june.service.impl;

import com.june.dao.UserDOMapper;
import com.june.dao.UserPasswordDOMapper;
import com.june.dataobject.UserDO;
import com.june.dataobject.UserPasswordDO;
import com.june.error.BuinessException;
import com.june.error.EmBusinessError;
import com.june.service.UserService;
import com.june.service.model.UserModel;
import com.june.validator.ValidationResult;
import com.june.validator.ValidatorImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserDOMapper userDOMapper;
    @Autowired
    private UserPasswordDOMapper userPasswordDOMapper;
    @Autowired
    private ValidatorImpl validator;
    @Autowired
    private RedisTemplate redisTemplate;
    //不能有返回值 不能将数据直接返回给想要业务的代码 使用model来返回数据
    @Override
    public UserModel userGetById(Integer id) {
        UserDO userDO = userDOMapper.selectByPrimaryKey(id);
        if(userDO == null){
            return null;
        }
        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserID(userDO.getId());
        //将两个表 合成一个model
        return convertFromDataObject(userDO, userPasswordDO);
    }

    @Override
    public UserModel getUserByIdInCache(Integer id) {
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get("user_validate_"+id);
        if(userModel == null){
            userModel = userGetById(id);
            redisTemplate.opsForValue().set("user_validate_"+id,userModel);
            redisTemplate.expire("user_validate_"+id,10, TimeUnit.MINUTES);
        }
        return userModel;
    }

    @Override
    @Transactional //事务
    public void register(UserModel userModel) throws BuinessException {
        if(userModel == null){
            throw new BuinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        // 参数约束
        ValidationResult result = validator.validate(userModel);
        if (result.isHasErrors()) {
            throw new BuinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, result.getErrMsg());
        }

        UserDO userDO = convertFromModel(userModel);
        //实现Usermodel->dataobject方法
        try {
            userDOMapper.insertSelective(userDO);  //高配版insert  字段会判断空指针 java 对 null 非常脆弱
        } catch (DuplicateKeyException e) {
            throw new BuinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"手机号已被注册！"); //手机号唯一标识
        }
        userModel.setId(userDO.getId());
        UserPasswordDO userPasswordDO = convertPasswordFromModel(userModel);
        userPasswordDOMapper.insertSelective(userPasswordDO);
        return;
    }



    @Override
    public UserModel validateLogin(String telphone,String encrptPassword) throws BuinessException {
        //同过手机号获取用户信息
        UserDO userDO = userDOMapper.selectByTelphone(telphone);
        if(userDO == null){
            throw new BuinessException(EmBusinessError.USER_LOGIN_FAIL);
        }
        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserID(userDO.getId());
        UserModel userModel = convertFromDataObject(userDO, userPasswordDO);

        //比对用户信息加密的密码是否和传输进来的密码相同
        if (!encrptPassword.equals(userModel.getEncrptPassword())){
            throw new BuinessException(EmBusinessError.USER_LOGIN_FAIL);
        }
        return userModel;
    }

    private UserPasswordDO convertPasswordFromModel(UserModel userModel) {
        if (userModel == null) {
            return null;
        }
        UserPasswordDO userPasswordDO = new UserPasswordDO();
        userPasswordDO.setEncrptPassword(userModel.getEncrptPassword());
        userPasswordDO.setUserId(userModel.getId());
        return userPasswordDO;
    }
    private UserDO convertFromModel(UserModel userModel) {
        if (userModel == null) {
            return null;
        }
        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(userModel, userDO);
        return userDO;
    }
    //将两个表 合成一个model
    private UserModel convertFromDataObject(UserDO userDO, UserPasswordDO userPasswordDO) {
        if (userDO == null) {
            return null;
        }

        UserModel userModel = new UserModel();
        BeanUtils.copyProperties(userDO, userModel);

        if (userPasswordDO != null) {
            userModel.setEncrptPassword(userPasswordDO.getEncrptPassword());
        }

        return userModel;
    }
}
