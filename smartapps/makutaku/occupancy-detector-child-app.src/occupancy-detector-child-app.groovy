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
*  Version: 0.01
*
*  DONE:
*  Forked from: https://github.com/adey/bangali/blob/master/smartapps/bangali/rooms-child-app.src/rooms-child-app.groovy
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

private getRoomDeviceId()	{	
	return "rm_${app.id}"	
}

private childCreated()		{
	if (getChildDevice(getRoomDeviceId()))
		return true
	else
		return false
}

def mainPage()	{

	def childWasCreated = childCreated()
	
	dynamicPage(name: "mainPage", 
    	title: "Adding a room", 
        install: true, uninstall: childWasCreated)	{
		if (!childWasCreated)	{
			section		{
				label title: "Room name:", defaultValue: app.label, required: true
			}
    	} else	{
			section		{
				paragraph "Room name:\n${app.label}"
			}
		}
		section("Update room state on away mode?")	{
 			input "awayModes", "mode", title: "Away mode(s)?", required: false, multiple: true
		}
		section("Which sensors will be used to learn if someone might be present in the room?")	{
        	sensorsInputs();
		}
		section("Revert room back to 'vacant' when motion is not detected?")	{
        	timeoutInputs()
		}
	}
}

def sensorsInputs() {
    input "insideMotionSensors", "capability.motionSensor", title: "Motion sensor(s) inside the room", required: false, multiple: true
    input "perimeterContactSensors", "capability.contactSensor", title: "Contact sensor(s) used to access the room", required: false, multiple: true
    input "outsideMotionSensors", "capability.motionSensor", title: "Motion sensor(s) outside the room", required: false, multiple: true
}

def timeoutInputs() {
    input "reservationTimeOutInSeconds", "number", title: "Time out when room is 'reserved'", required: false, multiple: false, defaultValue: 3*60, range: "5..*"
    input "occupationTimeOutInSeconds", "number", title: "Time out when room is 'occupied'", required: false, multiple: false, defaultValue: 60*60, range: "5..*"
    input "engagementTimeOutInSeconds", "number", title: "Time out when room is 'engaged'", required: false, multiple: false, defaultValue: 12*60*60, range: "5..*"
    input "outerPerimeterRestorationDelayInSeconds", "number", title: "Outside motion sensor delay'", required: false, multiple: false, defaultValue: 30, range: "0..*"
}

def installed()		{
	log.debug "Installed app ${app.label} with settings: ${settings}"
    log.debug "app label: ${app.label}"
    log.debug "app name: ${app.name}"
}

def updated()	{
	log.debug "Updated app ${app.label} with settings: ${settings}"
    log.debug "app label: ${app.label}"
    log.debug "app name: ${app.name}"
    unschedule()
    initialize()
}

def	initialize()	{
	log.debug "Initializing app ${app.label} ..."
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
    
    if (oldState == "kaput") {
    	log.info "Not changing room state to '${state}' because room is not in service."
        return
    }
    
    if (state != oldState) {
    	log.info "Requesting room state change: '${oldState}' => '${state}'"
        roomDevice.generateEvent(state)
        updateTimeout(state)
    }
}

def makeRoomVacant()	{
	log.debug "Making room vacant."
    setRoomState('vacant')
}

def makeRoomReserved()	{
	log.debug "Making room reserved."
    setRoomState('reserved')
}

def makeRoomOccupied()	{
	log.debug "Making room occupied."
    setRoomState('occupied')
}

def makeRoomEngaged()	{
	log.debug "Making room engaged."
    setRoomState('engaged')
}

private outerPerimeterBreached() {
	if (outsideMotionSensors) {
    	return outsideMotionSensors.any{sensor -> sensor.currentValue("motion") == "active"}
    }
	return true
}

private innerPerimeterBreached() {
	if (perimeterContactSensors) {
    	return perimeterContactSensors.any{sensor -> sensor.currentValue("contact") == "open"}
    }
    return true
}

private roomActive() {
	if (insideMotionSensors) {
    	return insideMotionSensors.any{sensor -> sensor.currentValue("motion") == "active"}
    }
	return false
}

def getReservationTimeOutInSecondsAsInteger() {
	return (reservationTimeOutInSeconds != null) ? reservationTimeOutInSeconds.toInteger() : 0
}

def getOccupationTimeOutInSecondsAsInteger() {
	return (occupationTimeOutInSeconds != null) ? occupationTimeOutInSeconds.toInteger() : 0
}

def getEngagementTimeOutInSecondsAsInteger() {
	return (engagementTimeOutInSeconds != null) ? engagementTimeOutInSeconds.toInteger() : 0
}

def updateTimeout(roomState = null)	{
    def timeoutInSeconds = 0
        
    if (!roomActive()) {
        if (!roomState)
            roomState = getRoomState()
            
        log.debug "Updating timeout for state '${roomState}'"

        switch (roomState) {
            case "reserved":
                timeoutInSeconds = getReservationTimeOutInSecondsAsInteger()
                break;
            case "occupied":
                timeoutInSeconds = getOccupationTimeOutInSecondsAsInteger()
                break;
            case "engaged":
                timeoutInSeconds = getEngagementTimeOutInSecondsAsInteger()
                break;
        }
    }

    if (timeoutInSeconds > 0) {
		scheduleTimeout(timeoutInSeconds)
    }
    else {
        cancelTimeout()
    }
}

def scheduleTimeout(timeoutInSeconds)	{
    log.debug "Scheduling timeout to ${timeoutInSeconds} seconds."
	runIn(timeoutInSeconds, timeoutExpired)
}

def cancelTimeout() {
	log.debug "Cancelling timeout."
    unschedule(timeoutExpired)
}

def timeoutExpired() {
	log.info "Timeout has expired."
    makeRoomVacant()
}

def	insideMotionActiveEventHandler(evt)	{
	log.debug "inside motion: active"
    cancelTimeout()

	if (innerPerimeterBreached()) {
    	if (outerPerimeterBreached())
        	makeRoomReserved()
        else
        	makeRoomOccupied()
    }
    else {
    	makeRoomEngaged()
    }
}

def	insideMotionInactiveEventHandler(evt)	{
	log.debug "inside motion: inactive"    
    updateTimeout()
}

def	perimeterContactOpenEventHandler(evt)	{
	log.debug "perimeter contact: open"
	log.info "inner perimeter has been breached"
    if (outerPerimeterBreached())
    	makeRoomReserved()
    else 
    	makeRoomOccupied()
}

def	perimeterContactClosedEventHandler(evt)	{
	log.debug "perimeter contact: closed"
	
    def innerPerimeterBreached = innerPerimeterBreached()
    if (innerPerimeterBreached) {
    	log.debug "inner perimeter is still breached"
    } 
    else  {
    	log.info "inner perimeter is restored"
    }
}

def	outsideMotionActiveEventHandler(evt)	{
	log.debug "outside motion: active"
 	log.info "outer perimeter has been breached"
    unschedule(onOutsidePerimeterRestored)
    def state = getRoomState()
    if (state == "occupied") {
    	makeRoomReserved()
    }
}

def	outsideMotionInactiveEventHandler(evt)	{
	log.debug "outside motion: inactive"
    
	def outerPerimeterBreached = outerPerimeterBreached()
    if (outerPerimeterBreached) {
    	log.debug "outer perimeter is still breached"
        return
    } 
    else  {
    	log.info "outer perimeter is restored"
    }
    
    if (getRoomState() == "engaged" || !roomActive()) {
    	//nothing to do
    	return
    }

	log.debug "Scheduling outsidePerimeterRestorationHandler to run in ${outerPerimeterRestorationDelayInSeconds} seconds."
	runIn(outerPerimeterRestorationDelayInSeconds, onOutsidePerimeterRestored)
}

def outsidePerimeterRestorationHandler() {
	log.debug "Running outsidePerimeterRestorationHandler."
    if (innerPerimeterBreached()) 
    	makeRoomOccupied()
    else 
    	makeRoomEngaged()
}

def	modeEventHandler(evt)	{
	if (awayModes && awayModes.contains(evt.value)) {
    	makeRoomVacant()
    }
}

def uninstalled() {
	getChildDevices().each	{
		deleteChildDevice(it.deviceNetworkId)
	}
}

def spawnChildDevice(roomName)	{
	if (!childCreated()) {
    	log.debug "Spawning child device."
		def device = addChildDevice("makutaku", "Room Occupancy", getRoomDeviceId(), null, [name: getRoomDeviceId(), label: roomName, completedSetup: true])
		log.info "Child device created: name=${device.name} label=${device.label}"
    }
}

def handleRoomStateChange(oldState = null, state = null)	{
	log.info "Room has changed state: '${oldState}' => '${state}'"
	if (state && oldState != state)	{
    	updateTimeout(state)
		return true
	}
	return false
}
