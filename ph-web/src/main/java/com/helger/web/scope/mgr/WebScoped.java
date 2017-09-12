/**
 * Copyright (C) 2014-2017 Philip Helger (www.helger.com)
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
package com.helger.web.scope.mgr;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.helger.commons.annotation.Nonempty;
import com.helger.scope.mgr.ScopeManager;
import com.helger.servlet.mock.MockHttpServletResponse;
import com.helger.servlet.mock.OfflineHttpServletRequest;
import com.helger.web.scope.IRequestWebScope;

/**
 * Auto closable wrapper around
 * {@link WebScopeManager#onRequestBegin(String, HttpServletRequest, HttpServletResponse)}
 * and {@link WebScopeManager#onRequestEnd()}
 *
 * @author Philip Helger
 * @since 9.0.0
 */
public class WebScoped implements AutoCloseable
{
  private IRequestWebScope m_aRequestScope;

  public WebScoped ()
  {
    this (ScopeManager.APPLICATION_ID_NOT_AVAILABLE);
  }

  public WebScoped (@Nonnull @Nonempty final String sApplicationID)
  {
    this (sApplicationID,
          new OfflineHttpServletRequest (WebScopeManager.getGlobalScope ().getServletContext (), false));
  }

  public WebScoped (@Nonnull @Nonempty final String sApplicationID, @Nonnull final HttpServletRequest aHttpRequest)
  {
    this (sApplicationID, aHttpRequest, new MockHttpServletResponse ());
  }

  public WebScoped (@Nonnull final HttpServletRequest aHttpRequest, @Nonnull final HttpServletResponse aHttpResponse)
  {
    this (ScopeManager.APPLICATION_ID_NOT_AVAILABLE, aHttpRequest, aHttpResponse);
  }

  public WebScoped (@Nonnull @Nonempty final String sApplicationID,
                    @Nonnull final HttpServletRequest aHttpRequest,
                    @Nonnull final HttpServletResponse aHttpResponse)
  {
    m_aRequestScope = WebScopeManager.onRequestBegin (sApplicationID, aHttpRequest, aHttpResponse);
  }

  @Nonnull
  public IRequestWebScope getRequestScope ()
  {
    if (m_aRequestScope == null)
      throw new IllegalStateException ("No request scope present!");
    return m_aRequestScope;
  }

  public void close ()
  {
    m_aRequestScope = null;
    WebScopeManager.onRequestEnd ();
  }
}