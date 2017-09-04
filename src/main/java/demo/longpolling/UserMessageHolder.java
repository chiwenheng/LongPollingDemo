package demo.longpolling;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class UserMessageHolder {

    private int currentSeq = 1;
    private final Queue<Message> messageQueue = new LinkedList<>(); // 消息暂存队列，最多缓存100条
    private final List<WaitContext> waitContextList = new LinkedList<>(); // 等待推送消息的异步servlet上下文
    
    private UserMessageHolder() {
    }
    
    // 添加新的推送消息，成功后返回序列号
    public synchronized Message addMessage(String fromUserId, String content) {
        Message msg = new Message(this.currentSeq++, System.currentTimeMillis(), fromUserId, content);

        // add to message queue
        this.messageQueue.offer(msg);
        while (this.messageQueue.size() > 100) {
            this.messageQueue.poll();
        }
        // dispatch asyncContext and remove 
        Iterator<WaitContext> it = this.waitContextList.iterator();
        while (it.hasNext()) {
            WaitContext ctx = it.next();
            if (msg.getCreateTime() > ctx.lastMsgCreateTime || msg.getSeq() > ctx.lastMsgSeq) {
                it.remove();
                ctx.asyncContext.dispatch();
            }
        }
        return msg;
    }
    
    // 获取lastCreateTime或lastSeq之后或者createTime之后创建的size条消息
    public synchronized List<Message> getMessage(int lastMsgSeq, long lastMsgCreateTime, int size) {
        List<Message> list = new ArrayList<>();
        for (Message msg : this.messageQueue) {
            if (msg.getCreateTime() > lastMsgCreateTime || msg.getSeq() > lastMsgSeq) {
                list.add(msg);
                if (list.size() >= size) {
                    return list;
                }
            }
        }
        return list;
    }
    
    // 获取lastSeq之后的size条消息,如果消息为空，则加入到等待列表中
    public synchronized List<Message> getMessageOrWait(int lastMsgSeq, long lastMsgCreateTime, int size, HttpServletRequest httpRequest) {
        List<Message> list = new ArrayList<>();
        for (Message msg : this.messageQueue) {
            if (msg.getCreateTime() > lastMsgCreateTime || msg.getSeq() > lastMsgSeq) {
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
            this.waitContextList.add(new WaitContext(lastMsgSeq, lastMsgCreateTime, asyncContext, System.currentTimeMillis()));
        }
        return list;
    }

    private synchronized void checkExpire(long now) {
        // 检查等待的异步servlet上下文是否有超时。超时时间30s
        Iterator<WaitContext> itCtx = this.waitContextList.iterator();
        while (itCtx.hasNext()) {
            WaitContext ctx = itCtx.next();
            if (now - ctx.createTime > 30000) {
                itCtx.remove();
                ctx.asyncContext.dispatch();
            }
        }
        // 检查等待的消息队列是否有超时。超时时间50s
        Iterator<Message> itMsg = this.messageQueue.iterator();
        while (itMsg.hasNext()) {
            Message msg = itMsg.next();
            if (now - msg.getCreateTime() > 50000) {
                itMsg.remove();
            }
        }
    }
    
    private static class WaitContext {
        private final int lastMsgSeq;
        private final long lastMsgCreateTime;
        private final AsyncContext asyncContext;
        private final long createTime;
        public WaitContext(int lastMsgSeq, long lastMsgCreateTime, AsyncContext asyncContext, long createTime) {
            this.lastMsgSeq = lastMsgSeq;
            this.lastMsgCreateTime = lastMsgCreateTime;
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
            thread.setName("CheckExpire");
            thread.setDaemon(true);
            return thread;
        }).scheduleAtFixedRate(() -> {
            final long now = System.currentTimeMillis();
            for (UserMessageHolder holder : USER_MESSAGE_HOLDER_MAP.values()) {
                holder.checkExpire(now);
            }
        }, 3, 3, TimeUnit.SECONDS); // 每3秒检查一次
    }
    
}
