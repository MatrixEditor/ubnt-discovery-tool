-- Save this Lua script as "ubnt_protocol.lua" and put it in the Wireshark "plugins" folder.

local ubnt_protocol = Proto("UBNT", "Ubnt Discovery Protocol")

local f_version = ProtoField.uint8("ubnt_protocol.version", "Version", base.DEC)
local f_command = ProtoField.uint8("ubnt_protocol.command", "Command", base.DEC)
local f_size = ProtoField.int16("ubnt_protocol.size", "Size", base.DEC)

local f_type = ProtoField.uint8("ubnt_protocol.type", "Type", base.DEC)
local f_length = ProtoField.int16("ubnt_protocol.length", "Length", base.DEC)
local f_value = ProtoField.bytes("ubnt_protocol.value", "Value")

ubnt_protocol.fields = {
    f_version, f_command, f_size,
    f_type, f_length, f_value
}

-- Mapping between record type values and their names
local record_type_names = {
    [1] = "HW_ADDRESS",
    [2] = "IPINFO",
    [3] = "FW_VERSION",
    [4] = "ADDRESS_ENTRY",
    [5] = "MAC_ENTRY",
    [6] = "USERNAME",
    [7] = "SALT",
    [8] = "RND_CHALLENGE",
    [9] = "CHALLENGE",
    [10] = "UPTIME",
    [11] = "HOSTNAME",
    [12] = "PLATFORM",
    [13] = "ESSID",
    [14] = "WIFI_MODE",
    [16] = "WEB_UI",
    [20] = "MODEL",
    [18] = "SEQ",
    [19] = "SOURCE_MAC",
    [21] = "MODEL_V2",
    [22] = "SHORT_VERSION",
    [23] = "DEFAULT",
    [24] = "LOCATING",
    [25] = "DHCPC",
    [26] = "DHCPC_BOUND",
    [27] = "REQ_W",
    [28] = "SSHD_PORT",
}

local function get_record_type_name(type_value)
    return record_type_names[type_value] or "<UNKNOWN>"
end

local function heuristic_checker(buffer, pinfo, tree)
    -- guard for length
    local length = buffer:len()
    if length < 3 then return false end

    local potential_len = buffer(2,2):int()
    -- why is this not working?
    if (potential_len == (buffer:len() - 4))
    then
        ubnt_protocol.dissector(buffer, pinfo, tree)
        return true
    else return false end
end


local function dissector(buffer, pinfo, tree)
    pinfo.cols.protocol = ubnt_protocol.name

    local offset = 0
    local version = buffer(offset, 1):uint()
    local command = buffer(offset + 1, 1):uint()
    local size = buffer(offset + 2, 2):int()

    local subtree = tree:add(ubnt_protocol, buffer(), "Ubnt Discovery Protocol, Length: " .. size)
    subtree:add(f_version, buffer(offset, 1))
    subtree:add(f_command, buffer(offset + 1, 1))
    subtree:add(f_size, buffer(offset + 2, 2))

    offset = offset + 4

    while offset < buffer:len() do
        local record_type = buffer(offset, 1):uint()
        local record_length = buffer(offset + 1, 2):int()
        local record_value = buffer(offset + 3, record_length):bytes()

        local record_subtree = subtree:add(buffer(offset, 3 + record_length), "Record (" .. get_record_type_name(record_type) .. "), Length: " .. record_length)
        record_subtree:add(f_type, buffer(offset, 1)):append_text(" (Type: " .. get_record_type_name(record_type) .. ")")
        record_subtree:add(f_length, buffer(offset + 1, 2))
        record_subtree:add(f_value, buffer(offset + 3, record_length))

        offset = offset + 3 + record_length
    end
end

function ubnt_protocol.dissector(tvbuffer, pinfo, treeitem)
    return dissector(tvbuffer, pinfo, treeitem)
end

ubnt_protocol:register_heuristic("udp", heuristic_checker)
