package com.june.controller.viewobject;

import lombok.Data;
import org.joda.time.DateTime;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
@Data
public class ItemVO {
    private Integer id;
    //商品名称
    private String title;
    //商品价格
    private BigDecimal price;
    //商品库存
    private Integer stock;
    //商品销量
    private Integer sales;
    //商品描述
    private String description;
    //商品描述图片的url;
    private String imgUrl;
    //活动的状态 0没有活动 1活动即将开始 2正在将进行中
    private Integer promoStatus;
    //秒杀价格
    private BigDecimal promoPrice;
    //秒杀活动id
    private Integer promoId;
    //秒杀活动开始时间 用于倒计时
    private String startDate;
}
