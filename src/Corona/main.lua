-- Age Range Plugin Sample
-- Simple demonstration of all Age Range API features

local ageRange = require("plugin.ageRange")
local widget = require("widget")

-- Display setup
display.setStatusBar(display.HiddenStatusBar)
local centerX = display.contentCenterX
local centerY = display.contentCenterY

-- Background
local background = display.newRect(centerX, centerY, display.contentWidth, display.contentHeight)
background:setFillColor(0.95, 0.95, 0.95)

-- Title
local title = display.newText({
    text = "Age Range Plugin Demo",
    x = centerX,
    y = 40,
    font = native.systemFontBold,
    fontSize = 18
})
title:setFillColor(0.2, 0.2, 0.2)

-- Status display
local statusText = display.newText({
    text = "Ready",
    x = centerX,
    y = 75,
    width = display.contentWidth - 40,
    font = native.systemFont,
    fontSize = 11,
    align = "center"
})
statusText:setFillColor(0.3, 0.3, 0.3)

-- Result display
local resultText = display.newText({
    text = "",
    x = centerX,
    y = 130,
    width = display.contentWidth - 40,
    height = 180,
    font = native.systemFont,
    fontSize = 10,
    align = "left"
})
resultText:setFillColor(0, 0, 0)

-- Helper function to update status
local function updateStatus(message)
    statusText.text = message
    print("Status: " .. message)
end

-- Helper function to display results
local function displayResult(eventData)
    local result = "Event Data:\n"
    for key, value in pairs(eventData) do
        result = result .. string.format("%s: %s\n", key, tostring(value))
    end
    resultText.text = result
    print(result)
end

-- Age Range Event Handler
local function onAgeRangeEvent(event)
    print("Age Range Event Received")
    displayResult(event)
    
    if event.isError then
        updateStatus("Error: " .. (event.errorMessage or "Unknown error"))
    elseif event.declined then
        updateStatus("User declined to share age")
    elseif event.isAvailable then
        local ageInfo = "Age Range: "
        if event.lowerBound then
            ageInfo = ageInfo .. event.lowerBound
            if event.upperBound then
                ageInfo = ageInfo .. "-" .. event.upperBound
            else
                ageInfo = ageInfo .. "+"
            end
        end
        if event.hasParentalControls then
            ageInfo = ageInfo .. " (Supervised)"
        end
        updateStatus(ageInfo)
    else
        updateStatus("Age range not available")
    end
end


-- Initialize plugin listeners
ageRange.init(onAgeRangeEvent)

-- Button Y position tracker
local buttonY = 240

-- Request Age Range Button
local btnRequestAge = widget.newButton({
    label = "Request Age Range",
    x = centerX,
    y = buttonY,
    width = 240,
    height = 35,
    fontSize = 13,
    onRelease = function()
        updateStatus("Requesting age range...")
        ageRange.requestAgeRange(13, 16, 18)
    end
})
buttonY = buttonY + 45

-- Clear Results Button
local btnClear = widget.newButton({
    label = "Clear Results",
    x = centerX,
    y = buttonY,
    width = 240,
    height = 35,
    fontSize = 13,
    fillColor = { default={0.7,0.7,0.7}, over={0.5,0.5,0.5} },
    onRelease = function()
        resultText.text = ""
        updateStatus("Ready")
    end
})
