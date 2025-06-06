package customer.ai2code.model;

import cds.gen.mainservice.Tasks;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenericTask implements Task {

    // @Getter
    private Tasks task;

    // @Override
    // public Tasks getTaskInstance() {
    //     // TODO Auto-generated method stub
    //     // throw new UnsupportedOperationException("Unimplemented method 'getTaskInstance'");

    // }

    // public GenericTask(String id, String name, String description, String taskTypeId, String botTypeId) {
    // }
    
}
