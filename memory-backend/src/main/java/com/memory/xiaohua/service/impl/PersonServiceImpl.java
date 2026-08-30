package com.memory.xiaohua.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.memory.xiaohua.mapper.AlbumMapper;
import com.memory.xiaohua.mapper.FaceMapper;
import com.memory.xiaohua.mapper.PersonFaceMapper;
import com.memory.xiaohua.mapper.PersonMapper;
import com.memory.xiaohua.model.dto.PersonDTO;
import com.memory.xiaohua.model.entity.Person;
import com.memory.xiaohua.model.entity.PersonFace;
import com.memory.xiaohua.model.vo.FileInfoListVO;
import com.memory.xiaohua.model.vo.album.PersonAlbumVO;
import com.memory.xiaohua.service.PersonService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 人物信息表 服务实现类
 * </p>
 *
 * @author xiaohua
 * @since 2025-03-07
 */
@Service
public class PersonServiceImpl extends ServiceImpl<PersonMapper, Person> implements PersonService {

    @Resource
    private PersonMapper personMapper;
    @Resource
    private FaceMapper faceMapper;

    @Resource
    private AlbumMapper albumMapper;

    @Resource
    private PersonFaceMapper personFaceMapper;

    @Override
    public PersonAlbumVO selectPersonById(Long userId, Long personId) {

        return personMapper.selectPersonById(userId, personId);
    }

    @Override
    public Page<PersonAlbumVO> selectAllPersonAlbum(Long userId, int current, int size, Boolean display) {
        Page<PersonAlbumVO> page = new Page<>();
        page.setCurrent(current);
        page.setSize(size);
        List<PersonAlbumVO> personAlbumVOS = personMapper.selectAllPersonAlbum(page, userId, display);
        page.setRecords(personAlbumVOS);

        return page;
    }

    @Override
    public Page<FileInfoListVO> getPersonFileInfo(Integer current, Integer size, Long userId, String orderType, String orderKeyword, String imageTypeText, Long personId) {
        Page<FileInfoListVO> page = new Page<>();
        page.setSize(size);
        page.setCurrent(current);
        List<FileInfoListVO> personFileInfo = albumMapper.getPersonFileInfo(page, userId, orderType, orderKeyword, imageTypeText, personId, false);
        page.setRecords(personFileInfo);
        return page;
    }


    @Override
    public void updatePerson(PersonDTO person, Long userId) {
        UpdateWrapper<Person> updateWrapper = new UpdateWrapper<>();
        Long personId = person.getPersonId();
        String personName = person.getPersonName();
        String personRelation = person.getPersonRelation();
        Long faceId = person.getFaceId();
        Long oldFaceId = person.getOldFaceId();
        updateWrapper.eq("person_id", personId);
        updateWrapper.eq("user_id", userId);
        if (personName != null) {
            updateWrapper.set("person_name", personName);
        }
        if (personRelation != null) {
            updateWrapper.set("person_relation", personRelation);
        }
        if (faceId != null && oldFaceId != null) {
            UpdateWrapper<PersonFace> pfUpdateWrapper = new UpdateWrapper<>();
            pfUpdateWrapper.eq("user_id", userId);
            pfUpdateWrapper.eq("face_id", oldFaceId);
            pfUpdateWrapper.set("representative", false);
            personFaceMapper.update(pfUpdateWrapper);
            pfUpdateWrapper.clear();
            pfUpdateWrapper.eq("user_id", userId);
            pfUpdateWrapper.eq("face_id", faceId);
            pfUpdateWrapper.set("representative", true);
            personFaceMapper.update(pfUpdateWrapper);
        }
        personMapper.update(updateWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public void mergePersons(Long userId, Long representativeId, List<Long> mergeIds) {
        //修改人物脸部关联表
        personMapper.updatePersonFaces(userId, representativeId, mergeIds);
        //删除被合并的人物
        personMapper.deletePersonById(userId,mergeIds);

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removePersonPicture(Long userId,Long oldPersonId, List<String> fileIds) {
        List<Long> faceIds = faceMapper.selectFaceIdByFileIds(userId, fileIds);

        //删除人物脸部关联表
        personMapper.deletePersonFacesByFaceId(userId,oldPersonId,faceIds);
        //创建新的人物;
        Person person = new Person();
        person.setUserId(userId);
        personMapper.insert(person);
        Long newPersonId = person.getPersonId();
        //将移出的图片插入新人物
        personFaceMapper.insertPersonFaces(userId,newPersonId,faceIds);
    }
}
