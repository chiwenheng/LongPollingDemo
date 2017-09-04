package demo.longpolling;

public class Message {

    private final int seq;
    private final long createTime;
    private final String fromUserId;
    private final String content;
    
    public Message(int seq, long createTime, String fromUserId, String content) {
        this.seq = seq;
        this.createTime = createTime;
        this.fromUserId = fromUserId;
        this.content = content;
    }

    public int getSeq() {
        return seq;
    }
    
    public long getCreateTime() {
        return createTime;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "Message{" +
                "seq=" + seq +
                ", createTime=" + createTime +
                ", fromUserId='" + fromUserId + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
