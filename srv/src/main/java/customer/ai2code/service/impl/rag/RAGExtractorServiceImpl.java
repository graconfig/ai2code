// package customer.ai2code.service.impl.rag;

// import java.util.Locale;

// import org.springframework.stereotype.Service;

// import customer.ai2code.service.execution.RAGExtractor;
// import customer.ai2code.service.impl.GenericCqnService;

// @Service
// public class RAGExtractorServiceImpl implements RAGExtractor {

//     private final GenericCqnService genericCqnService;

//     public RAGExtractorServiceImpl(GenericCqnService genericCqnService) {
//         this.genericCqnService = genericCqnService;
//     }

//     @Override
//     public String extract(String ragSource, int ragTopK, String query, Locale language, int threshold) {
//         // 示例逻辑：根据ragSource选择不同的数据表或方法
//         try {
//             switch (ragSource) {
//                 case "BusinessScenarios":
//                     return genericCqnService.searchBusinessScenario(query, ragTopK, threshold, language);
//                 case "CDSViews":
//                     return genericCqnService.searchCDSViews(query, ragTopK, threshold, language);
//                 case "Viewfields":
//                     return genericCqnService.searchViewFields(query, ragTopK, threshold, language);
//                 default:
//                     throw new IllegalArgumentException("Unsupported RAG source: " + ragSource);
//             }
//         } catch (Exception e) {
//             System.err.println("RAG extraction failed: " + e.getMessage());
//             return "";
//         }
//     }
// }


