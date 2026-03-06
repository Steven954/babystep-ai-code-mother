package com.yupi.babystepaicodemother.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.yupi.babystepaicodemother.annotation.AuthCheck;
import com.yupi.babystepaicodemother.common.BaseResponse;
import com.yupi.babystepaicodemother.common.DeleteRequest;
import com.yupi.babystepaicodemother.common.ResultUtils;
import com.yupi.babystepaicodemother.constant.AppConstant;
import com.yupi.babystepaicodemother.constant.UserConstant;
import com.yupi.babystepaicodemother.exception.BusinessException;
import com.yupi.babystepaicodemother.exception.ErrorCode;
import com.yupi.babystepaicodemother.exception.ThrowUtils;
import com.yupi.babystepaicodemother.model.dto.app.*;
import com.yupi.babystepaicodemother.model.entity.User;
import com.yupi.babystepaicodemother.model.vo.AppVO;
import com.yupi.babystepaicodemother.ratelimter.annotation.RateLimit;
import com.yupi.babystepaicodemother.ratelimter.enums.RateLimitType;
import com.yupi.babystepaicodemother.service.ProjectDownloadService;
import com.yupi.babystepaicodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import com.yupi.babystepaicodemother.model.entity.App;
import com.yupi.babystepaicodemother.service.AppService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 搴旂敤 鎺у埗灞傘€?
 *
 * @author <a href="https://github.com/liyupi">绋嬪簭鍛橀奔鐨?/a>
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private ProjectDownloadService projectDownloadService;

    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(limitType = RateLimitType.USER, rate = 5, rateInterval = 60, message = "AI 瀵硅瘽璇锋眰杩囦簬棰戠箒锛岃绋嶅悗鍐嶈瘯")
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                       @RequestParam String message,
                                                       HttpServletRequest request) {
        // 鍙傛暟鏍￠獙
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "搴旂敤 id 閿欒");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "鎻愮ず璇嶄笉鑳戒负绌?");
        // 鑾峰彇褰撳墠鐧诲綍鐢ㄦ埛
        User loginUser = userService.getLoginUser(request);
        // 璋冪敤鏈嶅姟鐢熸垚浠ｇ爜锛圫SE 娴佸紡杩斿洖锛?
        Flux<String> contentFlux = appService.chatToGenCode(appId, message, loginUser);
        return contentFlux
                .map(chunk -> {
                    Map<String, String> wrapper = Map.of("d", chunk);
                    String jsonData = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder()
                            .data(jsonData)
                            .build();
                })
                .concatWith(Mono.just(
                        // 鍙戦€佺粨鏉熶簨浠?
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ));
    }

    /**
     * 搴旂敤閮ㄧ讲
     *
     * @param appDeployRequest 閮ㄧ讲璇锋眰
     * @param request          璇锋眰
     * @return 閮ㄧ讲 URL
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        // 妫€鏌ラ儴缃茶姹傛槸鍚︿负绌?
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        // 鑾峰彇搴旂敤 ID
        Long appId = appDeployRequest.getAppId();
        // 妫€鏌ュ簲鐢?ID 鏄惁涓虹┖
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "搴旂敤 ID 涓嶈兘涓虹┖");
        // 鑾峰彇褰撳墠鐧诲綍鐢ㄦ埛
        User loginUser = userService.getLoginUser(request);
        // 璋冪敤鏈嶅姟閮ㄧ讲搴旂敤
        String deployUrl = appService.deployApp(appId, loginUser);
        // 杩斿洖閮ㄧ讲 URL
        return ResultUtils.success(deployUrl);
    }

    /**
     * 涓嬭浇搴旂敤浠ｇ爜
     *
     * @param appId    搴旂敤ID
     * @param request  璇锋眰
     * @param response 鍝嶅簲
     */
    @GetMapping("/download/{appId}")
    public void downloadAppCode(@PathVariable Long appId,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        // 1. 鍩虹鏍￠獙
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "搴旂敤ID鏃犳晥");
        // 2. 鏌ヨ搴旂敤淇℃伅
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "搴旂敤涓嶅瓨鍦?");
        // 3. 鏉冮檺鏍￠獙锛氬彧鏈夊簲鐢ㄥ垱寤鸿€呭彲浠ヤ笅杞戒唬鐮?
        User loginUser = userService.getLoginUser(request);
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "鏃犳潈闄愪笅杞借搴旂敤浠ｇ爜");
        }
        // 4. 鏋勫缓搴旂敤浠ｇ爜鐩綍璺緞锛堢敓鎴愮洰褰曪紝闈為儴缃茬洰褰曪級
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 5. 妫€鏌ヤ唬鐮佺洰褰曟槸鍚﹀瓨鍦?
        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(),
                ErrorCode.NOT_FOUND_ERROR, "搴旂敤浠ｇ爜涓嶅瓨鍦紝璇峰厛鐢熸垚浠ｇ爜");
        // 6. 鐢熸垚涓嬭浇鏂囦欢鍚嶏紙涓嶅缓璁坊鍔犱腑鏂囧唴瀹癸級
        String downloadFileName = String.valueOf(appId);
        // 7. 璋冪敤閫氱敤涓嬭浇鏈嶅姟
        projectDownloadService.downloadProjectAsZip(sourceDirPath, downloadFileName, response);
    }

    /**
     * 鍒涘缓搴旂敤
     *
     * @param appAddRequest 鍒涘缓搴旂敤璇锋眰
     * @param request       璇锋眰
     * @return 搴旂敤 id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 鑾峰彇褰撳墠鐧诲綍鐢ㄦ埛
        User loginUser = userService.getLoginUser(request);
        Long appId = appService.createApp(appAddRequest, loginUser);
        return ResultUtils.success(appId);
    }

    /**
     * 鏇存柊搴旂敤锛堢敤鎴峰彧鑳芥洿鏂拌嚜宸辩殑搴旂敤鍚嶇О锛?
     *
     * @param appUpdateRequest 鏇存柊璇锋眰
     * @param request          璇锋眰
     * @return 鏇存柊缁撴灉
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        if (appUpdateRequest == null || appUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = appUpdateRequest.getId();
        // 鍒ゆ柇鏄惁瀛樺湪
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 浠呮湰浜哄彲鏇存柊
        if (!oldApp.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        App app = new App();
        app.setId(id);
        app.setAppName(appUpdateRequest.getAppName());
        // 璁剧疆缂栬緫鏃堕棿
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 鍒犻櫎搴旂敤锛堢敤鎴峰彧鑳藉垹闄よ嚜宸辩殑搴旂敤锛?
     *
     * @param deleteRequest 鍒犻櫎璇锋眰
     * @param request       璇锋眰
     * @return 鍒犻櫎缁撴灉
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 鍒ゆ柇鏄惁瀛樺湪
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 浠呮湰浜烘垨绠＄悊鍛樺彲鍒犻櫎
        if (!oldApp.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 鏍规嵁 id 鑾峰彇搴旂敤璇︽儏
     *
     * @param id 搴旂敤 id
     * @return 搴旂敤璇︽儏
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 鏌ヨ鏁版嵁搴?
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 鑾峰彇灏佽绫伙紙鍖呭惈鐢ㄦ埛淇℃伅锛?
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 鍒嗛〉鑾峰彇褰撳墠鐢ㄦ埛鍒涘缓鐨勫簲鐢ㄥ垪琛?
     *
     * @param appQueryRequest 鏌ヨ璇锋眰
     * @param request         璇锋眰
     * @return 搴旂敤鍒楄〃
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        // 闄愬埗姣忛〉鏈€澶?20 涓?
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "姣忛〉鏈€澶氭煡璇?20 涓簲鐢?");
        long pageNum = appQueryRequest.getPageNum();
        // 鍙煡璇㈠綋鍓嶇敤鎴风殑搴旂敤
        appQueryRequest.setUserId(loginUser.getId());
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 鏁版嵁灏佽
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 鍒嗛〉鑾峰彇绮鹃€夊簲鐢ㄥ垪琛?
     *
     * @param appQueryRequest 鏌ヨ璇锋眰
     * @return 绮鹃€夊簲鐢ㄥ垪琛?
     */
    @PostMapping("/good/list/page/vo")
    @Cacheable(
            value = "good_app_page",
            key = "T(com.yupi.babystepaicodemother.utils.CacheKeyUtils).generateKey(#appQueryRequest)",
            condition = "#appQueryRequest.pageNum <= 10"
    )
    public BaseResponse<Page<AppVO>> listGoodAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 闄愬埗姣忛〉鏈€澶?20 涓?
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "姣忛〉鏈€澶氭煡璇?20 涓簲鐢?");
        long pageNum = appQueryRequest.getPageNum();
        // 鍙煡璇㈢簿閫夌殑搴旂敤
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        // 鍒嗛〉鏌ヨ
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 鏁版嵁灏佽
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 绠＄悊鍛樺垹闄ゅ簲鐢?
     *
     * @param deleteRequest 鍒犻櫎璇锋眰
     * @return 鍒犻櫎缁撴灉
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 鍒ゆ柇鏄惁瀛樺湪
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 绠＄悊鍛樻洿鏂板簲鐢?
     *
     * @param appAdminUpdateRequest 鏇存柊璇锋眰
     * @return 鏇存柊缁撴灉
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest) {
        if (appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = appAdminUpdateRequest.getId();
        // 鍒ゆ柇鏄惁瀛樺湪
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        App app = new App();
        BeanUtil.copyProperties(appAdminUpdateRequest, app);
        // 璁剧疆缂栬緫鏃堕棿
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 绠＄悊鍛樺垎椤佃幏鍙栧簲鐢ㄥ垪琛?
     *
     * @param appQueryRequest 鏌ヨ璇锋眰
     * @return 搴旂敤鍒楄〃
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 鏁版嵁灏佽
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 绠＄悊鍛樻牴鎹?id 鑾峰彇搴旂敤璇︽儏
     *
     * @param id 搴旂敤 id
     * @return 搴旂敤璇︽儏
     */
    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVOByIdByAdmin(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 鏌ヨ鏁版嵁搴?
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 鑾峰彇灏佽绫?
        return ResultUtils.success(appService.getAppVO(app));
    }
}

