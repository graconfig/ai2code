package customer.ai2code.handlers;

import cds.gen.mainservice.Excelupload_;
import cds.gen.mainservice.MainService_;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.service.execution.CdsViewFileUploadService;
import customer.ai2code.service.impl.rag.CdsViewFileUploadServiceImpl;

import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * MainService文件上传处理器，处理CDS视图和字段的上传事件
 * 参考现有处理器类的命名和代码风格实现
 */
@Component
@ServiceName(MainService_.CDS_NAME)
public class MainServiceFileUploadHandler implements EventHandler {
    
    private final CdsViewFileUploadService fileUploadService;

    public MainServiceFileUploadHandler(CdsViewFileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    /**
     * 处理CDS视图上传事件（无异常处理，异常向上抛出）
     */
    @On(event = "uploadCDSViews")
    public String handleUploadCDSViews(Excelupload_ ref, InputStream excel) {
        return fileUploadService.uploadCDSViews(excel);
    }

    /**
     * 处理CDS视图字段上传事件（无异常处理，异常向上抛出）
     */
    @On(event = "uploadCDSViewFields")
    public String handleUploadCDSViewFields(Excelupload_ ref, InputStream txt, String langu) {
        return fileUploadService.uploadCDSViewFields(txt, langu);
    }
}
