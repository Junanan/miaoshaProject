package com.june.service.impl;

import com.june.dao.PromoDOMapper;
import com.june.dataobject.PromoDO;
import com.june.service.ItemService;
import com.june.service.PromoService;
import com.june.service.UserService;
import com.june.service.model.ItemModel;
import com.june.service.model.PromoModel;
import com.june.service.model.UserModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.xml.crypto.Data;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PromoServiceImpl implements PromoService {
    @Autowired
    private PromoDOMapper promoDOMapper;
    @Autowired
    private ItemService itemService;
    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);

        //dataObject -> promoModel
        PromoModel promoModel = convertFromDataObject(promoDO);

        //判断当前时间是否是秒杀活动即将开始或进行
        if (promoModel.getStartDate().isAfterNow()){ //该函数返回布尔值，该值显示当前时间是否晚于指定的日期时间
            promoModel.setStatus(1);
        }else if (promoModel.getEndDate().isBeforeNow()){
            promoModel.setStatus(3);
        }else {
            promoModel.setStatus(2);
        }
        return promoModel;
    }

    @Override
    public void publishPromo(Integer promoId) {
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        if (promoDO.getItemId() == null || promoDO.getItemId().intValue() == 0){
            return ;
        }
        ItemModel itemModel = itemService.getItemById(promoDO.getItemId());
        //将库存同步到redis内
        redisTemplate.opsForValue().set("promo_item_stock_"+itemModel.getId(),itemModel.getStock().intValue());

        //将大闸的限制数字设置到redis内
        redisTemplate.opsForValue().set("promo_door_count_"+promoId,itemModel.getStock().intValue() * 5);
    }

    @Override
    public String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId) {
        //判断是否库存已售馨，若对应的售馨key存在，则直接返回下单失败
        if (redisTemplate.hasKey("promo_item_stock_invalid_"+itemId)){
            return null;
        }

        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);

        //promoDo(dataObject) -> PromoModel
        PromoModel promoModel = convertFromDataObject(promoDO);
        if(promoModel == null) {
            return null;
        }
        //判断当前时间是否秒杀活动即将开始或正在进行
        DateTime now = new DateTime();
        if(promoModel.getStartDate().isAfterNow()) {
            promoModel.setStatus(1);
        }else if(promoModel.getEndDate().isBeforeNow()) {
            promoModel.setStatus(3);
        }else {
            promoModel.setStatus(2);
        }
        //判断活动是否正在进行
        if(promoModel.getStatus()!=2){
            return null;
        }

        //判断item信息是否存在
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if(itemModel == null) {
            return null;
        }
        //判断用户信息是否存在
        UserModel userModel = userService.getUserByIdInCache(userId);
        if(userModel == null) {
            return null;
        }

        long result = redisTemplate.opsForValue().increment("promo_door_count_"+promoId,-1);
        if (result < 0){
            return null;
        }
        //生成token并且存入redis设置5分钟有效期
        String token = UUID.randomUUID().toString().replace("-","");
        redisTemplate.opsForValue().set("promo_token_"+promoId+"_userid_"+userId+"_itemid_"+itemId,token);
        redisTemplate.expire("promo_token_"+promoId+"_userid_"+userId+"_itemid_"+itemId,5, TimeUnit.MINUTES);

        return token;
    }

    private PromoModel convertFromDataObject(PromoDO promoDO){
        if (promoDO == null){
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDO,promoModel);
        promoModel.setStartDate(new DateTime(promoDO.getStartDate()));
        promoModel.setEndDate(new DateTime(promoDO.getEndDate()));
        return promoModel;
    }
}
