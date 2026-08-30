declare namespace API {
  // 响应data
  interface BaseResponse {
    code?: number
    data?: any
    message?: string
  }

  interface UserInfo {
    /** 用户ID (长整型数字) */
    userId: number;
    /** 用户名 */
    userName: string;
    /** 用户账号 */
    account: string;
    /** 绑定邮箱 */
    email: string;
    /** 账号创建时间 (ISO 8601 格式字符串) */
    createTime: string;
    /** 最后更新时间 (ISO 8601 格式字符串) */
    updateTime: string;
    /** 个人简介 */
    profile: string;
    /** 头像 URL (包含签名参数) */
    avatarUrl: string;
    /** 总存储空间 (单位：字节) */
    totalSpace: number;
    /** 已用存储空间 (单位：字节) */
    usedSpace: number;
    /** 账号状态 (枚举值) */
    accountStatus: 'normal' | 'vip' | 'svip';
    /** 会员剩余天数 */
    membershipDays: number;
  }


  interface FileInfoList {
    time: string; // ISO格式日期字符串 "YYYY-MM-DD"
    fileList: FileInfo[];
  }

  interface FileInfo {
    fileId: string;
    userId: number;
    originFileName: string;
    size: number;
    width: number;
    height: number;
 
    contentType: string; // 可细化成联合类型，如 "image/jpeg" | "image/png" | "video/mp4" 等
    category: string;    // 可细化成联合类型，如 "image" | "video" | "doc" 等
    duration: number | null;
    fileUrl: string;
    thumbnailUrl: string;
    thumbnailObjectName: string // 可选属性，图片类文件才有
  }

  interface albumInfo {
    albumId: number;
    userId: number;
    albumName: string;
    coverUrl: string;
    createTime: string;
    updateTime: string;
    imageCount: number;
    videoCount: number;
    description: string;
  }

  interface locationAlbum {
    locationLevel: string;
    locationValue: string;
    coverUrl: string;
    total: number;
  }

  interface PersonAlbum {
    personId: number;
    personName: string;
    personRelation: string;
    coverUrl: string;
    total: number;
    faceId: number;
    createTime: string;
  }

  interface ModelAlbum {

    makeName: string;
    modelName: string;
    coverUrl: string;
    total: number;
  }





  interface FileMetaData {
    fileInfo: FileEntity;
    videoMetaData: VideoMetaData;
    imageMetaData: ImageMetaData;
  }
  /**
 * 文件基础信息
 */
  interface FileEntity {
    /**​ 文件唯一ID (UUID格式) */
    fileId: string;

    /**​ 原始文件名（带扩展名） */
    originFileName: string;

    /**​ 文件大小（单位：字节） */
    size: number;
    /**​ 上传时间（ISO 8601格式） */
    uploadTime: string;


    /**​ 客户端最后修改时间 (ISO 8601格式) */
    lastModifiedTime: string;

    /**​ MIME类型 */
    contentType: string; // 可细化成联合类型："image/jpeg" | "image/png" | "video/mp4" | ...

    /**​ 文件分类 */
    category: string;

    /**​ 文件访问URL（CDN地址） */
    fileUrl: string;

    /**​ 缩略图URL（仅图片类文件存在） */
    thumbnailUrl?: string;

    /**​ 对象存储路径 */
    objectName: string;

    /**​ 文件哈希值（MD5） */
    md5: string;

    /**​ 地理位置描述（如："北京市朝阳区"） */
    location?: string;

    /**​ 媒体元数据 -------------------------------------------------- */

    /**​ 原始拍摄时间（EXIF信息） */
    dateTimeOriginal?: string;

    /**​ 媒体宽度（像素） */
    width?: number;

    /**​ 媒体高度（像素） */
    height?: number;

    /**​ 设备制造商（如：Sony/Canon/Apple） */
    make?: string;

    /**​ 设备型号（如：ILCE-7M4/iPhone 15 Pro） */
    model?: string;

    /**​ GPS地理坐标 ------------------------------------------------- */

    /**​ 纬度（十进制格式，范围-90~90） */
    latitude?: number;

    /**​ 纬度参考方向：N-北纬/S-南纬 */
    latitudeRef?: "";

    /**​ 经度（十进制格式，范围-180~180） */
    longitude?: number;

    /**​ 经度参考方向：E-东经/W-西经 */
    longitudeRef?: "";
  }

  /**
   * 视频文件扩展元数据（通过继承扩展）
   */
  interface VideoMetaData {
    /**​ 视频时长（秒，精确到小数点后2位） */
    duration: number;

    /**​ 视频编码格式 */
    videoCodecName: "";

    /**​ 视频码率（kbps） */
    videoBitrate: number;

    /**​ 帧率（fps） */
    fps: number;

    /**​ 视频旋转角度 */
    rotation?: 0 | 90 | 180 | 270;
  }

  /**
   * 图片文件扩展元数据（通过继承扩展）
   */
  interface ImageMetaData {
    /**​ 光圈值（如：f/2.8） */
    apertureValue?: number;

    /**​ 快门速度（如：1/400s） */
    shutterSpeed?: string;

    /**​ ISO感光度 */
    iso?: number;

    /**​ 焦距（单位：mm） */
    focalLength?: number;

    /**​ 白平衡模式 */
    whiteBalance?: "Auto" | "Manual";
  }

  interface SelectPicture {
    fileId: string;
    size: number;
    originFileName: string;
    thumbnailUrl: string;
    contentType: string;
    thumbnailObjectName: string;
    tags: TagResult[]
    selectedTagIndex: number;
  }
  interface TagResult {
    imageType: string;
    tagName: string;
    confidence: number;
  }

  interface PictureTag {
    id: number;
    fileId: string;
    tagName: string;
    imageType: string;
}
}  
