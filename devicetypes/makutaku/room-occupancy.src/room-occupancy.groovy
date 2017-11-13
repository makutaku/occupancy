/*****************************************************************************************************************
*
*  A SmartThings device handler to allow handling rooms as devices which have states.
*  Copyright (C) 2017 bangali
*
*  License:
*  This program is free software: you can redistribute it and/or modify it under the terms of the GNU
*  General Public License as published by the Free Software Foundation, either version 3 of the License, or
*  (at your option) any later version.
*
*  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
*  implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
*  for more details.
*
*  You should have received a copy of the GNU General Public License along with this program.
*  If not, see <http://www.gnu.org/licenses/>.
*
*  Name: Room Occupancy
*  Source: https://github.com/adey/bangali/blob/master/devicetypes/bangali/rooms-occupancy.src/rooms-occupancy.groovy
*  Version: 0.01
*
*
*****************************************************************************************************************/

metadata {
	definition (
    	name: "Room Occupancy",
        namespace: "makutaku",
        author: "makutaku")		{
		capability "Actuator"
		capability "Button"
		capability "Sensor"
        capability "Presence Sensor"
		attribute "roomOccupancy", "string"
		command "occupied"
        command "checking"
		command "vacant"
        command "engaged"
		command "reserved"
		command "kaput"
		command "updateRoomOccupancy", ["string"]
	}

	simulator	{
	}

	tiles(scale: 2)		{
    	multiAttributeTile(name: "roomOccupancy", width: 2, height: 2, canChangeBackground: true)		{
			tileAttribute ("device.roomOccupancy", key: "PRIMARY_CONTROL")		{
				attributeState "occupied", label: 'Occupied', icon:"st.Health & Wellness.health12", backgroundColor:"#90af89"
				attributeState "checking", label: 'Checking', icon:"st.Health & Wellness.health9", backgroundColor:"#616969"
				attributeState "vacant", label: 'Vacant', icon:"st.Home.home18", backgroundColor:"#6879af"
				attributeState "engaged", label: 'Engaged', icon:"st.locks.lock.locked", backgroundColor:"#c079a3"
				attributeState "reserved", label: 'Reserved', icon:"st.Office.office7", backgroundColor:"#b29600"
				attributeState "kaput", label: 'Kaput', icon:"st.Outdoor.outdoor18", backgroundColor:"#8a5128"
            }
       		tileAttribute ("device.status", key: "SECONDARY_CONTROL")	{
				attributeState "default", label:'${currentValue}'
			}
        }
        standardTile("occupied", "device.occupied", width: 2, height: 2, canChangeIcon: true) {
			state "occupied", label:"Occupied", icon: "st.Health & Wellness.health12", action: "occupied", backgroundColor:"#ffffff", nextState:"toOccupied"
            state "toOccupied", label:"Occupied", icon:"st.Health & Wellness.health12", backgroundColor:"#90af89"
		}
		standardTile("checking", "device.checking", width: 2, height: 2, canChangeIcon: true) {
			state "checking", label:"Checking", icon: "st.Health & Wellness.health9", action: "checking", backgroundColor:"#ffffff", nextState:"toChecking"
			state "toChecking", label:"Checking", icon: "st.Health & Wellness.health9", backgroundColor:"#616969"
		}
        standardTile("vacant", "device.vacant", width: 2, height: 2, canChangeIcon: true) {
			state "vacant", label:"Vacant", icon: "st.Home.home18", action: "vacant", backgroundColor:"#ffffff", nextState:"toVacant"
			state "toVacant", label:"Vacant", icon: "st.Home.home18", backgroundColor:"#6879af"
		}
        standardTile("engaged", "device.engaged", width: 2, height: 2, canChangeIcon: true) {
			state "engaged", label:"Engaged", icon: "st.locks.lock.locked", action: "engaged", backgroundColor:"#ffffff", nextState:"toEngaged"
			state "toEngaged", label:"Engaged", icon: "st.locks.lock.locked", backgroundColor:"#c079a3"
		}
        standardTile("reserved", "device.reserved", width: 2, height: 2, canChangeIcon: true) {
			state "reserved", label:"Reserved", icon: "st.Office.office7", action: "reserved", backgroundColor:"#ffffff", nextState:"toReserved"
			state "toReserved", label:"Reserved", icon: "st.Office.office7", backgroundColor:"#b29600"
		}
        standardTile("kaput", "device.kaput", width: 2, height: 2, canChangeIcon: true) {
			state "kaoput", label:"Kaput", icon: "st.Outdoor.outdoor18", action: "kaput", backgroundColor:"#ffffff", nextState:"toKaput"
			state "toKaput", label:"Kaput", icon: "st.Outdoor.outdoor18", backgroundColor:"#8a5128"
		}
		main (["roomOccupancy"])
		details (["roomOccupancy", "occupied", "checking", "vacant", "engaged", "reserved", "kaput"])
	}
}

def parse(String description)	{}

def installed()		{
	initialize()
	vacant()
}

def updated()	{	initialize()	}

def	initialize()	{	sendEvent(name: "numberOfButtons", value: 6)	}

def occupied()	{	stateUpdate('occupied')		}

def checking()	{	stateUpdate('checking')		}

def vacant()	{	stateUpdate('vacant')		}

def engaged()	{	stateUpdate('engaged')		}

def reserved()	{	stateUpdate('reserved')		}

def kaput()		{	stateUpdate('kaput')		}

private	stateUpdate(state)	{
	def oldState = device.currentValue('roomOccupancy')
	if (oldState != state)	{
		updateRoomOccupancy(state)
        if (parent)
        	parent.handleSwitches(oldState, state)
	}
	resetTile(state)
}

private updateRoomOccupancy(roomOccupancy = null) 	{
	roomOccupancy = roomOccupancy?.toLowerCase()
    log.debug "Updating room occupancy to ${roomOccupancy} ..."
	
    def msgTextMap = ['occupied':'Room is occupied: ', 'engaged':'Room is engaged: ', 'vacant':'Room is vacant: ', 'reserved':'Room is reserved: ', 'checking':'Checking room status: ', 'kaput':'Room not in service: ']
	if (!roomOccupancy || !(msgTextMap.containsKey(roomOccupancy))) {
    	log.debug "${device.displayName}: Missing or invalid parameter room occupancy. Allowed values Occupied, Vacant, Engaged, Reserved or Checking."
        return
    }
    
    def description = "${device.displayName} changed to ${roomOccupancy}"
    log.debug description
	sendEvent(name: "roomOccupancy", value: roomOccupancy, descriptionText: description, isStateChange: true, displayed: true)
	
    def buttonMap = ['occupied':1, 'engaged':4, 'vacant':3, 'reserved':5, 'checking':2, 'kaput':6]
    def button = buttonMap[roomOccupancy]
	sendEvent(name: "button", value: "pushed", data: [buttonNumber: button], descriptionText: "$device.displayName button $button was pushed.", isStateChange: true)
	
    def currentValue = device.currentValue('roomOccupancy')
    log.debug "currentValue=${currentValue}"
    log.debug msgTextMap[currentValue]
    def statusMsg = msgTextMap[currentValue] + "<datetime>"//+ formatLocalTime()
    
	sendEvent(name: "status", value: statusMsg, isStateChange: true, displayed: false)

    if (currentValue != "kaput") {
        log.debug "Sending Presence Sensor event"
		sendEvent(name: "presence", value: (currentValue == "vacant") ? "not present" : "present", isStateChange: true, displayed: false)
    }
}

private formatLocalTime(format = "EEEE", time = now())		{
	def formatter = new java.text.SimpleDateFormat(format)
	formatter.setTimeZone(location.timeZone)
	return formatter.format(time)
}

private	resetTile(roomOccupancy)	{
    sendEvent(name: roomOccupancy, value: roomOccupancy, descriptionText: "reset tile ${roomOccupancy} to ${roomOccupancy}", isStateChange: true, displayed: false)
}

def generateEvent(state = null)		{
	if	(state && device.currentValue('roomOccupancy') != state)
		updateRoomOccupancy(state)
	return null
}

def getRoomState()	{	return device.currentValue('roomOccupancy')		}