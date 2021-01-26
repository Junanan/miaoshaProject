package com.june.service.model;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class OrderModel {
    //2020080912356
    private  String id;
    //购买用户ID
    private Integer userId;
    //购买商品ID
    private Integer itemId;
    //购买数量
    private Integer amount;
    //若非空，则表示是以秒杀商品方式下单
    private Integer promoId;
    //购买商品单价,若非空，则表示是以秒杀商品方式下单
    private BigDecimal itemPrice;
    //购买金额,若非空，则表示是以秒杀商品方式下单
    private BigDecimal orderPrice;
}
