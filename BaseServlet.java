package com.computerstore.controllers;

import java.io.IOException;

import com.computerstore.exceptions.AppException;
import com.computerstore.models.User;
import com.computerstore.utils.ValidationUtil;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Base servlet providing common functionality for all servlets
 */
public abstract class BaseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Common URL patterns
    protected static final String HOME_URL = "/home";
    protected static final String LOGIN_URL = "/login";
    protected static final String REGISTER_URL = "/register";
    protected static final String CART_URL = "/cart";
    protected static final String CHECKOUT_URL = "/checkout";
    protected static final String PROFILE_URL = "/profile";
    protected static final String ADMIN_URL = "/admin/";

    // JSP paths
    protected static final String ERROR_JSP = "/jsp/error.jsp";
    protected static final String LOGIN_JSP = "/jsp/login.jsp";

    /**
     * Get current logged-in user from session
     */
    protected User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null ? (User) session.getAttribute("user") : null;
    }

    /**
     * Check if user is logged in
     */
    protected boolean isLoggedIn(HttpServletRequest request) {
        return getCurrentUser(request) != null;
    }

    /**
     * Check if current user is admin
     */
    protected boolean isAdmin(HttpServletRequest request) {
        User user = getCurrentUser(request);
        return user != null && "QUAN_TRI_VIEN".equals(user.getVaiTro());
    }

    /**
     * Require user to be logged in, redirect to login if not
     */
    protected void requireLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (!isLoggedIn(request)) {
            String redirectUrl = request.getRequestURI();
            if (request.getQueryString() != null) {
                redirectUrl += "?" + request.getQueryString();
            }
            request.getSession().setAttribute("redirectUrl", redirectUrl);
            response.sendRedirect(request.getContextPath() + LOGIN_URL);
            return;
        }
    }

    /**
     * Require user to be admin, redirect to home if not
     */
    protected void requireAdmin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (!isAdmin(request)) {
            response.sendRedirect(request.getContextPath() + HOME_URL);
            return;
        }
    }

    /**
     * Require user to be logged in and be a customer
     */
    protected void requireCustomer(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        requireLogin(request, response);
        if (!isLoggedIn(request))
            return;

        User user = getCurrentUser(request);
        if (!"KHACH_HANG".equals(user.getVaiTro())) {
            response.sendRedirect(request.getContextPath() + HOME_URL);
        }
    }

    /**
     * Sanitize input string
     */
    protected String sanitize(String input) {
        return ValidationUtil.sanitizeInput(input);
    }

    /**
     * Parse integer parameter with default value
     */
    protected int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Parse integer parameter with default value of 0
     */
    protected int parseInt(String value) {
        return parseInt(value, 0);
    }

    /**
     * Get parameter value, return null if empty
     */
    protected String getParameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value != null && value.trim().isEmpty()) {
            return null;
        }
        return value;
    }

    /**
     * Set success flash message
     */
    protected void setSuccessMessage(HttpSession session, String message) {
        session.setAttribute("successMessage", message);
    }

    /**
     * Set error flash message
     */
    protected void setErrorMessage(HttpSession session, String message) {
        session.setAttribute("errorMessage", message);
    }

    /**
     * Set validation error
     */
    protected void setValidationError(HttpSession session, String field, String message) {
        session.setAttribute("validationError", field + ": " + message);
    }

    /**
     * Handle exception and forward to error page
     */
    protected void handleError(Exception e, HttpServletRequest request, HttpServletResponse response)
            throws IOException, jakarta.servlet.ServletException {
        if (e instanceof AppException appException) {
            request.setAttribute("errorCode", appException.getErrorCode());
            request.setAttribute("errorMessage", appException.getUserMessage());
            request.setAttribute("httpStatus", appException.getHttpStatus());
        } else {
            request.setAttribute("errorCode", "INTERNAL_SERVER_ERROR");
            request.setAttribute("errorMessage", "Có lỗi xảy ra. Vui lòng thử lại sau.");
            request.setAttribute("httpStatus", 500);
        }

        request.setAttribute("requestUri", request.getRequestURI());
        request.getRequestDispatcher(ERROR_JSP).forward(request, response);
    }

    /**
     * Redirect back with error message
     */
    protected void redirectBackWithError(HttpServletRequest request, HttpServletResponse response,
            String message) throws IOException {
        HttpSession session = request.getSession();
        session.setAttribute("errorMessage", message);

        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains(request.getContextPath())) {
            response.sendRedirect(referer);
        } else {
            response.sendRedirect(request.getContextPath() + HOME_URL);
        }
    }

    /**
     * Redirect back with success message
     */
    protected void redirectBackWithSuccess(HttpServletRequest request, HttpServletResponse response,
            String message) throws IOException {
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", message);

        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains(request.getContextPath())) {
            response.sendRedirect(referer);
        } else {
            response.sendRedirect(request.getContextPath() + HOME_URL);
        }
    }

    /**
     * Get base URL of the application
     */
    protected String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
            url.append(":").append(serverPort);
        }
        url.append(contextPath);
        return url.toString();
    }

    /**
     * Check if request is AJAX
     */
    protected boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }
}
