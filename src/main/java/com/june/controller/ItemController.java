package com.june.controller;

import com.june.controller.viewobject.ItemVO;
import com.june.error.BuinessException;
import com.june.response.CommonReturnType;
import com.june.service.CacheService;
import com.june.service.ItemService;
import com.june.service.PromoService;
import com.june.service.model.ItemModel;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller("item")
@RequestMapping("/item")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*") //跨域请求
public class ItemController extends BaseController {
    @Autowired
    private ItemService itemService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private PromoService promoService;
    //创建商品
    @RequestMapping(value = "/create", method = RequestMethod.POST, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createItem(@RequestParam("title") String title,
                                       @RequestParam("description") String description,
                                       @RequestParam("price") BigDecimal price,
                                       @RequestParam("stock") Integer stock,
                                       @RequestParam("imgUrl") String imgUrl) throws BuinessException {
        ItemModel itemModel = new ItemModel();
        itemModel.setTitle(title);
        itemModel.setPrice(price);
        itemModel.setDescription(description);
        itemModel.setStock(stock);
        itemModel.setImgUrl(imgUrl);
        ItemModel item = itemService.createItem(itemModel);
        ItemVO itemVO = convertVOFromItemModel(item);
        return CommonReturnType.create(itemVO);
    }
    @RequestMapping(value = "/publishpromo", method = RequestMethod.GET)
    @ResponseBody
    public CommonReturnType publishPromo(@RequestParam("id") Integer id) {
        promoService.publishPromo(id);
        return CommonReturnType.create(null);
    }
    @RequestMapping(value = "/get", method = RequestMethod.GET)
    @ResponseBody
    public CommonReturnType getItem(@RequestParam("id") Integer id) {
        ItemModel  itemModel = null;
        //根据本地缓存去model
        itemModel = (ItemModel) cacheService.getFromCommonCache("item_"+id);
        //如果本地缓存没有model
        if (itemModel == null){
            //根据商品的id获取到redis内的model
            itemModel = (ItemModel) redisTemplate.opsForValue().get("item_"+id);
            //如果没有则往下游service获取
            if (itemModel == null){
                itemModel = itemService.getItemById(id);
                //设置itemModel到redis内
                redisTemplate.opsForValue().set("item_"+id,itemModel);
                //设置失效时间
                redisTemplate.expire("item_"+id,10, TimeUnit.MINUTES);
            }
            //将model放入本地缓存内
            cacheService.setCommonCache("item_"+id,itemModel);
        }

        ItemVO itemVO = convertVOFromItemModel(itemModel);
        return CommonReturnType.create(itemVO);
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public CommonReturnType listItem() {
        List<ItemModel> itemModelList = itemService.listItem();
        //使用stream api 将list 内的 itemmodel转化为 itemvo
        List<ItemVO> itemVOList = itemModelList.stream().map(itemModel -> {
            ItemVO itemVO = convertVOFromItemModel(itemModel);
            return itemVO;
        }).collect(Collectors.toList());
        return CommonReturnType.create(itemVOList);
    }

    public ItemVO convertVOFromItemModel(ItemModel itemModel) {
        if (itemModel == null) {
            return null;
        }
        ItemVO itemVO = new ItemVO();
        BeanUtils.copyProperties(itemModel, itemVO);
        if (itemModel.getPromoModel()!= null){
            //不为空即表示有正在进行或即将开始的活动
            itemVO.setPromoId(itemModel.getPromoModel().getId());
            itemVO.setPromoPrice(itemModel.getPromoModel().getPromoItemPrice());
            itemVO.setStartDate(itemModel.getPromoModel().getStartDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
            itemVO.setPromoStatus(itemModel.getPromoModel().getStatus());
        }else{
            itemVO.setPromoStatus(0);
        }
        return itemVO;

    }

}
