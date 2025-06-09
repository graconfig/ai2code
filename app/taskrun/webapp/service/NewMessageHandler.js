sap.ui.define([
    "sap/ui/base/Object",
    "./ChatService"
], function (Object, ChatService) {
    "use strict";

    // 定义INewMessageHandlerSettings函数，模拟接口（JavaScript中无严格接口概念，用对象结构约定参数形式）
    // function INewMessageHandlerSettings(chat, message, binding, sender, streamingCallback) {
    //     this.report = chat;
    //     this.message = message;
    //     this.binding = binding;
    //     this.sender = sender;
    //     this.streamingCallback = streamingCallback;
    // }
    return Object.extend("ai.orchestration.taskrun.service.NewMessageHandler", {
        constructor: function (settings) {
            this.botInstance = settings.botInstance;
            this.message = settings.message;
            this.binding = settings.binding;
            this.sender = settings.sender;
            this.bindingmodel  = settings.bindingmodel;
            this.servicemodel = settings.servicemodel;
            this.onCreatedEmptyAssistantMessage = settings.onCreatedEmptyAssistantMessage;
            this.streamingCallback = settings.streamingCallback;
            this.onComplete = settings.onComplete;

        },
        // 创建消息及完成回复的异步方法
        createMessageAndCompletion: function (streaming, streamingUrl) {
            var chatService = ChatService.getInstance();
            let tempUserContext = null;
            // create a temporary chat record context for user
            /**
             * For Temporary display of the message in the chat list, we are creating a temporary chat record context for the user.
             * This context will be deleted once the action newRecord is posted successfully.
             */
            chatService.createEntity({
                binding: this.binding,
                entity: {
                    role:this.sender ,
                    message: this.message.trim(),
                    ragData: this.message.trim()
                    //botInstance_ID: this.bindingmodel.getObject().ID
                },
                atEnd: true,
                submitBatch: false
            }).then((createdUserContext) => {
                tempUserContext = createdUserContext;
                if (streaming) {
                    return this.handleStreamingCompletion(createdUserContext, streamingUrl);
                } else {
                    return this.handleCompletion(createdUserContext);
                }
            }).then((result) => {
                console.log("Message posted successfully");
                if (streaming) {
                    return chatService.submitChanges("changes");
                } else {
                    // delete the temporary chat record context for user
                    return result.tempUserContext.delete();
                }

            }).then((a) => {
                /** 
                 * 非流式调用执行的是后端newRecord的操作，不会自动刷新
                 * */
                if (!streaming) {
                    this.botInstance.refresh();
                }
            }).catch(function (error) {
                console.error("Error posting message:", error);
                // if (streaming) {
                // delete the temporary chat record context for user
                tempUserContext.delete();
                // }
            });
        },
        // 处理非流式完成回复的异步方法
        handleCompletion: function (createdUserContext) {
            var chatService = ChatService.getInstance();
            return chatService.getCompletion({
                botInstance: this.botInstance,
                message: this.message.trim(),
                binding: this.binding,
                servicemodel: this.servicemodel,
                bindingmodel: this.bindingmodel,
                tempUserContext: createdUserContext
            });
        },
        // 处理流式完成回复的异步方法
        handleStreamingCompletion: function (createdUserContext, streamingUrl) {
            var chatService = ChatService.getInstance();
            //执行一些初始化操作，比如创建一个空的助手消息上下文
            return chatService.createEntity({
                binding: this.binding,
                entity: {
                    content: "",
                    createdBy: "AI",
                    role: "assistant"
                },
                atEnd: true,
                submitBatch: false
            }).then((createdAIContext) => {

                // 流式开始的回调(对创建的空的助手消息上下文做一些操作)
                this.onCreatedEmptyAssistantMessage?.(createdAIContext);

                return chatService.getCompletionAsStream({
                    botInstance: this.botInstance,
                    message: this.message.trim(),
                    tempUserContext: createdUserContext,
                    streamingUrl: streamingUrl
                },
                    (chunk) => {
                        if (chunk) {
                            //流式消息回调
                            this.streamingCallback?.(chunk, createdAIContext);
                        }
                    }
                ).finally(() => {
                    // 完成流式后的回调
                    this.onComplete?.();
                });
            });

        }



    });
});