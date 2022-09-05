package com.xiaohua.service.Impl;

import com.alibaba.fastjson.JSONObject;
import com.xiaohua.dao.ImageMapper;
import com.xiaohua.dao.VisualDataMapper;
import com.xiaohua.domain.VO.VisualDataVO;
import com.xiaohua.domain.VisualDataSite;
import com.xiaohua.domain.VisualDataType;
import com.xiaohua.service.VisualDataService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class VisualDataServiceImpl implements VisualDataService {
    @Resource
    VisualDataMapper visualDataMapper;
    @Resource
    ImageMapper imageMapper;

    /**
     * 查询可视化数据(类型，地点分布)
     * @param userId
     * @return
     */
    @Override
    public VisualDataVO selectTypeSite(Integer userId) {
        List<VisualDataType> visTypes = visualDataMapper.selectVisualDataType(userId);
        List<VisualDataSite> visSites = visualDataMapper.selectVisualDataSite(userId);
        List<String> imageSite = new ArrayList<>();
        List<Integer> SiteNumber = new ArrayList<>();
        for(VisualDataSite vis:visSites){
            imageSite.add(vis.getImageSite());
            SiteNumber.add(vis.getNumber());
        }
        return new VisualDataVO(visTypes,imageSite,SiteNumber);
    }

    /**
     * 查询用户信息
     * @param userId
     * @return
     */
    @Override
    public JSONObject selectUserInfo(Integer userId) {
        JSONObject res = new JSONObject();
        Integer imageSumNumber = imageMapper.selectImageTotalCount(userId);
        Integer imageSumSize = visualDataMapper.selectImageSumSize(userId);
        Integer capacity = visualDataMapper.selectCapacity(userId);
        res.put("imageSumSize",((float)imageSumSize/1024/1024));
        res.put("imageSumNumber",imageSumNumber);
        res.put("capacity",capacity);
        return res;
    }
}
