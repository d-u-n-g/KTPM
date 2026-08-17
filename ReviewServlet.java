package com.computerstore.controllers;

import java.io.IOException;

import com.computerstore.models.User;
import com.computerstore.services.ReviewService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/review")
public class ReviewServlet extends HttpServlet {
	private ReviewService reviewService = new ReviewService();

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		User u = (User) req.getSession().getAttribute("user");
		int maSP = Integer.parseInt(req.getParameter("maSP"));
		int rating = Integer.parseInt(req.getParameter("rating"));
		String comment = req.getParameter("comment");

		try {
			reviewService.submitReview(u.getMaKH(), maSP, rating, comment);
			req.getSession().setAttribute("reviewSuccess", "Đánh giá của bạn đã được gửi và đang chờ duyệt.");
		} catch (Exception e) {
			req.getSession().setAttribute("reviewError", e.getMessage());
		}

		resp.sendRedirect(req.getContextPath() + "/product?id=" + maSP);
	}
}
