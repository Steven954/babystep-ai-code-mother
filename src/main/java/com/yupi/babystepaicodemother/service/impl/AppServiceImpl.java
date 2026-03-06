package com.yupi.babystepaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yupi.babystepaicodemother.ai.AiCodeGenTypeRoutingService;
import com.yupi.babystepaicodemother.ai.AiCodeGenTypeRoutingServiceFactory;
import com.yupi.babystepaicodemother.constant.AppConstant;
import com.yupi.babystepaicodemother.core.AiCodeGeneratorFacade;
import com.yupi.babystepaicodemother.core.builder.VueProjectBuilder;
import com.yupi.babystepaicodemother.core.handler.StreamHandlerExecutor;
import com.yupi.babystepaicodemother.exception.BusinessException;
import com.yupi.babystepaicodemother.exception.ErrorCode;
import com.yupi.babystepaicodemother.exception.ThrowUtils;
import com.yupi.babystepaicodemother.model.dto.app.AppAddRequest;
import com.yupi.babystepaicodemother.model.dto.app.AppQueryRequest;
import com.yupi.babystepaicodemother.model.entity.App;
import com.yupi.babystepaicodemother.mapper.AppMapper;
import com.yupi.babystepaicodemother.model.entity.User;
import com.yupi.babystepaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.yupi.babystepaicodemother.model.enums.CodeGenTypeEnum;
import com.yupi.babystepaicodemother.model.vo.AppVO;
import com.yupi.babystepaicodemother.model.vo.UserVO;
import com.yupi.babystepaicodemother.monitor.MonitorContext;
import com.yupi.babystepaicodemother.monitor.MonitorContextHolder;
import com.yupi.babystepaicodemother.service.AppService;
import com.yupi.babystepaicodemother.service.ChatHistoryService;
import com.yupi.babystepaicodemother.service.ScreenshotService;
import com.yupi.babystepaicodemother.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 搴旂敤 鏈嶅姟灞傚疄鐜般€?
 *
 * @author <a href="https://github.com/liyupi">绋嬪簭鍛橀奔鐨?/a>
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Value("${code.deploy-host:http://localhost}")
    private String deployHost;

    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ScreenshotService screenshotService;

    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 1. 鍙傛暟鏍￠獙
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "搴旂敤 ID 閿欒");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "鎻愮ず璇嶄笉鑳戒负绌?");
        // 2. 鏌ヨ搴旂敤淇℃伅
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "搴旂敤涓嶅瓨鍦?");
        // 3. 鏉冮檺鏍￠獙锛屼粎鏈汉鍙互鍜岃嚜宸辩殑搴旂敤瀵硅瘽
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "鏃犳潈闄愯闂搴旂敤");
        }
        // 4. 鑾峰彇搴旂敤鐨勪唬鐮佺敓鎴愮被鍨?
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搴旂敤浠ｇ爜鐢熸垚绫诲瀷閿欒");
        }
        // 5. 鍦ㄨ皟鐢?AI 鍓嶏紝鍏堜繚瀛樼敤鎴锋秷鎭埌鏁版嵁搴撲腑
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        // 6. 璁剧疆鐩戞帶涓婁笅鏂囷紙鐢ㄦ埛 ID 鍜屽簲鐢?ID锛?
        MonitorContextHolder.setContext(
                MonitorContext.builder()
                        .userId(loginUser.getId().toString())
                        .appId(appId.toString())
                        .build()
        );
        // 7. 璋冪敤 AI 鐢熸垚浠ｇ爜锛堟祦寮忥級
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
        // 8. 鏀堕泦 AI 鍝嶅簲鐨勫唴瀹癸紝骞朵笖鍦ㄥ畬鎴愬悗淇濆瓨璁板綍鍒板璇濆巻鍙?
        return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, appId, loginUser, codeGenTypeEnum)
                .doFinally(signalType -> {
                    // 娴佺粨鏉熸椂娓呯悊锛堟棤璁烘垚鍔?澶辫触/鍙栨秷锛?
                    MonitorContextHolder.clearContext();
                });
    }

    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        // 鍙傛暟鏍￠獙
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "鍒濆鍖?prompt 涓嶈兘涓虹┖");
        // 鏋勯€犲叆搴撳璞?
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        // 搴旂敤鍚嶇О鏆傛椂涓?initPrompt 鍓?12 浣?
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // 浣跨敤 AI 鏅鸿兘閫夋嫨浠ｇ爜鐢熸垚绫诲瀷锛堝渚嬫ā寮忥級
        AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService();
        CodeGenTypeEnum selectedCodeGenType = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(selectedCodeGenType.getValue());
        // 鎻掑叆鏁版嵁搴?
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        log.info("搴旂敤鍒涘缓鎴愬姛锛孖D: {}, 绫诲瀷: {}", app.getId(), selectedCodeGenType.getValue());
        return app.getId();
    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 鍙傛暟鏍￠獙
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "搴旂敤 ID 閿欒");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "鐢ㄦ埛鏈櫥褰?");
        // 2. 鏌ヨ搴旂敤淇℃伅
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "搴旂敤涓嶅瓨鍦?");
        // 3. 鏉冮檺鏍￠獙锛屼粎鏈汉鍙互閮ㄧ讲鑷繁鐨勫簲鐢?
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "鏃犳潈闄愰儴缃茶搴旂敤");
        }
        // 4. 妫€鏌ユ槸鍚﹀凡鏈?deployKey
        String deployKey = app.getDeployKey();
        // 濡傛灉娌℃湁锛屽垯鐢熸垚 6 浣?deployKey锛堝瓧姣?+ 鏁板瓧锛?
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 鑾峰彇浠ｇ爜鐢熸垚绫诲瀷锛岃幏鍙栧師濮嬩唬鐮佺敓鎴愯矾寰勶紙搴旂敤璁块棶鐩綍锛?
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 妫€鏌ヨ矾寰勬槸鍚﹀瓨鍦?
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "搴旂敤浠ｇ爜璺緞涓嶅瓨鍦紝璇峰厛鐢熸垚搴旂敤");
        }
        // 7. Vue 椤圭洰鐗规畩澶勭悊锛氭墽琛屾瀯寤?
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // Vue 椤圭洰闇€瑕佹瀯寤?
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 椤圭洰鏋勫缓澶辫触锛岃閲嶈瘯");
            // 妫€鏌?dist 鐩綍鏄惁瀛樺湪
            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue 椤圭洰鏋勫缓瀹屾垚浣嗘湭鐢熸垚 dist 鐩綍");
            // 鏋勫缓瀹屾垚鍚庯紝闇€瑕佸皢鏋勫缓鍚庣殑鏂囦欢澶嶅埗鍒伴儴缃茬洰褰?
            sourceDir = distDir;
        }
        // 8. 澶嶅埗鏂囦欢鍒伴儴缃茬洰褰?
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "搴旂敤閮ㄧ讲澶辫触锛?" + e.getMessage());
        }
        // 9. 鏇存柊鏁版嵁搴?
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "鏇存柊搴旂敤閮ㄧ讲淇℃伅澶辫触");
        // 10. 鏋勫缓搴旂敤璁块棶 URL
        String appDeployUrl = String.format("%s/%s/", deployHost, deployKey);        // 11. 寮傛鐢熸垚鎴浘骞朵笖鏇存柊搴旂敤灏侀潰
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;
    }

    /**
     * 寮傛鐢熸垚搴旂敤鎴浘骞舵洿鏂板皝闈?
     *
     * @param appId  搴旂敤ID
     * @param appUrl 搴旂敤璁块棶URL
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 浣跨敤铏氭嫙绾跨▼骞舵墽琛?
        Thread.startVirtualThread(() -> {
            // 璋冪敤鎴浘鏈嶅姟鐢熸垚鎴浘骞朵笂浼?
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);
            // 鏇存柊鏁版嵁搴撶殑灏侀潰
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            boolean updated = this.updateById(updateApp);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "鏇存柊搴旂敤灏侀潰瀛楁澶辫触");
        });
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 鍏宠仈鏌ヨ鐢ㄦ埛淇℃伅
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 鎵归噺鑾峰彇鐢ㄦ埛淇℃伅锛岄伩鍏?N+1 鏌ヨ闂
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "璇锋眰鍙傛暟涓虹┖");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 鍒犻櫎搴旂敤鏃讹紝鍏宠仈鍒犻櫎瀵硅瘽鍘嗗彶
     *
     * @param id
     * @return
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        long appId = Long.parseLong(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 鍏堝垹闄ゅ叧鑱旂殑瀵硅瘽鍘嗗彶
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
            log.error("鍒犻櫎搴旂敤鍏宠仈鐨勫璇濆巻鍙插け璐ワ細{}", e.getMessage());
        }
        // 鍒犻櫎搴旂敤
        return super.removeById(id);
    }
}

