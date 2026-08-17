package com.computerstore.controllers;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/contact")
public class ContactServlet extends HttpServlet {
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.getRequestDispatcher("/jsp/contact.jsp").forward(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String name = req.getParameter("name");
		String email = req.getParameter("email");
		String subject = req.getParameter("subject");
		String message = req.getParameter("message");

		if (name == null || name.isBlank() || email == null || email.isBlank() || message == null
				|| message.isBlank()) {
			req.setAttribute("error", "Vui lòng điền đầy đủ tên, email và nội dung liên hệ.");
		} else {
			req.setAttribute("success", "Yêu cầu của bạn đã được gửi. Chúng tôi sẽ liên hệ lại qua email sớm nhất.");
		}

		req.setAttribute("name", name);
		req.setAttribute("email", email);
		req.setAttribute("subject", subject);
		req.setAttribute("message", message);
		req.getRequestDispatcher("/jsp/contact.jsp").forward(req, resp);
	}
}
