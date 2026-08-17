package com.computerstore.controllers;

import java.io.IOException;

import org.mindrot.jbcrypt.BCrypt;

import com.computerstore.models.User;
import com.computerstore.services.UserService;
import com.computerstore.utils.ValidationUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {
	private UserService userService = new UserService();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (req.getSession().getAttribute("user") == null) {
			resp.sendRedirect(req.getContextPath() + "/login");
			return;
		}
		req.getRequestDispatcher("/jsp/profile.jsp").forward(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		User user = (User) req.getSession().getAttribute("user");
		if (user == null) {
			resp.sendRedirect(req.getContextPath() + "/login");
			return;
		}

		String action = req.getParameter("action");

		if ("updateInfo".equals(action)) {
			handleUpdateInfo(req, resp, user);
		} else if ("changePassword".equals(action)) {
			handleChangePassword(req, resp, user);
		} else {
			resp.sendRedirect(req.getContextPath() + "/profile");
		}
	}

	private void handleUpdateInfo(HttpServletRequest req, HttpServletResponse resp, User user)
			throws ServletException, IOException {
		String hoTen = ValidationUtil.sanitize(req.getParameter("hoTen"));
		String email = ValidationUtil.sanitize(req.getParameter("email"));
		String soDienThoai = ValidationUtil.sanitize(req.getParameter("soDienThoai"));
		String diaChi = ValidationUtil.sanitize(req.getParameter("diaChi"));

		// Validate hoTen
		if (hoTen == null || hoTen.isBlank()) {
			req.setAttribute("infoError", "Họ tên không được để trống.");
			req.getRequestDispatcher("/jsp/profile.jsp").forward(req, resp);
			return;
		}

		// Validate email
		if (email == null || !ValidationUtil.isValidEmail(email)) {
			req.setAttribute("infoError", "Email không hợp lệ.");
			req.getRequestDispatcher("/jsp/profile.jsp").forward(req, resp);
			return;
		}

		// Check if email is already used by another account
		if (!email.equals(user.getEmail())) {
			User existing = userService.getByEmail(email);
			if (existing != null && existing.getMaTK() != user.getMaTK()) {
				req.setAttribute("infoError", "Email này đã được sử dụng bởi tài khoản khác.");
				req.getRequestDispatcher("/jsp/profile.jsp").forward(req, resp);
				return;
			}
		}

		// Validate phone (optional field but validate if provided)
		if (soDienThoai != null && !soDienThoai.isBlank() && !ValidationUtil.isValidPhone(soDienThoai)) {
			req.setAttribute("infoError", "Số điện thoại không hợp lệ (VD: 0912345678 hoặc +84912345678).");
			req.getRequestDispatcher("/jsp/profile.jsp").forward(req, resp);
			return;
		}

		user.setHoTen(hoTen);
		user.setEmail(email);
		user.setSoDienThoai(soDienThoai);
		user.setDiaChi(diaChi);

		if (userService.updateCustomerProfile(user)) {
			req.getSession().setAttribute("user", user);
			req.setAttribute("infoSuccess", "Cập nhật thông tin thành công.");
		} else {
			req.setAttribute("infoError", "Cập nhật thất bại. Vui lòng thử lại.");
		}
		req.getRequestDispatcher("/jsp/profile.jsp").forward(req, resp);
	}

	private void handleChangePassword(HttpServletRequest req, HttpServletResponse resp, User user)
			throws ServletException, IOException {
		String currentPw = req.getParameter("currentPassword");
		String newPw = req.getParameter("newPassword");
		String confirmPw = req.getParameter("confirmPassword");

		// Validate current password
		if (!BCrypt.checkpw(currentPw, user.getMatKhauHash())) {
			req.setAttribute("pwError", "Mật khẩu hiện tại không đúng.");
			req.getRequestDispatcher("/jsp/profile.jsp").forward(req, resp);
			return;
		}

		// Validate new password strength
		if (!ValidationUtil.isValidPassword(newPw)) {
			req.setAttribute("pwError",
					"Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và chữ số.");
			req.getRequestDispatcher("/jsp/profile.jsp").forward(req, resp);
			return;
		}

		// Check new password != current password
		if (BCrypt.checkpw(newPw, user.getMatKhauHash())) {
			req.setAttribute("pwError", "Mật khẩu mới không được giống mật khẩu hiện tại.");
			req.getRequestDispatcher("/jsp/profile.jsp").forward(req, resp);
			return;
		}

		// Check confirm password
		if (!newPw.equals(confirmPw)) {
			req.setAttribute("pwError", "Mật khẩu xác nhận không khớp.");
			req.getRequestDispatcher("/jsp/profile.jsp").forward(req, resp);
			return;
		}

		String hashed = BCrypt.hashpw(newPw, BCrypt.gensalt());
		if (userService.updatePassword(user.getMaTK(), hashed)) {
			user.setMatKhauHash(hashed);
			req.getSession().setAttribute("user", user);
			req.setAttribute("pwSuccess", "Đổi mật khẩu thành công.");
		} else {
			req.setAttribute("pwError", "Đổi mật khẩu thất bại. Vui lòng thử lại.");
		}
		req.getRequestDispatcher("/jsp/profile.jsp").forward(req, resp);
	}
}
