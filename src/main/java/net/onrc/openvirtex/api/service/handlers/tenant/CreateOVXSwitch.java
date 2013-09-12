package net.onrc.openvirtex.api.service.handlers.tenant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.exceptions.InvalidDPIDException;
import net.onrc.openvirtex.exceptions.InvalidTenantIdException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;

public class CreateOVXSwitch extends ApiHandler<Map<String, Object>> {

	Logger log = LogManager.getLogger(CreateOVXSwitch.class.getName());

	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		JSONRPC2Response resp = null;

		try {
			Number tenantId = HandlerUtils.<Number>fetchField(TenantHandler.TENANT, 
					params, true, null);
			List<String> dpids = HandlerUtils.<List<String>>fetchField(TenantHandler.DPIDS, 
					params, true, null);
			HandlerUtils.isValidTenantId(tenantId.intValue());
			final OVXMap map = OVXMap.getInstance();
			final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId.intValue());
			final List<Long> longDpids = new ArrayList<Long>();
			for (final String dpid : dpids) {
			    final long longDpid = Long.parseLong(dpid);
			    longDpids.add(longDpid);
			}
			HandlerUtils.isValidDPID(tenantId.intValue(), longDpids);
			final OVXSwitch ovxSwitch = virtualNetwork.createSwitch(longDpids);
			if (ovxSwitch == null) {
				resp = new JSONRPC2Response(-1, 0);
			} else {
			    this.log.info("Created virtual switch {} in virtual network {}",
				    ovxSwitch.getSwitchId(), virtualNetwork.getTenantId());
			    resp = new JSONRPC2Response(ovxSwitch.getSwitchId(), 0);
			}
			
		} catch (MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": Unable to create virtual network : " + e.getMessage()), 0);
		} catch (InvalidDPIDException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": Invalid DPID : " + e.getMessage()), 0);
		} catch (InvalidTenantIdException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": Invlaid tenant id : " + e.getMessage()), 0);
		}

		return resp;

	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}





}
