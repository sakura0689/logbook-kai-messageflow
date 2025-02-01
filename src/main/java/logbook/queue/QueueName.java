package logbook.queue;

public enum QueueName {
    API("APIQueue"),
    IMAGE("ImageQueue"),
    IMAGEJSON("ImageJsonQueue");
    
    String queueName;
    
    private QueueName(String queueName) {
        this.queueName = queueName;
    }
    
    public String getQueueName() {
        return this.queueName;
    }
}
