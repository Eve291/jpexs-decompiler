/*
 *  Copyright (C) 2010-2013 JPEXS
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jpexs.decompiler.flash.tags;

import com.jpexs.decompiler.flash.Main;
import com.jpexs.decompiler.flash.SWFInputStream;
import com.jpexs.decompiler.flash.SWFOutputStream;
import com.jpexs.decompiler.flash.abc.CopyOutputStream;
import com.jpexs.decompiler.flash.tags.base.BoundedTag;
import com.jpexs.decompiler.flash.tags.base.CharacterTag;
import com.jpexs.decompiler.flash.tags.base.Container;
import com.jpexs.decompiler.flash.types.MATRIX;
import com.jpexs.decompiler.flash.types.RECT;
import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Defines a sprite character
 */
public class DefineSpriteTag extends CharacterTag implements Container, BoundedTag {

   /**
    * Character ID of sprite
    */
   public int spriteId;
   /**
    * Number of frames in sprite
    */
   public int frameCount;
   /**
    * A series of tags
    */
   public List<Tag> subTags;
   private int level;

   @Override
   public int getCharacterID() {
      return spriteId;
   }

   private RECT getCharacterBounds(HashMap<Integer, CharacterTag> allCharacters, Set<Integer> characters) {
      RECT ret = new RECT(Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE);
      for (int c : characters) {
         Tag t = allCharacters.get(c);
         RECT r = null;
         if (t instanceof BoundedTag) {
            r = ((BoundedTag) t).getRect(allCharacters);
         }
         if (r != null) {
            ret.Xmin = Math.min(r.Xmin, ret.Xmin);
            ret.Ymin = Math.min(r.Ymin, ret.Ymin);
            ret.Xmax = Math.max(r.Xmax, ret.Xmax);
            ret.Ymax = Math.max(r.Ymax, ret.Ymax);
         }
      }
      return ret;
   }

   @Override
   public RECT getRect(HashMap<Integer, CharacterTag> characters) {
      RECT ret = new RECT(Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE);
      HashMap<Integer, Integer> depthMap = new HashMap<Integer, Integer>();
      for (Tag t : subTags) {
         Set<Integer> needed = t.getNeededCharacters();
         MATRIX m = null;
         if (t instanceof PlaceObjectTypeTag) {
            PlaceObjectTypeTag pot = (PlaceObjectTypeTag) t;
            m = pot.getMatrix();
            int charId = pot.getCharacterId();
            if (charId > -1) {
               depthMap.put(pot.getDepth(), charId);
            } else {
               needed.add(depthMap.get(pot.getDepth()));
            }
         }
         if (needed.isEmpty()) {
            continue;
         }
         RECT r = getCharacterBounds(characters, needed);

         if (m != null) {
            Point topleft = m.apply(new Point(r.Xmin, r.Ymin));
            Point bottomright = m.apply(new Point(r.Xmax, r.Ymax));
            r.Xmin = Math.min(topleft.x, bottomright.x);
            r.Ymin = Math.min(topleft.y, bottomright.y);
            r.Xmax = Math.max(topleft.x, bottomright.x);
            r.Ymax = Math.max(topleft.y, bottomright.y);
         }
         ret.Xmin = Math.min(r.Xmin, ret.Xmin);
         ret.Ymin = Math.min(r.Ymin, ret.Ymin);
         ret.Xmax = Math.max(r.Xmax, ret.Xmax);
         ret.Ymax = Math.max(r.Ymax, ret.Ymax);
      }
      return ret;
   }

   /**
    * Constructor
    *
    * @param data Data bytes
    * @param version SWF version
    * @throws IOException
    */
   public DefineSpriteTag(byte[] data, int version, int level, long pos) throws IOException {
      super(39, "DefineSprite", data, pos);
      SWFInputStream sis = new SWFInputStream(new ByteArrayInputStream(data), version, pos);
      spriteId = sis.readUI16();
      frameCount = sis.readUI16();
      subTags = sis.readTagList(level + 1);
   }
   static int c = 0;

   /**
    * Gets data bytes
    *
    * @param version SWF version
    * @return Bytes of data
    */
   @Override
   public byte[] getData(int version) {
      if (Main.DISABLE_DANGEROUS) {
         return super.getData(version);
      }
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      OutputStream os = baos;
      if (Main.DEBUG_COPY) {
         os = new CopyOutputStream(os, new ByteArrayInputStream(super.data));
      }
      SWFOutputStream sos = new SWFOutputStream(os, version);
      try {
         sos.writeUI16(spriteId);
         sos.writeUI16(frameCount);
         sos.writeTags(subTags);
         sos.writeUI16(0);
         sos.close();
      } catch (IOException e) {
      }
      return baos.toByteArray();
   }

   /**
    * Returns all sub-items
    *
    * @return List of sub-items
    */
   @Override
   public List<Object> getSubItems() {
      List<Object> ret = new ArrayList<Object>();
      ret.addAll(subTags);
      return ret;
   }

   /**
    * Returns number of sub-items
    *
    * @return Number of sub-items
    */
   @Override
   public int getItemCount() {
      return subTags.size();
   }

   @Override
   public boolean hasSubTags() {
      return true;
   }

   @Override
   public List<Tag> getSubTags() {
      return subTags;
   }

   @Override
   public Set<Integer> getNeededCharacters() {
      Set<Integer> ret = new HashSet<Integer>();
      for (Tag t : subTags) {
         ret.addAll(t.getNeededCharacters());
      }
      return ret;
   }
}
