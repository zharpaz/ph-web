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
package com.helger.web.mock;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.servlet.ServletInputStream;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.web.servlet.AbstractServletInputStream;

/**
 * A {@link ServletInputStream} for testing based on a predefined byte array or
 * an existing {@link InputStream}.
 *
 * @author Philip Helger
 */
public class MockServletInputStream extends AbstractServletInputStream
{
  private final InputStream m_aIS;

  public MockServletInputStream (@Nonnull final byte [] aContent)
  {
    this (new NonBlockingByteArrayInputStream (aContent));
  }

  public MockServletInputStream (@Nonnull final InputStream aBaseIS)
  {
    m_aIS = ValueEnforcer.notNull (aBaseIS, "BaseInputStream");
  }

  @Override
  public int read () throws IOException
  {
    return m_aIS.read ();
  }

  @Override
  public void close () throws IOException
  {
    m_aIS.close ();
    super.close ();
  }
}
