package com.memory.xiaohua.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memory.xiaohua.model.vo.FileInfoListVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface RecycleService {
    Page<FileInfoListVO> getFileInfoList(Integer current, Integer size, String orderType, String orderKeyword, String imageTypeText,  Long userId);

    void recoverPicture(List<String> fileIds,Long UserId);

    void dropPicture(List<String> fileIds,Long UserId);

    void cronDropPicture();


}
