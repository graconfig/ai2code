      
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

@Component
public class FileParser {
    
    // 解析CDSViews（Excel） - 保持不变
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
                view.setIsActive(true);
                
                if (view.getViewName() != null && !view.getViewName().isEmpty()) {
                    views.add(view);
                }
            }
        } catch (Exception e) {
            throw new BusinessException("Excel解析失败: " + e.getMessage(), e);
        }
        return views;
    }
    
    // 解析Viewfields（TXT） - 关键修改
    public List<Viewfields> parseViewFields(InputStream txtStream, String defaultLang) {
        List<Viewfields> fields = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(txtStream))) {
            // 读取表头
            String headerLine = reader.readLine();
            if (headerLine == null) return fields;
            
            String[] headers = headerLine.split(",");
            
            // 定义必需的列索引
            int categoryCol = indexOf(headers, "category");
            int contentCol = indexOf(headers, "content");
            int tableNameCol = indexOf(headers, "tableName");
            int tableDescCol = indexOf(headers, "tableDesc");
            int langCol = indexOf(headers, "langu"); // 可选列
            
            // 校验必需字段
            if (categoryCol == -1 || contentCol == -1 || 
                tableNameCol == -1 || tableDescCol == -1) {
                throw new BusinessException("TXT文件缺少必需表头字段");
            }
            
            // 读取数据行
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split("\t");
                if (data.length < headers.length) continue;
                
                Viewfields field = Viewfields.create();
                field.setCategory(data[categoryCol]);
                field.setContent(data[contentCol]);
                field.setTableName(data[tableNameCol]);
                field.setTableDesc(data[tableDescCol]);
                
                // 处理语言字段
                if (langCol >= 0 && langCol < data.length) {
                    field.setLangu(data[langCol]);
                } else {
                    field.setLangu(defaultLang);
                }
                
                fields.add(field);
            }
        } catch (BusinessException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            throw new BusinessException("TXT解析失败: " + e.getMessage(), e);
        }
        return fields;
    }
    
    // 工具方法 - 保持不变
    private int getColumnIndex(Row headerRow, String header) {
        for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null && header.equals(cell.getStringCellValue().trim())) {
                return i;
            }
        }
        throw new IllegalArgumentException("缺少表头：" + header);
    }
    
    // 增强单元格处理
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    // 增强表头查找
    private int indexOf(String[] array, String target) {
        if (array == null || target == null) return -1;
        
        for (int i = 0; i < array.length; i++) {
            if (target.equalsIgnoreCase(array[i].trim())) {
                return i;
            }
        }
        return -1; // 未找到返回-1
    }
}

    