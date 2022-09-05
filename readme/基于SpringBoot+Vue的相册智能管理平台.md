### 基于SpringBoot+Vue的智能相册管理平台

##### 基础开发环境：为避免搭建项目，出现问题这里建议统一开发环建版本。

#### 前后端分离项目：

##### 服务端：

- ##### JDK:1.8

- ##### Maven:3.5+

- ##### Mysql：8.0

- ##### Redis：3.2+

##### 前端：

- ##### Node.js:14.18+

- ##### Npm:6.14.5+

##### IdE:

- ##### IDEA2021

- ##### Visual Studio Code

##### 所用技术及API：

#####       此系统前端采用vue2.0cli脚手架开发，主要采用饿了么的前端框架 elementUI，进行前端页面的开发，服务端采用springboot2.7，使用阿里云图像识别技术，来对图像智能识别分类，redis用作验证码的存储。

##### 使用技术及框架：

##### 服务端：

- ##### Springboot2.7

- ##### mysql 8.0

- ##### mybatis 3.5.2

- ##### redis:3.2(windows版) ,服务器liunx采用redis7.0

- ##### 阿里云图像识别

- ##### jjwt(请求token)

- ##### thumbnailator(图片压缩工具)

##### 前端：

- ##### vue 2.6.7  elementui2.15.8 ,少量ant design 组件

- ##### echarts.js

- ##### axios

#### 项目配置：

![image-20220903223849408](F:\Desktop\Typora笔记\开发笔记\images\image-20220903223849408.png)

![L~5F3EDCSDO1KYH40%BIZIQ](F:\Desktop\Typora笔记\开发笔记\images\L~5F3EDCSDO1KYH40%BIZIQ.png)

#### 项目整个代码目录：

![image-20220903224632382](C:\Users\16090\AppData\Roaming\Typora\typora-user-images\image-20220903224632382.png)

![image-20220903224829961](C:\Users\16090\AppData\Roaming\Typora\typora-user-images\image-20220903224829961.png)

![image-20220903224955358](C:\Users\16090\AppData\Roaming\Typora\typora-user-images\image-20220903224955358.png)



#### 总结：

#####        整个小项目从无到有，耗时一到两周完成，基本实现了市面上主流图片管理网站的大多数功能，前后端，都是自己一点点慢慢写的(只有自己才知道里面的艰辛)。一边想实现思路，一边写前端，一边写后端，一边测试最终才完成这么一个项目,并部署到服务器可以使用。其中也经历了无数的bug与问题，都一一解决。虽然做的可能不是太好，但也是一种经历,能提高自己的编程水平。

##### 最后附上效果图：

#### 1.首页：

![image-20220903212109886](F:\Desktop\Typora笔记\开发笔记\images\image-20220903212109886.png)

![image-20220903211800057](F:\Desktop\Typora笔记\开发笔记\images\image-20220903211800057.png)

#### 2.相册

![image-20220903213011941](F:\Desktop\Typora笔记\开发笔记\images\image-20220903213011941.png)

![image-20220903213840773](F:\Desktop\Typora笔记\开发笔记\images\image-20220903213840773.png)

#### 3.回收站

![image-20220903225505541](F:\Desktop\Typora笔记\开发笔记\images\image-20220903225505541.png)

#### 4.操作记录

![image-20220903225539681](F:\Desktop\Typora笔记\开发笔记\images\image-20220903225539681.png)

#### 5.图片数据可视化

![image-20220903225605720](F:\Desktop\Typora笔记\开发笔记\images\image-20220903225605720.png)

![image-20220903225619413](F:\Desktop\Typora笔记\开发笔记\images\image-20220903225619413.png)

#### 6.上传

![image-20220903213543679](F:\Desktop\Typora笔记\开发笔记\images\image-20220903213543679.png)

#### 7.个人中心

![image-20220903225829412](F:\Desktop\Typora笔记\开发笔记\images\image-20220903225829412.png)

##### 最后附上项目地址：

xiaohua.live