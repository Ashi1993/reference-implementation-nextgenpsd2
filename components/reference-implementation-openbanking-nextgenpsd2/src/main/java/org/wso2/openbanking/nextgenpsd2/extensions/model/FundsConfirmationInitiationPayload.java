package org.wso2.openbanking.nextgenpsd2.extensions.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.validator.consent.ValidFundsConfirmationInitiationPayload;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Funds confirmation initiation payload object used for initiating funds confirmation consents.
 */
@ValidFundsConfirmationInitiationPayload
public class FundsConfirmationInitiationPayload {

    @NotNull(message = "FORMAT_ERROR:" + ErrorConstants.MANDATORY_ELEMENTS_MISSING)
    @Valid
    private AccountReference account;

    private LocalDate cardExpiryDate;

    @JsonIgnore
    private final Map<String, Object> additionalProperties = new HashMap<>();

    public AccountReference getAccount() {
        return account;
    }

    public void setAccount(AccountReference account) {
        this.account = account;
    }

    public LocalDate getCardExpiryDate() {
        return cardExpiryDate;
    }

    public void setCardExpiryDate(LocalDate cardExpiryDate) {
        this.cardExpiryDate = cardExpiryDate;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }
}
