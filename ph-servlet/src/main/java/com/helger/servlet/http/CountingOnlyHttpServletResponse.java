package com.helger.servlet.http;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.helger.http.CHTTPHeader;

/**
 * A response that includes no body, for use in (dumb) "HEAD" support. This just
 * swallows that body, counting the bytes in order to set the content length
 * appropriately. All other methods delegate directly to the wrapped HTTP
 * Servlet Response object.
 * 
 * @author Servlet Spec 3.1
 * @since 8.7.5
 */
class CountingOnlyHttpServletResponse extends HttpServletResponseWrapper
{
  private final CountingOnlyServletOutputStream m_aCountOnlyOS;
  private PrintWriter m_aWriter;
  private boolean m_bDidSetContentLength;
  private boolean m_bUsingOutputStream;

  CountingOnlyHttpServletResponse (final HttpServletResponse aResponse)
  {
    super (aResponse);
    m_aCountOnlyOS = new CountingOnlyServletOutputStream ();
  }

  void setContentLengthAutomatically ()
  {
    if (!m_bDidSetContentLength)
    {
      if (m_aWriter != null)
        m_aWriter.flush ();
      setContentLength (m_aCountOnlyOS.getContentLength ());
    }
  }

  @Override
  public void setContentLength (final int len)
  {
    super.setContentLength (len);
    m_bDidSetContentLength = true;
  }

  @Override
  public void setContentLengthLong (final long len)
  {
    super.setContentLengthLong (len);
    m_bDidSetContentLength = true;
  }

  private void _checkHeader (final String name)
  {
    if (CHTTPHeader.CONTENT_LENGTH.equalsIgnoreCase (name))
      m_bDidSetContentLength = true;
  }

  @Override
  public void setHeader (final String name, final String value)
  {
    super.setHeader (name, value);
    _checkHeader (name);
  }

  @Override
  public void addHeader (final String name, final String value)
  {
    super.addHeader (name, value);
    _checkHeader (name);
  }

  @Override
  public void setIntHeader (final String name, final int value)
  {
    super.setIntHeader (name, value);
    _checkHeader (name);
  }

  @Override
  public void addIntHeader (final String name, final int value)
  {
    super.addIntHeader (name, value);
    _checkHeader (name);
  }

  @Override
  public ServletOutputStream getOutputStream () throws IOException
  {
    if (m_aWriter != null)
      throw new IllegalStateException ("You already called getWriter!");
    m_bUsingOutputStream = true;
    return m_aCountOnlyOS;
  }

  @Override
  public PrintWriter getWriter () throws UnsupportedEncodingException
  {
    if (m_bUsingOutputStream)
      throw new IllegalStateException ("You already called getOutputStream!");

    if (m_aWriter == null)
    {
      final OutputStreamWriter w = new OutputStreamWriter (m_aCountOnlyOS, getCharacterEncoding ());
      m_aWriter = new PrintWriter (w);
    }
    return m_aWriter;
  }
}