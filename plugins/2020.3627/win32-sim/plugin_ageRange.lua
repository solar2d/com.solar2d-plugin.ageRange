local lib = require('CoronaLibrary'):new{name = 'plugin.ageRange', publisherId = 'com.solar2d'}


local functions = {'init', 'requestAgeRange', 'requestSignificantUpdatePermission', 'requestCommunicationPermission', 'startListeningForCommunicationResponses'}

for i = 1, #functions do
    local f = functions[i]
    lib[f] = function()
        print('plugin.ageRange: ' .. f .. '() is not supported on this platform.')
        return true
    end
end

return lib
