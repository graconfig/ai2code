package customer.ai2code.service.execution.functioncall;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * 完整的 Function Call 测试套件
 * 专注于测试 Function Calling 的核心逻辑，避免 SAP SDK 兼容性问题
 */
@Suite
@SelectClasses({
    FunctionCallProcessorTest.class,
    OpenAIParametersFormatTest.class,
    customer.ai2code.service.execution.functioncall.adapter.OpenAIFunctionCallAdapterTest.class,
    customer.ai2code.service.execution.functioncall.integration.OpenAIFunctionCallIntegrationTest.class,
    FunctionCallExecutionTest.class
})
public class FunctionCallTestSuite {
    // 测试套件运行器 - 专注于核心功能测试
}