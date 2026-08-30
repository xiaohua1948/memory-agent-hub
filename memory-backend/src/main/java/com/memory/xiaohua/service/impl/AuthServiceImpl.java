package com.memory.xiaohua.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import com.memory.xiaohua.mapper.UserMapper;
import com.memory.xiaohua.mapper.UserStorageMapper;
import com.memory.xiaohua.model.dto.UserAccountDTO;
import com.memory.xiaohua.model.entity.User;
import com.memory.xiaohua.model.entity.UserStorage;
import com.memory.xiaohua.model.vo.UserLoginVO;
import com.memory.xiaohua.service.AuthService;
import com.memory.xiaohua.utils.auth.PasswordUtil;
import com.memory.xiaohua.utils.auth.RedisUtil;
import com.memory.xiaohua.utils.auth.SendEmailUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/18,17:00
 */
@Service
public class AuthServiceImpl extends ServiceImpl<UserMapper, User> implements AuthService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private SendEmailUtil sendEmailUtil;

    @Resource
    private PasswordUtil passwordUtil;

    @Resource
    private UserStorageMapper userStorageMapper;


    @Override
    public Boolean accountRegister(UserAccountDTO userAccountDTO) {
        String account = userAccountDTO.getAccount();
        String password = userAccountDTO.getPassword();
        String email = userAccountDTO.getEmail();
        String code = userAccountDTO.getCode();
        // 1.校验
        if (account.length() < 6) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "用户账号少于6位!");
        }
        if (password.length() < 6) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "用户密码少于6位!");
        }
        if (sendEmailUtil.validate(email)){
            throw new BusinessException(StatusCode.PARAMS_ERROR, "邮箱格式错误!");
        }
        Object redisCode =  redisUtil.get("email:code:" + email);
        if (redisCode == null){
            throw new BusinessException(StatusCode.PARAMS_ERROR, "邮箱验证码错误!");
        }
        if(!redisCode.equals(code)){
            throw new BusinessException(StatusCode.PARAMS_ERROR, "邮箱验证码错误!");
        }
        // 2.查询是否存在
        QueryWrapper<User> QueryWrapper = new QueryWrapper<>();
        QueryWrapper.eq("account",account);
        User selectOne = userMapper.selectOne(QueryWrapper);
        if( selectOne != null ){
            throw new BusinessException(StatusCode.PARAMS_ERROR, "账户已注册，请换一个!");
        }
        QueryWrapper.clear();
        QueryWrapper.eq("email",email);
        selectOne = userMapper.selectOne(QueryWrapper);
        System.out.println(selectOne);
        if( selectOne != null ){
            throw new BusinessException(StatusCode.PARAMS_ERROR, "邮箱已注册，请换一个!");
        }
        User user = new User();
        user.setAccount(account);
        user.setPassword(passwordUtil.encode(password));
        user.setEmail(email);
        user.setUserName("小华"+ RandomUtil.randomString(4));
        // 3.注册,插入数据库
        int res = userMapper.insert(user);
        UserStorage userStorage = new UserStorage();
        userStorage.setUserId(user.getUserId());
        userStorageMapper.insert(userStorage);
        return res > 0;
    }

    @Override
    public UserLoginVO accountLogin(UserAccountDTO userAccountDTO) {
        String account = userAccountDTO.getAccount();
        String password = userAccountDTO.getPassword();
        // 1.校验
        if (StrUtil.hasBlank(account, password)) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数为空!");
        }
        if (account.length() < 5) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "用户账号错误！");
        }
        if (password.length() < 5) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "用户密码错误!");
        }
        // 2.查询
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("account", account);
        User user = userMapper.selectOne(queryWrapper);

        if(user == null){
            throw new BusinessException(StatusCode.PARAMS_ERROR, "用户账号不存在!");
        }
        if(!passwordUtil.matches(password,user.getPassword())){
            throw new BusinessException(StatusCode.PARAMS_ERROR, "用户账号或密码错误!");
        }
        UserLoginVO userLoginVO = new UserLoginVO();
        //so-token登录
        Long userId = user.getUserId();
        StpUtil.login(userId);
        String tokenValue = StpUtil.getTokenValue();
        userLoginVO.setToken(tokenValue);
        userLoginVO.setUser(user);
        return userLoginVO;
    }

    @Override
    public String getEmailCode(UserAccountDTO userAccountDTO) {
        String email = userAccountDTO.getEmail();
        String emailCode;
        try{
            emailCode = sendEmailUtil.sendEmailCode(email);
            if (sendEmailUtil.validate(email)){
                throw new BusinessException(StatusCode.PARAMS_ERROR, "邮箱格式错误!");
            }
        }
        catch (Exception e){
            throw new BusinessException(StatusCode.PARAMS_ERROR, "获取邮箱验证码失败!");
        }
        //验证码存入redis 有效期5min
        redisUtil.set("email:code:"+email,emailCode,5, TimeUnit.MINUTES);

        return emailCode;
    }

    @Override
    public UserLoginVO codeLogin(UserAccountDTO userAccountDTO) {
        // 1.校验
        String email = userAccountDTO.getEmail();
        String code = userAccountDTO.getCode();
        String redisCode = redisUtil.get("email:code:" + email).toString();
        if(!code.equals(redisCode)){
            throw new BusinessException(StatusCode.PARAMS_ERROR, "邮箱验证码错误!");
        }
        // 2.查询
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        User user = userMapper.selectOne(queryWrapper);
        if(user == null){
            throw new BusinessException(StatusCode.PARAMS_ERROR, "用户账户不存在!");
        }
        UserLoginVO userLoginVO = new UserLoginVO();
        //so-token登录
        StpUtil.login(user.getUserId());

        String tokenValue = StpUtil.getTokenValue();
        userLoginVO.setToken(tokenValue);
        userLoginVO.setUser(user);
        return userLoginVO;
    }

    @Override
    public void updatePassword(UserAccountDTO userAccountDTO) {
        // 1. 参数校验
        String oldPassword = userAccountDTO.getPassword();
        String newPassword = userAccountDTO.getNewPassword();

        if (StrUtil.hasBlank(oldPassword, newPassword)) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数不能为空!");
        }
        if (newPassword.length() < 6) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "密码长度至少6位!");
        }

        // 2. 获取当前登录用户
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "用户不存在!");
        }

        // 3. 验证旧密码
        if (!passwordUtil.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "原密码错误!");
        }

        // 4. 更新密码
        String encryptedPwd = passwordUtil.encode(newPassword);
        user.setPassword(encryptedPwd);
        int updateCount = userMapper.updateById(user);
        if (updateCount <= 0) {
            throw new BusinessException(StatusCode.SYSTEM_ERROR, "密码更新失败!");
        }
        StpUtil.logout();

    }

    @Override
    public void updateEmail(UserAccountDTO userAccountDTO) {
        // 1. 参数校验
        String newEmail = userAccountDTO.getEmail();
        String code = userAccountDTO.getCode();

        if (StrUtil.hasBlank(newEmail, code)) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数不能为空!");
        }
        if (sendEmailUtil.validate(newEmail)) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "邮箱格式不正确!");
        }

        // 2. 验证码校验
        String redisKey = "email:code:" + newEmail;
        String redisCode = redisUtil.get(redisKey).toString();
        if (!code.equals(redisCode)) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "验证码错误!");
        }

        // 3. 获取当前用户
        Long userId = StpUtil.getLoginIdAsLong();
        User currentUser = userMapper.selectById(userId);
        if (currentUser == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "用户不存在!");
        }

        // 4. 检查邮箱是否已被使用
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", newEmail)
                .ne("user_id", userId); // 排除当前用户
        Long existCount = userMapper.selectCount(wrapper);
        if (existCount > 0) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "该邮箱已被其他账号绑定!");
        }

        // 5. 更新邮箱
        currentUser.setEmail(newEmail);
        int updateCount = userMapper.updateById(currentUser);
        if (updateCount <= 0) {
            throw new BusinessException(StatusCode.SYSTEM_ERROR, "邮箱更新失败!");
        }

        // 6. 清理验证码缓存
        redisUtil.sClear(redisKey);
    }
}
