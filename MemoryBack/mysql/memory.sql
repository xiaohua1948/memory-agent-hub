/*
Navicat MySQL Data Transfer

Source Server         : localhost_3306
Source Server Version : 80027
Source Host           : localhost:3306
Source Database       : memory

Target Server Type    : MYSQL
Target Server Version : 80027
File Encoding         : 65001

Date: 2022-09-02 19:27:39
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for album
-- ----------------------------
DROP TABLE IF EXISTS `album`;
CREATE TABLE `album` (
  `id` int NOT NULL AUTO_INCREMENT,
  `albumName` varchar(30) DEFAULT NULL,
  `albumTheme` varchar(50) DEFAULT NULL,
  `albumContext` text,
  `albumImg` varchar(100) DEFAULT '/static/album/albumImg.png',
  `backgroundImage` varchar(100) DEFAULT NULL,
  `albumMusic` varchar(100) DEFAULT NULL,
  `userId` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `album_id_uindex` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for album_image
-- ----------------------------
DROP TABLE IF EXISTS `album_image`;
CREATE TABLE `album_image` (
  `id` int NOT NULL AUTO_INCREMENT,
  `albumId` int DEFAULT NULL,
  `imageId` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `album_image_id_uindex` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=99 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for image
-- ----------------------------
DROP TABLE IF EXISTS `image`;
CREATE TABLE `image` (
  `id` int NOT NULL AUTO_INCREMENT,
  `imageName` varchar(100) DEFAULT NULL,
  `imageSize` int DEFAULT NULL,
  `imageSite` varchar(30) DEFAULT NULL,
  `imageUrL` varchar(120) DEFAULT NULL,
  `compressUrL` varchar(120) DEFAULT NULL,
  `imageDate` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `image_id_uindex` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=219 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for image_type
-- ----------------------------
DROP TABLE IF EXISTS `image_type`;
CREATE TABLE `image_type` (
  `id` int NOT NULL AUTO_INCREMENT,
  `imageId` int DEFAULT NULL,
  `imageType` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `imageType_id_uindex` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=352 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for record
-- ----------------------------
DROP TABLE IF EXISTS `record`;
CREATE TABLE `record` (
  `id` int NOT NULL AUTO_INCREMENT,
  `date` datetime DEFAULT NULL,
  `operation` varchar(30) DEFAULT NULL,
  `number` int DEFAULT NULL,
  `ipv4` varchar(50) DEFAULT NULL,
  `userId` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `record_id_uindex` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=136 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for recycle
-- ----------------------------
DROP TABLE IF EXISTS `recycle`;
CREATE TABLE `recycle` (
  `id` int NOT NULL AUTO_INCREMENT,
  `userId` int DEFAULT NULL,
  `imageId` int DEFAULT NULL,
  `recycleDate` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `recycle_id_uindex` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `userName` varchar(25) NOT NULL,
  `passWord` varchar(25) NOT NULL,
  `sex` varchar(10) DEFAULT NULL,
  `email` varchar(30) DEFAULT NULL,
  `phone` varchar(15) DEFAULT NULL,
  `city` varchar(25) DEFAULT NULL,
  `birthday` date DEFAULT NULL,
  `capacity` int DEFAULT '1000',
  `avatar` varchar(100) DEFAULT '/static/avatar/default.jpg',
  UNIQUE KEY `user_id_uindex` (`id`),
  UNIQUE KEY `user_userName_uindex` (`userName`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for user_image
-- ----------------------------
DROP TABLE IF EXISTS `user_image`;
CREATE TABLE `user_image` (
  `id` int NOT NULL AUTO_INCREMENT,
  `userId` int DEFAULT NULL,
  `imageId` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_image_id_uindex` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=154 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
