package com.june.service;

import com.june.error.BuinessException;
import com.june.service.model.OrderModel;

public interface OrderService {
//    通过前端url上传过来秒杀活动id,然后下单接口内校验对应的id是否属于对应商品且活动已开始
    OrderModel createOrder(Integer userId,Integer itemId,Integer promoId,Integer amount,String stockLogId) throws BuinessException;


}
