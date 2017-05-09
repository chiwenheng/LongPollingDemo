package demo.longpolling;

public class Message {

	private final int seq;
	private final int createTime;
	private final String fromUserId;
	private final String content;
	
	public Message(int seq, int createTime, String fromUserId, String content) {
		this.seq = seq;
		this.createTime = createTime;
		this.fromUserId = fromUserId;
		this.content = content;
	}

	public int getSeq() {
		return seq;
	}
	
	public int getCreateTime() {
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
		StringBuilder builder = new StringBuilder();
		builder.append("Message [seq=");
		builder.append(seq);
		builder.append(", createTime=");
		builder.append(createTime);
		builder.append(", ");
		if (fromUserId != null) {
			builder.append("fromUserId=");
			builder.append(fromUserId);
			builder.append(", ");
		}
		if (content != null) {
			builder.append("content=");
			builder.append(content);
		}
		builder.append("]");
		return builder.toString();
	}
	
}
