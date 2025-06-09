import { ModelConfigs } from "#cds-models/ConfigService";
import { AIServiceConfig } from "srv/config/AIServiceConfig";

export interface AIModel {
    getModelName(): string;
    parseModelConfigs(): AIServiceConfig;
    getModelConfigs(): ModelConfigs;
}

