package com.computerstore.controllers;

import java.io.IOException;
import java.util.List;

import com.computerstore.dao.CategoryDAO;
import com.computerstore.dao.ProductDAO;
import com.computerstore.models.Category;
import com.computerstore.models.Product;
import com.computerstore.services.ProductService;
import com.computerstore.utils.AppConstants;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { "/home" })
public class HomeServlet extends HttpServlet {
	private ProductService productService = new ProductService();
	private CategoryDAO categoryDAO = new CategoryDAO();
	private ProductDAO productDAO = new ProductDAO();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// Featured products - chỉ lấy 1 lần, tránh ghi đè
		List<Product> featuredProducts = productDAO.getFeaturedProducts(AppConstants.FEATURED_PRODUCTS_LIMIT);
		req.setAttribute("featuredProducts", featuredProducts);

		// Danh mục
		List<Category> categories = categoryDAO.getAll();
		req.setAttribute("categories", categories);

		// Sản phẩm bán chạy theo danh mục
		req.setAttribute("bestSellingCPUs",
				productDAO.getBestSellingByCategory("CPU", AppConstants.BEST_SELLING_LIMIT));
		req.setAttribute("bestSellingGPUs",
				productDAO.getBestSellingByCategory("VGA", AppConstants.BEST_SELLING_LIMIT));
		req.setAttribute("bestSellingRAMs",
				productDAO.getBestSellingByCategory("RAM", AppConstants.BEST_SELLING_LIMIT));
		req.setAttribute("bestSellingCases",
				productDAO.getBestSellingByCategory("Case", AppConstants.BEST_SELLING_LIMIT));
		req.setAttribute("bestSellingPSUs",
				productDAO.getBestSellingByCategory("Nguồn", AppConstants.BEST_SELLING_LIMIT));
		req.setAttribute("bestSellingMainboards",
				productDAO.getBestSellingByCategory("Mainboard", AppConstants.BEST_SELLING_LIMIT));

		req.setAttribute("currentPage", "home");
		req.getRequestDispatcher("/jsp/index.jsp").forward(req, resp);
	}
}
