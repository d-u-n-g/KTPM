package com.computerstore.controllers;

import java.io.IOException;

import com.computerstore.models.User;
import com.computerstore.services.UserService;
import com.computerstore.utils.ValidationUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = { "/login", "/register", "/logout", "/forgot-password" })
public class AuthServlet extends HttpServlet {
	private UserService userService = new UserService();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = req.getServletPath();
		if ("/logout".equals(path)) {
			HttpSession session = req.getSession(false);
			if (session != null) {
				session.invalidate();
			}
			resp.sendRedirect(req.getContextPath() + "/");
			return;
		}

		if ("/login".equals(path)) {
			req.getRequestDispatcher("/jsp/login.jsp").forward(req, resp);
		} else if ("/register".equals(path)) {
			req.getRequestDispatcher("/jsp/register.jsp").forward(req, resp);
		} else if ("/forgot-password".equals(path)) {
			req.getRequestDispatcher("/jsp/forgot-password.jsp").forward(req, resp);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = req.getServletPath();

		if ("/login".equals(path)) {
			String u = req.getParameter("username");
			String p = req.getParameter("password");
			User user = userService.authenticate(u, p);

			if (user != null) {
				req.getSession().setAttribute("user", user);
				if ("QUAN_TRI_VIEN".equals(user.getVaiTro())) {
					resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
				} else {
					resp.sendRedirect(req.getContextPath() + "/");
				}
			} else {
				req.setAttribute("error", "Sai tên đăng nhập hoặc mật khẩu!");
				req.getRequestDispatcher("/jsp/login.jsp").forward(req, resp);
			}
		} else if ("/register".equals(path)) {
			String uname = req.getParameter("username");
			String pass = req.getParameter("password");
			String name = req.getParameter("fullname");
			String email = req.getParameter("email");
			String phone = req.getParameter("phone");

			// Validate inputs
			String validationError = null;
			if (uname == null || uname.trim().isEmpty()) {
				validationError = "Vui lòng nhập tên đăng nhập";
			} else if (!ValidationUtil.isValidUsername(uname)) {
				validationError = "Tên đăng nhập phải từ 3-20 ký tự (chữ, số, dấu gạch dưới)";
			} else if (pass == null || pass.isEmpty()) {
				validationError = "Vui lòng nhập mật khẩu";
			} else if (!ValidationUtil.isValidPassword(pass)) {
				validationError = "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường và số";
			} else if (email == null || email.trim().isEmpty()) {
				validationError = "Vui lòng nhập email";
			} else if (!ValidationUtil.isValidEmail(email)) {
				validationError = "Email không đúng định dạng";
			} else if (phone != null && !phone.trim().isEmpty() && !ValidationUtil.isValidPhone(phone)) {
				validationError = "Số điện thoại không đúng định dạng (VD: 0912345678 hoặc +84912345678)";
			}

			if (validationError != null) {
				req.setAttribute("error", validationError);
				req.setAttribute("username", uname);
				req.setAttribute("fullname", name);
				req.setAttribute("email", email);
				req.setAttribute("phone", phone);
				req.getRequestDispatcher("/jsp/register.jsp").forward(req, resp);
				return;
			}

			if (userService.registerCustomer(uname, pass, name, email, phone, "")) {
				resp.sendRedirect(req.getContextPath() + "/login?registered=true");
			} else {
				req.setAttribute("error", "Tên đăng nhập đã tồn tại!");
				req.getRequestDispatcher("/jsp/register.jsp").forward(req, resp);
			}
		} else if ("/forgot-password".equals(path)) {
			String email = req.getParameter("email");
			if (email == null || email.isBlank()) {
				req.setAttribute("error", "Vui lòng nhập email để tiếp tục.");
			} else {
				var user = userService.getByEmail(email);
				if (user != null) {
					req.setAttribute("success",
							"Nếu email tồn tại trong hệ thống, chúng tôi đã gửi hướng dẫn đặt lại mật khẩu.");
				} else {
					req.setAttribute("error", "Email không tồn tại trong hệ thống.");
				}
			}
			req.setAttribute("email", req.getParameter("email"));
			req.getRequestDispatcher("/jsp/forgot-password.jsp").forward(req, resp);
		}
	}
}
