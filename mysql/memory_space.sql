/*
 Navicat Premium Data Transfer

 Source Server         : mysql
 Source Server Type    : MySQL
 Source Server Version : 80027 (8.0.27)
 Source Host           : localhost:3306
 Source Schema         : memory_space

 Target Server Type    : MySQL
 Target Server Version : 80027 (8.0.27)
 File Encoding         : 65001

 Date: 18/04/2025 21:20:23
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for album
-- ----------------------------
DROP TABLE IF EXISTS `album`;
CREATE TABLE `album`  (
  `album_id` bigint NOT NULL AUTO_INCREMENT COMMENT '相册ID',
  `album_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '相册名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '相册描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `user_id` bigint NOT NULL COMMENT '用户ID（关联user.id）',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0正常 1公开 2私密）',
  PRIMARY KEY (`album_id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  CONSTRAINT `album_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1000000057 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '相册信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for album_picture
-- ----------------------------
DROP TABLE IF EXISTS `album_picture`;
CREATE TABLE `album_picture`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '关联记录ID',
  `album_id` bigint NOT NULL COMMENT '相册ID（关联album.album_id）',
  `file_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件ID（关联file.file_id）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_cover` tinyint(1) NULL DEFAULT 0,
  `user_id` bigint NOT NULL COMMENT '关联表user.id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_album_file`(`album_id` ASC, `file_id` ASC) USING BTREE,
  INDEX `idx_album_id`(`album_id` ASC) USING BTREE,
  INDEX `idx_file_id`(`file_id` ASC) USING BTREE,
  INDEX `idx_user_Id_index`(`user_id` ASC) USING BTREE,
  CONSTRAINT `album_picture__ibfk3` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `album_picture_ibfk_1` FOREIGN KEY (`album_id`) REFERENCES `album` (`album_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `album_picture_ibfk_2` FOREIGN KEY (`file_id`) REFERENCES `file` (`file_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 395 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '相册-图片关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for face
-- ----------------------------
DROP TABLE IF EXISTS `face`;
CREATE TABLE `face`  (
  `face_id` bigint NOT NULL AUTO_INCREMENT COMMENT '人像唯一标识',
  `file_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件标识(36位UUID)',
  `is_processed` tinyint(1) NULL DEFAULT 0 COMMENT '是否已处理',
  `feature_vector` longblob NULL COMMENT '128维特征向量(BLOB存储float数组)',
  `person_cover_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '封面图URL',
  `person_object_name` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '对象名称',
  `create_time` datetime NULL DEFAULT (now()) COMMENT '创建时间',
  `is_face` tinyint(1) NULL DEFAULT 0,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`face_id`) USING BTREE,
  INDEX `idx_file`(`file_id` ASC) USING BTREE,
  INDEX `face_user_id_fk`(`user_id` ASC) USING BTREE,
  CONSTRAINT `f` FOREIGN KEY (`file_id`) REFERENCES `file` (`file_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `face_ibfk_1` FOREIGN KEY (`file_id`) REFERENCES `file` (`file_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `face_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 493 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '人像特征表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for file
-- ----------------------------
DROP TABLE IF EXISTS `file`;
CREATE TABLE `file`  (
  `file_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件唯一ID UUID',
  `origin_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原始文件名（带扩展名）',
  `size` bigint NOT NULL COMMENT '文件大小（单位：字节）',
  `content_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'MIME类型（例：image/png）',
  `category` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件分类（image/doc/video等）',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件访问URL（CDN地址）',
  `thumbnail_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '缩略图URL（图片专用）',
  `file_object_name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件对象存储路径',
  `thumbnail_object_name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '缩略图对象存储',
  `md5` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件hash',
  `location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件(图片或视频)的地理位置',
  `width` int NULL DEFAULT NULL COMMENT '图像宽度（像素）',
  `height` int NULL DEFAULT NULL COMMENT '图像高度（像素）',
  `make` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT ' ' COMMENT '设备制造商',
  `model` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT ' ' COMMENT '设备型号',
  `last_modified_time` datetime NOT NULL COMMENT '客户端最后修改时间',
  `date_time_original` datetime NULL DEFAULT NULL COMMENT '原始拍摄时间',
  `longitude` double NULL DEFAULT NULL COMMENT '经度',
  `latitude` double NULL DEFAULT NULL COMMENT '纬度',
  `longitude_ref` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '经度参考方向(E/W)',
  `latitude_ref` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '纬度参考方向(N/S)',
  PRIMARY KEY (`file_id`) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_content_type`(`content_type` ASC) USING BTREE,
  INDEX `file_date_time_original_index`(`date_time_original` ASC) USING BTREE COMMENT '拍摄时间索引',
  INDEX `file_last_modified_time_index`(`last_modified_time` ASC) USING BTREE COMMENT '最后修改时间索引',
  INDEX `file_location_index`(`location` ASC) USING BTREE COMMENT '地理位置索引',
  INDEX `file_make_index`(`make` ASC) USING BTREE,
  INDEX `file_model_index`(`model` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for image_meta_data
-- ----------------------------
DROP TABLE IF EXISTS `image_meta_data`;
CREATE TABLE `image_meta_data`  (
  `file_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件唯一标识',
  `software` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '处理软件',
  `exposure_program` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '曝光程序',
  `exposure_time` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '曝光时间（秒）',
  `f_number` double NULL DEFAULT NULL COMMENT 'F数',
  `iso` int NULL DEFAULT NULL COMMENT 'ISO感光度',
  `focal_length` double NULL DEFAULT NULL COMMENT '实际焦距（mm）',
  `focal_length35` double NULL DEFAULT NULL COMMENT '等效35mm焦距',
  `altitude` double NULL DEFAULT NULL COMMENT '海拔高度',
  `altitude_ref` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '海拔基准',
  `version` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'EXIF版本',
  `aperture_value` double NULL DEFAULT NULL COMMENT '光圈值',
  `shutter_speed` double NULL DEFAULT NULL COMMENT '快门速度值',
  `metering_mode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '测光模式',
  `white_balance` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '白平衡模式',
  `color_space` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '色彩空间',
  `sensing_method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '传感器类型',
  `subject_distance` double NULL DEFAULT NULL COMMENT '拍摄对象距离（米）',
  `scene_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '场景类型',
  PRIMARY KEY (`file_id`) USING BTREE,
  CONSTRAINT `image_meta_data_file_file_id_fk` FOREIGN KEY (`file_id`) REFERENCES `file` (`file_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '图像元数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for location
-- ----------------------------
DROP TABLE IF EXISTS `location`;
CREATE TABLE `location`  (
  `file_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `full_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '完整地址',
  `country` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '国家',
  `province` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `district` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `township` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `street` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `street_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`file_id`) USING BTREE,
  CONSTRAINT `location_file_file_id_fk` FOREIGN KEY (`file_id`) REFERENCES `file` (`file_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for person
-- ----------------------------
DROP TABLE IF EXISTS `person`;
CREATE TABLE `person`  (
  `person_id` bigint NOT NULL AUTO_INCREMENT COMMENT '人物唯一标识',
  `person_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '人物姓名',
  `person_relation` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '无关系' COMMENT '人物关系(如：父子/同事)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `display` tinyint(1) NULL DEFAULT 1 COMMENT '是否隐藏',
  `user_id` bigint NOT NULL COMMENT '用户Id',
  PRIMARY KEY (`person_id`) USING BTREE,
  INDEX `idx_name`(`person_name` ASC) USING BTREE,
  INDEX `person_user_id_fk`(`user_id` ASC) USING BTREE,
  CONSTRAINT `person_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 151 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '人物信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for person_face
-- ----------------------------
DROP TABLE IF EXISTS `person_face`;
CREATE TABLE `person_face`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `person_id` bigint NOT NULL,
  `face_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `representative` tinyint(1) NOT NULL DEFAULT 0 COMMENT '代表',
  `update_time` datetime NOT NULL DEFAULT (now()),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `person_face_user_id_fk`(`user_id` ASC) USING BTREE,
  INDEX `person_face_face_face_id_fk`(`face_id` ASC) USING BTREE,
  INDEX `person_face_person_person_id_fk`(`person_id` ASC) USING BTREE,
  CONSTRAINT `person_face_face_face_id_fk` FOREIGN KEY (`face_id`) REFERENCES `face` (`face_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `person_face_person_person_id_fk` FOREIGN KEY (`person_id`) REFERENCES `person` (`person_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `person_face_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 221 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '人物脸部关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for picture_tag
-- ----------------------------
DROP TABLE IF EXISTS `picture_tag`;
CREATE TABLE `picture_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `image_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `tag_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `picture_tag_pk`(`file_id` ASC, `tag_name` ASC) USING BTREE,
  CONSTRAINT `picture_tag_file_file_id_fk` FOREIGN KEY (`file_id`) REFERENCES `file` (`file_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 162 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for record
-- ----------------------------
DROP TABLE IF EXISTS `record`;
CREATE TABLE `record`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `operation` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作名称',
  `number` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '操作数量',
  `ipv4` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作者IP地址（IPv4格式）',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '操作者用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_time_operation`(`operation_time` ASC) USING BTREE,
  INDEX `idx_userId`(`user_id` ASC) USING BTREE,
  INDEX `idx_ipv4`(`ipv4` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 861 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for similar_picture
-- ----------------------------
DROP TABLE IF EXISTS `similar_picture`;
CREATE TABLE `similar_picture`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `similar_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `file_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `similar_picture_file_file_id_fk`(`file_id` ASC) USING BTREE,
  INDEX `similar_picture_user_id_fk`(`user_id` ASC) USING BTREE,
  CONSTRAINT `similar_picture_file_file_id_fk` FOREIGN KEY (`file_id`) REFERENCES `file` (`file_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `similar_picture_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 301 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名称',
  `account` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户账号',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户密码',
  `email` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户邮箱',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `profile` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '用户简介',
  `avatar_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `avatar_object_name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `account`(`account` ASC) USING BTREE,
  UNIQUE INDEX `email`(`email` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1000000007 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_file
-- ----------------------------
DROP TABLE IF EXISTS `user_file`;
CREATE TABLE `user_file`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `user_id` bigint NOT NULL COMMENT '用户ID（关联user.id）',
  `file_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件ID（关联file.file_id）',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '删除状态：0-正常，1-已删除',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '文件关联时间',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间（is_deleted=1时有效）',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_user_file`(`user_id` ASC, `file_id` ASC) USING BTREE COMMENT '防止重复关联',
  INDEX `idx_user_upload`(`user_id` ASC, `upload_time` ASC) USING BTREE COMMENT '用户文件上传时间排序',
  INDEX `idx_file_status`(`file_id` ASC, `is_deleted` ASC) USING BTREE COMMENT '文件状态查询',
  CONSTRAINT `fk_user_file_file` FOREIGN KEY (`file_id`) REFERENCES `file` (`file_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1912739291209830403 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户-文件关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_storage
-- ----------------------------
DROP TABLE IF EXISTS `user_storage`;
CREATE TABLE `user_storage`  (
  `user_id` bigint NOT NULL COMMENT '用户ID（关联user.id）',
  `total_space` bigint NOT NULL DEFAULT 5368709120 COMMENT '总存储空间（单位：字节）',
  `used_space` bigint NOT NULL DEFAULT 0 COMMENT '已使用空间（单位：字节）',
  `account_status` enum('normal','vip','svip') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'normal' COMMENT '身份状态',
  `membership_days` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '会员剩余天数',
  PRIMARY KEY (`user_id`) USING BTREE,
  INDEX `idx_account_status`(`account_status` ASC) USING BTREE,
  CONSTRAINT `user_storage_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户存储信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for video_meta_data
-- ----------------------------
DROP TABLE IF EXISTS `video_meta_data`;
CREATE TABLE `video_meta_data`  (
  `file_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件唯一标识（关联文件表主键）',
  `color_space` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '色彩空间',
  `duration` double NULL DEFAULT NULL COMMENT '时长（秒）',
  `fps` double NULL DEFAULT NULL COMMENT '帧率',
  `rotation` double NULL DEFAULT NULL COMMENT '旋转角度',
  `video_codec_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '视频编码格式名称',
  `video_codec` int NULL DEFAULT NULL COMMENT '视频编码类型代码',
  `video_bitrate` int NULL DEFAULT NULL COMMENT '视频码率（bps）',
  `audio_codec_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '音频编码格式名称',
  `audio_codec` int NULL DEFAULT NULL COMMENT '音频编码类型代码',
  `audio_sample_rate` int NULL DEFAULT NULL COMMENT '采样率（Hz）',
  `audio_channels` int NULL DEFAULT NULL COMMENT '声道数',
  `copyright` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '版权信息',
  `language` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '语言',
  `profile` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '编码配置',
  `level` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '编码级别',
  `pixel_format` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '像素格式',
  PRIMARY KEY (`file_id`) USING BTREE,
  INDEX `idx_duration`(`duration` ASC) USING BTREE COMMENT '按视频时长过滤',
  CONSTRAINT `fk_video_file` FOREIGN KEY (`file_id`) REFERENCES `file` (`file_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '视频元数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Procedure structure for DecrementMembershipDays
-- ----------------------------
DROP PROCEDURE IF EXISTS `DecrementMembershipDays`;
delimiter ;;
CREATE PROCEDURE `DecrementMembershipDays`()
BEGIN
    -- 步骤1: 递减会员天数并降级身份状态
    UPDATE user_storage
    SET membership_days = GREATEST(membership_days - 1, 0),
        account_status = CASE
                             WHEN membership_days - 1 <= 0 THEN 'normal'
                             ELSE account_status
            END
    WHERE membership_days > 0;

    -- 步骤2: 同步更新总空间（通过触发器自动完成）
    -- 此处无需额外操作，由BEFORE UPDATE触发器处理
END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table user_file
-- ----------------------------
DROP TRIGGER IF EXISTS `trg_user_file_insert`;
delimiter ;;
CREATE TRIGGER `trg_user_file_insert` AFTER INSERT ON `user_file` FOR EACH ROW BEGIN

    UPDATE user_storage
    SET used_space = used_space + (
        SELECT size FROM file WHERE file_id = New.file_id
    )
    WHERE user_id = New.user_id;

END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table user_file
-- ----------------------------
DROP TRIGGER IF EXISTS `trg_user_file_delete`;
delimiter ;;
CREATE TRIGGER `trg_user_file_delete` AFTER DELETE ON `user_file` FOR EACH ROW BEGIN

        UPDATE user_storage
        SET used_space = used_space - (
            SELECT size FROM file WHERE file_id = OLD.file_id
        )
        WHERE user_id = OLD.user_id;

END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table user_storage
-- ----------------------------
DROP TRIGGER IF EXISTS `UpdateTotalSpaceOnStatusChange`;
delimiter ;;
CREATE TRIGGER `UpdateTotalSpaceOnStatusChange` BEFORE UPDATE ON `user_storage` FOR EACH ROW BEGIN
    -- 当身份状态变化时更新总空间
    IF NEW.account_status != OLD.account_status THEN
        SET NEW.total_space = CASE NEW.account_status
                                  WHEN 'normal' THEN 5368709120    -- 5GB
                                  WHEN 'vip' THEN 10737418240       -- 10GB
                                  WHEN 'svip' THEN 21474836480      -- 20GB
                                  ELSE NEW.total_space
            END;
    END IF;
END
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;
