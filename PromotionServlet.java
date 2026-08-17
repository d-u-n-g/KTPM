package com.computerstore.controllers;

import java.io.IOException;

import com.computerstore.services.PromotionService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/promotions")
public class PromotionServlet extends HttpServlet {
	private PromotionService promotionService = new PromotionService();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setAttribute("promotions", promotionService.getActivePromotions());
		req.getRequestDispatcher("/jsp/promotions.jsp").forward(req, resp);
	}
}
