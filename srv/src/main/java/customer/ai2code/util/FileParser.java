package customer.ai2code.util;

import cds.gen.mainservice.CDSViews;
import cds.gen.mainservice.Viewfields;
import customer.ai2code.exception.BusinessException;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;

/**
 * 统一文件解析器（Excel+TXT），基于viewName为主键
 */
@Component // 添加Spring组件注解
public class FileParser {
    
    // 解析CDSViews（Excel）
    public List<CDSViews> parseCDSViews(InputStream excelStream) {
        List<CDSViews> views = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(excelStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 1) return views;
            
            Row headerRow = sheet.getRow(0);
            int nameCol = getColumnIndex(headerRow, "viewName");
            int categoryCol = getColumnIndex(headerRow, "viewCategory");
            int descCol = getColumnIndex(headerRow, "viewDesc");
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                CDSViews view = CDSViews.create();
                view.setViewName(getCellValue(row.getCell(nameCol)));
                view.setViewCategory(getCellValue(row.getCell(categoryCol)));
                view.setViewDesc(getCellValue(row.getCell(descCol)));
                view.setIsActive(true); // 默认激活
                
                // 校验viewName非空
                if (view.getViewName() != null && !view.getViewName().isEmpty()) {
                    views.add(view);
                }
            }
        } catch (Exception e) {
            throw new BusinessException("Excel解析失败");
        }
        return views;
    }
    
    // 解析Viewfields（TXT）
    public List<Viewfields> parseViewFields(InputStream txtStream, String defaultLang) {
        List<Viewfields> fields = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(txtStream))) {
            String[] headers = reader.readLine().split("\t");
            int fileIdCol = indexOf(headers, "file_ID");
            int categoryCol = indexOf(headers, "category");
            int contentCol = indexOf(headers, "content");
            int langCol = indexOf(headers, "langu");
            
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split("\t");
                if (data.length < headers.length) continue;
                
                Viewfields field = Viewfields.create();
                field.setFileId(data[fileIdCol]);
                field.setCategory(data[categoryCol]);
                field.setContent(data[contentCol]);
                field.setLangu(langCol >= 0 ? data[langCol] : defaultLang);
                // field.setCreatedAt(Instant.now());
                // field.setModifiedAt(Instant.now());
                
                fields.add(field);
            }
        } catch (Exception e) {
            throw new BusinessException("TXT解析失败");
        }
        return fields;
    }
    
    // 工具方法
    private int getColumnIndex(Row headerRow, String header) {
        for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
            if (header.equals(headerRow.getCell(i).getStringCellValue().trim())) {
                return i;
            }
        }
        throw new IllegalArgumentException("缺少表头：" + header);
    }
    
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            default -> "";
        };
    }
    
    private int indexOf(String[] array, String target) {
        for (int i = 0; i < array.length; i++) {
            if (target.equals(array[i].trim())) return i;
        }
        return -1;
    }
}