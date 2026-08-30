package com.memory.xiaohua.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.memory.xiaohua.mapper.FaceMapper;
import com.memory.xiaohua.mapper.FileMapper;
import com.memory.xiaohua.mapper.PersonFaceMapper;
import com.memory.xiaohua.mapper.PersonMapper;
import com.memory.xiaohua.model.entity.Face;
import com.memory.xiaohua.model.entity.FileEntity;
import com.memory.xiaohua.model.entity.Person;
import com.memory.xiaohua.model.entity.PersonFace;
import com.memory.xiaohua.service.FaceService;
import com.memory.xiaohua.utils.file.MinioOSSUtil;
import com.memory.xiaohua.utils.picture.OpenCVPictureUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 * 人像特征表 服务实现类
 * </p>
 *
 * @author xiaohua
 * @since 2025-03-07
 */
@Service
public class FaceServiceImpl extends ServiceImpl<FaceMapper, Face> implements FaceService {

    @Resource
    private FaceMapper faceMapper;

    @Resource
    private FileMapper fileMapper;

    @Resource
    private PersonMapper personMapper;

    @Resource
    private OpenCVPictureUtil openCVPictureUtil;

    @Resource
    private MinioOSSUtil minioOSSUtil;

    @Resource
    private PersonFaceMapper personFaceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processPictureFaceInfo() {
        QueryWrapper<Face> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_processed", false);
        //未处理过的数据
        Page<Face> page = new Page<>();
        //分页查询，避免内存泄露
        page.setSize(100);
        page.setCurrent(1);
        List<Face> faces = faceMapper.selectList(page, queryWrapper);

        for (Face face : faces) {
            try {
                processOneFace(face);
            } catch (Exception e) {
                log.error("处理失败:faceId=" + face.getFaceId() + e.getMessage());
            }
        }


    }

    @Transactional(rollbackFor = Exception.class)
    public void processOneFace(Face face) {
        UpdateWrapper<Face> faceUpdateWrapper = new UpdateWrapper<>();

        Long faceId = face.getFaceId();
        Long userId = face.getUserId();
        String fileId = face.getFileId();
        //人脸数据未处理
        System.err.println("处理faceId："+face.getFaceId()+"对应的文件fileId:+"+face.getFileId());
        FileEntity fileEntity = fileMapper.selectById(fileId);
        String thumbnailObjectName = fileEntity.getThumbnailObjectName();
        byte[] fileBytes = minioOSSUtil.getFileBytes(thumbnailObjectName);
        //人脸数据
        byte[] mat = openCVPictureUtil.extractFace(fileBytes);

        LocalDateTime now = LocalDateTime.now();

        LocalDate localDate = now.toLocalDate();

        faceUpdateWrapper.eq("face_id", faceId);
        faceUpdateWrapper.set("is_processed", true);
        faceUpdateWrapper.set("create_time", now);
        if (mat == null) {
            //没有脸的照片
            faceUpdateWrapper.set("is_face", false);
        } else {
            //有脸的照片
            faceUpdateWrapper.set("is_face", true);
            faceUpdateWrapper.set("feature_vector", mat);

            String uuid = UUID.randomUUID().toString().replace("-", "");
            String objectName = "face/" + localDate + "/" + uuid + ".png";
            //人脸信息入库
            ByteArrayInputStream uploadMat = new ByteArrayInputStream(mat);
            minioOSSUtil.uploadToOSS(objectName, uploadMat, mat.length, "image/png");
            String fileUrl = minioOSSUtil.getFileUrl(objectName);
            faceUpdateWrapper.set("person_object_name", objectName);
            faceUpdateWrapper.set("person_cover_url", fileUrl);
            QueryWrapper<Person> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id",userId);
            List<Person> personList = personMapper.selectList(queryWrapper);

            int count = 0;
            if (!personList.isEmpty()) {
                //有人物，就查询匹配不
                for (Person person : personList) {
                    //人物id
                    Long personId = person.getPersonId();
                    //人物代表的脸id
                    Long compareFaceId = personMapper.selectRepresentativeId(personId);


                    Face compareFace = faceMapper.selectById(compareFaceId);
                    if(compareFace == null) continue;

                    byte[] compareMat = compareFace.getFeatureVector();
                    //是否相似
                    boolean res = openCVPictureUtil.isSamePerson(mat, compareMat);
                    if (res) {
                        //相似
                        PersonFace personFace = new PersonFace();
                        personFace.setUserId(userId);
                        personFace.setPersonId(personId);
                        personFace.setFaceId(faceId);
                        personFaceMapper.insert(personFace);
                        //跳出，不用再匹配下去了
                        break;
                    }
                    //每搜寻一次，就++
                    count++;
                }
            }
            if (personList.isEmpty() || personList.size() == count) {
                System.err.println("匹配失败创建新人物");
                //没有匹配的人物，创建人物;
                Person person = new Person();
                person.setUserId(userId);
                personMapper.insert(person);

                // 更新当前Face关联到新Person
                PersonFace personFace = new PersonFace();
                personFace.setUserId(userId);
                personFace.setPersonId(person.getPersonId());
                personFace.setFaceId(faceId);
                personFaceMapper.insert(personFace);
            }
        }

        faceMapper.update(faceUpdateWrapper);


    }
}
