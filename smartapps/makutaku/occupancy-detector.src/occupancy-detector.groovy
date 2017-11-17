/*****************************************************************************************************************
*
*  A SmartThings SmartApp to detect if someone might be using a room, even when the person stops moving in it.
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
*  Name: Occupancy Detector
*  Source: https://github.com/adey/bangali/blob/master/smartapps/bangali/rooms-manager.src/rooms-manager.groovy
*  Version: 0.01
*
*  DONE:   
*  0) Forked from https://github.com/adey/bangali/blob/master/smartapps/bangali/rooms-manager.src/rooms-manager.groovy
*****************************************************************************************************************/

definition (
    name: "Occupancy Detector",
    namespace: "makutaku",
    author: "makutaku",
    description: "Detect if someone might be present in a room, even after movement stops. An occupance device will be created for each configured room, and its state will change depending on the probability the room is occupied.",
    category: "My Apps",
    singleInstance: true,
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences	{
	page(name: "mainPage", title: "Installed Rooms", install: true, uninstall: true) {
		section {
            app(name: "Occupancy Detector", appName: "Occupancy Detector Child App", namespace: "makutaku", title: "New room", multiple: true)
		}
	}
}

def installed()		{	initialize()	}

def updated()		{
	unsubscribe()
	initialize()
}

def initialize()	{
	log.info "There are ${childApps.size()} rooms."
	childApps.each	{ child ->
		log.info "Room: ${child.label}"
	}
}