/**
 * openHAB, the open Home Automation Bus.
 * Copyright (C) 2010-2013, openHAB.org <admin@openhab.org>
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
package org.openhab.binding.digitalstrom.internal.client.constants;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 	Alexander Betker
 * @since 1.3.0
 * @version	digitalSTROM-API 1.14.5
 */
public class SceneToStateMapper {
	
	private Map<Short, Boolean>	map = null;
	
	public SceneToStateMapper() {
		this.map = new HashMap<Short, Boolean>();
		this.init();
	}
	
	private void init() {
		map.put((short) 0, false);
		map.put((short) 1, false);
		map.put((short) 2, false);
		map.put((short) 3, false);
		map.put((short) 4, false);
		
		map.put((short) 5, true);
		map.put((short) 6, true);
		map.put((short) 7, true);
		map.put((short) 8, true);
		map.put((short) 9, true);
		
		map.put((short) 13, false);
		map.put((short) 14, true);
		
		map.put((short) 32, false);
		map.put((short) 33, true);
		map.put((short) 34, false);
		map.put((short) 35, true);
		map.put((short) 36, false);
		map.put((short) 37, true);
		map.put((short) 38, false);
		map.put((short) 39, true);
		
		map.put((short) 50, false);
		map.put((short) 51, true);
		
	}
	
	/**
	 * If there is a state mapping for this scene,
	 * returns true. You should run this before using
	 * the method 'getMapping(short)' !!!
	 * 
	 * @param val	sceneId
	 * @return		true, if a mapping exists for this sceneId
	 */
	public boolean isMappable(short val) {
		return map.containsKey(val);
	}
	
	/**
	 * Please check at first with a call 'isMappable(short)'
	 * if there is a mapping for this number.
	 * If not you can not be sure to get a valid boolean-false
	 * 
	 * @param val	scene-number
	 * @return		true or false if this scene will cause a 'isOn' state in device
	 */
	public boolean getMapping(short val) {
		if (map.containsKey(val)) {
			return map.get(val);
		}
		return false;
	}

}
