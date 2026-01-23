package com.mtcarpenter.mall.portal.service;

import com.mtcarpenter.mall.domain.CartPromotionItem;
import com.mtcarpenter.mall.domain.SmsCouponHistoryDetail;
import com.mtcarpenter.mall.model.*;

import java.util.Date;
import java.util.List;

public interface CouponService {

    void add(Long couponId, Long memberId, String nickName);

    List<SmsCoupon> list(Long memberId, Integer useStatus);

    List<SmsCouponHistoryDetail> listCart(List<CartPromotionItem> cartPromotionItemList, Long memberId, Integer type);

    void updateCouponStatus(Long couponId, Long memberId, Integer useStatus);

    List<SmsCoupon> getAvailableCouponList(Long productId, Long productCategoryId);

    SmsFlashPromotionSession getNextFlashPromotionSession(Date date);

    List<SmsHomeAdvertise> getHomeAdvertiseList();

    SmsFlashPromotion getFlashPromotion(Date date);

    SmsFlashPromotionSession getFlashPromotionSession(Date date);

    List<SmsCouponHistory> listHistory(Long memberId, Integer useStatus);

    /**
     * Compatibilità: restituisce SOLO coupon legati direttamente al prodotto (senza categoria).
     */
    List<SmsCoupon> listByProduct(Long productId);

    /**
     * Nuova API: coupon legati al prodotto + coupon legati alla categoria (senza chiamare product-service).
     */
    List<SmsCoupon> listByProduct(Long productId, Long productCategoryId);
}
