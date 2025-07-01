import { resolve } from "path";
import * as TJS from "typescript-json-schema";
import * as functions from "./Functions/index.js";

export const executeHandler = async function (this: any, req: any) {

  const ClassName = "callOdata"; // Example function name, adjust as needed
  const functionName = ClassName as keyof typeof functions;

  const callOdata = functions[functionName];

  const program = TJS.getProgramFromFiles(
    [resolve("srv/actions/Functions/" + ClassName + ".ts")]
  );

  const schema = TJS.generateSchema(program, "odataRequest");

  console.log("Schema for odataRequest:", JSON.stringify(schema, null, 2));

  const request: any = {
    ProjName: "Project A",
    ProjDesc: "Description of Project A",
  };
  callOdata(request);
};
