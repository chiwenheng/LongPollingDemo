package demo.longpolling;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@SuppressWarnings("serial")
public class ReceiveMessageServlet extends HttpServlet {

	private static final String REQUEST_ATTR_PARAM = "demo.longpolling.ReceiveMessageServlet.REQUEST_ATTR_PARAM";
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		Param param = (Param) httpRequest.getAttribute(REQUEST_ATTR_PARAM);
		if (param == null) {
			String userId = httpRequest.getParameter("user_id");
			if (userId == null || userId.trim().isEmpty()) {
				httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			userId = userId.trim();
			
			int lastMsgSeq;
			String lastMsgSeqStr = httpRequest.getParameter("last_msg_seq");
			if (lastMsgSeqStr == null || lastMsgSeqStr.trim().isEmpty()) {
				lastMsgSeq = 0;
			} else {
				try {
					lastMsgSeq = Integer.parseInt(lastMsgSeqStr);
				} catch (NumberFormatException e) {
					httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
					return;
				}
			}

			long lastMsgCreateTime;
			String lastMsgCreateTimeStr = httpRequest.getParameter("last_msg_create_time");
			if (lastMsgCreateTimeStr == null || lastMsgCreateTimeStr.trim().isEmpty()) {
				lastMsgCreateTime = 0;
			} else {
				try {
					lastMsgCreateTime = Long.parseLong(lastMsgCreateTimeStr);
				} catch (NumberFormatException e) {
					httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
					return;
				}
			}
			
			int size;
			String sizeStr = httpRequest.getParameter("size");
			if (sizeStr == null || sizeStr.trim().isEmpty()) {
				size = 1;
			} else {
				try {
					size = Integer.parseInt(sizeStr);
				} catch (NumberFormatException e) {
					httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
					return;
				}
				if (size < 1) {
					httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
					return;
				}
			}
			
			httpRequest.setAttribute(REQUEST_ATTR_PARAM, new Param(userId, lastMsgSeq, lastMsgCreateTime, size));
			
			List<Message> messageList = UserMessageHolder.getUserMessageHolder(userId).getMessageOrWait(lastMsgSeq, lastMsgCreateTime, size, httpRequest);
			if (!messageList.isEmpty()) {
				httpResponse.setContentType("application/json;charset=UTF-8");
				GsonUtil.gson().toJson(messageList, httpResponse.getWriter());
			}
		} else {
			List<Message> messageList = UserMessageHolder.getUserMessageHolder(param.userId).getMessage(param.lastMsgSeq, param.lastMsgCreateTime, param.size);
			httpResponse.setContentType("application/json;charset=UTF-8");
			GsonUtil.gson().toJson(messageList, httpResponse.getWriter());
		}
	}
	
	private static class Param {
		final String userId;
		final int lastMsgSeq;
		final long lastMsgCreateTime;
		final int size;
		
		public Param(String userId, int lastMsgSeq, long lastMsgCreateTime, int size) {
			this.userId = userId;
			this.lastMsgSeq = lastMsgSeq;
			this.lastMsgCreateTime = lastMsgCreateTime;
			this.size = size;
		}
	}

}
