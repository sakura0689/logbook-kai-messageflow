package logbook.queue;

public enum QueueName {
    API("APIQueue"),
    IMAGE("ImageQueue"),
    IMAGEJSON("ImageJsonQueue"),
    NONE("none");
    
    String queueName;
    
    private QueueName(String queueName) {
        this.queueName = queueName;
    }
    
    public String getQueueName() {
        return this.queueName;
    }
    
    public static QueueName getQueue(String queueName) {
        for (QueueName qn : QueueName.values()) {
            if (qn.queueName.equals(queueName)) {
                return qn;
            }
        }
        return NONE;
    }
}
