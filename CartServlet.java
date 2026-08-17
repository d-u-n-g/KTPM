package com.computerstore.controllers;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import com.computerstore.models.CartItem;
import com.computerstore.models.User;
import com.computerstore.services.CartService;
import com.computerstore.services.PromotionService;
import com.computerstore.utils.AppConstants;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = { "/cart", "/cart/update", "/cart/remove", "/cart/clear", "/cart/voucher" })
public class CartServlet extends HttpServlet {
	private CartService cartService = new CartService();
	private PromotionService promotionService = new PromotionService();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		User u = (User) req.getSession().getAttribute("user");
		if (u == null) {
			resp.sendRedirect(req.getContextPath() + "/login");
			return;
		}
		populateCartAttributes(req, u.getMaKH());
		req.getRequestDispatcher("/jsp/cart.jsp").forward(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		User u = (User) req.getSession().getAttribute("user");
		if (u == null) {
			resp.sendRedirect(req.getContextPath() + "/login");
			return;
		}

		String path = req.getServletPath();
		HttpSession session = req.getSession();

		try {
			switch (path) {
				case "/cart/update": {
					int maSP = Integer.parseInt(req.getParameter("productId"));
					String action = req.getParameter("action");
					// Defensive: for +/- buttons action must be increase/decrease
					if ("increase".equals(action)) {

						List<CartItem> items = cartService.getCartItems(u.getMaKH());
						int current = items.stream().filter(i -> i.getMaSP() == maSP).mapToInt(CartItem::getSoLuong)
								.findFirst().orElse(0);
						cartService.updateQuantity(u.getMaKH(), maSP, current + 1);
					} else if ("decrease".equals(action)) {
						List<CartItem> items = cartService.getCartItems(u.getMaKH());
						int current = items.stream().filter(i -> i.getMaSP() == maSP).mapToInt(CartItem::getSoLuong)
								.findFirst().orElse(1);
						if (current <= 1)
							cartService.removeItem(u.getMaKH(), maSP);
						else
							cartService.updateQuantity(u.getMaKH(), maSP, current - 1);
					} else {
						// Legacy path: expects quantity field when action is not increase/decrease
						String qtyStr = req.getParameter("quantity");
						if (qtyStr == null || qtyStr.isBlank()) {
							// Không có quantity => bỏ qua để tránh parse lỗi khi bấm +/-
							break;
						}
						int qty = Integer.parseInt(qtyStr);
						if (qty <= 0)
							cartService.removeItem(u.getMaKH(), maSP);
						else
							cartService.updateQuantity(u.getMaKH(), maSP, qty);
					}

					break;
				}
				case "/cart/remove": {
					int maSP = Integer.parseInt(req.getParameter("productId"));
					cartService.removeItem(u.getMaKH(), maSP);
					break;
				}
				case "/cart/clear": {
					cartService.clearCart(u.getMaKH());
					session.removeAttribute("voucher");
					session.removeAttribute("discount");
					break;
				}
				case "/cart/voucher": {
					String code = req.getParameter("voucherCode");
					if (code != null && !code.isBlank()) {
						BigDecimal subtotal = cartService.getCartItems(u.getMaKH()).stream().map(CartItem::getThanhTien)
								.reduce(BigDecimal.ZERO, BigDecimal::add);
						// Try to find promotion by tenKM matching voucher code
						var promos = promotionService.getActivePromotions();
						var match = promos.stream().filter(p -> code.trim().equalsIgnoreCase(p.getTenKM())).findFirst();
						if (match.isPresent()) {
							int maKM = match.get().getMaKM();
							List<CartItem> items = cartService.getCartItems(u.getMaKH());
							BigDecimal discount = promotionService.applyVoucherToCart(maKM, items);
							session.setAttribute("voucher", code.trim().toUpperCase());
							session.setAttribute("discount", discount);
							session.setAttribute("voucherMsg",
									"Áp dụng mã thành công! Giảm " + discount.toPlainString() + "₫");
						} else {

							session.setAttribute("voucherError", "Mã giảm giá không hợp lệ hoặc đã hết hạn.");
							session.removeAttribute("voucher");
							session.removeAttribute("discount");
						}
					}
					break;
				}
				default: {
					// Legacy: /cart POST with action param
					String action = req.getParameter("action");
					int maSP = Integer.parseInt(req.getParameter("maSP"));
					if ("add".equals(action)) {
						int qty = Integer.parseInt(req.getParameter("quantity"));
						cartService.addToCart(u.getMaKH(), maSP, qty);
					} else if ("remove".equals(action)) {
						cartService.removeItem(u.getMaKH(), maSP);
					} else if ("update".equals(action)) {
						int qty = Integer.parseInt(req.getParameter("quantity"));
						if (qty <= 0)
							cartService.removeItem(u.getMaKH(), maSP);
						else
							cartService.updateQuantity(u.getMaKH(), maSP, qty);
					}
				}
			}
		} catch (Exception e) {
			session.setAttribute("cartError", e.getMessage());
			// Remove error after redirect to avoid showing it again
			// The error will be shown on the cart page and then cleared
		}

		resp.sendRedirect(req.getContextPath() + "/cart");
	}

	private void populateCartAttributes(HttpServletRequest req, int maTK) {
		List<CartItem> items = cartService.getCartItems(maTK);
		BigDecimal subtotal = items.stream().map(CartItem::getThanhTien).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal shipping = subtotal.compareTo(AppConstants.FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO
				: AppConstants.STANDARD_SHIPPING_FEE;
		BigDecimal discount = (BigDecimal) req.getSession().getAttribute("discount");
		if (discount == null)
			discount = BigDecimal.ZERO;
		BigDecimal total = subtotal.add(shipping).subtract(discount).max(BigDecimal.ZERO);

		req.setAttribute("cartItems", items);
		req.setAttribute("cartSubtotal", subtotal);
		req.setAttribute("cartTotal", total);
		req.setAttribute("discountAmount", discount);
		req.setAttribute("voucherCode", req.getSession().getAttribute("voucher"));
	}
}
