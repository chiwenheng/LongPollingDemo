package demo.longpolling;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
			
			int lastSeq;
			String lastSeqStr = httpRequest.getParameter("last_seq");
			if (lastSeqStr == null || lastSeqStr.trim().isEmpty()) {
				lastSeq = 0;
			} else {
				try {
					lastSeq = Integer.parseInt(lastSeqStr);
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
			
			httpRequest.setAttribute(REQUEST_ATTR_PARAM, new Param(userId, lastSeq, size));
			
			List<Message> messageList = UserMessageHolder.getUserMessageHolder(userId).getMessageOrWait(lastSeq, size, httpRequest);
			if (!messageList.isEmpty()) {
				httpResponse.setContentType("application/json;charset=UTF-8");
				GsonUtil.gson().toJson(messageList, httpResponse.getWriter());
			}
		} else {
			List<Message> messageList = UserMessageHolder.getUserMessageHolder(param.userId).getMessage(param.lastSeq, param.size);
			httpResponse.setContentType("application/json;charset=UTF-8");
			GsonUtil.gson().toJson(messageList, httpResponse.getWriter());
		}
	}
	
	private static class Param {
		final String userId;
		final int lastSeq;
		final int size;
		
		public Param(String userId, int lastSeq, int size) {
			this.userId = userId;
			this.lastSeq = lastSeq;
			this.size = size;
		}
	}

}
