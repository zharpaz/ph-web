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
package com.helger.web.useragent;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.pair.IPair;
import com.helger.commons.string.ToStringGenerator;

/**
 * This class wraps the basic elements of a user agent string.
 *
 * @author Philip Helger
 */
@NotThreadSafe
final class UserAgentElementList
{
  private final List <Object> m_aList = new ArrayList <Object> ();

  public void add (@Nonnull final IPair <String, String> aPair)
  {
    ValueEnforcer.notNull (aPair, "Pair");
    m_aList.add (aPair);
  }

  public void add (@Nonnull final String sValue)
  {
    ValueEnforcer.notNull (sValue, "Value");
    m_aList.add (sValue);
  }

  public void add (@Nonnull final List <String> aItems)
  {
    ValueEnforcer.notNull (aItems, "Items");
    m_aList.add (aItems);
  }

  @Nullable
  public String getPairValue (@Nullable final String sKey)
  {
    for (final Object aObj : m_aList)
      if (aObj instanceof IPair <?, ?>)
        if (((IPair <?, ?>) aObj).getFirst ().equals (sKey))
          return (String) ((IPair <?, ?>) aObj).getSecond ();
    return null;
  }

  /**
   * Search for a list item starting with the specified String.<br>
   * Example: any/version (list1; list2) any2/version2<br>
   * -> This method is for searching for "list1" and "list2"
   *
   * @param sPrefix
   *        The search string
   * @return The list item string
   */
  @Nullable
  public String getListItemStartingWith (@Nullable final String sPrefix)
  {
    for (final Object aObj : m_aList)
      if (aObj instanceof List <?>)
        for (final Object aListItem : (List <?>) aObj)
          if (((String) aListItem).startsWith (sPrefix))
            return (String) aListItem;
    return null;
  }

  public boolean containsString (@Nullable final String sString)
  {
    for (final Object aObj : m_aList)
      if (aObj instanceof String)
        if (((String) aObj).equals (sString))
          return true;
    return false;
  }

  @Nullable
  public String getStringValueFollowing (@Nullable final String sString)
  {
    int nIdx1 = -1;
    int nIdx = 0;
    for (final Object aObj : m_aList)
      if (aObj instanceof String)
      {
        if (nIdx1 >= 0 && nIdx == nIdx1 + 1)
          return (String) aObj;
        if (((String) aObj).equals (sString))
          nIdx1 = nIdx;
        nIdx++;
      }
    return null;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("items", m_aList).toString ();
  }
}
