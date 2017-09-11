/**
 * Copyright (C) 2016-2017 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.xservlet.filter;

import java.io.IOException;
import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.http.EHttpMethod;
import com.helger.commons.state.EContinue;
import com.helger.http.EHttpVersion;

/**
 * Low-level filter for a single XServlet. It has methods for before and after.
 *
 * @author Philip Helger
 * @since 9.0.0
 */
public interface IXServletLowLevelFilter extends Serializable
{
  /**
   * Invoked before an XServlet request is handled. This method is created
   * before the request is created! Exceptions occurring in this method will be
   * propagated to the outside, so be careful :)
   *
   * @param aHttpRequest
   *        HTTP servlet request. Never <code>null</code>.
   * @param aHttpResponse
   *        HTTP servlet response. Never <code>null</code>.
   * @param eHttpVersion
   *        HTTP version. Never <code>null</code>.
   * @param eHttpMethod
   *        HTTP method. Never <code>null</code>.
   * @param sApplicationID
   *        Application ID. Neither <code>null</code> nor empty.
   * @return {@link EContinue#CONTINUE} to continue processing, or
   *         {@link EContinue#BREAK} if this request should not be processed, in
   *         which case the HttpServletResponse must contain a valid response!
   * @exception ServletException
   *            in case of business logic error.
   * @throws IOException
   *         in case of IO error.
   */
  @Nonnull
  EContinue beforeRequest (@Nonnull HttpServletRequest aHttpRequest,
                           @Nonnull HttpServletResponse aHttpResponse,
                           @Nonnull EHttpVersion eHttpVersion,
                           @Nonnull EHttpMethod eHttpMethod,
                           @Nonnull @Nonempty String sApplicationID) throws ServletException, IOException;

  /**
   * Invoked after an XServlet request was handled. After is always called, even
   * if before request was canceled (in a finally)! Exceptions occurring in this
   * method will be propagated to the outside, so be careful :)
   *
   * @param aHttpRequest
   *        HTTP servlet request. Never <code>null</code>.
   * @param aHttpResponse
   *        HTTP servlet response. Never <code>null</code>.
   * @param eHttpVersion
   *        HTTP version. Never <code>null</code>.
   * @param eHttpMethod
   *        HTTP method. Never <code>null</code>.
   * @param bInvokeHandler
   *        <code>true</code> if the main handler was invoked,
   *        <code>false</code> if
   *        {@link #beforeRequest(HttpServletRequest, HttpServletResponse, EHttpVersion, EHttpMethod, String)}
   *        avoided the execution of the request.
   * @param aCaughtException
   *        An optionally caught exception. May be <code>null</code>. The
   *        exception was already logged, so please don't log it again!
   * @param bIsHandledAsync
   *        <code>true</code> if the request is handled asynchronously
   * @exception ServletException
   *            in case of business logic error
   * @throws IOException
   *         in case of IO error
   */
  void afterRequest (@Nonnull HttpServletRequest aHttpRequest,
                     @Nonnull HttpServletResponse aHttpResponse,
                     @Nonnull EHttpVersion eHttpVersion,
                     @Nonnull EHttpMethod eHttpMethod,
                     boolean bInvokeHandler,
                     @Nullable Throwable aCaughtException,
                     boolean bIsHandledAsync) throws ServletException, IOException;
}
