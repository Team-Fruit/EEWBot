package net.teamfruit.eewbot.gateway;

import net.teamfruit.eewbot.entity.dmdataapi.DmdataError;

public class DmdataGatewayException extends EEWGatewayException {

    private final DmdataError dmdataError;

    public DmdataGatewayException(DmdataError dmdataError) {
        this.dmdataError = dmdataError;
    }

    public DmdataError getDmdataError() {
        return dmdataError;
    }
}
