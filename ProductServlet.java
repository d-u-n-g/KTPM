package com.computerstore.controllers;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import com.computerstore.dao.CategoryDAO;
import com.computerstore.dao.ComponentDetailDAO;
import com.computerstore.dao.ProductDAO;
import com.computerstore.dao.ReviewDAO;
import com.computerstore.models.Category;
import com.computerstore.models.Product;
import com.computerstore.models.Review;
import com.computerstore.services.ProductService;
import com.computerstore.services.PromotionService;
import com.computerstore.utils.AppConstants;
import com.computerstore.utils.BeanToMapUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { "/products", "/product" })
public class ProductServlet extends HttpServlet {
	private ProductService productService = new ProductService();
	private PromotionService promotionService = new PromotionService();
	private CategoryDAO categoryDAO = new CategoryDAO();
	private ComponentDetailDAO componentDetailDAO = new ComponentDetailDAO();
	private ReviewDAO reviewDAO = new ReviewDAO();
	private ProductDAO productDAO = new ProductDAO();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = req.getServletPath();

		if ("/products".equals(path)) {
			handleProductList(req, resp);
		} else if ("/product".equals(path)) {
			handleProductDetail(req, resp);
		}
	}

	private void handleProductList(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		int page = 1;
		int limit = AppConstants.DEFAULT_PAGE_SIZE;
		String pstr = req.getParameter("page");
		if (pstr != null && !pstr.isEmpty()) {
			try {
				page = Integer.parseInt(pstr);
			} catch (NumberFormatException ignored) {
			}
		}

		String category = req.getParameter("category");
		String keyword = req.getParameter("q");
		String sort = req.getParameter("sort");
		if (sort == null || sort.isBlank())
			sort = "newest";

		List<Product> products;
		int totalProducts;
		int totalPages;

		if (category != null && !category.isBlank()) {
			products = productService.getProductsByCategoryName(category, page, limit, sort);
			totalProducts = productService.getTotalProductsByCategoryName(category);
			totalPages = productService.getTotalPagesByCategory(category, limit);
		} else if (keyword != null && !keyword.isBlank()) {
			products = productService.searchProducts(keyword, page, limit, sort);
			totalProducts = productService.getTotalSearchResults(keyword);
			totalPages = productService.getTotalSearchPages(keyword, limit);
		} else {
			products = productService.getAllProducts(page, limit, sort);
			totalProducts = productService.getTotalProducts();
			totalPages = productService.getTotalPages(limit);
		}

		req.setAttribute("products", products);
		req.setAttribute("currentPage", page);
		req.setAttribute("totalPages", totalPages);
		req.setAttribute("totalProducts", totalProducts);
		req.setAttribute("categories", categoryDAO.getAll());
		req.setAttribute("sort", sort);
		req.getRequestDispatcher("/jsp/products.jsp").forward(req, resp);
	}

	private void handleProductDetail(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String idstr = req.getParameter("id");
		if (idstr == null || idstr.isEmpty()) {
			resp.sendRedirect(req.getContextPath() + "/products");
			return;
		}

		try {
			int productId = Integer.parseInt(idstr);
			Product p = productService.getProductDetail(productId);
			req.setAttribute("product", p);

			if (p != null) {
				// Category & technical detail
				Category cat = categoryDAO.getById(p.getMaLoaiSP());
				if (cat != null) {
					String tenLoai = cat.getTenLoaiSP();
					req.setAttribute("categoryName", tenLoai);

					// Lấy chi tiết kỹ thuật theo loại sản phẩm và convert sang Map để JSP hiển thị
					Object detail = getTechnicalDetail(p.getMaSP(), tenLoai);
					req.setAttribute("techDetail", BeanToMapUtil.toMap(detail));
				}

				// Tính giá khuyến mãi (nếu có)
				BigDecimal discountedPrice = promotionService.getDiscountedPrice(productId);
				req.setAttribute("discountedPrice", discountedPrice);

				// Reviews
				List<Review> reviews = reviewDAO.getByProductId(productId);
				req.setAttribute("reviews", reviews);

				// Tính trung bình sao
				if (reviews != null && !reviews.isEmpty()) {
					double avgRating = reviews.stream()
							.mapToInt(Review::getSoSao)
							.average()
							.orElse(0.0);
					req.setAttribute("avgRating", String.format("%.1f", avgRating));
					req.setAttribute("totalReviews", reviews.size());
				} else {
					req.setAttribute("avgRating", "0.0");
					req.setAttribute("totalReviews", 0);
				}

				// Sản phẩm liên quan (cùng danh mục, không bao gồm sản phẩm hiện tại)
				if (cat != null) {
					List<Product> relatedProducts = productDAO.getByCategoryName(
							cat.getTenLoaiSP(), 0, AppConstants.BEST_SELLING_LIMIT);
					relatedProducts.removeIf(rp -> rp.getMaSP() == productId);
					req.setAttribute("relatedProducts", relatedProducts);
				}
			}

			req.setAttribute("categories", categoryDAO.getAll());
			req.getRequestDispatcher("/jsp/product-detail.jsp").forward(req, resp);

		} catch (NumberFormatException e) {
			resp.sendRedirect(req.getContextPath() + "/products");
		}
	}

	private Object getTechnicalDetail(int maSP, String tenLoai) {
		if ("Mainboard".equalsIgnoreCase(tenLoai)) {
			return componentDetailDAO.getMainboardDetail(maSP);
		} else if ("CPU".equalsIgnoreCase(tenLoai)) {
			return componentDetailDAO.getCpuDetail(maSP);
		} else if ("VGA".equalsIgnoreCase(tenLoai)) {
			return componentDetailDAO.getVgaDetail(maSP);
		} else if ("Case".equalsIgnoreCase(tenLoai)) {
			return componentDetailDAO.getCaseDetail(maSP);
		} else if ("Nguồn".equalsIgnoreCase(tenLoai)) {
			return componentDetailDAO.getPsuDetail(maSP);
		} else if ("Tản nhiệt".equalsIgnoreCase(tenLoai)) {
			return componentDetailDAO.getCoolerDetail(maSP);
		} else if ("Ổ cứng".equalsIgnoreCase(tenLoai)) {
			return componentDetailDAO.getStorageDetail(maSP);
		} else if ("RAM".equalsIgnoreCase(tenLoai)) {
			return componentDetailDAO.getRamDetail(maSP);
		}
		return null;
	}
}
