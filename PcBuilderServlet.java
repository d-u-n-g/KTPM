package com.computerstore.controllers;

import java.io.IOException;
import java.util.List;

import com.computerstore.dao.ProductDAO;
import com.computerstore.models.Product;
import com.computerstore.models.User;
import com.computerstore.services.CartService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/builder")
public class PcBuilderServlet extends HttpServlet {
	private CartService cartService = new CartService();
	private ProductDAO productDAO = new ProductDAO();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// Load products by category for builder
		req.setAttribute("cpus", productDAO.getBestSellingByCategory("CPU", 10));
		req.setAttribute("mainboards", productDAO.getBestSellingByCategory("Mainboard", 10));
		req.setAttribute("rams", productDAO.getBestSellingByCategory("RAM", 10));
		req.setAttribute("vgas", productDAO.getBestSellingByCategory("VGA", 10));
		req.setAttribute("storages", productDAO.getBestSellingByCategory("Ổ cứng", 10));
		req.setAttribute("cases", productDAO.getBestSellingByCategory("Case", 10));
		req.setAttribute("psus", productDAO.getBestSellingByCategory("Nguồn", 10));
		req.setAttribute("coolers", productDAO.getBestSellingByCategory("Tản nhiệt", 10));

		req.getRequestDispatcher("/jsp/builder.jsp").forward(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		HttpSession session = req.getSession();
		User user = (User) session.getAttribute("user");
		if (user == null || !"KHACH_HANG".equals(user.getVaiTro())) {
			session.setAttribute("error", "Vui lòng đăng nhập với tư cách khách hàng để mua hàng.");
			resp.sendRedirect(req.getContextPath() + "/login");
			return;
		}

		// Collect all selected component IDs
		String[] paramNames = { "maSP_CPU", "maSP_Mainboard", "maSP_RAM", "maSP_VGA",
				"maSP_Storage", "maSP_Case", "maSP_PSU", "maSP_Cooler" };
		boolean hasItems = false;

		for (String paramName : paramNames) {
			String maSPStr = req.getParameter(paramName);
			if (maSPStr != null && !maSPStr.isEmpty()) {
				try {
					int maSP = Integer.parseInt(maSPStr);
					cartService.addToCart(user.getMaKH(), maSP, 1);
					hasItems = true;
				} catch (NumberFormatException e) {
					// Ignore invalid maSP
				} catch (Exception e) {
					System.err.println("Lỗi khi thêm sản phẩm " + maSPStr + " vào giỏ hàng: " + e.getMessage());
				}
			}
		}

		if (hasItems) {
			session.setAttribute("successMessage", "Đã thêm toàn bộ cấu hình vào giỏ hàng.");
		} else {
			session.setAttribute("error", "Vui lòng chọn ít nhất một linh kiện.");
		}
		resp.sendRedirect(req.getContextPath() + "/cart");
	}
}
