package demo.longpolling;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;

public class UserMessageHolder {

	private int currentSeq = 1; // 当前用户推送消息序列号
	private final Queue<Message> messageQueue = new LinkedList<>(); // 消息暂存队列，最多缓存100条
	private final List<WaitContext> waitContextList = new LinkedList<>(); // 等待推送消息的异步servlet上下文
	
	private UserMessageHolder() {
	}
	
	// 添加新的推送消息，成功后返回序列号
	public synchronized int addMessage(String fromUserId, String content) {
		// add to message queue
		final int seq = this.currentSeq++;
		this.messageQueue.offer(new Message(seq, (int) (System.currentTimeMillis() / 1000L), fromUserId, content));
		while (this.messageQueue.size() > 100) {
			this.messageQueue.poll();
		}
		// dispatch asyncContext and remove 
		Iterator<WaitContext> it = this.waitContextList.iterator();
		while (it.hasNext()) {
			WaitContext ctx = it.next();
			if (seq > ctx.lastSeq) {
				it.remove();
				ctx.asyncContext.dispatch();
			}
		}
		return seq;
	}
	
	// 获取lastSeq之后的size条消息
	public synchronized List<Message> getMessage(int lastSeq, int size) {
		List<Message> list = new ArrayList<>();
		for (Message msg : this.messageQueue) {
			if (msg.getSeq() > lastSeq) {
				list.add(msg);
				if (list.size() >= size) {
					return list;
				}
			}
		}
		return list;
	}
	
	// 获取lastSeq之后的size条消息,如果消息为空，则加入到等待列表中
	public synchronized List<Message> getMessageOrWait(int lastSeq, int size, HttpServletRequest httpRequest) {
		List<Message> list = new ArrayList<>();
		for (Message msg : this.messageQueue) {
			if (msg.getSeq() > lastSeq) {
				list.add(msg);
				if (list.size() >= size) {
					return list;
				}
			}
		}
		if (list.isEmpty()) {
			// 如果消息为空，则加入到等待列表中
			AsyncContext asyncContext = httpRequest.startAsync();
			asyncContext.setTimeout(60000L); // 最多等待60s
			this.waitContextList.add(new WaitContext(lastSeq, asyncContext, System.currentTimeMillis()));
		}
		return list;
	}
	
	// 检查等待的异步servlet上下文是否有超时。超时时间30s
	private synchronized void checkWaitContextExpire(long now) {
		Iterator<WaitContext> it = this.waitContextList.iterator();
		while (it.hasNext()) {
			WaitContext ctx = it.next();
			if (now - ctx.createTime > 30000) {
				it.remove();
				ctx.asyncContext.dispatch();
			}
		}
	}
	
	private static class WaitContext {
		private final int lastSeq;
		private final AsyncContext asyncContext;
		private final long createTime;
		public WaitContext(int lastSeq, AsyncContext asyncContext, long createTime) {
			this.lastSeq = lastSeq;
			this.asyncContext = asyncContext;
			this.createTime = createTime;
		}
	}
	
	private static final ConcurrentMap<String, UserMessageHolder> USER_MESSAGE_HOLDER_MAP = new ConcurrentHashMap<String, UserMessageHolder>();
	
	public static UserMessageHolder getUserMessageHolder(String userId) {
		UserMessageHolder holder = USER_MESSAGE_HOLDER_MAP.get(userId);
		if (holder == null) {
			holder = new UserMessageHolder();
			UserMessageHolder tmp = USER_MESSAGE_HOLDER_MAP.putIfAbsent(userId, holder);
			if (tmp != null) {
				holder = tmp;
			}
		}
		return holder;
	}
	
	static {
		// 后台定时检查超时线程。必须设为daemon
		Executors.newScheduledThreadPool(1, (r) -> {
			Thread thread = new Thread(r);
			thread.setName("CheckWaitContextExpire");
			thread.setDaemon(true);
			return thread;
		}).scheduleAtFixedRate(() -> {
			final long now = System.currentTimeMillis();
			for (UserMessageHolder holder : USER_MESSAGE_HOLDER_MAP.values()) {
				holder.checkWaitContextExpire(now);
			}
		}, 3, 3, TimeUnit.SECONDS); // 每3秒检查一次
	}
	
}
