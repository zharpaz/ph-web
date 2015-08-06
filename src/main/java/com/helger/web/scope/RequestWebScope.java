/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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
package com.helger.web.scope;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.CGlobal;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.charset.CCharset;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.multimap.IMultiMapListBased;
import com.helger.commons.collection.multimap.MultiHashMapArrayListBased;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.lang.ServiceLoaderHelper;
import com.helger.web.fileupload.IFileItem;
import com.helger.web.fileupload.IFileItemFactory;
import com.helger.web.fileupload.IFileItemFactoryProviderSPI;
import com.helger.web.fileupload.IProgressListener;
import com.helger.web.fileupload.exception.FileUploadException;
import com.helger.web.fileupload.scope.GlobalDiskFileItemFactory;
import com.helger.web.fileupload.scope.ProgressListenerProvider;
import com.helger.web.fileupload.servlet.ServletFileUpload;
import com.helger.web.mock.MockHttpServletRequest;

/**
 * The default request web scope that also tries to parse multi part requests.
 *
 * @author Philip Helger
 */
public class RequestWebScope extends RequestWebScopeNoMultipart
{
  /**
   * The maximum size of a single file (in bytes) that will be handled. May not
   * be larger than 2 GB as browsers cannot handle more than 2GB. See e.g.
   * http://www.motobit.com/help/ScptUtl/pa33.htm or
   * https://bugzilla.mozilla.org/show_bug.cgi?id=215450 Extensive analysis:
   * <a href=
   * "http://tomcat.10.n6.nabble.com/Problems-uploading-huge-files-gt-2GB-to-Tomcat-app-td4730850.html"
   * >here</a>
   */
  public static final long MAX_REQUEST_SIZE = 5 * CGlobal.BYTES_PER_GIGABYTE;

  private static final Logger s_aLogger = LoggerFactory.getLogger (RequestWebScope.class);
  private static final IFileItemFactoryProviderSPI s_aFIFP;

  static
  {
    s_aFIFP = ServiceLoaderHelper.getFirstSPIImplementation (IFileItemFactoryProviderSPI.class);
    if (s_aFIFP != null)
      s_aLogger.info ("Using custom IFileItemFactoryProviderSPI " + s_aFIFP);
  }

  public RequestWebScope (@Nonnull final HttpServletRequest aHttpRequest,
                          @Nonnull final HttpServletResponse aHttpResponse)
  {
    super (aHttpRequest, aHttpResponse);
  }

  /**
   * Check if the parsed request is a multi part request, potentially containing
   * uploaded files.
   *
   * @return <code>true</code> if the current request is a multi part request
   */
  private boolean _isMultipartContent ()
  {
    return !(m_aHttpRequest instanceof MockHttpServletRequest) && ServletFileUpload.isMultipartContent (m_aHttpRequest);
  }

  @Nonnull
  private static IFileItemFactory _getFileItemFactory ()
  {
    if (s_aFIFP != null)
      return s_aFIFP.getFileItemFactory ();

    // Default to the global one
    return GlobalDiskFileItemFactory.getInstance ();
  }

  @Override
  @OverrideOnDemand
  protected boolean addSpecialRequestAttributes ()
  {
    // check file uploads
    // Note: this handles only POST parameters!
    boolean bAddedFileUploadItems = false;
    if (_isMultipartContent ())
    {
      try
      {
        // Setup the ServletFileUpload....
        final ServletFileUpload aUpload = new ServletFileUpload (_getFileItemFactory ());
        aUpload.setSizeMax (MAX_REQUEST_SIZE);
        aUpload.setHeaderEncoding (CCharset.CHARSET_UTF_8);
        final IProgressListener aProgressListener = ProgressListenerProvider.getInstance ().getProgressListener ();
        if (aProgressListener != null)
          aUpload.setProgressListener (aProgressListener);

        try
        {
          m_aHttpRequest.setCharacterEncoding (CCharset.CHARSET_UTF_8);
        }
        catch (final UnsupportedEncodingException ex)
        {
          s_aLogger.error ("Failed to set request character encoding to '" + CCharset.CHARSET_UTF_8 + "'", ex);
        }

        // Group all items with the same name together
        final IMultiMapListBased <String, String> aFormFields = new MultiHashMapArrayListBased <String, String> ();
        final IMultiMapListBased <String, IFileItem> aFormFiles = new MultiHashMapArrayListBased <String, IFileItem> ();
        final List <IFileItem> aFileItems = aUpload.parseRequest (m_aHttpRequest);
        for (final IFileItem aFileItem : aFileItems)
        {
          if (aFileItem.isFormField ())
          {
            // We need to explicitly use the charset, as by default only the
            // charset from the content type is used!
            aFormFields.putSingle (aFileItem.getFieldName (), aFileItem.getString (CCharset.CHARSET_UTF_8_OBJ));
          }
          else
            aFormFiles.putSingle (aFileItem.getFieldName (), aFileItem);
        }

        // set all form fields
        for (final Map.Entry <String, List <String>> aEntry : aFormFields.entrySet ())
        {
          // Convert list of String to value (String or String[])
          final List <String> aValues = aEntry.getValue ();
          final Object aValue = aValues.size () == 1 ? CollectionHelper.getFirstElement (aValues)
                                                     : ArrayHelper.newArray (aValues, String.class);
          setAttribute (aEntry.getKey (), aValue);
        }

        // set all form files (potentially overwriting form fields with the same
        // name)
        for (final Map.Entry <String, List <IFileItem>> aEntry : aFormFiles.entrySet ())
        {
          // Convert list of String to value (IFileItem or IFileItem[])
          final List <IFileItem> aValues = aEntry.getValue ();
          final Object aValue = aValues.size () == 1 ? CollectionHelper.getFirstElement (aValues)
                                                     : ArrayHelper.newArray (aValues, IFileItem.class);
          setAttribute (aEntry.getKey (), aValue);
        }

        // Parsing complex file upload succeeded -> do not use standard scan for
        // parameters
        bAddedFileUploadItems = true;
      }
      catch (final FileUploadException ex)
      {
        if (!StreamHelper.isKnownEOFException (ex.getCause ()))
          s_aLogger.error ("Error parsing multipart request content", ex);
      }
      catch (final RuntimeException ex)
      {
        s_aLogger.error ("Error parsing multipart request content", ex);
      }
    }
    return bAddedFileUploadItems;
  }
}