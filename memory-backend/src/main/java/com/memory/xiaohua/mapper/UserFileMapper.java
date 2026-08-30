package com.memory.xiaohua.mapper;

import com.memory.xiaohua.model.entity.FileEntity;
import com.memory.xiaohua.model.entity.UserFileEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 用户-文件关联表 Mapper 接口
 * </p>
 *
 * @author xiaohua
 * @since 2025-02-27
 */
public interface UserFileMapper extends BaseMapper<UserFileEntity> {
    void updateUseFile(Long userId, List<String> fileIds, boolean isDeleted);

    void dropFile(Long userId,@Param("fileIds") List<String> fileIds);
    @Select("SELECT file.* ,uf.file_id FROM file left join user_file uf on file.file_id = uf.file_id  WHERE (is_deleted = 1 AND DATE_ADD(deleted_time, INTERVAL 30 DAY) <= NOW()) OR (uf.file_id is null )")
    List<FileEntity> selectExpiredPicture();

    void dropAllFile(@Param("fileIds") List<String> fileIds);

}
