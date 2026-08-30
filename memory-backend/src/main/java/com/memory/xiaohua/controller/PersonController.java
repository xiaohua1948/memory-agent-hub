package com.memory.xiaohua.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memory.xiaohua.common.BaseResponse;
import com.memory.xiaohua.common.ResultUtil;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import com.memory.xiaohua.mapper.PersonMapper;
import com.memory.xiaohua.model.dto.PersonDTO;
import com.memory.xiaohua.model.entity.Person;
import com.memory.xiaohua.model.enums.PersonRelation;
import com.memory.xiaohua.model.vo.FileInfoListVO;
import com.memory.xiaohua.model.vo.album.PersonAlbumVO;
import com.memory.xiaohua.model.vo.album.PersonCoverVO;
import com.memory.xiaohua.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 人物信息表 前端控制器
 * </p>
 *
 * @author xiaohua
 * @since 2025-03-07
 */
@RestController
@RequestMapping("/person")
public class PersonController {

    @Resource
    private PersonService personService;

    @Resource
    private PersonMapper personMapper;


    @PostMapping("/selectPersonById")
    @Operation(summary = "查询人物", description = "查询所有人物")
    public BaseResponse<?> selectAllModelAlbum(Long personId) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        PersonAlbumVO personAlbumVO = personService.selectPersonById(userId, personId);
        return ResultUtil.success(personAlbumVO, "获取数据成功！");
    }

    @PostMapping("/selectAllPersonAlbum")
    @Operation(summary = "查询所有人物", description = "查询所有人物")
    public BaseResponse<?> selectAllModelAlbum(String current, String size,Boolean display) {
        int c;
        int s;
        try {
            c = Integer.parseInt(current);
            s = Integer.parseInt(size);
        } catch (NumberFormatException e) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "分页错误!");
        }
        if(display == null){
            display = true;
        }

        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        Page<PersonAlbumVO> personAlbumVOPage = personService.selectAllPersonAlbum(userId, c, s,display);
        return ResultUtil.success(personAlbumVOPage, "获取数据成功！");
    }

    @PostMapping("/selectAllPersonCover")
    @Operation(summary = "查询所有人物", description = "查询所有人物")
    public BaseResponse<?> SelectAllPersonCover(Long personId) {


        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        List<PersonCoverVO> personCover = personMapper.selectAllPersonCover(userId, personId);
        return ResultUtil.success(personCover, "获取数据成功！");
    }


    @PostMapping("/mergePerson")
    @Operation(summary = "查询所有人物", description = "查询所有人物")
    public BaseResponse<?> mergePerson(@RequestParam("personIds") List<Long> personIds) {
        if(personIds.isEmpty()||personIds.size()<2){
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误!");
        }
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        Long representativeId = personIds.get(0);
        personIds.remove(0);
        personService.mergePersons(userId,representativeId,personIds);

        return ResultUtil.success( "合并成功！");
    }



    @PostMapping("/hiddenPerson")
    @Operation(summary = "查询所有人物", description = "查询所有人物")
    public BaseResponse<?> hiddenPerson(@RequestParam("personIds") List<Long> personIds) {
        if(personIds.isEmpty()){
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误!");
        }
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        personMapper.hiddenPerson(userId,personIds);
        return ResultUtil.success( "隐藏成功！");
    }


    @PostMapping("/selectPersonAlbumFileInfo")
    @Operation(summary = "查询所有人物所对应的照片", description = "查询所有人物所对应的照片")
    public BaseResponse<?> selectPersonAlbumFileInfoString(Integer current, Integer size, String orderType, String orderKeyword, String imageTypeText, Long personId) {
        // 分组时间
        if ("upload_time".equals(orderKeyword)) {
            orderKeyword = "upload_time";
        } else {
            orderKeyword = "date_time_original";
        }
        // 排序方式
        if ("desc".equals(orderType)) {
            orderType = "desc";
        } else {
            orderType = "asc";
        }
        if (personId==null||current == null || size == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误!");
        }

        //图片类型
        Set<String> imageTypeTextSet = new HashSet<>(Arrays.asList("picture", "gif", "video"));
        imageTypeText = imageTypeTextSet.contains(imageTypeText) ? imageTypeText : "all";

        Long userId = Long.parseLong(StpUtil.getLoginId().toString());

        Page<FileInfoListVO> personFileInfo = personService.getPersonFileInfo(current, size, userId, orderType, orderKeyword, imageTypeText, personId);
        return ResultUtil.success(personFileInfo, "获取数据成功！");
    }

    @PostMapping("/updatePersonName")
    @Operation(summary = "修改人物名字", description = "查询所有按地点分组的相册")
    public BaseResponse<?> updatePersonName(@RequestBody PersonDTO person) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        person.setPersonRelation(null);
        person.setOldFaceId(person.getFaceId());
        personService.updatePerson(person,userId);
        return ResultUtil.success("名字已修改！");
    }


    @PostMapping("/updatePersonInfo")
    @Operation(summary = "修改人物信息", description = "查询所有按地点分组的相册")
    public BaseResponse<?> updatePersonInfo(@RequestBody PersonDTO person) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        Long personId = person.getPersonId();
        String personRelation = person.getPersonRelation();
        PersonRelation relation = PersonRelation.fromDisplayName(personRelation);
        Long faceId = person.getFaceId();
        if(relation == null){
            personRelation = null;
        }
        if(faceId!=null && faceId == -1){
            faceId = null;
        }

        personService.updatePerson(person,userId);
        return ResultUtil.success("人物信息已修改！");
    }



    @PostMapping("/removePersonPicture")
    @Operation(summary = "移除人物里的图片", description = "移除人物里的图片")
    public BaseResponse<?> removePersonPicture(Long personId,@RequestParam("fileIds") List<String> fileIds) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        if(personId == -1||fileIds.isEmpty()){
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误!");
        }
        personService.removePersonPicture(userId,personId,fileIds);
        return ResultUtil.success("人物信息已修改！");
    }



}
