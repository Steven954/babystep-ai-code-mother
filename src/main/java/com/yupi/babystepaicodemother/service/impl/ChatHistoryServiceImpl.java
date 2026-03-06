package com.yupi.babystepaicodemother.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yupi.babystepaicodemother.constant.UserConstant;
import com.yupi.babystepaicodemother.exception.ErrorCode;
import com.yupi.babystepaicodemother.exception.ThrowUtils;
import com.yupi.babystepaicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.yupi.babystepaicodemother.model.entity.App;
import com.yupi.babystepaicodemother.model.entity.ChatHistory;
import com.yupi.babystepaicodemother.mapper.ChatHistoryMapper;
import com.yupi.babystepaicodemother.model.entity.User;
import com.yupi.babystepaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.yupi.babystepaicodemother.service.AppService;
import com.yupi.babystepaicodemother.service.ChatHistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 瀵硅瘽鍘嗗彶 鏈嶅姟灞傚疄鐜般€?
 *
 * @author <a href="https://github.com/liyupi">绋嬪簭鍛橀奔鐨?/a>
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    @Lazy
    private AppService appService;

    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        // 鍩虹鏍￠獙
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "搴旂敤ID涓嶈兘涓虹┖");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "娑堟伅鍐呭涓嶈兘涓虹┖");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "娑堟伅绫诲瀷涓嶈兘涓虹┖");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "鐢ㄦ埛ID涓嶈兘涓虹┖");
        // 楠岃瘉娑堟伅绫诲瀷鏄惁鏈夋晥
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "涓嶆敮鎸佺殑娑堟伅绫诲瀷");
        // 鎻掑叆鏁版嵁搴?
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();
        return this.save(chatHistory);
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "搴旂敤ID涓嶈兘涓虹┖");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        return this.remove(queryWrapper);
    }

    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "搴旂敤ID涓嶈兘涓虹┖");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "椤甸潰澶у皬蹇呴』鍦?-50涔嬮棿");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 楠岃瘉鏉冮檺锛氬彧鏈夊簲鐢ㄥ垱寤鸿€呭拰绠＄悊鍛樺彲浠ユ煡鐪?
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "搴旂敤涓嶅瓨鍦?");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "鏃犳潈鏌ョ湅璇ュ簲鐢ㄧ殑瀵硅瘽鍘嗗彶");
        // 鏋勫缓鏌ヨ鏉′欢
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 鏌ヨ鏁版嵁
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }
            // 鍙嶈浆鍒楄〃锛岀‘淇濇寜鐓ф椂闂存搴忥紙鑰佺殑鍦ㄥ墠锛屾柊鐨勫湪鍚庯級
            historyList = historyList.reversed();
            // 鎸夌収鏃堕棿椤哄簭灏嗘秷鎭坊鍔犲埌璁板繂涓?
            int loadedCount = 0;
            // 鍏堟竻鐞嗗巻鍙茬紦瀛橈紝闃叉閲嶅鍔犺浇
            chatMemory.clear();
            for (ChatHistory history : historyList) {
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    chatMemory.add(AiMessage.from(history.getMessage()));
                }
                loadedCount++;
            }
            log.info("鎴愬姛涓?appId: {} 鍔犺浇 {} 鏉″巻鍙叉秷鎭?", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("鍔犺浇鍘嗗彶瀵硅瘽澶辫触锛宎ppId: {}, error: {}", appId, e.getMessage(), e);
            // 鍔犺浇澶辫触涓嶅奖鍝嶇郴缁熻繍琛岋紝鍙槸娌℃湁鍘嗗彶涓婁笅鏂?
            return 0;
        }
    }

    /**
     * 鑾峰彇鏌ヨ鍖呰绫?
     *
     * @param chatHistoryQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        // 鎷兼帴鏌ヨ鏉′欢
        queryWrapper.eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId);
        // 娓告爣鏌ヨ閫昏緫 - 鍙娇鐢?createTime 浣滀负娓告爣
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        // 鎺掑簭
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 榛樿鎸夊垱寤烘椂闂撮檷搴忔帓鍒?
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }
}

