package customer.ai2code.service.rag;

import java.io.InputStream;

public interface CdsViewFileUploadService {

    /**
     * 上传Excel文件导入CDS视图
     * @param excel Excel文件输入流
     * @return 操作结果描述
     */
    String uploadCDSViews(InputStream excel, String filename);
    
    /**
     * 上传TXT文件导入CDS视图字段
     * @param txt TXT文件输入流
     * @param langu 语言代码
     * @return 操作结果描述
     */
    String uploadCDSViewFields(InputStream txt, String langu, String filename);

}
