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
*  Version: 0.02
*
*  DONE:
*   1) added support for multiple away modes. when home changes to any these modes room is set to vacant but
*            only if room is in occupied or checking state.
*   2) added subscription for motion devices so if room is vacant or checking move room state to occupied.
*   3) added support for switches to be turned on when room is changed to occupied.
*   4) added support for switches to be turned off when room is changed to vacant, different switches from #3.
*   5) added button push events to tile commands, where occupied = button 1, ..., kaput = button 6 so it is
*            supported by ST Smart Lighting smartapp.
*
*****************************************************************************************************************/

definition (
    name: "Occupancy Detector",
    namespace: "makutaku",
    author: "makutaku",
    description: "Detect if someone might be using a room, even when the person stops moving in it.",
    category: "My Apps",
    singleInstance: true,
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences	{
	page(name: "mainPage", title: "Installed Rooms", install: true, uninstall: true, submitOnChange: true) {
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