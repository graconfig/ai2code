sap.ui.define([
    "sap/ui/base/Object",
    "sap/m/BusyDialog",
    "sap/m/MessageBox",
    "sap/ui/model/odata/v4/Context",
    "sap/ui/model/odata/v4/ODataContextBinding",
    "sap/ui/model/odata/v4/ODataListBinding",
    "sap/ui/model/odata/v4/ODataModel"
], function (Object, BusyDialog, MessageBox, Context, ODataContextBinding, ODataListBinding, ODataModel) {
    "use strict";
    var ChatService = Object.extend("chatbotui.service.ChatStreamingService", {
        constructor: function () {

        },
        // 设置模型的方法
        setModel: function (model) {
            // 打印检查模型设置
            console.log("Model settings:", {
                updateGroupId: model.getUpdateGroupId(),
                groupId: model.getGroupId(),
                bindingMode: model.getDefaultBindingMode()
            });
            this.model = model;
        },
        // 提交更改的异步方法
        submitChanges: function (sUpdateGroupId) {
            return this.model.submitBatch(sUpdateGroupId || this.model.getUpdateGroupId());
        },
        createEntity: function (params) {
            const { entity, binding, skipRefresh = false, atEnd = true, submitBatch = true } = params;
            return new Promise((resolve, reject) => {
                const context = binding.create(entity, skipRefresh, atEnd);
                if (submitBatch) {
                    context.created().then(() => {
                        resolve(context);
                    });
                    this.model.submitBatch(this.model.getUpdateGroupId());
                } else {
                    resolve(context);
                }
            });
        },
        createEntityInJsonModel: function (params) {
            const { entity, binding, skipRefresh = false, atEnd = true } = params;
            return new Promise((resolve) => {
                // Get the model from binding
                const model = binding.getModel();
                
                // Get current data array
                const path = binding.getPath();
                let data = model.getProperty(path) || [];
                
                // Create new entity with generated UUID
                const newEntity = {
                    ID: crypto.randomUUID(),
                    ...entity
                };

                // Add new entity to data array
                if (atEnd) {
                    data.push(newEntity);
                } else {
                    data.unshift(newEntity);
                }
                
                // Update model
                model.setProperty(path, data);
                
                // Force binding update to create new contexts
                binding.update();
                
                // Get all contexts after update
                const contexts = binding.getContexts();
                // Find the context for our new entity by ID
                const context = contexts.find(ctx => ctx.getProperty("ID") === newEntity.ID);
                
                if (!skipRefresh) {
                    model.refresh(true);
                }
                
                resolve(context);
            });
        },


        // 删除实体的异步方法
        deleteEntity: function (context) {
            return new Promise((resolve, reject) => {
                context.delete().then(resolve, reject);
                this.model.submitBatch(this.model.getUpdateGroupId());
            });
        },
        // 获取完成内容的异步方法
        getCompletion: function (params) {
            return new Promise((resolve, reject) => {
                const binding = this.model.bindContext("AIService.chatCompletion(...)", params.chat);
                binding.setParameter("content", params.message)
                const dialog = new BusyDialog({ text: "Thinking..." });
                dialog.open();
                binding.invoke().then(() => {
                    dialog.close();
                    resolve(
                        {
                            actionContext: binding.getBoundContext()
                            // tempUserContext: params.tempUserContext
                        }
                    );
                }).catch((error) => {
                    dialog.close();
                    MessageBox.alert(error.message, {
                        title: "Error"
                    });
                    reject(error);
                });
            });
        },
        // 以流的方式获取完成内容的异步方法
        getCompletionAsStream: function (params, callback) {
            return new Promise(async (resolve, reject) => {
                // 准备请求参数
                const requestData = {
                    id: params.botInstance.getProperty("ID"),
                    // isActiveEntity: params.report.getProperty("IsActiveEntity"),
                    content: params.message,
                    // locale: sap.ui.getCore().getConfiguration().getLanguage()
                };
                fetch(params.streamingUrl, {
                    // fetch("/api/chat/stream", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        "Accept": "text/event-stream"
                    },
                    body: JSON.stringify(requestData)
                }).then(response => {
                    /** 新修改 */
                    // const reader = response.body.getReader();
                    // 使用 TextDecoderStream 将字节流转换为文本流
                    const textStream = response.body.pipeThrough(new TextDecoderStream());

                    // 使用 TransformStream 按行拆分消息（SSE 规范每条事件以换行结尾）
                    /**
                     * this.buffer 用于累积未完成的文本数据片段。下面解释其作用和不使用它可能出现的问题：

                        作用：
                        当数据流没有以完整的换行符（或完整的事件分隔符）结束时，当前读取的数据可能只包含消息的一部分。这时，使用 buffer 将这部分未完成的数据保存起来，等待后续数据到达后再合并成完整的消息进行处理。

                        不使用 buffer 可能的问题：

                        未完整的消息片段会被丢弃，导致数据缺失。
                        消息分隔可能不准确，造成解析错误或数据断裂。
                        因为 SSE 消息可能跨多个数据块传输，不累积剩余数据会导致部分消息永远无法正确解析。
                        总结：this.buffer 是确保消息完整性的关键变量，用来处理跨块边界的部分数据。
                     * 
                     * 
                     */
                    const lineStream = textStream.pipeThrough(new TransformStream({
                        start(controller) {
                            this.buffer = "";
                        },
                        transform(chunk, controller) {
                            this.buffer += chunk;
                            const lines = this.buffer.split(/\n\n/);
                            this.buffer = lines.pop(); // 最后一行可能不完整
                            for (const line of lines) {
                                controller.enqueue(line);
                            }
                        },
                        flush(controller) {
                            if (this.buffer) {
                                controller.enqueue(this.buffer);
                            }
                        }
                    }));


                    // 读取拆分好的行，并处理每条 SSE 消息
                    const reader = lineStream.getReader();

                    let aiResponse = "";

                    return new Promise((streamResolve, streamReject) => {


                        function processStream() {
                            return reader.read().then(({ done, value }) => {

                                if (done) {
                                    return streamResolve(aiResponse);
                                }



                                // const chunk = new TextDecoder().decode(value);
                                // buffer = ""; // 重置buffer

                                // 使用正则表达式匹配 "data:" 后面的内容
                                const dataRegex = /data:([^]*?)(?=data:|-----Total Usage-----|$)/g;

                                // 修改正则表达式以保留前导空格
                                // const dataRegex = /data:([\s\S]*?)(?=(?:\r?\n)?data:|-----Total Usage-----|$)/g;

                                // 使用更精确的正则表达式匹配数据
                                // const dataRegex = /data:\s*([^]*?)(?=(?:\r?\n)?data:|-----Total Usage-----|$)/g;
                                // 匹配 data: 开头的消息，但保留原始格式
                                // const dataRegex = /data:\s*([^\n]*(?:\n(?!data:)[^\n]*)*)/g;
                                // let lastIndex = 0;
                                let matches;

                                // if (chunk) {
                                //     const objects = chunk.match(regex);
                                //     objects?.forEach(content => {
                                //         callback?.(content);
                                //         aiResponse += content;
                                //     });
                                // }

                                while ((matches = dataRegex.exec(value)) !== null) {
                                    const content = matches[1];
                                    if (content) {
                                        //判断content内容不应该包含 {"role":"user"} {"role":"assistant"}
                                        //这两个不要，然后最后的{"role":"assistant"}要获取相关信息进行返回的
                                        var user_start = "{\"role\":\"user\"";
                                        var assistant_start = "{\"role\":\"assistant\"";  
                                        
                                        //判断content内容：
                                        if (content.substring(0, 14) === user_start ) {
                                            console.log("user_start:" +  user_start);
                                        }
                                        else if (content.substring(0, 19) === assistant_start) { 
                                            console.log("assistant_start:" +  assistant_start);
                                        }
                                        else{
                                            aiResponse += content;
                                            callback?.(aiResponse);
                                        }
                                    }
                                    // lastIndex = matches.index + matches[0].length;
                                }

                                // // 保存未完成的消息段到buffer
                                // if (lastIndex < chunk.length) {
                                //     buffer = chunk.slice(lastIndex);
                                // }

                                // Call callback with current chunk if provided
                                // callback?.(chunk);

                                return processStream();
                            }).catch(streamReject);
                        }

                        return processStream();
                    });
                })
                    .then(finalResponse => {
                        // resolve(finalResponse);
                        resolve(
                            {
                                response: finalResponse
                            }
                        );
                    })
                    .catch(error => {
                        console.error('Stream error:', error);
                        resolve(
                            error
                        );

                    });
            });
        }
    });

    return {
        // 获取单例实例的静态方法
        getInstance: function () {
            this.instance ??= new ChatService();
            return this.instance;
        }
    };



});