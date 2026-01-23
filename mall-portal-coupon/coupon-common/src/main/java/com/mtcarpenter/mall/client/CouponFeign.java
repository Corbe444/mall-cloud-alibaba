package com.mtcarpenter.mall.client;

import com.mtcarpenter.mall.common.api.CommonResult;
import com.mtcarpenter.mall.domain.CartPromotionItem;
import com.mtcarpenter.mall.domain.SmsCouponHistoryDetail;
import com.mtcarpenter.mall.model.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * @author mtcarpenter
 * @github https://github.com/mtcarpenter/mall-cloud-alibaba
 * @desc 微信公众号：山间木匠
 */
@FeignClient(name = "mall-portal-coupon", path = "coupon")
public interface CouponFeign {

    @PostMapping(value = "/add/{couponId}")
    CommonResult add(@PathVariable Long couponId,
                     @RequestParam("memberId") Long memberId,
                     @RequestParam("nickName") String nickName);

    @GetMapping(value = "/listHistory")
    CommonResult<List<SmsCouponHistory>> listHistory(@RequestParam(value = "memberId", required = false) Long memberId,
                                                     @RequestParam(value = "useStatus", required = false) Integer useStatus);

    @GetMapping(value = "/list")
    CommonResult<List<SmsCoupon>> list(@RequestParam(value = "memberId", required = false) Long memberId,
                                       @RequestParam(value = "useStatus", required = false) Integer useStatus);

    /**
     * ✅ Unica API “cart coupon”: POST con body (niente chiamate coupon -> order).
     */
    @PostMapping(value = "/list/cart/{type}")
    CommonResult<List<SmsCouponHistoryDetail>> listCartPromotion(@PathVariable Integer type,
                                                                 @RequestBody List<CartPromotionItem> cartPromotionItemList,
                                                                 @RequestParam(value = "memberId", required = false) Long memberId);

    @GetMapping(value = "/updateCouponStatus")
    CommonResult updateCouponStatus(@RequestParam(value = "couponId") Long couponId,
                                    @RequestParam(value = "memberId") Long memberId,
                                    @RequestParam(value = "useStatus") Integer useStatus);

    @RequestMapping(value = "/getAvailableCouponList", method = RequestMethod.GET)
    CommonResult<List<SmsCoupon>> getAvailableCouponList(@RequestParam(value = "productId") Long productId,
                                                         @RequestParam(value = "productCategoryId") Long productCategoryId);

    @RequestMapping(value = "/getNextFlashPromotionSession", method = RequestMethod.GET)
    CommonResult<SmsFlashPromotionSession> getNextFlashPromotionSession(@RequestParam(value = "date") Date date);

    @RequestMapping(value = "/getHomeAdvertiseList", method = RequestMethod.GET)
    CommonResult<List<SmsHomeAdvertise>> getHomeAdvertiseList();

    @RequestMapping(value = "/getFlashPromotion", method = RequestMethod.GET)
    CommonResult<SmsFlashPromotion> getFlashPromotion(@RequestParam(value = "date") Date date);

    @RequestMapping(value = "/getFlashPromotionSession", method = RequestMethod.GET)
    CommonResult<SmsFlashPromotionSession> getFlashPromotionSession(@RequestParam(value = "date") Date date);
}
