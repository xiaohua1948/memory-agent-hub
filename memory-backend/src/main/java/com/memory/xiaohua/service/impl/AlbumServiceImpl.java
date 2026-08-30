package com.memory.xiaohua.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.memory.xiaohua.mapper.AlbumMapper;
import com.memory.xiaohua.mapper.FileMapper;
import com.memory.xiaohua.model.entity.Album;
import com.memory.xiaohua.model.entity.FileEntity;
import com.memory.xiaohua.model.vo.FileInfoListVO;
import com.memory.xiaohua.model.vo.album.AlbumVO;
import com.memory.xiaohua.model.vo.album.LocationAlbumVO;
import com.memory.xiaohua.model.vo.album.ModelAlbumVO;
import com.memory.xiaohua.service.AlbumService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 相册信息表 服务实现类
 * </p>
 *
 * @author xiaohua
 * @since 2025-02-24
 */
@Service
public class AlbumServiceImpl extends ServiceImpl<AlbumMapper, Album> implements AlbumService {

    @Resource
    private AlbumMapper albumMapper;

    @Resource
    private FileMapper fileMapper;


    @Override
    public void addPictureToAlbum(List<String> fileIds, Long albumId, Long userId) {
        QueryWrapper<Album> albumQueryWrapper = new QueryWrapper<>();
        albumQueryWrapper.eq("album_id", albumId);
        Album album = albumMapper.selectOne(albumQueryWrapper);
        if (album != null) {
            for (String fileId : fileIds) {
                Long id = albumMapper.selectPictureIsExit(albumId, fileId);
                //id没有存在，才能加入相册
                if (id == null && fileId.length() == 32) {
                    QueryWrapper<FileEntity> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("file_id", fileId);
                    FileEntity fileEntity = fileMapper.selectOne(queryWrapper);
                    albumMapper.addPictureToAlbum(albumId, fileId, userId);
                }
            }
        }
    }

    @Override
    public void removePictureFromAlbum(List<String> fileIds, Long albumId, Long userId) {
        ArrayList<Long> albumIds = new ArrayList<>();

        if (albumId != null) {
            albumIds.add(albumId);
        }

        albumMapper.removePictureFromAlbum(albumIds, fileIds, userId);
    }

    @Override
    public Page<AlbumVO> selectAllAlbum(int current, int size, String orderKeyword, String orderType, Long userId) {
        Page<AlbumVO> page = new Page<>();
        page.setCurrent(current);
        page.setSize(size);

        List<AlbumVO> albums = albumMapper.selectAllAlbum(page, orderKeyword, orderType, userId);
        page.setRecords(albums);
        return page;
    }

    @Override
    public Page<LocationAlbumVO> selectAllLocationAlbum(int current, int size, String locationLevel, Long userId) {
        Page<LocationAlbumVO> page = new Page<>();
        page.setCurrent(current);
        page.setSize(size);
        List<LocationAlbumVO> locationAlbumVOS = albumMapper.selectAllLocationAlbum(page, userId, locationLevel);
        page.setRecords(locationAlbumVOS);
        return page;
    }

    @Override
    public Page<ModelAlbumVO> selectAllModelAlbum(int current, int size,  Long userId) {

        Page<ModelAlbumVO> page = new Page<>();
        page.setCurrent(current);
        page.setSize(size);
        List<ModelAlbumVO> modelAlbumVOS = albumMapper.selectAllModelAlbum(page, userId);
        page.setRecords(modelAlbumVOS);
        return page;
    }

    @Override
    public Page<FileInfoListVO> getModelFileInfo(Integer current, Integer size,Long userId, String orderType, String orderKeyword, String imageTypeText, String makeName, String modelName) {
        Page<FileInfoListVO> page = new Page<>();
        page.setCurrent(current);
        page.setSize(size);
        List<FileInfoListVO> modelFileInfo = albumMapper.getModelFileInfo(page,userId, orderType, orderKeyword, imageTypeText, makeName, modelName,false);
        page.setRecords(modelFileInfo);
        return page;
    }


    @Override
    public AlbumVO selectAlbumById(Long albumId, Long userId) {
        return albumMapper.selectAlbumById(albumId, userId);
    }

    @Override
    public Boolean updateAlbumCover(Long albumId, String fileId, Long userId) {
        //  是否存在该相册，存在才能设置封面
        Long id = albumMapper.selectPictureIsExit(albumId, fileId);
        if (id != null) {
            albumMapper.updateAlbumCover(albumId, fileId, userId);
            return true;
        }
        return false;
    }

    @Override
    public Boolean updateAlbumInfo(Album album, Long userId) {
        UpdateWrapper<Album> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("album_id", album.getAlbumId());
        updateWrapper.eq("user_id", userId);
        updateWrapper.set("album_name", album.getAlbumName());
        updateWrapper.set("description", album.getDescription());
        albumMapper.update(updateWrapper);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteAlbum(List<Long> albumIds, Boolean isDeletePicture, Long userId) {
        //逻辑删除照片
        if (isDeletePicture) {
            fileMapper.setIsDeletedByAlbumIds(albumIds, userId);
        }
        //删除相册表
        albumMapper.deleteAlbumByIds(albumIds, userId);
        //删除照片相册关联表
        albumMapper.removePictureFromAlbum(albumIds, null, userId);
        return true;
    }


}
