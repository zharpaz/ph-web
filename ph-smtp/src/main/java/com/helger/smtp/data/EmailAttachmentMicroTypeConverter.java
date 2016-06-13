/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.smtp.data;

import java.nio.charset.Charset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ContainsSoftMigration;
import com.helger.commons.base64.Base64;
import com.helger.commons.charset.CharsetManager;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.IMicroTypeConverter;

public final class EmailAttachmentMicroTypeConverter implements IMicroTypeConverter
{
  private static final String ATTR_FILENAME = "filename";
  private static final String ATTR_CHARSET = "charset";
  private static final String ATTR_DISPOSITION = "disposition";

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final Object aSource,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final IEmailAttachment aAttachment = (IEmailAttachment) aSource;
    final IMicroElement eAttachment = new MicroElement (sNamespaceURI, sTagName);
    eAttachment.setAttribute (ATTR_FILENAME, aAttachment.getFilename ());
    if (aAttachment.getCharset () != null)
      eAttachment.setAttribute (ATTR_CHARSET, aAttachment.getCharset ().name ());
    eAttachment.setAttribute (ATTR_DISPOSITION, aAttachment.getDisposition ().getID ());
    // Base64 encode
    eAttachment.appendText (Base64.encodeBytes (StreamHelper.getAllBytes (aAttachment.getInputStream ())));
    return eAttachment;
  }

  @Nonnull
  @ContainsSoftMigration
  public EmailAttachment convertToNative (@Nonnull final IMicroElement eAttachment)
  {
    final String sFilename = eAttachment.getAttributeValue (ATTR_FILENAME);

    final String sCharset = eAttachment.getAttributeValue (ATTR_CHARSET);
    final Charset aCharset = sCharset == null ? null : CharsetManager.getCharsetFromName (sCharset);

    final String sDisposition = eAttachment.getAttributeValue (ATTR_DISPOSITION);
    EEmailAttachmentDisposition eDisposition = EEmailAttachmentDisposition.getFromIDOrNull (sDisposition);
    // migration
    if (eDisposition == null)
      eDisposition = EmailAttachment.DEFAULT_DISPOSITION;

    final byte [] aContent = Base64.safeDecode (eAttachment.getTextContent ());

    return new EmailAttachment (sFilename, aContent, aCharset, eDisposition);
  }
}