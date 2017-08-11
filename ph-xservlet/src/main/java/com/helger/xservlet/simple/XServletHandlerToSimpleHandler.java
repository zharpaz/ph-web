package com.helger.xservlet.simple;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.CHttp;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.regex.RegExHelper;
import com.helger.commons.state.EContinue;
import com.helger.commons.statistics.IMutableStatisticsHandlerCounter;
import com.helger.commons.statistics.StatisticsManager;
import com.helger.commons.string.StringHelper;
import com.helger.http.EHttpMethod;
import com.helger.http.EHttpVersion;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScope;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.handler.IXServletHandler;

final class XServletHandlerToSimpleHandler implements IXServletHandler
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (XServletHandlerToSimpleHandler.class);

  private final IMutableStatisticsHandlerCounter m_aStatsHasLastModification = StatisticsManager.getCounterHandler (getClass ().getName () +
                                                                                                                    "$has-lastmodification");
  private final IMutableStatisticsHandlerCounter m_aStatsHasETag = StatisticsManager.getCounterHandler (getClass ().getName () +
                                                                                                        "$has-etag");
  private final IMutableStatisticsHandlerCounter m_aStatsNotModifiedIfModifiedSince = StatisticsManager.getCounterHandler (getClass ().getName () +
                                                                                                                           "$notmodified.if-modified-since");
  private final IMutableStatisticsHandlerCounter m_aStatsModifiedIfModifiedSince = StatisticsManager.getCounterHandler (getClass ().getName () +
                                                                                                                        "$modified.if-modified-since");
  private final IMutableStatisticsHandlerCounter m_aStatsNotModifiedIfUnmodifiedSince = StatisticsManager.getCounterHandler (getClass ().getName () +
                                                                                                                             "$notmodified.if-unmodified-since");
  private final IMutableStatisticsHandlerCounter m_aStatsModifiedIfUnmodifiedSince = StatisticsManager.getCounterHandler (getClass ().getName () +
                                                                                                                          "$modified.if-unmodified-since");
  private final IMutableStatisticsHandlerCounter m_aStatsNotModifiedIfNonMatch = StatisticsManager.getCounterHandler (getClass ().getName () +
                                                                                                                      "$notmodified.if-unon-match");
  private final IMutableStatisticsHandlerCounter m_aStatsModifiedIfNonMatch = StatisticsManager.getCounterHandler (getClass ().getName () +
                                                                                                                   "$modified.if-unon-match");

  private final IXServletSimpleHandler m_aSimpleHandler;
  private String m_sApplicationID;

  public XServletHandlerToSimpleHandler (@Nonnull final IXServletSimpleHandler aSimpleHandler)
  {
    ValueEnforcer.notNull (aSimpleHandler, "SimpleHandler");
    m_aSimpleHandler = aSimpleHandler;
  }

  public final void onServletInit (@Nonnull @Nonempty final String sApplicationID,
                                   @Nonnull final Map <String, String> aInitParams)
  {
    m_sApplicationID = sApplicationID;
    m_aSimpleHandler.onServletInit (sApplicationID, aInitParams);
  }

  private void _onException (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                             @Nonnull final UnifiedResponse aUnifiedResponse,
                             @Nonnull final Throwable t) throws IOException, ServletException
  {
    s_aLogger.error ("An exception was caught in servlet processing for application '" + m_sApplicationID + "'", t);

    // Invoke exception handler
    if (m_aSimpleHandler.onException (aRequestScope, aUnifiedResponse, t).isContinue ())
    {
      // Propagate exception
      if (t instanceof IOException)
        throw (IOException) t;
      if (t instanceof ServletException)
        throw (ServletException) t;
      throw new ServletException (t);
    }
  }

  @Nonnull
  private EContinue _handleETag (@Nonnull final HttpServletRequest aHttpRequest,
                                 @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                 @Nonnull final UnifiedResponse aUnifiedResponse)
  {
    final LocalDateTime aLastModification = m_aSimpleHandler.getLastModificationDateTime (aRequestScope);
    if (aLastModification != null)
    {
      m_aStatsHasLastModification.increment ();

      // Get the If-Modified-Since date header
      final long nRequestIfModifiedSince = aHttpRequest.getDateHeader (CHttpHeader.IF_MODIFIED_SINCE);
      if (nRequestIfModifiedSince >= 0)
      {
        final LocalDateTime aRequestIfModifiedSince = CHttp.convertMillisToLocalDateTime (nRequestIfModifiedSince);
        if (aLastModification.compareTo (aRequestIfModifiedSince) <= 0)
        {
          if (s_aLogger.isDebugEnabled ())
            s_aLogger.debug ("Requested resource was not modified: " + aRequestScope.getPathWithinServlet ());

          // Was not modified since the passed time
          m_aStatsNotModifiedIfModifiedSince.increment ();
          return EContinue.BREAK;
        }
        m_aStatsModifiedIfModifiedSince.increment ();
      }

      // Get the If-Unmodified-Since date header
      final long nRequestIfUnmodifiedSince = aHttpRequest.getDateHeader (CHttpHeader.IF_UNMODIFIED_SINCE);
      if (nRequestIfUnmodifiedSince >= 0)
      {
        final LocalDateTime aRequestIfUnmodifiedSince = CHttp.convertMillisToLocalDateTime (nRequestIfUnmodifiedSince);
        if (aLastModification.compareTo (aRequestIfUnmodifiedSince) >= 0)
        {
          if (s_aLogger.isDebugEnabled ())
            s_aLogger.debug ("Requested resource was not modified: " + aRequestScope.getPathWithinServlet ());

          // Was not modified since the passed time
          m_aStatsNotModifiedIfUnmodifiedSince.increment ();
          return EContinue.BREAK;
        }
        m_aStatsModifiedIfUnmodifiedSince.increment ();
      }

      // No If-Modified-Since request header present, set the Last-Modified
      // header for later reuse
      aUnifiedResponse.setLastModified (aLastModification);
    }

    // Handle the ETag
    final String sSupportedETag = m_aSimpleHandler.getSupportedETag (aRequestScope);
    if (StringHelper.hasText (sSupportedETag))
    {
      m_aStatsHasETag.increment ();

      // get the request ETag
      final String sRequestETags = aHttpRequest.getHeader (CHttpHeader.IF_NON_MATCH);
      if (StringHelper.hasText (sRequestETags))
      {
        // Request header may contain several ETag values
        final ICommonsList <String> aAllETags = RegExHelper.getSplitToList (sRequestETags, ",\\s+");
        if (aAllETags.isEmpty ())
          s_aLogger.warn ("Empty ETag list found (" + sRequestETags + ")");
        else
        {
          // Scan all found ETags for match
          for (final String sCurrentETag : aAllETags)
            if (sSupportedETag.equals (sCurrentETag))
            {
              if (s_aLogger.isDebugEnabled ())
                s_aLogger.debug ("Requested resource has the same E-Tag: " + aRequestScope.getPathWithinServlet ());

              // We have a matching ETag
              m_aStatsNotModifiedIfNonMatch.increment ();
              return EContinue.BREAK;
            }
        }
        m_aStatsModifiedIfNonMatch.increment ();
      }

      // Save the ETag for the response
      aUnifiedResponse.setETagIfApplicable (sSupportedETag);
    }
    return EContinue.CONTINUE;
  }

  public void onRequest (@Nonnull final HttpServletRequest aHttpRequest,
                         @Nonnull final HttpServletResponse aHttpResponse,
                         @Nonnull final EHttpVersion eHttpVersion,
                         @Nonnull final EHttpMethod eHttpMethod,
                         @Nonnull final IRequestWebScope aRequestScope) throws ServletException, IOException
  {
    final UnifiedResponse aUnifiedResponse = m_aSimpleHandler.createUnifiedResponse (eHttpVersion,
                                                                                     eHttpMethod,
                                                                                     aHttpRequest);
    if (m_aSimpleHandler.initRequestState (aRequestScope, aUnifiedResponse).isBreak ())
    {
      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Cancelled request after initRequestState with response " + aUnifiedResponse);

      // May e.g. be an 404 error for some not-found resource
    }
    else
    {
      // Init was successful

      // Check for last-modification on GET and HEAD
      boolean bExecute = true;
      if (eHttpMethod == EHttpMethod.GET || eHttpMethod == EHttpMethod.HEAD)
        if (_handleETag (aHttpRequest, aRequestScope, aUnifiedResponse).isBreak ())
        {
          // ETag present in request
          aUnifiedResponse.setStatus (HttpServletResponse.SC_NOT_MODIFIED);
          bExecute = false;
        }

      if (bExecute)
      {
        // On request begin
        try
        {
          m_aSimpleHandler.onRequestBegin (aRequestScope);
        }
        catch (final Throwable t)
        {
          _onException (aRequestScope, aUnifiedResponse, t);
        }

        Throwable aCaughtException = null;
        try
        {
          // main servlet handling
          m_aSimpleHandler.handleRequest (aRequestScope, aUnifiedResponse);

          if (s_aLogger.isDebugEnabled ())
            s_aLogger.debug ("Successfully handled request: " + aRequestScope.getPathWithinServlet ());
        }
        catch (final Throwable t)
        {
          // Invoke exception handler
          // This internally re-throws the exception if needed
          aCaughtException = t;
          _onException (aRequestScope, aUnifiedResponse, t);
        }
        finally
        {
          // On request end
          try
          {
            m_aSimpleHandler.onRequestEnd (aCaughtException);
          }
          catch (final Throwable t)
          {
            s_aLogger.error ("onRequestEnd failed", t);
            // Don't throw anything here
          }
        }
      }
    }
    aUnifiedResponse.applyToResponse (aHttpResponse);
  }

  public final void onServletDestroy ()
  {
    m_aSimpleHandler.onServletDestroy ();
  }
}