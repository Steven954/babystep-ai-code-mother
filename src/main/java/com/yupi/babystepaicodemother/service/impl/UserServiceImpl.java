package com.yupi.babystepaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yupi.babystepaicodemother.exception.BusinessException;
import com.yupi.babystepaicodemother.exception.ErrorCode;
import com.yupi.babystepaicodemother.model.dto.user.UserQueryRequest;
import com.yupi.babystepaicodemother.model.entity.User;
import com.yupi.babystepaicodemother.mapper.UserMapper;
import com.yupi.babystepaicodemother.model.enums.UserRoleEnum;
import com.yupi.babystepaicodemother.model.vo.LoginUserVO;
import com.yupi.babystepaicodemother.model.vo.UserVO;
import com.yupi.babystepaicodemother.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.yupi.babystepaicodemother.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 鐢ㄦ埛 鏈嶅姟灞傚疄鐜般€?
 *
 * @author <a href="https://github.com/liyupi">绋嬪簭鍛橀奔鐨?/a>
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 鏍￠獙鍙傛暟
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "鍙傛暟涓虹┖");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "璐﹀彿闀垮害杩囩煭");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "瀵嗙爜闀垮害杩囩煭");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "涓ゆ杈撳叆鐨勫瘑鐮佷笉涓€鑷?");
        }
        // 2. 鏌ヨ鐢ㄦ埛鏄惁宸插瓨鍦?
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "璐﹀彿閲嶅");
        }
        // 3. 鍔犲瘑瀵嗙爜
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 鍒涘缓鐢ㄦ埛锛屾彃鍏ユ暟鎹簱
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("鏃犲悕");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "娉ㄥ唽澶辫触锛屾暟鎹簱閿欒");
        }
        return user.getId();
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 鏍￠獙鍙傛暟
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "鍙傛暟涓虹┖");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "璐﹀彿闀垮害杩囩煭");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "瀵嗙爜闀垮害杩囩煭");
        }
        // 2. 鍔犲瘑
        String encryptPassword = getEncryptPassword(userPassword);
        // 3. 鏌ヨ鐢ㄦ埛鏄惁瀛樺湪
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "鐢ㄦ埛涓嶅瓨鍦ㄦ垨瀵嗙爜閿欒");
        }
        // 4. 濡傛灉鐢ㄦ埛瀛樺湪锛岃褰曠敤鎴风殑鐧诲綍鎬?
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 5. 杩斿洖鑴辨晱鐨勭敤鎴蜂俊鎭?
        return this.getLoginUserVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 鍏堝垽鏂敤鎴锋槸鍚︾櫥褰?
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 浠庢暟鎹簱鏌ヨ褰撳墠鐢ㄦ埛淇℃伅
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 鍏堝垽鏂敤鎴锋槸鍚︾櫥褰?
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "鐢ㄦ埛鏈櫥褰?");
        }
        // 绉婚櫎鐧诲綍鎬?
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "璇锋眰鍙傛暟涓虹┖");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id) // where id = ${id}
                .eq("userRole", userRole) // and userRole = ${userRole}
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        // 鐩愬€硷紝娣锋穯瀵嗙爜
        final String SALT = "yupi";
        return DigestUtils.md5DigestAsHex((userPassword + SALT).getBytes(StandardCharsets.UTF_8));
    }
}

