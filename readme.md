# AI2Code 


## 本地运行
### 使用VSCode 自带的Debug工具运行
1. 登录PWC BTP Cloud Foundry环境
    - 账号密码和BTP环境信息在飞书文档"Resources"中。
    - 运行命令
    ```
    cf login -a https://api.cf.jp10.hana.ondemand.com --sso-passcode XXXX
    ```
    - 选择Org。
    - 选择Space。
2. 使用VSCode自带的java/spring boot debug工具运行项目。


### 使用maven命令行工具运行
使用这个工具运行需要执行前置命令：
1. 登录PWC BTP Cloud Foundry环境
2. 运行命令
```
cds bind --exec '--' node ./writecfenv.js
```
3. 运行命令
```
mvn spring-boot:run
```