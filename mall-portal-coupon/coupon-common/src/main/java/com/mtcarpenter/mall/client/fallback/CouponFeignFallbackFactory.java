package com.mtcarpenter.mall.client.fallback;

import com.mtcarpenter.mall.client.CouponFeign;
import com.mtcarpenter.mall.common.api.CommonResult;
import com.mtcarpenter.mall.domain.CartPromotionItem;
import com.mtcarpenter.mall.domain.SmsCouponHistoryDetail;
import com.mtcarpenter.mall.model.*;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class CouponFeignFallbackFactory implements FallbackFactory<CouponFeign> {

    @Override
    public CouponFeign create(Throwable cause) {
        return new CouponFeign() {
            @Override
            public CommonResult add(Long couponId, Long memberId, String nickName) {
                return CommonResult.failed("coupon-service unavailable (fallback)");
            }

            @Override
            public CommonResult<List<SmsCouponHistory>> listHistory(Long memberId, Integer useStatus) {
                return CommonResult.success(Collections.emptyList());
            }

            @Override
            public CommonResult<List<SmsCoupon>> list(Long memberId, Integer useStatus) {
                return CommonResult.success(Collections.emptyList());
            }

            @Override
            public CommonResult<List<SmsCouponHistoryDetail>> listCartPromotion(Integer type,
                                                                               List<CartPromotionItem> cartPromotionItemList,
                                                                               Long memberId) {
                return CommonResult.success(Collections.emptyList());
            }

            @Override
            public CommonResult updateCouponStatus(Long couponId, Long memberId, Integer useStatus) {
                return CommonResult.failed("coupon-service unavailable (fallback)");
            }

            @Override
            public CommonResult<List<SmsCoupon>> getAvailableCouponList(Long productId, Long productCategoryId) {
                return CommonResult.success(Collections.emptyList());
            }

            @Override
            public CommonResult<SmsFlashPromotionSession> getNextFlashPromotionSession(Date date) {
                return CommonResult.success(null);
            }

            @Override
            public CommonResult<List<SmsHomeAdvertise>> getHomeAdvertiseList() {
                return CommonResult.success(Collections.emptyList());
            }

            @Override
            public CommonResult<SmsFlashPromotion> getFlashPromotion(Date date) {
                return CommonResult.success(null);
            }

            @Override
            public CommonResult<SmsFlashPromotionSession> getFlashPromotionSession(Date date) {
                return CommonResult.success(null);
            }
        };
    }
}
