/* **DISCLAIMER**
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * Without limitation of the foregoing, Contributors/Regents expressly does not warrant that:
 * 1. the software will meet your requirements or expectations;
 * 2. the software or the software content will be free of bugs, errors, viruses or other defects;
 * 3. any results, output, or data provided through or generated by the software will be accurate, up-to-date, complete or reliable;
 * 4. the software will be compatible with third party software;
 * 5. any errors in the software will be corrected.
 * The user assumes all responsibility for selecting the software and for the results obtained from the use of the software. The user shall bear the entire risk as to the quality and the performance of the software.
 */ 
 
/**
 *  Mode Change Thermostat Temperature
 *  Version 2.1.0
 *
 * Copyright RBoy, redistribution of any changes or modified code is not allowed without permission
 * 2016-3-5 - Fixed bug with settings all modes temperature when in multi mode configuration mode
 * 2016-1-26 - Fixed a bug with modes selection
 * 2016-1-26 - Combined the single and individual temperature apps into a single app through a configurable menu
 * 2015-5-18 - Added support for individual mode temperatures
 * 2015-5-17 - Initial code
 *
 */
definition(
		name: "Ultimate Mode Change Thermostat Temperature",
		namespace: "rboy",
		author: "RBoy",
		description: "Change the thermostat(s) temperature on a mode(s) change",
    	category: "Green Living",
    	iconUrl: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving.png",
    	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving@2x.png",
    	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving@3x.png")

preferences {
	page(name: "setupApp")
    page(name: "tempPage")
}

def setupApp() {
    dynamicPage(name: "setupApp", title: "Mode and Thermostat Selection", install: false, uninstall: true, nextPage: "tempPage") {
        section("Choose thermostat(s)") {
            input "thermostats", "capability.thermostat", required: true, multiple: true
        }

        section("Choose Mode(s)") {
            input "modes", "mode", title: "Set for specific mode(s)", multiple: true, required: false
        }
        
        section() {
            label title: "Assign a name", required: false
        }
    }
}

def tempPage() {
    dynamicPage(name:"tempPage", title: "Temperature for each thermostat", uninstall: true, install: true) {
        section() {
            input name: "multiTempModes", type: "bool", title: "Separate temperatures for each mode", description: "Do you want to define different temperatures for each mode?", required: true, submitOnChange: true
            input name: "multiTempThermostat", type: "bool", title: "Separate temperatures for each thermostat", description: "Do you want to define different temperatures for each thermostat?", required: true, submitOnChange: true
        }
    	def maxModes = multiTempModes ? (modes == null ? 1 : modes.size()) : 1
    	for (int j = 0; j < maxModes; j++) {
        	def modeName = multiTempModes ? (modes == null ? "All" : modes[j]) : "All"
        	section() {
            	paragraph title: "$modeName Mode Thermostat Settings", "Enter the heat/cool temperatures for thermostats in this mode"
            }
            def maxThermostats = multiTempThermostat ? thermostats.size() : 1
            for (int i = 0; i < maxThermostats; i++) {
                def heat = settings."opHeatSet${i}${j}"
                def cool = settings."opCoolSet${i}${j}"
                log.debug "$modeName Mode ${multiTempThermostat ? thermostats[i] : "All Thermostats"} Heat: $heat, Cool: $cool"
                section("${multiTempThermostat ? thermostats[i] : "All Thermostats"} heat/cool temperatures") {
                    input name: "opHeatSet${i}${j}", type: "decimal", defaultValue: "${heat}", title: "When Heating", description: "Heating temperature for mode", required: true
                    input name: "opCoolSet${i}${j}", type: "decimal", defaultValue: "${cool}", title: "When Cooling", description: "Cooling temperature for mode", required: true
                }
            }
        }
    }
}

def installed()
{
	subscribeToEvents()
}

def updated()
{
    unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {
    subscribe(location, modeChangeHandler)
    log.debug "Selected Modes -> $modes, settings -> $settings"
}

// Handle mode changes, reinitialize the current temperature after a mode change
def modeChangeHandler(evt) {
	// Since we are manually entering the mode, check the mode before continuing since these aren't registered with the system (thanks @bravenel)
    if (modes && !modes.contains(evt.value)) {
    	log.debug "$evt.value mode not in list of selected modes $modes"
    	return
    }
        
    def maxModes = multiTempModes ? (modes == null ? 1 : modes.size()) : 1
    int j = 0
    for (j = 0; j < maxModes; j++) {
    	def modeName = multiTempModes ? (modes == null ? "All" : modes[j]) : "All"
        if (modeName == evt.value) { // check for matching mode in loop
            break // got it
        }
    }

    def maxThermostats = multiTempThermostat ? thermostats.size() : 1
    for (int i = 0; i < maxThermostats; i++) {
        def opHeatSet = settings."opHeatSet${i}${j}"
        def opCoolSet = settings."opCoolSet${i}${j}"

        if (multiTempThermostat) { // individual thermostat settings
            thermostats[i].setHeatingSetpoint(opHeatSet)
            thermostats[i].setCoolingSetpoint(opCoolSet)
            log.info "Set ${thermostats[i]} Heat $opHeatSet°, Cool $opCoolSet° on $evt.value mode"
            sendNotificationEvent("Set ${thermostats[i]} Heat $opHeatSet°, Cool $opCoolSet° on $evt.value mode")
        } else {
            thermostats.setHeatingSetpoint(opHeatSet)
            thermostats.setCoolingSetpoint(opCoolSet)
            log.info "Set ${thermostats} Heat $opHeatSet°, Cool $opCoolSet° on $evt.value mode"
            sendNotificationEvent("Set ${thermostats} Heat $opHeatSet°, Cool $opCoolSet° on $evt.value mode")
        }
    }
}