package customer.ai2code.service.constant;

public final class AIConstants {
    private AIConstants() {
    }

    public static final class Messages {
        public static final String RECORD_NOT_FOUND = "Record_Not_Found";
        public static final String REPORT_NOT_FOUND = "Report_Not_Found";
        public static final String THREAD_INTERRUPTED = "Thread_Interrupted_While_Waiting";
        public static final String UNEXPECTED_ROLE = "Unexpected_Role";
    }

    public static final class Roles {
        public static final String SYSTEM = "system";
        public static final String USER = "user";
        public static final String ASSISTANT = "assistant";
    }

    public static final long CHAT_DELAY_MS = 1000;

    public enum AIServiceType {
        SAPOPENAI,
        SAPCLAUDE,
        OPENAI,
        DEEPSEEK,
        SAPGEMINI
    }
}