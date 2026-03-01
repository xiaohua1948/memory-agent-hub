package com.memory.xiaohua.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memory.xiaohua.model.dto.PersonDTO;
import com.memory.xiaohua.model.entity.Person;
import com.baomidou.mybatisplus.extension.service.IService;
import com.memory.xiaohua.model.vo.FileInfoListVO;
import com.memory.xiaohua.model.vo.album.PersonAlbumVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 人物信息表 服务类
 * </p>
 *
 * @author xiaohua
 * @since 2025-03-07
 */
public interface PersonService extends IService<Person> {

    PersonAlbumVO selectPersonById(Long userId,Long personId);
    Page<PersonAlbumVO> selectAllPersonAlbum(Long userId,int current,int size,Boolean display);


    Page<FileInfoListVO> getPersonFileInfo(Integer current, Integer size, Long userId , String orderType, String orderKeyword, String imageTypeText, Long personId);

    void updatePerson(PersonDTO person , Long userId);

    @Transactional(rollbackFor = Exception.class)
    void mergePersons(Long userId,  Long representativeId, List<Long> mergeIds);

    @Transactional(rollbackFor = Exception.class)
    void removePersonPicture(Long userId,Long personId,List<String> fileIds);
}
