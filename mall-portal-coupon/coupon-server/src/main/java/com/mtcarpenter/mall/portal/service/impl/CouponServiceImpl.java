package com.mtcarpenter.mall.portal.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mtcarpenter.mall.common.exception.Asserts;
import com.mtcarpenter.mall.domain.CartPromotionItem;
import com.mtcarpenter.mall.domain.SmsCouponHistoryDetail;
import com.mtcarpenter.mall.mapper.*;
import com.mtcarpenter.mall.model.*;
import com.mtcarpenter.mall.portal.dao.SmsCouponHistoryDao;
import com.mtcarpenter.mall.portal.service.CouponService;
import com.mtcarpenter.mall.portal.util.DateUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author mtcarpenter
 * @github https://github.com/mtcarpenter/mall-cloud-alibaba
 * @desc 微信公众号：山间木匠
 */
@Service
public class CouponServiceImpl implements CouponService {

    @Autowired
    private SmsCouponMapper couponMapper;
    @Autowired
    private SmsCouponHistoryMapper couponHistoryMapper;
    @Autowired
    private SmsCouponHistoryDao couponHistoryDao;

    @Autowired
    private SmsHomeAdvertiseMapper advertiseMapper;

    @Autowired
    private SmsFlashPromotionSessionMapper promotionSessionMapper;
    @Autowired
    private SmsFlashPromotionMapper flashPromotionMapper;

    @Autowired
    private SmsCouponProductRelationMapper couponProductRelationMapper;

    @Autowired
    private SmsCouponProductCategoryRelationMapper couponProductCategoryRelationMapper;

    @Override
    public void add(Long couponId, Long memberId, String nickName) {
        SmsCoupon coupon = couponMapper.selectByPrimaryKey(couponId);
        if (coupon == null) {
            Asserts.fail("优惠券不存在");
        }
        if (coupon.getCount() <= 0) {
            Asserts.fail("优惠券已经领完了");
        }
        Date now = new Date();
        if (now.before(coupon.getEnableTime())) {
            Asserts.fail("优惠券还没到领取时间");
        }
        SmsCouponHistoryExample couponHistoryExample = new SmsCouponHistoryExample();
        couponHistoryExample.createCriteria().andCouponIdEqualTo(couponId).andMemberIdEqualTo(memberId);
        long count = couponHistoryMapper.countByExample(couponHistoryExample);
        if (count >= coupon.getPerLimit()) {
            Asserts.fail("您已经领取过该优惠券");
        }
        SmsCouponHistory couponHistory = new SmsCouponHistory();
        couponHistory.setCouponId(couponId);
        couponHistory.setCouponCode(generateCouponCode(memberId));
        couponHistory.setCreateTime(now);
        couponHistory.setMemberId(memberId);
        couponHistory.setMemberNickname(nickName);
        couponHistory.setGetType(1);
        couponHistory.setUseStatus(0);
        couponHistoryMapper.insert(couponHistory);

        coupon.setCount(coupon.getCount() - 1);
        coupon.setReceiveCount(coupon.getReceiveCount() == null ? 1 : coupon.getReceiveCount() + 1);
        couponMapper.updateByPrimaryKey(coupon);
    }

    @Override
    public List<SmsCoupon> list(Long memberId, Integer useStatus) {
        return couponHistoryDao.getCouponList(memberId, useStatus);
    }

    @Override
    public List<SmsCouponHistoryDetail> listCart(List<CartPromotionItem> cartPromotionItemList, Long memberId, Integer type) {
        Date now = new Date();
        List<SmsCouponHistoryDetail> allList = couponHistoryDao.getDetailList(memberId);
        List<SmsCouponHistoryDetail> enableList = new ArrayList<>();
        List<SmsCouponHistoryDetail> disableList = new ArrayList<>();

        for (SmsCouponHistoryDetail couponHistoryDetail : allList) {
            Integer useType = couponHistoryDetail.getCoupon().getUseType();
            BigDecimal minPoint = couponHistoryDetail.getCoupon().getMinPoint();
            Date endTime = couponHistoryDetail.getCoupon().getEndTime();

            if (useType.equals(0)) {
                BigDecimal totalAmount = calcTotalAmount(cartPromotionItemList);
                if (now.before(endTime) && totalAmount.subtract(minPoint).intValue() >= 0) {
                    enableList.add(couponHistoryDetail);
                } else {
                    disableList.add(couponHistoryDetail);
                }
            } else if (useType.equals(1)) {
                List<Long> productCategoryIds = new ArrayList<>();
                for (SmsCouponProductCategoryRelation categoryRelation : couponHistoryDetail.getCategoryRelationList()) {
                    productCategoryIds.add(categoryRelation.getProductCategoryId());
                }
                BigDecimal totalAmount = calcTotalAmountByproductCategoryId(cartPromotionItemList, productCategoryIds);
                if (now.before(endTime) && totalAmount.intValue() > 0 && totalAmount.subtract(minPoint).intValue() >= 0) {
                    enableList.add(couponHistoryDetail);
                } else {
                    disableList.add(couponHistoryDetail);
                }
            } else if (useType.equals(2)) {
                List<Long> productIds = new ArrayList<>();
                for (SmsCouponProductRelation productRelation : couponHistoryDetail.getProductRelationList()) {
                    productIds.add(productRelation.getProductId());
                }
                BigDecimal totalAmount = calcTotalAmountByProductId(cartPromotionItemList, productIds);
                if (now.before(endTime) && totalAmount.intValue() > 0 && totalAmount.subtract(minPoint).intValue() >= 0) {
                    enableList.add(couponHistoryDetail);
                } else {
                    disableList.add(couponHistoryDetail);
                }
            }
        }

        if (type.equals(1)) {
            return enableList.stream().map(s -> {
                SmsCouponHistoryDetail d = new SmsCouponHistoryDetail();
                BeanUtils.copyProperties(s, d);
                return d;
            }).collect(Collectors.toList());
        } else {
            return disableList.stream().map(s -> {
                SmsCouponHistoryDetail d = new SmsCouponHistoryDetail();
                BeanUtils.copyProperties(s, d);
                return d;
            }).collect(Collectors.toList());
        }
    }

    @Override
    public void updateCouponStatus(Long couponId, Long memberId, Integer useStatus) {
        if (couponId == null) {
            return;
        }
        SmsCouponHistoryExample example = new SmsCouponHistoryExample();
        example.createCriteria().andMemberIdEqualTo(memberId)
                .andCouponIdEqualTo(couponId).andUseStatusEqualTo(useStatus == 0 ? 1 : 0);
        List<SmsCouponHistory> couponHistoryList = couponHistoryMapper.selectByExample(example);
        if (!CollectionUtils.isEmpty(couponHistoryList)) {
            SmsCouponHistory couponHistory = couponHistoryList.get(0);
            couponHistory.setUseTime(new Date());
            couponHistory.setUseStatus(useStatus);
            couponHistoryMapper.updateByPrimaryKeySelective(couponHistory);
        }
    }

    @Override
    public List<SmsCoupon> getAvailableCouponList(Long productId, Long productCategoryId) {
        return couponHistoryDao.getAvailableCouponList(productId, productCategoryId);
    }

    @Override
    public SmsFlashPromotionSession getNextFlashPromotionSession(Date date) {
        SmsFlashPromotionSessionExample sessionExample = new SmsFlashPromotionSessionExample();
        sessionExample.createCriteria().andStartTimeGreaterThan(date);
        sessionExample.setOrderByClause("start_time asc");
        List<SmsFlashPromotionSession> list = promotionSessionMapper.selectByExample(sessionExample);
        return CollectionUtils.isEmpty(list) ? null : list.get(0);
    }

    @Override
    public List<SmsHomeAdvertise> getHomeAdvertiseList() {
        SmsHomeAdvertiseExample example = new SmsHomeAdvertiseExample();
        example.createCriteria().andTypeEqualTo(1).andStatusEqualTo(1);
        example.setOrderByClause("sort desc");
        return advertiseMapper.selectByExample(example);
    }

    @Override
    public SmsFlashPromotion getFlashPromotion(Date date) {
        Date currDate = DateUtil.getDate(date);
        SmsFlashPromotionExample example = new SmsFlashPromotionExample();
        example.createCriteria()
                .andStatusEqualTo(1)
                .andStartDateLessThanOrEqualTo(currDate)
                .andEndDateGreaterThanOrEqualTo(currDate);
        List<SmsFlashPromotion> list = flashPromotionMapper.selectByExample(example);
        return CollectionUtils.isEmpty(list) ? null : list.get(0);
    }

    @Override
    public SmsFlashPromotionSession getFlashPromotionSession(Date date) {
        Date currTime = DateUtil.getTime(date);
        SmsFlashPromotionSessionExample sessionExample = new SmsFlashPromotionSessionExample();
        sessionExample.createCriteria()
                .andStartTimeLessThanOrEqualTo(currTime)
                .andEndTimeGreaterThanOrEqualTo(currTime);
        List<SmsFlashPromotionSession> list = promotionSessionMapper.selectByExample(sessionExample);
        return CollectionUtils.isEmpty(list) ? null : list.get(0);
    }

    @Override
    public List<SmsCouponHistory> listHistory(Long memberId, Integer useStatus) {
        SmsCouponHistoryExample example = new SmsCouponHistoryExample();
        SmsCouponHistoryExample.Criteria c = example.createCriteria();
        c.andMemberIdEqualTo(memberId);
        if (useStatus != null) {
            c.andUseStatusEqualTo(useStatus);
        }
        return couponHistoryMapper.selectByExample(example);
    }

    /**
     * Compatibilità: SOLO coupon legati direttamente al prodotto.
     * (prima includeva anche quelli di categoria chiamando product-service: ora non più)
     */
    @Override
    public List<SmsCoupon> listByProduct(Long productId) {
        return listByProduct(productId, null);
    }

    /**
     * Nuova versione: riceve anche productCategoryId dal chiamante, quindi nessun Feign verso product.
     */
    @Override
    public List<SmsCoupon> listByProduct(Long productId, Long productCategoryId) {
        List<Long> allCouponIds = new ArrayList<>();

        // coupon legati direttamente al prodotto
        SmsCouponProductRelationExample cprExample = new SmsCouponProductRelationExample();
        cprExample.createCriteria().andProductIdEqualTo(productId);
        List<SmsCouponProductRelation> cprList = couponProductRelationMapper.selectByExample(cprExample);
        if (CollUtil.isNotEmpty(cprList)) {
            allCouponIds.addAll(cprList.stream().map(SmsCouponProductRelation::getCouponId).collect(Collectors.toList()));
        }

        // coupon legati alla categoria (se il chiamante ce la passa)
        if (productCategoryId != null) {
            SmsCouponProductCategoryRelationExample cpcrExample = new SmsCouponProductCategoryRelationExample();
            cpcrExample.createCriteria().andProductCategoryIdEqualTo(productCategoryId);
            List<SmsCouponProductCategoryRelation> cpcrList = couponProductCategoryRelationMapper.selectByExample(cpcrExample);
            if (CollUtil.isNotEmpty(cpcrList)) {
                allCouponIds.addAll(cpcrList.stream().map(SmsCouponProductCategoryRelation::getCouponId).collect(Collectors.toList()));
            }
        }

        if (CollUtil.isEmpty(allCouponIds)) {
            return new ArrayList<>();
        }

        Date now = new Date();
        SmsCouponExample couponExample = new SmsCouponExample();
        couponExample.createCriteria()
                .andEndTimeGreaterThan(now)
                .andStartTimeLessThan(now)
                .andUseTypeEqualTo(0);
        couponExample.or(couponExample.createCriteria()
                .andEndTimeGreaterThan(now)
                .andStartTimeLessThan(now)
                .andUseTypeNotEqualTo(0)
                .andIdIn(allCouponIds));

        return couponMapper.selectByExample(couponExample);
    }

    private String generateCouponCode(Long memberId) {
        StringBuilder sb = new StringBuilder();
        Long currentTimeMillis = System.currentTimeMillis();
        String timeMillisStr = currentTimeMillis.toString();
        sb.append(timeMillisStr.substring(timeMillisStr.length() - 8));
        for (int i = 0; i < 4; i++) {
            sb.append(new Random().nextInt(10));
        }
        String memberIdStr = memberId.toString();
        if (memberIdStr.length() <= 4) {
            sb.append(String.format("%04d", memberId));
        } else {
            sb.append(memberIdStr.substring(memberIdStr.length() - 4));
        }
        return sb.toString();
    }

    private BigDecimal calcTotalAmount(List<CartPromotionItem> cartItemList) {
        BigDecimal total = new BigDecimal("0");
        for (CartPromotionItem item : cartItemList) {
            BigDecimal realPrice = item.getPrice().subtract(item.getReduceAmount());
            total = total.add(realPrice.multiply(new BigDecimal(item.getQuantity())));
        }
        return total;
    }

    private BigDecimal calcTotalAmountByproductCategoryId(List<CartPromotionItem> cartItemList, List<Long> productCategoryIds) {
        BigDecimal total = new BigDecimal("0");
        for (CartPromotionItem item : cartItemList) {
            if (productCategoryIds.contains(item.getProductCategoryId())) {
                BigDecimal realPrice = item.getPrice().subtract(item.getReduceAmount());
                total = total.add(realPrice.multiply(new BigDecimal(item.getQuantity())));
            }
        }
        return total;
    }

    private BigDecimal calcTotalAmountByProductId(List<CartPromotionItem> cartItemList, List<Long> productIds) {
        BigDecimal total = new BigDecimal("0");
        for (CartPromotionItem item : cartItemList) {
            if (productIds.contains(item.getProductId())) {
                BigDecimal realPrice = item.getPrice().subtract(item.getReduceAmount());
                total = total.add(realPrice.multiply(new BigDecimal(item.getQuantity())));
            }
        }
        return total;
    }
}
