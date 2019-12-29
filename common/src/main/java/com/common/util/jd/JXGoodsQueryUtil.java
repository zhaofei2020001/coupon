package com.common.util.jd;

import com.common.constant.AllEnums;
import com.common.constant.Constants;
import com.jd.open.api.sdk.DefaultJdClient;
import com.jd.open.api.sdk.JdClient;
import com.jd.open.api.sdk.JdException;
import jd.union.open.goods.jingfen.query.request.JFGoodsReq;
import jd.union.open.goods.jingfen.query.request.UnionOpenGoodsJingfenQueryRequest;
import jd.union.open.goods.jingfen.query.response.JFGoodsResp;
import jd.union.open.goods.jingfen.query.response.UnionOpenGoodsJingfenQueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

/**
 * @author 京粉精选商品查询接口
 * since 2019/12/17
 */
@Slf4j
public class JXGoodsQueryUtil {
  public static List<JFGoodsResp> jxGoodsquery(AllEnums.eliteEnum eliteEnum) {

    List<JFGoodsResp> jfGoodsRespList = new ArrayList<>();
    JdClient client = new DefaultJdClient(Constants.JD_SERVER_URL, null, Constants.JD_APP_KEY, Constants.JD_APP_SECRET);
    UnionOpenGoodsJingfenQueryRequest request = new UnionOpenGoodsJingfenQueryRequest();
    JFGoodsReq goodsReq = new JFGoodsReq();
    goodsReq.setEliteId(eliteEnum.getCode());
    goodsReq.setPageIndex(Constants.pageIndex);
    goodsReq.setPageSize(Constants.pageSize);
    goodsReq.setSort(Constants.sort);
    goodsReq.setSortName(Constants.sort_name);
    request.setGoodsReq(goodsReq);
    UnionOpenGoodsJingfenQueryResponse response = null;
    try {
      response = client.execute(request);
    } catch (JdException e) {
      e.printStackTrace();
    }

    if (Objects.equals(response.getCode(), 200) && Objects.equals(response.getMessage(), "success")) {

      List<JFGoodsResp> list = CollectionUtils.arrayToList(response.getData());

      jfGoodsRespList.addAll(list);

      Long num = response.getTotalCount() / Constants.pageSize + (response.getTotalCount() % Constants.pageSize == 0L ? 0L : 1L);
      log.info("num--------->{}", num);
      for (int i = 2; i <= num.intValue(); i++) {
        JFGoodsReq jfgr = new JFGoodsReq();
        jfgr.setEliteId(eliteEnum.getCode());
        jfgr.setPageIndex(i);
        jfgr.setPageSize(Constants.pageSize);
        jfgr.setSort(Constants.sort);
        jfgr.setSortName(Constants.sort_name);
        request.setGoodsReq(jfgr);
        UnionOpenGoodsJingfenQueryResponse responseTWO = null;
        try {
          responseTWO = client.execute(request);
        } catch (JdException e) {
          e.printStackTrace();
        }

        if (Objects.equals(responseTWO.getCode(), 200) && Objects.equals(responseTWO.getMessage(), "success")) {

          List<JFGoodsResp> listTwo = CollectionUtils.arrayToList(responseTWO.getData());
          boolean b = jfGoodsRespList.addAll(listTwo);
          log.info("----->{},-->{}", b, i);
        }
      }

    }
    return jfGoodsRespList.stream()
        .filter(it -> it.getCouponInfo().getCouponList().length > 0)
        .collect(
            collectingAndThen(
                toCollection(() -> new TreeSet<>(Comparator.comparing(JFGoodsResp::getSkuId))), ArrayList::new)
        );
  }

//
//  public static String getSkUPromotionInfo() {
//    public JdClient client = new DefaultJdClient(SERVER_URL, accessToken, appKey, appSecret);
//
//    JosVoucherInfoGetRequest request = new JosVoucherInfoGetRequest();
//
//    request.setAccessToken("jingdong");
//    request.setCustomerUserId(123);
//
//    JosVoucherInfoGetResponse response = client.execute(request);
//    return null;
//
//  }
}


