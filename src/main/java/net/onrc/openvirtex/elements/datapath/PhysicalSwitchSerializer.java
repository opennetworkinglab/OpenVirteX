package net.onrc.openvirtex.elements.datapath;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class PhysicalSwitchSerializer implements JsonSerializer<PhysicalSwitch> {

    @Override
    public JsonElement serialize(PhysicalSwitch sw, Type switchType,
            JsonSerializationContext context) {
	JsonPrimitive dpid = new JsonPrimitive(sw.switchName);
	return dpid;
    }

}
