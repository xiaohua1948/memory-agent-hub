package com.memory.xiaohua.mapper;

import com.memory.xiaohua.model.entity.PictureTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.memory.xiaohua.model.vo.visual.FileTagVO;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author xiaohua
 * @since 2025-03-04
 */
public interface PictureTagMapper extends BaseMapper<PictureTag> {

    @Select("select tag_name from picture_tag join user_file  on picture_tag.file_id  = user_file.file_id where user_id = #{userId} and picture_tag.file_id = #{fileId}")
    List<String> selectTagsByFileId(String fileId,Long userId);
    List<FileTagVO> selectAllTags(Long userId);

}
