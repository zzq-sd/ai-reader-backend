package com.aireader.backend.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ChatModel提供者服务
 * <p>
 * 负责管理系统中所有可用的ChatModel实例，并根据名称提供对应的实例。
 * 这是一个关键的适配器，用于支持动态模型切换。
 */
@Service
@Slf4j
public class ChatModelProvider {

    private final Map<String, ChatModel> chatModels;

    /**
     * 构造函数，自动注入所有ChatModel类型的Bean。
     * Spring会将所有ChatModel实例收集到一个List中。
     *
     * @param chatModelList 系统中所有ChatModel Bean的列表
     */
    public ChatModelProvider(List<ChatModel> chatModelList) {
        // 将ChatModel列表转换为一个Map，便于按名称查找。
        // key是ChatModel的类名的小写形式（例如 "zhipuaichatmodel"）
        this.chatModels = chatModelList.stream()
                .collect(Collectors.toMap(
                        model -> model.getClass().getSimpleName().toLowerCase().replace("chatmodel", ""),
                        Function.identity(),
                        (existing, replacement) -> existing // 如果有重名，保留第一个
                ));

        log.info("✅ ChatModelProvider 初始化完成. 加载了以下模型: {}", this.chatModels.keySet());
    }

    /**
     * 根据模型名称获取ChatModel实例。
     *
     * @param modelName 模型名称 (例如 "zhipuai" 或 "deepseek")
     * @return 对应ChatModel的Optional封装
     */
    public Optional<ChatModel> getChatModel(String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) {
            return Optional.empty();
        }
        ChatModel model = chatModels.get(modelName.toLowerCase());
        if (model == null) {
            log.warn("未找到名为 '{}' 的ChatModel. 可用模型: {}", modelName, chatModels.keySet());
        }
        return Optional.ofNullable(model);
    }
} 