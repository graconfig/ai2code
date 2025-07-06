package customer.ai2code.model.s4;

import com.fasterxml.jackson.annotation.JsonProperty;
import customer.ai2code.model.s4.namespaces.s4createcdsmetadata.ZSGEN_DDLS_SOURCE_LIST;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

/**
 * Request model for autoActiveCDS action parameters
 * 对应 SAP S4/HANA 系统中 autoActiveCDS 操作的请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoActiveCDSRequest {

    /**
     * 项目名称
     * Constraints: Not nullable, Maximum length: 30
     */
    @JsonProperty("PROJNAME")
    private String projectName;

    /**
     * 项目描述
     * Constraints: Not nullable, Maximum length: 40
     */
    @JsonProperty("PROJDESC")
    private String projectDescription;

    /**
     * 是否使用草稿模式
     * Constraints: Not nullable
     */
    @JsonProperty("WITHDRAFT")
    private Boolean withDraft;

    /**
     * 是否包含额外保存功能
     * Constraints: Not nullable
     */
    @JsonProperty("WITHADDITIONALSAVE")
    private Boolean withAdditionalSave;

    /**
     * 开发包名称
     * Constraints: Not nullable, Maximum length: 30
     */
    @JsonProperty("DEVCLASS")
    private String developmentClass;

    /**
     * 传输请求号
     * Constraints: Not nullable, Maximum length: 20
     */
    @JsonProperty("TRKORR")
    private String transportRequest;

    /**
     * 源代码列表
     * Constraints: Not nullable
     */
    @JsonProperty("_SOURCE")
    private Collection<ZSGEN_DDLS_SOURCE_LIST> sourceList;

    /**
     * 创建一个带有默认值的请求对象
     * @param projectName 项目名称
     * @param projectDescription 项目描述
     * @return 带有默认值的请求对象
     */
    public static AutoActiveCDSRequest createDefault(String projectName, String projectDescription) {
        return AutoActiveCDSRequest.builder()
                .projectName(projectName)
                .projectDescription(projectDescription)
                .withDraft(false)
                .withAdditionalSave(false)
                .developmentClass("$TMP")
                .transportRequest("")
                .sourceList(new ArrayList<>())
                .build();
    }

    /**
     * 添加源代码信息
     * @param viewName 视图名称
     * @param viewDescription 视图描述
     * @param reference 引用对象
     * @param sourceCode 源代码内容
     */
    public void addSourceInfo(String viewName, String viewDescription, String reference, String sourceCode) {
        ZSGEN_DDLS_SOURCE_LIST sourceInfo = ZSGEN_DDLS_SOURCE_LIST.builder()
                .vIEWNAME(viewName)
                .vIEWDESC(viewDescription)
                .rEFERENCE(reference)
                .sOURCECODE(sourceCode)
                .build();
        
        if (this.sourceList instanceof List) {
            ((List<ZSGEN_DDLS_SOURCE_LIST>) this.sourceList).add(sourceInfo);
        }
    }

    /**
     * 添加源代码信息
     * @param sourceInfo ZSGEN_DDLS_SOURCE_LIST 对象
     */
    public void addSourceInfo(ZSGEN_DDLS_SOURCE_LIST sourceInfo) {
        if (this.sourceList instanceof List) {
            ((List<ZSGEN_DDLS_SOURCE_LIST>) this.sourceList).add(sourceInfo);
        }
    }

    /**
     * 验证请求参数是否有效
     * @return 验证是否通过
     */
    public boolean isValid() {
        return projectName != null && !projectName.trim().isEmpty() &&
               projectDescription != null && !projectDescription.trim().isEmpty() &&
               withDraft != null &&
               withAdditionalSave != null &&
               developmentClass != null && !developmentClass.trim().isEmpty() &&
               transportRequest != null &&
               sourceList != null;
    }
}
