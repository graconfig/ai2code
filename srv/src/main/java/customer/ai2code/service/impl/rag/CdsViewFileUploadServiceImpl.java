package customer.ai2code.service.impl.rag;

import cds.gen.mainservice.CDSViews;  // 导入mainservice包下的实体
import cds.gen.mainservice.Viewfields;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.service.execution.CdsViewFileUploadService;
import customer.ai2code.service.impl.GenericCqnService;
import customer.ai2code.util.FileParser;

import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
public class CdsViewFileUploadServiceImpl implements CdsViewFileUploadService {
    private final GenericCqnService cqnService;
    private final FileParser parser;

    public CdsViewFileUploadServiceImpl(GenericCqnService cqnService, FileParser parser) {
        this.cqnService = cqnService;
        this.parser = parser;
    }

    @Override
    public String uploadCDSViews(InputStream excel) {
        try {
            List<CDSViews> views = parser.parseCDSViews(excel);
            for (CDSViews view : views) {
                view.setIsActive(true);
                cqnService.insertCDSViews(view);
            }
            return "成功导入 " + views.size() + " 条CDS视图";
        } catch (Exception e) {
            throw new BusinessException("CDS视图导入失败", e);
        }
    }

    @Override
    public String uploadCDSViewFields(InputStream txt, String langu) {
        try {
            List<Viewfields> fields = parser.parseViewFields(txt, langu);
            for (Viewfields field : fields) {
                field.setIsGeneratedEmbedding(false);
                cqnService.insertViewfields(field);  // 传递mainservice包的实体
            }
            return "成功导入 " + fields.size() + " 条视图字段";
        } catch (Exception e) {
            throw new BusinessException("未知上传错误", e);
        }
    }
}
