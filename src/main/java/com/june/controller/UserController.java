package com.june.controller;

import com.june.controller.viewobject.UserVO;
import com.june.error.BuinessException;
import com.june.error.EmBusinessError;
import com.june.response.CommonReturnType;
import com.june.service.UserService;
import com.june.service.model.UserModel;
import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Controller("user")
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*") //跨域请求
public class UserController extends BaseController {
    @Autowired
    private UserService userService;
    @Autowired
    private HttpServletRequest httpServletRequest;
    @Autowired
    private RedisTemplate redisTemplate;
    //用户登录接口
    @RequestMapping(value = "/login",method = RequestMethod.POST,consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType login(@RequestParam("telphone")String telphone,
                                     @RequestParam("password")String password) throws BuinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
        //入参校验
        if(com.alibaba.druid.util.StringUtils.isEmpty(telphone)){
            throw new BuinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }
        //用户登录服务，用来校验用户登录是否合法
        UserModel userModel = userService.validateLogin(telphone, this.encodeByMd5(password));
        //将登陆凭证加入到用户登陆成功的session内

        //修改成若用户登录验证成功后将对应的登录信息和登录凭证一起存入redis中

        //生成登录凭证token，UUID
        String uuidToken = UUID.randomUUID().toString();
        uuidToken = uuidToken.replace("-","");
        //建议token和用户登陆态之间的联系
        redisTemplate.opsForValue().set(uuidToken,userModel);
        redisTemplate.expire(uuidToken,1, TimeUnit.HOURS);
//        httpServletRequest.getSession().setAttribute("IS_LOGIN",true);
//        httpServletRequest.getSession().setAttribute("LOGIN_USER",userModel);
        //下发了token
        return CommonReturnType.create(uuidToken);//返回 成功信息
    }
    //用户注册接口 consumes = {CONTENT_TYPE_FORMED} 设置ajax
    @RequestMapping(value = "/register",method = RequestMethod.POST,consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType register(@RequestParam("telphone")String telphone,
                                     @RequestParam("otpCode")String otpCode,
                                     @RequestParam("name")String name,
                                     @RequestParam("gender")Integer gender,
                                     @RequestParam("age")Integer age,
                                     @RequestParam("password")String password) throws BuinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
        //验证手机号和对应的otpCode
        String inSessionOtpCode = (String)this.httpServletRequest.getSession().getAttribute(telphone);
        if(!com.alibaba.druid.util.StringUtils.equals(otpCode,inSessionOtpCode)){
            throw new BuinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"短信验证不通过");
        }
        //用户注册流程
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setTelphone(telphone);
        userModel.setGender(new Byte(String.valueOf(gender.intValue())));
        userModel.setAge(age);
        userModel.setRegisterMode("byphone");
        userModel.setEncrptPassword(this.encodeByMd5(password));
        userService.register(userModel);
        return CommonReturnType.create(null);
    }
    public String encodeByMd5(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        //确定计算方法
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        BASE64Encoder base64en = new BASE64Encoder();
        //加密字符串
        String newstr = base64en.encode(md5.digest(str.getBytes("utf-8")));
        return newstr;
    }

    //用户短信接口
    @RequestMapping(value = "/getotp",method = RequestMethod.POST,consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam("telphone")String telphone){
        //按照一定规则生成OTP验证码

        Random random = new Random();
        int nextInt = random.nextInt(99999);//[0,99999]
        nextInt+=10000;//[10000,99999];
        String otpCode = String.valueOf(nextInt);

        //将OTP验证码同对应用户的手机号关联  (redis 处理 其有天然的键值对)
        httpServletRequest.getSession().setAttribute(telphone,otpCode);
        //将OTP验证通过手机短信发送给用户 （省略）
        System.out.println("telephone="+telphone+"  &otpCode="+otpCode);

        return CommonReturnType.create(null);
    }
    @RequestMapping("/get")
    @ResponseBody
    public CommonReturnType getUser(@RequestParam("id") Integer id) throws BuinessException {
        UserModel userModel = userService.userGetById(id);
        //如果获取对应的用户信息不存在
        if( userModel == null){
            throw new BuinessException(EmBusinessError.USER_NOT_EXIT);
        }

        //将核心领域模型转换用户对象转换为可供UI使用的viewobject
        UserVO userVO = convertFromModel(userModel);
        return CommonReturnType.create(userVO);
    }

    private UserVO convertFromModel(UserModel userModel){
        if (userModel == null){
            return null;
        }
        UserVO userVO = new UserVO();
        //将userModel属性复制到userVO中（只复制userVO有的）
        BeanUtils.copyProperties(userModel,userVO);
        return userVO;
    }


}

