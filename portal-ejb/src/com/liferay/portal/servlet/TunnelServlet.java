/**
 * Copyright (c) 2000-2006 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portal.servlet;

import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.security.auth.HttpPrincipal;
import com.liferay.portal.security.auth.PrincipalThreadLocal;
import com.liferay.portal.service.spring.UserLocalServiceUtil;
import com.liferay.portal.shared.util.MethodInvoker;
import com.liferay.portal.shared.util.MethodWrapper;
import com.liferay.util.ObjectValuePair;
import com.liferay.util.StringPool;
import com.liferay.portal.security.auth.PrincipalThreadLocal;
import com.liferay.portal.model.User;
import com.liferay.portal.service.spring.UserLocalServiceUtil;
import com.liferay.portal.shared.util.StackTraceUtil;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.security.permission.PermissionCheckerFactory;
import com.liferay.portal.security.permission.PermissionThreadLocal;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <a href="TunnelServlet.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Michael Weisser
 *
 */
public class TunnelServlet extends HttpServlet {

	public void doPost(HttpServletRequest req, HttpServletResponse res)
		throws IOException, ServletException {

		PermissionChecker permissionChecker = null;

		try {
			ObjectInputStream ois = new ObjectInputStream(req.getInputStream());

			Object returnObj = null;

			try {
				ObjectValuePair ovp = (ObjectValuePair)ois.readObject();

				HttpPrincipal httpPrincipal = (HttpPrincipal)ovp.getKey();
				MethodWrapper methodWrapper = (MethodWrapper)ovp.getValue();

				if (httpPrincipal.getUserId() != null) {
					PrincipalThreadLocal.setName(httpPrincipal.getUserId());

					User user = UserLocalServiceUtil.getUserById(
						httpPrincipal.getUserId());

					permissionChecker =
						PermissionCheckerFactory.create(user, true);

					PermissionThreadLocal.setPermissionChecker(
						permissionChecker);
				}

				if (returnObj == null) {
					returnObj = MethodInvoker.invoke(methodWrapper);
				}
			}
			catch (InvocationTargetException ite) {
				returnObj = ite.getCause();

				if (!(returnObj instanceof PortalException)) {
					ite.printStackTrace();

					returnObj = new SystemException();
				}
			}
			catch (Exception e) {
				_log.error(StackTraceUtil.getStackTrace(e));
			}

			if (returnObj != null) {
				ObjectOutputStream oos =
					new ObjectOutputStream(res.getOutputStream());

				oos.writeObject(returnObj);

				oos.flush();
				oos.close();
			}
		}
		finally {
			try {
				PermissionCheckerFactory.recycle(permissionChecker);
			}
			catch (Exception e) {
			}
		}
	}

	private String _getCompanyId(String userId)
		throws PortalException, SystemException {

		if (userId == null) {
			return StringPool.BLANK;
		}
		else {
			return UserLocalServiceUtil.getUserById(userId).getCompanyId();
		}
	}

	private String _getPortletId(String className) {
		String portletId = null;

		try {
			Object obj = Class.forName(className).newInstance();

			Field field = obj.getClass().getField(_PORTLET_ID);

			portletId = (String)field.get(obj);
		}
		catch (Exception e) {
		}

		return portletId;
	}

	private static final String _PORTLET_ID = "PORTLET_ID";

	private static Log _log = LogFactory.getLog(TunnelServlet.class);

}