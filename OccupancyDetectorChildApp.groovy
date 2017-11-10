/*****************************************************************************************************************
*
*  A SmartThings child smartapp which creates the "room" device using the rooms occupancy DTH.
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
*  Name: Room Child App
*  Source: https://github.com/adey/bangali/blob/master/smartapps/bangali/rooms-child-app.src/rooms-child-app.groovy
*  Version: 0.02
*
*   DONE:
*   1) added support for multiple away modes. when home changes to any these modes room is set to vacant but
*            only if room is in occupied or checking state.
*   2) added subscription for motion devices so if room is vacant or checking move room state to occupied.
*   3) added support for switches to be turned on when room is changed to occupied.
*   4) added support for switches to be turned off when room is changed to vacant, different switches from #3.
*   5) added button push events to tile commands, where occupied = button 1, ..., kaput = button 6 so it is
*            supported by ST Smart Lighting smartapp.
*
*****************************************************************************************************************/

definition	(
    name: "Occupancy Detector Child App",
    namespace: "makutaku",
    parent: "makutaku:Occupancy Detector",
    author: "makutaku",
    description: "DO NOT INSTALL DIRECTLY OR PUBLISH. Creates the \"room\" device using the Room Occupancy DTH.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	page(name: "mainPage")
}

def mainPage()	{
	dynamicPage(name: "mainPage", title: "Configure Occupancy Detection", install: true, uninstall: childCreated())		{
		if (!childCreated())	{
			section		{
				label title: "Room name:", required: true
			}
    	} else {
			section		{
				paragraph "Room name:\n${app.label}"
			}
		}
        
		section("Update room state on away mode?")		{
 			input "awayModes", "mode", title: "Away mode(s)?", required: false, multiple: true
		}
		section("Which sensors will be used to detect if someone might be inside the room?")		{
 			input "insideMotionSensors", "capability.motionSensor", title: "Motion sensor(s) inside the room", required: false, multiple: true
 			input "perimeterContactSensors", "capability.contactSensor", title: "Contact sensor(s) around the room", required: false, multiple: true
 			input "outsideMotionSensors", "capability.motionSensor", title: "Motion sensor(s) outside the room", required: false, multiple: true
		}
		section("Turn on switches when someone might be inside the room?")		{
 			input "switches", "capability.switch", title: "Which switch(es)?", required: false, multiple: true
		}
		section("Time (in seconds) to revert back to Vacant when motion is not detected")		{
 			input "reservationTimeOutInSeconds", "number", title: "Time out when room is Reserved", required: false, multiple: false, defaultValue: 90, range: "5..*"
 			input "occupationTimeOutInSeconds", "number", title: "Time out when room is Occupied", required: false, multiple: false, defaultValue: 90, range: "5..*"
 			input "engagementTimeOutInSeconds", "number", title: "Time out when room is Engaged", required: false, multiple: false, defaultValue: 90, range: "5..*"
		}
		section("Turn off switches when room is vacant?")		{
 			input "switches2", "capability.switch", title: "Which switch(es)?", required: false, multiple: true
		}
	}
}

def installed()		{
	log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated()	{
	log.debug "Updated with settings: ${settings}"
    unschedule()
    initialize()
}

def	initialize()	{
	log.debug "Initializing ..."
	if (!childCreated())	{
		spawnChildDevice(app.label)
	}
	if (awayModes)	{
		subscribe(location, modeEventHandler)
	}
	if (insideMotionSensors)	{
    	log.debug "Subscribing to inside motion sensors ..."
    	subscribe(insideMotionSensors, "motion.active", insideMotionActiveEventHandler)
    	subscribe(insideMotionSensors, "motion.inactive", insideMotionInactiveEventHandler)
	}
	if (perimeterContactSensors)	{
    	log.debug "Subscribing to perimeter contact sensors ..."
    	subscribe(perimeterContactSensors, "contact.open", perimeterContactOpenEventHandler)
    	subscribe(perimeterContactSensors, "contact.closed", perimeterContactClosedEventHandler)
	}
	if (outsideMotionSensors)	{
    	log.debug "Subscribing to outside motion sensors ..."
    	subscribe(outsideMotionSensors, "motion.active", outsideMotionActiveEventHandler)
    	subscribe(outsideMotionSensors, "motion.inactive", outsideMotionInactiveEventHandler)
	}
	log.debug "initialization done."
}

private getRoomState() {
	def roomDevice = getChildDevice(getRoomDeviceId())
	def state = roomDevice.getRoomState()
    return state
}

private setRoomState(state) {
	def roomDevice = getChildDevice(getRoomDeviceId())
    def oldState = roomDevice.getRoomState()
    if (state != oldState) {
    	log.debug "Changing state: ${oldState} => ${state}"
    	
        if (oldState == 'vacant') {
    		switchesOn()
    	}
        roomDevice.generateEvent(state)
    }
}

private outerPerimeterBreached() {
	if (outsideMotionSensors) {
    	return outsideMotionSensors.any{sensor -> sensor.currentValue("motion") == "active"}
    }
	return True
}

private innerPerimeterBreached() {
	if (perimeterContactSensors) {
    	return perimeterContactSensors.any{sensor -> sensor.currentValue("contact") == "open"}
    }
	return True
}

private roomActive() {
	if (insideMotionSensors) {
    	return insideMotionSensors.any{sensor -> sensor.currentValue("motion") == "active"}
    }
	return False
}

def getReservationTimeOutInSecondsAsInteger() {
	if (reservationTimeOutInSeconds != null) {
    	return reservationTimeOutInSeconds.toInteger()
    }
	return 10
}

def getOccupationTimeOutInSecondsAsInteger() {
	if (occupationTimeOutInSeconds != null) {
    	return occupationTimeOutInSeconds.toInteger()
    }
	return 20
}

def getEngagementTimeOutInSecondsAsInteger() {
	if (engagementTimeOutInSeconds != null) {
    	return engagementTimeOutInSeconds.toInteger()
    }
	return 30
}

def checkRoomActivation()	{
	log.debug "Checking room activation ..."
    if (!roomActive()) {
    	log.debug "room is not active"
        def roomState = getRoomState()
        log.debug "roomState=${roomState}"
    
    	def timeOutInSec = 0
    
        switch (roomState) {
        	case "reserved":
                timeOutInSec = getReservationTimeOutInSecondsAsInteger()
            	break;
        	case "occupied":
                timeOutInSec = getOccupationTimeOutInSecondsAsInteger()
            	break;
            case "engaged":
                timeOutInSec = getEngagementTimeOutInSecondsAsInteger()
                break;
        }
        
        if (timeOutInSec > 0) {
        	log.debug "Vacating room in ${timeOutInSec} seconds"
        	runIn(timeOutInSec, makeRoomVacant)
        }
    }
    else {
    	log.debug "room is still active"
    }
}

def	insideMotionActiveEventHandler(evt)	{
	log.debug "inside motion: active"

    unschedule()   

    if (innerPerimeterBreached()) {
    	setRoomState(outerPerimeterBreached() ? "reserved" : "occupied")
    }
    else {
    	setRoomState("engaged")
    }
}

def	insideMotionInactiveEventHandler(evt)	{
	log.debug "inside motion: inactive"    
    checkRoomActivation()
}

def	perimeterContactOpenEventHandler(evt)	{
	log.debug "perimeter contact: open"
	log.debug "inner perimeter has been breached"
    
    if (outerPerimeterBreached()) {
    	setRoomState("reserved")
    }
    else {
    	setRoomState("occupied")
    }
    
    checkRoomActivation()
}

def	perimeterContactClosedEventHandler(evt)	{
	log.debug "perimeter contact: closed"
	
    def innerPerimeterBreached = innerPerimeterBreached()
    if (innerPerimeterBreached) {
    	log.debug "inner perimeter is still breached"
    } 
    else  {
    	log.debug "inner perimeter is restored"
    }
}

def	outsideMotionActiveEventHandler(evt)	{
	log.debug "outside motion: active"
 	log.debug "outer perimeter has been breached"
    
    def state = getRoomState()
    if (state == "occupied") {
    	setRoomState("reserved")
        checkRoomActivation()
    }
}

def	outsideMotionInactiveEventHandler(evt)	{
	log.debug "outside motion: inactive"
    
	def outerPerimeterBreached = outerPerimeterBreached()
    if (outerPerimeterBreached) {
    	log.debug "outer perimeter is still breached"
    } 
    else  {
    	log.debug "outer perimeter is restored"
    }
    
    if (!outerPerimeterBreached && roomActive() && getRoomState() == "reserved") {
    	setRoomState("occupied")
        checkRoomActivation()
    }
}

def	modeEventHandler(evt)	{
	//if (awayModes && awayModes.contains(evt.value))
    //	makeRoomVacant()
}

def makeRoomVacant()	{
	log.debug "Making room vacant."
    setRoomState('vacant')
	switchesOff()
        
	//def state = getRoomState()
	//if (['occupied', 'checking'].contains(state))	{
	//	setRoomState('vacant')
	//	switchesOff()
	//}
    
    //def roomState = getRoomState()
    //log.debug "room state: ${roomState}"
}

def uninstalled() {
	getChildDevices().each	{
		deleteChildDevice(it.deviceNetworkId)
	}
}

def spawnChildDevice(roomName)	{
	app.updateLabel(app.label)
	if (!childCreated())
    	log.debug "Spawning child device ..."
		def child = addChildDevice("makutaku", "Room Occupancy", getRoomDeviceId(), null, [name: getRoomDeviceId(), label: roomName, completedSetup: true])
}

private childCreated()		{
	if (getChildDevice(getRoomDeviceId()))
		return true
	else
		return false
}

private getRoomDeviceId()	{	return "rm_${app.id}"	}

def handleSwitches(oldState = null, state = null)	{
	if (state && oldState != state)	{
		if (state == 'occupied')
			switchesOn()
		else
			if (state == 'vacant')
				switchesOff()
		return true
	}
    else
    	return false
}

private switchesOn()	{
	if (switches)
		switches.on()
}

private switchesOff()	{
	if (switches2)
		switches2.off()
}