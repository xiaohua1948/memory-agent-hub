package com.xiaohua.service;

import com.alibaba.fastjson.JSONObject;
import com.xiaohua.domain.VO.VisualDataVO;
import org.springframework.stereotype.Service;

@Service
public interface VisualDataService {
    public VisualDataVO selectTypeSite(Integer userId);

    public JSONObject selectUserInfo(Integer userId);

}
