/**
 * openHAB, the open Home Automation Bus.
 * Copyright (C) 2011, openHAB.org <admin@openhab.org>
 *
 * See the contributors.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Additional permission under GNU GPL version 3 section 7
 *
 * If you modify this Program, or any covered work, by linking or
 * combining it with Eclipse (or a modified version of that library),
 * containing parts covered by the terms of the Eclipse Public License
 * (EPL), the licensors of this Program grant you additional permission
 * to convey the resulting work.
 */

package org.openhab.io.net.http;

import java.io.IOException;
import java.net.URL;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.util.Base64;
import org.eclipse.jetty.plus.jaas.callback.ObjectCallback;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of {@link HttpContext} which adds Basic-Authentication 
 * functionality to openHAB.
 * 
 * @author Thomas.Eichstaedt-Engelen
 * @since 0.9.0
 */
public class SecureHttpContext implements HttpContext {

	private static final Logger logger = 
		LoggerFactory.getLogger(SecureHttpContext.class);

	/** the name of the system property which switches the openhab security*/
	public static final String SECURITY_SYSTEM_PROPERTY = "openhab.securityEnabled";
	
	private static final String HTTP_HEADER__AUTHENTICATE = "WWW-Authenticate";

	private static final String HTTP_HEADER__AUTHORIZATION = "Authorization";
	
	private final HttpContext defaultContext;

	private final String realm;

	
	public SecureHttpContext(HttpContext defaultContext, final String realm) {
		this.defaultContext = defaultContext;
		this.realm = realm;
	}

	
	/**
	 * <p>@{inheritDoc}</p>
	 * <p>Delegates to <code>defaultContext.getMimeType()</code> 
	 */
	public String getMimeType(String name) {
		return this.defaultContext.getMimeType(name);
	}

	/**
	 * <p>@{inheritDoc}</p>
	 * <p>Delegates to <code>defaultContext.getResource()</code> 
	 */
	public URL getResource(String name) {
		return this.defaultContext.getResource(name);
	}

	/**
	 * @{inheritDoc}
	 */
	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) {
		boolean authenticationResult = false;

		try {
			String authHeader = request.getHeader(HTTP_HEADER__AUTHORIZATION);
			if (StringUtils.isBlank(authHeader)) {
				sendAuthenticationHeader(response, realm);
			}
			else {
				authenticationResult = computeAuthHeader(request, authHeader, realm);
				if (!authenticationResult) {
					response.sendError(HttpServletResponse.SC_FORBIDDEN);
				}
			}
		}
		catch (IOException ioe) {
			logger.warn("sending response failed", ioe.getLocalizedMessage());
		}

		return authenticationResult;
	}

	/**
	 * Sets the authentication header for BasicAuthentication and sends the
	 * response back to the client (HTTP-StatusCode '401' UNAUTHORIZED).
	 * 
	 * @param response to set the authentication header
	 * @param realm the given <code>realm</code>
	 * 
	 * @throws IOException if an error occurred while sending <code>response</code> 
	 */
	private void sendAuthenticationHeader(HttpServletResponse response, final String realm) throws IOException {
		response.setHeader(HTTP_HEADER__AUTHENTICATE,
			HttpServletRequest.BASIC_AUTH + " realm=\"" + realm + "\"");
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
	}

	/**
	 * Parses the given <code>authHeader</code>, extracts username and password
	 * and tries to authenticate with these credentials. If the login succeeded
	 * it sets the appropriate headers to the <code>request</code>
	 * 
	 * @param request
	 * @param authHeader
	 * @param realm
	 * 
	 * @return <code>true</code> if the login succeeded and <code>false</code>
	 * in all other cases.
	 */
	private boolean computeAuthHeader(HttpServletRequest request, final String authHeader, final String realm) {
		logger.trace("received authentication request '{}'", authHeader);
		
		String[] authHeaders = authHeader.trim().split(" ");
		if (authHeaders.length == 2) {
			String authType = StringUtils.trim(authHeaders[0]);
			String authInfo = StringUtils.trim(authHeaders[1]);

			if (HttpServletRequest.BASIC_AUTH.equalsIgnoreCase(authType)) {
				String authInfoString = new String(Base64.decodeBase64(authInfo));
				String[] authInfos = authInfoString.split(":");
				if (authInfos.length < 2) {
					logger.warn("authInfos '{}' must contain two elements separated by a colon", authInfoString);
					return false;
				}		
				
				String username = authInfos[0];
				String password = authInfos[1];
				
				Subject subject = authenticate(realm, username, password);
				if (subject != null) {
					request.setAttribute(
							HttpContext.AUTHENTICATION_TYPE,
							HttpServletRequest.BASIC_AUTH);
					request.setAttribute(HttpContext.REMOTE_USER, username);
					logger.trace("authentication of user '{}' succeeded!", username);
					return true;
				}
			}
			else {
				logger.warn("we don't support '{}' authentication -> processing aborted", authType);
			}
		}
		else {
			logger.warn("authentication header '{}' must contain of two parts separated by a blank", authHeader);
		}
		
		return false;
	}

	/**
	 * <p>Authenticates the given <code>username</code> and <code>password</code>
	 * with respect to the given <code>realm</code> against the configured
	 * {@link LoginModule} (see login.conf in &lt;openhabhome&gt;/etc to learn
	 * more about the configured {@link LoginModule})</p>
	 * <p><b>Note:</b>Roles aren't supported yet!</p>
	 * 
	 * @param realm the realm used by the configured {@link LoginModule}. 
	 * <i>Note:</i> the given <code>realm</code> must be same name as configured
	 * in <code>login.conf</code>
	 * @param username
	 * @param password
	 * 
	 * @return a {@link Subject} filled with username, password, realm, etc. or
	 * <code>null</code> if the login failed
	 * @throws UnsupportedCallbackException if a {@link Callback}-instance other
	 * than {@link NameCallback} or {@link ObjectCallback} is going to be handled
	 */
	private Subject authenticate(final String realm, final String username, final String password) {
		try {
			logger.trace("going to authenticate user '{}', real '{}'", username, realm);

			Subject subject = new Subject();
			
			LoginContext lContext = new LoginContext(realm, subject,
				new CallbackHandler() {
					public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
						for (int i = 0; i < callbacks.length; i++) {
							if (callbacks[i] instanceof NameCallback) {
								((NameCallback) callbacks[i]).setName(username);
							}
							else if (callbacks[i] instanceof ObjectCallback) {
								((ObjectCallback) callbacks[i]).setObject(password);
							}
							else {
								throw new UnsupportedCallbackException(callbacks[i]);
							}
						}
					}
				});
			lContext.login();

			// TODO: TEE: implement role handling here!
			
			return subject;
		}
		catch (LoginException le) {
			logger.warn("authentication of user '" + username + "' failed", le);
			return null;
		}
	}
	

}
