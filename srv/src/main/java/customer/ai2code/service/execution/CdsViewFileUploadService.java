package customer.ai2code.service.execution;

import customer.ai2code.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * CDS视图文件上传服务接口
 * 基于CDS的CQN服务操作数据
 */
public interface CdsViewFileUploadService {
    
    /**
     * 上传Excel文件并导入CDS Views
     * @param file Excel文件
     * @return 导入成功的记录数
     * @throws BusinessException 业务异常
     * @throws IOException 文件操作异常
     */
    int uploadExcelForCdsViews(MultipartFile file) throws BusinessException, IOException;
    
    /**
     * 上传TXT文件并导入CDS View Fields
     * @param file TXT文件
     * @param viewName 所属视图名称
     * @param locale 语言区域
     * @return 导入成功的记录数
     * @throws BusinessException 业务异常
     * @throws IOException 文件操作异常
     */
    int uploadTxtForCdsViewFields(MultipartFile file, String viewName, String locale) 
            throws BusinessException, IOException;
}