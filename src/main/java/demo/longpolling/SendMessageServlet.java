package demo.longpolling;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;

@SuppressWarnings("serial")
public class SendMessageServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		String fromUserId = httpRequest.getParameter("from_user_id");
		if (fromUserId == null || fromUserId.trim().isEmpty()) {
			httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		fromUserId = fromUserId.trim();
		
		String toUserId = httpRequest.getParameter("to_user_id");
		if (toUserId == null || toUserId.trim().isEmpty()) {
			httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		toUserId = toUserId.trim();
		
		String content = httpRequest.getParameter("content");
		if (content == null) {
			httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		int seq = UserMessageHolder.getUserMessageHolder(toUserId).addMessage(fromUserId, content);
		
		JsonObject obj = new JsonObject();
		obj.addProperty("result", "SUCC");
		obj.addProperty("seq", seq);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		GsonUtil.gson().toJson(obj, httpResponse.getWriter());
	}

}
