package com.computerstore.controllers;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import com.computerstore.dao.PaymentDAO;
import com.computerstore.models.CartItem;
import com.computerstore.models.Order;
import com.computerstore.models.User;
import com.computerstore.services.CartService;
import com.computerstore.services.OrderService;
import com.computerstore.services.PaymentService;
import com.computerstore.utils.AppConstants;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = { "/checkout", "/orders", "/orders/cancel" })
public class OrderServlet extends HttpServlet {
	private OrderService orderService = new OrderService();
	private PaymentService paymentService = new PaymentService();
	private PaymentDAO paymentDAO = new PaymentDAO();
	private CartService cartService = new CartService();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = req.getServletPath();
		User u = (User) req.getSession().getAttribute("user");

		if (u == null) {
			resp.sendRedirect(req.getContextPath() + "/login");
			return;
		}

		if ("/checkout".equals(path)) {
			// Load cart data for order summary
			populateCheckoutAttributes(req, u.getMaKH());
			req.setAttribute("paymentMethods", paymentService.getPaymentMethods());
			req.getRequestDispatcher("/jsp/checkout.jsp").forward(req, resp);
		} else if ("/orders".equals(path)) {
			String action = req.getParameter("action");
			if ("detail".equals(action)) {
				// View order details
				try {
					int maDonHang = Integer.parseInt(req.getParameter("id"));
					Order order = orderService.getOrderById(maDonHang);
					// Check if this order belongs to current user
					if (order != null && order.getMaKH() == u.getMaKH()) {
						req.setAttribute("order", order);
						req.setAttribute("orderDetails", orderService.getOrderDetails(maDonHang));
						req.setAttribute("payment", paymentDAO.getByOrderId(maDonHang));
						req.getRequestDispatcher("/jsp/order-detail.jsp").forward(req, resp);
						return;
					}
				} catch (NumberFormatException e) {
					// Invalid order ID
				}
				resp.sendRedirect(req.getContextPath() + "/orders");
				return;
			}
			req.setAttribute("orders", orderService.getOrdersByCustomer(u.getMaKH()));
			req.getRequestDispatcher("/jsp/orders.jsp").forward(req, resp);
		}
	}

	private void populateCheckoutAttributes(HttpServletRequest req, int maKH) {
		List<CartItem> items = cartService.getCartItems(maKH);
		BigDecimal subtotal = items.stream().map(CartItem::getThanhTien).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal shipping = subtotal.compareTo(AppConstants.FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO
				: AppConstants.STANDARD_SHIPPING_FEE;
		BigDecimal discount = BigDecimal.ZERO;

		HttpSession session = req.getSession(false);
		if (session != null) {
			String voucherCode = (String) session.getAttribute("voucher");
			BigDecimal sessionDiscount = (BigDecimal) session.getAttribute("discount");
			if (sessionDiscount != null) {
				discount = sessionDiscount;
			}
			req.setAttribute("voucherCode", voucherCode);
		}

		BigDecimal total = subtotal.add(shipping).subtract(discount).max(BigDecimal.ZERO);

		req.setAttribute("cartItems", items);
		req.setAttribute("cartSubtotal", subtotal);
		req.setAttribute("cartTotal", total);
		req.setAttribute("discountAmount", discount);
		req.setAttribute("shippingAmount", shipping);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = req.getServletPath();
		User u = (User) req.getSession().getAttribute("user");

		if (u == null) {
			resp.sendRedirect(req.getContextPath() + "/login");
			return;
		}

		if ("/checkout".equals(path)) {
			String address = req.getParameter("address");
			String phone = req.getParameter("phone");
			String note = req.getParameter("note");

			if (phone == null || phone.isEmpty()) {
				phone = u.getSoDienThoai();
			}
			if (address == null || address.isEmpty()) {
				address = u.getDiaChi();
			}

			String paymentMethodStr = req.getParameter("maPTTT");
			int maPTTT = 1; // Default fallback
			if (paymentMethodStr != null && !paymentMethodStr.isEmpty()) {
				try {
					maPTTT = Integer.parseInt(paymentMethodStr);
				} catch (NumberFormatException ignored) {
				}
			}

			// Pass voucher to backend so DON_HANG.TongTien reflects discount
			Integer maKM = null;
			HttpSession session = req.getSession(false);

			if (session != null) {
				String voucherCode = (String) session.getAttribute("voucher");
				// current implementation matches voucher by TenKM in CartServlet; map code ->
				// MaKM via active promotions
				if (voucherCode != null && !voucherCode.isBlank()) {
					var promos = new com.computerstore.services.PromotionService().getActivePromotions();
					var match = promos.stream().filter(p -> voucherCode.trim().equalsIgnoreCase(p.getTenKM()))
							.findFirst();
					if (match.isPresent()) {
						maKM = match.get().getMaKM();
					}
				}
			}

			if (orderService.placeOrder(u.getMaKH(), address, phone, note, maKM, maPTTT)) {
				// clear voucher after successful checkout
				if (session != null) {
					session.removeAttribute("voucher");
					session.removeAttribute("discount");
				}
				resp.sendRedirect(req.getContextPath() + "/orders?success=true");
			} else {
				req.setAttribute("error", "Lỗi đặt hàng (Giỏ hàng trống?)");
				req.setAttribute("paymentMethods", paymentService.getPaymentMethods());
				req.getRequestDispatcher("/jsp/checkout.jsp").forward(req, resp);
			}
		} else if ("/orders/cancel".equals(path)) {
			String maDonHangStr = req.getParameter("maDonHang");
			if (maDonHangStr != null && !maDonHangStr.isEmpty()) {
				try {
					int maDonHang = Integer.parseInt(maDonHangStr);
					Order order = orderService.getOrderById(maDonHang);
					if (order != null && order.getMaKH() == u.getMaKH()) {
						if (orderService.cancelOrder(maDonHang)) {
							resp.sendRedirect(
									req.getContextPath() + "/orders?action=detail&id=" + maDonHang + "&cancel=success");
							return;
						}
					}
				} catch (NumberFormatException ignored) {
				}
			}
			resp.sendRedirect(req.getContextPath() + "/orders");
		}
	}
}
