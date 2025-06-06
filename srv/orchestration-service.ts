// import { Record, Report } from "#cds-models/ChatService";
// import cds from "@sap/cds";
// import { readFile } from "fs";
// import { addRecordHandler } from "./actions/addRecord";
// import { adoptHandler } from "./actions/adopt";
// import { fixCDSHandler } from "./actions/fixCDS";
// import { generateCDSHandler } from "./actions/generateCDS";
// import { generatePCLHandler } from "./actions/generatePCL";
// import { generateProgramHandler } from "./actions/generateProgram";
// import { syncToChatHandler } from "./actions/syncToChat";
// import { verifyHandler } from "./actions/verify";

// export default class MainService extends cds.ApplicationService {
//   async init() {
//     const { Records, Reports } = this.entities;

//     let gpt: any, claude5: any, claude7: any;

//     if (process.env.CDS_ENV == "hybrid") {
//       // cds.env.requires["gen-ai-hub"]['gpt-4o'];
//       readFile("default-env.json", "utf8", (err: any, vcapservicejson: any) => {
//         if (err) {
//           console.error("Error reading the file:", err);
//         }
//         process.env.VCAP_SERVICES = vcapservicejson;
//       });

//       gpt = "d254738a6298582a";
//       claude5 = "d42cd80d244e37fb";
//       claude7 = "	d4e4e1602382dd4f";
//     } else {
//       const aiAPI = await import("@sap-ai-sdk/ai-api");

//       // 使用custom destination 查询所有 deployment
//       const { resources } = await aiAPI.DeploymentApi.deploymentQuery(
//         {
//           status: "RUNNING",
//           executableIds: ["azure-openai", "aws-bedrock"],
//           scenarioId: "foundation-models",
//         },
//         { "AI-Resource-Group": "default" }
//       ).execute({ destinationName: "AICore" });

//       // 通过model 获得deployment
//       gpt = resources.filter(
//         (deployment) =>
//           deployment.details?.resources?.backendDetails?.model?.name ===
//           "gpt-4o"
//       )[0].id;

//       claude5 = resources.filter(
//         (deployment) =>
//           deployment.details?.resources?.backendDetails?.model?.name ===
//           "anthropic--claude-3.5-sonnet"
//       )[0].id;

//       claude7 = resources.filter(
//         (deployment) =>
//           deployment.details?.resources?.backendDetails?.model?.name ===
//           "anthropic--claude-3.7-sonnet"
//       )[0].id;
//     }

//     this.on("UPDATE", Report.drafts, async (req, next) => {
//       await next();
//       if (req.data.ProjectId) {
//         await cds.run(
//           UPDATE(req.subject).with({
//             ProjectId: req.data.ProjectId.toUpperCase(),
//             CDS1Name: "ZI_" + req.data.ProjectId.toUpperCase() + "_001",
//             CDS2Name: "ZI_" + req.data.ProjectId.toUpperCase() + "_002",
//             CDS3Name: "ZC_" + req.data.ProjectId.toUpperCase() + "_001",
//             CDS4Name: "ZC_" + req.data.ProjectId.toUpperCase() + "_002",
//           })
//         );
//       }
//     });

//     this.after("DELETE", Report, async (results, req) => {
//       await DELETE.from(Records).where({ report_ID: req.data.ID });
//     });

//     this.after("DELETE", Report.drafts, async (results, req) => {
//       if (!(await SELECT.from(Reports, req.data.ID))) {
//         await DELETE.from(Records).where({ report_ID: req.data.ID });
//       }
//     });

//     const {
//       addRecord,
//       SyncTochat,
//       verify,
//       generateProgram,
//       generatePCL,
//       generateCDS,
//       regenerateCDS,
//     } = Report.actions;
//     const { Newadopt } = Record.actions;

//     this.on(addRecord, (req) => addRecordHandler.call(this, req, claude7));

//     this.on(Newadopt, (req) => adoptHandler.call(this, req));

//     this.on(verify, (req) => verifyHandler.call(this, req));

//     this.on(SyncTochat, (req) => syncToChatHandler.call(this, req));

//     this.on(generateCDS, (req) =>
//       generateCDSHandler.call(this, req, claude5, claude7)
//     );

//     this.on(regenerateCDS, (req) => fixCDSHandler.call(this, req, claude5, claude7));

//     this.on(generateProgram, (req) => generateProgramHandler.call(this, req));

//     this.on(generatePCL, (req) => generatePCLHandler.call(this, req, gpt));

//     return super.init();
//   }
// }
