package customer.ai2code.handlers;

import cds.gen.mainservice.MainService_;
import cds.gen.mainservice.UploadCDSViewsContext;
import cds.gen.mainservice.UploadCDSViewFieldsContext;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.service.execution.CdsViewFileUploadService;

import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Component
@ServiceName(MainService_.CDS_NAME)
public class MainServiceFileUploadHandler implements EventHandler {
    
    private final CdsViewFileUploadService fileUploadService;

    public MainServiceFileUploadHandler(CdsViewFileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    /**
     * 处理CDS视图上传（无界action，使用EventContext）
     */
    @On(event = UploadCDSViewsContext.CDS_NAME)
    public void handleUploadCDSViews(UploadCDSViewsContext context) {
        try (InputStream excel = new ByteArrayInputStream(context.getExcel())) {
            String result = fileUploadService.uploadCDSViews(excel);
            context.setResult(result);
        } catch (Exception e) {
            throw new BusinessException("CDS视图上传失败", e);
        }
    }

    /**
     * 处理CDS视图字段上传（无界action，使用EventContext）
     */
    @On(event = UploadCDSViewFieldsContext.CDS_NAME)
    public void handleUploadCDSViewFields(UploadCDSViewFieldsContext context) {
        try (InputStream txt = new ByteArrayInputStream(context.getTxt())) {
            String result = fileUploadService.uploadCDSViewFields(txt, context.getLangu());
            context.setResult(result);
        } catch (Exception e) {
            throw new BusinessException("视图字段上传失败", e);
        }
    }
}